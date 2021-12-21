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

package dev.waterdog.chunky.common.palette;

import com.nukkitx.nbt.NbtList;
import com.nukkitx.nbt.NbtMap;

public class LegacyStateBlockPalette extends StateBlockPalette implements BlockPaletteLegacy {

    /*
     * This implementation of legacy block palette assumes legacy full IDs are same as runtime IDs
     */

    public LegacyStateBlockPalette(NbtList<NbtMap> mapping) {
        super(mapping);
    }

    @Override
    public int runtimeId2LegacyFullId(int runtimeId) {
        return runtimeId;
    }

    public int runtimeId2LegacyId(int runtimeId) {
        return runtimeId >> 6;
    }

    public int runtimeId2LegacyData(int runtimeId) {
        return runtimeId & 0xf;
    }

    @Override
    public int state2LegacyId(NbtMap state) {
        int runtimeId = this.state2RuntimeId(state);
        if (runtimeId != -1) {
            return this.runtimeId2LegacyId(runtimeId);
        }
        return runtimeId;
    }

    @Override
    public int state2LegacyData(NbtMap state) {
        int runtimeId = this.state2RuntimeId(state);
        if (runtimeId != -1) {
            return this.runtimeId2LegacyData(runtimeId);
        }
        return runtimeId;
    }

    @Override
    public NbtMap legacy2State(int legacyId, int legacyData) {
        int runtimeId = legacyId << 6 | legacyData;
        return this.runtimeId2State(runtimeId);
    }
}
