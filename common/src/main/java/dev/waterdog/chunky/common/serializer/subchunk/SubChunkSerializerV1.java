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
import dev.waterdog.chunky.common.data.chunk.ChunkyBlockStorage;
import dev.waterdog.chunky.common.data.chunk.ChunkHolder;
import dev.waterdog.chunky.common.palette.BlockPalette;
import dev.waterdog.chunky.common.serializer.SubChunkSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.io.IOException;

public class SubChunkSerializerV1 implements SubChunkSerializer {
    public static final SubChunkSerializerV1 INSTANCE = new SubChunkSerializerV1();

    @Override
    public ChunkyBlockStorage[] deserialize(ByteBuf buffer, ChunkHolder chunkHolder, BlockPalette blockPalette) {
        ChunkyBlockStorage storage = new ChunkyBlockStorage();
        storage.setLegacy(false);
        storage.setPaletteHeader(buffer.readUnsignedByte());
        // storage is 16 * 16 * 16 large
        int blocksPerWord = Integer.SIZE / storage.getBitsPerBlock();
        int wordsCount = (4096 + blocksPerWord - 1) / blocksPerWord;
        if (!storage.isPersistent()) {
            throw new IllegalStateException("SubChunk version 1 does not support runtime storages over network!");
        }

        int[] words = new int[wordsCount];
        for (int i = 0; i < wordsCount; i++) {
            words[i] = buffer.readIntLE();
        }
        storage.setWords(words);

        int paletteSize = buffer.readIntLE();
        storage.setPalette(new IntArrayList());
        try (ByteBufInputStream stream = new ByteBufInputStream(buffer);
             NBTInputStream nbtInputStream = NbtUtils.createReaderLE(stream)) {
            for (int i = 0; i < paletteSize; i++) {
                int runtimeId = blockPalette.state2RuntimeId((NbtMap) nbtInputStream.readTag());
                storage.getPalette().add(runtimeId);
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot read persistent block palette", e);
        }

        ChunkyBlockStorage[] storages = new ChunkyBlockStorage[2];
        storages[0] = storage;
        return storages;
    }
}
