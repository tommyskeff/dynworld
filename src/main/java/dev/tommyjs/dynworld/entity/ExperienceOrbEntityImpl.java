package dev.tommyjs.dynworld.entity;

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnExperienceOrb;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

class ExperienceOrbEntityImpl extends AbstractVirtualEntity implements ExperienceOrbEntity {

    private final int amount;

    ExperienceOrbEntityImpl(@NotNull EntityTrackerImpl tracker, int entityId, @NotNull EntityPose pose, int amount) {
        super(tracker, entityId, pose);
        this.amount = amount;
    }

    @Override
    public int amount() {
        return amount;
    }

    @Override
    protected void sendSpawn(@NotNull Player viewer) {
        send(viewer, new WrapperPlayServerSpawnExperienceOrb(entityId, pose().x(), pose().y(), pose().z(), (short) amount));
    }

    @Override
    protected void sendDestroy(@NotNull Player viewer) {
        send(viewer, new WrapperPlayServerDestroyEntities(entityId));
    }

    @Override
    protected boolean pinWhileFrozen() {
        return true;
    }

}
