package assos.hytale.home.commands;

import assos.hytale.home.HomePermissions;
import assos.hytale.home.home.Home;
import assos.hytale.home.home.HomeService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;

public final class HomesCommand extends AbstractPlayerCommand {

    private final HomeService homeService;

    public HomesCommand(@Nonnull HomeService homeService, boolean requirePermissions) {
        super("homes", "List your homes");
        HomePermissions.apply(this, requirePermissions, HomePermissions.HOMES);
        this.homeService = homeService;
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        List<Home> homes = homeService.list(playerRef.getUuid());
        if (homes.isEmpty()) {
            context.sendMessage(Message.raw("You have no homes. Use /sethome to create one.").color("#FF5555"));
            return;
        }

        context.sendMessage(Message.raw("Your homes (" + homes.size() + "/" + homeService.getMaxHomes() + "):")
                .color("#FFFF55"));
        for (int index = 0; index < homes.size(); index++) {
            Home home = homes.get(index);
            String defaultMarker = home.getName().equals(homeService.getDefaultHomeName()) ? " (default)" : "";
            context.sendMessage(Message.raw(" " + (index + 1) + ". " + home.getName() + defaultMarker
                    + " — world: " + home.getWorld() + " — " + formatPosition(home)).color("#AAAAAA"));
        }
        context.sendMessage(Message.raw("Use /home <number> or /home <name>.").color("#AAAAAA"));
    }

    private static String formatPosition(Home home) {
        return String.format(Locale.ROOT, "%.1f, %.1f, %.1f", home.getX(), home.getY(), home.getZ());
    }
}
