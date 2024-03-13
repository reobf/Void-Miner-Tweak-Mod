package reobf.vmtweak.main;

import java.io.File;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldSettings.GameType;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.storage.IPlayerFileData;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

public class DummyWorld extends World{
	 

	   public DummyWorld(String name,int id) {
	      super(new ISaveHandler(){

			@Override
			public WorldInfo loadWorldInfo() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void checkSessionLock() throws MinecraftException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public IChunkLoader getChunkLoader(WorldProvider p_75763_1_) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void saveWorldInfoWithPlayer(WorldInfo p_75755_1_, NBTTagCompound p_75755_2_) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void saveWorldInfo(WorldInfo p_75761_1_) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public IPlayerFileData getSaveHandler() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void flush() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public File getWorldDirectory() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public File getMapFileFromName(String p_75758_1_) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getWorldDirectoryName() {
				// TODO Auto-generated method stub
				return null;
			}}
	      
	      
	      , "DummyWorld", new WorldSettings(0, GameType.ADVENTURE, false, false,  WorldType .DEFAULT),
	    		  
	    		  
	    		  create(name,id), null);
	     
	    
	   }
public static WorldProvider create(String name,int id){
	return new WorldProvider(){
		{this.dimensionId=id;}
		@Override
		public String getDimensionName() {
			
			return name;
		}};
	
	
	
	
}
	@Override
	protected IChunkProvider createChunkProvider() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected int func_152379_p() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Entity getEntityByID(int p_73045_1_) {
		// TODO Auto-generated method stub
		return null;
	}
	 
	  

}
