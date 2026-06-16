package dev.tommyjs.dynworld.entity;

import com.github.retrooper.packetevents.protocol.world.Direction;
import com.github.retrooper.packetevents.protocol.world.PaintingType;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPainting;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

class PaintingEntityImpl extends AbstractVirtualEntity implements PaintingEntity {

    private final PaintingType art;
    private final Direction direction;

    PaintingEntityImpl(@NotNull EntityTrackerImpl tracker, int entityId, @NotNull PaintingType art, @NotNull EntityPose pose, @NotNull Direction direction) {
        super(tracker, entityId, pose);
        this.art = art;
        this.direction = direction;
    }

    @Override
    protected void sendSpawn(@NotNull Player viewer) {
        send(viewer, new WrapperPlayServerSpawnPainting(entityId, UUID.randomUUID(), art,
            new Vector3i((int) Math.floor(pose().x()), (int) Math.floor(pose().y()), (int) Math.floor(pose().z())), direction));
    }

    @Override
    protected void sendDestroy(@NotNull Player viewer) {
        send(viewer, new WrapperPlayServerDestroyEntities(entityId));
    }

    @Override
    protected boolean pinWhileFrozen() {
        return false;
    }

}
