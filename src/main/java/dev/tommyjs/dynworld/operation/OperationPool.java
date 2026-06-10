package dev.tommyjs.dynworld.operation;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface OperationPool {

    static @NotNull OperationPool create(@NotNull Plugin plugin, long maxMillisPerTick, int maxConcurrent) {
        return new OperationPoolImpl(plugin, maxMillisPerTick, maxConcurrent);
    }

    @NotNull CompletableFuture<Void> submit(@NotNull Operation operation);

    void start();

    void stop();

    void setMaxMillisPerTick(long millis);

    void setMaxConcurrent(int maxConcurrent);

    int queued();

    int running();

}
