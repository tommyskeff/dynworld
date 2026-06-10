package dev.tommyjs.dynworld.region;

import dev.tommyjs.dynworld.block.BlockState;
import dev.tommyjs.dynworld.chunk.ChunkProvider;
import dev.tommyjs.dynworld.chunk.ChunkView;
import dev.tommyjs.dynworld.nms.NmsBlockRegistry;
import dev.tommyjs.dynworld.util.SectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CapturedRegion {

    private final int width;
    private final int height;
    private final int length;

    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;

    private final int sectionsX;
    private final int sectionsY;
    private final int sectionsZ;

    private final char[][] sections;
    private final int[] nonAir;

    private CapturedRegion(int width, int height, int length, int offsetX, int offsetY, int offsetZ) {
        this.width = width;
        this.height = height;
        this.length = length;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.sectionsX = ceilDiv(width);
        this.sectionsY = ceilDiv(height);
        this.sectionsZ = ceilDiv(length);
        this.sections = new char[sectionsX * sectionsY * sectionsZ][];
        this.nonAir = new int[sections.length];
    }

    public static @NotNull CapturedRegion empty(int width, int height, int length,
                                                int offsetX, int offsetY, int offsetZ) {
        return new CapturedRegion(width, height, length, offsetX, offsetY, offsetZ);
    }

    public static @NotNull CapturedRegion capture(@NotNull ChunkProvider source,
                                                  int x1, int y1, int z1, int x2, int y2, int z2,
                                                  int anchorX, int anchorY, int anchorZ) {
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.max(0, Math.min(y1, y2)), maxY = Math.min(255, Math.max(y1, y2));
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);

        CapturedRegion region = new CapturedRegion(
            maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1,
            anchorX - minX, anchorY - minY, anchorZ - minZ);

        boolean aligned = (minX & 15) == 0 && (minY & 15) == 0 && (minZ & 15) == 0;

        for (int cx = minX >> 4; cx <= (maxX >> 4); cx++) {
            for (int cz = minZ >> 4; cz <= (maxZ >> 4); cz++) {
                ChunkView view = source.getChunkAt(cx, cz);
                for (int sy = minY >> 4; sy <= (maxY >> 4); sy++) {
                    int wx0 = Math.max(cx << 4, minX), wx1 = Math.min((cx << 4) + 16, maxX + 1);
                    int wy0 = Math.max(sy << 4, minY), wy1 = Math.min((sy << 4) + 16, maxY + 1);
                    int wz0 = Math.max(cz << 4, minZ), wz1 = Math.min((cz << 4) + 16, maxZ + 1);
                    if (wx0 >= wx1 || wy0 >= wy1 || wz0 >= wz1) {
                        continue;
                    }

                    char[] section = view.copySection(sy);
                    if (section == null) {
                        continue;
                    }

                    boolean full = wx1 - wx0 == 16 && wy1 - wy0 == 16 && wz1 - wz0 == 16;
                    if (aligned && full) {
                        region.setSection((wx0 - minX) >> 4, (wy0 - minY) >> 4, (wz0 - minZ) >> 4, section);
                        continue;
                    }

                    for (int wy = wy0; wy < wy1; wy++) {
                        for (int wz = wz0; wz < wz1; wz++) {
                            for (int wx = wx0; wx < wx1; wx++) {
                                char c = section[SectionUtil.getPositionIndex(wx, wy, wz)];
                                if (c != 0) {
                                    region.setIndex(wx - minX, wy - minY, wz - minZ, c);
                                }
                            }
                        }
                    }
                }
            }
        }

        return region;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getLength() {
        return length;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public int getOffsetZ() {
        return offsetZ;
    }

    public @NotNull BlockState getBlock(int x, int y, int z) {
        return BlockState.of(NmsBlockRegistry.getFullId(getIndex(x, y, z)));
    }

    public void setBlock(int x, int y, int z, @NotNull BlockState state) {
        setIndex(x, y, z, state.isAir() ? 0 : NmsBlockRegistry.getIndex(state.getFullId()));
    }

    public void setBlock(int x, int y, int z, int id, int data) {
        setBlock(x, y, z, BlockState.of(id, data));
    }

    public char @Nullable [] getSection(int sx, int sy, int sz) {
        return sections[sectionIdx(sx, sy, sz)];
    }

    private void setSection(int sx, int sy, int sz, char[] section) {
        int i = sectionIdx(sx, sy, sz);
        sections[i] = section;
        int count = 0;
        for (char c : section) {
            if (c != 0) {
                count++;
            }
        }
        nonAir[i] = count;
    }

    public boolean isFullSection(int sx, int sy, int sz) {
        return (sx + 1) * 16 <= width && (sy + 1) * 16 <= height && (sz + 1) * 16 <= length;
    }

    public int getSectionNonAir(int sx, int sy, int sz) {
        return nonAir[sectionIdx(sx, sy, sz)];
    }

    public char getIndex(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= width || y >= height || z >= length) {
            return 0;
        }
        char[] section = sections[sectionIdx(x >> 4, y >> 4, z >> 4)];
        return section == null ? 0 : section[SectionUtil.getPositionIndex(x, y, z)];
    }

    private void setIndex(int x, int y, int z, char value) {
        int i = sectionIdx(x >> 4, y >> 4, z >> 4);
        char[] section = sections[i];
        if (section == null) {
            if (value == 0) {
                return;
            }
            section = new char[SectionUtil.VOLUME];
            sections[i] = section;
        }
        int idx = SectionUtil.getPositionIndex(x, y, z);
        char prev = section[idx];
        section[idx] = value;
        nonAir[i] += (prev == 0 ? 0 : -1) + (value == 0 ? 0 : 1);
    }

    private int sectionIdx(int sx, int sy, int sz) {
        return (sy * sectionsZ + sz) * sectionsX + sx;
    }

    private static int ceilDiv(int v) {
        return (v + 15) >> 4;
    }

}
