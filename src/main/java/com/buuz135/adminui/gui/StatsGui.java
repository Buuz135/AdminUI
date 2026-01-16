package com.buuz135.adminui.gui;

import com.buuz135.adminui.AdminUI;
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
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.TimeUnit;

public class StatsGui extends InteractiveCustomUIPage<StatsGui.SearchGuiData> {

    private Thread updateThread;

    public StatsGui(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, StatsGui.SearchGuiData.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/Stat/Buuz135_AdminUI_StatPage.ui");
        NavBarHelper.setupBar(ref, uiCommandBuilder, uiEventBuilder, store);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of("Button", "BackButton"), false);
        var player = store.getComponent(ref, Player.getComponentType());
        this.buildList(ref, uiCommandBuilder, uiEventBuilder, store, player);
        this.updateThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                UICommandBuilder commandBuilder = new UICommandBuilder();
                UIEventBuilder eventBuilder = new UIEventBuilder();
                this.buildList(ref, commandBuilder, eventBuilder, store, player);
                this.sendUpdate(commandBuilder, eventBuilder, false);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {

                }
            }
        });
        this.updateThread.start();
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull StatsGui.SearchGuiData data) {
        super.handleDataEvent(ref, store, data);
        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        var player = store.getComponent(ref, Player.getComponentType());
        if (NavBarHelper.handleData(ref, store, data.navbar, () -> this.updateThread.interrupt())) {
            this.updateThread.interrupt();
            return;
        }
        if (data.button != null) {
            if (data.button.equals("BackButton")) {
                this.updateThread.interrupt();
                player.getPageManager().openCustomPage(ref, store, new AdminIndexGui(playerRef, CustomPageLifetime.CanDismiss));
                return;
            }
        }
    }

    private void buildList(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull ComponentAccessor<EntityStore> componentAccessor, Player player) {
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        if (operatingSystemMXBean instanceof com.sun.management.OperatingSystemMXBean sunOSBean) {
            var systemCpuLoad = sunOSBean.getSystemCpuLoad();
            var processCpuLoad = sunOSBean.getProcessCpuLoad();
            if (!(player.getPageManager().getCustomPage() instanceof StatsGui)) return;

            commandBuilder.set("#SystemCPUUsage.Text", ((int)(systemCpuLoad*100)) + "%");
            commandBuilder.set("#SystemCPUUsageBar.Value", systemCpuLoad);
            commandBuilder.set("#ProcessCPUUsage.Text", ((int)(processCpuLoad*100)) + "%");
            commandBuilder.set("#ProcessCPUUsageBar.Value", processCpuLoad);

            var used = sunOSBean.getTotalMemorySize() - sunOSBean.getFreeMemorySize();
            commandBuilder.set("#AllRamUsageBar.Value", used / (double) sunOSBean.getTotalMemorySize());
            commandBuilder.set("#AllRamUsage.Text", FormatUtil.bytesToString(used));
            commandBuilder.set("#MaxAllRamUsage.Text", FormatUtil.bytesToString(sunOSBean.getTotalMemorySize()));
        }
        var loadAverage = operatingSystemMXBean.getSystemLoadAverage();
        commandBuilder.set("#LoadAverageCPUUsage.Text", loadAverage < 0 ? "Unknown" : ((int)(loadAverage*100)) + "%");
        commandBuilder.set("#LoadAverageCPUUsageBar.Value", loadAverage);
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        commandBuilder.set("#ProcessUptime.Text", FormatUtil.timeUnitToString(runtimeMXBean.getUptime(), TimeUnit.MILLISECONDS));
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        var memoryUsage = memoryMXBean.getHeapMemoryUsage();
        commandBuilder.set("#ServerRamUsageBar.Value", memoryUsage.getUsed() / (double) memoryUsage.getMax());
        commandBuilder.set("#ServerRamUsage.Text", FormatUtil.bytesToString(memoryUsage.getUsed()));
        commandBuilder.set("#MaxServerRamUsage.Text", FormatUtil.bytesToString(memoryUsage.getMax()));
    }

    @Override
    protected void close() {
        super.close();
        this.updateThread.interrupt();
    }

    @Override
    public void onDismiss(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store) {
        super.onDismiss(ref, store);
        this.updateThread.interrupt();
    }

    public static class SearchGuiData {
        static final String KEY_BUTTON = "Button";
        static final String KEY_NAVBAR = "NavBar";

        public static final BuilderCodec<StatsGui.SearchGuiData> CODEC = BuilderCodec.<StatsGui.SearchGuiData>builder(StatsGui.SearchGuiData.class, StatsGui.SearchGuiData::new)
                .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (searchGuiData, s) -> searchGuiData.button = s, searchGuiData -> searchGuiData.button)
                .addField(new KeyedCodec<>(KEY_NAVBAR, Codec.STRING), (searchGuiData, s) -> searchGuiData.navbar = s, searchGuiData -> searchGuiData.navbar)
                .build();

        private String button;
        private String navbar;

    }
}
