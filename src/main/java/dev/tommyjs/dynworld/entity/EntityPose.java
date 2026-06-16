package dev.tommyjs.dynworld.entity;

public record EntityPose(double x, double y, double z, float yaw, float pitch, float headYaw) {

    public static EntityPose of(double x, double y, double z, float yaw, float pitch) {
        return new EntityPose(x, y, z, yaw, pitch, yaw);
    }

    public EntityPose withPosition(double x, double y, double z) {
        return new EntityPose(x, y, z, yaw, pitch, headYaw);
    }

    public EntityPose withRotation(float yaw, float pitch, float headYaw) {
        return new EntityPose(x, y, z, yaw, pitch, headYaw);
    }

}
