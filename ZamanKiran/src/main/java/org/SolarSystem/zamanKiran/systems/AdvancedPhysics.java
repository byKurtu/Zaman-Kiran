package org.SolarSystem.zamanKiran.systems;

import org.joml.Vector3d;

public class AdvancedPhysics {
    private static final double GRAVITY = -9.81;
    private static final double AIR_RESISTANCE = 0.02;
    
    public static Vector3d calculateProjectileMotion(Vector3d position, Vector3d velocity, double dt) {
        Vector3d acceleration = new Vector3d(0, GRAVITY, 0);
        Vector3d newPosition = position.add(
            velocity.mul(dt).add(acceleration.mul(0.5 * dt * dt))
        );
        return newPosition;
    }
}
