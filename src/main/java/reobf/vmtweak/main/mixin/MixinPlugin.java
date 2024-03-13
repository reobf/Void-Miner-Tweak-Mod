package reobf.vmtweak.main.mixin;

import static java.nio.file.Files.walk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.minecraft.launchwrapper.Launch;
import reobf.vmtweak.Tags;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import cpw.mods.fml.relauncher.FMLLaunchHandler;

import ru.timeconqueror.spongemixins.MinecraftURLClassPath;

public class MixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getRefMapperConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // TODO Auto-generated method stub

    }

    @SuppressWarnings("unused")
    @Override
    public List<String> getMixins() {
     
        ArrayList<String> ret = new ArrayList<>();
        ret.add("Mixin1");
      
        return ret;

    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

}
