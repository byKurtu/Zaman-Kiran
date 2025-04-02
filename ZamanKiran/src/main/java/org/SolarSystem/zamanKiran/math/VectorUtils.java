package org.SolarSystem.zamanKiran.math;

import org.bukkit.util.Vector;
import org.joml.Vector3d;
import org.joml.Quaternionf;

public class VectorUtils {
    public static Vector3d toVec3d(Vector bukkitVector) {
        return new Vector3d(
            bukkitVector.getX(),
            bukkitVector.getY(),
            bukkitVector.getZ()
        );
    }

    public static Vector toBukkitVector(Vector3d jomlVector) {
        return new Vector(
            jomlVector.x,
            jomlVector.y,
            jomlVector.z
        );
    }

    public static Vector3d rotateVector(Vector3d vector, Quaternionf rotation) {
        Vector3d result = new Vector3d(vector);
        rotation.transform(result);
        return result;
    }

    public static Vector3d calculateInterceptPoint(Vector3d projectilePos, Vector3d projectileVel, 
                                                 Vector3d targetPos, Vector3d targetVel) {
        Vector3d relativePos = targetPos.sub(projectilePos, new Vector3d());
        Vector3d relativeVel = targetVel.sub(projectileVel, new Vector3d());
        
        double a = relativeVel.lengthSquared();
        double b = 2 * relativePos.dot(relativeVel);
        double c = relativePos.lengthSquared();
        
        // Solve quadratic equation: at² + bt + c = 0
        double discriminant = b * b - 4 * a * c;
        if (discriminant < 0) return null;
        
        double t = (-b - Math.sqrt(discriminant)) / (2 * a);
        if (t < 0) return null;
        
        return targetPos.add(targetVel.mul(t, new Vector3d()), new Vector3d());
    }

    public static Vector3d calculateArcTrajectory(Vector3d start, Vector3d end, double height) {
        double distance = start.distance(end);
        double gravity = 0.05;

        double vY = Math.sqrt(2 * gravity * height);
        double time = (vY / gravity) * 2;
        
        Vector3d direction = end.sub(start, new Vector3d()).normalize();
        double vXZ = distance / time;
        
        return direction.mul(vXZ).add(0, vY, 0);
    }

    public static Vector3d calculateReflection(Vector3d incident, Vector3d normal) {
        double dot = incident.dot(normal);
        return incident.sub(normal.mul(2 * dot, new Vector3d()), new Vector3d());
    }

    public static Quaternionf createRotationBetweenVectors(Vector3d from, Vector3d to) {
        Vector3d fromNorm = new Vector3d(from).normalize();
        Vector3d toNorm = new Vector3d(to).normalize();
        
        double dot = fromNorm.dot(toNorm);
        if (dot > 0.999999) return new Quaternionf();
        
        Vector3d rotationAxis = fromNorm.cross(toNorm, new Vector3d()).normalize();
        double angle = Math.acos(dot);
        
        return new Quaternionf().rotateAxis((float)angle, 
            (float)rotationAxis.x, (float)rotationAxis.y, (float)rotationAxis.z);
    }

    public static double calculateProjectileDistance(double velocity, double angle, double gravity) {
        // d = (v² * sin(2θ)) / g
        double radians = Math.toRadians(angle);
        return (velocity * velocity * Math.sin(2 * radians)) / Math.abs(gravity);
    }

    public static Vector rotateVector(Vector vector, double angleX, double angleY, double angleZ) {
        double x = vector.getX();
        double y = vector.getY();
        double z = vector.getZ();
        
        // X ekseni etrafında döndürme
        double cosX = Math.cos(angleX);
        double sinX = Math.sin(angleX);
        double newY = y * cosX - z * sinX;
        double newZ = y * sinX + z * cosX;
        y = newY;
        z = newZ;
        
        // Y ekseni etrafında döndürme
        double cosY = Math.cos(angleY);
        double sinY = Math.sin(angleY);
        double newX = x * cosY + z * sinY;
        newZ = -x * sinY + z * cosY;
        x = newX;
        z = newZ;
        
        // Z ekseni etrafında döndürme
        double cosZ = Math.cos(angleZ);
        double sinZ = Math.sin(angleZ);
        newX = x * cosZ - y * sinZ;
        newY = x * sinZ + y * cosZ;
        x = newX;
        y = newY;
        
        return new Vector(x, y, z);
    }

    public static Vector calculateInterceptVector(Vector from, Vector target, double projectileSpeed) {
        Vector toTarget = target.clone().subtract(from);
        double distance = toTarget.length();
        
        // Hedefin hızı sıfır kabul edilirse:
        double timeToTarget = distance / projectileSpeed;
        
        return toTarget.normalize().multiply(projectileSpeed);
    }
} 