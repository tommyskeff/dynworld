package dev.tommyjs.dynworld.world;

import dev.tommyjs.dynworld.block.BlockState;
import dev.tommyjs.dynworld.chunk.ChunkView;
import dev.tommyjs.dynworld.chunk.ChunkViewImpl;
import dev.tommyjs.dynworld.operation.Operation;
import dev.tommyjs.dynworld.region.CapturedRegion;
import dev.tommyjs.dynworld.region.PasteOptions;
import dev.tommyjs.dynworld.region.RegionPasteOperation;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

final class DynamicWorldImpl implements DynamicWorld {

    private final String name;
    private final UUID id;
    private final Object worldServer;
    private final World bukkit;
    private final Object chunkProvider;
    private final ChunkGenerator generator;
    private final ChunkStorage storage;
    private final boolean skyLight;
    private final ReentrantLock chunkLock = new ReentrantLock();

    private boolean active;

    DynamicWorldImpl(String name, UUID id, Object worldServer, World bukkit, Object chunkProvider,
                     ChunkGenerator generator, ChunkStorage storage, boolean skyLight) {
        this.name = name;
        this.id = id;
        this.worldServer = worldServer;
        this.bukkit = bukkit;
        this.chunkProvider = chunkProvider;
        this.generator = generator;
        this.storage = storage;
        this.skyLight = skyLight;
    }

    @Override
    public @NotNull World getBukkitWorld() {
        return bukkit;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull UUID getId() {
        return id;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public @NotNull ChunkView getChunkAt(int chunkX, int chunkZ) {
        chunkLock.lock();
        try {
            Object handle = WorldInjector.chunkIfLoaded(chunkProvider, chunkX, chunkZ);
            if (handle == null) {
                handle = WorldInjector.loadChunk(worldServer, chunkProvider, chunkX, chunkZ, generator, skyLight);
            }
            return new ChunkViewImpl(handle, chunkX, chunkZ, skyLight);
        } finally {
            chunkLock.unlock();
        }
    }

    @Override
    public void refreshChunk(int chunkX, int chunkZ) {
        WorldInjector.refreshChunk(bukkit, chunkX, chunkZ);
    }

    @Override
    public void loadChunkRegion(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        chunkLock.lock();
        try {
            for (int x = minChunkX; x <= maxChunkX; x++) {
                for (int z = minChunkZ; z <= maxChunkZ; z++) {
                    getChunkAt(x, z);
                }
            }
        } finally {
            chunkLock.unlock();
        }
    }

    @Override
    public boolean unloadChunk(int chunkX, int chunkZ) {
        chunkLock.lock();
        try {
            return WorldInjector.unloadChunk(chunkProvider, chunkX, chunkZ);
        } finally {
            chunkLock.unlock();
        }
    }

    @Override
    public @NotNull BlockState getBlock(int x, int y, int z) {
        return getChunkAt(x >> 4, z >> 4).getBlock(x & 15, y, z & 15);
    }

    @Override
    public void setBlock(int x, int y, int z, @NotNull BlockState state) {
        getChunkAt(x >> 4, z >> 4).setBlock(x & 15, y, z & 15, state);
        if (active) {
            refreshChunk(x >> 4, z >> 4);
        }
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

    Object worldServer() {
        return worldServer;
    }

    ChunkStorage storage() {
        return storage;
    }

    void setActive(boolean active) {
        this.active = active;
    }

}
