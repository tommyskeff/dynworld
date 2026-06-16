package dev.tommyjs.dynworld.entity;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

class MobEntityImpl extends AbstractVirtualEntity implements MobEntity {

    private final EntityType type;

    MobEntityImpl(@NotNull EntityTrackerImpl tracker, int entityId, @NotNull EntityType type, @NotNull EntityPose pose) {
        super(tracker, entityId, pose);
        this.type = type;
    }

    @Override
    protected void sendSpawn(@NotNull Player viewer) {
        send(viewer, new WrapperPlayServerSpawnLivingEntity(entityId, UUID.randomUUID(), type, location(), pose().headYaw(), new Vector3d(), List.of()));
    }

    @Override
    protected void sendDestroy(@NotNull Player viewer) {
        send(viewer, new WrapperPlayServerDestroyEntities(entityId));
    }

    @Override
    protected boolean pinWhileFrozen() {
        return false;
    }

    @Override
    protected boolean tracksHealth() {
        return true;
    }

}
