package assos.hytale.home.home;

import assos.hytale.home.HomePermissions;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.damage.DamageDataComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class HomeTeleportService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String ERROR_COLOR = "#FF5555";
    private static final String SUCCESS_COLOR = "#55FF55";

    private final HomeService homeService;
    private final boolean safeLanding;
    private final boolean combatTagEnabled;
    private final int combatTagSeconds;
    private final int warmupSeconds;
    private final long cooldownMillis;
    private final CooldownTracker cooldowns = new CooldownTracker();
    private final Map<UUID, PendingTeleport> pending = new ConcurrentHashMap<>();

    public HomeTeleportService(@Nonnull HomeService homeService, boolean safeLanding, int cooldownSeconds,
            int warmupSeconds, boolean combatTagEnabled, int combatTagSeconds) {
        this.homeService = Objects.requireNonNull(homeService, "homeService");
        this.safeLanding = safeLanding;
        this.cooldownMillis = Math.max(0, cooldownSeconds) * 1000L;
        this.warmupSeconds = Math.max(0, warmupSeconds);
        this.combatTagEnabled = combatTagEnabled;
        this.combatTagSeconds = Math.max(0, combatTagSeconds);
    }

    public void request(@Nonnull PlayerRef playerRef, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull World currentWorld, @Nonnull Home home) {
        UUID playerId = playerRef.getUuid();
        cancel(playerId, null);

        if (homeService.isWorldBlocked(home.getWorld())) {
            sendError(playerRef, "Teleport to this world is blocked.");
            return;
        }

        if (!homeService.isCrossWorldAllowed() && !Objects.equals(home.getWorld(), currentWorld.getName())) {
            sendError(playerRef, "Cross-world teleport is disabled.");
            return;
        }

        World targetWorld = Universe.get().getWorld(home.getWorld());
        if (targetWorld == null) {
            sendError(playerRef, "World '" + home.getWorld() + "' is not loaded.");
            return;
        }

        if (combatTagEnabled && isInCombat(store, ref)) {
            sendError(playerRef, "You are in combat. Wait " + combatRemainingSeconds(store, ref) + " seconds.");
            return;
        }

        if (safeLanding && !HomeSafety.isSafe(targetWorld, home.getX(), home.getY(), home.getZ())) {
            sendError(playerRef, "The destination of this home is not safe.");
            return;
        }

        if (!playerRef.hasPermission(HomePermissions.BYPASS_COOLDOWN)) {
            long remaining = cooldowns.remainingMillis(playerId, System.currentTimeMillis(), cooldownMillis);
            if (remaining > 0) {
                sendError(playerRef, "You must wait " + ((remaining + 999) / 1000) + " seconds before teleporting home.");
                return;
            }
        }

        if (warmupSeconds == 0) {
            teleportNow(playerRef, store, ref, home);
            return;
        }

        startWarmup(playerRef, store, ref, currentWorld, home);
    }

    public void cancel(@Nonnull UUID playerId, @Nullable String message) {
        PendingTeleport pendingTeleport = pending.remove(playerId);
        if (pendingTeleport == null) {
            return;
        }
        if (pendingTeleport.future != null) {
            pendingTeleport.future.cancel(false);
        }
        if (message != null) {
            sendError(pendingTeleport.playerRef, message);
        }
    }

    public void shutdown() {
        pending.values().forEach(pendingTeleport -> {
            if (pendingTeleport.future != null) {
                pendingTeleport.future.cancel(false);
            }
        });
        pending.clear();
    }

    private void startWarmup(@Nonnull PlayerRef playerRef, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull World currentWorld, @Nonnull Home home) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            sendError(playerRef, "Your position is not available.");
            return;
        }

        DamageDataComponent damageData = store.getComponent(ref, DamageDataComponent.getComponentType());
        PendingTeleport pendingTeleport = new PendingTeleport(playerRef, ref, currentWorld, home,
                new Vector3d(transform.getPosition()),
                damageData == null ? null : damageData.getLastDamageTime(),
                damageData == null ? null : damageData.getLastCombatAction());
        pending.put(playerRef.getUuid(), pendingTeleport);
        pendingTeleport.future = currentWorld.scheduleAfter(
                () -> currentWorld.execute(() -> complete(pendingTeleport)), warmupSeconds, TimeUnit.SECONDS);
        playerRef.sendMessage(Message.raw("Teleporting to home '" + home.getName() + "' in " + warmupSeconds
                + " seconds. Don't move.").color("#FFFF55"));
    }

    private void complete(@Nonnull PendingTeleport pendingTeleport) {
        UUID playerId = pendingTeleport.playerRef.getUuid();
        if (pending.remove(playerId, pendingTeleport) == false) {
            return;
        }
        if (!pendingTeleport.playerRef.isValid() || !pendingTeleport.ref.isValid()) {
            return;
        }

        Store<EntityStore> store = pendingTeleport.ref.getStore();
        World currentWorld = store.getExternalData().getWorld();
        if (currentWorld != pendingTeleport.world) {
            sendError(pendingTeleport.playerRef, "Teleport cancelled: you changed world.");
            return;
        }

        TransformComponent transform = store.getComponent(pendingTeleport.ref, TransformComponent.getComponentType());
        if (transform == null || transform.getPosition().distanceSquared(pendingTeleport.startPosition) > 0.25) {
            sendError(pendingTeleport.playerRef, "Teleport cancelled: you moved.");
            return;
        }

        DamageDataComponent damageData = store.getComponent(pendingTeleport.ref, DamageDataComponent.getComponentType());
        if (damageData != null
                && (!Objects.equals(damageData.getLastDamageTime(), pendingTeleport.startLastDamage)
                        || !Objects.equals(damageData.getLastCombatAction(), pendingTeleport.startLastCombat))) {
            sendError(pendingTeleport.playerRef, "Teleport cancelled: you took damage.");
            return;
        }

        if (combatTagEnabled && isInCombat(store, pendingTeleport.ref)) {
            sendError(pendingTeleport.playerRef, "Teleport cancelled: you are in combat.");
            return;
        }

        teleportNow(pendingTeleport.playerRef, store, pendingTeleport.ref, pendingTeleport.home);
    }

    private void teleportNow(@Nonnull PlayerRef playerRef, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull Home home) {
        World targetWorld = Universe.get().getWorld(home.getWorld());
        if (targetWorld == null) {
            sendError(playerRef, "World '" + home.getWorld() + "' is not loaded.");
            return;
        }
        if (homeService.isWorldBlocked(home.getWorld())) {
            sendError(playerRef, "Teleport to this world is blocked.");
            return;
        }
        if (safeLanding && !HomeSafety.isSafe(targetWorld, home.getX(), home.getY(), home.getZ())) {
            sendError(playerRef, "The destination of this home is not safe.");
            return;
        }

        try {
            HomeTeleporter.teleport(store, ref, home);
            cooldowns.mark(playerRef.getUuid(), System.currentTimeMillis());
            playerRef.sendMessage(Message.raw("Teleported to home '" + home.getName() + "'.").color(SUCCESS_COLOR));
        } catch (HomeTeleportException exception) {
            sendError(playerRef, exception.getMessage());
        } catch (RuntimeException exception) {
            LOGGER.at(Level.SEVERE).withCause(exception)
                    .log("Failed to teleport %s to home %s", playerRef.getUsername(), home.getName());
            sendError(playerRef, "Teleport failed. Check the server logs.");
        }
    }

    private boolean isInCombat(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        return combatRemainingSeconds(store, ref) > 0;
    }

    private long combatRemainingSeconds(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        if (!combatTagEnabled || combatTagSeconds <= 0) {
            return 0;
        }
        DamageDataComponent damageData = store.getComponent(ref, DamageDataComponent.getComponentType());
        if (damageData == null || damageData.getLastCombatAction() == null) {
            return 0;
        }
        TimeResource time = store.getResource(TimeResource.getResourceType());
        Instant now = time == null ? Instant.now() : time.getNow();
        long elapsed = Math.max(0, Duration.between(damageData.getLastCombatAction(), now).getSeconds());
        return Math.max(0, combatTagSeconds - elapsed);
    }

    private static void sendError(@Nonnull PlayerRef playerRef, @Nullable String message) {
        if (message != null) {
            playerRef.sendMessage(Message.raw(message).color(ERROR_COLOR));
        }
    }

    private static final class PendingTeleport {
        private final PlayerRef playerRef;
        private final Ref<EntityStore> ref;
        private final World world;
        private final Home home;
        private final Vector3d startPosition;
        private final Instant startLastDamage;
        private final Instant startLastCombat;
        private ScheduledFuture<?> future;

        private PendingTeleport(PlayerRef playerRef, Ref<EntityStore> ref, World world, Home home,
                Vector3d startPosition, Instant startLastDamage, Instant startLastCombat) {
            this.playerRef = playerRef;
            this.ref = ref;
            this.world = world;
            this.home = home;
            this.startPosition = startPosition;
            this.startLastDamage = startLastDamage;
            this.startLastCombat = startLastCombat;
        }
    }
}
