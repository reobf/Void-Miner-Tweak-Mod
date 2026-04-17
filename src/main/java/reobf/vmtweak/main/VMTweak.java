package reobf.vmtweak.main;

import java.util.HashMap;
import java.util.List;

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

/**
 * Main mod class for Void Miner Tweak.
 * <p>
 * Maintains a mapping of dimension IDs to their abbreviation names used by
 * the GT NEI Ore Plugin. This mapping is populated at load time for known
 * dimensions and dynamically expanded when new worlds are loaded.
 */
@Mod(
    modid = "vmtweak",
    version = "0.0.3",
    name = "Void Miner Tweak",
    acceptableRemoteVersions = "*",
    acceptedMinecraftVersions = "[1.7.10]")
public class VMTweak {

    public static Logger LOGGER;

    /** Bidirectional map: dimension ID ↔ abbreviation (e.g. 0 ↔ "Ow"). */
    public static final BiMap<Integer, String> dimDirectMap = HashBiMap.create();

    /** Cache of dimension ID → chunk provider class name for debug/fallback purposes. */
    public static final HashMap<Integer, String> chunkProviderCache = new HashMap<>();

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
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
        // Register well-known vanilla and modded dimensions
        dimDirectMap.put(0, "Ow");
        dimDirectMap.put(-1, "Ne");
        dimDirectMap.put(1, "ED");
        dimDirectMap.put(7, "TF");

        // Ross128 dimensions from BartWorks
        try {
            dimDirectMap.put(bartworks.common.configs.Configuration.crossModInteractions.ross128BAID, "Rb");
            dimDirectMap.put(bartworks.common.configs.Configuration.crossModInteractions.ross128BID, "Ra");
        } catch (NoClassDefFoundError ignored) {
            // BartWorks not present — skip
        }
    }

    /**
     * When a world loads, attempt to identify its dimension abbreviation by
     * matching its chunk provider against GalacticGreg's registered dimension
     * definitions.
     */
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        int dimID = event.world.provider.dimensionId;
        if (dimDirectMap.containsKey(dimID)) return;

        try {
            String chunkProviderName = ((ChunkProviderServer) event.world.getChunkProvider())
                .currentChunkProvider.getClass()
                .getName();
            chunkProviderCache.put(dimID, chunkProviderName);

            if ("com.rwtema.extrautils.worldgen.Underdark.ChunkProviderUnderdark".equals(chunkProviderName)) {
                dimDirectMap.put(dimID, "DD");
                return;
            }

            List<String> dimNames = DimensionHelper.ALL_DIM_NAMES;
            String matchedName = GalacticGregRegistry.getModContainers()
                .stream()
                .flatMap(container -> container.getDimensionList().stream())
                .filter(def -> {
                    if (DimensionManager.getWorld(dimID) == null) return false;
                    return def.getChunkProviderName()
                        .equals(
                            DimensionManager.getProvider(dimID)
                                .createChunkGenerator()
                                .getClass()
                                .getName());
                })
                .map(ModDimensionDef::getDimIdentifier)
                .findFirst()
                .orElse(null);

            if (matchedName != null) {
                int index = dimNames.indexOf(matchedName);
                if (index >= 0) {
                    dimDirectMap.forcePut(dimID, DimensionHelper.ALL_DISPLAYED_NAMES.get(index));
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to identify dimension {} chunk provider", dimID, e);
        }
    }
}
