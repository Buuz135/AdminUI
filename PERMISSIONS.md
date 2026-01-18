# AdminUI Permissions

This document lists all the permissions used by the AdminUI plugin.

## Core Permissions

| Permission | Description |
|------------|-------------|
| `AdminUI.admin` | Master permission. Grants access to all AdminUI features. |

## Module Permissions

These permissions grant access to specific modules within the AdminUI. They control visibility in the navigation bar and the ability to open the respective GUIs.

Granting the root permission (e.g., `AdminUI.whitelist`) grants access to all actions within that module.

| Permission                | Description                                               |
|---------------------------|-----------------------------------------------------------|
| `AdminUI.ban.open`        | Access to the Ban management GUI.                         |
| `AdminUI.mute.open`       | Access to the Mute management GUI.                        |
| `AdminUI.player.open`     | Access to the Player management GUI.                      |
| `AdminUI.stats.open`      | Access to the Server Stats GUI.                           |
| `AdminUI.ui.open`         | Grants access to the main `/admin` command and index GUI. |
| `AdminUI.warp.open`       | Access to the Warps management GUI.                       |
| `AdminUI.whitelist.open`  | Access to the Whitelist management GUI.                   |
| `AdminUI.adminstick.open` | Access to the Admin Stick configuration GUI.              |
| `AdminUI.adminstick.use`  | Access to the Admin Stick usage.                          |
| `AdminUI.backup.open`     | Access to the Server Backups management GUI.              |

## Permission Roots

You can also use these root permissions to grant full access to a specific module:

- `AdminUI.ui`
- `AdminUI.ban`
- `AdminUI.mute`
- `AdminUI.player`
- `AdminUI.stats`
- `AdminUI.warp`
- `AdminUI.whitelist`
- `AdminUI.adminstick`
- `AdminUI.backup`
