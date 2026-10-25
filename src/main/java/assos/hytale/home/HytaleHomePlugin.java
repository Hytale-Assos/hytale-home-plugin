package assos.hytale.home;

import assos.hytale.home.commands.DelHomeCommand;
import assos.hytale.home.commands.HomeCommand;
import assos.hytale.home.commands.HomesCommand;
import assos.hytale.home.commands.SetHomeCommand;
import assos.hytale.home.config.HomesConfig;
import assos.hytale.home.home.HomeService;
import assos.hytale.home.home.HomeStorage;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Level;

public final class HytaleHomePlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String CONFIG_NAME = "homes_config";
    private static final String DATA_FILE = "homes.json";

    private final Config<HomesConfig> config;
    private HomeStorage storage;
    private HomeService homeService;

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
                settings.getMaxHomeNameLength(), settings.isAllowCrossWorld());

        getCommandRegistry().registerCommand(new SetHomeCommand(homeService));
        getCommandRegistry().registerCommand(new HomeCommand(homeService));
        getCommandRegistry().registerCommand(new HomesCommand(homeService));
        getCommandRegistry().registerCommand(new DelHomeCommand(homeService));
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
}
