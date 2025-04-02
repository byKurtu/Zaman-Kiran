package org.SolarSystem.zamanKiran.skills;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;
import org.bukkit.block.Block;
import org.joml.Vector3d;
import org.SolarSystem.zamanKiran.math.VectorUtils;

import java.util.*;

public class ThrowSkill extends Skill {
    private static final double THROW_SPEED = 1.5;
    private static final double GRAVITY = -0.05;
    private static final double AIR_RESISTANCE = 0.02;
    private static final double DAMAGE = 15.0;
    private static final double MAX_DISTANCE = 30.0;
    private static final double PICKUP_DISTANCE = 2.0;
    private static final double AUTO_TARGET_RANGE = 5.0;
    private static final double EXPLOSION_RADIUS = 2.0;
    
    private ThrowMode currentMode = ThrowMode.DIRECTIVE;
    private final Map<UUID, ArmorStand> activeWeapons = new HashMap<>();
    private final Map<UUID, BukkitRunnable> trajectoryTasks = new HashMap<>();

    public ThrowSkill(Plugin plugin) {
        super(plugin, "Throw Skill", 5, 30);
    }

    public void setMode(ThrowMode mode) {
        this.currentMode = mode;
    }

    public ThrowMode getMode() {
        return currentMode;
    }

    @Override
    public void cast(Player caster) {
        startSkill();
        Location start = caster.getEyeLocation();
        Vector direction = start.getDirection();

        switch (currentMode) {
            case AUTO:
                castAutoMode(caster, start, direction);
                break;
            case DIRECTIVE:
                castDirectiveMode(caster, start, direction);
                break;
            case CALCULATIVE:
                castCalculativeMode(caster, start, direction);
                break;
        }
    }

    private void castAutoMode(Player caster, Location start, Vector direction) {
        // En yakın hedefi bul
        Entity target = findNearestTarget(caster, start);
        if (target == null) {
            castDirectiveMode(caster, start, direction);
            return;
        }

        ArmorStand weaponStand = createWeaponStand(start);
        if (weaponStand == null) return;

        activeWeapons.put(caster.getUniqueId(), weaponStand);
        
        new BukkitRunnable() {
            private double t = 0;
            private final Location targetLoc = target.getLocation();
            private final Vector3d p0 = VectorUtils.toVec3d(start.toVector());
            private final Vector3d p2 = VectorUtils.toVec3d(targetLoc.toVector());
            private final Vector3d p1 = calculateControlPoint(p0, p2);
            
            @Override
            public void run() {
                if (!isActive || t >= 1.0 || !weaponStand.isValid()) {
                    cleanup();
                    return;
                }

                // Quadratic Bézier eğrisi
                Vector3d position = calculateBezierPoint(p0, p1, p2, t);
                Location newLoc = new Location(start.getWorld(), position.x, position.y, position.z);
                
                // Rotasyon ve hareket
                updateWeaponPosition(weaponStand, newLoc, targetLoc);
                
                // Parçacık efektleri
                createAutoModeParticles(newLoc);
                
                // Hedef kontrolü
                if (checkTargetHit(caster, weaponStand, newLoc, target)) {
                    cleanup();
                    return;
                }

                t += 0.05;
            }

            private void cleanup() {
                activeWeapons.remove(caster.getUniqueId());
                if (weaponStand.isValid()) weaponStand.remove();
                this.cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void castDirectiveMode(Player caster, Location start, Vector direction) {
        ArmorStand weaponStand = createWeaponStand(start);
        if (weaponStand == null) return;

        activeWeapons.put(caster.getUniqueId(), weaponStand);
        
        new BukkitRunnable() {
            private Vector velocity = direction.clone().multiply(THROW_SPEED);
            private Location currentLoc = start.clone();
            private double distance = 0;
            
            @Override
            public void run() {
                if (!isActive || distance >= MAX_DISTANCE || !weaponStand.isValid()) {
                    cleanup();
                    return;
                }

                // Fizik güncelleme
                velocity.add(new Vector(0, GRAVITY, 0));
                velocity.multiply(1 - AIR_RESISTANCE);
                
                currentLoc.add(velocity);
                distance += velocity.length();

                // Silah pozisyonunu güncelle
                updateWeaponPosition(weaponStand, currentLoc, currentLoc.clone().add(velocity));
                
                // Parçacık efektleri
                createDirectiveModeParticles(currentLoc, velocity);
                
                // Çarpışma kontrolü
                if (checkCollision(caster, weaponStand, currentLoc)) {
                    createExplosion(currentLoc);
                    cleanup();
                    return;
                }
            }

            private void cleanup() {
                activeWeapons.remove(caster.getUniqueId());
                if (weaponStand.isValid()) weaponStand.remove();
                this.cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void castCalculativeMode(Player caster, Location start, Vector direction) {
        // Önceki trajectory task'ı temizle
        BukkitRunnable oldTask = trajectoryTasks.remove(caster.getUniqueId());
        if (oldTask != null) oldTask.cancel();

        // Yeni trajectory task başlat
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!caster.isOnline() || !isActive) {
                    this.cancel();
                    return;
                }

                Vector vel = caster.getLocation().getDirection().multiply(THROW_SPEED);
                showTrajectory(caster.getEyeLocation(), vel);
            }
        };
        
        trajectoryTasks.put(caster.getUniqueId(), task);
        task.runTaskTimer(plugin, 0L, 2L);

        // Fırlatma işlemi
        castDirectiveMode(caster, start, direction);
    }

    private ArmorStand createWeaponStand(Location location) {
        ArmorStand stand = createArmorStand(location, true, false);
        if (stand != null) {
            stand.setHelmet(new ItemStack(Material.DIAMOND_PICKAXE));
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setSmall(false);
            stand.setCustomName("§b✧ Zaman Kıran §b✧");
            stand.setCustomNameVisible(true);
        }
        return stand;
    }

    private void updateWeaponPosition(ArmorStand stand, Location current, Location target) {
        stand.teleport(current);
        
        Vector direction = target.toVector().subtract(current.toVector());
        double yaw = Math.atan2(direction.getZ(), direction.getX());
        double pitch = Math.atan2(Math.sqrt(direction.getX() * direction.getX() + direction.getZ() * direction.getZ()), direction.getY());
        
        stand.setHeadPose(new EulerAngle(pitch, yaw, 0));
    }

    private Entity findNearestTarget(Player caster, Location location) {
        return location.getWorld().getNearbyEntities(location, AUTO_TARGET_RANGE, AUTO_TARGET_RANGE, AUTO_TARGET_RANGE)
            .stream()
            .filter(e -> e instanceof LivingEntity && e != caster && !(e instanceof ArmorStand))
            .min(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(location)))
            .orElse(null);
    }

    private Vector3d calculateControlPoint(Vector3d start, Vector3d end) {
        Vector3d mid = new Vector3d(
            (start.x + end.x) / 2,
            (start.y + end.y) / 2 + 5, // Yay yüksekliği
            (start.z + end.z) / 2
        );
        return mid;
    }

    private Vector3d calculateBezierPoint(Vector3d p0, Vector3d p1, Vector3d p2, double t) {
        double u = 1 - t;
        double tt = t * t;
        double uu = u * u;
        
        return new Vector3d(
            uu * p0.x + 2 * u * t * p1.x + tt * p2.x,
            uu * p0.y + 2 * u * t * p1.y + tt * p2.y,
            uu * p0.z + 2 * u * t * p1.z + tt * p2.z
        );
    }

    private boolean checkTargetHit(Player caster, ArmorStand weapon, Location location, Entity target) {
        if (location.distance(target.getLocation()) < 1.5) {
            if (target instanceof LivingEntity) {
                ((LivingEntity) target).damage(DAMAGE, caster);
                createHitEffect(location);
            }
            return true;
        }
        return false;
    }

    private boolean checkCollision(Player caster, ArmorStand weapon, Location location) {
        // Blok kontrolü
        Block block = location.getBlock();
        if (block.getType().isSolid()) return true;

        // Entity kontrolü
        for (Entity entity : location.getWorld().getNearbyEntities(location, 1, 1, 1)) {
            if (entity instanceof LivingEntity && entity != caster && entity != weapon) {
                if (entity instanceof LivingEntity) {
                    ((LivingEntity) entity).damage(DAMAGE, caster);
                    createHitEffect(location);
                }
                return true;
            }
        }
        return false;
    }

    private void createExplosion(Location location) {
        // Patlama efekti
        location.getWorld().spawnParticle(
            Particle.EXPLOSION_LARGE,
            location,
            1, 0, 0, 0, 0
        );
        
        // Yakındaki entitylere hasar
        location.getWorld().getNearbyEntities(location, EXPLOSION_RADIUS, EXPLOSION_RADIUS, EXPLOSION_RADIUS)
            .stream()
            .filter(e -> e instanceof LivingEntity)
            .forEach(e -> {
                double distance = e.getLocation().distance(location);
                double damage = DAMAGE * (1 - distance / EXPLOSION_RADIUS);
                ((LivingEntity) e).damage(Math.max(1, damage));
            });
        
        // Ses efekti
        playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
    }

    private void createHitEffect(Location location) {
        location.getWorld().spawnParticle(
            Particle.CRIT,
            location,
            10, 0.2, 0.2, 0.2, 0.1
        );
        playSound(location, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.0f);
    }

    private void createAutoModeParticles(Location location) {
        location.getWorld().spawnParticle(
            Particle.SPELL_WITCH,
            location,
            1, 0.1, 0.1, 0.1, 0
        );
    }

    private void createDirectiveModeParticles(Location location, Vector velocity) {
        location.getWorld().spawnParticle(
            Particle.CRIT_MAGIC,
            location,
            1, 0.1, 0.1, 0.1, 0.01
        );
    }

    private void showTrajectory(Location start, Vector velocity) {
        Location current = start.clone();
        Vector vel = velocity.clone();
        
        for (int i = 0; i < 30; i++) {
            vel.add(new Vector(0, GRAVITY, 0));
            vel.multiply(1 - AIR_RESISTANCE);
            current.add(vel);
            
            if (current.getBlock().getType().isSolid()) break;
            
            start.getWorld().spawnParticle(
                Particle.VILLAGER_HAPPY,
                current,
                1, 0, 0, 0, 0
            );
        }
    }

    public enum ThrowMode {
        AUTO("Otomatik Hedefleme"),
        DIRECTIVE("Doğrusal Fırlatma"),
        CALCULATIVE("Hesaplamalı Mod");

        private final String displayName;

        ThrowMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
} 