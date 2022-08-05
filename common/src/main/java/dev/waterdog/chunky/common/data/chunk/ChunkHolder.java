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

package dev.waterdog.chunky.common.data.chunk;

import dev.waterdog.chunky.common.palette.BlockPaletteLegacy;
import io.netty.buffer.ByteBuf;
import lombok.Data;

@Data
public class ChunkHolder {
    private final int chunkX;
    private final int chunkZ;
    private final int subChunksLength;
    private final BlockPaletteLegacy blockPalette;

    private SubChunkHolder[] subChunks;
    private byte[] biomeData;
    private PaletteHolder[] palettedBiomes;
    private byte[] blockEntities;

    public boolean hasAllSubChunks() {
        for (SubChunkHolder subChunk : this.subChunks) {
            if (subChunk == null) {
                return false;
            }
        }
        return true;
    }

    public void addBlockEntities(ByteBuf buffer) {
        int size = this.blockEntities == null ? 0 : this.blockEntities.length;
        byte[] blockEntities = new byte[size + buffer.readableBytes()];

        if (size > 0) {
            System.arraycopy(this.blockEntities, 0, blockEntities, 0, size);
        }
        buffer.readBytes(blockEntities, size, buffer.readableBytes());
        this.blockEntities = blockEntities;
    }
}
