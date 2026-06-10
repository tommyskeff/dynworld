package dev.tommyjs.dynworld.world;

import dev.tommyjs.dynworld.block.BlockState;
import dev.tommyjs.dynworld.chunk.ChunkProvider;
import dev.tommyjs.dynworld.operation.Operation;
import dev.tommyjs.dynworld.region.CapturedRegion;
import dev.tommyjs.dynworld.region.PasteOptions;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.util.UUID;

public interface DynamicWorld extends ChunkProvider {

    static @NotNull DynamicWorldBuilder builder() {
        return new DynamicWorldBuilder();
    }

    static @NotNull DynamicWorld fromBukkit(@NotNull World world) {
        return new BukkitDynamicWorld(world);
    }

    static @NotNull DynamicWorld fromBukkit(@NotNull String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            throw new IllegalArgumentException("No world found with name: " + worldName);
        }

        return fromBukkit(world);
    }

    @NotNull World getBukkitWorld();

    @NotNull String getName();

    @NotNull UUID getId();

    boolean isActive();

    void loadChunkRegion(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ);

    boolean unloadChunk(int chunkX, int chunkZ);

    @NotNull BlockState getBlock(int x, int y, int z);

    void setBlock(int x, int y, int z, @NotNull BlockState state);

    void setRegion(@NotNull CapturedRegion region, int x, int y, int z, @NotNull PasteOptions options);

    @NotNull Operation prepareSetRegion(@NotNull CapturedRegion region, int x, int y, int z,
                                        @NotNull PasteOptions options);

}
