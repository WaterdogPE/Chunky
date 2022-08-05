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

package dev.waterdog.chunky.common.serializer.chunk;

import dev.waterdog.chunky.common.data.chunk.ChunkHolder;
import dev.waterdog.chunky.common.data.chunk.PaletteHolder;
import dev.waterdog.chunky.common.serializer.PaletteStorageSerializer;
import io.netty.buffer.ByteBuf;

public class ChunkSerializer475 extends ChunkSerializer338 {

    public static final ChunkSerializer475 INSTANCE = new ChunkSerializer475();

    @Override
    public void readBiomeData(ByteBuf buffer, ChunkHolder chunkHolder) {
        PaletteHolder[] biomes = new PaletteHolder[chunkHolder.getSubChunksLength()];
        for (int i = 0; i < biomes.length; i++) {
            PaletteHolder palette = this.readPalettedBiomes(buffer);
            if (palette == null && i == 0) {
                throw new IllegalStateException("First biome palette can not point to previous!");
            }

            if (palette == null) {
                palette = biomes[i - 1].clone();
            }
            biomes[i] = palette;
        }

        chunkHolder.setPalettedBiomes(biomes);
    }

    private PaletteHolder readPalettedBiomes(ByteBuf buffer) {
        int index = buffer.readerIndex();
        int size = buffer.readUnsignedByte() >> 1;
        if (size == 127) {
            // This means this paletted storage had the flag pointing to the previous one
            return null;
        }

        buffer.readerIndex(index);
        PaletteHolder palette = new PaletteHolder();
        PaletteStorageSerializer.deserializePalette(buffer, palette);
        return palette;
    }
}
