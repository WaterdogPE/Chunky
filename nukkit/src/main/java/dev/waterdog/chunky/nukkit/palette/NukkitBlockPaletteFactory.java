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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.nbt.NbtUtils;
import dev.waterdog.chunky.common.palette.BlockPalette;
import dev.waterdog.chunky.common.palette.BlockPaletteFactory;
import dev.waterdog.chunky.common.palette.BlockPaletteLegacy;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NukkitBlockPaletteFactory implements BlockPaletteFactory {

    private static NukkitBlockPaletteFactory instance;
    private final BiMap<Integer, NbtMap> vanillaStates = HashBiMap.create();

    public static NukkitBlockPaletteFactory get() {
        if (instance == null) {
            instance = new NukkitBlockPaletteFactory();
        }
        return instance;
    }

    public NukkitBlockPaletteFactory() {
        this.loadVanillaStates();
    }

    private void loadVanillaStates() {
        List<NbtMap> states;
        try (InputStream stream = NukkitBlockPaletteFactory.class.getClassLoader().getResourceAsStream("latest_palette.nbt")) {
            states = ((NbtMap) NbtUtils.createGZIPReader(stream).readTag()).getList("blocks", NbtType.COMPOUND);
        } catch (Exception e) {
            throw new AssertionError("Error loading block palette latest_palette.nbt", e);
        }

        for (int i = 0; i < states.size(); i++) {
            this.vanillaStates.put(i, states.get(i));
        }
    }

    public Map<Integer, NbtMap> getVanillaStates() {
        return Collections.unmodifiableMap(this.vanillaStates);
    }

    public int state2Runtime(NbtMap state) {
        Integer runtimeId = this.vanillaStates.inverse().get(state);
        return runtimeId == null ? -1 : runtimeId;
    }

    public NbtMap runtime2State(int runtimeId) {
        return this.vanillaStates.get(runtimeId);
    }

    @Override
    public BlockPalette createBlockPalette(List<NbtMap> blockStates, int version) {
        return new NukkitLegacyBlockPalette(blockStates, version, this);
    }

    @Override
    public BlockPaletteLegacy createLegacyBlockPalette(List<NbtMap> blockStates, int version) {
        return new NukkitLegacyBlockPalette(blockStates, version, this);
    }
}
