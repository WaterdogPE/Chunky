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

package dev.waterdog.chunky.common.data.login;

import com.google.gson.*;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.shaded.json.JSONObject;
import com.nimbusds.jose.shaded.json.JSONStyle;
import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.packet.LoginPacket;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import dev.waterdog.chunky.common.network.HandshakeUtils;
import dev.waterdog.chunky.common.network.MinecraftVersion;
import io.netty.util.AsciiString;
import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Data
public class LoginData {

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
            .create();

    private static final byte[] cachedSkinData = randomSkinData();
    private static final byte[] cachedGeometryData = loadGeometryData();

    private static byte[] randomSkinData() {
        byte[] bytes = new byte[]{(byte) 0xa5, 0x00, 0x01, (byte) 0xff};
        byte[] skinData = new byte[4096 * 4];
        for (int i = 0; i < skinData.length; i++) {
            skinData[i] = bytes[i % bytes.length];
        }
        return skinData;
    }

    private static byte[] loadGeometryData() {
        try (InputStream stream = LoginData.class.getClassLoader().getResourceAsStream("steve_geometry.json")) {
            byte[] bytes = new byte[stream.available()];
            stream.read(bytes);
            return bytes;
        } catch (IOException e) {
            throw new RuntimeException("Cannot load geometry data", e);
        }
    }

    public static ExtraData buildExtraData(BedrockPacketCodec protocolCodec) {
        ExtraData.Builder builder = ExtraData.builder()
                .skinId(UUID.randomUUID().toString())
                .skinData(cachedSkinData)
                .skinImageHeight(64)
                .skinImageWidth(64)
                .skinResourcePatch( "{\"geometry\" : {\"default\" : \"geometry.humanoid.custom\"}}")
                .skinGeometryData(cachedGeometryData)
                .skinAnimationData(new byte[0])
                .personaAnimations(new JsonArray())
                .personaPieces(new JsonArray())
                .personaPieceTints(new JsonArray())
                .armSize("wide")
                .playFabId(UUID.randomUUID().toString())
                .personaSkin(false)
                .premiumSkin(false)
                .primaryUser(true)
                .skinColor("")
                .capeId("")
                .capeData(new byte[0])
                .capeImageHeight(0)
                .capeImageWidth(0)
                .capeOnClassicSkin(false)
                .deviceOs(7) // UNIVERSAL_WINDOWS_PLATFORM
                .deviceId("")
                .deviceModel("chunky")
                .clientRandomId(ThreadLocalRandom.current().nextLong())
                .currentInputMode(1) // mouse
                .defaultInputMode(1) // mouse again
                .guiScale(0)
                .gameVersion(protocolCodec.getMinecraftVersion())
                .languageCode("en_GB")
                .platformOfflineId("")
                .platformOnlineId("")
                .selfSignedId(UUID.randomUUID())
                .serverAddress("")
                .thirdPartyName("")
                .thirdPartyNameOnly(false)
                .uiProfile(1);
        return builder.build();
    }

    public static IdentityData buildIdentityData(BedrockPacketCodec protocolCodec) {
        IdentityData.Builder builder = new IdentityData.Builder()
                .xuid("")
                .identity(UUID.randomUUID())
                .displayName("chunky" + ThreadLocalRandom.current().nextInt(0, 1000))
                .titleId(String.valueOf(ThreadLocalRandom.current().nextInt(0, 99999999) + 100000000));
        return builder.build();
    }

    private final KeyPair keyPair = EncryptionUtils.createKeyPair();
    private final MinecraftVersion version;
    private final IdentityData identityData;
    private final ExtraData extraData;

    public LoginData(MinecraftVersion version) {
        this.version = version;
        this.identityData = buildIdentityData(version.getCodec());
        this.extraData = buildExtraData(version.getCodec());
    }

    public LoginPacket createLoginPacket() {
        JWSObject signedIdentityData = HandshakeUtils.createExtraData(this.keyPair, (JsonObject) GSON.toJsonTree(this.identityData));
        JWSObject signedExtraData = HandshakeUtils.encodeJWT(this.keyPair, (JsonObject) GSON.toJsonTree(this.extraData));

        JSONObject chainJson = new JSONObject();
        chainJson.put("chain", Collections.singletonList(signedIdentityData.serialize()));
        AsciiString chainData = AsciiString.of(chainJson.toString(JSONStyle.LT_COMPRESS));

        LoginPacket loginPacket = new LoginPacket();
        loginPacket.setChainData(chainData);
        loginPacket.setSkinData(AsciiString.of(signedExtraData.serialize()));
        loginPacket.setProtocolVersion(this.version.getProtocol());
        return loginPacket;
    }
}
