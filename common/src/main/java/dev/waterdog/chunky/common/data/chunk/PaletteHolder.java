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

package dev.waterdog.chunky.common.data.chunk;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaletteHolder {
    private int paletteHeader;
    private int[] words;
    private IntList palette;

    public int getBitsPerBlock() {
        return this.paletteHeader >> 1;
    }

    public boolean isPersistent() {
        return (this.paletteHeader & 0x01) == 0;
    }

    public PaletteHolder copy() {
        int[] words = new int[this.words.length];
        System.arraycopy(this.words, 0, words, 0, words.length);
        return new PaletteHolder(this.paletteHeader, words, new IntArrayList(this.palette));
    }
}
