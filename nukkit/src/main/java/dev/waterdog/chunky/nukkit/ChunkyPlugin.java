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
import dev.waterdog.chunky.common.network.ChunkyClient;
import dev.waterdog.chunky.common.network.MinecraftVersion;
import dev.waterdog.chunky.nukkit.palette.NukkitBlockPaletteFactory;
import dev.waterdog.chunky.nukkit.world.ChunkyManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

public class ChunkyPlugin extends PluginBase implements Listener {

    private static final Logger log = LogManager.getLogger("Chunky");

    private final Set<ChunkyClient> clients = new HashSet<>();
    private String worldName;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        NukkitBlockPaletteFactory.get();
        this.worldName = this.getConfig().getString("world_name");
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        this.clients.forEach(ChunkyClient::disconnect);
        this.clients.clear();
    }

    @EventHandler
    public void onLevelLoad(LevelLoadEvent event) {
        Level level = event.getLevel();
        if (!level.getFolderName().equals(this.worldName)) {
            return;
        }

        ChunkyClient client = this.buildClient();
        ChunkyManager chunkyManager = new ChunkyManager(client, level);
        client.setListener(chunkyManager);
        level.setGeneratorTaskFactory(chunkyManager);
        client.connect().whenComplete((v, error) -> this.onBindError(error));
        this.clients.add(client);
    }

    private void onBindError(Throwable t) {
        log.error("Failed to start Chunky peers", t);
    }

    private ChunkyClient buildClient() {
        int peerCount = this.getConfig().getInt("peer_count", 2);
        int maxRequests = this.getConfig().getInt("max_pending_requests", 20);
        boolean autoReconnect = this.getConfig().getBoolean("auto_reconnect");
        long reconnectInterval = this.getConfig().getLong("reconnect_interval");

        HostAndPort hostAndPort = HostAndPort.fromString(this.getConfig().getString("target_address"));
        InetSocketAddress address = new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPort());

        String codecVersion = this.getConfig().getString("codec_version");
        MinecraftVersion version = MinecraftVersion.fromVersionString(codecVersion);
        if (version == null) {
            throw new IllegalStateException("Unknown version: " + codecVersion);
        }

        return ChunkyClient.builder()
                .peerCount(peerCount)
                .minecraftVersion(version)
                .targetAddress(address)
                .paletteFactory(NukkitBlockPaletteFactory.get())
                .maxPendingRequests(maxRequests)
                .autoReconnect(autoReconnect)
                .reconnectInterval(reconnectInterval)
                .build();
    }
}
