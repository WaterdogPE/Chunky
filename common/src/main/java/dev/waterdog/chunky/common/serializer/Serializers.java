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

import com.nukkitx.protocol.bedrock.v388.Bedrock_v388;
import dev.waterdog.chunky.common.data.chunk.ChunkHolder;
import dev.waterdog.chunky.common.data.chunk.BlockStorage;
import dev.waterdog.chunky.common.palette.BlockPalette;
import dev.waterdog.chunky.common.serializer.chunk.ChunkSerializer338;
import dev.waterdog.chunky.common.serializer.subchunk.SubChunkSerializerV1;
import dev.waterdog.chunky.common.serializer.subchunk.SubChunkSerializerV3;
import dev.waterdog.chunky.common.serializer.subchunk.SubChunkSerializerV8;
import dev.waterdog.chunky.common.serializer.subchunk.SubChunkSerializerV7;
import io.netty.buffer.ByteBuf;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;

public class Serializers {

    public static final int MIN_SUBCHUNK_INDEX = 0;
    public static final int MAX_SUBCHUNK_INDEX = 15;
    public static final int LATEST_CHUNK_VERSION = Bedrock_v388.V388_CODEC.getProtocolVersion();
    public static final int LATEST_SUB_CHUNK_VERSION = 8;

    private static final IntObjectMap<ChunkSerializer> CHUNK_SERIALIZERS = new IntObjectHashMap<>();
    private static final IntObjectMap<SubChunkSerializer> SUB_CHUNK_SERIALIZERS = new IntObjectHashMap<>();

    static {
        // Chunk serializers
        CHUNK_SERIALIZERS.put(Bedrock_v388.V388_CODEC.getProtocolVersion(), ChunkSerializer338.INSTANCE);
        // SubChunk serializers
        SUB_CHUNK_SERIALIZERS.put(0, SubChunkSerializerV3.INSTANCE);
        SUB_CHUNK_SERIALIZERS.put(1, SubChunkSerializerV1.INSTANCE);
        SUB_CHUNK_SERIALIZERS.put(2, SubChunkSerializerV3.INSTANCE);
        SUB_CHUNK_SERIALIZERS.put(3, SubChunkSerializerV3.INSTANCE);
        SUB_CHUNK_SERIALIZERS.put(4, SubChunkSerializerV7.INSTANCE);
        SUB_CHUNK_SERIALIZERS.put(5, SubChunkSerializerV7.INSTANCE);
        SUB_CHUNK_SERIALIZERS.put(6, SubChunkSerializerV7.INSTANCE);
        SUB_CHUNK_SERIALIZERS.put(7, SubChunkSerializerV7.INSTANCE);
        SUB_CHUNK_SERIALIZERS.put(8, SubChunkSerializerV8.INSTANCE);
    }

    private static ChunkSerializer getChunkSerializer(int version) {
        ChunkSerializer chunkSerializer = CHUNK_SERIALIZERS.get(version);
        if (chunkSerializer == null) {
            chunkSerializer = CHUNK_SERIALIZERS.get(LATEST_CHUNK_VERSION);
            if (chunkSerializer == null) {
                throw new IllegalArgumentException("Invalid chunk serialize version " + version);
            }
        }
        return chunkSerializer;
    }

    public static void deserializeChunk(ByteBuf buffer, ChunkHolder chunkHolder, BlockPalette blockPalette, int version) {
        getChunkSerializer(version).deserialize(buffer, chunkHolder, blockPalette);
    }

    private static SubChunkSerializer getSubChunkSerializer(int version) {
        SubChunkSerializer chunkSerializer = SUB_CHUNK_SERIALIZERS.get(version);
        if (chunkSerializer == null) {
            throw new IllegalArgumentException("Invalid subChunk serialize version " + version);
        }
        return chunkSerializer;
    }

    public static BlockStorage[] deserializeSubChunk(ByteBuf buffer, ChunkHolder chunkHolder, BlockPalette blockPalette, int version) {
        return getSubChunkSerializer(version).deserialize(buffer, chunkHolder, blockPalette);
    }
}
