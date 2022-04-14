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

import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.LevelProvider;
import cn.nukkit.level.format.anvil.Anvil;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.level.generator.GeneratorTaskFactory;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.scheduler.AsyncTask;
import cn.nukkit.scheduler.ServerScheduler;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NBTOutputStream;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtUtils;
import dev.waterdog.chunky.common.ChunkyListener;
import dev.waterdog.chunky.common.data.ChunkRequest;
import dev.waterdog.chunky.common.data.chunk.ChunkHolder;
import dev.waterdog.chunky.common.network.ChunkyClient;
import dev.waterdog.chunky.common.network.ChunkyPeer;
import dev.waterdog.chunky.nukkit.world.anvil.AnvilChunkBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.List;


public class ChunkyManager implements ChunkyListener, GeneratorTaskFactory {
    private static final Logger log = LogManager.getLogger("Chunky");

    private final ChunkyClient chunky;
    private final Level level;

    public ChunkyManager(ChunkyClient chunkyClient, Level level) {
        this.chunky = chunkyClient;
        this.level = level;
    }

    public void requestChunk(BaseFullChunk chunk) {
        log.info("Requesting chunk x={} z={}", chunk.getX(), chunk.getZ());
        this.chunky.requestChunk(chunk.getX(), chunk.getZ()).whenComplete(((chunkHolder, error) -> {
            if (error != null) {
                log.error("Failed to generate chunk", error);
            } else {
                this.onChunkReceived(chunkHolder, chunk);
            }
        }));
    }

    private void requestChunkInternal(int chunkX, int chunkZ) {
        BaseFullChunk baseChunk = this.level.getChunk(chunkX, chunkZ, true);
        if (!baseChunk.isGenerated() || !baseChunk.isPopulated()) {
            this.requestChunk(baseChunk);
        }
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

        List<CompoundTag> blockEntities = BlockEntityLoader.get().loadBlockEntities(chunk, chunkHolder, this.chunky.getMinecraftVersion());
        this.getScheduler().scheduleTask(() -> {
            this.level.generateChunkCallback(chunkHolder.getChunkX(), chunkHolder.getChunkZ(), chunk);
            if (blockEntities != null) {
                BaseFullChunk fullChunk = this.level.getChunk(chunkHolder.getChunkX(), chunkHolder.getChunkZ(), false);
                for (CompoundTag nbt : blockEntities) {
                    BlockEntityLoader.get().spawnBlockEntity(fullChunk, nbt);
                }
            }
        });
    }

    @Override
    public void onBlockEntityUpdate(Vector3i position, NbtMap nbt) {
        try {
            CompoundTag compoundTag = convertNbtMap(nbt);
            compoundTag.putInt("x", position.getX());
            compoundTag.putInt("y", position.getY());
            compoundTag.putInt("z", position.getZ());
            this.getScheduler().scheduleTask(() -> this.createBlockEntity(position, compoundTag));
        } catch (IOException e) {
            log.error("Failed to convert NbtMap", e);
        }
    }

    private void createBlockEntity(Vector3i position, CompoundTag nbt) {
        BlockEntity blockEntity = this.level.getBlockEntity(new Vector3(position.getX(), position.getY(), position.getZ()));
        if (blockEntity != null && blockEntity.isValid()) {
            return;
        }
        BaseFullChunk chunk = this.level.getChunkIfLoaded(position.getX(), position.getZ());
        CompoundTag compoundTag = BlockEntityLoader.get().createBlockEntityNBT(nbt.getString("id"), nbt, chunk);
        if (compoundTag != null) {
            BlockEntityLoader.get().spawnBlockEntity(chunk, compoundTag);
        }
    }

    @Override
    public void onChunkRequestTimeout(ChunkRequest request, ChunkyPeer peer) {
        log.warn("Chunk request x={} z={} timed out", request.getChunkX(), request.getChunkZ());
        if (!peer.canRequestChunks()) {
            // Peer was closed or is reconnecting
            this.getScheduler().scheduleDelayedTask(() -> this.requestChunkInternal(request.getChunkX(), request.getChunkZ()), 15);
        }
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

    public static CompoundTag convertNbtMap(NbtMap nbt) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try (NBTOutputStream nbtOutputStream = NbtUtils.createWriter(stream)){
            nbtOutputStream.writeTag(nbt);
        } finally {
            stream.close();
        }
        return NBTIO.read(stream.toByteArray(), ByteOrder.BIG_ENDIAN, false);
    }
}
