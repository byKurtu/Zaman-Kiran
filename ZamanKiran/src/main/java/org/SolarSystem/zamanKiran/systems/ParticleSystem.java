package org.SolarSystem.zamanKiran.systems;

import org.bukkit.Location;

public class ParticleSystem {
    public static void createHelix(Location center, double radius, double height) {
        double phi = Math.PI / 2;
        for(double t = 0; t <= 2*Math.PI; t += 0.1) {
            double x = radius * Math.cos(t);
            double y = height * t / (2*Math.PI);
            double z = radius * Math.sin(t);
        }
    }
}
