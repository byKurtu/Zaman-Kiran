package org.SolarSystem.zamanKiran.math;

import org.bukkit.util.Vector;
import org.joml.Vector3d;
import org.joml.Quaternionf;

public class VectorUtils {
    public static Vector3d toVec3d(Vector bukkitVector) {
        return new Vector3d(bukkitVector.getX(), bukkitVector.getY(), bukkitVector.getZ());
    }

    public static Vector toBukkitVector(Vector3d vec3d) {
        return new Vector(vec3d.x, vec3d.y, vec3d.z);
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
} 