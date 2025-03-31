package org.SolarSystem.zamanKiran.skills;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

public class WaveCutting extends Skill {
    private static final double DAMAGE = 15.0;
    private static final double RANGE = 15.0;
    private static final double WIDTH = 2.0;

    public WaveCutting(Plugin plugin) {
        super(plugin, "Wave Cutting", 10, 50);
    }

    @Override
    public void cast(Player caster) {
        startSkill();
        Location start = caster.getEyeLocation();
        Vector direction = start.getDirection();
        
        ArmorStand swordStand = createArmorStand(start, true, false);
        if (swordStand != null) {
            swordStand.setHelmet(new org.bukkit.inventory.ItemStack(Material.NETHERITE_SWORD));
        }

        List<BlockDisplay> waveDisplays = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            BlockDisplay display = createBlockDisplay(start, Material.PURPLE_STAINED_GLASS);
            if (display != null) {
                waveDisplays.add(display);
                scaleBlockDisplay(display, 0.5f);
            }
        }

        playSound(start, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.5f);
        playSound(start, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 2.0f);

        new BukkitRunnable() {
            double distance = 0;
            double angle = 0;
            
            @Override
            public void run() {
                if (!isActive || distance >= RANGE) {
                    endSkill();
                    this.cancel();
                    return;
                }

                if (swordStand != null && swordStand.isValid()) {
                    Location swordLoc = start.clone().add(direction.clone().multiply(distance));
                    swordStand.teleport(swordLoc);
                    swordStand.setRotation(swordLoc.getYaw() + (float)angle, swordLoc.getPitch());
                }

                for (int i = 0; i < waveDisplays.size(); i++) {
                    BlockDisplay display = waveDisplays.get(i);
                    if (display != null && display.isValid()) {
                        double offset = (Math.PI * 2 * i) / waveDisplays.size();
                        double waveX = Math.cos(angle + offset) * WIDTH;
                        double waveY = Math.sin(angle + offset) * WIDTH;
                        
                        Vector perpendicular = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
                        Location waveLoc = start.clone()
                            .add(direction.clone().multiply(distance))
                            .add(perpendicular.clone().multiply(waveX))
                            .add(0, waveY, 0);
                        
                        display.teleport(waveLoc);
                        rotateBlockDisplay(display, (float)(angle * 2), 1, 1, 1);
                    }
                }

                Location checkLoc = start.clone().add(direction.clone().multiply(distance));
                checkLoc.getWorld().getNearbyEntities(checkLoc, WIDTH, WIDTH, WIDTH).forEach(entity -> {
                    if (entity instanceof LivingEntity && entity != caster) {
                        LivingEntity target = (LivingEntity) entity;
                        if (!target.isInvulnerable()) {
                            target.damage(DAMAGE, caster);
                            target.setVelocity(direction.clone().multiply(0.5));
                            
                            target.getWorld().spawnParticle(
                                Particle.SWEEP_ATTACK,
                                target.getLocation().add(0, 1, 0),
                                3, 0.2, 0.2, 0.2, 0
                            );
                            playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.0f);
                        }
                    }
                });

                for (int i = 0; i < 3; i++) {
                    Location particleLoc = start.clone()
                        .add(direction.clone().multiply(distance))
                        .add(Math.random() * WIDTH - WIDTH/2, Math.random() * WIDTH, Math.random() * WIDTH - WIDTH/2);
                    
                    checkLoc.getWorld().spawnParticle(
                        Particle.CRIT_MAGIC,
                        particleLoc,
                        1, 0, 0, 0, 0
                    );
                }

                distance += 0.5;
                angle += Math.PI / 8;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
} 