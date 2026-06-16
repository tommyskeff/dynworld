package dev.tommyjs.dynworld.entity;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAttachEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation.EntityAnimationType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMoveAndRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

abstract class AbstractVirtualEntity implements VirtualEntity {

    private static final double FIXED = 32.0;
    private static final long RELATIVE_THRESHOLD = 128;
    private static final int TELEPORT_RESYNC = 400;
    private static final int HEALTH_METADATA_INDEX = 6;
    private static final double DEFAULT_RANGE = 48.0;

    private static final Set<EntityType> CLIENT_SIMULATED = Set.of(
        EntityTypes.ARROW,
        EntityTypes.ITEM,
        EntityTypes.FALLING_BLOCK,
        EntityTypes.TNT,
        EntityTypes.FIREBALL,
        EntityTypes.SMALL_FIREBALL,
        EntityTypes.WITHER_SKULL,
        EntityTypes.DRAGON_FIREBALL,
        EntityTypes.SNOWBALL,
        EntityTypes.EGG,
        EntityTypes.ENDER_PEARL,
        EntityTypes.POTION,
        EntityTypes.EXPERIENCE_BOTTLE,
        EntityTypes.FISHING_BOBBER,
        EntityTypes.BOAT,
        EntityTypes.FIREWORK_ROCKET,
        EntityTypes.EXPERIENCE_ORB,
        EntityTypes.MINECART,
        EntityTypes.CHEST_MINECART,
        EntityTypes.FURNACE_MINECART,
        EntityTypes.TNT_MINECART,
        EntityTypes.HOPPER_MINECART,
        EntityTypes.SPAWNER_MINECART,
        EntityTypes.COMMAND_BLOCK_MINECART
    );

    static boolean isClientSimulated(@NotNull EntityType type) {
        return CLIENT_SIMULATED.contains(type);
    }

    protected final EntityTrackerImpl tracker;
    protected final int entityId;

    private final ReentrantLock lock = new ReentrantLock();

    private EntityPose pose;
    private boolean smooth;
    private Vector3d velocity;
    private List<EntityData<?>> metadata;
    private List<Equipment> equipment;
    private @Nullable Integer vehicleId;
    private boolean present;
    private double range;
    private final Set<UUID> hiddenFor;
    private boolean frozen;
    private final Set<UUID> shown;

    private boolean positionDirty;
    private boolean metadataDirty;
    private boolean equipmentDirty;
    private boolean velocityDirty;
    private boolean attachDirty;

    private boolean established;
    private long encX, encY, encZ;
    private int encYaw, encPitch, encHead;
    private int sinceTeleport;
    private long moveTicks;

    private @Nullable Float lastHealth;
    private boolean respawnDirty;
    private boolean wasFrozen;

    AbstractVirtualEntity(@NotNull EntityTrackerImpl tracker, int entityId, @NotNull EntityPose pose) {
        this.tracker = tracker;
        this.entityId = entityId;
        this.pose = pose;
        this.velocity = new Vector3d();
        this.metadata = List.of();
        this.equipment = List.of();
        this.present = true;
        this.range = DEFAULT_RANGE;
        this.hiddenFor = new HashSet<>();
        this.shown = new HashSet<>();
    }

    protected abstract void sendSpawn(@NotNull Player viewer);

    protected abstract void sendDestroy(@NotNull Player viewer);

    protected abstract boolean pinWhileFrozen();

    protected boolean tracksHealth() {
        return false;
    }

    protected void onShow(@NotNull Player viewer) {}

    protected void onTick() {}

    protected void onHide(@NotNull Player viewer) {}

    @Override
    public int entityId() {
        return entityId;
    }

    protected @NotNull EntityPose pose() {
        return pose;
    }

    protected @NotNull Location location() {
        return new Location(pose.x(), pose.y(), pose.z(), pose.yaw(), pose.pitch());
    }

    protected static void send(@NotNull Player viewer, @NotNull PacketWrapper<?> packet) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
    }

    private void forEachViewer(@NotNull Consumer<Player> action) {
        for (UUID uuid : shown) {
            Player player = tracker.getViewer(uuid);
            if (player != null) {
                action.accept(player);
            }
        }
    }

    private void broadcast(@NotNull PacketWrapper<?> packet) {
        forEachViewer(player -> send(player, packet));
    }

    @Override
    public void move(@NotNull EntityPose to, boolean smooth) {
        lock.lock();
        try {
            this.pose = to;
            this.smooth = smooth;
            this.positionDirty = true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void lookHead(float headYaw) {
        lock.lock();
        try {
            this.pose = new EntityPose(pose.x(), pose.y(), pose.z(), pose.yaw(), pose.pitch(), headYaw);
            this.positionDirty = true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setVelocity(@NotNull Vector3d velocity) {
        lock.lock();
        try {
            this.velocity = velocity;
            this.velocityDirty = true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setMetadata(@NotNull List<EntityData<?>> metadata) {
        lock.lock();
        try {
            this.metadata = metadata;
            this.metadataDirty = true;
            if (tracksHealth()) {
                Float health = healthOf(metadata);
                if (health != null) {
                    if (lastHealth != null && lastHealth <= 0f && health > 0f) {
                        respawnDirty = true;
                    }
                    lastHealth = health;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setEquipment(@NotNull List<Equipment> equipment) {
        lock.lock();
        try {
            this.equipment = equipment;
            this.equipmentDirty = true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void animate(@NotNull EntityAnimationType type) {
        lock.lock();
        try {
            broadcast(new WrapperPlayServerEntityAnimation(entityId, type));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void sendStatus(byte status) {
        lock.lock();
        try {
            broadcast(new WrapperPlayServerEntityStatus(entityId, status));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setRange(double range) {
        lock.lock();
        try {
            this.range = range;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void show(@NotNull Player viewer) {
        lock.lock();
        try {
            hiddenFor.remove(viewer.getUniqueId());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void hide(@NotNull Player viewer) {
        lock.lock();
        try {
            hiddenFor.add(viewer.getUniqueId());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setPresent(boolean present) {
        lock.lock();
        try {
            this.present = present;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void freeze() {
        lock.lock();
        try {
            this.frozen = true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void unfreeze() {
        lock.lock();
        try {
            this.frozen = false;
            this.established = false;
            this.velocityDirty = true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean frozen() {
        lock.lock();
        try {
            return frozen;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void mount(@NotNull VirtualEntity vehicle) {
        lock.lock();
        try {
            this.vehicleId = vehicle.entityId();
            this.attachDirty = true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void dismount() {
        lock.lock();
        try {
            this.vehicleId = null;
            this.attachDirty = true;
        } finally {
            lock.unlock();
        }
    }

    void tick(@NotNull Collection<Player> viewers) {
        lock.lock();
        try {
            pruneDepartedViewers(viewers);
            updateVisibility(viewers);
            if (!shown.isEmpty()) {
                flushState();
                if (frozen && pinWhileFrozen()) {
                    pinFrozen();
                }
            }
            onTick();
            clearDirty();
        } finally {
            lock.unlock();
        }
    }

    private void pruneDepartedViewers(@NotNull Collection<Player> viewers) {
        Set<UUID> viewerUuids = new HashSet<>();
        for (Player p : viewers) {
            viewerUuids.add(p.getUniqueId());
        }

        Set<UUID> toRemove = new HashSet<>();
        for (UUID uuid : shown) {
            if (!viewerUuids.contains(uuid)) {
                toRemove.add(uuid);
            }
        }
        for (UUID uuid : toRemove) {
            Player player = tracker.getViewer(uuid);
            if (player != null) {
                hideFor(player);
            } else {
                shown.remove(uuid);
            }
        }
    }

    private void updateVisibility(@NotNull Collection<Player> viewers) {
        for (Player player : viewers) {
            UUID uuid = player.getUniqueId();
            boolean visible = present && !hiddenFor.contains(uuid) && withinRange(player);
            boolean wasShown = shown.contains(uuid);
            if (visible && !wasShown) {
                showFor(player);
                shown.add(uuid);
            } else if (!visible && wasShown) {
                hideFor(player);
                shown.remove(uuid);
            }
        }
    }

    private void flushState() {
        if (respawnDirty) {
            forEachViewer(this::showFor);
            established = false;
        }
        if (frozen && !wasFrozen) {
            broadcast(new WrapperPlayServerEntityVelocity(entityId, new Vector3d()));
        }
        if (positionDirty) {
            flushPosition();
        }
        if (metadataDirty && !metadata.isEmpty()) {
            broadcast(new WrapperPlayServerEntityMetadata(entityId, metadata));
        }
        if (equipmentDirty) {
            forEachViewer(this::sendEquipment);
        }
        if (velocityDirty && !frozen) {
            broadcast(new WrapperPlayServerEntityVelocity(entityId, velocity));
        }
        if (attachDirty) {
            broadcast(new WrapperPlayServerAttachEntity(entityId, vehicleId == null ? -1 : vehicleId, false));
        }
    }

    private void pinFrozen() {
        broadcast(new WrapperPlayServerEntityVelocity(entityId, new Vector3d()));
        broadcast(new WrapperPlayServerEntityTeleport(entityId, location(), true));
    }

    private void clearDirty() {
        positionDirty = false;
        metadataDirty = false;
        equipmentDirty = false;
        velocityDirty = false;
        attachDirty = false;
        respawnDirty = false;
        wasFrozen = frozen;
    }

    private void flushPosition() {
        EntityPose target = pose;
        boolean useSmooth = smooth;

        int yawByte = toAngle(target.yaw());
        int pitchByte = toAngle(target.pitch());
        int headByte = toAngle(target.headYaw());

        if (!useSmooth || !established) {
            doTeleport(target, yawByte, pitchByte, headByte);
            return;
        }

        moveTicks++;
        sinceTeleport++;

        long fx = toFixedPoint(target.x());
        long fy = toFixedPoint(target.y());
        long fz = toFixedPoint(target.z());
        long dx = fx - encX;
        long dy = fy - encY;
        long dz = fz - encZ;

        boolean relative = dx >= -RELATIVE_THRESHOLD && dx < RELATIVE_THRESHOLD
            && dy >= -RELATIVE_THRESHOLD && dy < RELATIVE_THRESHOLD
            && dz >= -RELATIVE_THRESHOLD && dz < RELATIVE_THRESHOLD
            && sinceTeleport <= TELEPORT_RESYNC;
        if (!relative) {
            doTeleport(target, yawByte, pitchByte, headByte);
            return;
        }

        boolean moved = Math.abs(dx) >= 4 || Math.abs(dy) >= 4 || Math.abs(dz) >= 4 || moveTicks % 60 == 0;
        boolean rotated = Math.abs(yawByte - encYaw) >= 4 || Math.abs(pitchByte - encPitch) >= 4;
        boolean headMoved = Math.abs(headByte - encHead) >= 4;

        if (moved) {
            encX = fx;
            encY = fy;
            encZ = fz;
        }
        if (rotated) {
            encYaw = yawByte;
            encPitch = pitchByte;
        }
        if (headMoved) {
            encHead = headByte;
        }

        PacketWrapper<?> motion;
        if (moved && rotated) {
            motion = new WrapperPlayServerEntityRelativeMoveAndRotation(entityId,
                dx / FIXED, dy / FIXED, dz / FIXED, target.yaw(), target.pitch(), true);
        } else if (moved) {
            motion = new WrapperPlayServerEntityRelativeMove(entityId, dx / FIXED, dy / FIXED, dz / FIXED, true);
        } else if (rotated) {
            motion = new WrapperPlayServerEntityRotation(entityId, target.yaw(), target.pitch(), true);
        } else {
            motion = null;
        }

        WrapperPlayServerEntityHeadLook headPacket = headMoved
            ? new WrapperPlayServerEntityHeadLook(entityId, target.headYaw())
            : null;

        forEachViewer(player -> {
            if (motion != null) {
                send(player, motion);
            }
            if (headPacket != null) {
                send(player, headPacket);
            }
        });
    }

    private void doTeleport(@NotNull EntityPose target, int yawByte, int pitchByte, int headByte) {
        encX = toFixedPoint(target.x());
        encY = toFixedPoint(target.y());
        encZ = toFixedPoint(target.z());
        encYaw = yawByte;
        encPitch = pitchByte;
        encHead = headByte;
        sinceTeleport = 0;
        established = true;

        Location loc = new Location(target.x(), target.y(), target.z(), target.yaw(), target.pitch());
        WrapperPlayServerEntityTeleport tpPacket = new WrapperPlayServerEntityTeleport(entityId, loc, true);
        WrapperPlayServerEntityHeadLook headPacket = new WrapperPlayServerEntityHeadLook(entityId, target.headYaw());
        forEachViewer(player -> {
            send(player, tpPacket);
            send(player, headPacket);
        });
    }

    private void showFor(@NotNull Player player) {
        sendSpawn(player);
        if (!metadata.isEmpty()) {
            send(player, new WrapperPlayServerEntityMetadata(entityId, metadata));
        }
        sendEquipment(player);
        send(player, new WrapperPlayServerEntityHeadLook(entityId, pose.headYaw()));
        Vector3d vel = frozen ? new Vector3d() : velocity;
        send(player, new WrapperPlayServerEntityVelocity(entityId, vel));
        if (vehicleId != null) {
            send(player, new WrapperPlayServerAttachEntity(entityId, vehicleId, false));
        }
        established = false;
        onShow(player);
    }

    private void hideFor(@NotNull Player player) {
        sendDestroy(player);
        shown.remove(player.getUniqueId());
        onHide(player);
    }

    void viewerRemoved(@NotNull Player player) {
        lock.lock();
        try {
            if (shown.contains(player.getUniqueId())) {
                hideFor(player);
            }
        } finally {
            lock.unlock();
        }
    }

    void removeAll() {
        lock.lock();
        try {
            Set<UUID> copy = new HashSet<>(shown);
            for (UUID uuid : copy) {
                Player player = tracker.getViewer(uuid);
                if (player != null) {
                    hideFor(player);
                }
            }
            shown.clear();
        } finally {
            lock.unlock();
        }
    }

    private boolean withinRange(@NotNull Player player) {
        double dx = player.getLocation().getX() - pose.x();
        double dz = player.getLocation().getZ() - pose.z();
        return dx * dx + dz * dz <= range * range;
    }

    private void sendEquipment(@NotNull Player player) {
        for (Equipment slot : equipment) {
            send(player, new WrapperPlayServerEntityEquipment(entityId, List.of(slot)));
        }
    }

    private static @Nullable Float healthOf(@NotNull List<EntityData<?>> metadata) {
        for (EntityData<?> data : metadata) {
            if (data.getIndex() == HEALTH_METADATA_INDEX && data.getValue() instanceof Float health) {
                return health;
            }
        }
        return null;
    }

    private static long toFixedPoint(double value) {
        return (long) Math.floor(value * FIXED);
    }

    private static int toAngle(float degrees) {
        return (int) Math.floor(degrees * 256.0f / 360.0f);
    }

}
