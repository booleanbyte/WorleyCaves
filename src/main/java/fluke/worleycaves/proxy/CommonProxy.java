package fluke.worleycaves.proxy;

import java.util.function.Consumer;

import fluke.worleycaves.world.WorldCarverWorley;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.feature.ProbabilityConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;

public class CommonProxy
{
	private WorldCarverWorley worleyCarver;
	private ConfiguredCarver<ProbabilityConfig> configuredWorleyCarver;
	private long worldSeed;
	private boolean seedsSet = false;
	
	public void start()
	{
		IEventBus fmlBus = FMLJavaModLoadingContext.get().getModEventBus();
		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		
		worleyCarver = new WorldCarverWorley(ProbabilityConfig::deserialize, 256);
		configuredWorleyCarver = Biome.createCarver(worleyCarver, new ProbabilityConfig(1));
		
		registerListeners(fmlBus, forgeBus);
	}
	
	public void registerListeners(IEventBus fmlBus, IEventBus forgeBus) 
	{
        fmlBus.addListener(this::commonSetup);

        forgeBus.addListener(this::worldLoad);
        forgeBus.addListener(this::worldCreateSpawn);
        forgeBus.addListener(this::worldUnload);
    }
	
	public void commonSetup(FMLCommonSetupEvent event)
	{
		ForgeRegistries.BIOMES.forEach(new Consumer<Biome>()
		{
			@Override
			public void accept(Biome b)
			{
				// Exclude Nether and End biomes
				if (b.getCategory() == Biome.Category.NETHER || b.getCategory() == Biome.Category.THEEND)
					return;

				b.getCarvers(GenerationStage.Carving.AIR).clear();
				b.getCarvers(GenerationStage.Carving.LIQUID).clear();
				b.getCarvers(GenerationStage.Carving.LIQUID).add(configuredWorleyCarver);
			}
		});
	}
	
	public void worldLoad(WorldEvent.Load event)
	{
		setWorldSeed(event);
	}
	
	public void worldCreateSpawn(WorldEvent.CreateSpawnPosition event)
	{
		setWorldSeed(event);
	}
	
	public void worldUnload(WorldEvent.Unload event) 
	{
		//if player quits world make sure we reset seed
		if(event.getWorld().getDimension().isSurfaceWorld())
		{
			seedsSet = false;
			worldSeed = 0;
		}
	}
	
	public void setWorldSeed(WorldEvent event)
	{
		if(seedsSet)
			return;
		
		long seed = event.getWorld().getSeed();
		
		if(seed != 0)
			worldSeed = seed;
		
		worleyCarver.init(worldSeed);
	}

}