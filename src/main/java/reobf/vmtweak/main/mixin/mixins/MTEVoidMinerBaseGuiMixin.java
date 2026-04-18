package reobf.vmtweak.main.mixin.mixins;

import java.lang.reflect.Method;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.value.sync.PanelSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;

import net.minecraft.util.StatCollector;

import bwcrossmod.galacticgreg.MTEVoidMinerBase;
import bwcrossmod.galacticgreg.VoidMinerUtility;
import gregtech.common.gui.modularui.multiblock.MTEVoidMinerBaseGui;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gtneioreplugin.plugin.item.ItemDimensionDisplay;
import gtneioreplugin.util.DimensionHelper;
import reobf.vmtweak.main.IVMTweakOverride;

/**
 * Mixin for {@link MTEVoidMinerBaseGui} that integrates dimension-override
 * information into the MUI2 GUI.
 */
@SuppressWarnings("rawtypes")
@Mixin(value = MTEVoidMinerBaseGui.class, remap = false)
public abstract class MTEVoidMinerBaseGuiMixin extends MTEMultiBlockBaseGui<MTEVoidMinerBase> {

    @SuppressWarnings("DataFlowIssue")
    private MTEVoidMinerBaseGuiMixin() {
        super(null);
    }

    /** Last seen dimension change version, used to detect when override changes. */
    @Unique
    private int vmtweak$lastSeenVersion = -1;

    /**
     * State machine for live-refresh of the filter popup.
     * 0 = IDLE (monitoring for changes)
     * 1 = CLOSING (sent close to client, waiting for round-trip confirmation)
     * 2 = DISPOSED (disposed cache, will reopen next tick)
     */
    @Unique
    private int vmtweak$refreshState = 0;

    /**
     * Override createTerminalTextWidget to add dimension override display text
     * below the existing status lines in the terminal/screen area.
     */
    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager syncManager, ModularPanel parent) {
        ListWidget<IWidget, ?> list = super.createTerminalTextWidget(syncManager, parent);

        MTEVoidMinerBase mb = this.multiblock;

        // Sync the override display text to the client
        StringSyncValue overrideTextSyncer = new StringSyncValue(
            () -> ((IVMTweakOverride) mb).vmtweak$getOverrideDisplayText());
        syncManager.syncValue("vmtweak$overrideText", overrideTextSyncer);

        // Sync the warning text
        StringSyncValue warningSyncer = new StringSyncValue(
            () -> ((IVMTweakOverride) mb).vmtweak$getWarning());
        syncManager.syncValue("vmtweak$warning", warningSyncer);

        // Add override text label below existing status lines
        // The synced value is raw data from the server: "" (no override),
        // "!warningKey" (warning), or a dimension abbreviation like "Ow", "Ne", "EA".
        // Client-side lambda performs localization using client-safe APIs.
        list.child(
            IKey.dynamic(() -> {
                    String raw = overrideTextSyncer.getValue();
                    if (raw == null || raw.isEmpty()) return "";
                    // "!" prefix means it's a warning lang key
                    if (raw.startsWith("!")) {
                        return StatCollector.translateToLocal(raw.substring(1));
                    }
                    // Otherwise it's a dimension abbreviation — localize on client
                    String displayName;
                    if ("EA".equals(raw)) {
                        displayName = StatCollector.translateToLocal("vmtweak.gui.override.end_asteroids");
                    } else {
                        String internalName = DimensionHelper.ABBR_TO_INTERNAL.getOrDefault(raw, raw);
                        displayName = DimensionHelper.getDimLocalizedName(internalName);
                        if (displayName == null || displayName.isEmpty()) {
                            displayName = raw;
                        }
                    }
                    return StatCollector.translateToLocal("vmtweak.gui.override.description") + displayName;
                })
                .color(Color.WHITE.main)
                .asWidget()
                .setEnabledIf(w -> {
                    String raw = overrideTextSyncer.getValue();
                    return raw != null && !raw.isEmpty();
                })
                .marginBottom(2)
                .widthRel(1));

        return list;
    }

    /**
     * Inject at RETURN of createRightPanelGapRow to register a server tick handler
     * that monitors for dimension override changes. When a change is detected while
     * the filter popup is open, it closes it, deletes the cached panel, and reopens
     * it so the new dimension's ores are displayed.
     */
    @Inject(method = "createRightPanelGapRow", at = @At("RETURN"), require = 1, remap = false)
    private void vmtweak$onCreateRightPanelGapRow(ModularPanel parent, PanelSyncManager syncManager,
        CallbackInfoReturnable<Flow> cir) {

        MTEVoidMinerBase mb = this.multiblock;

        // Look up the filter panel handler that was just registered by the original method
        IPanelHandler filterPanel = syncManager.findPanelHandlerNullable("filter");
        if (filterPanel == null) return;

        // Initialize the last seen version
        vmtweak$lastSeenVersion = ((IVMTweakOverride) mb).vmtweak$getDimChangeVersion();

        // Register a server tick handler that checks for dimension changes
        // and live-refreshes the filter popup using a 3-phase state machine:
        //   IDLE → detect change → close panel → CLOSING
        //    → wait for client close round-trip (isPanelOpen becomes false) → dispose →
        //    DISPOSED → reopen panel → IDLE
        // This ensures the client fully closes the panel before we dispose its syncManager,
        // avoiding NPE in closeSubPanels().
        syncManager.onServerTick(() -> {
            switch (vmtweak$refreshState) {
                case 1: // CLOSING - waiting for panel to fully close via round-trip
                    if (!filterPanel.isPanelOpen()) {
                        // Client confirmed close. Now safe to dispose cached panel on both sides.
                        vmtweak$forceDisposePanel((PanelSyncHandler) filterPanel);
                        vmtweak$refreshState = 2;
                    }
                    return;
                case 2: // DISPOSED - reopen with fresh UI
                    filterPanel.openPanel();
                    ((PanelSyncHandler) filterPanel).syncToClient(PanelSyncHandler.SYNC_OPEN);
                    vmtweak$refreshState = 0;
                    return;
                default: // IDLE - monitor for changes
                    break;
            }
            int currentVersion = ((IVMTweakOverride) mb).vmtweak$getDimChangeVersion();
            if (currentVersion != vmtweak$lastSeenVersion) {
                vmtweak$lastSeenVersion = currentVersion;
                if (filterPanel.isPanelOpen()) {
                    // Send close to client; server will see isPanelOpen()==false
                    // after client round-trips closePanelInternal back.
                    filterPanel.closePanel();
                    vmtweak$refreshState = 1;
                } else {
                    // Not open - just dispose cache so next manual open is fresh
                    vmtweak$forceDisposePanel((PanelSyncHandler) filterPanel);
                }
            }
        });
    }

    /**
     * Inject at HEAD of createFilterPopup to ensure the dropMap is correct on BOTH
     * client and server. The client-side MTE's dropMap may be stale because
     * calculateDropMap only runs on the server during recipe processing.
     */
    @Inject(method = "createFilterPopup", at = @At("HEAD"), require = 1, remap = false, cancellable = true)
    private void vmtweak$onCreateFilterPopup(PanelSyncManager syncManager, IPanelHandler panelHandler,
        CallbackInfoReturnable<ModularPanel> cir) {

        MTEVoidMinerBase mb = multiblock;

        // Try to resolve the correct dropMap from the override slot
        try {
            net.minecraft.item.ItemStack slotStack = mb.mInventory[1];
            if (slotStack != null && slotStack.getItem() instanceof ItemDimensionDisplay) {
                String dimAbbr = ItemDimensionDisplay.getDimension(slotStack);
                if (dimAbbr != null && !"None".equals(dimAbbr)) {
                    String targetDimName = DimensionHelper.ABBR_TO_INTERNAL.get(dimAbbr);
                    if (targetDimName != null) {
                        VoidMinerUtility.DropMap resolvedMap = VoidMinerUtility.dropMapsByDimName.get(targetDimName);
                        if (resolvedMap != null) {
                            VoidMinerUtility.DropMap extraMap = VoidMinerUtility.extraDropsByDimName
                                .getOrDefault(targetDimName, new VoidMinerUtility.DropMap());
                            resolvedMap.isDistributionCached(extraMap);
                            mb.dropMap = resolvedMap;
                            mb.extraDropMap = extraMap;
                            // Ensure selected filter handler is at least as large as ore count
                            if (resolvedMap.getOres() != null
                                && mb.selected.getSlots() < resolvedMap.getOres().length) {
                                mb.selected.setSize(resolvedMap.getOres().length);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        // Final guard: if dropMap is still null or has no ores, show the "no ores" panel
        VoidMinerUtility.DropMap dm = mb.dropMap;
        if (dm == null) {
            cir.setReturnValue(vmtweak$emptyFilterPanel());
            return;
        }
        dm.isDistributionCached(mb.extraDropMap);
        if (dm.getOres() == null || dm.getOres().length == 0) {
            cir.setReturnValue(vmtweak$emptyFilterPanel());
            return;
        }
        // Ensure selected is at least as large as ores count (never shrink)
        int oreCount = dm.getOres().length;
        if (mb.selected.getSlots() < oreCount) {
            mb.selected.setSize(oreCount);
        }
    }

    /**
     * Forces disposal of the cached panel data on both server and client.
     * This bypasses {@link PanelSyncHandler#deleteCachedPanel()}'s early return
     * (which checks openedPanel != null — always null on server) by directly
     * invoking the private {@code disposePanel()} method via reflection, then
     * sending {@code SYNC_DISPOSE} to the client.
     */
    @Unique
    private static void vmtweak$forceDisposePanel(PanelSyncHandler psh) {
        try {
            Method disposeMethod = PanelSyncHandler.class.getDeclaredMethod("disposePanel");
            disposeMethod.setAccessible(true);
            disposeMethod.invoke(psh);
            // Tell client to dispose its cached panel too
            psh.sync(PanelSyncHandler.SYNC_DISPOSE);
        } catch (Exception ignored) {}
    }

    @Unique
    private ModularPanel vmtweak$emptyFilterPanel() {
        return new ModularPanel("gt:vm:filter").child(ButtonWidget.panelCloseButton())
            .child(
                IKey.lang("vmtweak.gui.filter.no_ores")
                    .color(Color.WHITE.main)
                    .asWidget()
                    .margin(8))
            .coverChildren();
    }
}
