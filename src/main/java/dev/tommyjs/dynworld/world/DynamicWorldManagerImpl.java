package dev.tommyjs.dynworld.world;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public final class DynamicWorldManagerImpl implements DynamicWorldManager {

    private final Map<String, DynamicWorld> worlds = new LinkedHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public @NotNull DynamicWorldBuilder builder() {
        return new DynamicWorldBuilder().setWorldManager(this);
    }

    @Override
    public @NotNull Collection<DynamicWorld> getWorlds() {
        lock.lock();
        try {
            return Collections.unmodifiableCollection(new ArrayList<>(worlds.values()));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public @NotNull DynamicWorld create(@NotNull DynamicWorldBuilder builder) {
        UUID id = UUID.randomUUID();
        String name = builder.getName() != null ? builder.getName() : "dyn-" + id.toString().substring(0, 8);
        boolean skyLight = builder.getEnvironment() == World.Environment.NORMAL;

        DynamicWorldImpl world;
        lock.lock();
        try {
            if (Bukkit.getWorld(name) != null || worlds.containsKey(name)) {
                throw new IllegalStateException("A world named '" + name + "' already exists");
            }

            Object worldServer = WorldInjector.newWorldServer(name, id, builder.getEnvironment(), builder.getGameMode(),
                builder.getChunkGenerator(), builder.getChunkStorage(), skyLight);
            World bukkit = WorldInjector.bukkitWorld(worldServer);

            if (!builder.isAutoUnload()) {
                bukkit.setAutoSave(false);
            }

            for (Map.Entry<String, String> rule : builder.getGameRules().entrySet()) {
                bukkit.setGameRuleValue(rule.getKey(), rule.getValue());
            }

            Object chunkProvider = WorldInjector.chunkProvider(worldServer);
            world = new DynamicWorldImpl(name, id, worldServer, bukkit, chunkProvider, builder.getChunkGenerator(),
                builder.getChunkStorage(), skyLight);

            WorldInjector.addToServerWorlds(worldServer);
            WorldInjector.registerCraftWorld(bukkit);

            world.setActive(true);
            worlds.put(name, world);
        } finally {
            lock.unlock();
        }

        try {
            Bukkit.getPluginManager().callEvent(new WorldLoadEvent(world.getBukkitWorld()));
        } catch (Throwable ignored) {
        }

        return world;
    }

    @Override
    public void unload(@NotNull DynamicWorld world) {
        if (!(world instanceof DynamicWorldImpl)) {
            throw new IllegalArgumentException(
                "Cannot unload world '" + world.getName() + "': incompatible DynamicWorld implementation");
        }

        if (!world.getBukkitWorld().getPlayers().isEmpty()) {
            throw new IllegalStateException(
                "World '" + world.getName() + "' still has players; move them out before unloading");
        }

        try {
            Bukkit.getPluginManager().callEvent(new WorldUnloadEvent(world.getBukkitWorld()));
        } catch (Throwable ignored) {
        }

        DynamicWorldImpl impl = (DynamicWorldImpl) world;
        lock.lock();
        try {
            WorldInjector.unregisterCraftWorld(world.getBukkitWorld());
            WorldInjector.removeFromServerWorlds(impl.worldServer());
            impl.setActive(false);
            worlds.remove(world.getName().toLowerCase());
        } finally {
            lock.unlock();
        }

        try {
            impl.storage().close();
        } catch (Throwable t) {
            Bukkit.getLogger().warning("ChunkStorage.close failed for '" + world.getName() + "': " + t);
        }
    }

}
