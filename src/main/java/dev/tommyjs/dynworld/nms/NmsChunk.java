package dev.tommyjs.dynworld.nms;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class NmsChunk {

    private static final Class<?> CHUNK = Reflect.nms("Chunk");
    private static final Class<?> SECTION = Reflect.nms("ChunkSection");

    private static final Constructor<?> NEW_SECTION = Reflect.constructor(SECTION, int.class, boolean.class);

    private static final Method INIT_LIGHTING = Reflect.method(CHUNK, "initLighting");

    private static final Field F_SECTIONS = Reflect.field(CHUNK, "sections");
    private static final Field F_BLOCK_IDS = Reflect.field(SECTION, "blockIds");
    private static final Field F_SKY_LIGHT = Reflect.field(SECTION, "skyLight");
    private static final Field F_EMITTED_LIGHT = Reflect.field(SECTION, "emittedLight");
    private static final Field F_NON_EMPTY_COUNT = Reflect.field(SECTION, "nonEmptyBlockCount");
    private static final Field F_NIBBLE_BYTES = Reflect.field(Reflect.nms("NibbleArray"), "a");
    private static final Field F_LOC_X = Reflect.field(CHUNK, "locX");
    private static final Field F_LOC_Z = Reflect.field(CHUNK, "locZ");

    private NmsChunk() {
    }

    public static Object @NotNull [] sections(@NotNull Object chunk) {
        return (Object[]) Reflect.get(F_SECTIONS, chunk);
    }

    public static char @NotNull [] blockIds(@NotNull Object section) {
        return (char[]) Reflect.get(F_BLOCK_IDS, section);
    }

    public static byte @Nullable [] skyLight(@NotNull Object section) {
        return nibbleBytes(Reflect.get(F_SKY_LIGHT, section));
    }

    public static byte @Nullable [] emittedLight(@NotNull Object section) {
        return nibbleBytes(Reflect.get(F_EMITTED_LIGHT, section));
    }

    public static @NotNull Object newSection(int sectionY, boolean skyLight) {
        return Reflect.construct(NEW_SECTION, sectionY, skyLight);
    }

    public static int nonEmptyCount(@NotNull Object section) {
        return (Integer) Reflect.get(F_NON_EMPTY_COUNT, section);
    }

    public static void setNonEmptyCount(@NotNull Object section, int value) {
        Reflect.set(F_NON_EMPTY_COUNT, section, value);
    }

    public static void initLighting(@NotNull Object chunk) {
        Reflect.invoke(INIT_LIGHTING, chunk);
    }

    public static int locX(@NotNull Object chunk) {
        return (Integer) Reflect.get(F_LOC_X, chunk);
    }

    public static int locZ(@NotNull Object chunk) {
        return (Integer) Reflect.get(F_LOC_Z, chunk);
    }

    private static byte[] nibbleBytes(Object nibbleArray) {
        return nibbleArray == null ? null : (byte[]) Reflect.get(F_NIBBLE_BYTES, nibbleArray);
    }

}
