package dev.tommyjs.dynworld.world;

import dev.tommyjs.dynworld.chunk.ChunkView;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ChunkGenerator {

    void generate(int chunkX, int chunkZ, @NotNull ChunkView chunk);

    ChunkGenerator VOID = (chunkX, chunkZ, chunk) -> {};

}
