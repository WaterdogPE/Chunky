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

import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.v388.Bedrock_v388;
import com.nukkitx.protocol.bedrock.v475.Bedrock_v475;
import dev.waterdog.chunky.common.network.ChunkyPeer;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.net.InetSocketAddress;

public class SimpleChunkyPeerTest {

    private static final BedrockPacketCodec CODEC = Bedrock_v388.V388_CODEC;
    public static final InetSocketAddress ADDRESS = new InetSocketAddress("192.168.0.50", 19132);

    public static void main(String[] args) {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);
        ChunkyPeer peer = new ChunkyPeer(CODEC, ADDRESS, eventLoopGroup.next());
        peer.start().join();
    }
}
