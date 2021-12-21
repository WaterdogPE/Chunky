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

package dev.waterdog.chunky.nukkit.palette;

import cn.nukkit.level.GlobalBlockPalette;
import com.google.common.base.Preconditions;
import com.nukkitx.blockstateupdater.BlockStateUpdaters;
import com.nukkitx.nbt.NbtList;
import com.nukkitx.nbt.NbtMap;
import dev.waterdog.chunky.common.palette.BlockPaletteLegacy;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class NukkitLegacyBlockPalette implements BlockPaletteLegacy {

    private final Object2IntMap<NbtMap> state2RuntimeMap = new Object2IntOpenHashMap<>();
    private final Int2ObjectMap<NbtMap> runtime2StateMap = new Int2ObjectOpenHashMap<>();
    private final Int2IntMap runtime2FullIdMap = new Int2IntOpenHashMap();
    private final Object2IntMap<NbtMap> state2FullIdMap = new Object2IntOpenHashMap<>();
    private final Int2ObjectMap<NbtMap> legacyId2UpdatedStateMap = new Int2ObjectOpenHashMap<>();
    private final int version;

    public NukkitLegacyBlockPalette(NbtList<NbtMap> mapping, int version, NukkitBlockPaletteFactory factory) {
        this.state2RuntimeMap.defaultReturnValue(-1);
        this.runtime2FullIdMap.defaultReturnValue(-1);
        for (int i = 0; i < mapping.size(); i++) {
            this.registerState(mapping.get(i), i, factory);
        }
        this.version = version;
    }

    private void registerState(NbtMap state, int runtimeId, NukkitBlockPaletteFactory factory) {
        Preconditions.checkArgument(!this.runtime2StateMap.containsKey(runtimeId),
                "Mapping for runtimeId " + runtimeId + " is already created!");
        Preconditions.checkArgument(!this.state2RuntimeMap.containsKey(state),
                "Mapping for state is already created: " + state);
        this.runtime2StateMap.put(runtimeId, state);
        this.state2RuntimeMap.put(state, runtimeId);

        // Setup mapping to latest states here so we don't do it on runtime
        NbtMap blockState = state.getCompound("block");
        NbtMap updatedState = BlockStateUpdaters.updateBlockState(blockState, blockState.getInt("version"));
        int updatedRuntimeId = factory.state2Runtime(updatedState);
        int legacyFullId = GlobalBlockPalette.getLegacyFullId(updatedRuntimeId);

        this.runtime2FullIdMap.put(runtimeId, legacyFullId);
        this.state2FullIdMap.put(state, legacyFullId);
        this.legacyId2UpdatedStateMap.put(legacyFullId, updatedState);
    }

    @Override
    public int state2RuntimeId(NbtMap state) {
        return this.state2RuntimeMap.getInt(state);
    }

    @Override
    public NbtMap runtimeId2State(int runtimeId) {
        return this.runtime2StateMap.get(runtimeId);
    }

    @Override
    public int runtimeId2LegacyFullId(int runtimeId) {
        return this.runtime2FullIdMap.get(runtimeId);
    }

    @Override
    public int state2LegacyId(NbtMap state) {
        int fullId = this.state2FullIdMap.getInt(state);
        if (fullId != -1) {
            return fullId >> 6;
        }
        return fullId;
    }

    @Override
    public int state2LegacyData(NbtMap state) {
        int fullId = this.state2FullIdMap.getInt(state);
        if (fullId != -1) {
            return fullId & 0xf;
        }
        return fullId;
    }

    @Override
    public NbtMap legacy2State(int legacyId, int legacyData) {
        int fillId = legacyId << 6 | legacyData;
        return this.legacyId2UpdatedStateMap.get(fillId);
    }
}
