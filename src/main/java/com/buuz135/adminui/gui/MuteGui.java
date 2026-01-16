package com.buuz135.adminui.gui;


import com.buuz135.adminui.AdminUI;
import com.buuz135.adminui.util.AuthUtil;
import com.buuz135.adminui.util.DurationParser;
import com.buuz135.adminui.util.MuteTracker;
import com.google.protobuf.Duration;
import com.google.protobuf.DurationOrBuilder;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.common.util.FormatUtil;
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
import java.time.Period;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MuteGui extends InteractiveCustomUIPage<MuteGui.SearchGuiData> {

    private String searchQuery = "";
    private HashMap<MuteTracker.Mute, String> visibleItems;
    private int requestingConfirmation;
    private String inputField;
    private String reasonField;
    private String durationField;

    public MuteGui(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, SearchGuiData.CODEC);
        this.searchQuery = "";
        this.requestingConfirmation = -1;
        this.visibleItems = new LinkedHashMap<>();
        this.inputField = "";
        this.reasonField = "";
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/Mute/Buuz135_AdminUI_MutePage.ui");
        NavBarHelper.setupBar(ref, uiCommandBuilder, uiEventBuilder, store);
        uiCommandBuilder.set("#SearchInput.Value", this.searchQuery);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput", EventData.of("@SearchQuery", "#SearchInput.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of("Button", "BackButton"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#UsernameField", EventData.of("@InputField", "#UsernameField.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ReasonField", EventData.of("@ReasonField", "#ReasonField.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#DurationField", EventData.of("@DurationField", "#DurationField.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#AddToMuteButton", EventData.of("Button", "AddToMuteButton"), false);
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

            if (data.button.equals("AddToMuteButton")){
                ProfileServiceClient.PublicGameProfile profile = AuthUtil.getProfile(inputField);
                if (profile == null){
                    return;
                }
                var time = DurationParser.parse(durationField);
                var instant = Instant.now().plusMillis(time);
                AdminUI.getInstance().getMuteTracker().addMute(new MuteTracker.Mute(profile.getUuid(), playerRef.getUuid(), instant, reasonField));
                UICommandBuilder commandBuilder = new UICommandBuilder();
                UIEventBuilder eventBuilder = new UIEventBuilder();
                this.buildList(ref, commandBuilder, eventBuilder, store);
                this.sendUpdate(commandBuilder, eventBuilder, false);
                return;
            }
        }
        if (data.inputField != null) {
            inputField = data.inputField;
        }
        if (data.reasonField != null) {
            reasonField = data.reasonField;
        }
        if (data.durationField != null) {
            durationField = data.durationField;
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
                AdminUI.getInstance().getMuteTracker().getMutes().removeIf(mute -> mute.target().equals(uuid));
                AdminUI.getInstance().getMuteTracker().syncSave();
                player.sendMessage(Message.raw("Unmuted player " + uuid));
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
        HashMap<MuteTracker.Mute, String> itemList = new HashMap<>();


        for (MuteTracker.Mute mute : AdminUI.getInstance().getMuteTracker().getMutes()) {
            var tracker = AdminUI.getInstance().getPlayerTracker().getPlayer(mute.target());
            itemList.put(mute, tracker == null ? "Unknown" : tracker.name());
        }

        Player playerComponent = componentAccessor.getComponent(ref, Player.getComponentType());

        assert playerComponent != null;

        if (this.searchQuery.isEmpty()) {
            visibleItems.clear();
            visibleItems.putAll(itemList);
        } else {
            visibleItems.clear();
            for (Map.Entry<MuteTracker.Mute, String> entry : itemList.entrySet()) {
                if (entry.getValue().toLowerCase().contains(this.searchQuery.toLowerCase())) {
                    visibleItems.put(entry.getKey(), entry.getValue());
                    continue;
                }
                if (entry.getKey().reason().toLowerCase().contains(this.searchQuery.toLowerCase())) {
                    visibleItems.put(entry.getKey(), entry.getValue());
                    continue;
                }
                var playerTracker = AdminUI.getInstance().getPlayerTracker().getPlayer(entry.getKey().mutedBy());
                if (playerTracker != null && playerTracker.name().toLowerCase().contains(this.searchQuery.toLowerCase())) {
                    visibleItems.put(entry.getKey(), entry.getValue());
                    continue;
                }
            }
        }
        this.buildButtons(visibleItems, playerComponent, commandBuilder, eventBuilder);
    }

    private void buildButtons(HashMap<MuteTracker.Mute, String> items, @Nonnull Player playerComponent, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        uiCommandBuilder.clear("#IndexCards");
        uiCommandBuilder.appendInline("#Main #IndexList", "Group #IndexCards { LayoutMode: Left; }");
        var i = 0;
        for (Map.Entry<MuteTracker.Mute, String> name : items.entrySet()) {
            uiCommandBuilder.append("#IndexCards", "Pages/Mute/Buuz135_AdminUI_MuteEntry.ui");
            uiCommandBuilder.set("#IndexCards[" + i + "] #MemberName.Text", name.getValue());
            uiCommandBuilder.set("#IndexCards[" + i + "] #MemberUUID.Text", name.getKey().target().toString());
            uiCommandBuilder.set("#IndexCards[" + i + "] #MemberReason.Text", (name.getKey().reason().isEmpty() ? "No reason provided" : name.getKey().reason()));
            var playerTracker = AdminUI.getInstance().getPlayerTracker().getPlayer(name.getKey().mutedBy());
            uiCommandBuilder.set("#IndexCards[" + i + "] #MemberBy.Text", (playerTracker == null ? "Unknown" : playerTracker.name()));
            uiCommandBuilder.set("#IndexCards[" + i + "] #TimeLeft.Text", FormatUtil.timeUnitToString((name.getKey().until().toEpochMilli() - Instant.now().toEpochMilli()) / 1000, TimeUnit.SECONDS));

            if (this.requestingConfirmation == i) {
                uiCommandBuilder.set("#IndexCards[" + i + "] #RemoveMemberButton.Text", "Are you sure?");
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IndexCards[" + i + "] #RemoveMemberButton", EventData.of("RemoveButtonAction", "Delete:" + name.getKey().target().toString()), false);
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
        static final String KEY_DURATION = "@DurationField";

        public static final BuilderCodec<SearchGuiData> CODEC = BuilderCodec.<SearchGuiData>builder(SearchGuiData.class, SearchGuiData::new)
                .addField(new KeyedCodec<>(KEY_SEARCH_QUERY, Codec.STRING), (searchGuiData, s) -> searchGuiData.searchQuery = s, searchGuiData -> searchGuiData.searchQuery)
                .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (searchGuiData, s) -> searchGuiData.button = s, searchGuiData -> searchGuiData.button)
                .addField(new KeyedCodec<>(KEY_REMOVE_BUTTON_ACTION, Codec.STRING), (searchGuiData, s) -> searchGuiData.removeButtonAction = s, searchGuiData -> searchGuiData.removeButtonAction)
                .addField(new KeyedCodec<>(KEY_INPUT_FIELD, Codec.STRING), (searchGuiData, s) -> searchGuiData.inputField = s, searchGuiData -> searchGuiData.inputField)
                .addField(new KeyedCodec<>(KEY_REASON_FIELD, Codec.STRING), (searchGuiData, s) -> searchGuiData.reasonField = s, searchGuiData -> searchGuiData.reasonField)
                .addField(new KeyedCodec<>(KEY_NAVBAR, Codec.STRING), (searchGuiData, s) -> searchGuiData.navbar = s, searchGuiData -> searchGuiData.navbar)
                .addField(new KeyedCodec<>(KEY_DURATION, Codec.STRING), (searchGuiData, s) -> searchGuiData.durationField = s, searchGuiData -> searchGuiData.durationField)
                .build();

        private String button;
        private String searchQuery;
        private String removeButtonAction;
        private String inputField;
        private String reasonField;
        private String navbar;
        private String durationField;

    }

}
