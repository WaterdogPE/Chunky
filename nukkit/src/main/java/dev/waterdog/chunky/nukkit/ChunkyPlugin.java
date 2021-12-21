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

package dev.waterdog.chunky.nukkit;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.level.LevelLoadEvent;
import cn.nukkit.level.Level;
import cn.nukkit.plugin.PluginBase;
import com.google.common.net.HostAndPort;
import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.v388.Bedrock_v388;
import dev.waterdog.chunky.common.network.ChunkyClient;
import dev.waterdog.chunky.nukkit.palette.NukkitBlockPaletteFactory;
import dev.waterdog.chunky.nukkit.world.ChunkyManager;

import java.net.InetSocketAddress;

public class ChunkyPlugin extends PluginBase implements Listener {

    private ChunkyClient chunkyClient;
    private String worldName;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        NukkitBlockPaletteFactory.get();
        if (this.chunkyClient == null) {
            this.chunkyClient = this.buildClient();
        }
        this.worldName = this.getConfig().getString("world_name");
        this.chunkyClient.connect();

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        if (this.chunkyClient != null) {
            this.chunkyClient.disconnect();
        }
    }

    private ChunkyClient buildClient() {
        int peerCount = this.getConfig().getInt("peer_count", 2);
        int maxRequests = this.getConfig().getInt("max_pending_requests", 20);

        HostAndPort hostAndPort = HostAndPort.fromString(this.getConfig().getString("target_address"));
        InetSocketAddress address = new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPort());

        String codecVersion = this.getConfig().getString("codec_version");
        BedrockPacketCodec codec = Bedrock_v388.V388_CODEC; // TODO: parse from string
        int raknetVersion = 9; // TODO:

        return ChunkyClient.builder()
                .peerCount(peerCount)
                .codec(codec)
                .raknetVersion(raknetVersion)
                .targetAddress(address)
                .paletteFactory(NukkitBlockPaletteFactory.get())
                .maxPendingRequests(maxRequests)
                .build();
    }

    @EventHandler
    public void onLevelLoad(LevelLoadEvent event) {
        Level level = event.getLevel();
        if (!level.getFolderName().equals(this.worldName)) {
            return;
        }

        ChunkyManager chunkyManager = new ChunkyManager(this.chunkyClient, level);
        this.chunkyClient.setChunkListener(chunkyManager);
        level.setGeneratorTaskFactory(chunkyManager);
    }
}
