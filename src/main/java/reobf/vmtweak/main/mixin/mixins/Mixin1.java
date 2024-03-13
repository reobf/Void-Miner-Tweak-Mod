package reobf.vmtweak.main.mixin.mixins;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import pers.gwyog.gtneioreplugin.plugin.item.ItemDimensionDisplay;
import reobf.vmtweak.main.Callback;
import reobf.vmtweak.main.DummyWorld;
import reobf.vmtweak.main.MyMod;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Surrogate;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.bartimaeusnek.crossmod.galacticgreg.GT_TileEntity_VoidMiner_Base;

import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.common.GT_Worldgen_GT_Ore_Layer;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_DrillerBase;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import scala.actors.threadpool.Arrays;




@Mixin(GT_TileEntity_VoidMiner_Base.class)
public abstract class Mixin1 extends GT_MetaTileEntity_DrillerBase {

  public Mixin1(String aName) {
		super(aName);
	
	}


 @ModifyVariable(method = "calculateDropMap", at = @At("STORE"), require = 1,remap=false)
    public int calculateDropMap( int i) {
    	
       return Callback.calculateDropMap(this, i);
    }
    
    @Inject(method = "makeModDimDef", at = @At("HEAD"), require = 1, cancellable = true,remap=false)
    public void makeModDimDef( CallbackInfoReturnable c) {
   
    	Callback.makeModDimDef(this, c);
    }
    
    private String mLastDimensionOverride="";
   
    @Inject(method = "saveNBTData", at = @At("HEAD"), require = 1,remap=false)
    public void saveNBTData(NBTTagCompound aNBT, CallbackInfo c) {
       
        aNBT.setString("mLastDimensionOverride", this.mLastDimensionOverride);
    }

    @Inject(method = "loadNBTData", at = @At("HEAD"), require = 1, remap=false)
    public void loadNBTData(NBTTagCompound aNBT, CallbackInfo c) {
        
        this.mLastDimensionOverride = aNBT.getString("mLastDimensionOverride");
    }
    
    
    @Inject(method = "workingAtBottom", at = @At("HEAD"), require = 1, remap=false)
    public void workingAtBottom(ItemStack aStack, int xDrill, int yDrill, int zDrill, int xPipe, int zPipe,
            int yHead, int oldYHead, CallbackInfoReturnable c) {
    	String dim= Optional.ofNullable(
				this.mInventory[1])
			 .filter(s->s.getItem() instanceof ItemDimensionDisplay )
			 .map(s->ItemDimensionDisplay.getDimension(s))
			.orElse("None")
			 
			 ;
    	
    	if(!Objects.equals(dim, mLastDimensionOverride)){
    		
    		mLastDimensionOverride=dim;
    		totalWeight=0;
    		System.out.println("set"+totalWeight+ dim);
    	};
    	
    	
    }

    
    @Shadow
    private float totalWeight;
   
    
   private static DummyWorld Ow=new DummyWorld("Overworld",0);
   private  static DummyWorld Ne=new DummyWorld("Nether",-1);
   private  static DummyWorld ED=new DummyWorld("The End",1);
   private  static DummyWorld TW=new DummyWorld("Twilight Forest",7);
    @ModifyVariable(method = "makeOreLayerPredicate", at = @At("STORE"), require = 1,remap=false)
    public World makeOreLayerPredicate(World w) {
    	String dim= Optional.ofNullable(
				this.mInventory[1])
			 .filter(s->s.getItem() instanceof ItemDimensionDisplay )
			 .map(s->ItemDimensionDisplay.getDimension(s))
			.orElse("None")
			 ;
    	switch (dim){
    	case "Ne":return Ne;
    	case "Ow":return Ow;
    	case "ED":return ED;
    	case "TW":return TW;
    
    	}
    	
    
    	return w;
     }

}
