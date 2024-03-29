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

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import dev.waterdog.chunky.common.data.ChunkRequest;
import dev.waterdog.chunky.common.data.chunk.ChunkHolder;
import dev.waterdog.chunky.common.network.ChunkyPeer;

import java.util.concurrent.CompletableFuture;

public interface ChunkyListener {

    default void onPeerReconnect(ChunkyPeer peer, CompletableFuture<Void> reconnectFuture) {
    }

    default void onUnhandledChunkReceived(ChunkHolder chunkHolder, ChunkyPeer peer) {
    }

    default void onBlockEntityUpdate(Vector3i position, NbtMap nbt) {
    }

    default void onChunkRequestTimeout(ChunkRequest request, ChunkyPeer peer) {
    }
}
