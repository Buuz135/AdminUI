package com.buuz135.adminui.gui;


import com.buuz135.adminui.AdminUI;
import com.buuz135.adminui.util.AuthUtil;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.auth.ProfileServiceClient;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.accesscontrol.ban.Ban;
import com.hypixel.hytale.server.core.modules.accesscontrol.ban.InfiniteBan;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;


import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class BanGui extends InteractiveCustomUIPage<BanGui.SearchGuiData> {

    private String searchQuery = "";
    private HashMap<Ban, String> visibleItems;
    private int requestingConfirmation;
    private String inputField;
    private String reasonField;

    public BanGui(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, SearchGuiData.CODEC);
        this.searchQuery = "";
        this.requestingConfirmation = -1;
        this.visibleItems = new LinkedHashMap<>();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/Ban/Buuz135_AdminUI_BanPage.ui");
        NavBarHelper.setupBar(ref, uiCommandBuilder, uiEventBuilder, store);
        uiCommandBuilder.set("#SearchInput.Value", this.searchQuery);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput", EventData.of("@SearchQuery", "#SearchInput.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of("Button", "BackButton"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#UsernameField", EventData.of("@InputField", "#UsernameField.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ReasonField", EventData.of("@ReasonField", "#ReasonField.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#AddToBanButton", EventData.of("Button", "AddToBanButton"), false);
        this.buildList(ref, uiCommandBuilder, uiEventBuilder, store);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull SearchGuiData data) {
        super.handleDataEvent(ref, store, data);
        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        var player = store.getComponent(ref, Player.getComponentType());
        if (NavBarHelper.handleData(ref, store, data.navbar, () -> {})) {
            return;
        }
        if (data.button != null) {
            if (data.button.equals("BackButton")) {
                player.getPageManager().openCustomPage(ref, store, new AdminIndexGui(playerRef, CustomPageLifetime.CanDismiss));
                return;
            }

            if (data.button.equals("AddToBanButton")){
                ProfileServiceClient.PublicGameProfile profile = AuthUtil.getProfile(inputField);
                if (profile == null){
                    return;
                }
                var ban = new InfiniteBan(profile.getUuid(), store.getComponent(ref, UUIDComponent.getComponentType()).getUuid(), Instant.now(), reasonField);
                if (AdminUI.getInstance().getBanProvider().modify(uuids -> {uuids.put(profile.getUuid(), ban);return true;})) {
                    AdminUI.getInstance().getPlayerTracker().addPlayer(profile.getUsername(), profile.getUuid());
                    AdminUI.getInstance().getBanProvider().syncSave();
                    player.sendMessage(Message.translation("server.modules.ban.bannedWithReason").param("name", this.inputField).param("reason", this.reasonField));
                    attemptToKickPlayerIfPresent(ban, playerRef, this.inputField, this.reasonField);
                    UICommandBuilder commandBuilder = new UICommandBuilder();
                    UIEventBuilder eventBuilder = new UIEventBuilder();
                    this.buildList(ref, commandBuilder, eventBuilder, store);
                    this.sendUpdate(commandBuilder, eventBuilder, false);
                    return;
                }
                return;
            }
        }
        if (data.inputField != null) {
            inputField = data.inputField;
        }
        if (data.reasonField != null) {
            reasonField = data.reasonField;
        }
        if (data.removeButtonAction != null) {
            var split = data.removeButtonAction.split(":");
            var action = split[0];
            if (action.equals("Click")){
                var index = Integer.parseInt(split[1]);
                this.requestingConfirmation = index;
            }
            if (action.equals("Delete")){
                var uuid = UUID.fromString(split[1]);
                AdminUI.getInstance().getBanProvider().modify(uuids -> uuids.remove(uuid) != null);
                AdminUI.getInstance().getBanProvider().syncSave();
                player.sendMessage(Message.translation("server.modules.unban.success").param("name", uuid.toString()));
                this.requestingConfirmation = -1;
            }
            UICommandBuilder commandBuilder = new UICommandBuilder();
            UIEventBuilder eventBuilder = new UIEventBuilder();
            this.buildList(ref, commandBuilder, eventBuilder, store);
            this.sendUpdate(commandBuilder, eventBuilder, false);
            return;
        }
        if (data.searchQuery != null) {
            this.searchQuery = data.searchQuery.trim().toLowerCase();
            UICommandBuilder commandBuilder = new UICommandBuilder();
            UIEventBuilder eventBuilder = new UIEventBuilder();
            this.buildList(ref, commandBuilder, eventBuilder, store);
            this.sendUpdate(commandBuilder, eventBuilder, false);
        }
    }

    private void attemptToKickPlayerIfPresent(Ban ban, PlayerRef context, String username, String reason){
        PlayerRef player = Universe.get().getPlayer(ban.getTarget());
        if (player != null) {
            var disconnectReason = "Failed to get disconnect reason.";
            try {
                disconnectReason = ban.getDisconnectReason(ban.getTarget()).get().orElse("Failed to get disconnect reason.");
            } catch (InterruptedException | ExecutionException e) {
                context.sendMessage(Message.translation("server.modules.ban.failedDisconnectReason").param("name", username));
                e.printStackTrace();
            }
            player.getPacketHandler().disconnect(disconnectReason);
            context.sendMessage(Message.translation("server.modules.ban.bannedWithReason").param("name", username).param("reason", reason));
        }
    }

    private void buildList(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull ComponentAccessor<EntityStore> componentAccessor) {
        HashMap<Ban, String> itemList = new HashMap<>();

        HashMap<UUID, Ban> bans = new HashMap<>();
        AdminUI.getInstance().getBanProvider().modify(uuidBanMap -> {
            bans.putAll(uuidBanMap);
            return false;
        });
        for (Ban ban : bans.values()) {
            var tracker = AdminUI.getInstance().getPlayerTracker().getPlayer(ban.getTarget());
            itemList.put(ban, tracker == null ? "Unknown" : tracker.name());
        }

        Player playerComponent = componentAccessor.getComponent(ref, Player.getComponentType());

        assert playerComponent != null;

        if (this.searchQuery.isEmpty()) {
            visibleItems.clear();
            visibleItems.putAll(itemList);
        } else {
            visibleItems.clear();
            for (Map.Entry<Ban, String> entry : itemList.entrySet()) {
                if (entry.getValue().toLowerCase().contains(this.searchQuery.toLowerCase())) {
                    visibleItems.put(entry.getKey(), entry.getValue());
                    continue;
                }
                if (entry.getKey().getReason().orElse("").toLowerCase().contains(this.searchQuery.toLowerCase())) {
                    visibleItems.put(entry.getKey(), entry.getValue());
                    continue;
                }
                var playerTracker = AdminUI.getInstance().getPlayerTracker().getPlayer(entry.getKey().getBy());
                if (playerTracker != null && playerTracker.name().toLowerCase().contains(this.searchQuery.toLowerCase())) {
                    visibleItems.put(entry.getKey(), entry.getValue());
                    continue;
                }
            }
        }
        this.buildButtons(visibleItems, playerComponent, commandBuilder, eventBuilder);
    }

    private void buildButtons(HashMap<Ban, String> items, @Nonnull Player playerComponent, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        uiCommandBuilder.clear("#IndexCards");
        uiCommandBuilder.appendInline("#Main #IndexList", "Group #IndexCards { LayoutMode: Left; }");
        var i = 0;
        for (Map.Entry<Ban, String> name : items.entrySet()) {
            uiCommandBuilder.append("#IndexCards", "Pages/Ban/Buuz135_AdminUI_BanEntry.ui");
            uiCommandBuilder.set("#IndexCards[" + i + "] #MemberName.Text", name.getValue());
            uiCommandBuilder.set("#IndexCards[" + i + "] #MemberUUID.Text", name.getKey().getTarget().toString());
            uiCommandBuilder.set("#IndexCards[" + i + "] #MemberReason.Text",  name.getKey().getReason().orElse("No reason provided"));
            var playerTracker = AdminUI.getInstance().getPlayerTracker().getPlayer(name.getKey().getBy());
            uiCommandBuilder.set("#IndexCards[" + i + "] #MemberBy.Text",  (playerTracker == null ? "Unknown" : playerTracker.name()));

            if (this.requestingConfirmation == i) {
                uiCommandBuilder.set("#IndexCards[" + i + "] #RemoveMemberButton.Text", "Are you sure?");
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IndexCards[" + i + "] #RemoveMemberButton", EventData.of("RemoveButtonAction", "Delete:" + name.getKey().getTarget().toString()), false);
                eventBuilder.addEventBinding(CustomUIEventBindingType.MouseExited, "#IndexCards[" + i + "] #RemoveMemberButton", EventData.of("RemoveButtonAction", "Click:-1"), false);
            } else {
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IndexCards[" + i + "] #RemoveMemberButton", EventData.of("RemoveButtonAction", "Click:" + i), false);
            }
            ++i;
        }
    }

    public static class SearchGuiData {
        static final String KEY_BUTTON = "Button";
        static final String KEY_REMOVE_BUTTON_ACTION = "RemoveButtonAction";
        static final String KEY_SEARCH_QUERY = "@SearchQuery";
        static final String KEY_INPUT_FIELD = "@InputField";
        static final String KEY_REASON_FIELD = "@ReasonField";
        static final String KEY_NAVBAR = "NavBar";

        public static final BuilderCodec<SearchGuiData> CODEC = BuilderCodec.<SearchGuiData>builder(SearchGuiData.class, SearchGuiData::new)
                .addField(new KeyedCodec<>(KEY_SEARCH_QUERY, Codec.STRING), (searchGuiData, s) -> searchGuiData.searchQuery = s, searchGuiData -> searchGuiData.searchQuery)
                .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (searchGuiData, s) -> searchGuiData.button = s, searchGuiData -> searchGuiData.button)
                .addField(new KeyedCodec<>(KEY_REMOVE_BUTTON_ACTION, Codec.STRING), (searchGuiData, s) -> searchGuiData.removeButtonAction = s, searchGuiData -> searchGuiData.removeButtonAction)
                .addField(new KeyedCodec<>(KEY_INPUT_FIELD, Codec.STRING), (searchGuiData, s) -> searchGuiData.inputField = s, searchGuiData -> searchGuiData.inputField)
                .addField(new KeyedCodec<>(KEY_REASON_FIELD, Codec.STRING), (searchGuiData, s) -> searchGuiData.reasonField = s, searchGuiData -> searchGuiData.reasonField)
                .addField(new KeyedCodec<>(KEY_NAVBAR, Codec.STRING), (searchGuiData, s) -> searchGuiData.navbar = s, searchGuiData -> searchGuiData.navbar)
                .build();

        private String button;
        private String searchQuery;
        private String removeButtonAction;
        private String inputField;
        private String reasonField;
        private String navbar;

    }

}
