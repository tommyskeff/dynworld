package dev.tommyjs.dynworld.entity;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

class ObjectEntityImpl extends AbstractVirtualEntity implements ObjectEntity {

    private final EntityType type;
    private final int objectData;

    ObjectEntityImpl(@NotNull EntityTrackerImpl tracker, int entityId, @NotNull EntityType type, @NotNull EntityPose pose, int objectData) {
        super(tracker, entityId, pose);
        this.type = type;
        this.objectData = objectData;
    }

    @Override
    protected void sendSpawn(@NotNull Player viewer) {
        Optional<Vector3d> velocity = objectData != 0 ? Optional.of(new Vector3d()) : Optional.empty();
        send(viewer, new WrapperPlayServerSpawnEntity(entityId, Optional.of(UUID.randomUUID()), type,
            new Vector3d(pose().x(), pose().y(), pose().z()), pose().pitch(), pose().yaw(), pose().headYaw(), objectData, velocity));
    }

    @Override
    protected void sendDestroy(@NotNull Player viewer) {
        send(viewer, new WrapperPlayServerDestroyEntities(entityId));
    }

    @Override
    protected boolean pinWhileFrozen() {
        return isClientSimulated(type);
    }

}
