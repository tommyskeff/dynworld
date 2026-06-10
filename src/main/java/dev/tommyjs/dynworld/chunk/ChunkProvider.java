package dev.tommyjs.dynworld.chunk;

import org.jetbrains.annotations.NotNull;

public interface ChunkProvider {

    @NotNull ChunkView getChunkAt(int chunkX, int chunkZ);

    void refreshChunk(int chunkX, int chunkZ);

}
