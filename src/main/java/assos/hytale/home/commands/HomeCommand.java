package assos.hytale.home.commands;

import assos.hytale.home.HomePermissions;
import assos.hytale.home.home.Home;
import assos.hytale.home.home.HomeService;
import assos.hytale.home.home.HomeTeleportService;
import assos.hytale.home.ui.HomesPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Optional;

public final class HomeCommand extends AbstractPlayerCommand {

    private final HomeService homeService;
    private final HomeTeleportService teleportService;

    public HomeCommand(@Nonnull HomeService homeService, @Nonnull HomeTeleportService teleportService,
            boolean requirePermissions) {
        super("home", "Open the homes menu or teleport to a named home");
        HomePermissions.apply(this, requirePermissions, HomePermissions.HOME);
        this.homeService = homeService;
        this.teleportService = teleportService;
        addUsageVariant(new NamedHomeCommand(homeService, teleportService, requirePermissions));
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            context.sendMessage(Message.raw("Could not open the homes menu.").color("#FF5555"));
            return;
        }
        player.getPageManager().openCustomPage(ref, store, new HomesPage(playerRef, homeService, teleportService));
    }

    private static void teleportHome(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world,
            @Nonnull HomeService homeService, @Nonnull HomeTeleportService teleportService,
            @Nonnull String requestedName) {
        Optional<Home> target;
        try {
            target = homeService.resolveSelector(playerRef.getUuid(), requestedName);
        } catch (IllegalArgumentException exception) {
            context.sendMessage(Message.raw(exception.getMessage()).color("#FF5555"));
            return;
        }

        if (target.isEmpty()) {
            String label = isNumeric(requestedName)
                    ? "Home number " + requestedName
                    : "Home '" + requestedName + "'";
            context.sendMessage(
                    Message.raw(label + " not found. Use /homes to see the numbered list.").color("#FF5555"));
            return;
        }

        teleportService.request(playerRef, store, ref, world, target.get());
    }

    private static boolean isNumeric(@Nonnull String value) {
        return value.trim().matches("\\d+");
    }

    private static final class NamedHomeCommand extends AbstractPlayerCommand {

        private final HomeService homeService;
        private final HomeTeleportService teleportService;
        private final RequiredArg<String> nameArg;

        private NamedHomeCommand(@Nonnull HomeService homeService, @Nonnull HomeTeleportService teleportService,
                boolean requirePermissions) {
            super("Teleport to a named home");
            HomePermissions.apply(this, requirePermissions, HomePermissions.HOME);
            this.homeService = homeService;
            this.teleportService = teleportService;
            this.nameArg = withRequiredArg("name", "Home name", ArgTypes.STRING);
            this.nameArg.suggest((sender, input, offset, result) -> HomeSuggestions.suggest(homeService, sender,
                    input, result));
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            teleportHome(context, store, ref, playerRef, world, homeService, teleportService, nameArg.get(context));
        }
    }
}
