package dev.tommyjs.dynworld.entity;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.protocol.world.Direction;
import com.github.retrooper.packetevents.protocol.world.PaintingType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

class EntityTrackerImpl implements EntityTracker {

    private final EntityIdAllocator allocator;
    private final Map<Integer, AbstractVirtualEntity> entities;
    private final Map<UUID, Player> viewers;
    private final Set<Integer> fixedIds;

    EntityTrackerImpl(@NotNull EntityIdAllocator allocator) {
        this.allocator = allocator;
        this.entities = new ConcurrentHashMap<>();
        this.viewers = new ConcurrentHashMap<>();
        this.fixedIds = ConcurrentHashMap.newKeySet();
    }

    @Override
    public void addViewer(@NotNull Player player) {
        viewers.put(player.getUniqueId(), player);
    }

    @Override
    public void removeViewer(@NotNull Player player) {
        viewers.remove(player.getUniqueId());
        for (AbstractVirtualEntity entity : entities.values()) {
            entity.viewerRemoved(player);
        }
    }

    @Override
    public void transferViewer(@NotNull Player player, @NotNull EntityTracker to) {
        EntityTrackerImpl target = (EntityTrackerImpl) to;
        if (target == this) {
            return;
        }
        if (viewers.remove(player.getUniqueId()) == null) {
            target.addViewer(player);
            return;
        }

        target.addViewer(player);
        for (AbstractVirtualEntity entity : entities.values()) {
            AbstractVirtualEntity successor = target.entities.get(entity.entityId());
            if (successor == null || !successor.adopt(player, entity)) {
                entity.viewerRemoved(player);
            }
        }
    }

    @Override
    public @NotNull Collection<UUID> viewers() {
        return new ArrayList<>(viewers.keySet());
    }

    @Nullable Player getViewer(@NotNull UUID uuid) {
        return viewers.get(uuid);
    }

    @Override
    public @NotNull MobEntity spawnMob(@NotNull EntityType type, @NotNull EntityPose at) {
        int id = allocator.acquire();
        MobEntityImpl entity = new MobEntityImpl(this, id, type, at);
        entities.put(id, entity);
        return entity;
    }

    @Override
    public @NotNull MobEntity spawnMob(int entityId, @NotNull EntityType type, @NotNull EntityPose at) {
        claim(entityId);
        MobEntityImpl entity = new MobEntityImpl(this, entityId, type, at);
        entities.put(entityId, entity);
        return entity;
    }

    @Override
    public @NotNull ObjectEntity spawnObject(@NotNull EntityType type, @NotNull EntityPose at, int objectData) {
        int id = allocator.acquire();
        ObjectEntityImpl entity = new ObjectEntityImpl(this, id, type, at, objectData);
        entities.put(id, entity);
        return entity;
    }

    @Override
    public @NotNull ObjectEntity spawnObject(int entityId, @NotNull EntityType type, @NotNull EntityPose at, int objectData) {
        claim(entityId);
        ObjectEntityImpl entity = new ObjectEntityImpl(this, entityId, type, at, objectData);
        entities.put(entityId, entity);
        return entity;
    }

    @Override
    public @NotNull PlayerEntity spawnPlayer(@NotNull UserProfile profile, @NotNull EntityPose at) {
        int id = allocator.acquire();
        PlayerEntityImpl entity = new PlayerEntityImpl(this, id, profile, at);
        entities.put(id, entity);
        return entity;
    }

    @Override
    public @NotNull PlayerEntity spawnPlayer(int entityId, @NotNull UserProfile profile, @NotNull EntityPose at) {
        claim(entityId);
        PlayerEntityImpl entity = new PlayerEntityImpl(this, entityId, profile, at);
        entities.put(entityId, entity);
        return entity;
    }

    @Override
    public @NotNull PaintingEntity spawnPainting(@NotNull PaintingType art, @NotNull EntityPose at, @NotNull Direction direction) {
        int id = allocator.acquire();
        PaintingEntityImpl entity = new PaintingEntityImpl(this, id, art, at, direction);
        entities.put(id, entity);
        return entity;
    }

    @Override
    public @NotNull ExperienceOrbEntity spawnExperienceOrb(@NotNull EntityPose at, int amount) {
        int id = allocator.acquire();
        ExperienceOrbEntityImpl entity = new ExperienceOrbEntityImpl(this, id, at, amount);
        entities.put(id, entity);
        return entity;
    }

    @Override
    public void remove(@NotNull VirtualEntity entity) {
        AbstractVirtualEntity impl = (AbstractVirtualEntity) entity;
        impl.removeAll();
        if (!entities.remove(entity.entityId(), impl)) {
            return;
        }
        if (!fixedIds.remove(entity.entityId())) {
            allocator.release(entity.entityId());
        }
    }

    private void claim(int entityId) {
        if (entities.containsKey(entityId) || !fixedIds.add(entityId)) {
            throw new IllegalStateException("entity id " + entityId + " is already in use");
        }
    }

    @Override
    public void freeze() {
        for (AbstractVirtualEntity entity : entities.values()) {
            entity.freeze();
        }
    }

    @Override
    public void unfreeze() {
        for (AbstractVirtualEntity entity : entities.values()) {
            entity.unfreeze();
        }
    }

    @Override
    public void tick() {
        List<UUID> offline = new ArrayList<>();
        for (Map.Entry<UUID, Player> entry : viewers.entrySet()) {
            if (!entry.getValue().isOnline()) {
                offline.add(entry.getKey());
            }
        }
        for (UUID uuid : offline) {
            Player player = viewers.remove(uuid);
            if (player != null) {
                for (AbstractVirtualEntity entity : entities.values()) {
                    entity.viewerRemoved(player);
                }
            }
        }

        List<Player> snapshot = new ArrayList<>(viewers.values());
        for (AbstractVirtualEntity entity : entities.values()) {
            entity.tick(snapshot);
        }
    }

}
