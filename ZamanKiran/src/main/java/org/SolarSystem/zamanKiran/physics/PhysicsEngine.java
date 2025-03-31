package org.SolarSystem.zamanKiran.physics;

import org.bukkit.util.Vector;
import org.joml.Vector3d;
import org.SolarSystem.zamanKiran.math.VectorUtils;

public class PhysicsEngine {
    private static final double GRAVITY = -0.08;
    private static final double AIR_RESISTANCE = 0.02;
    private static final double GROUND_FRICTION = 0.6;
    private static final double ELASTICITY = 0.7;
    private static final double MIN_VELOCITY = 0.005;

    public static class PhysicsState {
        public Vector3d position;
        public Vector3d velocity;
        public Vector3d acceleration;
        public double mass;
        public boolean grounded;

        public PhysicsState(Vector3d position, Vector3d velocity, double mass) {
            this.position = new Vector3d(position);
            this.velocity = new Vector3d(velocity);
            this.acceleration = new Vector3d(0, GRAVITY, 0);
            this.mass = mass;
            this.grounded = false;
        }

        public void update(double deltaTime) {
            Vector3d halfStepVelocity = new Vector3d(velocity).add(
                acceleration.x * deltaTime * 0.5,
                acceleration.y * deltaTime * 0.5,
                acceleration.z * deltaTime * 0.5
            );

            position.add(
                halfStepVelocity.x * deltaTime,
                halfStepVelocity.y * deltaTime,
                halfStepVelocity.z * deltaTime
            );

            velocity.add(
                acceleration.x * deltaTime,
                acceleration.y * deltaTime,
                acceleration.z * deltaTime
            );

            if (!grounded) {
                velocity.mul(1.0 - AIR_RESISTANCE * deltaTime);
            }
            else {
                velocity.x *= 1.0 - GROUND_FRICTION * deltaTime;
                velocity.z *= 1.0 - GROUND_FRICTION * deltaTime;
            }

            if (velocity.lengthSquared() < MIN_VELOCITY * MIN_VELOCITY) {
                velocity.zero();
            }
        }
    }

    public static class CollisionResult {
        public boolean collided;
        public Vector3d normal;
        public double penetration;

        public CollisionResult() {
            this.collided = false;
            this.normal = new Vector3d();
            this.penetration = 0;
        }
    }

    public static void resolveCollision(PhysicsState state, CollisionResult collision) {
        if (!collision.collided) return;

        state.position.add(new Vector3d(collision.normal).mul(collision.penetration));

        double velocityAlongNormal = state.velocity.dot(collision.normal);
        if (velocityAlongNormal < 0) {
            Vector3d reflectionVelocity = VectorUtils.calculateReflection(state.velocity, collision.normal);
            state.velocity = reflectionVelocity.mul(ELASTICITY);
            
            state.grounded = collision.normal.y > 0.7;
        }
    }

    public static void applyForce(PhysicsState state, Vector3d force) {
        state.acceleration.add(new Vector3d(force).div(state.mass));
    }

    public static void applyImpulse(PhysicsState state, Vector3d impulse) {
        state.velocity.add(new Vector3d(impulse).div(state.mass));
    }

    public static CollisionResult checkSphereCollision(Vector3d center1, double radius1, 
                                                     Vector3d center2, double radius2) {
        CollisionResult result = new CollisionResult();
        Vector3d normal = center2.sub(center1, new Vector3d());
        double distance = normal.length();
        double minDistance = radius1 + radius2;

        if (distance < minDistance) {
            result.collided = true;
            result.normal = normal.normalize();
            result.penetration = minDistance - distance;
        }

        return result;
    }

    public static CollisionResult checkBoxCollision(Vector3d min1, Vector3d max1, 
                                                  Vector3d min2, Vector3d max2) {
        CollisionResult result = new CollisionResult();

        boolean overlapX = max1.x >= min2.x && min1.x <= max2.x;
        boolean overlapY = max1.y >= min2.y && min1.y <= max2.y;
        boolean overlapZ = max1.z >= min2.z && min1.z <= max2.z;

        if (overlapX && overlapY && overlapZ) {
            result.collided = true;

            double[] penetrations = new double[] {
                Math.min(max1.x - min2.x, max2.x - min1.x),
                Math.min(max1.y - min2.y, max2.y - min1.y),
                Math.min(max1.z - min2.z, max2.z - min1.z)
            };

            int minIndex = 0;
            for (int i = 1; i < 3; i++) {
                if (penetrations[i] < penetrations[minIndex]) {
                    minIndex = i;
                }
            }

            result.penetration = penetrations[minIndex];
            result.normal = new Vector3d();
            switch (minIndex) {
                case 0: result.normal.x = (max1.x + min1.x < max2.x + min2.x) ? -1 : 1; break;
                case 1: result.normal.y = (max1.y + min1.y < max2.y + min2.y) ? -1 : 1; break;
                case 2: result.normal.z = (max1.z + min1.z < max2.z + min2.z) ? -1 : 1; break;
            }
        }

        return result;
    }
} 