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

import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import dev.waterdog.chunky.common.data.chunk.ChunkHolder;
import dev.waterdog.chunky.common.palette.BlockPaletteFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

@Getter
public class ChunkyClient {

    private final int peerCount;
    private final BedrockPacketCodec codec;
    private final int raknetVersion;
    private final InetSocketAddress targetAddress;

    private final BlockPaletteFactory paletteFactory;

    private final EventLoopGroup eventLoopGroup;
    private ChunkyPeer peer;

    public static Builder builder() {
        return new Builder();
    }

    public ChunkyClient(int peerCount, BedrockPacketCodec codec, int raknetVersion, InetSocketAddress targetAddress, BlockPaletteFactory paletteFactory) {
        this.peerCount = peerCount;
        this.codec = codec;
        this.raknetVersion = raknetVersion;
        this.targetAddress = targetAddress;
        this.paletteFactory = paletteFactory;
        this.eventLoopGroup = new NioEventLoopGroup(peerCount);
    }

    public CompletableFuture<Void> connect() {
        // TODO: start all peers
        this.peer = new ChunkyPeer(this, this.codec, this.targetAddress, this.eventLoopGroup.next());
        return this.peer.start();
    }

    public void disconnect() {
        if (this.peer != null) {
            this.peer.close("Disconnected");
        }
    }

    protected void onChunkDeserializedCallback(ChunkHolder chunkHolder, ChunkyPeer peer) {
        
    }

    @Setter
    @Accessors(fluent = true)
    public static class Builder {
        private int peerCount;
        private BedrockPacketCodec codec;
        private int raknetVersion;
        private InetSocketAddress targetAddress;
        private BlockPaletteFactory paletteFactory;

        public ChunkyClient build() {
            return new ChunkyClient(this.peerCount, this.codec, this.raknetVersion, this.targetAddress, this.paletteFactory);
        }
    }
}
