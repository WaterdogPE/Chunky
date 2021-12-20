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

package dev.waterdog.chunky.common.util;

import java.util.function.LongConsumer;

public class ChunkUtils {

    public static long chunkIndex(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) | (chunkZ & 0xffffffffL);
    }

    public static int chunkX(long index) {
        return (int) (index >> 32);
    }

    public static int chunkZ(long index) {
        return (int) index;
    }

    public static boolean isIndexInRadius(int chunkX, int chunkZ, int radius, long index){
        int minX = chunkX - radius;
        int minZ = chunkZ - radius;
        int maxX = chunkX + radius;
        int maxZ = chunkZ + radius;
        int x = chunkX(index);
        int z = chunkZ(index);
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public static void checkChunksInRadius(int chunkX, int chunkZ, int radius, LongConsumer consumer) {
        int squaredRadius = radius * radius;

        for (int x = 0; x <= radius; x++) {
            int squaredX = x * x;
            for (int z = 0; z <= x; z++) {
                int distanceSqr = squaredX + z * z;
                if (distanceSqr > squaredRadius) {
                    continue;
                }
                // Top right quadrant
                consumer.accept(chunkIndex(chunkX + x, chunkZ + z));
                // Top left quadrant
                consumer.accept(chunkIndex(chunkX - x - 1, chunkZ + z));
                // Bottom right quadrant
                consumer.accept(chunkIndex(chunkX + x, chunkZ - z - 1));
                // Bottom left quadrant
                consumer.accept(chunkIndex(chunkX - x - 1, chunkZ - z - 1));
                if (x == z) {
                    continue;
                }
                // Top right quadrant mirror
                consumer.accept(chunkIndex(chunkX + z, chunkZ + x));
                // Top left quadrant mirror
                consumer.accept(chunkIndex(chunkX - z - 1, chunkZ + x));
                // Bottom right quadrant mirror
                consumer.accept(chunkIndex(chunkX + z, chunkZ - x - 1));
                // Bottom left quadrant mirror
                consumer.accept(chunkIndex(chunkX - z - 1, chunkZ - x - 1));
            }
        }
    }
}
