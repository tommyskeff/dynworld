package dev.tommyjs.dynworld.chunk;

import dev.tommyjs.dynworld.block.BlockState;
import dev.tommyjs.dynworld.region.CapturedRegion;
import dev.tommyjs.dynworld.region.PasteOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ChunkView {

    int getX();

    int getZ();

    @NotNull Object getHandle();

    @NotNull BlockState getBlock(int x, int y, int z);

    void setBlock(int x, int y, int z, @NotNull BlockState state);

    void setBlock(int x, int y, int z, int id, int data);

    char @Nullable [] copySection(int sectionY);

    void setSection(int sectionY, char @NotNull [] data);

    void setRegion(@NotNull CapturedRegion region, int originX, int originY, int originZ, @NotNull PasteOptions options);

    void clear();

}
