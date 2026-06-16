package dev.tommyjs.dynworld.entity;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.protocol.world.Direction;
import com.github.retrooper.packetevents.protocol.world.PaintingType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;

public interface EntityTracker {

    int DEFAULT_MIN_ID = Integer.MAX_VALUE / 2;

    static @NotNull EntityTracker create() {
        return create(EntityIdAllocator.range(DEFAULT_MIN_ID, Integer.MAX_VALUE));
    }

    static @NotNull EntityTracker create(@NotNull EntityIdAllocator allocator) {
        return new EntityTrackerImpl(allocator);
    }

    void addViewer(@NotNull Player player);

    void removeViewer(@NotNull Player player);

    @NotNull Collection<UUID> viewers();

    @NotNull MobEntity spawnMob(@NotNull EntityType type, @NotNull EntityPose at);

    @NotNull ObjectEntity spawnObject(@NotNull EntityType type, @NotNull EntityPose at, int objectData);

    @NotNull PlayerEntity spawnPlayer(@NotNull UserProfile profile, @NotNull EntityPose at);

    @NotNull PaintingEntity spawnPainting(@NotNull PaintingType art, @NotNull EntityPose at, @NotNull Direction direction);

    @NotNull ExperienceOrbEntity spawnExperienceOrb(@NotNull EntityPose at, int amount);

    void remove(@NotNull VirtualEntity entity);

    void freeze();

    void unfreeze();

    void tick();

}
