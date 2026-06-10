package dev.tommyjs.dynworld.util;

public final class SectionUtil {

    public static final int SIZE = 16;
    public static final int VOLUME = SIZE * SIZE * SIZE;

    private SectionUtil() {
    }

    public static int getPositionIndex(int x, int y, int z) {
        return ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
    }

}
