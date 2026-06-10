package dev.tommyjs.dynworld.chunk;

import dev.tommyjs.dynworld.block.BlockState;
import dev.tommyjs.dynworld.nms.NmsBlockRegistry;
import dev.tommyjs.dynworld.nms.NmsChunk;
import dev.tommyjs.dynworld.region.CapturedRegion;
import dev.tommyjs.dynworld.region.PasteOptions;
import dev.tommyjs.dynworld.util.SectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class ChunkViewImpl implements ChunkView {

    private final Object handle;
    private final int x;
    private final int z;
    private final boolean skyLight;

    public ChunkViewImpl(@NotNull Object handle, int x, int z, boolean skyLight) {
        this.handle = handle;
        this.x = x;
        this.z = z;
        this.skyLight = skyLight;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getZ() {
        return z;
    }

    @Override
    public @NotNull Object getHandle() {
        return handle;
    }

    @Override
    public @NotNull BlockState getBlock(int x, int y, int z) {
        Object section = NmsChunk.sections(handle)[y >> 4];
        if (section == null) {
            return BlockState.AIR;
        }

        char index = NmsChunk.blockIds(section)[SectionUtil.getPositionIndex(x, y, z)];
        return BlockState.of(NmsBlockRegistry.getFullId(index));
    }

    @Override
    public void setBlock(int x, int y, int z, @NotNull BlockState state) {
        write(ensureSection(y >> 4), SectionUtil.getPositionIndex(x, y, z),
            state.isAir() ? 0 : NmsBlockRegistry.getIndex(state.getFullId()));
    }

    @Override
    public void setBlock(int x, int y, int z, int id, int data) {
        setBlock(x, y, z, BlockState.of(id, data));
    }

    @Override
    public char @Nullable [] copySection(int sectionY) {
        Object section = NmsChunk.sections(handle)[sectionY];
        return section == null ? null : NmsChunk.blockIds(section).clone();
    }

    @Override
    public void setSection(int sectionY, char @NotNull [] data) {
        int count = 0;
        for (char c : data) {
            if (c != 0) {
                count++;
            }
        }

        setSection(sectionY, data, count);
    }

    public void setSection(int sectionY, char @NotNull [] data, int nonAir) {
        Object section = ensureSection(sectionY);
        System.arraycopy(data, 0, NmsChunk.blockIds(section), 0, SectionUtil.VOLUME);
        NmsChunk.setNonEmptyCount(section, nonAir);
    }

    @Override
    public void setRegion(@NotNull CapturedRegion region, int originX, int originY, int originZ,
                          @NotNull PasteOptions options) {
        boolean ignoreAir = options.ignoreAir();
        boolean aligned = (originX & 15) == 0 && (originY & 15) == 0 && (originZ & 15) == 0;

        int minX = Math.max(x << 4, originX), maxX = Math.min((x << 4) + 16, originX + region.getWidth());
        int minZ = Math.max(z << 4, originZ), maxZ = Math.min((z << 4) + 16, originZ + region.getLength());
        int minY = Math.max(0, originY), maxY = Math.min(256, originY + region.getHeight());

        if (minX >= maxX || minZ >= maxZ || minY >= maxY) {
            return;
        }

        for (int sy = minY >> 4; sy <= ((maxY - 1) >> 4); sy++) {
            int wy0 = Math.max(sy << 4, minY), wy1 = Math.min((sy << 4) + 16, maxY);
            boolean full = maxX - minX == 16 && wy1 - wy0 == 16 && maxZ - minZ == 16;

            if (aligned && full && !ignoreAir) {
                int sx = (minX - originX) >> 4, ry = (wy0 - originY) >> 4, sz = (minZ - originZ) >> 4;
                if (region.isFullSection(sx, ry, sz)) {
                    char[] section = region.getSection(sx, ry, sz);
                    if (section == null) {
                        clearSection(sy);
                    } else {
                        setSection(sy, section, region.getSectionNonAir(sx, ry, sz));
                    }

                    continue;
                }
            }

            Object section = null;
            for (int wy = wy0; wy < wy1; wy++) {
                for (int wz = minZ; wz < maxZ; wz++) {
                    for (int wx = minX; wx < maxX; wx++) {
                        char c = region.getIndex(wx - originX, wy - originY, wz - originZ);
                        if (ignoreAir && c == 0) {
                            continue;
                        }
                        if (section == null) {
                            section = ensureSection(sy);
                        }

                        write(section, SectionUtil.getPositionIndex(wx, wy, wz), c);
                    }
                }
            }
        }
    }

    @Override
    public void clear() {
        Object[] sections = NmsChunk.sections(handle);
        for (int sy = 0; sy < sections.length; sy++) {
            if (sections[sy] != null) {
                clearSection(sy);
            }
        }
    }

    private void clearSection(int sy) {
        Object section = NmsChunk.sections(handle)[sy];
        if (section == null) {
            return;
        }

        char[] ids = NmsChunk.blockIds(section);
        Arrays.fill(ids, (char) 0);
        NmsChunk.setNonEmptyCount(section, 0);
    }

    private Object ensureSection(int sy) {
        Object[] sections = NmsChunk.sections(handle);
        Object section = sections[sy];
        if (section == null) {
            section = NmsChunk.newSection(sy << 4, skyLight);
            if (skyLight) {
                byte[] sky = NmsChunk.skyLight(section);
                if (sky != null) {
                    Arrays.fill(sky, (byte) -1);
                }
            }

            sections[sy] = section;
        }
        return section;
    }

    private void write(Object section, int idx, char value) {
        char[] ids = NmsChunk.blockIds(section);
        char prev = ids[idx];
        if (prev == value) {
            return;
        }

        ids[idx] = value;
        int delta = (prev == 0 ? 0 : -1) + (value == 0 ? 0 : 1);
        if (delta != 0) {
            NmsChunk.setNonEmptyCount(section, Math.max(0, NmsChunk.nonEmptyCount(section) + delta));
        }
    }

}
