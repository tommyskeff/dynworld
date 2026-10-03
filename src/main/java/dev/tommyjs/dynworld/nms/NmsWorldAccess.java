package dev.tommyjs.dynworld.nms;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class NmsWorldAccess {

    private static final Class<?> CRAFT_WORLD = Reflect.craft("CraftWorld");
    private static final Class<?> NMS_WORLD = Reflect.nms("World");
    private static final Class<?> BLOCK_POSITION = Reflect.nms("BlockPosition");
    private static final Class<?> CRAFT_PLAYER = Reflect.craft("entity.CraftPlayer");
    private static final Class<?> WORLD_SERVER = Reflect.nms("WorldServer");
    private static final Class<?> PLAYER_CHUNK_MAP = Reflect.nms("PlayerChunkMap");
    private static final Class<?> ENTITY_PLAYER = Reflect.nms("EntityPlayer");

    private static final Constructor<?> NEW_BLOCK_POSITION = Reflect.constructor(BLOCK_POSITION, int.class, int.class,
        int.class);

    private static final Method GET_HANDLE = Reflect.method(CRAFT_WORLD, "getHandle");
    private static final Method GET_CHUNK_AT = Reflect.method(NMS_WORLD, "getChunkAt", int.class, int.class);
    private static final Method NOTIFY = Reflect.method(NMS_WORLD, "notify", BLOCK_POSITION);
    private static final Method REFRESH_CHUNK = Reflect.method(CRAFT_WORLD, "refreshChunk", int.class, int.class);
    private static final Method GET_PLAYER_HANDLE = Reflect.method(CRAFT_PLAYER, "getHandle");
    private static final Method GET_PLAYER_CHUNK_MAP = Reflect.method(WORLD_SERVER, "getPlayerChunkMap");
    private static final Method IS_CHUNK_SENT = Reflect.method(PLAYER_CHUNK_MAP, "a", ENTITY_PLAYER, int.class,
        int.class);

    private NmsWorldAccess() {
    }

    public static @NotNull Object worldHandle(@NotNull World world) {
        return Reflect.invoke(GET_HANDLE, world);
    }

    public static @NotNull Object chunkAt(@NotNull Object worldHandle, int chunkX, int chunkZ) {
        return Reflect.invoke(GET_CHUNK_AT, worldHandle, chunkX, chunkZ);
    }

    public static void notifyBlock(@NotNull Object worldHandle, int x, int y, int z) {
        Reflect.invoke(NOTIFY, worldHandle, Reflect.construct(NEW_BLOCK_POSITION, x, y, z));
    }

    public static void refreshChunk(@NotNull World world, int chunkX, int chunkZ) {
        Reflect.invoke(REFRESH_CHUNK, world, chunkX, chunkZ);
    }

    public static boolean isChunkSent(@NotNull Player player, int chunkX, int chunkZ) {
        Object chunkMap = Reflect.invoke(GET_PLAYER_CHUNK_MAP, worldHandle(player.getWorld()));
        Object handle = Reflect.invoke(GET_PLAYER_HANDLE, player);
        return (boolean) Reflect.invoke(IS_CHUNK_SENT, chunkMap, handle, chunkX, chunkZ);
    }

}
