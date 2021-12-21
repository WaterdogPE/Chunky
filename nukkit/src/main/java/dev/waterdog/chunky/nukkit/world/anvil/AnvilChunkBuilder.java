/*
 * Copyright 2021 WaterdogTEAM
 * Licensed under the GNU General Public License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.waterdog.chunky.nukkit.world.anvil;

import cn.nukkit.block.BlockID;
import cn.nukkit.level.format.anvil.Chunk;
import cn.nukkit.level.format.anvil.ChunkSection;
import cn.nukkit.level.format.anvil.util.BlockStorage;
import cn.nukkit.level.format.anvil.util.NibbleArray;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.level.util.BitArray;
import cn.nukkit.level.util.BitArrayVersion;
import dev.waterdog.chunky.common.data.chunk.ChunkyBlockStorage;
import dev.waterdog.chunky.common.data.chunk.ChunkHolder;
import dev.waterdog.chunky.common.data.chunk.SubChunkHolder;
import dev.waterdog.chunky.common.palette.BlockPaletteLegacy;
import dev.waterdog.chunky.nukkit.world.ChunkBuilder;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class AnvilChunkBuilder implements ChunkBuilder {
    public static final AnvilChunkBuilder INSTANCE = new AnvilChunkBuilder();

    @Override
    public BaseFullChunk buildChunk(BaseFullChunk chunk, ChunkHolder chunkHolder, BlockPaletteLegacy blockPalette) {
        if (!(chunk instanceof Chunk)) {
            throw new IllegalArgumentException("Provided chunk is not anvil!");
        }

        for (SubChunkHolder subChunkHolder : chunkHolder.getSubChunks()) {
            if (subChunkHolder.getStorages().length < 1 || subChunkHolder.getStorages()[0] == null) {
                throw new IllegalArgumentException("Chunk section has 0 block storages!");
            }

            ChunkyBlockStorage storage = subChunkHolder.getStorages()[0];
            BlockStorage anvilStorage;
            if (storage.isLegacy()) {
                anvilStorage = new BlockStorage(storage.getBlockIds(), new NibbleArray(storage.getBlockData()));
            } else {
                BitArrayVersion version = BitArrayVersion.get(storage.getBitsPerBlock(), true);
                BitArray bitArray = version.createPalette(PalettedBlockStorage.SIZE, storage.getWords());
                PalettedBlockStorage palettedStorage = new PalettedBlockStorage(bitArray, storage.getPalette());
                anvilStorage = this.convertStorages(palettedStorage, blockPalette);
            }

            ChunkSection section = new ChunkSection(subChunkHolder.getY(), anvilStorage, null, null, null, false, false);
            ((Chunk) chunk).setSection(subChunkHolder.getY(), section);
        }

        chunk.setBiomeIdArray(chunkHolder.getBiomeData());
        return chunk;
    }

    private BlockStorage convertStorages(PalettedBlockStorage palletedStorage, BlockPaletteLegacy blockPalette) {
        BlockStorage storage = new BlockStorage();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 16; y++) {
                    int runtimeId = palletedStorage.getBlock(x, y, z);
                    int fullId = blockPalette.runtimeId2LegacyFullId(runtimeId);
                    if (fullId == -1) {
                        storage.setBlockId(x, y, z, BlockID.INFO_UPDATE);
                    } else {
                        int blockId = fullId >> 6;
                        int meta = fullId & 0xf;
                        if (blockId >= 0 && blockId < 256) {
                            storage.setBlockId(x, y, z, blockId);
                            storage.setBlockData(x, y, z, meta);
                        } else {
                            storage.setBlockId(x, y, z, BlockID.INFO_UPDATE);
                        }
                    }
                }
            }
        }
        return storage;
    }
}
