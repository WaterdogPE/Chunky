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
import com.nukkitx.math.vector.Vector2i;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.network.util.DisconnectReason;
import com.nukkitx.protocol.bedrock.BedrockClient;
import com.nukkitx.protocol.bedrock.BedrockClientSession;
import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.*;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import dev.waterdog.chunky.common.data.chunk.ChunkHolder;
import dev.waterdog.chunky.common.data.login.LoginData;
import dev.waterdog.chunky.common.data.login.LoginState;
import dev.waterdog.chunky.common.data.PeerClientData;
import dev.waterdog.chunky.common.palette.BlockPaletteLegacy;
import dev.waterdog.chunky.common.serializer.Serializers;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.ScheduledFuture;
import it.unimi.dsi.fastutil.longs.*;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.interfaces.ECPublicKey;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static dev.waterdog.chunky.common.util.ChunkUtils.*;

@Log4j2
public class ChunkyPeer implements BedrockPacketHandler {

    private final ChunkyClient parent;
    private final InetSocketAddress targetAddress;
    private final EventLoop eventLoop;

    @Getter
    private final LoginData loginData;
    private BlockPaletteLegacy blockPalette;
    @Getter
    private final PeerClientData clientData = new PeerClientData();
    private BedrockClient bedrockClient;
    private BedrockClientSession session;
    @Getter
    private LoginState loginState = LoginState.CONNECTING;

    private ScheduledFuture<?> tickFuture;

    private final List<Long> chunkRequests = new CopyOnWriteArrayList<>(); // LongLists.synchronize(new LongArrayList());
    private final LongSet pendingChunks = new LongArraySet();
    private long lastRequest;

    public ChunkyPeer(ChunkyClient parent, BedrockPacketCodec codec, InetSocketAddress targetAddress, EventLoop eventLoop) {
        this.parent = parent;
        this.targetAddress = targetAddress;
        this.eventLoop = eventLoop;
        this.loginData = new LoginData(codec);
    }

    public CompletableFuture<Void> start() {
        if (this.session != null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Peer is already running!"));
            return future;
        }

        this.bedrockClient = new BedrockClient(new InetSocketAddress("0.0.0.0", 0), this.eventLoop);
        this.bedrockClient.setRakNetVersion(this.parent.getRaknetVersion());
        CompletableFuture<BedrockClientSession> future = this.bedrockClient.bind().thenCompose(v -> this.bedrockClient.connect(this.targetAddress));
        return future.thenAccept(this::onConnected);
    }

    private void onConnected(BedrockClientSession session) {
        this.session = session;
        session.setLogging(false);
        session.addDisconnectHandler(reason -> {
            if (reason != DisconnectReason.DISCONNECTED) {
                this.close(reason.name());
            }
        });
        session.setPacketHandler(this);
        session.setPacketCodec(this.loginData.getCodec());
        session.sendPacket(this.loginData.createLoginPacket());
        log.info("[{}] started with login sequence", this.getDisplayName());
    }

    private void onLogin() {
        this.session.sendPacket(this.clientData.cacheStatusPacket());
    }

    private void doSpawn() {
        log.info("[{}] has spawned and is ready for incoming chunks!", this.getDisplayName());
        this.tickFuture = this.eventLoop.scheduleAtFixedRate(this::onTick, 0, 200, TimeUnit.MILLISECONDS);
    }

    public void close(String reason) {
        if (this.session != null && !this.session.isClosed()) {
            this.bedrockClient.close(false);
        }
        if (this.tickFuture != null) {
            this.tickFuture.cancel(true);
        }
        log.info("[{}] with state {} was closed: {}", this.getDisplayName(), this.loginState, reason);
        this.loginState = LoginState.CLOSED;
    }

    public boolean requestChunk(long chunkIndex) {
        if (this.loginState == LoginState.CLOSED) {
            return false;
        }

        if (this.parent.getMaxPendingRequests() != 0 && this.chunkRequests.size() >= this.parent.getMaxPendingRequests()) {
            return false;
        }
        this.chunkRequests.add(chunkIndex);
        return true;
    }

    private void onTick() {
        if (this.loginState == LoginState.CLOSED || this.session == null || this.session.isClosed()) {
            return;
        }

        long currTime = System.currentTimeMillis();
        if (currTime > this.lastRequest + (1000 * 2)) {
            LongIterator iterator = this.pendingChunks.iterator();
            while (iterator.hasNext()) {
                this.parent.onPendingChunkTimeout(iterator.nextLong(), this);
                iterator.remove();
            }
        }

        try {
            // Remove duplicates
            this.chunkRequests.removeIf((index) -> this.pendingChunks.contains((long) index));
            // Request new chunks
            if (!this.chunkRequests.isEmpty() && this.pendingChunks.isEmpty()) {
                this.requestChunkInternal(this.chunkRequests.remove(0));
            }
        } catch (Exception e) {
            log.error("[{}] Unable to tick", this.getDisplayName(), e);
        }
    }

    private void requestChunkInternal(long index) {
        int chunkX = chunkX(index);
        int chunkZ = chunkZ(index);
        this.pendingChunks.add(index);
        if (log.isDebugEnabled()) {
            log.debug("[{}] Requesting chunk x={} z={}", this.getDisplayName(), chunkX, chunkZ);
        }
        // Calculate all chunks that server will send us
        // Firstly we need all chunks from new position
        int radius = Math.max(1, this.getChunkRadius() - 1);
        LongSet newChunks = new LongArraySet();
        checkChunksInRadius(chunkX, chunkZ, radius, newChunks::add);
        // Remove chunks that we already got
        checkChunksInRadius(this.getChunkX(), this.getChunkZ(), radius, newChunks::remove);
        // this.pendingChunks.addAll(newChunks);
        this.chunkRequests.removeAll(newChunks);
        // Change position
        this.clientData.updateAndSendPosition(Vector3f.from((chunkX << 4) + 10, 255, (chunkZ << 4) + 10), this.session);
        this.lastRequest = System.currentTimeMillis();
    }

    protected void offerChunkRequestUnsafe(long index) {
        this.chunkRequests.add(index);
    }

    protected void offerChunkUnsafe(long index) {
        // this.eventLoop.execute(() -> this.pendingChunks.add(index));
        this.pendingChunks.add(index);
    }

    public String getDisplayName() {
        return this.loginData.getIdentityData().getDisplayName();
    }

    public Vector2i getChunkPosition() {
        return Vector2i.from(this.getChunkX(), this.getChunkZ());
    }

    public int getChunkX() {
        return (int) this.clientData.getPosition().getX() >> 4;
    }

    public int getChunkZ() {
        return (int) this.clientData.getPosition().getZ() >> 4;
    }

    public int getChunkRadius() {
        return this.clientData.getChunkRadius();
    }

    public Collection<Long> getChunkRequests() {
        return Collections.unmodifiableCollection(this.chunkRequests);
    }

    public Collection<Long> getPendingChunks() {
        return Collections.unmodifiableCollection(this.pendingChunks);
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
    public boolean handle(PlayStatusPacket packet) {
        switch (packet.getStatus()) {
            case LOGIN_SUCCESS:
                this.loginState = LoginState.LOGIN;
                this.onLogin();
                break;
            case PLAYER_SPAWN:
                this.loginState = LoginState.SPAWNED;
                this.doSpawn();
                break;
            default:
                this.close(packet.getStatus().name());
                break;
        }
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
        if (log.isDebugEnabled()) {
            log.debug("[{}] Changed chunk radius: {}", this.getDisplayName(), packet.getRadius());
        }
        this.clientData.setChunkRadius(packet.getRadius());
        return true;
    }

    @Override
    public final boolean handle(StartGamePacket packet) {
        this.clientData.setEntityId(packet.getRuntimeEntityId());
        this.clientData.setBlockPalette(packet.getBlockPalette());
        if (packet.getPlayerMovementSettings() != null) {
            this.clientData.setMovementMode(packet.getPlayerMovementSettings().getMovementMode());
        }
        this.blockPalette = this.parent.getPaletteFactory()
                .createLegacyBlockPalette(packet.getBlockPalette(), this.loginData.getCodec().getProtocolVersion());
        // Request chunk radius
        this.session.sendPacket(this.clientData.radiusPacket());
        // Confirm received position
        this.clientData.updateAndSendPosition(packet.getPlayerPosition(), this.session);
        // Tell server that we are ready, with a small delay
        SetLocalPlayerAsInitializedPacket initializedPacket = new SetLocalPlayerAsInitializedPacket();
        initializedPacket.setRuntimeEntityId(this.clientData.getEntityId());
        this.eventLoop.schedule(() -> this.session.sendPacket(initializedPacket), 200, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public boolean handle(MovePlayerPacket packet) {
        if (packet.getRuntimeEntityId() != this.clientData.getEntityId()) {
            return true;
        }

        if (log.isDebugEnabled()) {
            log.debug("[{}] Update position: {}", this.getDisplayName(), packet.getPosition());
        }

        if (packet.getMode() == MovePlayerPacket.Mode.RESPAWN) {
            this.clientData.updateAndSendPosition(packet.getPosition(), this.session);
        } else {
            this.clientData.setPosition(packet.getPosition());
        }
        return true;
    }

    @Override
    public boolean handle(LevelChunkPacket packet) {
        if (log.isDebugEnabled()) {
            log.debug("[{}] Chunk: x={} z={}", this.getDisplayName(), packet.getChunkX(), packet.getChunkZ());
        }

        if (this.loginState != LoginState.SPAWNED) {
            return true;
        }

        ByteBuf buffer = Unpooled.wrappedBuffer(packet.getData());
        ChunkHolder chunkHolder = new ChunkHolder(packet.getChunkX(), packet.getChunkZ(), packet.getSubChunksLength(), this.blockPalette);
        Serializers.deserializeChunk(buffer, chunkHolder, this.blockPalette, this.loginData.getCodec().getProtocolVersion());

        long chunkIndex = chunkIndex(packet.getChunkX(), packet.getChunkZ());
        if (!this.pendingChunks.remove(chunkIndex) && log.isDebugEnabled()) {
            log.debug("[{}] Not requested chunk: x={} z={}", this.getDisplayName(), packet.getChunkX(), packet.getChunkZ());
        }
        this.parent.onChunkDeserializedCallback(chunkHolder, this);
        return true;
    }
}
