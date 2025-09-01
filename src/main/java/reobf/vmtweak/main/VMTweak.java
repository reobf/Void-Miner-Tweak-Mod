package reobf.vmtweak.main;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import galacticgreg.api.ModDimensionDef;
import galacticgreg.registry.GalacticGregRegistry;
import gtneioreplugin.util.DimensionHelper;

@Mod(
    modid = "vmtweak",
    version = "0.0.2",
    name = "vmtweak",
    acceptableRemoteVersions = "*",
    acceptedMinecraftVersions = "[1.7.10]")
public class VMTweak {

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {

        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    public static List<String> dimName = Arrays.asList(DimensionHelper.DimName);
//    public static List<String> dimNameShort = Arrays.asList(DimensionHelper.DimNameDisplayed);

    public static BiMap<Integer, String> dimMapping = HashBiMap.create();
    public static HashMap<Integer, String> cache = new HashMap<>();

    @SubscribeEvent
    public void a(WorldEvent.Load e) {
        for (int i : DimensionManager.getStaticDimensionIDs()) {
            if (dimMapping.containsKey(i)) continue;
            String name = getNameForID(i);

            int index;
            if ((index = dimName.indexOf(name)) >= 0) {
                dimMapping.forcePut(i, DimensionHelper.DimNameDisplayed[index]);
                // dimDefMapping.forcePut(def, DimensionHelper.DimNameDisplayed[index]);
            }
        }

        try {
            cache.put(
                e.world.provider.dimensionId,
                ((ChunkProviderServer) e.world.getChunkProvider()).currentChunkProvider.getClass()
                    .getName());
        } catch (Exception ignored) {}

    }

    public static String getNameForID(int id) {
        if (id == bartworks.common.configs.Configuration.crossModInteractions.ross128BID) {
            return "Ross128b";
        }
        if (id == bartworks.common.configs.Configuration.crossModInteractions.ross128BAID) {
            return "Ross128ba";
        }
        if (id == 0) {
            return "Overworld";
        }
        if (id == -1) {
            return "Nether";
        }
        if (id == 7) {
            return "Twilight";
        }
        if (id == 1) {
            return "TheEnd";
        }

        return GalacticGregRegistry.getModContainers()
            .stream()
            .flatMap(
                modContainer -> modContainer.getDimensionList()
                    .stream())

            .filter(s -> {

                if (DimensionManager.getWorld(id) == null) return false;

                return s.getChunkProviderName()
                    .equals(
                        DimensionManager.getProvider(id)
                            .createChunkGenerator()
                            .getClass()
                            .getName());

            }

            )
            .map(ModDimensionDef::getDimIdentifier)
            .findFirst()
            .orElse(null)

        ;

    }

}
