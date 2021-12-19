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

import com.nukkitx.nbt.NBTInputStream;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.network.VarInts;
import dev.waterdog.chunky.common.data.ChunkHolder;
import dev.waterdog.chunky.common.data.PalettedStorage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.IOException;
import java.util.List;

public class SubChunkSerializerV8 implements SubChunkSerializer {
    public static final SubChunkSerializerV8 INSTANCE = new SubChunkSerializerV8();

    @Override
    public PalettedStorage[] deserialize(ByteBuf buffer, ChunkHolder chunkHolder) {
        int storagesCount = buffer.readUnsignedByte();
        PalettedStorage[] storages = new PalettedStorage[storagesCount];

        for (int y = 0; y < storagesCount; y++) {
            PalettedStorage storage = new PalettedStorage();
            storage.setPaletteHeader(buffer.readUnsignedByte());
            // storage is 16 * 16 * 16 large
            int blocksPerWord = Integer.SIZE / storage.getBitsPerBlock();
            int wordsCount = (4096 + blocksPerWord - 1) / blocksPerWord;

            // 1 byte per word
            byte[] words = new byte[wordsCount];
            buffer.readBytes(words);
            storage.setWords(words);

            int paletteSize = VarInts.readInt(buffer);
            if (storage.isPersistent()) {
                List<NbtMap> persistentPalette = new ObjectArrayList<>();
                readPersistentState(buffer, paletteSize, persistentPalette);
                storage.setPersistentPalette(persistentPalette);
            } else {
                IntList runtimePalette = new IntArrayList();
                for (int i = 0; i < paletteSize; i++) {
                    runtimePalette.add(VarInts.readInt(buffer));
                }
                storage.setRuntimePalette(runtimePalette);
            }
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
