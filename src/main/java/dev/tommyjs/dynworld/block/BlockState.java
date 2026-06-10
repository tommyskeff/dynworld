package dev.tommyjs.dynworld.block;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public final class BlockState {

    public static final BlockState AIR = new BlockState(0, 0);

    private final int id;
    private final int data;

    private BlockState(int id, int data) {
        this.id = id;
        this.data = data;
    }

    public static @NotNull BlockState of(int id, int data) {
        return id == 0 ? AIR : new BlockState(id & 0xFFF, data & 0xF);
    }

    @SuppressWarnings("deprecation")
    public static @NotNull BlockState of(@NotNull Material material, int data) {
        return of(material.getId(), data);
    }

    public static @NotNull BlockState of(int fullId) {
        return of(fullId & 0xFFF, (fullId >> 12) & 0xF);
    }

    public int getId() {
        return id;
    }

    public int getData() {
        return data;
    }

    public int getFullId() {
        return id | (data << 12);
    }

    public boolean isAir() {
        return id == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof BlockState other && other.id == id && other.data == data;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id) * 31 + Integer.hashCode(data);
    }

    @Override
    public String toString() {
        return "BlockState(" + id + ":" + data + ")";
    }

}
