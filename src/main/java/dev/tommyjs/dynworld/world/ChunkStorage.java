package dev.tommyjs.dynworld.world;

import dev.tommyjs.dynworld.chunk.ChunkView;
import org.jetbrains.annotations.NotNull;

public interface ChunkStorage {

    boolean load(int chunkX, int chunkZ, @NotNull ChunkView chunk);

    void save(int chunkX, int chunkZ, @NotNull ChunkView chunk);

    default void close() {
    }

    ChunkStorage EMPTY = new ChunkStorage() {

        @Override
        public boolean load(int chunkX, int chunkZ, @NotNull ChunkView chunk) {
            return false;
        }

        @Override
        public void save(int chunkX, int chunkZ, @NotNull ChunkView chunk) {
        }

    };

}
