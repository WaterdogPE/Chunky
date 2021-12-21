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

package dev.waterdog.chunky.common.serializer.subchunk;

import dev.waterdog.chunky.common.data.chunk.ChunkyBlockStorage;
import dev.waterdog.chunky.common.data.chunk.ChunkHolder;
import dev.waterdog.chunky.common.palette.BlockPalette;
import dev.waterdog.chunky.common.palette.BlockPaletteLegacy;
import dev.waterdog.chunky.common.serializer.SubChunkSerializer;
import io.netty.buffer.ByteBuf;

public class SubChunkSerializerV3 implements SubChunkSerializer {
    public static final SubChunkSerializerV3 INSTANCE = new SubChunkSerializerV3();

    @Override
    public ChunkyBlockStorage[] deserialize(ByteBuf buffer, ChunkHolder chunkHolder, BlockPalette blockPalette) {
        if (!(blockPalette instanceof BlockPaletteLegacy)) {
            throw new IllegalArgumentException("Cannot deserialize legacy chunk storage with runtime palette: " + blockPalette.getClass().getSimpleName());
        }

        byte[] blockIds = new byte[4096];
        buffer.readBytes(blockIds);

        byte[] blockData = new byte[2048];
        buffer.readBytes(blockData);

        buffer.skipBytes(4096); // blockLight

        ChunkyBlockStorage[] storages = new ChunkyBlockStorage[2];
        ChunkyBlockStorage storage = new ChunkyBlockStorage();
        storage.setLegacy(true);
        storage.setBlockIds(blockIds);
        storage.setBlockData(blockData);
        storages[0] = storage;
        return storages;
    }
}
