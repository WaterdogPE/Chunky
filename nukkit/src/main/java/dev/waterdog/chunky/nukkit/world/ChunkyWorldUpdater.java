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

package dev.waterdog.chunky.nukkit.world;

import cn.nukkit.Player;
import cn.nukkit.block.BlockID;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.ChunkSection;
import cn.nukkit.level.format.generic.BaseChunk;
import cn.nukkit.level.format.generic.BaseFullChunk;

public class ChunkyWorldUpdater {

    private static final int NEGATIVE_SECTION = -4;
    private static final int MAX_REQUESTS_PER_TICK = 40;
    private static final int CHUNK_RADIUS = 2;
    public static final int CHUNK_POST_UPDATE_RADIUS = 10;

    private final ChunkyManager loader;

    public ChunkyWorldUpdater(ChunkyManager loader) {
        this.loader = loader;
        loader.getLevel().getServer().getScheduler()
                .scheduleRepeatingTask(this::doTick, 20, false);
    }

    public void doTick() {
        int count = 0;

        Level level = this.loader.getLevel();
        for (Player player : level.getPlayers().values()) {
            for (int x = -CHUNK_RADIUS; x < CHUNK_RADIUS; x++) {
                for (int z = -CHUNK_RADIUS; z < CHUNK_RADIUS; z++) {
                    if (count > MAX_REQUESTS_PER_TICK) {
                        return;
                    }

                    BaseFullChunk chunk = level.getChunk(player.getChunkX() + x, player.getChunkZ() + z);
                    if (chunk instanceof BaseChunk && chunk.isGenerated() && this.canChunkUpdate((BaseChunk) chunk)) {
                        this.loader.requestChunkUpdate(chunk.getX(), chunk.getZ());
                        count++;
                    }
                }
            }
        }
    }

    public boolean canChunkUpdate(BaseChunk chunk) {
        ChunkSection section = chunk.getSection(NEGATIVE_SECTION);
        if (section.isEmpty()) {
            return true;
        }
        return section.getBlockId(0, 2, 0) == BlockID.AIR;
    }
}
