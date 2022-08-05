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

package dev.waterdog.chunky.common;

import dev.waterdog.chunky.common.data.ChunkRequest;
import dev.waterdog.chunky.common.data.chunk.ChunkHolder;
import dev.waterdog.chunky.common.network.ChunkyClient;
import dev.waterdog.chunky.common.network.ChunkyPeer;
import dev.waterdog.chunky.common.network.MinecraftVersion;
import dev.waterdog.chunky.common.palette.DefaultBlockPaletteFactory;
import dev.waterdog.chunky.common.palette.VanillaBlockStates;
import dev.waterdog.chunky.common.util.ChunkUtils;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.LongConsumer;

@Log4j2
public class SimpleChunkyTest {

    private static final MinecraftVersion VERSION = MinecraftVersion.MINECRAFT_PE_1_18_30;
    public static final InetSocketAddress ADDRESS = new InetSocketAddress("127.0.0.1", 19132);
    private static volatile boolean running = true;

    private static AtomicInteger receivedCount = new AtomicInteger();

    private static final BiConsumer<ChunkHolder, Throwable> HANDLER = (holder, error) -> {
        if (error != null) {
            log.error("Failed to request chunk", error);
        } else {
            receivedCount.incrementAndGet();
        }
    };

    private static final ChunkyListener LISTENER = new ChunkyListener() {};

    public static void main(String[] args) throws Exception {
        new SimpleChunkyTest();
    }

    public SimpleChunkyTest() throws Exception {
        VanillaBlockStates.get().getOrCreatePalette(VERSION, DefaultBlockPaletteFactory.INSTANCE);

        ChunkyClient chunkyClient = ChunkyClient.builder()
                .peerCount(4)
                .minecraftVersion(VERSION)
                .targetAddress(ADDRESS)
                .paletteFactory(DefaultBlockPaletteFactory.INSTANCE)
                .maxPendingRequests(0)
                .autoReconnect(true)
                .reconnectInterval(8)
                .listener(LISTENER)
                .build();
        chunkyClient.connect().join();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdownHook(chunkyClient)));

        // chunkyClient.requestChunk(80, 10).whenComplete(HANDLER);
        // chunkyClient.requestChunk(90, 25).whenComplete(HANDLER);

        requestRadius(50, 50, 12, chunkyClient);
        // requestRadius(55, 50, 7, chunkyClient);
        // requestRadius(200, 50, 4, chunkyClient);
        // requestRadius(180, 50, 6, chunkyClient);
        // requestRadius(500, 50, 6, chunkyClient);
        // requestRadius(200, 150, 6, chunkyClient);

        // System.out.println(ChunkUtils.chunkX(Long.parseLong("197568495664"))); //46
        // System.out.println(ChunkUtils.chunkZ(Long.parseLong("197568495664"))); //48
        loop();
    }

    private static void requestRadius(int chunkX, int chunkZ, int radius, ChunkyClient chunky) {
        LongSet chunks = new LongArraySet();
        ChunkUtils.checkChunksInRadius(chunkX, chunkZ, radius, chunks::add);
        log.info("Requesting {} chunks", chunks.size());
        chunks.forEach((LongConsumer) index -> chunky.requestChunk(index).whenComplete(HANDLER));
    }

    private static void shutdownHook(ChunkyClient chunky) {
        log.warn("Pending chunks: " + chunky.getPendingChunkRequests().size());
        log.warn("Received chunks: " + receivedCount.get());
        chunky.getPendingChunkRequests().values().forEach(request ->
                log.info("pending chunk x={} z={}", request.getChunkX(), request.getChunkZ()));
        running = false;
    }

    private void loop() {
        while (running) {
            try {
                synchronized (this) {
                    this.wait();
                }
            } catch (InterruptedException e) {
            }
        }
    }
}
