package org.SolarSystem.zamanKiran.skills;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.util.Transformation;
import org.bukkit.inventory.ItemStack;
import org.joml.Vector3f;
import org.joml.Quaternionf;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ChronoSphere extends Skill {
    private static final double RADIUS = 5.0;
    private static final int DURATION = 5 * 20; // 5 seconds
    private final Set<Entity> frozenEntities = new HashSet<>();

    public ChronoSphere(Plugin plugin) {
        super(plugin, "ChronoSphere", 30, 100);
    }

    @Override
    public void cast(Player caster) {
        startSkill();
        Location center = caster.getLocation();
        
        playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.5f);

        ArmorStand clockStand = createArmorStand(center.clone().add(0, 1, 0), true, false);
        if (clockStand != null) {
            clockStand.setHelmet(new ItemStack(Material.CLOCK));
        }

        createSphereEffect(center);

        Collection<Entity> nearbyEntities = center.getWorld().getNearbyEntities(center, RADIUS, RADIUS, RADIUS);
        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity && entity != caster) {
                freezeEntity((LivingEntity) entity);
                frozenEntities.add(entity);
            }
        }

        new BukkitRunnable() {
            int ticks = 0;
            double rotation = 0;
            final int particleCount = 8;
            final ItemDisplay[] orbitalDisplays = new ItemDisplay[particleCount];

            @Override
            public void run() {
                if (!isActive) {
                    this.cancel();
                    return;
                }

                if (ticks == 0) {
                    for (int i = 0; i < particleCount; i++) {
                        ItemDisplay display = createItemDisplay(center.clone(), Material.END_CRYSTAL);
                        if (display != null) {
                            orbitalDisplays[i] = display;
                            scaleItemDisplay(display, 0.3f);
                        }
                    }
                }

                if (ticks++ >= DURATION) {
                    endSkill();
                    unfreezeEntities();
                    this.cancel();
                    return;
                }

                if (clockStand != null && clockStand.isValid()) {
                    clockStand.setRotation(clockStand.getLocation().getYaw() + 10, 0);
                }

                rotation += Math.PI / 16;
                for (int i = 0; i < particleCount; i++) {
                    ItemDisplay display = orbitalDisplays[i];
                    if (display != null && display.isValid()) {
                        double angle = rotation + ((2 * Math.PI * i) / particleCount);
                        double x = RADIUS * Math.cos(angle);
                        double z = RADIUS * Math.sin(angle);
                        
                        Location newLoc = center.clone().add(x, Math.sin(rotation + i) * 0.5, z);
                        display.teleport(newLoc);
                        rotateItemDisplay(display, (float)rotation, 0f, 1f, 0f);
                    }
                }

                for (Entity entity : frozenEntities) {
                    if (entity instanceof LivingEntity && entity.isValid()) {
                        maintainFreeze((LivingEntity) entity);
                        
                        Location entityLoc = entity.getLocation();
                        entityLoc.getWorld().spawnParticle(
                            Particle.SNOWFLAKE,
                            entityLoc.clone().add(0, 1, 0),
                            3, 0.2, 0.5, 0.2, 0
                        );
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void createSphereEffect(Location center) {
        new BukkitRunnable() {
            double radius = 0;
            final ItemDisplay[] sphereDisplays = new ItemDisplay[16];
            int tick = 0;
            
            @Override
            public void run() {
                if (!isActive) {
                    this.cancel();
                    return;
                }

                if (tick == 0) {
                    for (int i = 0; i < sphereDisplays.length; i++) {
                        ItemDisplay display = createItemDisplay(center, Material.LIGHT_BLUE_STAINED_GLASS);
                        if (display != null) {
                            sphereDisplays[i] = display;
                            scaleItemDisplay(display, 0.5f);
                        }
                    }
                }

                if (radius >= RADIUS) {
                    this.cancel();
                    return;
                }

                for (int i = 0; i < sphereDisplays.length; i++) {
                    ItemDisplay display = sphereDisplays[i];
                    if (display != null && display.isValid()) {
                        double phi = Math.PI * i / (sphereDisplays.length / 2);
                        double x = radius * Math.sin(phi) * Math.cos(tick * 0.2);
                        double y = radius * Math.cos(phi);
                        double z = radius * Math.sin(phi) * Math.sin(tick * 0.2);
                        
                        Location displayLoc = center.clone().add(x, y, z);
                        display.teleport(displayLoc);
                        rotateItemDisplay(display, tick * 0.1f, 1f, 1f, 1f);
                    }
                }

                radius += 0.2;
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void freezeEntity(LivingEntity entity) {

        entity.setMetadata("frozen_velocity", new org.bukkit.metadata.FixedMetadataValue(
            plugin, entity.getVelocity()
        ));

        ItemDisplay iceDisplay = createItemDisplay(
            entity.getLocation().add(0, 2, 0),
            Material.ICE
        );
        if (iceDisplay != null) {
            scaleItemDisplay(iceDisplay, 0.3f);
        }

        entity.setVelocity(new Vector(0, 0, 0));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, DURATION, 100, false, false));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, DURATION, 128, false, false));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, DURATION, 1, false, false));
    }

    private void maintainFreeze(LivingEntity entity) {
        entity.setVelocity(new Vector(0, 0, 0));
    }

    private void unfreezeEntities() {
        for (Entity entity : frozenEntities) {
            if (entity instanceof LivingEntity && entity.isValid()) {
                LivingEntity living = (LivingEntity) entity;
                
                if (living.hasMetadata("frozen_velocity")) {
                    Vector velocity = (Vector) living.getMetadata("frozen_velocity").get(0).value();
                    living.setVelocity(velocity);
                    living.removeMetadata("frozen_velocity", plugin);
                }

                living.removePotionEffect(PotionEffectType.SLOW);
                living.removePotionEffect(PotionEffectType.JUMP);
                living.removePotionEffect(PotionEffectType.GLOWING);

                Location loc = living.getLocation();
                loc.getWorld().spawnParticle(
                    Particle.CLOUD,
                    loc.clone().add(0, 1, 0),
                    20, 0.5, 1, 0.5, 0.1
                );
            }
        }
        frozenEntities.clear();
    }
} 