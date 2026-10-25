package assos.hytale.home.ui;

import assos.hytale.home.home.Home;
import assos.hytale.home.home.HomeService;
import assos.hytale.home.home.HomeTeleportService;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class HomesPage extends InteractiveCustomUIPage<HomesPage.PageData> {

    private static final String PAGE_LAYOUT = "Pages/SelectOverrideRespawnPointPage.ui";
    private static final String HOME_BUTTON_LAYOUT = "Pages/OverrideRespawnPointButton.ui";
    private static final Value<String> HOME_BUTTON_STYLE = Value.ref(HOME_BUTTON_LAYOUT,
            "DefaultRespawnButtonStyle");

    private final PlayerRef playerRef;
    private final HomeService homeService;
    private final HomeTeleportService teleportService;

    public HomesPage(@Nonnull PlayerRef playerRef, @Nonnull HomeService homeService,
            @Nonnull HomeTeleportService teleportService) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.playerRef = playerRef;
        this.homeService = homeService;
        this.teleportService = teleportService;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder builder,
            @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        builder.append(PAGE_LAYOUT);

        builder.clear("#Title");
        builder.appendInline("#Title",
                "Label { Text: \"Your Homes\"; Style: (FontSize: 15, VerticalAlignment: Center, "
                        + "RenderUppercase: true, TextColor: #b4c8c9, RenderBold: true, "
                        + "HorizontalAlignment: Center); Anchor: (Height: 31); Padding: (Horizontal: 19); }");

        builder.clear("#Content");
        builder.appendInline("#Content", "Group #HomeList { LayoutMode: Top; }");
        builder.appendInline("#Content",
                "Label #Hint { Text: \"Click a home to teleport.\"; Style: (FontSize: 14, "
                        + "TextColor: #96a9be, HorizontalAlignment: Center); Anchor: (Top: 10); }");

        List<Home> homes = homeService.list(playerRef.getUuid());
        if (homes.isEmpty()) {
            builder.set("#Hint.Text", Message.raw("You have no homes. Use /sethome to create one."));
            return;
        }

        for (int index = 0; index < homes.size(); index++) {
            Home home = homes.get(index);
            String selector = "#HomeList[" + index + "]";
            builder.append("#HomeList", HOME_BUTTON_LAYOUT);
            builder.set(selector + ".Style", HOME_BUTTON_STYLE);
            String defaultMarker = home.getName().equals(homeService.getDefaultHomeName()) ? " (default)" : "";
            builder.set(selector + " #Name.Text",
                    home.getName() + defaultMarker + " - world: " + home.getWorld());
            builder.set(selector + " #Distance.Text", formatPosition(home));
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selector,
                    EventData.of("Home", home.getName()), false);
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull PageData data) {
        if (data.getHome() == null) {
            return;
        }

        Optional<Home> target = homeService.resolveSelector(playerRef.getUuid(), data.getHome());
        if (target.isPresent()) {
            teleportService.request(playerRef, store, ref, store.getExternalData().getWorld(), target.get());
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            player.getPageManager().closePage(ref, store);
        }
    }

    private static String formatPosition(Home home) {
        return String.format(Locale.ROOT, "%.1f, %.1f, %.1f", home.getX(), home.getY(), home.getZ());
    }

    public static final class PageData {

        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Home", Codec.STRING),
                        (data, value, info) -> data.home = value,
                        (data, info) -> data.home)
                .add()
                .build();

        private String home;

        private PageData() {
        }

        public String getHome() {
            return home;
        }
    }
}
