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

package dev.waterdog.chunky.nukkit.world.anvil;

import cn.nukkit.level.format.anvil.util.BlockStorage;
import cn.nukkit.level.util.BitArray;
import cn.nukkit.level.util.BitArrayVersion;
import it.unimi.dsi.fastutil.ints.IntList;

public class PalettedBlockStorage {

    public static final int SIZE = 4096;
    private final IntList palette;
    private BitArray bitArray;

    public PalettedBlockStorage(BitArray bitArray, IntList palette) {
        this.palette = palette;
        this.bitArray = bitArray;
    }

    private int getPaletteHeader(BitArrayVersion version, boolean runtime) {
        return (version.getId() << 1) | (runtime ? 1 : 0);
    }

    private int getIndex(int x, int y, int z) {
        return (x << 8) + (z << 4) + y;
    }

    public void setBlock(int index, int runtimeId) {
        try {
            int id = this.idFor(runtimeId);
            this.bitArray.set(index, id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unable to set block runtime ID: " + runtimeId + ", palette: " + palette, e);
        }
    }

    public int getBlock(int x, int y, int z) {
        return this.getBlock(this.getIndex(x, y, z));
    }

    public int getBlock(int index) {
        int paletteId = this.bitArray.get(index);
        return this.palette.getInt(paletteId);
    }

    private void onResize(BitArrayVersion version) {
        BitArray newBitArray = version.createPalette(SIZE);

        for (int i = 0; i < SIZE; i++) {
            newBitArray.set(i, this.bitArray.get(i));
        }
        this.bitArray = newBitArray;
    }

    private int idFor(int runtimeId) {
        int index = this.palette.indexOf(runtimeId);
        if (index != -1) {
            return index;
        }

        index = this.palette.size();
        BitArrayVersion version = this.bitArray.getVersion();
        if (index > version.getMaxEntryValue()) {
            BitArrayVersion next = version.next();
            if (next != null) {
                this.onResize(next);
            }
        }
        this.palette.add(runtimeId);
        return index;
    }
}
