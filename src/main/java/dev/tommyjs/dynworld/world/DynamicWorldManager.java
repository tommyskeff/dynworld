package dev.tommyjs.dynworld.world;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface DynamicWorldManager {

    DynamicWorldManager DEFAULT = new DynamicWorldManagerImpl();

    @NotNull DynamicWorldBuilder builder();

    @NotNull Collection<DynamicWorld> getWorlds();

    @NotNull DynamicWorld create(@NotNull DynamicWorldBuilder builder);

    void unload(@NotNull DynamicWorld world);

}
