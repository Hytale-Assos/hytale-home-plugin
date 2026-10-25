package assos.hytale.home.home;

import com.hypixel.hytale.builtin.teleport.components.TeleportHistory;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public final class HomeTeleporter {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private HomeTeleporter() {
    }

    public static void teleport(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
            @Nonnull Home home) throws HomeTeleportException {
        String worldName = home.getWorld();
        if (worldName == null || worldName.isBlank()) {
            throw new HomeTeleportException("This home has no world.");
        }

        World targetWorld = Universe.get().getWorld(worldName);
        if (targetWorld == null) {
            throw new HomeTeleportException("World '" + worldName + "' is not loaded.");
        }

        recordHistory(store, ref);

        Transform transform = new Transform(
                new Vector3d(home.getX(), home.getY(), home.getZ()),
                new Rotation3f(home.getPitch(), home.getYaw(), home.getRoll()));
        Teleport teleport = Teleport.createForPlayer(targetWorld, transform);
        store.addComponent(ref, Teleport.getComponentType(), teleport);
    }

    private static void recordHistory(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        try {
            ComponentType<EntityStore, TeleportHistory> historyType = TeleportHistory.getComponentType();
            if (historyType == null) {
                return;
            }

            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) {
                return;
            }

            HeadRotation headRotation = store.getComponent(ref, HeadRotation.getComponentType());
            Rotation3f rotation = headRotation != null
                    ? new Rotation3f(headRotation.getRotation())
                    : new Rotation3f(transform.getRotation());

            TeleportHistory history = store.ensureAndGetComponent(ref, historyType);
            history.append(store.getExternalData().getWorld(), new Vector3d(transform.getPosition()), rotation, "Home");
        } catch (RuntimeException exception) {
            LOGGER.at(Level.FINE).withCause(exception).log("Could not record teleport history");
        }
    }
}
