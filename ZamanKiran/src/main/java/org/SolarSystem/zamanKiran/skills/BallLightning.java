package org.SolarSystem.zamanKiran.skills;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BallLightning extends Skill {
    private static final double DAMAGE = 10.0;
    private static final double RANGE = 20.0;
    private static final double RADIUS = 2.0;
    private static final Random random = new Random();

    public BallLightning(Plugin plugin) {
        super(plugin, "Ball Lightning", 15, 75);
    }

    @Override
    public void cast(Player caster) {
        startSkill();
        Location start = caster.getEyeLocation();
        Vector direction = start.getDirection();
        
        BlockDisplay coreBall = createBlockDisplay(start, Material.LIGHT_BLUE_STAINED_GLASS);
        if (coreBall != null) {
            scaleBlockDisplay(coreBall, 0.5f);
        }

        List<BlockDisplay> orbitalDisplays = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            BlockDisplay display = createBlockDisplay(start, Material.LIGHTNING_ROD);
            if (display != null) {
                orbitalDisplays.add(display);
                scaleBlockDisplay(display, 0.3f);
            }
        }

        List<BlockDisplay> particleDisplays = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            BlockDisplay display = createBlockDisplay(start, Material.BLUE_STAINED_GLASS);
            if (display != null) {
                particleDisplays.add(display);
                scaleBlockDisplay(display, 0.2f);
            }
        }

        playSound(start, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 2.0f);

        new BukkitRunnable() {
            double distance = 0;
            double angle = 0;
            List<Location> hitLocations = new ArrayList<>();
            
            @Override
            public void run() {
                if (!isActive || distance >= RANGE) {
                    endSkill();
                    this.cancel();
                    return;
                }

                Location current = start.clone().add(direction.clone().multiply(distance));

                if (coreBall != null && coreBall.isValid()) {
                    coreBall.teleport(current);
                    rotateBlockDisplay(coreBall, (float)angle, 1, 1, 1);
                }

                for (int i = 0; i < orbitalDisplays.size(); i++) {
                    BlockDisplay display = orbitalDisplays.get(i);
                    if (display != null && display.isValid()) {
                        double orbitAngle = angle + ((2 * Math.PI * i) / orbitalDisplays.size());
                        Location orbitLoc = current.clone().add(
                            Math.cos(orbitAngle) * 0.5,
                            Math.sin(orbitAngle) * 0.5,
                            Math.sin(orbitAngle) * 0.5
                        );
                        display.teleport(orbitLoc);
                        rotateBlockDisplay(display, (float)orbitAngle, 0, 1, 0);
                    }
                }

                for (int i = 0; i < particleDisplays.size(); i++) {
                    BlockDisplay display = particleDisplays.get(i);
                    if (display != null && display.isValid()) {
                        double particleAngle = angle + ((2 * Math.PI * i) / particleDisplays.size());
                        double radius = 0.7 + Math.sin(angle * 2) * 0.3;
                        Location particleLoc = current.clone().add(
                            Math.cos(particleAngle) * radius,
                            Math.sin(particleAngle * 2) * 0.5,
                            Math.sin(particleAngle) * radius
                        );
                        display.teleport(particleLoc);
                    }
                }

                if (random.nextInt(5) == 0) {
                    current.getWorld().strikeLightningEffect(current);
                }

                current.getWorld().getNearbyEntities(current, RADIUS, RADIUS, RADIUS).forEach(entity -> {
                    if (entity instanceof LivingEntity && entity != caster) {
                        LivingEntity target = (LivingEntity) entity;
                        Location targetLoc = target.getLocation();
                        
                        if (!target.isInvulnerable() && !hitLocations.contains(targetLoc)) {
                            target.damage(DAMAGE, caster);
                            hitLocations.add(targetLoc);
                            
                            target.getWorld().strikeLightningEffect(targetLoc);
                            playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.0f);
                            
                            target.getWorld().spawnParticle(
                                Particle.ELECTRIC_SPARK,
                                targetLoc.add(0, 1, 0),
                                20, 0.5, 1, 0.5, 0.1
                            );
                        }
                    }
                });

                current.getWorld().spawnParticle(
                    Particle.SOUL_FIRE_FLAME,
                    current,
                    5, 0.2, 0.2, 0.2, 0.02
                );

                distance += 0.5;
                angle += Math.PI / 8;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
} 