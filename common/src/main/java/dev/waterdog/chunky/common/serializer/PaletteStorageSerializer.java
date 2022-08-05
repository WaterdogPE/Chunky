/*
 * Copyright 2022 WaterdogTEAM
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
import dev.waterdog.chunky.common.data.chunk.PaletteHolder;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class PaletteStorageSerializer {

    public static void deserializePalette(ByteBuf buffer, PaletteHolder storage) {
        storage.setPaletteHeader(buffer.readUnsignedByte());
        if (storage.isPersistent()) {
            throw new IllegalStateException("SubChunk version 8 does not support persistent storages over network!");
        }

        int paletteSize = 1;
        if (storage.getBitsPerBlock() != 0) {
            // storage is 16 * 16 * 16 large
            int blocksPerWord = Integer.SIZE / storage.getBitsPerBlock();
            int wordsCount = (4096 + blocksPerWord - 1) / blocksPerWord;
            int[] words = new int[wordsCount];
            for (int i = 0; i < wordsCount; i++) {
                words[i] = buffer.readIntLE();
            }
            storage.setWords(words);
            paletteSize = VarInts.readInt(buffer);
        }

        storage.setPalette(new IntArrayList());
        for (int i = 0; i < paletteSize; i++) {
            storage.getPalette().add(VarInts.readInt(buffer));
        }
    }
}
