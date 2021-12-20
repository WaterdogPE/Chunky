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

import com.google.common.base.Preconditions;
import com.nukkitx.nbt.NbtList;
import com.nukkitx.nbt.NbtMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class StateBlockPalette implements BlockPalette {

    private final Int2ObjectMap<NbtMap> runtime2StateMap = new Int2ObjectOpenHashMap<>();
    private final Object2IntMap<NbtMap> state2RuntimeMap = new Object2IntOpenHashMap<>();

    public StateBlockPalette(NbtList<NbtMap> mapping) {
        this.state2RuntimeMap.defaultReturnValue(-1);
        for (int i = 0; i < mapping.size(); i++) {
            this.registerState(mapping.get(i), i);
        }
    }

    private void registerState(NbtMap state, int runtimeId) {
        Preconditions.checkArgument(!this.runtime2StateMap.containsKey(runtimeId),
                "Mapping for runtimeId " + runtimeId + " is already created!");
        Preconditions.checkArgument(!this.state2RuntimeMap.containsKey(state),
                "Mapping for state is already created: " + state);
        this.runtime2StateMap.put(runtimeId, state);
        this.state2RuntimeMap.put(state, runtimeId);
    }

    @Override
    public int state2RuntimeId(NbtMap state) {
        return this.state2RuntimeMap.getInt(state);
    }

    @Override
    public NbtMap runtimeId2State(int runtimeId) {
        return this.runtime2StateMap.get(runtimeId);
    }
}
