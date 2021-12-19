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

import com.google.gson.JsonArray;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import dev.waterdog.chunky.common.util.Base64ArrayAdapter;
import dev.waterdog.chunky.common.util.Base64StringAdapter;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder(builderClassName = "Builder")
public class ExtraData {

    // Skin data
    private final String skinId;

    @JsonAdapter(Base64ArrayAdapter.class)
    private final byte[] skinData;
    private final int skinImageHeight;
    private final int skinImageWidth;

    @JsonAdapter(Base64StringAdapter.class)
    private final String skinResourcePatch;

    @JsonAdapter(Base64ArrayAdapter.class)
    private final byte[] skinGeometryData;

    @JsonAdapter(Base64ArrayAdapter.class)
    private final byte[] skinAnimationData;

    @SerializedName("AnimatedImageData")
    private final JsonArray personaAnimations;

    private final JsonArray personaPieces;

    @SerializedName("PieceTintColors")
    private final JsonArray personaPieceTints;

    private final String armSize;
    private final String playFabId;
    private final boolean personaSkin;
    private final boolean premiumSkin;
    private final boolean primaryUser;
    private final String skinColor;

    // Cape data
    private final String capeId;

    @JsonAdapter(Base64ArrayAdapter.class)
    private final byte[] capeData;

    private final int capeImageHeight;
    private final int capeImageWidth;
    private final boolean capeOnClassicSkin;

    // User data
    @SerializedName("DeviceOS")
    private final int deviceOs;

    private final String deviceId;
    private final String deviceModel;
    private final long clientRandomId;
    private final int currentInputMode;
    private final int defaultInputMode;
    private final int guiScale;
    private final String gameVersion;
    private final String languageCode;
    private final String platformOfflineId;
    private final String platformOnlineId;
    private final UUID selfSignedId;
    private final String serverAddress;
    private final String thirdPartyName;
    private final boolean thirdPartyNameOnly;

    @SerializedName("UIProfile")
    private final int uiProfile;
}
