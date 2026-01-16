package com.buuz135.adminui.gui;


import com.buuz135.adminui.AdminUI;
import com.hypixel.hytale.builtin.adventure.teleporter.TeleporterPlugin;
import com.hypixel.hytale.builtin.teleport.TeleportPlugin;
import com.hypixel.hytale.builtin.teleport.Warp;
import com.hypixel.hytale.builtin.teleport.components.TeleportHistory;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class WarpGui extends InteractiveCustomUIPage<WarpGui.SearchGuiData> {

    private String searchQuery = "";
    private HashMap<String, Warp> visibleItems;
    private int requestingConfirmation;
    private String inputField;

    public WarpGui(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, SearchGuiData.CODEC);
        this.searchQuery = "";
        this.requestingConfirmation = -1;
        this.visibleItems = new LinkedHashMap<>();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/Warp/Buuz135_AdminUI_WarpPage.ui");
        NavBarHelper.setupBar(ref, uiCommandBuilder, uiEventBuilder, store);
        uiCommandBuilder.set("#SearchInput.Value", this.searchQuery);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput", EventData.of("@SearchQuery", "#SearchInput.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of("Button", "BackButton"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NewWarpField", EventData.of("@InputField", "#NewWarpField.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#AddWarpButton", EventData.of("Button", "AddWarpButton"), false);
        this.buildList(ref, uiCommandBuilder, uiEventBuilder, store);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull SearchGuiData data) {
        super.handleDataEvent(ref, store, data);
        if (NavBarHelper.handleData(ref, store, data.navbar, () -> {})) {
            return;
        }
        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        var player = store.getComponent(ref, Player.getComponentType());
        if (data.button != null) {
            if (data.button.equals("BackButton")) {
                player.getPageManager().openCustomPage(ref, store, new AdminIndexGui(playerRef, CustomPageLifetime.CanDismiss));
                return;
            }
            if (data.button.equals("AddWarpButton")){
                TransformComponent transformComponent = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());

                assert transformComponent != null;

                HeadRotation headRotationComponent = (HeadRotation)store.getComponent(ref, HeadRotation.getComponentType());

                assert headRotationComponent != null;

                Warp newWarp = new Warp(transformComponent.getTransform(), inputField.toLowerCase(), player.getWorld(), playerRef.getUsername(), Instant.now());
                TeleportPlugin.get().getWarps().put(newWarp.getId(), newWarp);
                TeleportPlugin.get().saveWarps();
                UICommandBuilder commandBuilder = new UICommandBuilder();
                UIEventBuilder eventBuilder = new UIEventBuilder();
                this.buildList(ref, commandBuilder, eventBuilder, store);
                this.sendUpdate(commandBuilder, eventBuilder, false);
                return;
            }
            if (data.button.startsWith("Go")) {
                var split = data.button.split(":");
                var id = split[1];
                var warp = TeleportPlugin.get().getWarps().get(id);
                if (warp == null){
                    return;
                }
                TransformComponent transformComponent = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());
                assert transformComponent != null;
                HeadRotation headRotationComponent = (HeadRotation)store.getComponent(ref, HeadRotation.getComponentType());
                assert headRotationComponent != null;
                Vector3d playerPosition = transformComponent.getPosition();
                Vector3f playerHeadRotation = headRotationComponent.getRotation();
                store.ensureAndGetComponent(ref, TeleportHistory.getComponentType()).append(player.getWorld(), playerPosition.clone(), playerHeadRotation.clone(), "Warp '" + warp + "'");
                store.addComponent(ref, Teleport.getComponentType(), warp.toTeleport());
                playerRef.sendMessage(Message.translation("commands.teleport.warp.warpedTo").param("name", warp.getId()));
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
                TeleportPlugin.get().getWarps().remove(split[1]);
                TeleportPlugin.get().saveWarps();
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
        HashMap<String, Warp>  itemList = new HashMap<>();
        itemList.putAll(TeleportPlugin.get().getWarps());


        Player playerComponent = componentAccessor.getComponent(ref, Player.getComponentType());

        assert playerComponent != null;

        if (this.searchQuery.isEmpty()) {
            visibleItems.clear();
            visibleItems.putAll(itemList);
        } else {
            visibleItems.clear();
            for (Map.Entry<String, Warp> entry : itemList.entrySet()) {
                if (entry.getValue().getId().toLowerCase().contains(this.searchQuery.toLowerCase())) {
                    visibleItems.put(entry.getKey(), entry.getValue());
                }
            }
        }
        this.buildButtons(visibleItems, playerComponent, commandBuilder, eventBuilder);
    }

    private void buildButtons(HashMap<String, Warp> items, @Nonnull Player playerComponent, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        uiCommandBuilder.clear("#IndexCards");
        uiCommandBuilder.appendInline("#Main #IndexList", "Group #IndexCards { LayoutMode: Left; }");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());
        var i = 0;
        for (Map.Entry<String, Warp> warpEntry : items.entrySet()) {
            uiCommandBuilder.append("#IndexCards", "Pages/Warp/Buuz135_AdminUI_WarpEntry.ui");
            uiCommandBuilder.set("#IndexCards[" + i + "] #WarpName.Text", warpEntry.getValue().getId());
            uiCommandBuilder.set("#IndexCards[" + i + "] #WarpBy.Text", "By " + warpEntry.getValue().getCreator());
            uiCommandBuilder.set("#IndexCards[" + i + "] #WarpWhen.Text", formatter.format(warpEntry.getValue().getCreationDate()));

            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IndexCards[" + i + "] #GoButton", EventData.of("Button", "Go:" + warpEntry.getValue().getId()), false);

            uiCommandBuilder.set("#IndexCards[" + i + "] #WorldValue.Text", warpEntry.getValue().getWorld());
            uiCommandBuilder.set("#IndexCards[" + i + "] #XValue.Text", (int) warpEntry.getValue().getTransform().getPosition().getX() +"");
            uiCommandBuilder.set("#IndexCards[" + i + "] #YValue.Text", (int) warpEntry.getValue().getTransform().getPosition().getY() +"");
            uiCommandBuilder.set("#IndexCards[" + i + "] #ZValue.Text", (int) warpEntry.getValue().getTransform().getPosition().getZ() +"");

            if (this.requestingConfirmation == i) {
                uiCommandBuilder.set("#IndexCards[" + i + "] #RemoveWarpButton.Text", "Are you sure?");
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IndexCards[" + i + "] #RemoveWarpButton", EventData.of("RemoveButtonAction", "Delete:" + warpEntry.getValue().getId()), false);
                eventBuilder.addEventBinding(CustomUIEventBindingType.MouseExited, "#IndexCards[" + i + "] #RemoveWarpButton", EventData.of("RemoveButtonAction", "Click:-1"), false);
            } else {
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IndexCards[" + i + "] #RemoveWarpButton", EventData.of("RemoveButtonAction", "Click:" + i), false);
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
