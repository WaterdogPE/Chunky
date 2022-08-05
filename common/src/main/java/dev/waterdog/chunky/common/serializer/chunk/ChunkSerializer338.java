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

package dev.waterdog.chunky.common.serializer.chunk;

import dev.waterdog.chunky.common.data.chunk.ChunkHolder;
import dev.waterdog.chunky.common.data.chunk.ChunkyBlockStorage;
import dev.waterdog.chunky.common.data.chunk.SubChunkHolder;
import dev.waterdog.chunky.common.palette.BlockPalette;
import dev.waterdog.chunky.common.serializer.ChunkSerializer;
import dev.waterdog.chunky.common.serializer.Serializers;
import io.netty.buffer.ByteBuf;

import static dev.waterdog.chunky.common.serializer.Serializers.*;

public class ChunkSerializer338 implements ChunkSerializer {
    public static final ChunkSerializer338 INSTANCE = new ChunkSerializer338();

    @Override
    public void deserialize(ByteBuf buffer, ChunkHolder chunkHolder, BlockPalette blockPalette) {
        SubChunkHolder[] subChunks = new SubChunkHolder[chunkHolder.getSubChunksLength()];
        for (int y = MIN_SUBCHUNK_INDEX, i = 0; y < MAX_SUBCHUNK_INDEX && i < chunkHolder.getSubChunksLength() ;y++, i++) {
            int subChunkVersion = buffer.readUnsignedByte();
            ChunkyBlockStorage[] storages = Serializers.deserializeSubChunk(buffer, chunkHolder, blockPalette, subChunkVersion);
            subChunks[y] = new SubChunkHolder(y, storages);
        }
        chunkHolder.setSubChunks(subChunks);

        this.readLevelData(buffer, chunkHolder);
    }

    @Override
    public void readLevelData(ByteBuf buffer, ChunkHolder chunkHolder) {
        this.readBiomeData(buffer, chunkHolder);

        short borderBlocksSize = buffer.readUnsignedByte();
        buffer.skipBytes(borderBlocksSize); // 1 byte per borderBlock

        byte[] blockEntities = new byte[buffer.readableBytes()];
        buffer.readBytes(blockEntities);
        chunkHolder.setBlockEntities(blockEntities);
    }

    @Override
    public void readBiomeData(ByteBuf buffer, ChunkHolder chunkHolder) {
        byte[] biomeData = new byte[256];
        buffer.readBytes(biomeData);
        chunkHolder.setBiomeData(biomeData);
    }
}
