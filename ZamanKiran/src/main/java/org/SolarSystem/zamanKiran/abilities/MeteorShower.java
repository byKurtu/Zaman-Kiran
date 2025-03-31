package org.SolarSystem.zamanKiran.abilities;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Collection;

public class MeteorShower extends Ability {
    private static final int METEOR_COUNT = 5;
    private static final double METEOR_RADIUS = 10.0;
    private static final double DAMAGE = 8.0;

    public MeteorShower(Plugin plugin) {
        super(plugin, "Meteor Shower", 30, 50);
    }

    @Override
    public void cast(Player player) {
        Location playerLoc = player.getLocation();
        World world = player.getWorld();

        for (int i = 0; i < METEOR_COUNT; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    spawnMeteor(player, playerLoc);
                }
            }.runTaskLater(plugin, i * 10L);
        }

        startCooldown(player);
    }

    private void spawnMeteor(Player caster, Location targetLoc) {
        double angle = Math.random() * 2 * Math.PI;
        double radius = Math.random() * METEOR_RADIUS;
        Location spawnLoc = targetLoc.clone().add(
            radius * Math.cos(angle),
            20,
            radius * Math.sin(angle)
        );

        BlockDisplay meteor = (BlockDisplay) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.BLOCK_DISPLAY);
        meteor.setBlock(Material.MAGMA_BLOCK.createBlockData());

        Transformation transformation = new Transformation(
            new Vector3f(0, 0, 0),
            new AxisAngle4f((float) Math.random() * 360, 1, 1, 1),
            new Vector3f(0.5f, 0.5f, 0.5f),
            new AxisAngle4f(0, 0, 0, 1)
        );
        meteor.setTransformation(transformation);

        new BukkitRunnable() {
            private final Vector velocity = new Vector(0, -0.5, 0);
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks++ > 40 || meteor.isDead()) {
                    meteor.remove();
                    this.cancel();
                    return;
                }

                Location loc = meteor.getLocation();
                meteor.teleport(loc.add(velocity));

                loc.getWorld().spawnParticle(Particle.FLAME, loc, 5, 0.2, 0.2, 0.2, 0.05);

                if (loc.getBlock().getType().isSolid()) {
                    createExplosion(loc);
                    meteor.remove();
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void createExplosion(Location location) {
        World world = location.getWorld();
        world.spawnParticle(Particle.EXPLOSION_HUGE, location, 1);
        world.spawnParticle(Particle.FLAME, location, 30, 2, 2, 2, 0.1);
        
        Collection<Entity> nearbyEntities = world.getNearbyEntities(location, 3, 3, 3);
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Player) {
                ((Player) entity).damage(DAMAGE);
            }
        }
    }
} 