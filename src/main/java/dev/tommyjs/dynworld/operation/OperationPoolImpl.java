package dev.tommyjs.dynworld.operation;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class OperationPoolImpl implements OperationPool {

    private final Plugin plugin;
    private final Deque<Entry> queue = new ArrayDeque<>();
    private final List<Entry> active = new ArrayList<>();

    private long maxNanosPerTick;
    private int maxConcurrent;
    private BukkitTask task;

    public OperationPoolImpl(@NotNull Plugin plugin, long maxMillisPerTick, int maxConcurrent) {
        this.plugin = plugin;
        this.maxNanosPerTick = maxMillisPerTick * 1_000_000L;
        this.maxConcurrent = Math.max(1, maxConcurrent);
    }

    @Override
    public @NotNull CompletableFuture<Void> submit(@NotNull Operation operation) {
        Entry entry = new Entry(operation);
        queue.add(entry);
        return entry.future;
    }

    @Override
    public void start() {
        if (task == null) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        }
    }

    @Override
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public void setMaxMillisPerTick(long millis) {
        this.maxNanosPerTick = millis * 1_000_000L;
    }

    @Override
    public void setMaxConcurrent(int maxConcurrent) {
        this.maxConcurrent = Math.max(1, maxConcurrent);
    }

    @Override
    public int queued() {
        return queue.size();
    }

    @Override
    public int running() {
        return active.size();
    }

    private void tick() {
        while (active.size() < maxConcurrent && !queue.isEmpty()) {
            active.add(queue.poll());
        }
        if (active.isEmpty()) {
            return;
        }

        long deadline = System.nanoTime() + maxNanosPerTick;
        int i = 0;
        while (i < active.size()) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                break;
            }
            Entry entry = active.get(i);
            long slice = remaining / (active.size() - i) / 1_000_000L;
            boolean done;
            try {
                done = entry.operation.execute(Math.max(1, slice));
            } catch (Throwable t) {
                entry.future.completeExceptionally(t);
                active.remove(i);
                continue;
            }
            if (done) {
                entry.future.complete(null);
                active.remove(i);
            } else {
                i++;
            }
        }
    }

    private static final class Entry {
        final Operation operation;
        final CompletableFuture<Void> future = new CompletableFuture<>();

        Entry(Operation operation) {
            this.operation = operation;
        }
    }

}
