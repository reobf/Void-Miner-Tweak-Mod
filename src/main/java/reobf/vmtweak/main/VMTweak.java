package reobf.vmtweak.main;

import java.lang.reflect.Field;
import java.util.*;

import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import micdoodle8.mods.galacticraft.api.galaxies.CelestialBody;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import gtneioreplugin.util.DimensionHelper;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = "vmtweak",
    version = "0.0.2",
    name = "vmtweak",
    acceptableRemoteVersions = "*",
    acceptedMinecraftVersions = "[1.7.10]")
public class VMTweak {

    public static Logger LOGGER;

    public static List<String> dimName = Arrays.asList(DimensionHelper.DimName);
    public static List<String> dimNameTrimmed = Arrays.asList(DimensionHelper.DimNameTrimmed);
    public static List<String> dimNameShort = Arrays.asList(DimensionHelper.DimNameDisplayed);
    public static HashMap<Integer, String> dimMap = new HashMap<>();
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
        dimDirectMap.put(getRoss128bDimID(false), "Rb");
        dimDirectMap.put(getRoss128bDimID(true), "Ra");
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load e) {
        if (dimDirectMap.containsKey(e.world.provider.dimensionId)) return;
        try {
            // 访问当前已经拥有 DimID 的维度，获取他们对应的 名字、ID和ChunkProvider。
            Class<?> clazz = DimensionManager.class;
            Field providersField = clazz.getDeclaredField("providers");
            providersField.setAccessible(true);
            @SuppressWarnings("unchecked") Hashtable<Integer, Class<? extends WorldProvider>> providers =
                (Hashtable<Integer, Class<? extends WorldProvider>>) providersField.get(null);
            // 遍历所有WorldProvider
            for (Map.Entry<Integer, Class<? extends WorldProvider>> entry : providers.entrySet()) {
                int dimID = entry.getKey();
                Class<? extends WorldProvider> providerClass = entry.getValue();
                if (dimMap.containsKey(dimID) || dimDirectMap.containsKey(dimID)) continue;
                switch (providerClass.getName()) {
                    case "com.rwtema.extrautils.worldgen.Underdark.WorldProviderUnderdark":
                        dimMap.put(dimID, "Underdark");
                        cache.put(dimID, "com.rwtema.extrautils.worldgen.Underdark.ChunkProviderUnderdark");
                        continue;
                    case "toxiceverglades.world.WorldProviderMod":
                        dimMap.put(dimID, "ToxicEverglades");
                        cache.put(dimID, "toxiceverglades.chunk.ChunkProviderModded");
                        continue;
                    case "me.eigenraven.personalspace.world.PersonalWorldProvider":
                        dimMap.put(dimID, "PersonalWorld");
                        cache.put(dimID, "me.eigenraven.personalspace.world.PersonalChunkProvider");
                        continue;
                    case "appeng.spatial.StorageWorldProvider":
                        dimMap.put(dimID, "StorageWorld");
                        cache.put(dimID, "appeng.spatial.StorageChunkProvider.StorageChunkProvider");
                        continue;
                    case "com.rwtema.extrautils.worldgen.endoftime.WorldProviderEndOfTime":
                        dimMap.put(dimID, "EndOfTime");
                        cache.put(dimID, "com.rwtema.extrautils.worldgen.endoftime.ChunkProviderEndOfTime");
                        continue;
                }
                // 星系维度自动获取
                WorldProvider provider = providerClass.newInstance();
                @SuppressWarnings("unchecked") String chunkProviderName =
                    ((Class<? extends IChunkProvider>)providerClass
                        .getMethod("getChunkProviderClass")
                        .invoke(provider))
                        .getName();
                String dimName = ((CelestialBody)providerClass
                        .getMethod("getCelestialBody")
                        .invoke(provider)).getName();
                // 获取chunkProviderClass的name
                dimMap.put(dimID, dimName);
                cache.put(dimID, chunkProviderName);
            }

        } catch (Exception exception) {
            LOGGER.error("Error loading world providers: ", exception);
        }
        for (Map.Entry<Integer, String> dimMap : dimMap.entrySet()) {
            // 将dimID和dimNameShort加入dimDirectMap
            int dimID = dimMap.getKey();
            String dimNameMap = dimMap.getValue();
            for (int i = 0; i < dimNameTrimmed.size(); i++) {
                if (dimNameMap.equalsIgnoreCase(dimNameTrimmed.get(i)) ||
                    dimNameMap.equalsIgnoreCase(dimName.get(i))) {
                    dimDirectMap.put(dimID, dimNameShort.get(i));
                    break;
                }
            }
        }
    }

    protected static int getRoss128bDimID(boolean isRoss128ba) {
        return isRoss128ba ?
            bartworks.common.configs.Configuration.crossModInteractions.ross128BID :
            bartworks.common.configs.Configuration.crossModInteractions.ross128BAID;
    }

}
