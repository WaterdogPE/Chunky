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

import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.StringTag;
import dev.waterdog.chunky.common.data.chunk.ChunkHolder;
import dev.waterdog.chunky.common.network.MinecraftVersion;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.List;

public class BlockEntityLoader {

    private static final Logger log = LogManager.getLogger("Chunky");
    private static final BlockEntityLoader INSTANCE = new BlockEntityLoader();

    private final Object2ObjectMap<String, BlockEntityFactory> factories = new Object2ObjectOpenHashMap<>();

    public static BlockEntityLoader get() {
        return INSTANCE;
    }

    public BlockEntityLoader() {
        this.registerFactory(BlockEntity.CHEST, BlockEntityFactory.VOID_FACTORY);
        this.registerFactory(BlockEntity.MOB_SPAWNER, BlockEntityFactory.VOID_FACTORY);
    }

    public void registerFactory(String identifier, BlockEntityFactory factory) {
        if (this.factories.containsKey(identifier)) {
            throw new IllegalArgumentException("BlockEntity factory is already registered: " + identifier);
        }
        this.factories.put(identifier, factory);
    }

    public CompoundTag createBlockEntityNBT(String identifier, CompoundTag nbt, BaseFullChunk chunk) {
        if (!nbt.contains("x") || !nbt.contains("y") || !nbt.contains("z")) {
            return null;
        }

        BlockEntityFactory factory = this.factories.get(identifier);
        if (factory == null) {
            return null;
        }

        return factory.createServerNbt(nbt, chunk);
    }

    public BlockEntity spawnBlockEntity(BaseFullChunk chunk, CompoundTag nbt) {
        if (!nbt.contains("id")) {
            return null;
        }

        if ((nbt.getInt("x") >> 4) != chunk.getX() || ((nbt.getInt("z") >> 4) != chunk.getZ())) {
            return null;
        }

        String identifier = nbt.getString("id").replaceFirst("BlockEntity", "");
        BlockEntity blockEntity =  BlockEntity.createBlockEntity(identifier, chunk, nbt);
        if (blockEntity != null && blockEntity.isValid()) {
            log.info("Spawned BlockEntity {} at x={} y={} z={}", blockEntity.getName(), blockEntity.getX(), blockEntity.getY(), blockEntity.getZ());
        }
        return blockEntity;
    }

    public List<CompoundTag> loadBlockEntities(BaseFullChunk chunk, ChunkHolder chunkHolder, MinecraftVersion version) {
        if (chunkHolder.getBlockEntities() == null || chunkHolder.getBlockEntities().length < 1) {
            return null;
        }

        List<CompoundTag> blockEntities = new ObjectArrayList<>();
        try (InputStream stream = new ByteArrayInputStream(chunkHolder.getBlockEntities())) {
            while (stream.available() > 0) {
                CompoundTag nbt = NBTIO.read(stream, ByteOrder.LITTLE_ENDIAN, true);
                if (nbt.contains("id") && nbt.get("id") instanceof StringTag) {
                    CompoundTag compoundTag = this.createBlockEntityNBT(nbt.getString("id"), nbt, chunk);
                    if (compoundTag != null) blockEntities.add(compoundTag);
                }
            }
        } catch (IOException e) {
            log.error("Exception was thrown while reading BlockEntity nbt x=" + chunkHolder.getChunkX() + " z=" + chunkHolder.getChunkZ(), e);
        }

        return blockEntities;
    }
}
