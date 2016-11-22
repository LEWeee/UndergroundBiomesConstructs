/*
 */

package exterminatorjeff.undergroundbiomes.world;

import exterminatorjeff.undergroundbiomes.api.API;
import exterminatorjeff.undergroundbiomes.api.StrataLayer;
import exterminatorjeff.undergroundbiomes.api.UBBiome;
import exterminatorjeff.undergroundbiomes.api.UBStrataColumn;
import exterminatorjeff.undergroundbiomes.api.UBStrataColumnProvider;
import exterminatorjeff.undergroundbiomes.common.block.UBStone;
import exterminatorjeff.undergroundbiomes.config.UBConfig;
import exterminatorjeff.undergroundbiomes.intermod.OresRegistry;
import exterminatorjeff.undergroundbiomes.world.noise.NoiseGenerator;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public abstract class UBStoneReplacer implements UBStrataColumnProvider {

	final UBBiome[] biomeList;
        final NoiseGenerator noiseGenerator;
        
        public UBStoneReplacer(UBBiome [] biomeList, NoiseGenerator noiseGenerator) {
            this.biomeList = biomeList;
            this.noiseGenerator = noiseGenerator;
            if (biomeList == null) throw new RuntimeException();
            if (noiseGenerator == null) throw new RuntimeException();
        }
        public abstract int [] getBiomeValues(Chunk chunk);

	public void replaceStoneInChunk(Chunk chunk) {
            int[] biomeValues = getBiomeValues(chunk);
            int xPos = chunk.xPosition * 16;
            int zPos = chunk.zPosition * 16;

		// For each storage array
		for (ExtendedBlockStorage storage : chunk.getBlockStorageArray()) {
			if (storage != null && !storage.isEmpty()) {
				int yPos = storage.getYLocation();
				if (yPos >= UBConfig.SPECIFIC.generationHeight())
					return;
				//
				for (int x = 0; x < 16; ++x) {
					for (int z = 0; z < 16; ++z) {
						// Get the underground biome for the position
						UBBiome currentBiome = biomeList[biomeValues[x *16 + z]];
                                                if (currentBiome == null) throw new RuntimeException(
                                                        ""+biomeValues[x *16 + z]);
						//
						// Perlin noise for strata layers height variation
					        int variation = (int) (noiseGenerator.noise((xPos + x) / 55.533, (zPos + z) / 55.533, 3, 1, 0.5) * 10 - 5);
						for (int y = 0; y < 16; ++y) {
                                                    IBlockState currentBlockState = storage.get(x, y, z);
							Block currentBlock = currentBlockState.getBlock();
							/*
							 * Skip air, water and UBStone
							 */
							if (Block.isEqualTo(Blocks.AIR, currentBlock))
								continue;
							if (Block.isEqualTo(Blocks.WATER, currentBlock))
								continue;
							// TODO Test without
							if (currentBlock instanceof UBStone)
								continue;
							/*
							 * Stone
							 */
							if (currentBlock == Blocks.STONE) {
								// Replace with UBified version
								storage.set(x, y, z, currentBiome.getStrataBlockAtLayer(yPos + y + variation));
							} else {
								/*
								 * Ore
								 */
									IBlockState strata = currentBiome.getStrataBlockAtLayer(yPos + y + variation);
								if (OresRegistry.INSTANCE.isUBified(strata.getBlock(),currentBlockState)) {
									if (strata.getBlock() instanceof UBStone) {
										UBStone stone = ((UBStone) strata.getBlock());
										IBlockState ore = OresRegistry.INSTANCE.getUBifiedOre(strata.getBlock(), stone.getMetaFromState(strata),currentBlockState);
										storage.set(x, y, z, ore);
									}
								} 
							}
						}
					}
				}
			}
		}
	}

    abstract public UBBiome UBBiomeAt(int x, int z);
    
    private UBStrataColumn strataColumn(
            final StrataLayer[] strata,
            final IBlockState fillerBlockCodes,
            final int variation) {
        return new UBStrataColumn() {


            public IBlockState stone(int y){
                for(int i = 0; i < strata.length; i++){
                    if(strata[i].heightInLayer(y+variation) == true){
                        return strata[i].filler;
                    }
                }
                return fillerBlockCodes;
            }

            public IBlockState cobblestone(int height){
                IBlockState stone = stone(height);
                if (stone.getBlock() == API.IGNEOUS_STONE.getBlock() ) {
                    return API.IGNEOUS_COBBLE.getBlock().getStateFromMeta(stone.getBlock().getMetaFromState(stone));
                }
                if (stone.getBlock() == API.METAMORPHIC_STONE.getBlock() ) {
                    return API.METAMORPHIC_COBBLE.getBlock().getStateFromMeta(stone.getBlock().getMetaFromState(stone));
                }
                return stone;
            }

            public IBlockState cobblestone(){
                IBlockState stone = stone();
                if (stone.getBlock() == API.IGNEOUS_STONE.getBlock() ) {
                    return API.IGNEOUS_COBBLE.getBlock().getStateFromMeta(stone.getBlock().getMetaFromState(stone));
                }
                if (stone.getBlock() == API.METAMORPHIC_STONE.getBlock() ) {
                    return API.METAMORPHIC_COBBLE.getBlock().getStateFromMeta(stone.getBlock().getMetaFromState(stone));
                }
                return stone;
            }

            public IBlockState stone(){
                return fillerBlockCodes;
            }
        };
    }

    public UBStrataColumn strataColumn(int x, int z) {
        // make sure we have the right chunk
        UBBiome biome = UBBiomeAt(x, z);
	int variation = (int) (noiseGenerator.noise((x) / 55.533, (z) / 55.533, 3, 1, 0.5) * 10 - 5);
        return strataColumn(biome.strata, biome.filler, variation);
    }
}