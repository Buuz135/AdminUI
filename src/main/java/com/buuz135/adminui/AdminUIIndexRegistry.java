package com.buuz135.adminui;

import com.buuz135.adminui.command.AdminCommand;
import com.buuz135.adminui.command.AdminShortcutCommand;
import com.buuz135.adminui.util.PermissionList;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class AdminUIIndexRegistry {

    private static final AdminUIIndexRegistry INSTANCE = new AdminUIIndexRegistry();

    public static AdminUIIndexRegistry getInstance() {
        return INSTANCE;
    }

    private List<Entry> entries;
    private AdminCommand command;

    public AdminUIIndexRegistry() {
        this.entries = new ArrayList<>();
        this.command = new AdminCommand();
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public AdminUIIndexRegistry register(Entry entry){
        this.entries.add(entry);
        if (entry.commandShortcut().length > 0) {
            this.command.addSubCommand(new AdminShortcutCommand(entry));
        }
        return this;
    }

    public Entry getEntry(String id){
        return this.entries.stream().filter(entry -> entry.id().equals(id)).findFirst().orElse(null);
    }

    public AdminCommand getCommand() {
        return command;
    }

    public record Entry(String id, String displayName, PermissionList permission, Function<PlayerRef, ? extends InteractiveCustomUIPage<?>> guiSupplier, boolean showsInNavBar, String... commandShortcut) {
    }
}
