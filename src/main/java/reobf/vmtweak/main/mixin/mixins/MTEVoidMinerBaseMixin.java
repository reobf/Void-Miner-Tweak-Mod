package reobf.vmtweak.main.mixin.mixins;

import java.util.Objects;
import java.util.Optional;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.cleanroommc.modularui.utils.item.ItemStackHandler;

import bwcrossmod.galacticgreg.MTEVoidMinerBase;
import bwcrossmod.galacticgreg.VoidMinerUtility;
import galacticgreg.api.ModDimensionDef;
import galacticgreg.api.enums.DimensionDef;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gtneioreplugin.plugin.item.ItemDimensionDisplay;
import gtneioreplugin.util.DimensionHelper;
import reobf.vmtweak.main.IVMTweakOverride;

/**
 * Mixin for {@link MTEVoidMinerBase} that adds dimension-override functionality.
 * <p>
 * Players can place a NEI Dimension Display block (from GT NEI Ore Plugin) into
 * the controller's special slot to override which dimension's ore table the void
 * miner uses. This enables void mining ores from other dimensions (including
 * private/personal dimensions that normally have no ore table).
 */
@SuppressWarnings("rawtypes")
@Mixin(MTEVoidMinerBase.class)
public abstract class MTEVoidMinerBaseMixin extends MTEEnhancedMultiBlockBase implements IVMTweakOverride {

    // region Shadow fields from MTEVoidMinerBase

    @Shadow(remap = false)
    private float totalWeight;

    @Shadow(remap = false)
    public VoidMinerUtility.DropMap dropMap;

    @Shadow(remap = false)
    public VoidMinerUtility.DropMap extraDropMap;

    @Shadow(remap = false)
    private ModDimensionDef dimensionDef;

    @Shadow(remap = false)
    private boolean canVoidMine;

    @Shadow(remap = false)
    public ItemStackHandler selected;

    // endregion

    // region Unique fields for dimension override

    /** Warning message displayed in the GUI when override fails. */
    @Unique
    private String vmtweak$warning = "";

    /** The last dimension abbreviation used for override (e.g. "Ow", "Ne"). "None" means no override. */
    @Unique
    private String vmtweak$lastDimOverride = "None";

    /** Monotonically increasing counter that increments each time the dimension override changes. */
    @Unique
    private int vmtweak$dimChangeVersion = 0;

    // endregion

    protected MTEVoidMinerBaseMixin(String aName) {
        super(aName);
    }

    // region calculateDropMap override

    /**
     * Replaces the original {@code onFirstTick} logic after calculateDropMap to
     * handle null getOres() safely. The original code does:
     * {@code int size = dropMap.getOres().length} which NPEs when getOres() is null
     * (e.g., for empty DropMaps from private dimensions).
     */
    @Inject(method = "onFirstTick", at = @At("HEAD"), require = 1, remap = false, cancellable = true)
    private void vmtweak$onFirstTick(gregtech.api.interfaces.tileentity.IGregTechTileEntity aBaseMetaTileEntity,
        CallbackInfo ci) {
        ci.cancel();
        // Reproduce super.onFirstTick behavior
        super.onFirstTick(aBaseMetaTileEntity);
        // Our replacement for calculateDropMap
        vmtweak$recalculateDropMap();
        // Safe null-check for getOres()
        vmtweak$resizeSelected();
    }

    /**
     * Replaces the original {@code calculateDropMap} to support dimension override
     * via NEI ore blocks placed in the controller slot.
     */
    @Inject(method = "calculateDropMap", at = @At("HEAD"), require = 1, remap = false, cancellable = true)
    private void vmtweak$onCalculateDropMap(CallbackInfo ci) {
        ci.cancel();
        vmtweak$recalculateDropMap();
    }

    @Unique
    private void vmtweak$recalculateDropMap() {
        // Reset state
        this.dropMap = null;
        this.extraDropMap = null;
        this.totalWeight = 0;
        this.canVoidMine = false;
        this.vmtweak$warning = "";

        // Determine the machine's actual dimension definition
        if (getBaseMetaTileEntity() != null) {
            this.dimensionDef = DimensionDef.getDefForWorld(getBaseMetaTileEntity().getWorld());
        }

        // Check if a dimension override block is present
        String overrideAbbr = vmtweak$readOverrideSlot();
        boolean hasOverride = !"None".equals(overrideAbbr);

        // Attempt dimension override
        if (hasOverride) {
            String targetDimName = DimensionHelper.ABBR_TO_INTERNAL.get(overrideAbbr);
            if (targetDimName != null && VoidMinerUtility.dropMapsByDimName.containsKey(targetDimName)) {
                this.canVoidMine = true;
                this.dropMap = VoidMinerUtility.dropMapsByDimName.get(targetDimName);
                this.extraDropMap = VoidMinerUtility.extraDropsByDimName
                    .getOrDefault(targetDimName, new VoidMinerUtility.DropMap());
                this.dropMap.isDistributionCached(this.extraDropMap);
                this.totalWeight = dropMap.getTotalWeight() + extraDropMap.getTotalWeight();
                vmtweak$resizeSelected();
                return;
            }
            // Override attempted but target dimension has no ore data
            this.vmtweak$warning = StatCollector.translateToLocal("vmtweak.gui.override.error");
        }

        // Fallback to the machine's actual dimension
        if (this.dimensionDef == null || !this.dimensionDef.canBeVoidMined()) {
            // For private dimensions without GalacticGreg registration, set an empty
            // dropMap so the filter gear button remains visible in the GUI.
            this.dropMap = new VoidMinerUtility.DropMap();
            this.extraDropMap = new VoidMinerUtility.DropMap();
            vmtweak$resizeSelected();
            return;
        }

        this.canVoidMine = true;
        String actualDimName = this.dimensionDef.getDimensionName();
        this.dropMap = VoidMinerUtility.dropMapsByDimName
            .getOrDefault(actualDimName, new VoidMinerUtility.DropMap());
        this.extraDropMap = VoidMinerUtility.extraDropsByDimName
            .getOrDefault(actualDimName, new VoidMinerUtility.DropMap());
        this.dropMap.isDistributionCached(this.extraDropMap);
        this.totalWeight = dropMap.getTotalWeight() + extraDropMap.getTotalWeight();

        if (hasOverride) {
            // Override was attempted but failed — show appropriate warning
            if (this.totalWeight > 0) {
                this.vmtweak$warning = StatCollector.translateToLocal("vmtweak.gui.override.failed");
            } else {
                this.vmtweak$warning = StatCollector.translateToLocal("vmtweak.gui.override.error");
            }
        }
        vmtweak$resizeSelected();
    }

    /**
     * Reads the dimension abbreviation from the NEI ore block in the controller slot.
     * Updates {@link #vmtweak$lastDimOverride} and resets totalWeight on change.
     */
    @Unique
    private String vmtweak$readOverrideSlot() {
        String dimAbbr;
        try {
            dimAbbr = Optional.ofNullable(this.mInventory[1])
                .filter(s -> s.getItem() instanceof ItemDimensionDisplay)
                .map(ItemDimensionDisplay::getDimension)
                .orElse("None");
        } catch (Exception e) {
            dimAbbr = "None";
        }

        if (!Objects.equals(dimAbbr, vmtweak$lastDimOverride)) {
            vmtweak$lastDimOverride = dimAbbr;
            this.totalWeight = 0;
        }
        return dimAbbr;
    }

    /**
     * Ensures the selected filter {@link ItemStackHandler} is at least as large as
     * the current dropMap's ore count. Never shrinks to avoid index-out-of-bounds
     * crashes from cached GUI widgets that still reference old indices.
     */
    @Unique
    private void vmtweak$resizeSelected() {
        if (this.dropMap == null) return;
        gregtech.api.util.GTUtility.ItemId[] ores = this.dropMap.getOres();
        if (ores == null) return;
        int oreCount = ores.length;
        if (this.selected.getSlots() < oreCount) {
            this.selected.setSize(oreCount);
        }
    }

    // endregion

    // region NBT persistence

    @Inject(method = "saveNBTData", at = @At("HEAD"), require = 1, remap = false)
    private void vmtweak$onSaveNBT(NBTTagCompound aNBT, CallbackInfo ci) {
        aNBT.setString("vmtweak$lastDimOverride", this.vmtweak$lastDimOverride);
    }

    @Inject(method = "loadNBTData", at = @At("HEAD"), require = 1, remap = false)
    private void vmtweak$onLoadNBT(NBTTagCompound aNBT, CallbackInfo ci) {
        this.vmtweak$lastDimOverride = aNBT.getString("vmtweak$lastDimOverride");
        if (this.vmtweak$lastDimOverride.isEmpty()) {
            this.vmtweak$lastDimOverride = "None";
        }
    }

    // endregion

    // region Working tick — detect slot changes

    @Inject(method = "working", at = @At("HEAD"), require = 1, remap = false)
    private void vmtweak$onWorking(CallbackInfoReturnable<Boolean> cir) {
        String prev = vmtweak$lastDimOverride;
        vmtweak$readOverrideSlot();
        if (!Objects.equals(prev, vmtweak$lastDimOverride)) {
            vmtweak$dimChangeVersion++;
            vmtweak$recalculateDropMap();
        }
    }

    // endregion

    // region Public accessors for GUI mixin

    @Unique
    @Override
    public int vmtweak$getDimChangeVersion() {
        return vmtweak$dimChangeVersion;
    }

    /**
     * @return the current warning message, or empty string if no warning.
     */
    @Unique
    @Override
    public String vmtweak$getWarning() {
        return vmtweak$warning;
    }

    /**
     * Returns raw dimension override data for syncing to the client.
     * <p>
     * Does NOT perform any localization — the client-side GUI widget handles
     * that to avoid loading client-only classes (e.g. {@code I18n}) on the
     * server thread.
     *
     * @return "" if no override; the dimension abbreviation (e.g. "Ow", "Ne", "EA")
     *         if overriding successfully; or a raw warning string prefixed with "!"
     *         if the override failed.
     */
    @Unique
    @Override
    public String vmtweak$getOverrideDisplayText() {
        if ("None".equals(vmtweak$lastDimOverride)) {
            return "";
        }
        if (!vmtweak$warning.isEmpty() && (this.dropMap == null || this.dropMap.getTotalWeight() <= 0)) {
            // Prefix with "!" so the client can distinguish warnings from abbreviations
            return "!" + vmtweak$warning;
        }
        // Return the raw abbreviation; client will localize it
        return vmtweak$lastDimOverride;
    }

    // endregion
}
