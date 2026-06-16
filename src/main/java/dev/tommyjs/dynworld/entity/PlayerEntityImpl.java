package dev.tommyjs.dynworld.entity;

import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo.Action;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo.PlayerData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

class PlayerEntityImpl extends AbstractVirtualEntity implements PlayerEntity {

    private final UserProfile profile;
    private @Nullable Duration tablistLinger = Duration.ofSeconds(3);
    private final Map<UUID, Long> lingerDeadlines = new ConcurrentHashMap<>();

    PlayerEntityImpl(@NotNull EntityTrackerImpl tracker, int entityId, @NotNull UserProfile profile, @NotNull EntityPose pose) {
        super(tracker, entityId, pose);
        this.profile = profile;
    }

    @Override
    public void setTablistLinger(@Nullable Duration linger) {
        this.tablistLinger = linger;
    }

    @Override
    protected void sendSpawn(@NotNull Player viewer) {
        PlayerData data = new PlayerData(null, profile, GameMode.SURVIVAL, 0);
        send(viewer, new WrapperPlayServerPlayerInfo(Action.ADD_PLAYER, List.of(data)));
        send(viewer, new WrapperPlayServerSpawnPlayer(entityId, profile.getUUID(), location(), Collections.emptyList()));
        if (tablistLinger != null) {
            lingerDeadlines.put(viewer.getUniqueId(), System.currentTimeMillis() + tablistLinger.toMillis());
        }
    }

    @Override
    protected void sendDestroy(@NotNull Player viewer) {
        send(viewer, new WrapperPlayServerDestroyEntities(entityId));
        UUID uuid = viewer.getUniqueId();
        if (lingerDeadlines.remove(uuid) != null) {
            PlayerData data = new PlayerData(null, profile, GameMode.SURVIVAL, 0);
            send(viewer, new WrapperPlayServerPlayerInfo(Action.REMOVE_PLAYER, List.of(data)));
        }
    }

    @Override
    protected void onTick() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> entry : lingerDeadlines.entrySet()) {
            if (entry.getValue() <= now) {
                UUID uuid = entry.getKey();
                lingerDeadlines.remove(uuid);
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) continue;
                PlayerData data = new PlayerData(null, profile, GameMode.SURVIVAL, 0);
                send(player, new WrapperPlayServerPlayerInfo(Action.REMOVE_PLAYER, List.of(data)));
            }
        }
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
