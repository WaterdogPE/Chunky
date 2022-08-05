/*
 * Copyright 2022 WaterdogTEAM
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

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.nbt.NbtUtils;
import dev.waterdog.chunky.common.network.MinecraftVersion;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.List;

public class VanillaBlockStates {

    private static final VanillaBlockStates INSTANCE = new VanillaBlockStates();

    private final EnumMap<MinecraftVersion, BlockPaletteLegacy> defaultMappings = new EnumMap<>(MinecraftVersion.class);

    public static VanillaBlockStates get() {
        return INSTANCE;
    }

    public void initVanillaMapping(BlockPaletteFactory factory, MinecraftVersion version) {
        int protocol = getPaletteVersion(version);
        List<NbtMap> blockStates = this.loadPaletteStates("block_palette_" + protocol + ".nbt");
        BlockPaletteLegacy palette = factory.createLegacyBlockPalette(blockStates, protocol);
        this.defaultMappings.put(version, palette);
    }

    public BlockPaletteLegacy getOrCreatePalette(MinecraftVersion version, BlockPaletteFactory factory) {
        version = MinecraftVersion.fromProtocol(getPaletteVersion(version));
        BlockPaletteLegacy palette = this.defaultMappings.get(version);
        if (palette == null) {
            this.initVanillaMapping(factory, version);
        }
        return this.getVanillaPalette(version);
    }

    public BlockPaletteLegacy getVanillaPalette(MinecraftVersion version) {
        int protocol = getPaletteVersion(version);
        BlockPaletteLegacy palette = this.defaultMappings.get(MinecraftVersion.fromProtocol(protocol));
        if (palette == null) {
            throw new IllegalArgumentException("No default vanilla palette found for version " + version.name());
        }
        return palette;
    }


    private List<NbtMap> loadPaletteStates(String fileName) {
        try (InputStream stream = VanillaBlockStates.class.getClassLoader().getResourceAsStream(fileName)) {
            assert stream != null;
            return ((NbtMap) NbtUtils.createGZIPReader(stream).readTag()).getList("blocks", NbtType.COMPOUND);
        } catch (Exception e) {
            throw new AssertionError("Error loading block palette from " + fileName, e);
        }
    }

    public static int getPaletteVersion(MinecraftVersion version) {
        switch (version) {
            case MINECRAFT_PE_1_16:
            case MINECRAFT_PE_1_16_20:
                return MinecraftVersion.MINECRAFT_PE_1_16.getProtocol();
            case MINECRAFT_PE_1_16_100:
            case MINECRAFT_PE_1_16_200:
                return MinecraftVersion.MINECRAFT_PE_1_16_100.getProtocol();
            case MINECRAFT_PE_1_16_210:
            case MINECRAFT_PE_1_16_220:
                return MinecraftVersion.MINECRAFT_PE_1_16_210.getProtocol();
            case MINECRAFT_PE_1_17_0:
                return MinecraftVersion.MINECRAFT_PE_1_17_0.getProtocol();
            case MINECRAFT_PE_1_17_10:
                return MinecraftVersion.MINECRAFT_PE_1_17_10.getProtocol();
            case MINECRAFT_PE_1_17_30:
                return MinecraftVersion.MINECRAFT_PE_1_17_30.getProtocol();
            case MINECRAFT_PE_1_17_40:
                return MinecraftVersion.MINECRAFT_PE_1_17_40.getProtocol();
            case MINECRAFT_PE_1_18_0:
                return MinecraftVersion.MINECRAFT_PE_1_18_0.getProtocol();
            case MINECRAFT_PE_1_18_10:
                return MinecraftVersion.MINECRAFT_PE_1_18_10.getProtocol();
            case MINECRAFT_PE_1_18_30:
                return MinecraftVersion.MINECRAFT_PE_1_18_30.getProtocol();
            case MINECRAFT_PE_1_19_0:
            case MINECRAFT_PE_1_19_10:
                return MinecraftVersion.MINECRAFT_PE_1_19_0.getProtocol();
            case MINECRAFT_PE_1_13:
            case MINECRAFT_PE_1_14_30:
            case MINECRAFT_PE_1_14_60:
            default:
                throw new IllegalArgumentException("Default vanilla block palette not supported: " + version.name());
        }
    }
}
