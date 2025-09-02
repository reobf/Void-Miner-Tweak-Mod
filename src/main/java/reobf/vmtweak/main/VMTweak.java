package reobf.vmtweak.main;

import java.util.*;

import galacticgreg.api.ModDimensionDef;
import galacticgreg.registry.GalacticGregRegistry;
import gtneioreplugin.util.DimensionHelper;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import org.apache.logging.log4j.Logger;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

@Mod(
    modid = "vmtweak",
    version = "0.0.2",
    name = "vmtweak",
    acceptableRemoteVersions = "*",
    acceptedMinecraftVersions = "[1.7.10]")
public class VMTweak {

    public static Logger LOGGER;

    public static BiMap<Integer, String> dimDirectMap = HashBiMap.create();
    public static HashMap<Integer, String> cache = new HashMap<>();

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog(); // 获取模组专用的Logger
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @Mod.EventHandler
    public void onModLoadingComplete(FMLLoadCompleteEvent event) {
        // 原版维度、暮色维度
        dimDirectMap.put(0, "Ow");
        dimDirectMap.put(-1, "Ne");
        dimDirectMap.put(1, "ED");
        dimDirectMap.put(7, "TF");
        // Ross128b 维度
        dimDirectMap.put(bartworks.common.configs.Configuration.crossModInteractions.ross128BAID, "Rb");
        dimDirectMap.put(bartworks.common.configs.Configuration.crossModInteractions.ross128BID, "Ra");
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load e) {
        int dimID = e.world.provider.dimensionId;
        if (dimDirectMap.containsKey(dimID)) return;
        try {
            String chunkProviderName = ((ChunkProviderServer) e.world.getChunkProvider())
                .currentChunkProvider.getClass()
                .getName();
            cache.put(dimID,chunkProviderName);
            if (chunkProviderName.equals("com.rwtema.extrautils.worldgen.Underdark.ChunkProviderUnderdark"))
                dimDirectMap.put(dimID, "DD");
            else {
                List<String> dimName = Arrays.asList(DimensionHelper.DimName);
                String name = GalacticGregRegistry.getModContainers()
                    .stream()
                    .flatMap(
                        modContainer -> modContainer.getDimensionList()
                            .stream())
                    .filter(s -> {
                            if (DimensionManager.getWorld(dimID) == null) return false;

                            return s.getChunkProviderName()
                                .equals(
                                    DimensionManager.getProvider(dimID)
                                        .createChunkGenerator()
                                        .getClass()
                                        .getName());
                        }
                    )
                    .map(ModDimensionDef::getDimIdentifier)
                    .findFirst()
                    .orElse(null);
                int index;
                if ((index = dimName.indexOf(name)) >= 0) {
                    dimDirectMap.forcePut(dimID, DimensionHelper.DimNameDisplayed[index]);
                }
            }
        } catch (Exception ignored) {}
    }

}
