package com.buuz135.adminui.gui;


import com.buuz135.adminui.AdminUI;
import com.buuz135.adminui.util.AuthUtil;
import com.hypixel.hytale.Main;
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
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class WhitelistGui extends InteractiveCustomUIPage<WhitelistGui.SearchGuiData> {

    private String searchQuery = "";
    private HashMap<UUID, String> visibleItems;
    private int requestingConfirmation;
    private String inputField;

    public WhitelistGui(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, SearchGuiData.CODEC);
        this.searchQuery = "";
        this.requestingConfirmation = -1;
        this.visibleItems = new LinkedHashMap<>();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/Whitelist/Buuz135_AdminUI_WhitelistPage.ui");
        NavBarHelper.setupBar(ref, uiCommandBuilder, uiEventBuilder, store);
        uiCommandBuilder.set("#SearchInput.Value", this.searchQuery);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput", EventData.of("@SearchQuery", "#SearchInput.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of("Button", "BackButton"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#WhitelistSetting #CheckBox", EventData.of("Button", "WhitelistSetting"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NewWhitelistField", EventData.of("@InputField", "#NewWhitelistField.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#AddToWhitelistButton", EventData.of("Button", "AddMemberButton"), false);
        uiCommandBuilder.set("#WhitelistSetting #CheckBox.Value", AdminUI.getInstance().getWhitelistProvider().isEnabled());
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
            if (data.button.equals("WhitelistSetting")) {
                AdminUI.getInstance().getWhitelistProvider().setEnabled(!AdminUI.getInstance().getWhitelistProvider().isEnabled());
                AdminUI.getInstance().getWhitelistProvider().syncSave();
                if (AdminUI.getInstance().getWhitelistProvider().isEnabled()){
                    player.sendMessage(Message.translation("server.modules.whitelist.enabled"));
                } else {
                    player.sendMessage(Message.translation("server.modules.whitelist.disabled"));
                }
                return;
            }
            if (data.button.equals("AddMemberButton")){
                ProfileServiceClient.PublicGameProfile profile = AuthUtil.getProfile(inputField);
                if (profile == null){
                    return;
                }
                if (AdminUI.getInstance().getWhitelistProvider().modify(uuids -> uuids.add(profile.getUuid()))) {
                    AdminUI.getInstance().getWhitelistProvider().syncSave();
                    AdminUI.getInstance().getPlayerTracker().addPlayer(profile.getUsername(), profile.getUuid());
                    player.sendMessage(Message.translation("server.modules.whitelist.addSuccess").param("name", this.inputField));
                    UICommandBuilder commandBuilder = new UICommandBuilder();
                    UIEventBuilder eventBuilder = new UIEventBuilder();
                    this.buildList(ref, commandBuilder, eventBuilder, store);
                    this.sendUpdate(commandBuilder, eventBuilder, false);
                    return;
                } else {
                    player.sendMessage(Message.translation("server.modules.whitelist.alreadyWhitelisted").param("name", inputField));
                }

                return;
            }
        }
        if (data.inputField != null) {
            inputField = data.inputField;
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
                AdminUI.getInstance().getWhitelistProvider().modify(uuids -> uuids.remove(uuid));
                AdminUI.getInstance().getWhitelistProvider().syncSave();
                player.sendMessage(Message.translation("server.modules.whitelist.removalSuccess").param("uuid", uuid.toString()));
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

    private void buildList(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull ComponentAccessor<EntityStore> componentAccessor) {
        HashMap<UUID, String> itemList = new HashMap<>();

        for (UUID uuid : AdminUI.getInstance().getWhitelistProvider().getList()) {
            var tracker = AdminUI.getInstance().getPlayerTracker().getPlayer(uuid);
            itemList.put(uuid, tracker == null ? "Unknown" :tracker.name());
        }

        Player playerComponent = componentAccessor.getComponent(ref, Player.getComponentType());

        assert playerComponent != null;

        if (this.searchQuery.isEmpty()) {
            visibleItems.clear();
            visibleItems.putAll(itemList);
        } else {
            visibleItems.clear();
            for (Map.Entry<UUID, String> entry : itemList.entrySet()) {
                if (entry.getValue().toLowerCase().contains(this.searchQuery.toLowerCase())) {
                    visibleItems.put(entry.getKey(), entry.getValue());
                }
            }
        }
        this.buildButtons(visibleItems, playerComponent, commandBuilder, eventBuilder);
    }

    private void buildButtons(HashMap<UUID, String> items, @Nonnull Player playerComponent, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        uiCommandBuilder.clear("#IndexCards");
        uiCommandBuilder.appendInline("#Main #IndexList", "Group #IndexCards { LayoutMode: Left; }");
        var i = 0;
        for (Map.Entry<UUID, String> name : items.entrySet()) {
            uiCommandBuilder.append("#IndexCards", "Pages/Whitelist/Buuz135_AdminUI_WhitelistEntry.ui");
            uiCommandBuilder.set("#IndexCards[" + i + "] #MemberName.Text", name.getValue());
            uiCommandBuilder.set("#IndexCards[" + i + "] #MemberUUID.Text", name.getKey().toString());

            if (this.requestingConfirmation == i) {
                uiCommandBuilder.set("#IndexCards[" + i + "] #RemoveMemberButton.Text", "Are you sure?");
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IndexCards[" + i + "] #RemoveMemberButton", EventData.of("RemoveButtonAction", "Delete:" + name.getKey().toString()), false);
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
        static final String KEY_NAVBAR = "NavBar";

        public static final BuilderCodec<SearchGuiData> CODEC = BuilderCodec.<SearchGuiData>builder(SearchGuiData.class, SearchGuiData::new)
                .addField(new KeyedCodec<>(KEY_SEARCH_QUERY, Codec.STRING), (searchGuiData, s) -> searchGuiData.searchQuery = s, searchGuiData -> searchGuiData.searchQuery)
                .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (searchGuiData, s) -> searchGuiData.button = s, searchGuiData -> searchGuiData.button)
                .addField(new KeyedCodec<>(KEY_REMOVE_BUTTON_ACTION, Codec.STRING), (searchGuiData, s) -> searchGuiData.removeButtonAction = s, searchGuiData -> searchGuiData.removeButtonAction)
                .addField(new KeyedCodec<>(KEY_INPUT_FIELD, Codec.STRING), (searchGuiData, s) -> searchGuiData.inputField = s, searchGuiData -> searchGuiData.inputField)
                .addField(new KeyedCodec<>(KEY_NAVBAR, Codec.STRING), (searchGuiData, s) -> searchGuiData.navbar = s, searchGuiData -> searchGuiData.navbar)
                .build();

        private String button;
        private String searchQuery;
        private String removeButtonAction;
        private String inputField;
        private String navbar;

    }

}
