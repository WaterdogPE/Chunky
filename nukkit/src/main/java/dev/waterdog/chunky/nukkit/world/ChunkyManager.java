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
import cn.nukkit.level.format.ChunkSection;
import cn.nukkit.level.format.LevelProvider;
import cn.nukkit.level.format.anvil.Anvil;
import cn.nukkit.level.format.generic.BaseChunk;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.level.generator.GeneratorTaskFactory;
import cn.nukkit.level.util.PalettedBlockStorage;
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
import dev.waterdog.chunky.common.data.chunk.SubChunkHolder;
import dev.waterdog.chunky.common.network.ChunkyClient;
import dev.waterdog.chunky.common.network.ChunkyPeer;
import dev.waterdog.chunky.nukkit.world.anvil.AnvilChunkBuilder;
import io.netty.util.internal.PlatformDependent;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.jodah.expiringmap.ExpiringMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static dev.waterdog.chunky.common.util.ChunkUtils.*;
import static dev.waterdog.chunky.nukkit.world.ChunkyWorldUpdater.CHUNK_POST_UPDATE_RADIUS;


public class ChunkyManager implements ChunkyListener, GeneratorTaskFactory {
    private static final Logger log = LogManager.getLogger("Chunky");

    private final ChunkyClient chunky;
    private final Level level;

    private final ChunkyWorldUpdater worldUpdater;
    private final Queue<Long> chunkUpdateRequests = PlatformDependent.newMpscQueue(100);
    private final Set<Long> lastRequests = Collections.newSetFromMap(ExpiringMap.builder()
            .expiration(120, TimeUnit.SECONDS)
            .build());

    public ChunkyManager(ChunkyClient chunkyClient, Level level, boolean worldUpdates) {
        this.chunky = chunkyClient;
        this.level = level;
        if (worldUpdates) {
            this.worldUpdater = new ChunkyWorldUpdater(this);
            level.getServer().getScheduler().scheduleRepeatingTask(this::tickChunkUpdates, 20, true);
        } else {
            this.worldUpdater = null;
        }
    }

    public void requestChunk(BaseFullChunk chunk) {
        log.info("Requesting chunk x={} z={}", chunk.getX(), chunk.getZ());
        this.lastRequests.add(chunkIndex(chunk.getX(), chunk.getZ()));
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

    public void requestChunkUpdate(int chunkX, int chunkZ) {
        long index = chunkIndex(chunkX, chunkZ);
        if (!this.chunkUpdateRequests.contains(index)) {
            this.chunkUpdateRequests.offer(index);
        }
    }

    private void requestChunkUpdateInternal(int chunkX, int chunkZ) {
        log.info("Requesting chunk update x={} z={}", chunkX, chunkZ);
        this.chunky.requestChunk(chunkX, chunkZ).whenComplete(((chunkHolder, error) -> {
            if (error != null) {
                log.error("Failed to update chunk", error);
            } else {
                this.onChunkUpdateReceived(chunkHolder);
            }
        }));
    }

    public void tickChunkUpdates() {
        int requests = 0;
        while (!this.chunkUpdateRequests.isEmpty() && requests < 10) {
            long index = this.chunkUpdateRequests.poll();
            this.requestChunkUpdateInternal(chunkX(index), chunkZ(index));
            requests++;
        }
    }

    @Override
    public void onUnhandledChunkReceived(ChunkHolder chunkHolder, ChunkyPeer peer) {
        this.getScheduler().scheduleTask(() -> {
           BaseFullChunk baseChunk = this.level.getChunk(chunkHolder.getChunkX(), chunkHolder.getChunkZ(), true);
           if (!baseChunk.isGenerated() || !baseChunk.isPopulated()) {
               this.getScheduler().scheduleTask(() -> this.onChunkReceived(chunkHolder, baseChunk), true);
           } else if (baseChunk.isGenerated() && this.worldUpdater != null && this.worldUpdater.canChunkUpdate((BaseChunk) baseChunk)) {
               this.getScheduler().scheduleTask(() -> this.onChunkUpdateReceived(chunkHolder), true);
           }
        });
    }

    private void onChunkReceived(ChunkHolder chunkHolder, BaseFullChunk baseChunk) {
        log.info("Received chunk x={} z={}", chunkHolder.getChunkX(), chunkHolder.getChunkZ());

        ChunkBuilder chunkBuilder = this.getChunkBuilder();
        if (chunkBuilder == null) {
            return;
        }

        BaseFullChunk chunk = chunkBuilder.buildChunk(baseChunk, chunkHolder, chunkHolder.getBlockPalette());;
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

    private void onChunkUpdateReceived(ChunkHolder chunkHolder) {
        log.info("Received chunk update x={} z={}", chunkHolder.getChunkX(), chunkHolder.getChunkZ());
        if (!this.level.isChunkGenerated(chunkHolder.getChunkX(), chunkHolder.getChunkZ())) {
            return;
        }

        ChunkBuilder chunkBuilder = this.getChunkBuilder();
        if (chunkBuilder == null) {
            return;
        }

        boolean wasNeighbourGenerated = false;
        for (int x = -CHUNK_POST_UPDATE_RADIUS; x < CHUNK_POST_UPDATE_RADIUS && !wasNeighbourGenerated; x++) {
            for (int z = -CHUNK_POST_UPDATE_RADIUS; z < CHUNK_POST_UPDATE_RADIUS; z++) {
                long index = chunkIndex(chunkHolder.getChunkX() + x, chunkHolder.getChunkZ() + z);
                if (this.lastRequests.contains(index)) {
                    wasNeighbourGenerated = true;
                    log.info("Doing full chunk update at x={} z={}", chunkHolder.getChunkX(), chunkHolder.getChunkZ());
                    break;
                }
            }
        }

        Int2ObjectMap<ChunkSection> sections = new Int2ObjectOpenHashMap<>();
        for (SubChunkHolder subChunk : chunkHolder.getSubChunks()) {
            if (wasNeighbourGenerated || subChunk.getY() <= 0) {
                ChunkSection section = chunkBuilder.buildChunkSection(subChunk, chunkHolder.getBlockPalette());
                sections.put(subChunk.getY(), section);
            }
        }

        this.getScheduler().scheduleTask(() -> {
            BaseChunk chunk = (BaseChunk) this.level.getChunk(chunkHolder.getChunkX(), chunkHolder.getChunkZ(), false);
            if (chunk == null) {
                return;
            }

            sections.forEach(chunk::setSection);
            this.level.setChunk(chunkHolder.getChunkX(), chunkHolder.getChunkZ(), chunk, false);
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
        if (chunk == null) {
            return;
        }

        CompoundTag compoundTag = BlockEntityLoader.get().createBlockEntityNBT(nbt.getString("id"), nbt, chunk);
        if (compoundTag != null) {
            BlockEntityLoader.get().spawnBlockEntity(chunk, compoundTag);
        }
    }

    @Override
    public void onChunkRequestTimeout(ChunkRequest request, ChunkyPeer peer) {
        log.info("Chunk request x={} z={} timed out", request.getChunkX(), request.getChunkZ());
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

    public ChunkBuilder getChunkBuilder() {
        LevelProvider provider = this.level.getProvider();
        if (provider instanceof Anvil) {
            return AnvilChunkBuilder.INSTANCE;
        } else {
            log.warn("[Chunky] Unsupported provider {}", provider.getClass().getSimpleName());
        }
        return null;
    }

    public Level getLevel() {
        return this.level;
    }
}
