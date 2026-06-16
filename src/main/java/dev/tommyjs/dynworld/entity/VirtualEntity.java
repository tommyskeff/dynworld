package dev.tommyjs.dynworld.entity;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation.EntityAnimationType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface VirtualEntity {

    int entityId();

    void move(@NotNull EntityPose to, boolean smooth);

    void lookHead(float headYaw);

    void setVelocity(@NotNull Vector3d velocity);

    void setMetadata(@NotNull List<EntityData<?>> metadata);

    void setEquipment(@NotNull List<Equipment> equipment);

    void animate(@NotNull EntityAnimationType type);

    void sendStatus(byte status);

    void setRange(double range);

    void show(@NotNull Player viewer);

    void hide(@NotNull Player viewer);

    void setPresent(boolean present);

    void freeze();

    void unfreeze();

    boolean frozen();

    void mount(@NotNull VirtualEntity vehicle);

    void dismount();

}
