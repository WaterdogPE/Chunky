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

import com.nimbusds.jwt.SignedJWT;
import com.nukkitx.protocol.bedrock.BedrockClient;
import com.nukkitx.protocol.bedrock.BedrockClientSession;
import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.*;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import dev.waterdog.chunky.common.data.LoginData;
import dev.waterdog.chunky.common.data.LoginState;
import dev.waterdog.chunky.common.data.PeerClientData;
import io.netty.channel.EventLoop;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Log4j2
public class ChunkyPeer implements BedrockPacketHandler {

    private final InetSocketAddress targetAddress;
    private final EventLoop eventLoop;

    @Getter
    private final LoginData loginData;
    @Getter
    private final PeerClientData clientData = new PeerClientData();

    private BedrockClient bedrockClient;
    private BedrockClientSession session;

    @Getter
    private LoginState loginState = LoginState.CONNECTING;

    public ChunkyPeer(BedrockPacketCodec codec, InetSocketAddress targetAddress, EventLoop eventLoop) {
        this.targetAddress = targetAddress;
        this.eventLoop = eventLoop;
        this.loginData = new LoginData(codec);
    }

    public CompletableFuture<Void> start() {
        if (this.session != null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Peer is already running!"));
        }

        this.bedrockClient = new BedrockClient(new InetSocketAddress("0.0.0.0", 0), this.eventLoop);
        this.bedrockClient.setRakNetVersion(9);
        CompletableFuture<BedrockClientSession> future = this.bedrockClient.bind().thenCompose(v -> this.bedrockClient.connect(this.targetAddress));
        return future.thenAccept(this::onConnected);
    }

    private void onConnected(BedrockClientSession session) {
        this.session = session;
        // TODO: disconnect handler
        session.setPacketHandler(this);
        session.setPacketCodec(this.loginData.getCodec());
        session.sendPacket(this.loginData.createLoginPacket());
        log.info("[{}] started with login sequence", this.loginData.getIdentityData().getDisplayName());
    }

    private void onLogin() {
        this.session.sendPacket(this.clientData.cacheStatusPacket());
    }

    public void close(String reason) {
        if (this.session != null && !this.session.isClosed()) {
            this.bedrockClient.close(false);
        }
        log.info("[{}] was closed: {}", this.loginData.getIdentityData().getDisplayName(), reason);
    }

    @Override
    public boolean handle(PlayStatusPacket packet) {
        switch (packet.getStatus()) {
            case LOGIN_SUCCESS:
                this.loginState = LoginState.LOGIN;
                this.onLogin();
                break;
            case PLAYER_SPAWN:
                this.loginState = LoginState.SPAWNED;
                break;
            default:
                this.close(packet.getStatus().name());
                break;
        }

        log.info("changed state to {}", loginState); // TODO: remove
        return true;
    }

    // Handlers

    @Override
    public final boolean handle(ServerToClientHandshakePacket packet) {
        try {
            SignedJWT saltJwt = SignedJWT.parse(packet.getJwt());
            URI x5u = saltJwt.getHeader().getX509CertURL();
            ECPublicKey serverKey = EncryptionUtils.generateKey(x5u.toASCIIString());
            SecretKey key = EncryptionUtils.getSecretKey(
                    this.loginData.getKeyPair().getPrivate(),
                    serverKey,
                    Base64.getDecoder().decode(saltJwt.getJWTClaimsSet().getStringClaim("salt"))
            );
            this.session.enableEncryption(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.session.sendPacket(new ClientToServerHandshakePacket());
        return true;
    }

    @Override
    public final boolean handle(ResourcePacksInfoPacket packet) {
        ResourcePackClientResponsePacket response = new ResourcePackClientResponsePacket();
        response.setStatus(ResourcePackClientResponsePacket.Status.HAVE_ALL_PACKS);
        this.session.sendPacket(response);
        return true;
    }

    @Override
    public final boolean handle(ResourcePackStackPacket packet) {
        ResourcePackClientResponsePacket response = new ResourcePackClientResponsePacket();
        response.setStatus(ResourcePackClientResponsePacket.Status.COMPLETED);
        this.session.sendPacket(response);
        return true;
    }

    @Override
    public boolean handle(ChunkRadiusUpdatedPacket packet) {
        this.clientData.setChunkRadius(packet.getRadius());
        return true;
    }

    @Override
    public final boolean handle(StartGamePacket packet) {
        this.clientData.setEntityId(packet.getRuntimeEntityId());
        if (packet.getPlayerMovementSettings() != null) {
            this.clientData.setMovementMode(packet.getPlayerMovementSettings().getMovementMode());
        }
        // Request chunk radius
        this.session.sendPacket(this.clientData.radiusPacket());
        // Tell server that we are ready
        SetLocalPlayerAsInitializedPacket initializedPacket = new SetLocalPlayerAsInitializedPacket();
        initializedPacket.setRuntimeEntityId(this.clientData.getEntityId());
        // Delay this a bit
        this.eventLoop.schedule(() -> this.session.sendPacket(initializedPacket), 200, TimeUnit.MILLISECONDS);
        // TODO: load palette if sent over network

        this.clientData.updateAndSendPosition(packet.getPlayerPosition(), this.session);
        return true;
    }

    @Override
    public boolean handle(MovePlayerPacket packet) {
        if (packet.getRuntimeEntityId() != this.clientData.getEntityId()) {
            return true;
        }

        log.info("update position: {}", packet.getPosition());
        if (packet.getMode() == MovePlayerPacket.Mode.RESPAWN) {
            this.clientData.updateAndSendPosition(packet.getPosition(), this.session);
        } else {
            this.clientData.setPosition(packet.getPosition());
        }
        return true;
    }

    @Override
    public boolean handle(LevelChunkPacket packet) {
        // TODO: deserialize and pass to the handler
        log.info("chunk: x={} z={}", packet.getChunkX(), packet.getChunkZ());
        return true;
    }
}
