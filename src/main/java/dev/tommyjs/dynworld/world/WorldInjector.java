package dev.tommyjs.dynworld.world;

import dev.tommyjs.dynworld.chunk.ChunkViewImpl;
import dev.tommyjs.dynworld.nms.NmsChunk;
import dev.tommyjs.dynworld.nms.Reflect;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class WorldInjector {

    private static final AtomicInteger DIMENSION = new AtomicInteger(1000);

    private static final Class<?> MINECRAFT_SERVER = Reflect.nms("MinecraftServer");
    private static final Class<?> WORLD_SERVER = Reflect.nms("WorldServer");
    private static final Class<?> NMS_WORLD = Reflect.nms("World");
    private static final Class<?> WORLD_DATA = Reflect.nms("WorldData");
    private static final Class<?> WORLD_SETTINGS = Reflect.nms("WorldSettings");
    private static final Class<?> ENUM_GAMEMODE = Reflect.nms("WorldSettings$EnumGamemode");
    private static final Class<?> WORLD_TYPE = Reflect.nms("WorldType");
    private static final Class<?> METHOD_PROFILER = Reflect.nms("MethodProfiler");
    private static final Class<?> I_DATA_MANAGER = Reflect.nms("IDataManager");
    private static final Class<?> I_CHUNK_LOADER = Reflect.nms("IChunkLoader");
    private static final Class<?> I_WORLD_ACCESS = Reflect.nms("IWorldAccess");
    private static final Class<?> WORLD_MANAGER = Reflect.nms("WorldManager");
    private static final Class<?> NMS_CHUNK = Reflect.nms("Chunk");
    private static final Class<?> CHUNK_PROVIDER_SERVER = Reflect.nms("ChunkProviderServer");
    private static final Class<?> I_CHUNK_PROVIDER = Reflect.nms("IChunkProvider");
    private static final Class<?> CRAFT_SERVER = Reflect.craft("CraftServer");
    private static final Class<?> CRAFT_WORLD = Reflect.craft("CraftWorld");
    private static final Class<?> LONG_HASH = Reflect.craft("util.LongHash");

    private static final Constructor<?> NEW_WORLD_SETTINGS =
        Reflect.constructor(WORLD_SETTINGS, long.class, ENUM_GAMEMODE, boolean.class, boolean.class, WORLD_TYPE);
    private static final Constructor<?> NEW_WORLD_DATA = Reflect.constructor(WORLD_DATA, WORLD_SETTINGS, String.class);
    private static final Constructor<?> NEW_METHOD_PROFILER = Reflect.constructor(METHOD_PROFILER);
    private static final Constructor<?> NEW_WORLD_SERVER = Reflect.constructor(WORLD_SERVER,
        MINECRAFT_SERVER, I_DATA_MANAGER, WORLD_DATA, int.class, METHOD_PROFILER,
        World.Environment.class, org.bukkit.generator.ChunkGenerator.class);
    private static final Constructor<?> NEW_WORLD_MANAGER = Reflect.constructor(WORLD_MANAGER, MINECRAFT_SERVER, WORLD_SERVER);
    private static final Constructor<?> NEW_CHUNK = Reflect.constructor(NMS_CHUNK, NMS_WORLD, int.class, int.class);

    private static final Method GET_SERVER = Reflect.method(MINECRAFT_SERVER, "getServer");
    private static final Method ADD_WORLD_ACCESS = Reflect.method(WORLD_SERVER, "addIWorldAccess", I_WORLD_ACCESS);
    private static final Method WS_INIT = Reflect.method(WORLD_SERVER, "b");
    private static final Method WS_GET_WORLD = Reflect.method(WORLD_SERVER, "getWorld");
    private static final Method SET_NEIGHBOR = Reflect.method(NMS_CHUNK, "setNeighborLoaded", int.class, int.class);
    private static final Method CPS_GET_IF_LOADED = Reflect.method(CHUNK_PROVIDER_SERVER, "getChunkIfLoaded", int.class, int.class);
    private static final Method CPS_SAVE_CHUNK = Reflect.method(CHUNK_PROVIDER_SERVER, "saveChunk", NMS_CHUNK);
    private static final Method CHUNK_REMOVE_ENTITIES = Reflect.method(NMS_CHUNK, "removeEntities");
    private static final Method REFRESH_CHUNK = Reflect.method(CRAFT_WORLD, "refreshChunk", int.class, int.class);
    private static final Method LONG_HASH_TO_LONG = Reflect.method(LONG_HASH, "toLong", int.class, int.class);

    private static final Field F_CHUNK_PROVIDER_SERVER = Reflect.field(WORLD_SERVER, "chunkProviderServer");
    private static final Field F_CPS_CHUNKS = Reflect.field(CHUNK_PROVIDER_SERVER, "chunks");
    private static final Field F_CPS_CHUNK_PROVIDER = Reflect.field(CHUNK_PROVIDER_SERVER, "chunkProvider");
    private static final Field F_MS_WORLDS = Reflect.field(MINECRAFT_SERVER, "worlds");
    private static final Field F_CRAFT_WORLDS = Reflect.field(CRAFT_SERVER, "worlds");

    private static final Object WORLD_TYPE_FLAT = Reflect.staticField(WORLD_TYPE, "FLAT");

    private WorldInjector() {
    }

    public static @NotNull Object newWorldServer(@NotNull String name, @NotNull UUID id, @NotNull World.Environment env,
                                                 @NotNull GameMode gameMode, @Nullable ChunkGenerator generator,
                                                 @NotNull ChunkStorage storage, boolean skyLight) {
        Object server = Reflect.invoke(GET_SERVER, null);
        Object settings = Reflect.construct(NEW_WORLD_SETTINGS, 0L, enumGamemode(gameMode), false, false, WORLD_TYPE_FLAT);
        Object data = Reflect.construct(NEW_WORLD_DATA, settings, name);
        Object profiler = Reflect.construct(NEW_METHOD_PROFILER);
        Object dataManager = dataManagerProxy(data, chunkLoaderProxy(storage, skyLight), id);
        int dim = DIMENSION.getAndIncrement();

        Object ws = Reflect.construct(NEW_WORLD_SERVER, server, dataManager, data, dim, profiler, env, new VoidChunkGenerator());
        Reflect.invoke(ADD_WORLD_ACCESS, ws, Reflect.construct(NEW_WORLD_MANAGER, server, ws));
        Object initialized = Reflect.invoke(WS_INIT, ws);
        Object world = initialized != null ? initialized : ws;

        Reflect.set(F_CPS_CHUNK_PROVIDER, chunkProvider(world), voidChunkProviderProxy(world, generator, skyLight));
        return world;
    }

    public static @NotNull World bukkitWorld(@NotNull Object worldServer) {
        return (World) Reflect.invoke(WS_GET_WORLD, worldServer);
    }

    public static @NotNull Object chunkProvider(@NotNull Object worldServer) {
        return Reflect.get(F_CHUNK_PROVIDER_SERVER, worldServer);
    }

    public static @Nullable Object chunkIfLoaded(@NotNull Object chunkProvider, int x, int z) {
        return Reflect.invoke(CPS_GET_IF_LOADED, chunkProvider, x, z);
    }

    public static @NotNull Object loadChunk(@NotNull Object worldServer, @NotNull Object chunkProvider,
                                            int x, int z, @Nullable ChunkGenerator generator, boolean skyLight) {
        Object chunk = createMarkedChunk(worldServer, x, z, generator, skyLight);
        Object chunks = Reflect.get(F_CPS_CHUNKS, chunkProvider);
        mapPut(chunks, key(x, z), chunk);

        for (int dx = -2; dx < 3; dx++) {
            for (int dz = -2; dz < 3; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                Object neighbor = chunkIfLoaded(chunkProvider, x + dx, z + dz);
                if (neighbor != null) {
                    Reflect.invoke(SET_NEIGHBOR, neighbor, -dx, -dz);
                    Reflect.invoke(SET_NEIGHBOR, chunk, dx, dz);
                }
            }
        }
        return chunk;
    }

    public static boolean unloadChunk(@NotNull Object chunkProvider, int x, int z) {
        Object chunks = Reflect.get(F_CPS_CHUNKS, chunkProvider);
        long key = key(x, z);
        Object chunk = mapGet(chunks, key);
        if (chunk == null) {
            return false;
        }
        Reflect.invoke(CPS_SAVE_CHUNK, chunkProvider, chunk);
        Reflect.invoke(CHUNK_REMOVE_ENTITIES, chunk);
        mapRemove(chunks, key);
        return true;
    }

    @SuppressWarnings("unchecked")
    public static void addToServerWorlds(@NotNull Object worldServer) {
        ((java.util.List<Object>) Reflect.get(F_MS_WORLDS, Reflect.invoke(GET_SERVER, null))).add(worldServer);
    }

    @SuppressWarnings("unchecked")
    public static void removeFromServerWorlds(@NotNull Object worldServer) {
        ((java.util.List<Object>) Reflect.get(F_MS_WORLDS, Reflect.invoke(GET_SERVER, null))).remove(worldServer);
    }

    @SuppressWarnings("unchecked")
    public static void registerCraftWorld(@NotNull World world) {
        ((Map<String, World>) Reflect.get(F_CRAFT_WORLDS, Bukkit.getServer())).put(world.getName().toLowerCase(), world);
    }

    @SuppressWarnings("unchecked")
    public static void unregisterCraftWorld(@NotNull World world) {
        ((Map<String, World>) Reflect.get(F_CRAFT_WORLDS, Bukkit.getServer())).remove(world.getName().toLowerCase());
    }

    public static void refreshChunk(@NotNull World world, int x, int z) {
        Reflect.invoke(REFRESH_CHUNK, world, x, z);
    }

    private static void markChunkNonEmpty(Object chunk) {
        Object[] sections = NmsChunk.sections(chunk);
        for (Object s : sections) {
            if (s != null && NmsChunk.nonEmptyCount(s) != 0) {
                return;
            }
        }
        Object section = sections[0];
        if (section == null) {
            section = NmsChunk.newSection(0, true);
            sections[0] = section;
        }
        NmsChunk.setNonEmptyCount(section, 1);
    }

    private static Object createMarkedChunk(Object worldServer, int x, int z, ChunkGenerator generator, boolean skyLight) {
        Object chunk = Reflect.construct(NEW_CHUNK, worldServer, x, z);
        NmsChunk.initLighting(chunk);
        if (generator != null) {
            generator.generate(x, z, new ChunkViewImpl(chunk, x, z, skyLight));
        }
        markChunkNonEmpty(chunk);
        return chunk;
    }

    private static Object voidChunkProviderProxy(Object worldServer, ChunkGenerator generator, boolean skyLight) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return objectMethod(proxy, method, args);
            }
            return switch (method.getName()) {
                // Chunk getOrCreateChunk(int, int)
                case "getOrCreateChunk" -> createMarkedChunk(worldServer, ((Number) args[0]).intValue(),
                    ((Number) args[1]).intValue(), generator, skyLight);
                // Chunk getChunkAt(BlockPosition) AND void getChunkAt(IChunkProvider, int, int)
                case "getChunkAt" -> null;
                // boolean a(IChunkProvider, Chunk, int, int)
                case "a" -> false;
                // boolean isChunkLoaded(int, int)
                case "isChunkLoaded" -> false;
                // boolean saveChunks(boolean, IProgressUpdate)
                case "saveChunks" -> true;
                // boolean unloadChunks()
                case "unloadChunks" -> false;
                // boolean canSave()
                case "canSave" -> true;
                // String getName()
                case "getName" -> "VoidChunkProvider";
                // List<BiomeBase.BiomeMeta> getMobsFor(EnumCreatureType, BlockPosition)
                case "getMobsFor" -> new ArrayList<>();
                // BlockPosition findNearestMapFeature(World, String, BlockPosition)
                case "findNearestMapFeature" -> null;
                // int getLoadedChunks()
                case "getLoadedChunks" -> 0;
                // void recreateStructures(Chunk, int, int)
                case "recreateStructures" -> null;
                // void c()
                case "c" -> null;
                default -> throw new UnsupportedOperationException("Unhandled IChunkProvider method: " + method);
            };
        };
        return Proxy.newProxyInstance(I_CHUNK_PROVIDER.getClassLoader(), new Class[]{I_CHUNK_PROVIDER}, handler);
    }

    private static Object dataManagerProxy(Object worldData, Object chunkLoader, UUID id) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return objectMethod(proxy, method, args);
            }

            return switch (method.getName()) {
                // WorldData getWorldData()
                case "getWorldData" -> worldData;
                // IChunkLoader createChunkLoader(WorldProvider)
                case "createChunkLoader" -> chunkLoader;
                // UUID getUUID()
                case "getUUID" -> id;
                // void checkSession()
                case "checkSession" -> null;
                // void saveWorldData(WorldData) AND void saveWorldData(WorldData, NBTTagCompound)
                case "saveWorldData" -> null;
                // void a()
                case "a" -> null;
                // IPlayerFileData getPlayerFileData()
                case "getPlayerFileData" -> null;
                // File getDirectory()
                case "getDirectory" -> null;
                // File getDataFile(String)
                case "getDataFile" -> null;
                // String g()
                case "g" -> null;
                default -> throw new UnsupportedOperationException("Unhandled IDataManager method: " + method);
            };
        };
        return Proxy.newProxyInstance(I_DATA_MANAGER.getClassLoader(), new Class[]{I_DATA_MANAGER}, handler);
    }

    private static Object chunkLoaderProxy(ChunkStorage storage, boolean skyLight) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return objectMethod(proxy, method, args);
            }

            String name = method.getName();
            int n = method.getParameterCount();
            if (name.equals("a") && n == 3) {
                Object world = args[0];
                int x = ((Number) args[1]).intValue();
                int z = ((Number) args[2]).intValue();
                Object chunk = Reflect.construct(NEW_CHUNK, world, x, z);
                if (!storage.load(x, z, new ChunkViewImpl(chunk, x, z, skyLight))) {
                    return null;
                }
                NmsChunk.initLighting(chunk);
                markChunkNonEmpty(chunk);
                return chunk;
            }
            if (name.equals("a") && n == 2) {
                Object chunk = args[1];
                storage.save(NmsChunk.locX(chunk), NmsChunk.locZ(chunk),
                    new ChunkViewImpl(chunk, NmsChunk.locX(chunk), NmsChunk.locZ(chunk), skyLight));
                return null;
            }
            // void b(World, Chunk), void a(), void b()
            if ((name.equals("b") && n == 2) || ((name.equals("a") || name.equals("b")) && n == 0)) {
                return null;
            }
            throw new UnsupportedOperationException("Unhandled IChunkLoader method: " + method);
        };
        return Proxy.newProxyInstance(I_CHUNK_LOADER.getClassLoader(), new Class[]{I_CHUNK_LOADER}, handler);
    }

    private static Object objectMethod(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "NmsProxy@" + Integer.toHexString(System.identityHashCode(proxy));
            default -> null;
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumGamemode(GameMode gameMode) {
        return Enum.valueOf((Class<Enum>) ENUM_GAMEMODE.asSubclass(Enum.class), gameMode.name());
    }

    private static long key(int x, int z) {
        return (Long) Reflect.invoke(LONG_HASH_TO_LONG, null, x, z);
    }

    private static void mapPut(Object map, long key, Object value) {
        try {
            map.getClass().getMethod("put", long.class, Object.class).invoke(map, key, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("chunks.put failed", e);
        }
    }

    private static Object mapGet(Object map, long key) {
        try {
            return map.getClass().getMethod("get", long.class).invoke(map, key);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("chunks.get failed", e);
        }
    }

    private static void mapRemove(Object map, long key) {
        try {
            map.getClass().getMethod("remove", long.class).invoke(map, key);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("chunks.remove failed", e);
        }
    }

}
