package reobf.vmtweak.main.mixin.mixins;

import java.util.Objects;
import java.util.Optional;

import gtneioreplugin.plugin.block.ModBlocks;
import gtneioreplugin.plugin.item.ItemDimensionDisplay;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

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
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import reobf.vmtweak.main.VMTweak;

@Mixin(MTEVoidMinerBase.class)
public abstract class MTEVoidMinerBaseMixin<T extends MTEVoidMinerBase<T>> extends MTEEnhancedMultiBlockBase<T> {

    @Shadow(remap = false)
    private float totalWeight;

    public MTEVoidMinerBaseMixin(String aName) {
        super(aName);

    }

    @ModifyVariable(method = "handleExtraDrops", at = @At("HEAD"), require = 1, remap = false, argsOnly = true)
    private int handleExtraDrops(int id) {
        return VMTweak.dimMapping.inverse()
            .getOrDefault(void_Miner_Tweak_Mod$a(), id);
    }

    @ModifyVariable(method = "handleModDimDef", at = @At("HEAD"), require = 1, remap = false, argsOnly = true)
    private int handleModDimDef(int id) {
        return void_Miner_Tweak_Mod$dim = VMTweak.dimMapping.inverse()
            .getOrDefault(void_Miner_Tweak_Mod$a(), id);
    }

    @Unique
    private int void_Miner_Tweak_Mod$dim;

    @ModifyVariable(method = "handleModDimDef", at = @At("STORE"), require = 1, remap = false)
    private String handleModDimDef0(String id) {
        return VMTweak.cache.getOrDefault(void_Miner_Tweak_Mod$dim, id);
    }

    @Unique
    private String void_Miner_Tweak_Mod$a() {

        return Optional.ofNullable(this.mInventory[1])
            .filter(s -> s.getItem() instanceof ItemDimensionDisplay)
            .map(ItemDimensionDisplay::getDimension)
            .orElse("None")

        ;
    }

    @Unique
    private String void_Miner_Tweak_Mod$mLastDimensionOverride = "None";

    @Inject(method = "saveNBTData", at = @At("HEAD"), require = 1, remap = false)
    public void saveNBTData(NBTTagCompound aNBT, CallbackInfo c) {

        aNBT.setString("mLastDimensionOverride", this.void_Miner_Tweak_Mod$mLastDimensionOverride);
    }

    @Inject(method = "loadNBTData", at = @At("HEAD"), require = 1, remap = false)
    public void loadNBTData(NBTTagCompound aNBT, CallbackInfo c) {

        this.void_Miner_Tweak_Mod$mLastDimensionOverride = aNBT.getString("mLastDimensionOverride");
    }

    @Inject(method = "working", at = @At("HEAD"), require = 1, remap = false)
    public void working(CallbackInfoReturnable<Boolean> c) {
        String dim = Optional.ofNullable(this.mInventory[1])
            .filter(s -> s.getItem() instanceof ItemDimensionDisplay)
            .map(ItemDimensionDisplay::getDimension)
            .orElse("None")

        ;

        if (!Objects.equals(dim, void_Miner_Tweak_Mod$mLastDimensionOverride)) {

            void_Miner_Tweak_Mod$mLastDimensionOverride = dim;
            totalWeight = 0;
            // System.out.println("set "+totalWeight+" "+dim);
        }
    }

    @Unique
    private String void_Miner_Tweak_Mod$get() {
        String ext = null;
        try {
            Block block = ModBlocks.getBlock(void_Miner_Tweak_Mod$mLastDimensionOverride);
            ext = new ItemStack(block).getDisplayName();
        } catch (Exception ignored) {}

        return "Dimension Override:" + (ext == null ? void_Miner_Tweak_Mod$mLastDimensionOverride : ext)

        ;

    }

    @Override
    protected void drawTexts(DynamicPositionedColumn screenElements, SlotWidget inventorySlot) {
        super.drawTexts(screenElements, inventorySlot);
        screenElements.widget(
            TextWidget.dynamicString(this::void_Miner_Tweak_Mod$get)
                .setSynced(true)
                .setTextAlignment(Alignment.CenterLeft)
                .setEnabled(true));
    }
}
