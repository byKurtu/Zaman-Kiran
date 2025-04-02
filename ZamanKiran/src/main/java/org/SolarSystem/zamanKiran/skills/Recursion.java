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
import java.util.Random;

public class Recursion extends Skill {
    private static final double DAMAGE = 20.0;
    private static final double RANGE = 30.0;
    private static final int DURATION = 10 * 20; // 10 saniye
    private static final double INITIAL_SPEED = 0.5;
    private static final double ACCELERATION = 0.02;
    private static final double CIRCLE_RADIUS = 3.5;
    private static final Random random = new Random();

    public Recursion(Plugin plugin) {
        super(plugin, "Recursion", 15, 60);
    }

    @Override
    public void cast(Player caster) {
        startSkill();
        Location start = caster.getLocation().add(0, 2, 0);
        Vector direction = caster.getEyeLocation().getDirection();

        // Portal çemberi oluştur
        List<BlockDisplay> portalBlocks = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            double angle = (2 * Math.PI * i) / 16;
            Location blockLoc = start.clone().add(
                Math.cos(angle) * CIRCLE_RADIUS,
                0,
                Math.sin(angle) * CIRCLE_RADIUS
            );
            
            BlockDisplay block = createBlockDisplay(blockLoc, Material.LIGHT_BLUE_STAINED_GLASS);
            if (block != null) {
                portalBlocks.add(block);
                if (i % 4 == 0) {
                    scaleBlockDisplay(block, 1.0f);
                } else {
                    scaleBlockDisplay(block, 0.5f);
                }
            }
        }

        // Portal efektleri
        new BukkitRunnable() {
            int ticks = 0;
            double currentSpeed = INITIAL_SPEED;
            
            @Override
            public void run() {
                if (!isActive || ticks >= DURATION) {
                    portalBlocks.forEach(Entity::remove);
                    endSkill();
                    this.cancel();
                    return;
                }

                // Portal bloklarını döndür
                double rotationAngle = ticks * 0.1;
                for (int i = 0; i < portalBlocks.size(); i++) {
                    BlockDisplay block = portalBlocks.get(i);
                    if (block != null && block.isValid()) {
                        double angle = ((2 * Math.PI * i) / 16) + rotationAngle;
                        Location newLoc = start.clone().add(
                            Math.cos(angle) * CIRCLE_RADIUS,
                            Math.sin(angle * 0.5) * 0.5,
                            Math.sin(angle) * CIRCLE_RADIUS
                        );
                        block.teleport(newLoc);
                        rotateBlockDisplay(block, (float)angle, 0, 1, 0);
                    }
                }

                // Her 5 tickte bir yeni blok fırlat
                if (ticks % 5 == 0) {
                    double spawnAngle = random.nextDouble() * 2 * Math.PI;
                    Location spawnLoc = start.clone().add(
                        Math.cos(spawnAngle) * CIRCLE_RADIUS,
                        0,
                        Math.sin(spawnAngle) * CIRCLE_RADIUS
                    );

                    BlockDisplay projectile = createBlockDisplay(spawnLoc, 
                        random.nextBoolean() ? Material.LIGHT_BLUE_STAINED_GLASS : Material.PURPLE_STAINED_GLASS);
                    if (projectile != null) {
                        float scale = 0.3f + (random.nextFloat() * 0.4f);
                        scaleBlockDisplay(projectile, scale);
                        
                        // Rastgele sapma ekle
                        Vector velocity = direction.clone()
                            .add(new Vector(
                                (random.nextDouble() - 0.5) * 0.2,
                                (random.nextDouble() - 0.5) * 0.2,
                                (random.nextDouble() - 0.5) * 0.2
                            ))
                            .normalize()
                            .multiply(currentSpeed);

                        // Blok hareketi
                        new BukkitRunnable() {
                            Location currentLoc = spawnLoc.clone();
                            Vector currentVel = velocity.clone();
                            int projectileTicks = 0;

                            @Override
                            public void run() {
                                if (!projectile.isValid() || projectileTicks++ > 100 || 
                                    currentLoc.distance(start) > RANGE) {
                                    projectile.remove();
                                    this.cancel();
                                    return;
                                }

                                // Pozisyonu güncelle
                                currentLoc.add(currentVel);
                                projectile.teleport(currentLoc);
                                rotateBlockDisplay(projectile, projectileTicks * 0.2f, 1, 1, 1);

                                // Parçacık efekti
                                if (random.nextDouble() < 0.3) {
                                    currentLoc.getWorld().spawnParticle(
                                        Particle.PORTAL,
                                        currentLoc,
                                        1, 0.1, 0.1, 0.1, 0
                                    );
                                }

                                // Hasar kontrolü
                                for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, 1, 1, 1)) {
                                    if (entity instanceof LivingEntity && entity != caster) {
                                        LivingEntity target = (LivingEntity) entity;
                                        if (!target.isInvulnerable()) {
                                            target.damage(DAMAGE, caster);
                                            target.setVelocity(currentVel.clone().multiply(0.3));
                                            projectile.remove();
                                            
                                            // Vuruş efektleri
                                            currentLoc.getWorld().spawnParticle(
                                                Particle.EXPLOSION_NORMAL,
                                                currentLoc,
                                                10, 0.2, 0.2, 0.2, 0.1
                                            );
                                            playSound(currentLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
                                            this.cancel();
                                            return;
                                        }
                                    }
                                }

                                // Blok çarpışma kontrolü
                                if (currentLoc.getBlock().getType().isSolid()) {
                                    projectile.remove();
                                    currentLoc.getWorld().spawnParticle(
                                        Particle.BLOCK_CRACK,
                                        currentLoc,
                                        20, 0.2, 0.2, 0.2, 0,
                                        Material.GLASS.createBlockData()
                                    );
                                    this.cancel();
                                    return;
                                }
                            }
                        }.runTaskTimer(plugin, 0L, 1L);
                    }
                }

                // Portal efektleri
                start.getWorld().spawnParticle(
                    Particle.PORTAL,
                    start,
                    5, CIRCLE_RADIUS, 0.5, CIRCLE_RADIUS, 0
                );

                if (ticks % 20 == 0) { // Her saniye
                    playSound(start, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 1.0f);
                }

                currentSpeed += ACCELERATION;
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
} 