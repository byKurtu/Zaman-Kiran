package org.SolarSystem.zamanKiran.skills;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class DimensionGate extends Skill {
    private static final double RADIUS = 10.0;
    private static final int DURATION = 60 * 20; // 60 seconds
    private static final Sound[] SCARY_SOUNDS = {
        Sound.ENTITY_ENDERMAN_SCREAM,
        Sound.ENTITY_ENDER_DRAGON_GROWL,
        Sound.ENTITY_VEX_DEATH,
        Sound.ENTITY_ELDER_GUARDIAN_CURSE,
        Sound.AMBIENT_CAVE
    };
    
    private final Map<UUID, Location> originalLocations = new HashMap<>();
    private final Map<UUID, BukkitRunnable> returnTasks = new HashMap<>();
    private final Random random = new Random();
    private final Set<UUID> inShadowRealm = new HashSet<>();

    public DimensionGate(Plugin plugin) {
        super(plugin, "Boyut Kapısı", 120, 100);
    }

    @Override
    public void cast(Player caster) {
        Location center = caster.getLocation();
        Collection<Entity> nearbyEntities = center.getWorld().getNearbyEntities(center, RADIUS, RADIUS, RADIUS);
        
        if (inShadowRealm.contains(caster.getUniqueId())) {
            // Return from shadow realm
            returnFromShadowRealm(caster);
            for (Entity entity : nearbyEntities) {
                if (entity instanceof Player && inShadowRealm.contains(entity.getUniqueId())) {
                    returnFromShadowRealm((Player) entity);
                }
            }
        } else {
            // Send to shadow realm
            sendToShadowRealm(caster);
            for (Entity entity : nearbyEntities) {
                if (entity instanceof Player && entity != caster) {
                    sendToShadowRealm((Player) entity);
                }
            }
        }

        // Play random scary sound
        center.getWorld().playSound(center, SCARY_SOUNDS[random.nextInt(SCARY_SOUNDS.length)], 2f, 0.6f);
    }

    private void sendToShadowRealm(Player player) {
        UUID playerId = player.getUniqueId();
        Location original = player.getLocation();
        originalLocations.put(playerId, original);
        inShadowRealm.add(playerId);

        // Visual effects
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 500);
        player.getWorld().spawnParticle(Particle.SMOKE_LARGE, player.getLocation(), 100);
        
        // Apply effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 5));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 3));
        
        // Send title
        player.sendTitle(
            ChatColor.DARK_RED + "BURASI... CANLI!",
            ChatColor.GRAY + "Alt boyuta hoş geldin...",
            10, 40, 10
        );

        // Create ghost silhouettes
        createSilhouette(player);

        // Schedule automatic return
        BukkitRunnable returnTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (inShadowRealm.contains(playerId)) {
                    returnFromShadowRealm(player);
                }
            }
        };
        returnTask.runTaskLater(plugin, DURATION);
        returnTasks.put(playerId, returnTask);
    }

    private void returnFromShadowRealm(Player player) {
        UUID playerId = player.getUniqueId();
        inShadowRealm.remove(playerId);
        
        // Visual effects
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getLocation(), 300);
        
        // Remove effects
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.SLOW);
        
        // Apply return penalties
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 5 * 20, 0));
        player.setFoodLevel(Math.max(0, player.getFoodLevel() - 3));
        
        // Send message
        player.sendMessage(ChatColor.RED + "Ruhum orada kaldı...");
        
        // Cancel return task if exists
        BukkitRunnable returnTask = returnTasks.remove(playerId);
        if (returnTask != null) {
            returnTask.cancel();
        }
        
        // Play return sound
        player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1f, 1.5f);
    }

    private void createSilhouette(Player player) {
        Location loc = player.getLocation();
        ArmorStand ghost = loc.getWorld().spawn(loc, ArmorStand.class);
        ghost.setInvisible(true);
        ghost.setHelmet(new ItemStack(Material.BLACK_STAINED_GLASS));
        ghost.setGravity(false);
        
        new BukkitRunnable() {
            double angle = 0;
            int ticks = 0;
            
            @Override
            public void run() {
                if (!inShadowRealm.contains(player.getUniqueId()) || ticks++ > 200) {
                    ghost.remove();
                    this.cancel();
                    return;
                }
                
                angle += Math.PI / 16;
                double x = Math.cos(angle) * 2;
                double z = Math.sin(angle) * 2;
                Location newLoc = player.getLocation().add(x, 0.5, z);
                ghost.teleport(newLoc);
                
                player.getWorld().spawnParticle(
                    Particle.SOUL_FIRE_FLAME,
                    newLoc,
                    1, 0.1, 0.1, 0.1, 0.01
                );
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public boolean isInShadowRealm(Player player) {
        return inShadowRealm.contains(player.getUniqueId());
    }
} 