package dev.tommyjs.dynworld.region;

import dev.tommyjs.dynworld.block.BlockState;
import net.querz.nbt.io.NBTDeserializer;
import net.querz.nbt.io.NBTSerializer;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class SchematicUtil {

    private SchematicUtil() {
    }

    public static void write(@NotNull CapturedRegion region, @NotNull Path file) throws IOException {
        try (OutputStream out = Files.newOutputStream(file)) {
            write(region, out);
        }
    }

    public static void write(@NotNull CapturedRegion region, @NotNull OutputStream out) throws IOException {
        int width = region.getWidth();
        int height = region.getHeight();
        int length = region.getLength();
        int volume = width * height * length;

        byte[] blocks = new byte[volume];
        byte[] data = new byte[volume];
        byte[] addBlocks = null;

        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    BlockState state = region.getBlock(x, y, z);
                    int id = state.getId();
                    int index = (y * length + z) * width + x;

                    blocks[index] = (byte) (id & 0xFF);
                    data[index] = (byte) state.getData();

                    if (id > 0xFF) {
                        if (addBlocks == null) {
                            addBlocks = new byte[(volume + 1) >> 1];
                        }
                        int high = (id >> 8) & 0x0F;
                        if ((index & 1) == 0) {
                            addBlocks[index >> 1] |= (byte) high;
                        } else {
                            addBlocks[index >> 1] |= (byte) (high << 4);
                        }
                    }
                }
            }
        }

        CompoundTag schematic = new CompoundTag();
        schematic.putShort("Width", (short) width);
        schematic.putShort("Height", (short) height);
        schematic.putShort("Length", (short) length);
        schematic.putString("Materials", "Alpha");
        schematic.putByteArray("Blocks", blocks);
        schematic.putByteArray("Data", data);
        if (addBlocks != null) {
            schematic.putByteArray("AddBlocks", addBlocks);
        }
        schematic.putInt("WEOffsetX", region.getOffsetX());
        schematic.putInt("WEOffsetY", region.getOffsetY());
        schematic.putInt("WEOffsetZ", region.getOffsetZ());

        BufferedOutputStream buffered = new BufferedOutputStream(out);
        GZIPOutputStream gzip = new GZIPOutputStream(buffered);
        new NBTSerializer(false).toStream(new NamedTag("Schematic", schematic), gzip);
        gzip.finish();
        buffered.flush();
    }

    public static @NotNull CapturedRegion read(@NotNull Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return read(in);
        }
    }

    public static @NotNull CapturedRegion read(@NotNull InputStream in) throws IOException {
        NamedTag named = new NBTDeserializer(false).fromStream(new GZIPInputStream(new BufferedInputStream(in)));
        CompoundTag schematic = (CompoundTag) named.getTag();

        int width = schematic.getShort("Width") & 0xFFFF;
        int height = schematic.getShort("Height") & 0xFFFF;
        int length = schematic.getShort("Length") & 0xFFFF;

        byte[] blocks = schematic.getByteArray("Blocks");
        byte[] data = schematic.getByteArray("Data");
        byte[] addBlocks = schematic.containsKey("AddBlocks") ? schematic.getByteArray("AddBlocks") : null;

        int offsetX = schematic.getInt("WEOffsetX");
        int offsetY = schematic.getInt("WEOffsetY");
        int offsetZ = schematic.getInt("WEOffsetZ");

        CapturedRegion region = CapturedRegion.empty(width, height, length, offsetX, offsetY, offsetZ);

        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int index = (y * length + z) * width + x;
                    int id = blocks[index] & 0xFF;
                    if (addBlocks != null && (index >> 1) < addBlocks.length) {
                        int add =
                            (index & 1) == 0 ? (addBlocks[index >> 1] & 0x0F) : ((addBlocks[index >> 1] & 0xF0) >> 4);
                        id |= add << 8;
                    }
                    if (id == 0) {
                        continue;
                    }
                    region.setBlock(x, y, z, id, data[index] & 0x0F);
                }
            }
        }

        return region;
    }

}
