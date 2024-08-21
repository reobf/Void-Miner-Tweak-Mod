package reobf.vmtweak.main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerUseItemEvent;
import net.minecraftforge.event.world.WorldEvent;
import pers.gwyog.gtneioreplugin.plugin.block.ModBlocks;
import pers.gwyog.gtneioreplugin.plugin.item.ItemDimensionDisplay;
import pers.gwyog.gtneioreplugin.util.DimensionHelper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import bloodasp.galacticgreg.api.ModDimensionDef;
import bloodasp.galacticgreg.registry.GalacticGregRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;
import cpw.mods.fml.relauncher.Side;


@Mod(modid = "vmtweak", version = "0.0.2", name = "vmtweak",
acceptableRemoteVersions="*",


acceptedMinecraftVersions = "[1.7.10]"

)
public class MyMod {
@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		
		  MinecraftForge.EVENT_BUS.register(this);
		  FMLCommonHandler.instance().bus().register(this);
		   }	public static List<String> dimName=Arrays.asList(DimensionHelper.DimName);
			public static List<String> dimNameShort=Arrays.asList(DimensionHelper.DimNameDisplayed);
			
	
public static BiMap<Integer, String> dimMapping=HashBiMap.create();
public static HashMap<Integer,String> cahce=new HashMap<>();
	

	@SubscribeEvent
	public  void a(WorldEvent.Load e){
		for(int i:DimensionManager.getStaticDimensionIDs()){
			if(dimMapping.containsKey(i))continue;
			String name=getNameForID(i);
			
			int index;
			if((index=dimName.indexOf(name))>=0){
				dimMapping.forcePut(i, DimensionHelper.DimNameDisplayed[index]);
				//dimDefMapping.forcePut(def, DimensionHelper.DimNameDisplayed[index]);
			};
		}
		
		
		try{
		cahce.put(e.world.provider.dimensionId,
		((ChunkProviderServer)e.world.getChunkProvider()).currentChunkProvider.getClass().getName());
		}catch(Exception ee){}
	
	
	}
	
	
	
	
	 
	public static String getNameForID(int id){
		if(id==com.github.bartimaeusnek.bartworks.common.configs.ConfigHandler.ross128BID){
			return "Ross128b";
		}
		if(id==com.github.bartimaeusnek.bartworks.common.configs.ConfigHandler.ross128BAID){
			return "Ross128ba";
		}
		if(id==0){
		return "Overworld";
		}
		if(id==-1){
			return "Nether";
		}
		if(id==7){
			return "Twilight";
		}if(id==1){
			return "TheEnd";}
		
		
	return	GalacticGregRegistry.getModContainers().stream()
		.flatMap(modContainer -> modContainer.getDimensionList().stream())
	
		.filter(s->{
			 
				
						if(DimensionManager.getWorld(id)==null)return false;
						
							return s.getChunkProviderName().equals(
		DimensionManager.getProvider(id).createChunkGenerator().getClass().getName());
		
		
		
		}
		
				
				
				)	.map(s->s.getDimIdentifier())
		.findFirst().orElse(null)
		
		
		;	
		
		
		
		
		
	}
	
	
	
	
	
	
}
