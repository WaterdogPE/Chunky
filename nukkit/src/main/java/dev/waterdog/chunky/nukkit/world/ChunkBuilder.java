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

package dev.waterdog.chunky.nukkit.world;

import cn.nukkit.level.format.ChunkSection;
import cn.nukkit.level.format.generic.BaseFullChunk;
import dev.waterdog.chunky.common.data.chunk.ChunkHolder;
import dev.waterdog.chunky.common.data.chunk.SubChunkHolder;
import dev.waterdog.chunky.common.palette.BlockPaletteLegacy;

public interface ChunkBuilder {

    BaseFullChunk buildChunk(BaseFullChunk chunk, ChunkHolder chunkHolder, BlockPaletteLegacy blockPalette);

    ChunkSection buildChunkSection(SubChunkHolder subChunkHolder, BlockPaletteLegacy blockPalette);
}
