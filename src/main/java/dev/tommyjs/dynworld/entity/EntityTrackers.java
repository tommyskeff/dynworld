package dev.tommyjs.dynworld.entity;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public final class EntityTrackers {

    private static volatile ScheduledExecutorService defaultExecutor;

    private EntityTrackers() {
    }

    private static ScheduledExecutorService defaultExecutor() {
        if (defaultExecutor == null) {
            synchronized (EntityTrackers.class) {
                if (defaultExecutor == null) {
                    ThreadFactory factory = r -> {
                        Thread t = new Thread(r);
                        t.setDaemon(true);
                        return t;
                    };
                    defaultExecutor = Executors.newSingleThreadScheduledExecutor(factory);
                }
            }
        }
        return defaultExecutor;
    }

    public static @NotNull Closeable schedule(@NotNull EntityTracker tracker, @NotNull Duration interval) {
        return schedule(tracker, interval, defaultExecutor());
    }

    public static @NotNull Closeable schedule(@NotNull EntityTracker tracker, @NotNull Duration interval, @NotNull ScheduledExecutorService executor) {
        long nanos = interval.toNanos();
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(tracker::tick, nanos, nanos, TimeUnit.NANOSECONDS);
        return () -> future.cancel(false);
    }

}
