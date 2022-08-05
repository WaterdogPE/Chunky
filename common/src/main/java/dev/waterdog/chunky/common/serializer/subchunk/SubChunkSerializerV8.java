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

import com.nukkitx.nbt.NBTInputStream;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtUtils;
import dev.waterdog.chunky.common.data.chunk.ChunkHolder;
import dev.waterdog.chunky.common.data.chunk.ChunkyBlockStorage;
import dev.waterdog.chunky.common.palette.BlockPalette;
import dev.waterdog.chunky.common.serializer.PaletteStorageSerializer;
import dev.waterdog.chunky.common.serializer.SubChunkSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;

import java.io.IOException;
import java.util.List;

public class SubChunkSerializerV8 implements SubChunkSerializer {
    public static final SubChunkSerializerV8 INSTANCE = new SubChunkSerializerV8();

    @Override
    public ChunkyBlockStorage[] deserialize(ByteBuf buffer, ChunkHolder chunkHolder, BlockPalette blockPalette) {
        int storagesCount = buffer.readUnsignedByte();
        return this.deserialize(buffer, storagesCount, chunkHolder, blockPalette);
    }

    protected ChunkyBlockStorage[] deserialize(ByteBuf buffer, int storagesCount, ChunkHolder chunkHolder, BlockPalette blockPalette) {
        ChunkyBlockStorage[] storages = new ChunkyBlockStorage[storagesCount];

        for (int y = 0; y < storagesCount; y++) {
            ChunkyBlockStorage storage = new ChunkyBlockStorage();
            storage.setLegacy(false);
            PaletteStorageSerializer.deserializePalette(buffer, storage);
            storages[y] = storage;
        }
        return storages;
    }

    private void readPersistentState(ByteBuf buffer, int paletteSize, List<NbtMap> palette) {
        try (ByteBufInputStream stream = new ByteBufInputStream(buffer);
             NBTInputStream nbtInputStream = NbtUtils.createReaderLE(stream)) {
            for (int i = 0; i < paletteSize; i++) {
                palette.add((NbtMap) nbtInputStream.readTag());
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot read persistent block palette", e);
        }
    }
}
