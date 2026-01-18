package com.buuz135.adminui.gui;

import com.buuz135.adminui.AdminUIIndexRegistry;
import com.buuz135.adminui.util.PermissionList;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class NavBarHelper {

    public static void setupBar(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store){
        var player = store.getComponent(ref, Player.getComponentType());
        int index = 0;
        uiCommandBuilder.appendInline("#AdminUITopNavigationBar #NavBarButtons", "Group #NavCards { LayoutMode: Left; }");
        for (AdminUIIndexRegistry.Entry entry : AdminUIIndexRegistry.getInstance().getEntries()) {
            if(!entry.permission().hasPermission(player)) {continue;}
            uiCommandBuilder.append("#NavCards", "Pages/Nav/Buuz135_AdminUI_TopNavigationBarButton.ui");
            uiCommandBuilder.set("#NavBarButtons #NavCards[" + index + "] #NavActionButton.Text", entry.displayName());
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#NavBarButtons #NavCards[" + index + "] #NavActionButton", EventData.of("NavBar", entry.id()));
            ++index;
        }
    }

    public static boolean handleData(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, String navBarData, Runnable onCancel){
        if (navBarData == null) return false;
        var entry = AdminUIIndexRegistry.getInstance().getEntry(navBarData);
        if (entry == null) return false;
        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        var player = store.getComponent(ref, Player.getComponentType());
        onCancel.run();
        player.getPageManager().openCustomPage(ref, store, entry.guiSupplier().apply(playerRef));
        return true;
    }
}
