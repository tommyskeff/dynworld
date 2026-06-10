package dev.tommyjs.dynworld.region;

import dev.tommyjs.dynworld.chunk.ChunkProvider;
import dev.tommyjs.dynworld.chunk.ChunkView;
import dev.tommyjs.dynworld.operation.Operation;
import org.jetbrains.annotations.NotNull;

public final class RegionPasteOperation implements Operation {

    private final ChunkProvider source;
    private final CapturedRegion region;
    private final PasteOptions options;

    private final int originX;
    private final int originY;
    private final int originZ;

    private final int minChunkX;
    private final int minChunkZ;
    private final int chunksX;
    private final int chunksZ;

    private int cursor;

    public RegionPasteOperation(@NotNull ChunkProvider source, @NotNull CapturedRegion region, int anchorX, int anchorY,
                                int anchorZ, @NotNull PasteOptions options) {
        this.source = source;
        this.region = region;
        this.options = options;
        this.originX = anchorX - region.getOffsetX();
        this.originY = anchorY - region.getOffsetY();
        this.originZ = anchorZ - region.getOffsetZ();
        this.minChunkX = originX >> 4;
        this.minChunkZ = originZ >> 4;
        this.chunksX = (((originX + region.getWidth() - 1) >> 4) - minChunkX) + 1;
        this.chunksZ = (((originZ + region.getLength() - 1) >> 4) - minChunkZ) + 1;
    }

    @Override
    public boolean execute(long maxTimeMillis) {
        long deadline = maxTimeMillis <= 0 ? Long.MAX_VALUE : System.currentTimeMillis() + maxTimeMillis;
        int total = chunksX * chunksZ;

        while (cursor < total) {
            int cx = minChunkX + (cursor % chunksX);
            int cz = minChunkZ + (cursor / chunksX);

            ChunkView view = source.getChunkAt(cx, cz);
            view.setRegion(region, originX, originY, originZ, options);

            if (options.refresh()) {
                source.refreshChunk(cx, cz);
            }

            cursor++;

            if (System.currentTimeMillis() >= deadline) {
                break;
            }
        }

        return cursor >= total;
    }

}
