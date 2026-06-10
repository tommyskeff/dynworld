package dev.tommyjs.dynworld.world;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DynamicWorldBuilder {

    private DynamicWorldManager worldManager = DynamicWorldManager.DEFAULT;
    private String name;
    private World.Environment environment = World.Environment.NORMAL;
    private GameMode gameMode = GameMode.ADVENTURE;
    private final Map<String, String> gamerules = new LinkedHashMap<>();
    private ChunkGenerator chunkGenerator = ChunkGenerator.VOID;
    private ChunkStorage chunkStorage = ChunkStorage.EMPTY;
    private boolean autoUnload = false;

    DynamicWorldBuilder() {
    }

    public @NotNull DynamicWorldBuilder setWorldManager(@NotNull DynamicWorldManager worldManager) {
        this.worldManager = worldManager;
        return this;
    }

    public @NotNull DynamicWorldManager getWorldManager() {
        return worldManager;
    }

    public @NotNull DynamicWorldBuilder setName(@NotNull String name) {
        this.name = name;
        return this;
    }

    public @Nullable String getName() {
        return name;
    }

    public @NotNull DynamicWorldBuilder setEnvironment(@NotNull World.Environment environment) {
        this.environment = environment;
        return this;
    }

    public @NotNull World.Environment getEnvironment() {
        return environment;
    }

    public @NotNull DynamicWorldBuilder setGameMode(@NotNull GameMode gameMode) {
        this.gameMode = gameMode;
        return this;
    }

    public @NotNull GameMode getGameMode() {
        return gameMode;
    }

    public @NotNull DynamicWorldBuilder setGameRule(@NotNull String rule, @NotNull String value) {
        this.gamerules.put(rule, value);
        return this;
    }

    public @NotNull Map<String, String> getGameRules() {
        return Map.copyOf(gamerules);
    }

    public @NotNull DynamicWorldBuilder setChunkGenerator(@NotNull ChunkGenerator generator) {
        this.chunkGenerator = generator;
        return this;
    }

    public @NotNull ChunkGenerator getChunkGenerator() {
        return chunkGenerator;
    }

    public @NotNull DynamicWorldBuilder setChunkStorage(@NotNull ChunkStorage storage) {
        this.chunkStorage = storage;
        return this;
    }

    public @NotNull ChunkStorage getChunkStorage() {
        return chunkStorage;
    }

    public @NotNull DynamicWorldBuilder setAutoUnload(boolean autoUnload) {
        this.autoUnload = autoUnload;
        return this;
    }

    public boolean isAutoUnload() {
        return autoUnload;
    }

    public @NotNull DynamicWorld create() {
        return worldManager.create(this);
    }

}
