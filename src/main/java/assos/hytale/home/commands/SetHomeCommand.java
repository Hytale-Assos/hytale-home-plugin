package assos.hytale.home.commands;

import assos.hytale.home.home.Home;
import assos.hytale.home.home.HomeService;
import assos.hytale.home.home.HomeService.SetHomeResult;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Level;

public final class SetHomeCommand extends AbstractPlayerCommand {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final HomeService homeService;

    public SetHomeCommand(@Nonnull HomeService homeService) {
        super("sethome", "Set a home at your current position");
        requireNoPermission();
        this.homeService = homeService;
        addUsageVariant(new NamedSetHomeCommand(homeService));
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        setHome(context, store, ref, playerRef, world, homeService, homeService.getDefaultHomeName());
    }

    private static void setHome(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world,
            @Nonnull HomeService homeService, @Nonnull String rawName) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            context.sendMessage(Message.raw("Your position is not available.").color("#FF5555"));
            return;
        }

        HeadRotation headRotation = store.getComponent(ref, HeadRotation.getComponentType());
        Rotation3f rotation = headRotation != null
                ? new Rotation3f(headRotation.getRotation())
                : new Rotation3f(transform.getRotation());
        Vector3d position = transform.getPosition();
        Home home = new Home(rawName, world.getName(), position.x, position.y, position.z, rotation.pitch(),
                rotation.yaw(), rotation.roll(), System.currentTimeMillis());

        try {
            SetHomeResult result = homeService.setHome(playerRef.getUuid(), rawName, home);
            switch (result.status()) {
                case CREATED -> context.sendMessage(
                        Message.raw("Home '" + result.name() + "' set.").color("#55FF55"));
                case UPDATED -> context.sendMessage(
                        Message.raw("Home '" + result.name() + "' updated.").color("#55FF55"));
                case LIMIT_REACHED -> context.sendMessage(Message.raw(
                        "You have reached the maximum of " + homeService.getMaxHomes()
                                + " homes. Delete one with /delhome first.")
                        .color("#FF5555"));
            }
        } catch (IllegalArgumentException exception) {
            context.sendMessage(Message.raw(exception.getMessage()).color("#FF5555"));
        } catch (IOException exception) {
            LOGGER.at(Level.SEVERE).withCause(exception)
                    .log("Failed to save home for %s", playerRef.getUsername());
            context.sendMessage(Message.raw("Could not save your home. Check the server logs.").color("#FF5555"));
        }
    }

    private static final class NamedSetHomeCommand extends AbstractPlayerCommand {

        private final HomeService homeService;
        private final RequiredArg<String> nameArg;

        private NamedSetHomeCommand(@Nonnull HomeService homeService) {
            super("Set a home at your current position with a custom name");
            requireNoPermission();
            this.homeService = homeService;
            this.nameArg = withRequiredArg("name", "Home name", ArgTypes.STRING);
            this.nameArg.suggest((sender, input, offset, result) -> HomeSuggestions.suggest(homeService, sender,
                    input, result));
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            setHome(context, store, ref, playerRef, world, homeService, nameArg.get(context));
        }
    }
}
