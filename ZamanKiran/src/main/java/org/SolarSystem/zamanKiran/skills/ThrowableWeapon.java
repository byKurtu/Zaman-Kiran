package org.SolarSystem.zamanKiran.skills;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.EulerAngle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ThrowableWeapon {
    private static final double THROW_SPEED = 1.5;
    private static final double GRAVITY = -0.05;
    private static final double AIR_RESISTANCE = 0.02;
    private static final double ROTATION_SPEED = Math.PI / 8;
    private static final double DAMAGE = 15.0;
    private static final double MAX_DISTANCE = 30.0;
    private static final double PICKUP_DISTANCE = 2.0;
    private final Plugin plugin;
    private final Map<UUID, ArmorStand> droppedWeapons = new HashMap<>();
    private final Map<UUID, Location> weaponLocations = new HashMap<>();
    private final Map<UUID, Double> weaponSpeeds = new HashMap<>();
    private final Map<UUID, BukkitRunnable> flyTasks = new HashMap<>();

    public ThrowableWeapon(Plugin plugin) {
        this.plugin = plugin;
        startActionBarTask();
    }

    private void startActionBarTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    ItemStack mainHand = player.getInventory().getItemInMainHand();
                    if (mainHand != null && mainHand.getType() == Material.DIAMOND_PICKAXE) {

                        String direction = getDirectionArrow(player.getLocation().getDirection());
                        double speed = player.getVelocity().length();
                        String message = String.format("§cSilah: §aElde §8| §eHız: §6%.1f §8| §eYön: §6%s", 
                            speed, direction);
                        
                        player.spigot().sendMessage(
                            net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message)
                        );
                        
                        if (!player.getAllowFlight()) {
                            player.setAllowFlight(true);
                            player.setFlying(true);
                            startFlyTask(player);
                        }
                    } else {
                        if (player.getAllowFlight() && player.getGameMode() != GameMode.CREATIVE) {
                            player.setAllowFlight(false);
                            player.setFlying(false);
                            stopFlyTask(player);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void startFlyTask(Player player) {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.getInventory().getItemInMainHand().getType() != Material.DIAMOND_PICKAXE) {
                    if (player.isOnline() && player.getGameMode() != GameMode.CREATIVE) {
                        player.setAllowFlight(false);
                        player.setFlying(false);
                    }
                    stopFlyTask(player);
                    return;
                }
            }
        };
        task.runTaskTimer(plugin, 0L, 1L);
        flyTasks.put(player.getUniqueId(), task);
    }

    private void stopFlyTask(Player player) {
        BukkitRunnable task = flyTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    public void throwWeapon(Player player, ThrowMode mode) {
        Location start = player.getEyeLocation();
        Vector velocity = calculateInitialVelocity(player, mode);
        
        ArmorStand weaponStand = createWeaponStand(start, new ItemStack(Material.DIAMOND_PICKAXE));
        
        droppedWeapons.put(player.getUniqueId(), weaponStand);
        weaponLocations.put(player.getUniqueId(), start);
        weaponSpeeds.put(player.getUniqueId(), velocity.length());

        new BukkitRunnable() {
            private Vector currentVelocity = velocity.clone();
            private Location currentLocation = start.clone();
            private int ticks = 0;
            private boolean isGrounded = false;
            private final int MAX_TICKS = 200;

            @Override
            public void run() {
                if (ticks++ > MAX_TICKS || weaponStand.isDead()) {
                    cleanup(false);
                    return;
                }

                if (!isGrounded) {
                    applyPhysics();
                    
                    currentLocation.add(currentVelocity);
                    weaponStand.teleport(currentLocation);
                    
                    rotateWeapon(weaponStand, currentVelocity);
                    
                    updateActionBar(player, currentVelocity);
                    
                    if (currentLocation.getBlock().getType().isSolid()) {
                        isGrounded = true;
                        currentLocation.setY(Math.ceil(currentLocation.getY()));
                        weaponStand.teleport(currentLocation);
                        weaponLocations.put(player.getUniqueId(), currentLocation);
                        
                        createImpactEffect(currentLocation);
                    }
                } else {
                    for (Player nearbyPlayer : currentLocation.getWorld().getPlayers()) {
                        if (nearbyPlayer.getLocation().distance(currentLocation) <= PICKUP_DISTANCE 
                            && nearbyPlayer.isSneaking()) {
                            giveWeaponToPlayer(nearbyPlayer, weaponStand);
                            cleanup(true);
                            return;
                        }
                    }
                }
            }

            private void applyPhysics() {
                currentVelocity.setY(currentVelocity.getY() + GRAVITY);

                currentVelocity.multiply(1 - AIR_RESISTANCE);
                
                weaponSpeeds.put(player.getUniqueId(), currentVelocity.length());
            }

            private void cleanup(boolean wasPickedUp) {
                if (!wasPickedUp) {
                    weaponStand.remove();
                }
                droppedWeapons.remove(player.getUniqueId());
                weaponLocations.remove(player.getUniqueId());
                weaponSpeeds.remove(player.getUniqueId());
                this.cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private ArmorStand createWeaponStand(Location location, ItemStack weapon) {
        ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class);
        
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setSmall(true);
        stand.setMarker(true);
        stand.setCustomNameVisible(true);
        stand.setCustomName(ChatColor.GOLD + "✧ Zaman Kıran ✧");
        
        stand.setRightArmPose(new EulerAngle(Math.PI / 4, 0, 0));
        stand.getEquipment().setItemInMainHand(weapon);
        
        return stand;
    }

    private void rotateWeapon(ArmorStand stand, Vector velocity) {
        double pitch = Math.atan2(velocity.getY(), 
            Math.sqrt(velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ()));
        double yaw = Math.atan2(-velocity.getX(), velocity.getZ());
        
        stand.setRightArmPose(new EulerAngle(
            pitch + Math.PI / 4,
            yaw,
            0
        ));
    }

    private void updateActionBar(Player player, Vector velocity) {
        String direction = getDirectionArrow(velocity);
        String speed = String.format("%.1f", velocity.length());
        String message = String.format("§cSilah: §7Havada §8| §eHız: §6%s §8| §eYön: §6%s", 
            speed, direction);
        
        player.spigot().sendMessage(
            net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message)
        );
    }

    private String getDirectionArrow(Vector velocity) {
        double yaw = Math.atan2(-velocity.getX(), velocity.getZ());
        double degrees = Math.toDegrees(yaw);
        while (degrees < 0) degrees += 360;
        
        if (degrees >= 337.5 || degrees < 22.5) return "↑";
        if (degrees >= 22.5 && degrees < 67.5) return "↗";
        if (degrees >= 67.5 && degrees < 112.5) return "→";
        if (degrees >= 112.5 && degrees < 157.5) return "↘";
        if (degrees >= 157.5 && degrees < 202.5) return "↓";
        if (degrees >= 202.5 && degrees < 247.5) return "↙";
        if (degrees >= 247.5 && degrees < 292.5) return "←";
        return "↖";
    }

    private void createImpactEffect(Location location) {
        location.getWorld().spawnParticle(
            org.bukkit.Particle.EXPLOSION_NORMAL,
            location,
            10, 0.2, 0.2, 0.2, 0.1
        );
        location.getWorld().playSound(
            location,
            Sound.BLOCK_ANVIL_LAND,
            1.0f,
            1.5f
        );
    }

    private void giveWeaponToPlayer(Player player, ArmorStand weaponStand) {
        ItemStack weapon = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = weapon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "✧ Zaman Kıran ✧");
            weapon.setItemMeta(meta);
        }
        player.getInventory().addItem(weapon);
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.0f, 1.0f);
        player.spigot().sendMessage(
            net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
            net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§aZaman Kıran silahını yerden aldın!")
        );
    }

    private Vector calculateInitialVelocity(Player player, ThrowMode mode) {
        Vector direction = player.getLocation().getDirection();
        
        switch (mode) {
            case AUTO:
                Player target = findNearestPlayer(player, 30);
                if (target != null) {
                    direction = target.getLocation().toVector()
                        .subtract(player.getLocation().toVector())
                        .normalize();
                }
                break;
            case CALCULATIVE:
                showPredictedPath(player, direction);
                break;
            case DIRECTIVE:
            default:
                break;
        }

        return direction.multiply(THROW_SPEED);
    }

    private Player findNearestPlayer(Player thrower, double maxDistance) {
        Player nearest = null;
        double nearestDistance = maxDistance;

        for (Player target : thrower.getWorld().getPlayers()) {
            if (target != thrower) {
                double distance = target.getLocation().distance(thrower.getLocation());
                if (distance < nearestDistance) {
                    nearest = target;
                    nearestDistance = distance;
                }
            }
        }

        return nearest;
    }

    private void showPredictedPath(Player player, Vector direction) {
        Location start = player.getEyeLocation();
        Vector velocity = direction.multiply(THROW_SPEED);
        
        for (int i = 0; i < 20; i++) {
            Location point = start.clone().add(
                velocity.getX() * i,
                velocity.getY() * i + 0.5 * GRAVITY * i * i,
                velocity.getZ() * i
            );
            player.spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, 
                point, 1, 0, 0, 0, 0);
        }
    }

    public enum ThrowMode {
        AUTO,
        DIRECTIVE,
        CALCULATIVE
    }
} 