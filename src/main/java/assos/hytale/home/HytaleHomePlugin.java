package assos.hytale.home;

import assos.hytale.home.commands.DelHomeCommand;
import assos.hytale.home.commands.HomeCommand;
import assos.hytale.home.commands.HomesCommand;
import assos.hytale.home.commands.SetHomeCommand;
import assos.hytale.home.config.HomesConfig;
import assos.hytale.home.home.HomeService;
import assos.hytale.home.home.HomeStorage;
import assos.hytale.home.home.HomeTeleportService;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class HytaleHomePlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String CONFIG_NAME = "homes_config";
    private static final String DATA_FILE = "homes.json";

    private final Config<HomesConfig> config;
    private HomeStorage storage;
    private HomeService homeService;
    private HomeTeleportService teleportService;
    private EventRegistration<?, ?> disconnectRegistration;

    public HytaleHomePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        this.config = this.withConfig(CONFIG_NAME, HomesConfig.CODEC);
    }

    @Override
    protected void setup() {
        config.save();

        HomesConfig settings = config.get();
        this.storage = new HomeStorage(getDataDirectory().resolve(DATA_FILE));
        this.homeService = new HomeService(storage, settings.getDefaultHomeName(), settings.getMaxHomes(),
                settings.getMaxHomeNameLength(), settings.isAllowCrossWorld(),
                parseBlockedWorlds(settings.getBlockedWorlds()));
        this.teleportService = new HomeTeleportService(homeService, settings.isSafeLanding(),
                settings.getTeleportCooldownSeconds(), settings.getTeleportWarmupSeconds(),
                settings.isCombatTagEnabled(), settings.getCombatTagSeconds());

        boolean requirePermissions = settings.isRequirePermissions();
        getCommandRegistry().registerCommand(new SetHomeCommand(homeService, requirePermissions));
        getCommandRegistry().registerCommand(new HomeCommand(homeService, teleportService, requirePermissions));
        getCommandRegistry().registerCommand(new HomesCommand(homeService, requirePermissions));
        getCommandRegistry().registerCommand(new DelHomeCommand(homeService, requirePermissions));

        this.disconnectRegistration = getEventRegistry().registerGlobal(PlayerDisconnectEvent.class,
                event -> teleportService.cancel(event.getPlayerRef().getUuid(), null));
    }

    @Override
    protected void start() {
        try {
            storage.load();
        } catch (IOException exception) {
            LOGGER.at(Level.SEVERE).withCause(exception)
                    .log("Failed to load homes; starting with an empty store");
        }
    }

    @Override
    protected void shutdown() {
        if (disconnectRegistration != null) {
            disconnectRegistration.unregister();
            disconnectRegistration = null;
        }
        if (teleportService != null) {
            teleportService.shutdown();
            teleportService = null;
        }
    }

    private static Set<String> parseBlockedWorlds(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
