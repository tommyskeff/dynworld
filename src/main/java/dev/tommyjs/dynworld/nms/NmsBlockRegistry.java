package dev.tommyjs.dynworld.nms;

import java.lang.reflect.Method;

public final class NmsBlockRegistry {

    private static final Class<?> BLOCK = Reflect.nms("Block");
    private static final Class<?> I_BLOCK_DATA = Reflect.nms("IBlockData");
    private static final Class<?> REGISTRY_ID = Reflect.nms("RegistryID");

    private static final Method BY_COMBINED_ID = Reflect.method(BLOCK, "getByCombinedId", int.class);
    private static final Method COMBINED_ID_OF = Reflect.method(BLOCK, "getCombinedId", I_BLOCK_DATA);
    private static final Method REGISTRY_BY_ID = Reflect.method(REGISTRY_ID, "a", int.class);
    private static final Method REGISTRY_ID_OF = Reflect.method(REGISTRY_ID, "b", Object.class);

    private static final Object BLOCK_REGISTRY = Reflect.staticField(BLOCK, "d");

    private static final char[] TO_REGISTRY = new char[1 << 16];
    private static final int[] TO_COMBINED = new int[1 << 16];
    private static final boolean[] TO_REGISTRY_KNOWN = new boolean[1 << 16];
    private static final boolean[] TO_COMBINED_KNOWN = new boolean[1 << 16];

    private NmsBlockRegistry() {
    }

    public static char getIndex(int fullId) {
        if (TO_REGISTRY_KNOWN[fullId]) {
            return TO_REGISTRY[fullId];
        }

        Object data = Reflect.invoke(BY_COMBINED_ID, null, fullId);
        char id = (char) (int) (Integer) Reflect.invoke(REGISTRY_ID_OF, BLOCK_REGISTRY, data);
        TO_REGISTRY[fullId] = id;
        TO_REGISTRY_KNOWN[fullId] = true;

        return id;
    }

    public static int getFullId(char index) {
        if (TO_COMBINED_KNOWN[index]) {
            return TO_COMBINED[index];
        }

        Object data = Reflect.invoke(REGISTRY_BY_ID, BLOCK_REGISTRY, (int) index);
        int combined = data == null ? 0 : (Integer) Reflect.invoke(COMBINED_ID_OF, null, data);
        TO_COMBINED[index] = combined;
        TO_COMBINED_KNOWN[index] = true;

        return combined;
    }

}
