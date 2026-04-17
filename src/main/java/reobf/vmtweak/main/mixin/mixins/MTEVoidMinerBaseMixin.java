package reobf.vmtweak.main.mixin.mixins;

import java.util.Objects;
import java.util.Optional;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizons.modularui.api.math.Alignment;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.widget.DynamicPositionedColumn;
import com.gtnewhorizons.modularui.common.widget.FakeSyncWidget;
import com.gtnewhorizons.modularui.common.widget.SlotWidget;
import com.gtnewhorizons.modularui.common.widget.TextWidget;

import bwcrossmod.galacticgreg.MTEVoidMinerBase;
import bwcrossmod.galacticgreg.VoidMinerUtility;
import galacticgreg.api.ModDimensionDef;
import galacticgreg.api.enums.DimensionDef;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gtneioreplugin.plugin.block.ModBlocks;
import gtneioreplugin.plugin.item.ItemDimensionDisplay;
import gtneioreplugin.util.DimensionHelper;

@SuppressWarnings("rawtypes")
@Mixin(MTEVoidMinerBase.class)
public abstract class MTEVoidMinerBaseMixin extends MTEEnhancedMultiBlockBase {

    public MTEVoidMinerBaseMixin(String aName) {
        super(aName);
    }

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

    @Unique
    private String VMTweak$warning = "";

    @Unique
    private String VMTweak$mLastDimensionOverride = "None";

    @Inject(method = "calculateDropMap", at = @At("HEAD"), require = 1, remap = false, cancellable = true)
    private void calculateDropMap(CallbackInfo ci) {
        ci.cancel();
        VMTweak$doCalculateDropMap();
    }

    @Unique
    private void VMTweak$doCalculateDropMap() {
        this.dropMap = null;
        this.extraDropMap = null;
        this.totalWeight = 0;
        this.canVoidMine = false;
        VMTweak$warning = "";

        // Always determine the actual dimension def (used for data copy, display, etc.)
        this.dimensionDef = DimensionDef.getDefForWorld(getBaseMetaTileEntity().getWorld());

        String dimOverrideAbbr = VMTweak$checkNEIOreBlockDim();
        boolean overrideAttempted = !"None".equals(dimOverrideAbbr);

        // Try dimension override from NEI ore block
        if (overrideAttempted) {
            String targetDimName = DimensionHelper.ABBR_TO_INTERNAL.get(dimOverrideAbbr);
            if (targetDimName != null && VoidMinerUtility.dropMapsByDimName.containsKey(targetDimName)) {
                this.canVoidMine = true;
                this.dropMap = VoidMinerUtility.dropMapsByDimName.get(targetDimName);
                this.extraDropMap = VoidMinerUtility.extraDropsByDimName
                    .getOrDefault(targetDimName, new VoidMinerUtility.DropMap());
                this.dropMap.isDistributionCached(this.extraDropMap);
                this.totalWeight = dropMap.getTotalWeight() + extraDropMap.getTotalWeight();
                return;
            }
        }

        // Fallback to actual dimension
        if (this.dimensionDef == null || !this.dimensionDef.canBeVoidMined()) return;

        this.canVoidMine = true;
        String actualDimName = this.dimensionDef.getDimensionName();
        this.dropMap = VoidMinerUtility.dropMapsByDimName
            .getOrDefault(actualDimName, new VoidMinerUtility.DropMap());
        this.extraDropMap = VoidMinerUtility.extraDropsByDimName
            .getOrDefault(actualDimName, new VoidMinerUtility.DropMap());
        this.dropMap.isDistributionCached(this.extraDropMap);
        this.totalWeight = dropMap.getTotalWeight() + extraDropMap.getTotalWeight();

        if (overrideAttempted) {
            if (this.totalWeight > 0) {
                VMTweak$warning = StatCollector.translateToLocal("gui.dimension.overwrite.failed");
            } else {
                VMTweak$warning = StatCollector.translateToLocal("gui.dimension.overwrite.error");
                this.dropMap = new VoidMinerUtility.DropMap();
            }
        }
    }

    @Unique
    private String VMTweak$checkNEIOreBlockDim() {
        String neiOreBlockDim = Optional.ofNullable(this.mInventory[1])
            .filter(s -> s.getItem() instanceof ItemDimensionDisplay)
            .map(ItemDimensionDisplay::getDimension)
            .orElse("None");
        if (!Objects.equals(neiOreBlockDim, VMTweak$mLastDimensionOverride)) {
            VMTweak$mLastDimensionOverride = neiOreBlockDim;
            totalWeight = 0;
        }
        return neiOreBlockDim;
    }

    @Inject(method = "saveNBTData", at = @At("HEAD"), require = 1, remap = false)
    public void saveNBTData(NBTTagCompound aNBT, CallbackInfo c) {
        aNBT.setString("VMTweak$mLastDimensionOverride", this.VMTweak$mLastDimensionOverride);
    }

    @Inject(method = "loadNBTData", at = @At("HEAD"), require = 1, remap = false)
    public void loadNBTData(NBTTagCompound aNBT, CallbackInfo c) {
        this.VMTweak$mLastDimensionOverride = aNBT.getString("VMTweak$mLastDimensionOverride");
    }

    @Inject(method = "working", at = @At("HEAD"), require = 1, remap = false)
    public void working(CallbackInfoReturnable<Boolean> c) {
        String prev = VMTweak$mLastDimensionOverride;
        VMTweak$checkNEIOreBlockDim();
        if (!Objects.equals(prev, VMTweak$mLastDimensionOverride)) {
            VMTweak$doCalculateDropMap();
        }
    }

    @Unique
    private String VMTweak$getGuiText() {
        String ext = null;
        try {
            Block block = ModBlocks.getBlock(VMTweak$mLastDimensionOverride);
            ext = new ItemStack(block).getDisplayName();
        } catch (Exception ignored) {}
        ext = ext == null ? VMTweak$mLastDimensionOverride : ext;
        ext = Objects.equals(VMTweak$mLastDimensionOverride, "EA")
            ? StatCollector.translateToLocal("gui.dimension.overwrite.EndAsteroids")
            : ext;
        ext = Objects.equals(ext, "None") ? ""
            : StatCollector.translateToLocal("gui.dimension.overwrite.description") + ext;
        if (!ext.isEmpty() && !VMTweak$warning.isEmpty() && this.dropMap != null && this.dropMap.getTotalWeight() <= 0)
            return VMTweak$warning;
        return ext;
    }

    @Override
    protected void drawTexts(DynamicPositionedColumn screenElements, SlotWidget inventorySlot) {
        super.drawTexts(screenElements, inventorySlot);
        screenElements.widget(
            TextWidget.dynamicString(this::VMTweak$getGuiText)
                .setSynced(false)
                .setTextAlignment(Alignment.CenterLeft)
                .setEnabled(true));
    }

    @Override
    public void addUIWidgets(ModularWindow.Builder builder, UIBuildContext buildContext) {
        super.addUIWidgets(builder, buildContext);
        builder.widget(new FakeSyncWidget.StringSyncer(() -> VMTweak$mLastDimensionOverride, s -> this.VMTweak$mLastDimensionOverride = s).setSynced(true, false));
        builder.widget(new FakeSyncWidget.StringSyncer(() -> VMTweak$warning, s -> this.VMTweak$warning = s).setSynced(true, false));
    }
}
