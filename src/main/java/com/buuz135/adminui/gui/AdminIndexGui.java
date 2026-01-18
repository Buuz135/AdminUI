package com.buuz135.adminui.gui;


import com.buuz135.adminui.AdminUIIndexRegistry;
import com.buuz135.adminui.util.PermissionList;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class AdminIndexGui extends InteractiveCustomUIPage<AdminIndexGui.IndexGuiData> {



    public AdminIndexGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime) {
        super(playerRef, lifetime, IndexGuiData.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/Buuz135_AdminUI_Index.ui");
        NavBarHelper.setupBar(ref, uiCommandBuilder, uiEventBuilder, store);
        int rowIndex = 0;
        int cardsInCurrentRow = 0;
        var player = store.getComponent(ref, Player.getComponentType());
        for (AdminUIIndexRegistry.Entry entry : AdminUIIndexRegistry.getInstance().getEntries()) {
            if(!entry.permission().hasPermission(player)) {continue;}
            if (cardsInCurrentRow == 0) {
                uiCommandBuilder.appendInline("#IndexCards", "Group { LayoutMode: Left; Anchor: (Bottom: 0); }");
            }

            uiCommandBuilder.append("#IndexCards[" + rowIndex + "]", "Pages/Buuz135_AdminUI_IndexEntry.ui");

            uiCommandBuilder.set("#IndexCards[" + rowIndex + "][" + cardsInCurrentRow + "] #IndexName.Text", entry.displayName());
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#IndexCards[" + rowIndex + "][" + cardsInCurrentRow + "]", EventData.of("Button", entry.id()));

            ++cardsInCurrentRow;
            if (cardsInCurrentRow >= 3) {
                cardsInCurrentRow = 0;
                ++rowIndex;
            }
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull IndexGuiData data) {
        super.handleDataEvent(ref, store, data);
        if (NavBarHelper.handleData(ref, store, data.navbar, () -> {})) {
            return;
        }
        if (data.button != null) {
            for (AdminUIIndexRegistry.Entry entry : AdminUIIndexRegistry.getInstance().getEntries()) {
                if (entry.id().equals(data.button)) {
                    var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                    var player = store.getComponent(ref, Player.getComponentType());
                    player.getPageManager().openCustomPage(ref, store, entry.guiSupplier().apply(playerRef));
                    return;
                }
            }
        }
        this.sendUpdate();
    }

    public static class IndexGuiData {
        static final String KEY_BUTTON = "Button";
        static final String KEY_NAVBAR = "NavBar";

        public static final BuilderCodec<IndexGuiData> CODEC = BuilderCodec.<IndexGuiData>builder(IndexGuiData.class, IndexGuiData::new)
                .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (searchGuiData, s) -> searchGuiData.button = s, searchGuiData -> searchGuiData.button)
                .addField(new KeyedCodec<>(KEY_NAVBAR, Codec.STRING), (searchGuiData, s) -> searchGuiData.navbar = s, searchGuiData -> searchGuiData.navbar)
                .build();

        private String button;
        private String navbar;

    }

}
