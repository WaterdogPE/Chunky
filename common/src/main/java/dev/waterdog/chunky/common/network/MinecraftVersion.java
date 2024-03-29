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
import com.nukkitx.protocol.bedrock.v388.Bedrock_v388;
import com.nukkitx.protocol.bedrock.v389.Bedrock_v389;
import com.nukkitx.protocol.bedrock.v390.Bedrock_v390;
import com.nukkitx.protocol.bedrock.v407.Bedrock_v407;
import com.nukkitx.protocol.bedrock.v408.Bedrock_v408;
import com.nukkitx.protocol.bedrock.v419.Bedrock_v419;
import com.nukkitx.protocol.bedrock.v422.Bedrock_v422;
import com.nukkitx.protocol.bedrock.v428.Bedrock_v428;
import com.nukkitx.protocol.bedrock.v431.Bedrock_v431;
import com.nukkitx.protocol.bedrock.v440.Bedrock_v440;
import com.nukkitx.protocol.bedrock.v448.Bedrock_v448;
import com.nukkitx.protocol.bedrock.v465.Bedrock_v465;
import com.nukkitx.protocol.bedrock.v471.Bedrock_v471;
import com.nukkitx.protocol.bedrock.v475.Bedrock_v475;
import com.nukkitx.protocol.bedrock.v486.Bedrock_v486;
import com.nukkitx.protocol.bedrock.v503.Bedrock_v503;
import com.nukkitx.protocol.bedrock.v527.Bedrock_v527;
import com.nukkitx.protocol.bedrock.v534.Bedrock_v534;
import lombok.ToString;

@ToString(exclude = {"bedrockCodec"})
public enum MinecraftVersion {

    MINECRAFT_PE_1_13(388, Bedrock_v388.V388_CODEC, 9),
    MINECRAFT_PE_1_14_30(389, Bedrock_v389.V389_CODEC, 9),
    MINECRAFT_PE_1_14_60(390, Bedrock_v390.V390_CODEC, 9),
    MINECRAFT_PE_1_16(407, Bedrock_v407.V407_CODEC, 10),
    MINECRAFT_PE_1_16_20(408, Bedrock_v408.V408_CODEC, 10),
    MINECRAFT_PE_1_16_100(419, Bedrock_v419.V419_CODEC, 10),
    MINECRAFT_PE_1_16_200(422, Bedrock_v422.V422_CODEC, 10),
    MINECRAFT_PE_1_16_210(428, Bedrock_v428.V428_CODEC, 10),
    MINECRAFT_PE_1_16_220(431, Bedrock_v431.V431_CODEC, 10),
    MINECRAFT_PE_1_17_0(440, Bedrock_v440.V440_CODEC, 10),
    MINECRAFT_PE_1_17_10(448, Bedrock_v448.V448_CODEC, 10),
    MINECRAFT_PE_1_17_30(465, Bedrock_v465.V465_CODEC, 10),
    MINECRAFT_PE_1_17_40(471, Bedrock_v471.V471_CODEC, 10),
    MINECRAFT_PE_1_18_0(475, Bedrock_v475.V475_CODEC, 10),
    MINECRAFT_PE_1_18_10(486, Bedrock_v486.V486_CODEC, 10),
    MINECRAFT_PE_1_18_30(503, Bedrock_v503.V503_CODEC, 10),
    MINECRAFT_PE_1_19_0(527, Bedrock_v527.V527_CODEC, 10),
    MINECRAFT_PE_1_19_10(534, Bedrock_v534.V534_CODEC, 10);

    public static final MinecraftVersion[] VALUES = values();

    private final int protocol;
    private final int raknetVersion;

    private final BedrockPacketCodec bedrockCodec;

    MinecraftVersion(int protocol, BedrockPacketCodec bedrockCodec, int raknetVersion) {
        this.protocol = protocol;
        this.bedrockCodec = bedrockCodec;
        this.raknetVersion = raknetVersion;
    }

    public boolean isBefore(MinecraftVersion version) {
        return this.protocol < version.protocol;
    }

    public boolean isBeforeOrEqual(MinecraftVersion version) {
        return this.protocol <= version.protocol;
    }

    public boolean isAfter(MinecraftVersion version) {
        return this.protocol > version.protocol;
    }

    public boolean isAfterOrEqual(MinecraftVersion version) {
        return this.protocol >= version.protocol;
    }

    public int getProtocol() {
        return this.protocol;
    }

    public int getRaknetVersion() {
        return this.raknetVersion;
    }

    public BedrockPacketCodec getCodec() {
        return this.bedrockCodec;
    }

    public String getMinecraftVersion() {
        return this.getCodec().getMinecraftVersion();
    }

    public static MinecraftVersion fromVersionString(String version) {
        for (MinecraftVersion minecraftVersion : VALUES) {
            if (minecraftVersion.getMinecraftVersion().equals(version)) {
                return minecraftVersion;
            }
        }
        return null;
    }

    public static MinecraftVersion fromProtocol(int protocol) {
        for (MinecraftVersion minecraftVersion : VALUES) {
            if (minecraftVersion.getProtocol() == protocol) {
                return minecraftVersion;
            }
        }
        return null;
    }
}
