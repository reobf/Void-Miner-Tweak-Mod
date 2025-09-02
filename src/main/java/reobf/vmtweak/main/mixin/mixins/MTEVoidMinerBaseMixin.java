package reobf.vmtweak.main.mixin.mixins;

import java.util.Objects;
import java.util.Optional;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraft.world.gen.ChunkProviderServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizons.modularui.api.math.Alignment;
import com.gtnewhorizons.modularui.common.widget.DynamicPositionedColumn;
import com.gtnewhorizons.modularui.common.widget.SlotWidget;
import com.gtnewhorizons.modularui.common.widget.TextWidget;

import bwcrossmod.galacticgreg.MTEVoidMinerBase;
import bwcrossmod.galacticgreg.VoidMinerUtility;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gtneioreplugin.plugin.block.ModBlocks;
import gtneioreplugin.plugin.item.ItemDimensionDisplay;
import reobf.vmtweak.main.VMTweak;

@Mixin(MTEVoidMinerBase.class)
public abstract class MTEVoidMinerBaseMixin<T extends MTEVoidMinerBase<T>> extends MTEEnhancedMultiBlockBase<T> {

    public MTEVoidMinerBaseMixin(String aName) {
        super(aName);
    }

    @Shadow(remap = false)
    private float totalWeight;

    @Shadow(remap = false)
    private VoidMinerUtility.DropMap dropMap;

    @Unique
    private String VMTweak$warning = "";

    @Unique
    private String VMTweak$mLastDimensionOverride = "None";

    // 有些mod会在这个方法里直接用id来判断维度添加产物。
    @ModifyVariable(method = "handleExtraDrops", at = @At("HEAD"), require = 1, remap = false, argsOnly = true)
    private int handleExtraDrops(int id) {
        return VMTweak.dimDirectMap.inverse()
            .getOrDefault(VMTweak$checkNEIOreBlockDim(), id);
    }

    // 这个方法首先会根据id来判断是否有对应的掉落辞典，如果没有的话则会通过chunkProviderName查找它给自己创建的映射
    @Inject(method = "handleModDimDef", at = @At("HEAD"), require = 1, remap = false, cancellable = true)
    private void handleModDimDef(int id, CallbackInfo ci) {
        ci.cancel();

        // 判断能否覆写维度id
        VMTweak$warning = "";
        int dimId = VMTweak.dimDirectMap.inverse()
            .getOrDefault(VMTweak$checkNEIOreBlockDim(), id);
        if (VMTweak.dimDirectMap.inverse()
            .containsKey(VMTweak$checkNEIOreBlockDim())) {
            if (VoidMinerUtility.dropMapsByDimId.containsKey(dimId)) {
                this.dropMap = VoidMinerUtility.dropMapsByDimId.get(dimId);
                return;
            }
        } else if (Objects.equals(VMTweak$mLastDimensionOverride, "EA")) {
            dimId = 1;
            if (VoidMinerUtility.dropMapsByDimId.containsKey(dimId)) {
                this.dropMap = VoidMinerUtility.dropMapsByDimId.get(dimId);
                return;
            }
        }
        if (VMTweak.cache.containsKey(dimId)) {
            String chunkProviderName = VMTweak.cache.get(dimId);
            if (VoidMinerUtility.dropMapsByChunkProviderName.containsKey(chunkProviderName)) {
                VoidMinerUtility.DropMap tempDropMap = VoidMinerUtility.dropMapsByChunkProviderName
                    .get(chunkProviderName);
                if (tempDropMap.getTotalWeight() > 0) {
                    this.dropMap = tempDropMap;
                    return;
                }
            }
        }

        // 如果不能覆写维度id，则使用原本的逻辑检测当前维度
        if (VoidMinerUtility.dropMapsByDimId.containsKey(id)) {
            this.dropMap = VoidMinerUtility.dropMapsByDimId.get(id);
            VMTweak$warning = StatCollector.translateToLocal("gui.dimension.overwrite.failed");
            return;
        } else {
            String chunkProviderName = null;
            if (this.getBaseMetaTileEntity() != null) {
                chunkProviderName = ((ChunkProviderServer) this.getBaseMetaTileEntity()
                    .getWorld()
                    .getChunkProvider()).currentChunkProvider.getClass()
                        .getName();
            }

            if (VoidMinerUtility.dropMapsByChunkProviderName.containsKey(chunkProviderName)) {
                this.dropMap = VoidMinerUtility.dropMapsByChunkProviderName.get(chunkProviderName);
                VMTweak$warning = StatCollector.translateToLocal("gui.dimension.overwrite.failed");
                return;
            }
        }

        // 如果当前维度也没有对应的掉落辞典，则清空掉落辞典，并提示
        VMTweak$warning = StatCollector.translateToLocal("gui.dimension.overwrite.error");
        this.dropMap = new VoidMinerUtility.DropMap();
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
        VMTweak$checkNEIOreBlockDim();
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
                .setSynced(true)
                .setTextAlignment(Alignment.CenterLeft)
                .setEnabled(true));
    }
}
