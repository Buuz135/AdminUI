![alt text](https://media.forgecdn.net/attachments/1447/922/admin_ui-png.png)![code cat](https://media.forgecdn.net/attachments/1426/453/code_cat-png.png)

[![](https://img.shields.io/twitter/follow/Buuz135mods?color=E04E14&labelColor=2D2D2D&style=for-the-badge)](https://twitter.com/Buuz135mods) [![](https://img.shields.io/badge/DONATE-KOFI-E04E14?labelColor=2D2D2D&style=for-the-badge)](https://ko-fi.com/buuz135) [![](https://img.shields.io/discord/357597633566605313?color=E04E14&labelColor=2D2D2D&label=JOIN-DISCORD&style=for-the-badge)](https://discord.gg/4tPfwjn)

AdminUI adds multiple GUI that will allow you to handle all server administrative operations.

These include:

*   Whitelist (/admin wl)
*   Mute (Custom Feature) (/admin m)
*   Ban (/admin b)
*   Players (/admin p)
*   Warps (/admin w)
*   Live Server Stats Page (/admin st)
*   Backup managing (/admin bk)

You can also open the main interface with GUI

[![APEX HOSTING](https://media.forgecdn.net/attachments/1464/785/837x80_hytale-v3-png.png)](https://apexhost.gg/Buuz135) ![alt text](https://media.forgecdn.net/attachments/1447/928/hytaleclient_bo51rronkn-png.png)

You can open the AdminUI interface with the /admin command. You can also open the GUI with the Admin Stick.

The Admin Stick is a configurable shortcuts item, where you can specify which GUI each action will do. This configuration is player based so each player can have a configuration each.

![alt text](https://media.forgecdn.net/attachments/1447/949/hytaleclient_2t4i7jgym0-jpg.jpg)

# AdminUI Permissions

## Core Permissions

| Permission    |Description                                               |
| ------------- |--------------------------------------------------------- |
| <code>AdminUI.admin</code> |Master permission. Grants access to all AdminUI features. |

## Module Permissions

These permissions grant access to specific modules within the AdminUI. They control visibility in the navigation bar and the ability to open the respective GUIs.

Granting the root permission (e.g., `AdminUI.whitelist`) grants access to all actions within that module.

| Permission              |Description                                             |
| ----------------------- |------------------------------------------------------- |
| <code>AdminUI.ban.open</code> |Access to the Ban management GUI.                       |
| <code>AdminUI.mute.open</code> |Access to the Mute management GUI.                      |
| <code>AdminUI.player.open</code> |Access to the Player management GUI.                    |
| <code>AdminUI.stats.open</code> |Access to the Server Stats GUI.                         |
| <code>AdminUI.ui.open</code> |Grants access to the main <code>/admin</code> command and index GUI. |
| <code>AdminUI.warp.open</code> |Access to the Warps management GUI.                     |
| <code>AdminUI.whitelist.open</code> |Access to the Whitelist management GUI.                 |
| <code>AdminUI.adminstick.open</code> |Access to the Admin Stick configuration GUI.            |
| <code>AdminUI.adminstick.use</code> |Access to the Admin Stick usage.                        |
| <code>AdminUI.backup.open</code> |Access to the Server Backups management GUI.            |

## Permission Roots

You can also use these root permissions to grant full access to a specific module:

*   `AdminUI.ui`
*   `AdminUI.ban`
*   `AdminUI.mute`
*   `AdminUI.player`
*   `AdminUI.stats`
*   `AdminUI.warp`
*   `AdminUI.whitelist`
*   `AdminUI.adminstick`
*   `AdminUI.backup`