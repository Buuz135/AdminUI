package com.buuz135.adminui.util;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;

import java.util.HashMap;
import java.util.Map;

public enum PermissionList {
    //COMMANDS
    OPEN_UI("open", "ui", "You don't have permission to open admin menu"),
    //WARPS
    WARP_OPEN_UI("open", "warp", "You don't have permission to open warp menu"),
    //MUTES
    MUTE_OPEN_UI("open", "mute", "You don't have permission to open mutes menu"),
    //BANS
    BAN_OPEN_UI("open", "ban", "You don't have permission to open bans menu"),
    //PLAYERS
    PLAYER_OPEN_UI("open", "player", "You don't have permission to open players menu"),
    //ADMIN STICK
    ADMIN_STICK_OPEN_UI("open", "adminstick", "You don't have permission to open admin stick menu"),
    ADMIN_STICK_USE("use", "adminstick", "You don't have permission to use the admin stick"),
    //BACKUPS
    BACKUP_OPEN_UI("open", "backup", "You don't have permission to open backups menu"),
    //STATS
    STATS_OPEN_UI("open", "stats", "You don't have permission to open server stats menu"),
    //WHITELIST
    WHITELIST_OPEN_UI("open", "whitelist", "You don't have permission to open whitelist menu");
    private final String permission;
    private final String permissionRoot;
    private final String denyMessage;

    private PermissionList(String permission, String permissionRoot, String denyMessage){
        this.permission = String.format("AdminUI.%s.%s", permissionRoot, permission);
        this.permissionRoot = String.format("AdminUI.%s", permissionRoot);
        this.denyMessage = denyMessage;
    }

    public Message getMessage(){
        return Message.raw(denyMessage);
    }

    public String getPermission() {
        return permission;
    }

    public String getPermissionRoot() {
        return permissionRoot;
    }

    public Boolean hasPermission(Player player){
        return player.hasPermission(this.getPermission()) || player.hasPermission(this.getPermissionRoot()) || player.hasPermission("AdminUI.admin");
    }
}
