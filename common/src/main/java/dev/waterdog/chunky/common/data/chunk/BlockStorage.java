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

package dev.waterdog.chunky.common.data.chunk;

import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Data;


@Data
public class BlockStorage {
    private boolean legacy;
    // Legacy data
    private byte[] blockIds;
    private byte[] blockData;
    // New paletted format
    private int paletteHeader;
    private byte[] words;
    private IntList palette;

    public int getBitsPerBlock() {
        return this.paletteHeader >> 1;
    }

    public boolean isPersistent() {
        return (this.paletteHeader & 0x01) == 0;
    }
}
