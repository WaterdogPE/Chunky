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

package dev.waterdog.chunky.common.data;

import com.nukkitx.math.vector.Vector2f;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.BedrockClientSession;
import com.nukkitx.protocol.bedrock.data.AuthoritativeMovementMode;
import com.nukkitx.protocol.bedrock.data.ClientPlayMode;
import com.nukkitx.protocol.bedrock.data.InputMode;
import com.nukkitx.protocol.bedrock.packet.ClientCacheStatusPacket;
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket;
import com.nukkitx.protocol.bedrock.packet.PlayerAuthInputPacket;
import com.nukkitx.protocol.bedrock.packet.RequestChunkRadiusPacket;
import lombok.Data;

@Data
public class PeerClientData {

    private int chunkRadius = 8;
    private long entityId;
    private Vector3f position;
    private boolean chunkCache = false;
    private NbtList<NbtMap> blockPalette;
    private AuthoritativeMovementMode movementMode = AuthoritativeMovementMode.CLIENT;

    public RequestChunkRadiusPacket radiusPacket() {
        RequestChunkRadiusPacket packet = new RequestChunkRadiusPacket();
        packet.setRadius(chunkRadius);
        return packet;
    }

    public ClientCacheStatusPacket cacheStatusPacket() {
        ClientCacheStatusPacket packet = new ClientCacheStatusPacket();
        packet.setSupported(this.chunkCache);
        return packet;
    }

    public void updateAndSendPosition(Vector3f position, BedrockClientSession session) {
        if (movementMode == AuthoritativeMovementMode.CLIENT) {
            MovePlayerPacket packet = new MovePlayerPacket();
            packet.setRuntimeEntityId(this.entityId);
            packet.setPosition(position);
            packet.setRotation(Vector3f.ZERO);
            packet.setMode(MovePlayerPacket.Mode.NORMAL); // TODO: maybe teleport
            packet.setOnGround(false);
            packet.setEntityType(0);
            packet.setTick(0);
            session.sendPacket(packet);
        } else {
            PlayerAuthInputPacket packet = new PlayerAuthInputPacket();
            packet.setPosition(position);
            packet.setRotation(Vector3f.ZERO);
            packet.setMotion(Vector2f.from(this.position.getX() - position.getX(), this.position.getZ() - position.getZ()));
            packet.setInputMode(InputMode.MOTION_CONTROLLER);
            // packet.getInputData().add(Plaa)
            packet.setPlayMode(ClientPlayMode.NORMAL);
            packet.setDelta(this.position.sub(position));
            session.sendPacket(packet);
        }
        this.position = position;
    }

    public void setPosition(Vector3f position) {
        this.position = position;
    }

    public void setPosition(Vector3i position) {
        this.position = position.toFloat();
    }
}
