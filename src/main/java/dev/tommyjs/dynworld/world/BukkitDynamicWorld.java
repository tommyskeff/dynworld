package dev.tommyjs.dynworld.world;

import dev.tommyjs.dynworld.block.BlockState;
import dev.tommyjs.dynworld.chunk.ChunkView;
import dev.tommyjs.dynworld.chunk.ChunkViewImpl;
import dev.tommyjs.dynworld.nms.NmsWorldAccess;
import dev.tommyjs.dynworld.operation.Operation;
import dev.tommyjs.dynworld.region.CapturedRegion;
import dev.tommyjs.dynworld.region.PasteOptions;
import dev.tommyjs.dynworld.region.RegionPasteOperation;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A {@link DynamicWorld} adapted from an already-loaded Bukkit world (see
 * {@link DynamicWorld#fromBukkit(World)}). It exposes the same block/region/chunk
 * operations against the world's existing NMS handle, but owns no lifecycle: it is
 * never registered with a {@link DynamicWorldManager} and cannot be unloaded
 * through one. The world keeps its real chunk generator, so chunks are loaded
 * through Bukkit's normal generation path rather than injected empty.
 */
final class BukkitDynamicWorld implements DynamicWorld {

    private final World world;
    private final Object handle;
    private final boolean skyLight;

    BukkitDynamicWorld(@NotNull World world) {
        this.world = world;
        this.handle = NmsWorldAccess.worldHandle(world);
        this.skyLight = world.getEnvironment() == World.Environment.NORMAL;
    }

    @Override
    public @NotNull World getBukkitWorld() {
        return world;
    }

    @Override
    public @NotNull String getName() {
        return world.getName();
    }

    @Override
    public @NotNull UUID getId() {
        return world.getUID();
    }

    @Override
    public boolean isActive() {
        // We don't own this world's lifecycle; "active" just means it's still loaded.
        return Bukkit.getWorld(world.getUID()) != null;
    }

    @Override
    public @NotNull ChunkView getChunkAt(int chunkX, int chunkZ) {
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            world.loadChunk(chunkX, chunkZ);
        }
        return new ChunkViewImpl(NmsWorldAccess.chunkAt(handle, chunkX, chunkZ), chunkX, chunkZ, skyLight);
    }

    @Override
    public void refreshChunk(int chunkX, int chunkZ) {
        NmsWorldAccess.refreshChunk(world, chunkX, chunkZ);
    }

    @Override
    public void loadChunkRegion(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                getChunkAt(x, z);
            }
        }
    }

    @Override
    public boolean unloadChunk(int chunkX, int chunkZ) {
        return world.unloadChunk(chunkX, chunkZ);
    }

    @Override
    public @NotNull BlockState getBlock(int x, int y, int z) {
        return getChunkAt(x >> 4, z >> 4).getBlock(x & 15, y, z & 15);
    }

    @Override
    public void setBlock(int x, int y, int z, @NotNull BlockState state) {
        getChunkAt(x >> 4, z >> 4).setBlock(x & 15, y, z & 15, state);
        NmsWorldAccess.notifyBlock(handle, x, y, z);
    }

    @Override
    public void setRegion(@NotNull CapturedRegion region, int x, int y, int z, @NotNull PasteOptions options) {
        prepareSetRegion(region, x, y, z, options).execute();
    }

    @Override
    public @NotNull Operation prepareSetRegion(@NotNull CapturedRegion region, int x, int y, int z,
                                               @NotNull PasteOptions options) {
        return new RegionPasteOperation(this, region, x, y, z, options);
    }

}
