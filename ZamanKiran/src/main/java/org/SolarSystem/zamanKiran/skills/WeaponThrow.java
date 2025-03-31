package org.SolarSystem.zamanKiran.skills;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.util.Transformation;
import org.bukkit.inventory.ItemStack;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import org.joml.Math;

public class WeaponThrow extends Skill {
    private static final double THROW_SPEED = 1.5;
    private static final double GRAVITY = -0.05;
    private static final double AIR_RESISTANCE = 0.02;
    private static final double ROTATION_SPEED = Math.PI / 8;
    private static final double DAMAGE = 15.0;
    private static final double MAX_DISTANCE = 30.0;

    public WeaponThrow(Plugin plugin) {
        super(plugin, "Weapon Throw", 5, 30);
    }

    @Override
    public void cast(Player caster) {
        startSkill();
        Location start = caster.getEyeLocation();
        Vector direction = start.getDirection();
        
        ItemDisplay weaponDisplay = start.getWorld().spawn(start, ItemDisplay.class);
        ItemStack weaponItem = new ItemStack(Material.NETHERITE_SWORD);
        weaponDisplay.setItemStack(weaponItem);
        
        BlockDisplay trailDisplay = createBlockDisplay(start, Material.PURPLE_STAINED_GLASS);
        if (trailDisplay != null) {
            scaleBlockDisplay(trailDisplay, 0.3f);
        }

        ArmorStand hitbox = createArmorStand(start, true, false);
        if (hitbox != null) {
            hitbox.setMarker(true);
        }

        playSound(start, Sound.ITEM_TRIDENT_THROW, 1.0f, 0.5f);

        new BukkitRunnable() {
            private double distance = 0;
            private Vector currentVelocity = direction.clone().multiply(THROW_SPEED);
            private Location currentLoc = start.clone();
            private float rotation = 0;
            private final Vector3f rotationAxis = new Vector3f(1, 1, 1).normalize();
            
            @Override
            public void run() {
                if (!isActive || distance >= MAX_DISTANCE) {
                    cleanup();
                    return;
                }

                currentVelocity.setY(currentVelocity.getY() + GRAVITY);
                currentVelocity.multiply(1 - AIR_RESISTANCE);
                
                currentLoc.add(currentVelocity);
                distance += currentVelocity.length();
                rotation += ROTATION_SPEED;

                if (weaponDisplay != null && weaponDisplay.isValid()) {
                    weaponDisplay.teleport(currentLoc);
                    
                    Quaternionf rot = new Quaternionf()
                        .rotateAxis(rotation, rotationAxis.x, rotationAxis.y, rotationAxis.z);
                    
                    Transformation transform = new Transformation(
                        new Vector3f(0, 0, 0),
                        rot,
                        new Vector3f(1, 1, 1),
                        new Quaternionf()
                    );
                    weaponDisplay.setTransformation(transform);
                }

                if (trailDisplay != null && trailDisplay.isValid()) {
                    trailDisplay.teleport(currentLoc);
                    rotateBlockDisplay(trailDisplay, rotation, rotationAxis.x, rotationAxis.y, rotationAxis.z);
                }

                if (hitbox != null && hitbox.isValid()) {
                    hitbox.teleport(currentLoc);
                }

                if (currentLoc.getBlock().getType().isSolid()) {
                    createImpactEffect(currentLoc);
                    cleanup();
                    return;
                }

                currentLoc.getWorld().getNearbyEntities(currentLoc, 1, 1, 1).forEach(entity -> {
                    if (entity instanceof LivingEntity && entity != caster && 
                        !entity.equals(weaponDisplay) && !entity.equals(hitbox) && 
                        !entity.equals(trailDisplay)) {
                        LivingEntity target = (LivingEntity) entity;
                        if (!target.isInvulnerable()) {
                            target.damage(DAMAGE, caster);
                            createImpactEffect(target.getLocation());
                            cleanup();
                            cancel();
                        }
                    }
                });

                spawnParticleLine(
                    currentLoc,
                    currentLoc.clone().add(currentVelocity.clone().multiply(-0.5)),
                    Particle.CRIT,
                    2
                );
            }

            private void cleanup() {
                if (weaponDisplay != null && weaponDisplay.isValid()) {
                    weaponDisplay.remove();
                }
                endSkill();
                cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void createImpactEffect(Location location) {
        location.getWorld().spawnParticle(
            Particle.EXPLOSION_LARGE,
            location,
            1, 0, 0, 0, 0
        );
        
        location.getWorld().spawnParticle(
            Particle.CRIT_MAGIC,
            location,
            20, 0.5, 0.5, 0.5, 0.5
        );

        playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);
        
        for (double i = 0; i < Math.PI * 2; i += Math.PI / 8) {
            double x = Math.cos(i) * 2;
            double z = Math.sin(i) * 2;
            Location particleLoc = location.clone().add(x, 0, z);
            
            location.getWorld().spawnParticle(
                Particle.SWEEP_ATTACK,
                particleLoc,
                1, 0, 0, 0, 0
            );
        }
    }
} 