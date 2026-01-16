package com.buuz135.adminui.gui;


import com.buuz135.adminui.AdminUI;
import com.buuz135.adminui.AdminUIIndexRegistry;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class AdminStickGui extends InteractiveCustomUIPage<AdminStickGui.SearchGuiData> {


    public AdminStickGui(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, SearchGuiData.CODEC);

    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/AdminStick/Buuz135_AdminUI_AdminStickPage.ui");
        NavBarHelper.setupBar(ref, uiCommandBuilder, uiEventBuilder, store);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of("Button", "BackButton"), false);
        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        var config = AdminUI.getInstance().getAdminStickCustomConfig().getPlayer(playerRef.getUuid());
        var entries = AdminUIIndexRegistry.getInstance().getEntries().stream().map(entry -> new DropdownEntryInfo(LocalizableString.fromString(entry.displayName()), entry.id())).collect(Collectors.toList());
        entries.add(0, new DropdownEntryInfo(LocalizableString.fromString("None"), ""));

        uiCommandBuilder.set("#Ability1Dropdown.Entries", entries);
        uiCommandBuilder.set("#Ability1Dropdown.Value", config.ability1());
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#Ability1Dropdown", EventData
                .of("Button", "Ability1").append("@DropdownValue", "#Ability1Dropdown.Value"), false);

        uiCommandBuilder.set("#Ability2Dropdown.Entries", entries);
        uiCommandBuilder.set("#Ability2Dropdown.Value", config.ability2());
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#Ability2Dropdown", EventData
                .of("Button", "Ability2").append("@DropdownValue", "#Ability2Dropdown.Value"), false);

        uiCommandBuilder.set("#Ability3Dropdown.Entries", entries);
        uiCommandBuilder.set("#Ability3Dropdown.Value", config.ability3());
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#Ability3Dropdown", EventData
                .of("Button", "Ability3").append("@DropdownValue", "#Ability3Dropdown.Value"), false);

        uiCommandBuilder.set("#LeftClickDropdown.Entries", entries);
        uiCommandBuilder.set("#LeftClickDropdown.Value", config.primary());
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LeftClickDropdown", EventData
                .of("Button", "LeftClick").append("@DropdownValue", "#LeftClickDropdown.Value"), false);

        uiCommandBuilder.set("#RightClickDropdown.Entries", entries);
        uiCommandBuilder.set("#RightClickDropdown.Value", config.secondary());
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#RightClickDropdown", EventData
                .of("Button", "RightClick").append("@DropdownValue", "#RightClickDropdown.Value"), false);

        uiCommandBuilder.set("#MiddleClickDropdown.Entries", entries);
        uiCommandBuilder.set("#MiddleClickDropdown.Value", config.pick());
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#MiddleClickDropdown", EventData
                .of("Button", "MiddleClick").append("@DropdownValue", "#MiddleClickDropdown.Value"), false);
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
            var config = AdminUI.getInstance().getAdminStickCustomConfig().getPlayer(playerRef.getUuid());
            if (data.button.equals("Ability1")){
                config.setAbility1(data.dropdownValue);
            }
            if (data.button.equals("Ability2")){
                config.setAbility2(data.dropdownValue);
            }
            if (data.button.equals("Ability3")){
                config.setAbility3(data.dropdownValue);
            }
            if (data.button.equals("LeftClick")){
                config.setPrimary(data.dropdownValue);
            }
            if (data.button.equals("RightClick")){
                config.setSecondary(data.dropdownValue);
            }
            if (data.button.equals("MiddleClick")){
                config.setPick(data.dropdownValue);
            }
            AdminUI.getInstance().getAdminStickCustomConfig().addPlayer(playerRef.getUuid(), config);
        }
    }



    public static class SearchGuiData {
        static final String KEY_BUTTON = "Button";
        static final String KEY_DROPDOWN_VALUE_QUERY = "@DropdownValue";
        static final String KEY_NAVBAR = "NavBar";

        public static final BuilderCodec<SearchGuiData> CODEC = BuilderCodec.<SearchGuiData>builder(SearchGuiData.class, SearchGuiData::new)
                .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (searchGuiData, s) -> searchGuiData.button = s, searchGuiData -> searchGuiData.button)
                .addField(new KeyedCodec<>(KEY_DROPDOWN_VALUE_QUERY, Codec.STRING), (searchGuiData, s) -> searchGuiData.dropdownValue = s, searchGuiData -> searchGuiData.dropdownValue)
                .addField(new KeyedCodec<>(KEY_NAVBAR, Codec.STRING), (searchGuiData, s) -> searchGuiData.navbar = s, searchGuiData -> searchGuiData.navbar)
                .build();

        private String button;
        private String dropdownValue;
        private String navbar;
    }

}
