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

package dev.waterdog.chunky.common.serializer;

import com.nukkitx.network.VarInts;
import dev.waterdog.chunky.common.data.ChunkHolder;
import dev.waterdog.chunky.common.data.PalettedStorage;
import dev.waterdog.chunky.common.data.SubChunkHolder;
import io.netty.buffer.ByteBuf;

import static dev.waterdog.chunky.common.serializer.Serializers.*;

public class ChunkSerializer_338 implements ChunkSerializer {
    public static final ChunkSerializer_338 INSTANCE = new ChunkSerializer_338();

    @Override
    public void deserialize(ByteBuf buffer, ChunkHolder chunkHolder) {
        SubChunkHolder[] subChunks = new SubChunkHolder[chunkHolder.getSubChunksLength()];
        for (int y = MIN_SUBCHUNK_INDEX; y < chunkHolder.getSubChunksLength(); y++) {
            int subChunkVersion = buffer.readUnsignedByte();
            PalettedStorage[] storages = Serializers.deserializeSubChunk(buffer, chunkHolder, subChunkVersion);
            subChunks[y] = new SubChunkHolder(y, storages);
        }

        // TODO: we don't support 3D biomes so far
        byte[] biomeData = new byte[256];
        buffer.readBytes(biomeData);

        short borderBlocksSize = buffer.readUnsignedByte();
        buffer.skipBytes(borderBlocksSize); // 1 byte per borderBlock

        int extraDataSize = VarInts.readInt(buffer); // extraData count
        // TODO: we are probably going to read this

        byte[] blockEntities = new byte[buffer.readableBytes()];
        buffer.readBytes(blockEntities);
    }
}
