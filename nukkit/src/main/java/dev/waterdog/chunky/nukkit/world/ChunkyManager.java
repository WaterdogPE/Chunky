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

import cn.nukkit.level.Level;
import cn.nukkit.level.format.LevelProvider;
import cn.nukkit.level.format.anvil.Anvil;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.scheduler.AsyncTask;
import cn.nukkit.scheduler.ServerScheduler;
import dev.waterdog.chunky.common.ChunkyListener;
import dev.waterdog.chunky.common.data.ChunkRequest;
import dev.waterdog.chunky.common.data.chunk.ChunkHolder;
import dev.waterdog.chunky.common.network.ChunkyClient;
import dev.waterdog.chunky.common.network.ChunkyPeer;
import dev.waterdog.chunky.nukkit.world.anvil.AnvilChunkBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ChunkyManager implements ChunkyListener, GeneratorTaskFactory {

    private static final Logger log = LogManager.getLogger("Chunky");

    private final ChunkyClient chunkyClient;
    private final Level level;

    public ChunkyManager(ChunkyClient chunkyClient, Level level) {
        this.chunkyClient = chunkyClient;
        this.level = level;
    }

    public void requestChunk(BaseFullChunk chunk) {
        log.info("Requesting chunk x={} z={}", chunk.getX(), chunk.getZ());
        this.chunkyClient.requestChunk(chunk.getX(), chunk.getZ()).whenComplete(((chunkHolder, error) -> {
            if (error != null) {
                log.error("Failed to generate chunk", error);
            } else {
                this.onChunkReceived(chunkHolder, chunk);
            }
        }));
    }

    @Override
    public void onUnhandledChunkReceived(ChunkHolder chunkHolder, ChunkyPeer peer) {
        this.getScheduler().scheduleTask(() -> {
           BaseFullChunk baseChunk = this.level.getChunk(chunkHolder.getChunkX(), chunkHolder.getChunkZ(), true);
           if (!baseChunk.isGenerated() || !baseChunk.isPopulated()) {
               this.getScheduler().scheduleTask(() -> this.onChunkReceived(chunkHolder, baseChunk), true);
           }
        });
    }

    private void onChunkReceived(ChunkHolder chunkHolder, BaseFullChunk baseChunk) {
        log.info("Received chunk x={} z={}", chunkHolder.getChunkX(), chunkHolder.getChunkZ());

        LevelProvider provider = this.level.getProvider();
        BaseFullChunk chunk;
        if (provider instanceof Anvil) {
            chunk = AnvilChunkBuilder.INSTANCE.buildChunk(baseChunk, chunkHolder, chunkHolder.getBlockPalette());
        } else {
            log.warn("[Chunky] Unsupported provider {}", provider.getClass().getSimpleName());
            return;
        }

        chunk.setGenerated();
        chunk.setPopulated();

        this.getScheduler().scheduleTask(() -> this.level.generateChunkCallback(chunkHolder.getChunkX(), chunkHolder.getChunkZ(), chunk));
    }

    @Override
    public void onChunkRequestTimeout(ChunkRequest request, ChunkyPeer peer) {

    }

    private ServerScheduler getScheduler() {
        return this.level.getServer().getScheduler();
    }

    @Override
    public AsyncTask populateChunkTask(BaseFullChunk chunk, Level level) {
        return new ChunkyGeneratorTask(this, chunk);
    }

    @Override
    public AsyncTask generateChunkTask(BaseFullChunk chunk, Level level) {
        return new ChunkyGeneratorTask(this, chunk);
    }
}
