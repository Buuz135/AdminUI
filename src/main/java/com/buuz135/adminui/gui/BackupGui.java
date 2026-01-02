package com.buuz135.adminui.gui;


import com.buuz135.adminui.AdminUI;
import com.buuz135.adminui.util.DurationParser;
import com.buuz135.adminui.util.MuteTracker;
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
import com.hypixel.hytale.server.core.Options;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.AuthUtil;

import javax.annotation.Nonnull;
import java.io.File;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class BackupGui extends InteractiveCustomUIPage<BackupGui.SearchGuiData> {

    private String searchQuery = "";
    private List<File> visibleItems;
    private int requestingConfirmation;
    private boolean isArgumentEnabled;
    private boolean isEnabled;
    private String pathField;
    private int retentionAmount;
    private int backupFrequency;

    public BackupGui(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, SearchGuiData.CODEC);
        this.searchQuery = "";
        this.requestingConfirmation = -1;
        this.visibleItems = new ArrayList<>();
        if (Options.getOptionSet().has(Options.BACKUP)) {
            this.isArgumentEnabled = true;
            this.isEnabled = true;
            this.pathField = Options.getOptionSet().valueOf(Options.BACKUP_DIRECTORY).toString();
            this.retentionAmount = Options.getOptionSet().valueOf(Options.BACKUP_MAX_COUNT);
            this.backupFrequency = Options.getOptionSet().valueOf(Options.BACKUP_FREQUENCY_MINUTES);
        } else {
            this.isArgumentEnabled = false;
            this.isEnabled = AdminUI.getInstance().getBackupConfiguration().isEnabled();
            this.pathField = AdminUI.getInstance().getBackupConfiguration().getFolder();
            this.retentionAmount = AdminUI.getInstance().getBackupConfiguration().getRetentionAmount();
            this.backupFrequency = AdminUI.getInstance().getBackupConfiguration().getBackupFrequency();
        }
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/Backup/Buuz135_AdminUI_BackupPage.ui");
        NavBarHelper.setupBar(ref, uiCommandBuilder, uiEventBuilder, store);
        uiCommandBuilder.set("#SearchInput.Value", this.searchQuery);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput", EventData.of("@SearchQuery", "#SearchInput.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of("Button", "BackButton"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#BackupEnabled #CheckBox", EventData.of("Button", "BackupEnabled"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PathField", EventData.of("@PathField", "#PathField.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#FrequencyField", EventData.of("@FrequencyField", "#FrequencyField.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#RetentionField", EventData.of("@RetentionField", "#RetentionField.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SaveButton", EventData.of("Button", "SaveButton"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CreateButton", EventData.of("Button", "CreateButton"), false);
        uiCommandBuilder.set("#BackupEnabled #CheckBox.Value", this.isEnabled);
        uiCommandBuilder.set("#BackupEnabled #CheckBox.Disabled", this.isArgumentEnabled);
        uiCommandBuilder.set("#PathField.Value", this.pathField);
        uiCommandBuilder.set("#PathField.IsReadOnly", this.isArgumentEnabled);
        uiCommandBuilder.set("#FrequencyField.Value", this.backupFrequency);
        uiCommandBuilder.set("#FrequencyField.IsReadOnly", this.isArgumentEnabled);
        uiCommandBuilder.set("#RetentionField.Value", this.retentionAmount);
        uiCommandBuilder.set("#RetentionField.IsReadOnly", this.isArgumentEnabled);
        uiCommandBuilder.set("#SaveButton.Visible", !this.isArgumentEnabled);
        uiCommandBuilder.set("#CreateButton.Visible", !this.isArgumentEnabled);
        uiCommandBuilder.set("#WarningLabel.Visible", this.isArgumentEnabled);

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

            if (data.button.equals("CreateButton")){
                if (!this.pathField.isEmpty()){
                    Universe.get().runBackup().thenAccept(unused -> {
                        UICommandBuilder commandBuilder = new UICommandBuilder();
                        UIEventBuilder eventBuilder = new UIEventBuilder();
                        this.buildList(ref, commandBuilder, eventBuilder, store);
                        this.sendUpdate(commandBuilder, eventBuilder, false);
                    });
                }
            }

            if (data.button.equals("BackupEnabled")) {
                this.isEnabled = !this.isEnabled;
            }

            if (data.button.equals("SaveButton")){
                AdminUI.getInstance().getBackupConfiguration().setEnabled(this.isEnabled);
                AdminUI.getInstance().getBackupConfiguration().setFolder(this.pathField);
                AdminUI.getInstance().getBackupConfiguration().setRetentionAmount(this.retentionAmount);
                AdminUI.getInstance().getBackupConfiguration().setBackupFrequency(this.backupFrequency);
                AdminUI.getInstance().getBackupConfiguration().syncSave();
                this.searchQuery = data.searchQuery.trim().toLowerCase();
                UICommandBuilder commandBuilder = new UICommandBuilder();
                UIEventBuilder eventBuilder = new UIEventBuilder();
                this.buildList(ref, commandBuilder, eventBuilder, store);
                return;
            }
        }
        if (data.pathField != null) {
            this.pathField = data.pathField;
        }
        if (data.retentionAmount != 0) {
            this.retentionAmount = data.retentionAmount;
        }
        if (data.backupFrequency != 0) {
            this.backupFrequency = data.backupFrequency;
        }
        if (data.removeButtonAction != null) {
            var split = data.removeButtonAction.split(":");
            var action = split[0];
            if (action.equals("Click")){
                var index = Integer.parseInt(split[1]);
                this.requestingConfirmation = index;
            }
            if (action.equals("Delete")){
                var folder = new File(this.pathField);
                if (folder.exists() && folder.isDirectory()){
                    for (String s : folder.list()) {
                        File file = new File(folder, s);
                        if (file.isFile() && file.getName().equals(split[1])) {
                            file.delete();
                        }
                    }
                }
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
        List<File> itemList = new ArrayList<>();
        var folder = new File(this.pathField);
        if (folder.exists() && folder.isDirectory()){
            for (String s : folder.list()) {
                File file = new File(folder, s);
                if (file.isFile() && file.getName().endsWith(".zip")) {
                    itemList.add(file);
                }
            }
        }
        itemList = itemList.reversed();

        Player playerComponent = componentAccessor.getComponent(ref, Player.getComponentType());

        assert playerComponent != null;

        if (this.searchQuery.isEmpty()) {
            visibleItems.clear();
            visibleItems.addAll(itemList);
        } else {
            visibleItems.clear();
            for (File entry : itemList) {
                if (entry.getName().contains(this.searchQuery.toLowerCase())) {
                    visibleItems.add(entry);
                    continue;
                }
            }
        }
        this.buildButtons(visibleItems, playerComponent, commandBuilder, eventBuilder);
    }

    private void buildButtons(List<File> items, @Nonnull Player playerComponent, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        uiCommandBuilder.clear("#IndexCards");
        uiCommandBuilder.appendInline("#Main #IndexList", "Group #IndexCards { LayoutMode: Left; }");
        var i = 0;
        for (File name : items) {
            uiCommandBuilder.append("#IndexCards", "Pages/Backup/Buuz135_AdminUI_BackupEntry.ui");
            uiCommandBuilder.set("#IndexCards[" + i + "] #BackupName.Text", name.getName());
            uiCommandBuilder.set("#IndexCards[" + i + "] #BackupSize.Text", FormatUtil.bytesToString(name.length()));
            uiCommandBuilder.set("#IndexCards[" + i + "] #TimeLeft.Text", FormatUtil.timeUnitToString((Instant.now().toEpochMilli() - name.lastModified()) / 1000, TimeUnit.SECONDS) );

            if (this.requestingConfirmation == i) {
                uiCommandBuilder.set("#IndexCards[" + i + "] #RemoveMemberButton.Text", "Are you sure?");
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IndexCards[" + i + "] #RemoveMemberButton", EventData.of("RemoveButtonAction", "Delete:" + name.getName()), false);
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
        static final String KEY_PATH_FIELD = "@PathField";
        static final String KEY_RETENTION_FIELD = "@RetentionField";
        static final String KEY_FREQUENCY = "@FrequencyField";
        static final String KEY_NAVBAR = "NavBar";

        public static final BuilderCodec<SearchGuiData> CODEC = BuilderCodec.<SearchGuiData>builder(SearchGuiData.class, SearchGuiData::new)
                .addField(new KeyedCodec<>(KEY_SEARCH_QUERY, Codec.STRING), (searchGuiData, s) -> searchGuiData.searchQuery = s, searchGuiData -> searchGuiData.searchQuery)
                .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (searchGuiData, s) -> searchGuiData.button = s, searchGuiData -> searchGuiData.button)
                .addField(new KeyedCodec<>(KEY_REMOVE_BUTTON_ACTION, Codec.STRING), (searchGuiData, s) -> searchGuiData.removeButtonAction = s, searchGuiData -> searchGuiData.removeButtonAction)
                .addField(new KeyedCodec<>(KEY_PATH_FIELD, Codec.STRING), (searchGuiData, s) -> searchGuiData.pathField = s, searchGuiData -> searchGuiData.pathField)
                .addField(new KeyedCodec<>(KEY_RETENTION_FIELD, Codec.INTEGER), (searchGuiData, s) -> searchGuiData.retentionAmount = s, searchGuiData -> searchGuiData.retentionAmount)
                .addField(new KeyedCodec<>(KEY_FREQUENCY, Codec.INTEGER), (searchGuiData, s) -> searchGuiData.backupFrequency = s, searchGuiData -> searchGuiData.backupFrequency)
                .addField(new KeyedCodec<>(KEY_NAVBAR, Codec.STRING), (searchGuiData, s) -> searchGuiData.navbar = s, searchGuiData -> searchGuiData.navbar)

                .build();

        private String button;
        private String searchQuery;
        private String removeButtonAction;
        private String pathField;
        private int retentionAmount;
        private int backupFrequency;
        private String navbar;


    }

}
