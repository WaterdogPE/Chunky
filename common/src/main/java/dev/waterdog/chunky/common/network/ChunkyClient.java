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

package dev.waterdog.chunky.common.network;

import com.google.common.base.Preconditions;
import com.nukkitx.network.raknet.util.RoundRobinIterator;
import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import dev.waterdog.chunky.common.UnhandledChunkListener;
import dev.waterdog.chunky.common.data.ChunkRequest;
import dev.waterdog.chunky.common.data.chunk.ChunkHolder;
import dev.waterdog.chunky.common.palette.BlockPaletteFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static dev.waterdog.chunky.common.util.ChunkUtils.*;

@Log4j2
@Getter
public class ChunkyClient {

    private final int peerCount;
    private final BedrockPacketCodec codec;
    private final int raknetVersion;
    private final InetSocketAddress targetAddress;
    private final int maxPendingRequests;

    private final BlockPaletteFactory paletteFactory;
    @Setter
    private UnhandledChunkListener chunkListener;

    private final EventLoopGroup eventLoopGroup;
    private final Set<ChunkyPeer> peers = ObjectSets.synchronize(new ObjectArraySet<>());
    private final RoundRobinIterator<ChunkyPeer> peerIterator = new RoundRobinIterator<>(this.peers);
    private volatile boolean running = false;

    private final Long2ObjectMap<ChunkRequest> pendingChunkRequests = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());

    public static Builder builder() {
        return new Builder();
    }

    public ChunkyClient(int peerCount, BedrockPacketCodec codec, int raknetVersion, InetSocketAddress targetAddress, int maxPendingRequests, BlockPaletteFactory paletteFactory, UnhandledChunkListener chunkListener) {
        this.peerCount = peerCount;
        this.codec = codec;
        this.raknetVersion = raknetVersion;
        this.targetAddress = targetAddress;
        this.maxPendingRequests = maxPendingRequests;
        this.paletteFactory = paletteFactory;
        this.chunkListener = chunkListener;
        this.eventLoopGroup = new NioEventLoopGroup(peerCount);
    }

    public CompletableFuture<Void> connect() {
        Preconditions.checkArgument(!this.running, "ChunkyClient is already running");
        this.running = true;

        CompletableFuture<Void>[] futures = new CompletableFuture[this.peerCount];
        for (int i = 0; i < this.peerCount; i++) {
            ChunkyPeer peer = new ChunkyPeer(this, this.codec, this.targetAddress, this.eventLoopGroup.next());
            this.peers.add(peer);
            futures[i] = peer.start();
        }
        return CompletableFuture.allOf(futures);
    }

    public void disconnect() {
        if (!this.running) {
            return;
        }
        this.running = false;

        for (ChunkyPeer peer : this.peers) {
            peer.close("Disconnected");
        }
        this.peers.clear();
    }

    public CompletableFuture<ChunkHolder> requestChunk(long index) {
        return this.requestChunk(chunkX(index), chunkZ(index));
    }

    public CompletableFuture<ChunkHolder> requestChunk(int chunkX, int chunkZ) {
        long index = chunkIndex(chunkX, chunkZ);
        ChunkRequest request = this.pendingChunkRequests.get(index);
        if (request != null) {
            return request.getFuture();
        }

        ChunkyPeer peer = this.assignRequest(index, request = new ChunkRequest(chunkX, chunkZ));
        if (peer == null) {
            CompletableFuture<ChunkHolder> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Can not request chunk x=" + chunkX + " z=" + chunkZ));
            return future;
        }
        this.pendingChunkRequests.put(index, request);
        return request.getFuture();
    }

    protected void onChunkDeserializedCallback(ChunkHolder chunkHolder, ChunkyPeer peer) {
        long index = chunkIndex(chunkHolder.getChunkX(), chunkHolder.getChunkZ());
        ChunkRequest request = this.pendingChunkRequests.remove(index);
        if (request != null) {
            request.getFuture().complete(chunkHolder);
            return;
        }

        if (this.chunkListener != null) {
            this.chunkListener.onChunkReceived(chunkHolder, peer);
        }
    }

    private ChunkyPeer assignRequest(long index, ChunkRequest request) {
        if (this.peers.isEmpty()) {
            return null;
        }

        for (int i = 0; i < this.peerCount; i++) {
            ChunkyPeer peer = this.peerIterator.next();
            Collection<Long> chunkRequests = peer.getChunkRequests();
            Collection<Long> pendingChunks = peer.getPendingChunks();
            if (pendingChunks.contains(index) || chunkRequests.contains(index)) {
                return peer;
            }
            // Shrink the radius a bit as our calculations may be bit different
            int radius = Math.max(1, peer.getChunkRadius() - 1);
            // Check if peer will be requesting chunk in same area. If yes,
            // we can expect our chunk to be sent than
            for (long chunkIndex : chunkRequests) {
                if (isIndexInRadius(chunkX(chunkIndex), chunkZ(chunkIndex), radius, index)) {
                    peer.offerChunkRequestUnsafe(index);
                    return peer;
                }
            }

            /*for (long chunkIndex : pendingChunks) {
                if (isIndexInRadius(chunkX(chunkIndex), chunkZ(chunkIndex), radius, index)) {
                    peer.offerChunkUnsafe(index);
                    return peer;
                }
            }*/

            if (peer.requestChunk(index)) {
                return peer;
            }
        }
        return null;
    }

    @Setter
    @Accessors(fluent = true)
    public static class Builder {
        private int peerCount;
        private BedrockPacketCodec codec;
        private int raknetVersion;
        private InetSocketAddress targetAddress;
        private int maxPendingRequests;
        private BlockPaletteFactory paletteFactory;
        private UnhandledChunkListener chunkListener;

        public ChunkyClient build() {
            return new ChunkyClient(this.peerCount, this.codec, this.raknetVersion, this.targetAddress, this.maxPendingRequests, this.paletteFactory, this.chunkListener);
        }
    }
}
