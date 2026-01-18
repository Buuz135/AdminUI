package com.buuz135.adminui;

import com.buuz135.adminui.command.AdminCommand;
import com.buuz135.adminui.gui.*;
import com.buuz135.adminui.interaction.AdminStickInteraction;
import com.buuz135.adminui.util.*;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.Options;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.modules.accesscontrol.AccessControlModule;
import com.hypixel.hytale.server.core.modules.accesscontrol.provider.HytaleBanProvider;
import com.hypixel.hytale.server.core.modules.accesscontrol.provider.HytaleWhitelistProvider;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import joptsimple.AbstractOptionSpec;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class AdminUI extends JavaPlugin {

    public static Map<String, ModelAsset> MODELS = new HashMap<String, ModelAsset>();

    private static AdminUI INSTANCE;

    public static AdminUI getInstance() {
        return INSTANCE;
    }

    private HytaleWhitelistProvider whitelistProvider;
    private HytaleBanProvider banProvider;
    private PlayerTracker playerTracker;
    private AdminStickCustomConfig adminStickCustomConfig;
    private MuteTracker muteTracker;
    private BackupConfiguration backupConfiguration;

    public AdminUI(@Nonnull JavaPluginInit init) {
        super(init);
        INSTANCE = this;
        this.playerTracker = new PlayerTracker();
        this.adminStickCustomConfig = new AdminStickCustomConfig();
        this.muteTracker = new MuteTracker();
        this.backupConfiguration = new BackupConfiguration();
    }

    @Override
    protected void setup() {
        super.setup();

        var folder = new File("AdminUI");
        if (!folder.exists()) folder.mkdirs();

        //PLAYER TRACKER
        this.playerTracker.syncLoad();
        this.getEventRegistry().registerGlobal(PlayerConnectEvent.class, (event) -> {
            var player = event.getHolder().getComponent(Player.getComponentType());
            var uuid = event.getHolder().getComponent(UUIDComponent.getComponentType()).getUuid();
            this.playerTracker.addPlayer(player.getDisplayName(), uuid);
        });

        //LOADING FILES
        this.adminStickCustomConfig.syncLoad();
        this.muteTracker.syncLoad();
        this.backupConfiguration.syncLoad();

        //ADMIN UI PAGES
        AdminUIIndexRegistry.getInstance().register(new AdminUIIndexRegistry.Entry("whitelist", "Whitelists", PermissionList.WHITELIST_OPEN_UI, WhitelistGui::new, true, "wl", "whitelists"));
        AdminUIIndexRegistry.getInstance().register(new AdminUIIndexRegistry.Entry("mute", "Mutes", PermissionList.MUTE_OPEN_UI,MuteGui::new, true, "m", "mute"));
        AdminUIIndexRegistry.getInstance().register(new AdminUIIndexRegistry.Entry("ban", "Bans", PermissionList.BAN_OPEN_UI, BanGui::new, true, "b", "bans"));
        AdminUIIndexRegistry.getInstance().register(new AdminUIIndexRegistry.Entry("player", "Players", PermissionList.PLAYER_OPEN_UI, PlayerGui::new, true, "p", "players"));
        AdminUIIndexRegistry.getInstance().register(new AdminUIIndexRegistry.Entry("warps", "Warps", PermissionList.WARP_OPEN_UI, WarpGui::new, true, "w", "warps"));
        AdminUIIndexRegistry.getInstance().register(new AdminUIIndexRegistry.Entry("admin-stick", "Admin Stick", PermissionList.ADMIN_STICK_OPEN_UI, AdminStickGui::new, true));
        AdminUIIndexRegistry.getInstance().register(new AdminUIIndexRegistry.Entry("backup", "Server Backups", PermissionList.BACKUP_OPEN_UI, BackupGui::new, true, "bk", "backup"));
        AdminUIIndexRegistry.getInstance().register(new AdminUIIndexRegistry.Entry("server", "Server Stats", PermissionList.STATS_OPEN_UI, StatsGui::new, true, "st", "stats"));

        //PROVIDERS
        this.whitelistProvider = ReflectionUtil.getPublic(HytaleWhitelistProvider.class, AccessControlModule.get(), "whitelistProvider");
        this.banProvider = ReflectionUtil.getPublic(HytaleBanProvider.class, AccessControlModule.get(), "banProvider");
        if (whitelistProvider == null || banProvider == null) throw new RuntimeException("Could not find Hytale Access Control Module");


        this.getEventRegistry().register(LoadedAssetsEvent.class, ModelAsset.class, AdminUI::onModelAssetLoad);

        //COMMANDS
        this.getCommandRegistry().registerCommand(AdminUIIndexRegistry.getInstance().getCommand());

        this.getCodecRegistry(Interaction.CODEC).register("Buuz135_AdminUI_AdminStickInteraction", AdminStickInteraction.class, AdminStickInteraction.CODEC);

        this.getEventRegistry().registerGlobal(PlayerChatEvent.class, (event) -> {
            if (this.muteTracker.isMuted(event.getSender().getUuid())) {
                event.setCancelled(true);
                event.getSender().sendMessage(Message.join(Message.raw("You are muted and cannot chat!").color(Color.RED).bold(true), Message.raw( " Reason: " + this.muteTracker.getPlayer(event.getSender().getUuid()).reason())));
            }
        });

        if (!Options.getOptionSet().has(Options.BACKUP)) {
            if (this.backupConfiguration.isEnabled()) {
                var optionSet = Options.getOptionSet();
                var map = (Map<AbstractOptionSpec<?>, List<String>>) ReflectionUtil.getPublic(Map.class, optionSet, "optionsToArguments");
                map.put((AbstractOptionSpec<?>) Options.BACKUP_MAX_COUNT, List.of(this.backupConfiguration.getRetentionAmount() + ""));
                map.put((AbstractOptionSpec<?>) Options.BACKUP_DIRECTORY, List.of(this.backupConfiguration.getFolder()));
                int frequencyMinutes = Math.max(this.backupConfiguration.getBackupFrequency(), 1);
                this.getLogger().at(Level.INFO).log("Scheduled backup to run every %d minute(s)", frequencyMinutes);
                HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
                    try {
                        this.getLogger().at(Level.INFO).log("Backing up universe...");
                        Universe.get().runBackup().thenAccept((aVoid) -> this.getLogger().at(Level.INFO).log("Completed scheduled backup."));
                    } catch (Exception e) {
                        this.getLogger().at(Level.SEVERE).withCause(e).log("Error backing up universe");
                    }

                }, frequencyMinutes, frequencyMinutes, TimeUnit.MINUTES);
            }
        } else {
            this.getLogger().at(Level.INFO).log("Ignoring scheduled backups as it was enabled with the backup option arguments");
        }
    }

    private static void onModelAssetLoad(LoadedAssetsEvent<String, ModelAsset, DefaultAssetMap<String, ModelAsset>> event) {
        MODELS = event.getAssetMap().getAssetMap();
    }

    @Override
    protected void shutdown() {
        super.shutdown();
        this.playerTracker.syncSave();
        this.adminStickCustomConfig.syncSave();
        this.muteTracker.syncSave();
    }

    public HytaleBanProvider getBanProvider() {
        return banProvider;
    }

    public HytaleWhitelistProvider getWhitelistProvider() {
        return whitelistProvider;
    }

    public PlayerTracker getPlayerTracker() {
        return playerTracker;
    }

    public AdminStickCustomConfig getAdminStickCustomConfig() {
        return adminStickCustomConfig;
    }

    public MuteTracker getMuteTracker() {
        return muteTracker;
    }

    public BackupConfiguration getBackupConfiguration() {
        return backupConfiguration;
    }

    /**
     * Backup Module:
     *  Directory : BACKUP_DIRECTORY
     *  Max Amount: BACKUP_MAX_COUNT
     *  Frequency: BACKUP_FREQUENCY_MINUTES
     *  Custom Config
     */
}