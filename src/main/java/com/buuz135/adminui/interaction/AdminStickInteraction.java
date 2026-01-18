package com.buuz135.adminui.interaction;


import com.buuz135.adminui.AdminUI;
import com.buuz135.adminui.AdminUIIndexRegistry;
import com.buuz135.adminui.gui.AdminIndexGui;
import com.buuz135.adminui.gui.PlayerGui;
import com.buuz135.adminui.gui.StatsGui;
import com.buuz135.adminui.util.PermissionList;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class AdminStickInteraction extends SimpleInteraction {

    public static final BuilderCodec<AdminStickInteraction> CODEC = BuilderCodec.builder(AdminStickInteraction.class, AdminStickInteraction::new).build();

    @Override
    public void handle(@NonNullDecl Ref<EntityStore> ref, boolean firstRun, float time, @NonNullDecl InteractionType type, @NonNullDecl InteractionContext interactionContext) {
        super.handle(ref, firstRun, time, type, interactionContext);
        var store = ref.getStore();
        var player = ref.getStore().getComponent(ref, Player.getComponentType());
        PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
        if (!PermissionList.ADMIN_STICK_USE.hasPermission(player)) return;
        var config = AdminUI.getInstance().getAdminStickCustomConfig();
        var id = "";
        if (type == InteractionType.Ability1) id = config.getPlayer(playerRefComponent.getUuid()).ability1();
        if (type == InteractionType.Ability2) id = config.getPlayer(playerRefComponent.getUuid()).ability2();
        if (type == InteractionType.Ability3) id = config.getPlayer(playerRefComponent.getUuid()).ability3();
        if (type == InteractionType.Primary) id = config.getPlayer(playerRefComponent.getUuid()).primary();
        if (type == InteractionType.Secondary) id = config.getPlayer(playerRefComponent.getUuid()).secondary();
        if (type == InteractionType.Pick) id = config.getPlayer(playerRefComponent.getUuid()).pick();

        var entry = AdminUIIndexRegistry.getInstance().getEntry(id);
        if (entry == null || id.isEmpty() || !entry.permission().hasPermission(player)) {
            player.getPageManager().openCustomPage(ref, store, new AdminIndexGui(playerRefComponent, CustomPageLifetime.CanDismiss));
            return;
        }
        if (entry.permission().hasPermission(player)) {
            player.getPageManager().openCustomPage(ref, store, entry.guiSupplier().apply(playerRefComponent));
        }
    }

}
