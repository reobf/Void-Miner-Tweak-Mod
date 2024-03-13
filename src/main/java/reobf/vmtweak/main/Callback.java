package reobf.vmtweak.main;

import java.util.Optional;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import bloodasp.galacticgreg.api.ModDimensionDef;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_DrillerBase;
import pers.gwyog.gtneioreplugin.plugin.item.ItemDimensionDisplay;

public class Callback {
	  public static  int calculateDropMap(GT_MetaTileEntity_DrillerBase b, int i) {
	    	String dim= Optional.ofNullable(
	    				b.mInventory[1])
	    			 .filter(s->s.getItem() instanceof ItemDimensionDisplay )
	    			 .map(s->ItemDimensionDisplay.getDimension(s))
	    			.orElse(null)
	    			 
	    			 ;
	       if(dim!=null){
	    	return MyMod.dimMapping.inverse().getOrDefault(dim, 9999);
	    	
	    	}
	       
	       return i;
	    }
	  
	  
	  public static  void makeModDimDef( GT_MetaTileEntity_DrillerBase b,CallbackInfoReturnable<ModDimensionDef> c) {
		   
      	String dim= Optional.ofNullable(
      				b.mInventory[1])
      			 .filter(s->s.getItem() instanceof ItemDimensionDisplay )
      			 .map(s->ItemDimensionDisplay.getDimension(s))
      			.orElse(null)
      			 
      			 ;
         if(dim!=null){
      
      	c.setReturnValue( MyMod.dimDefMapping.inverse().getOrDefault(dim, null));
      	}
  }
}
