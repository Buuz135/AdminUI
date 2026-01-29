package com.buuz135.adminui.gui;


import com.buuz135.adminui.AdminUI;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.common.util.FormatUtil;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.connection.PongType;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerWindow;
import com.hypixel.hytale.server.core.entity.entities.player.windows.Window;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.DelegateItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.filter.FilterType;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.modules.accesscontrol.ban.Ban;
import com.hypixel.hytale.server.core.modules.accesscontrol.ban.InfiniteBan;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PositionDataComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.*;

import java.util.stream.Collectors;

public class PlayerGui extends InteractiveCustomUIPage<PlayerGui.SearchGuiData> {

    private String searchQuery = "";
    private List<String> visibleItems;
    private List<String> expandedItems;

    public PlayerGui(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, SearchGuiData.CODEC);
        this.searchQuery = "";
        this.visibleItems = new ArrayList<>();
        this.expandedItems = new ArrayList<>();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/Player/Buuz135_AdminUI_PlayerPage.ui");
        NavBarHelper.setupBar(ref, uiCommandBuilder, uiEventBuilder, store);
        uiCommandBuilder.set("#SearchInput.Value", this.searchQuery);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput", EventData.of("@SearchQuery", "#SearchInput.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of("Button", "BackButton"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#KillAllButton", EventData.of("Button", "KillAllButton"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TeleportAllButton", EventData.of("Button", "TeleportAllButton"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#KickAllButton", EventData.of("Button", "KickAllButton"), false);
        this.buildList(ref, uiCommandBuilder, uiEventBuilder, store);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> storePrivate, @Nonnull SearchGuiData data) {
        super.handleDataEvent(ref, storePrivate, data);
        var playerRef = storePrivate.getComponent(ref, PlayerRef.getComponentType());
        var player = storePrivate.getComponent(ref, Player.getComponentType());
        if (NavBarHelper.handleData(ref, storePrivate, data.navbar, () -> {})) {
            return;
        }
        if (data.button != null) {
            if (data.button.equals("BackButton")) {
                player.getPageManager().openCustomPage(ref, storePrivate, new AdminIndexGui(playerRef, CustomPageLifetime.CanDismiss));
                return;
            }
            if (data.button.equals("Heal")) {
                heal(UUID.fromString(data.uuid));
            }
            if (data.button.equals("Kill")) {
                kill(UUID.fromString(data.uuid));
            }
            if (data.button.equals("Inventory")) {
                inventory(storePrivate, UUID.fromString(data.uuid));
            }
            if (data.button.equals("Kick")) {
                kick(UUID.fromString(data.uuid));
            }
            if (data.button.equals("Ban")) {
                ban(storePrivate, UUID.fromString(data.uuid));
            }
            if (data.button.equals("GamemodeDropdown")) {
                var uuid = UUID.fromString(data.uuid);
                var gamemode = GameMode.valueOf(data.dropdownValue);
                var otherReference = Universe.get().getPlayer(uuid).getReference();
                Player.setGameMode(ref, gamemode, ref.getStore());
            }
            if (data.button.equals("ModelDropdown")) {
                var uuid = UUID.fromString(data.uuid);
                var model = AdminUI.MODELS.get(data.dropdownValue);
                var otherReference = Universe.get().getPlayer(uuid).getReference();
                otherReference.getStore().putComponent(otherReference, ModelComponent.getComponentType(), new ModelComponent(Model.createScaledModel(model, model.generateRandomScale())));
                otherReference.getStore().getComponent(otherReference, PlayerSkinComponent.getComponentType()).consumeNetworkOutdated();
            }
            if (data.button.equals("ResetModel")) {
                var uuid = UUID.fromString(data.uuid);
                var otherReference = Universe.get().getPlayer(uuid).getReference();
                PlayerSkinComponent playerSkinComponent = otherReference.getStore().getComponent(otherReference, PlayerSkinComponent.getComponentType());
                CosmeticsModule cosmeticsModule = CosmeticsModule.get();
                Model newModel = cosmeticsModule.createModel(playerSkinComponent.getPlayerSkin());
                otherReference.getStore().putComponent(otherReference, ModelComponent.getComponentType(), new ModelComponent(newModel));
                playerSkinComponent.setNetworkOutdated();
            }
            if (data.button.equals("TeleportTo")){
                var uuid = UUID.fromString(data.uuid);
                var otherReference = Universe.get().getPlayer(uuid).getReference();
                var positionTo = otherReference.getStore().getComponent(otherReference, TransformComponent.getComponentType());
                teleport(storePrivate.getComponent(playerRef.getReference(), UUIDComponent.getComponentType()).getUuid(), new Teleport(otherReference.getStore().getComponent(otherReference, Player.getComponentType()).getWorld(), positionTo.getPosition(), positionTo.getRotation()));
            }
            if (data.button.equals("TeleportHere")){
                var uuid = UUID.fromString(data.uuid);
                var positionHere = storePrivate.getComponent(playerRef.getReference(), TransformComponent.getComponentType());
                teleport(uuid, new Teleport(player.getWorld(), positionHere.getPosition(), positionHere.getRotation()));
            }
            if (data.button.equals("KillAllButton")){
                for (PlayerRef player1 : Universe.get().getPlayers()) {
                    if (player1.getUuid().equals(playerRef.getUuid())) continue;
                    kill(player1.getUuid());
                }
            }
            if (data.button.equals("TeleportAllButton")){
                for (PlayerRef player1 : Universe.get().getPlayers()) {
                    if (player1.getUuid().equals(playerRef.getUuid())) continue;
                    var positionHere = storePrivate.getComponent(ref, TransformComponent.getComponentType());
                    teleport(player1.getUuid(), new Teleport(player.getWorld(), positionHere.getPosition(), positionHere.getRotation()));
                }
            }
            if (data.button.equals("KickAllButton")){
                for (PlayerRef player1 : Universe.get().getPlayers()) {
                    if (player1.getUuid().equals(playerRef.getUuid())) continue;
                    kick(player1.getUuid());
                }
            }
            if (data.button.equals("ToggleExpanded")){
                if (expandedItems.contains(data.uuid)) {
                    expandedItems.remove(data.uuid);
                } else {
                    expandedItems.add(data.uuid);
                }
            }
        }

        if (data.searchQuery != null) {
            this.searchQuery = data.searchQuery.trim().toLowerCase();
        }
        UICommandBuilder commandBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();
        this.buildList(ref, commandBuilder, eventBuilder, storePrivate);
        this.sendUpdate(commandBuilder, eventBuilder, false);
    }

    private void buildList(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder,  @Nonnull Store<EntityStore> store) {
        var players = Universe.get().getPlayers().stream().map(PlayerRef::getUsername).toList();

        if (this.searchQuery.isEmpty()) {
            visibleItems.clear();
            visibleItems.addAll(players);
        } else {
            visibleItems.clear();
            for (String entry : players) {
                if (entry.toLowerCase().contains(this.searchQuery.toLowerCase())) {
                    visibleItems.add(entry);
                    continue;
                }

            }
        }
        this.buildButtons(visibleItems, commandBuilder, eventBuilder);
    }

    @Override
    protected void close() {
        super.close();
    }

    private void buildButtons(List<String> items,  @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        uiCommandBuilder.clear("#IndexCards");
        uiCommandBuilder.appendInline("#Main #IndexList", "Group #IndexCards { LayoutMode: Left; }");
        var gamemodes = Arrays.stream(GameMode.values()).map(mode -> new DropdownEntryInfo(LocalizableString.fromString(mode.name()), mode.name())).collect(Collectors.toList());
        var models = AdminUI.MODELS.keySet().stream().map(model -> new DropdownEntryInfo(LocalizableString.fromString(model), model)).toList();
        var i = 0;
        for (String name : items) {
            var playerRef = Universe.get().getPlayer(name, NameMatching.EXACT);
            if (playerRef == null || !playerRef.isValid() || playerRef.getReference() == null) continue;
            var store = playerRef.getReference().getStore();
            var player = store.getComponent(playerRef.getReference(), Player.getComponentType());
            var entityStatMap = store.getComponent(playerRef.getReference(), EntityStatsModule.get().getEntityStatMapComponentType());
            var health = EntityStatType.getAssetMap().getIndex("Health");

            var uuid = playerRef.getUuid();
            PermissionsModule perms = PermissionsModule.get();
            var position = store.getComponent(playerRef.getReference(), TransformComponent.getComponentType());


            uiCommandBuilder.append("#IndexCards", "Pages/Player/Buuz135_AdminUI_PlayerEntry.ui");
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IndexCards[" + i + "]", EventData.of("Button", "ToggleExpanded").append("UUID", name), false);
            if (this.expandedItems.contains(name)) {
                uiCommandBuilder.set("#IndexCards[" + i + "] #ExtendedInfo.Visible", true);
                uiCommandBuilder.set("#IndexCards[" + i + "] #ExpandedIcon.Visible", false);
                uiCommandBuilder.set("#IndexCards[" + i + "] #ExpandedIconDown.Visible", true);
            }

            uiCommandBuilder.set("#IndexCards[" + i + "] #MemberName.Text", name);
            uiCommandBuilder.set("#IndexCards[" + i + "] #MemberUUID.Text", uuid.toString());
            uiCommandBuilder.set("#IndexCards[" + i + "] #Ping.Text", "Ping: "+ FormatUtil.simpleTimeUnitFormat((long) playerRef.getPacketHandler().getPingInfo(PongType.Raw).getPingMetricSet().getAverage(0), PacketHandler.PingInfo.TIME_UNIT, 0));
            uiCommandBuilder.set("#IndexCards[" + i + "] #IsOp.Visible", perms.getGroupsForUser(uuid).contains("OP"));
            uiCommandBuilder.set("#IndexCards[" + i + "] #IsWhitelisted.Visible", AdminUI.getInstance().getWhitelistProvider().getList().contains(uuid));
            uiCommandBuilder.set("#IndexCards[" + i + "] #HealthValue.Text", ((int)entityStatMap.get(health).asPercentage() * 100) + "%" );
            uiCommandBuilder.set("#IndexCards[" + i + "] #WorldValue.Text", player != null && player.getWorld() != null ? player.getWorld().getName() : "Unknown");
            uiCommandBuilder.set("#IndexCards[" + i + "] #XValue.Text", (int) position.getPosition().getX() +"");
            uiCommandBuilder.set("#IndexCards[" + i + "] #YValue.Text", (int) position.getPosition().getY() +"");
            uiCommandBuilder.set("#IndexCards[" + i + "] #ZValue.Text", (int) position.getPosition().getZ() +"");
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IndexCards[" + i + "] #TeleportToButton", EventData.of("Button", "TeleportTo").append("UUID", uuid.toString()), false);


            //Gamemode Dropdown
            uiCommandBuilder.set("#IndexCards[" + i + "] #GamemodeDropdown.Entries", gamemodes);
            uiCommandBuilder.set("#IndexCards[" + i + "] #GamemodeDropdown.Value", player != null ? player.getGameMode().name() : "Unknown");
            eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#IndexCards[" + i + "] #GamemodeDropdown", EventData
                    .of("Button", "GamemodeDropdown").append("UUID", uuid.toString()).append("@DropdownValue", "#IndexCards[" + i + "] #GamemodeDropdown.Value"), false);

            //Model Dropdown
            uiCommandBuilder.set("#IndexCards[" + i + "] #ModelDropdown.Entries", models);
            uiCommandBuilder.set("#IndexCards[" + i + "] #ModelDropdown.Value", store.getComponent(playerRef.getReference(), ModelComponent.getComponentType()).getModel().getModelAssetId());
            eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#IndexCards[" + i + "] #ModelDropdown", EventData
                    .of("Button", "ModelDropdown").append("UUID", uuid.toString()).append("@DropdownValue", "#IndexCards[" + i + "] #ModelDropdown.Value"), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IndexCards[" + i + "] #ModelResetButton", EventData.of("Button", "ResetModel").append("UUID", uuid.toString()), false);

            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IndexCards[" + i + "] #HealButton", EventData.of("Button", "Heal").append("UUID", uuid.toString()), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IndexCards[" + i + "] #KillButton", EventData.of("Button", "Kill").append("UUID", uuid.toString()), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IndexCards[" + i + "] #InventoryButton", EventData.of("Button", "Inventory").append("UUID", uuid.toString()), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IndexCards[" + i + "] #KickButton", EventData.of("Button", "Kick").append("UUID", uuid.toString()), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IndexCards[" + i + "] #BanButton", EventData.of("Button", "Ban").append("UUID", uuid.toString()), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IndexCards[" + i + "] #TeleportHereButton", EventData.of("Button", "TeleportHere").append("UUID", uuid.toString()), false);
            ++i;
        }
    }

    public void heal(UUID uuid){
        var entity = Universe.get().getPlayer(uuid);
        if (entity == null || !entity.isValid()) return;
        var store = entity.getReference().getStore();
        var entityStatMap = store.getComponent(entity.getReference(), EntityStatsModule.get().getEntityStatMapComponentType());
        var health = EntityStatType.getAssetMap().getIndex("Health");
        var stamina = EntityStatType.getAssetMap().getIndex("Stamina");
        EntityStatValue healthStatValue = entityStatMap.get(health);
        entityStatMap.setStatValue(health, healthStatValue.getMax());
        EntityStatValue staminaStatValue = entityStatMap.get(stamina);
        entityStatMap.setStatValue(stamina, staminaStatValue.getMax());
    }

    public void kill(UUID uuid){
        var entity = Universe.get().getPlayer(uuid);
        if (entity == null || !entity.isValid()) return;
        var store = entity.getReference().getStore();
        Damage.Source damageSource = new Damage.EnvironmentSource("ADMIN");
        DeathComponent.tryAddComponent(store, entity.getReference(), new Damage(damageSource, DamageCause.COMMAND, (float)Integer.MAX_VALUE));
    }

    public void inventory(Store<EntityStore> ownerStore, UUID uuid){
        var entity = Universe.get().getPlayer(uuid);
        if (entity == null || !entity.isValid()) return;
        var store = entity.getReference().getStore();
        var ref = entity.getReference();
        var targetPlayerComponent = store.getComponent(ref, Player.getComponentType());
        CombinedItemContainer targetInventory = targetPlayerComponent.getInventory().getCombinedHotbarFirst();
        ItemContainer targetItemContainer = targetInventory;
        var owner = ownerStore.getComponent(this.playerRef.getReference(), Player.getComponentType());
        owner.getPageManager().setPageWithWindows(ref, ownerStore, Page.Bench, true, new Window[]{new ContainerWindow(targetItemContainer)});
    }

    public void kick(UUID uuid){
        var entity = Universe.get().getPlayer(uuid);
        if (entity == null || !entity.isValid()) return;
        var store = entity.getReference().getStore();
        var ref = entity.getReference();
        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        playerRef.getPacketHandler().disconnect("You were kicked by an admin.");
    }

    public void teleport(UUID uuid, Teleport teleport){
        var entity = Universe.get().getPlayer(uuid);
        if (entity == null || !entity.isValid()) return;
        var store = entity.getReference().getStore();
        var ref = entity.getReference();
        store.putComponent(ref, Teleport.getComponentType(), teleport);
    }

    public void ban(Store<EntityStore> ownerStore, UUID uuid){
        var entity = Universe.get().getPlayer(uuid);
        if (entity == null || !entity.isValid()) return;
        var store = entity.getReference().getStore();
        var ref = entity.getReference();

        var ban = new InfiniteBan(uuid, ownerStore.getComponent(this.playerRef.getReference(), UUIDComponent.getComponentType()).getUuid(), Instant.now(), "You were banned by an admin.");
        if (AdminUI.getInstance().getBanProvider().modify(uuids -> {uuids.put(uuid, ban);return true;})) {
            var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            playerRef.getPacketHandler().disconnect("You were banned by an admin.");
        }
    }

    public static class SearchGuiData {
        static final String KEY_BUTTON = "Button";
        static final String KEY_NAVBAR = "NavBar";
        static final String KEY_UUID = "UUID";
        static final String KEY_SEARCH_QUERY = "@SearchQuery";
        static final String KEY_DROPDOWN_VALUE_QUERY = "@DropdownValue";

        public static final BuilderCodec<SearchGuiData> CODEC = BuilderCodec.<SearchGuiData>builder(SearchGuiData.class, SearchGuiData::new)
                .addField(new KeyedCodec<>(KEY_SEARCH_QUERY, Codec.STRING), (searchGuiData, s) -> searchGuiData.searchQuery = s, searchGuiData -> searchGuiData.searchQuery)
                .addField(new KeyedCodec<>(KEY_UUID, Codec.STRING), (searchGuiData, s) -> searchGuiData.uuid = s, searchGuiData -> searchGuiData.uuid)
                .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (searchGuiData, s) -> searchGuiData.button = s, searchGuiData -> searchGuiData.button)
                .addField(new KeyedCodec<>(KEY_DROPDOWN_VALUE_QUERY, Codec.STRING), (searchGuiData, s) -> searchGuiData.dropdownValue = s, searchGuiData -> searchGuiData.dropdownValue)
                .addField(new KeyedCodec<>(KEY_NAVBAR, Codec.STRING), (searchGuiData, s) -> searchGuiData.navbar = s, searchGuiData -> searchGuiData.navbar)
                .build();

        private String button;
        private String searchQuery;
        private String uuid;
        private String dropdownValue;
        private String navbar;

    }

}
