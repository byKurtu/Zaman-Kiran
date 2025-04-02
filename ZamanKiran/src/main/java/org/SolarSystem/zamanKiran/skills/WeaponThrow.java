package org.SolarSystem.zamanKiran.skills;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.util.EulerAngle;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.Block;

public class WeaponThrow extends Skill {
    private static final double THROW_SPEED = 1.5;
    private static final double GRAVITY = -0.05;
    private static final double AIR_RESISTANCE = 0.02;
    private static final double BOUNCE_FACTOR = 0.6;
    private static final double DAMAGE = 15.0;
    private static final double MAX_DISTANCE = 30.0;
    private static final double PICKUP_DISTANCE = 2.0;

    public WeaponThrow(Plugin plugin) {
        super(plugin, "Weapon Throw", 5, 30);
    }

    @Override
    public void cast(Player caster) {
        startSkill();
        Location start = caster.getEyeLocation();
        Vector direction = start.getDirection();
        
        ArmorStand weaponStand = createArmorStand(start, false, false);
        if (weaponStand != null) {
            weaponStand.setHelmet(new ItemStack(Material.DIAMOND_PICKAXE));
            weaponStand.setVisible(false);
            weaponStand.setGravity(false);
            weaponStand.setSmall(false);
            
            // Hologram isim
            weaponStand.setCustomName("§b✧ Zaman Kıran §b✧");
            weaponStand.setCustomNameVisible(true);
        }

        playSound(start, Sound.ITEM_TRIDENT_THROW, 1.0f, 0.5f);

        new BukkitRunnable() {
            private Vector velocity = direction.clone().multiply(THROW_SPEED);
            private Location currentLoc = start.clone();
            private double distance = 0;
            private int bounceCount = 0;
            private final int MAX_BOUNCES = 3;
            private boolean isGrounded = false;
            
            @Override
            public void run() {
                if (!isActive || distance >= MAX_DISTANCE || bounceCount > MAX_BOUNCES) {
                    if (weaponStand != null && weaponStand.isValid()) {
                        if (!isGrounded) {
                            dropWeapon(weaponStand, currentLoc);
                        }
                    }
                    this.cancel();
                    return;
                }

                if (!isGrounded) {
                    // Yerçekimi ve hava direnci
                    velocity.setY(velocity.getY() + GRAVITY);
                    velocity.multiply(1 - AIR_RESISTANCE);
                    
                    // Yeni pozisyon
                    currentLoc.add(velocity);
                    distance += velocity.length();

                    // ArmorStand'i güncelle
                    if (weaponStand != null && weaponStand.isValid()) {
                        weaponStand.teleport(currentLoc);
                        
                        // Yatay rotasyon
                        double yaw = Math.atan2(velocity.getZ(), velocity.getX());
                        // Dikey rotasyon
                        double pitch = Math.atan2(velocity.getY(), Math.sqrt(velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ()));
                        
                        weaponStand.setHeadPose(new EulerAngle(pitch, yaw, 0));
                    }

                    // Blok kontrolü
                    Block hitBlock = currentLoc.getBlock();
                    if (hitBlock.getType().isSolid()) {
                        // Sekme
                        if (bounceCount < MAX_BOUNCES) {
                            Vector normal = getNormalVector(hitBlock, currentLoc);
                            velocity = calculateReflection(velocity, normal).multiply(BOUNCE_FACTOR);
                            currentLoc.subtract(velocity); // Bloktan çıkar
                            bounceCount++;
                            
                            // Sekme efektleri
                            playSound(currentLoc, Sound.BLOCK_ANVIL_LAND, 0.5f, 2.0f);
                            currentLoc.getWorld().spawnParticle(
                                Particle.CRIT,
                                currentLoc,
                                10, 0.2, 0.2, 0.2, 0.1
                            );
                        } else {
                            dropWeapon(weaponStand, currentLoc);
                            isGrounded = true;
                        }
                    }

                    // Hasar kontrolü
                    if (!isGrounded) {
                        currentLoc.getWorld().getNearbyEntities(currentLoc, 1, 1, 1).forEach(entity -> {
                            if (entity instanceof LivingEntity && entity != caster && 
                                entity != weaponStand && !entity.isDead()) {
                                LivingEntity target = (LivingEntity) entity;
                                if (!target.isInvulnerable()) {
                                    target.damage(DAMAGE, caster);
                                    target.setVelocity(velocity.clone().multiply(0.3));
                                    createHitEffect(currentLoc);
                                }
                            }
                        });
                    }

                    // Parçacık efekti
                    if (Math.random() < 0.3) {
                        currentLoc.getWorld().spawnParticle(
                            Particle.CRIT_MAGIC,
                            currentLoc,
                            1, 0.1, 0.1, 0.1, 0.01
                        );
                    }
                } else {
                    // Yerden alma kontrolü
                    currentLoc.getWorld().getNearbyEntities(currentLoc, PICKUP_DISTANCE, PICKUP_DISTANCE, PICKUP_DISTANCE)
                        .stream()
                        .filter(entity -> entity instanceof Player)
                        .map(entity -> (Player) entity)
                        .filter(Player::isSneaking)
                        .findFirst()
                        .ifPresent(player -> {
                            giveWeaponToPlayer(player, weaponStand);
                            endSkill();
                            this.cancel();
                        });
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private Vector getNormalVector(Block block, Location hitLoc) {
        double x = hitLoc.getX() - block.getX();
        double y = hitLoc.getY() - block.getY();
        double z = hitLoc.getZ() - block.getZ();
        
        if (x < 0.3) return new Vector(1, 0, 0);
        if (x > 0.7) return new Vector(-1, 0, 0);
        if (y < 0.3) return new Vector(0, 1, 0);
        if (y > 0.7) return new Vector(0, -1, 0);
        if (z < 0.3) return new Vector(0, 0, 1);
        return new Vector(0, 0, -1);
    }

    private Vector calculateReflection(Vector velocity, Vector normal) {
        double dot = velocity.dot(normal);
        return velocity.subtract(normal.multiply(2 * dot));
    }

    private void dropWeapon(ArmorStand weaponStand, Location location) {
        if (weaponStand != null && weaponStand.isValid()) {
            weaponStand.setGravity(false);
            weaponStand.teleport(location.add(0, 0.5, 0));
            weaponStand.setHeadPose(new EulerAngle(Math.PI / 4, 0, 0));
            
            // Düşme efekti
            location.getWorld().spawnParticle(
                Particle.CLOUD,
                location,
                10, 0.2, 0.1, 0.2, 0.05
            );
            playSound(location, Sound.BLOCK_CHAIN_BREAK, 1.0f, 1.0f);
        }
    }

    private void giveWeaponToPlayer(Player player, ArmorStand weaponStand) {
        if (weaponStand != null && weaponStand.isValid()) {
            // Alma efektleri
            Location loc = weaponStand.getLocation();
            loc.getWorld().spawnParticle(
                Particle.CLOUD,
                loc,
                20, 0.2, 0.2, 0.2, 0.1
            );
            playSound(loc, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.0f, 1.0f);
            
            weaponStand.remove();
            player.sendMessage(ChatColor.GREEN + "» Zaman Kıran'ı yerden aldın!");
        }
    }

    private void createHitEffect(Location location) {
        location.getWorld().spawnParticle(
            Particle.EXPLOSION_NORMAL,
            location,
            10, 0.2, 0.2, 0.2, 0.1
        );
        playSound(location, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.0f);
    }
} 