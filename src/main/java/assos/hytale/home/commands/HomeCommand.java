package assos.hytale.home.commands;

import assos.hytale.home.home.Home;
import assos.hytale.home.home.HomeService;
import assos.hytale.home.home.HomeTeleportException;
import assos.hytale.home.home.HomeTeleporter;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

public final class HomeCommand extends AbstractPlayerCommand {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final HomeService homeService;

    public HomeCommand(@Nonnull HomeService homeService) {
        super("home", "Teleport to a home");
        requireNoPermission();
        this.homeService = homeService;
        addUsageVariant(new NamedHomeCommand(homeService));
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        teleportHome(context, store, ref, playerRef, world, homeService, null);
    }

    private static void teleportHome(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world,
            @Nonnull HomeService homeService, @Nullable String requestedName) {
        Optional<Home> target;
        try {
            target = homeService.resolveSelector(playerRef.getUuid(), requestedName);
        } catch (IllegalArgumentException exception) {
            context.sendMessage(Message.raw(exception.getMessage()).color("#FF5555"));
            return;
        }

        if (target.isEmpty()) {
            if (requestedName != null) {
                String label = isNumeric(requestedName)
                        ? "Home number " + requestedName
                        : "Home '" + requestedName + "'";
                context.sendMessage(
                        Message.raw(label + " not found. Use /homes to see the numbered list.").color("#FF5555"));
            } else if (homeService.list(playerRef.getUuid()).isEmpty()) {
                context.sendMessage(
                        Message.raw("You have no homes. Use /sethome to create one.").color("#FF5555"));
            } else {
                context.sendMessage(Message.raw("Home '" + homeService.getDefaultHomeName()
                        + "' not found. Use /homes to choose a home.").color("#FF5555"));
            }
            return;
        }

        Home home = target.get();
        if (!homeService.isCrossWorldAllowed() && !Objects.equals(home.getWorld(), world.getName())) {
            context.sendMessage(Message.raw("Cross-world teleport is disabled.").color("#FF5555"));
            return;
        }

        try {
            HomeTeleporter.teleport(store, ref, home);
            context.sendMessage(Message.raw("Teleported to home '" + home.getName() + "'.").color("#55FF55"));
        } catch (HomeTeleportException exception) {
            context.sendMessage(Message.raw(exception.getMessage()).color("#FF5555"));
        } catch (RuntimeException exception) {
            LOGGER.at(Level.SEVERE).withCause(exception)
                    .log("Failed to teleport %s to home %s", playerRef.getUsername(), home.getName());
            context.sendMessage(Message.raw("Teleport failed. Check the server logs.").color("#FF5555"));
        }
    }

    private static boolean isNumeric(@Nullable String value) {
        return value != null && value.trim().matches("\\d+");
    }

    private static final class NamedHomeCommand extends AbstractPlayerCommand {

        private final HomeService homeService;
        private final RequiredArg<String> nameArg;

        private NamedHomeCommand(@Nonnull HomeService homeService) {
            super("Teleport to a named home");
            requireNoPermission();
            this.homeService = homeService;
            this.nameArg = withRequiredArg("name", "Home name", ArgTypes.STRING);
            this.nameArg.suggest((sender, input, offset, result) -> HomeSuggestions.suggest(homeService, sender,
                    input, result));
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            teleportHome(context, store, ref, playerRef, world, homeService, nameArg.get(context));
        }
    }
}
