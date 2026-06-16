package dev.tommyjs.dynworld.entity;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public interface PlayerEntity extends VirtualEntity {

    void setTablistLinger(@Nullable Duration linger);

}
