package org.SolarSystem.zamanKiran.skills;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

public class ShadowInvasion extends Skill {
    private static final int DURATION = 10 * 20; // 10 saniye
    private static final int TENTACLE_COUNT = 6;
    private static final double TENTACLE_HEIGHT = 3.0;
    private static final double TENTACLE_RADIUS = 4.0;
    private static final int BLINDNESS_DURATION = 3 * 20; // 3 saniye
    
    private final Map<UUID, List<ArmorStand>> activeTentacles = new HashMap<>();
    private final Map<UUID, List<ItemStack>> stolenItems = new HashMap<>();
    private final Random random = new Random();

    public ShadowInvasion(Plugin plugin) {
        super(plugin, "Shadow Invasion", 40, 70);
    }

    @Override
    public void cast(Player caster) {
        startSkill();
        makeInvisible(caster);
        spawnTentacles(caster);
        
        // Başlangıç efektleri
        Location loc = caster.getLocation();
        playSound(loc, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        caster.sendMessage(ChatColor.DARK_GRAY + "» Ellerin gölgelere karışıyor...");
        
        // Gölge efektleri
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (!isActive || ticks >= DURATION) {
                    cleanup(caster);
                    this.cancel();
                    return;
                }
                
                // Gölge parçacıkları
                Location loc = caster.getLocation();
                for (int i = 0; i < 5; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double radius = random.nextDouble() * 2;
                    Location particleLoc = loc.clone().add(
                        Math.cos(angle) * radius,
                        random.nextDouble() * 2,
                        Math.sin(angle) * radius
                    );
                    
                    loc.getWorld().spawnParticle(
                        Particle.SMOKE_NORMAL,
                        particleLoc,
                        0, 0, 0, 0, 0
                    );
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void makeInvisible(Player caster) {
        caster.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, DURATION, 0));
    }

    private void spawnTentacles(Player caster) {
        List<ArmorStand> tentacles = new ArrayList<>();
        Location center = caster.getLocation();
        
        for (int i = 0; i < TENTACLE_COUNT; i++) {
            double angle = (2 * Math.PI * i) / TENTACLE_COUNT;
            Location spawnLoc = center.clone().add(
                Math.cos(angle) * TENTACLE_RADIUS,
                -2, // Yeraltından başla
                Math.sin(angle) * TENTACLE_RADIUS
            );
            
            // Her tentakül için 3 ArmorStand (alt, orta, üst)
            for (int j = 0; j < 3; j++) {
                ArmorStand stand = createTentaclePart(spawnLoc.clone().add(0, j * 0.5, 0));
                tentacles.add(stand);
            }
        }
        
        activeTentacles.put(caster.getUniqueId(), tentacles);
        
        // Tentakül animasyonu
        new BukkitRunnable() {
            double progress = 0;
            boolean rising = true;
            
            @Override
            public void run() {
                if (!isActive || progress >= 1.0) {
                    if (!rising) {
                        cleanup(caster);
                        this.cancel();
                        return;
                    }
                    rising = false;
                }
                
                List<ArmorStand> stands = activeTentacles.get(caster.getUniqueId());
                if (stands == null) return;
                
                for (int i = 0; i < stands.size(); i++) {
                    ArmorStand stand = stands.get(i);
                    if (!stand.isValid()) continue;
                    
                    int tentacleIndex = i / 3;
                    int partIndex = i % 3;
                    
                    double angle = (2 * Math.PI * tentacleIndex) / TENTACLE_COUNT;
                    double waveOffset = Math.sin(progress * Math.PI + partIndex * 0.5) * 0.5;
                    
                    Location newLoc = center.clone().add(
                        Math.cos(angle) * (TENTACLE_RADIUS + waveOffset),
                        (rising ? progress : (1 - progress)) * TENTACLE_HEIGHT + partIndex * 0.5,
                        Math.sin(angle) * (TENTACLE_RADIUS + waveOffset)
                    );
                    
                    stand.teleport(newLoc);
                    
                    // Yakındaki oyuncuları kontrol et
                    for (Entity entity : stand.getNearbyEntities(0.5, 0.5, 0.5)) {
                        if (entity instanceof Player && entity != caster) {
                            Player target = (Player) entity;
                            handleTentacleHit(target);
                        }
                    }
                }
                
                progress += 0.05;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private ArmorStand createTentaclePart(Location loc) {
        ArmorStand stand = loc.getWorld().spawn(loc, ArmorStand.class);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setSmall(true);
        stand.setHelmet(new ItemStack(Material.BLACK_WOOL));
        return stand;
    }

    private void handleTentacleHit(Player target) {
        // Körlük efekti
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, BLINDNESS_DURATION, 0));
        
        // Rastgele eşya çalma
        PlayerInventory inv = target.getInventory();
        int slot = random.nextInt(36); // 0-35 arası slot
        ItemStack item = inv.getItem(slot);
        
        if (item != null && !item.getType().isAir()) {
            UUID targetId = target.getUniqueId();
            stolenItems.computeIfAbsent(targetId, k -> new ArrayList<>()).add(item.clone());
            inv.setItem(slot, null);
            
            target.sendMessage(ChatColor.DARK_RED + "» Gölgeler eşyalarını çalıyor!");
            playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        }
    }

    private void cleanup(Player caster) {
        // Tentakülleri temizle
        List<ArmorStand> tentacles = activeTentacles.remove(caster.getUniqueId());
        if (tentacles != null) {
            tentacles.forEach(stand -> {
                if (stand.isValid()) {
                    Location loc = stand.getLocation();
                    stand.remove();
                    loc.getWorld().spawnParticle(
                        Particle.SMOKE_LARGE,
                        loc,
                        10, 0.2, 0.2, 0.2, 0.05
                    );
                }
            });
        }
        
        // Çalınan eşyaları geri ver
        stolenItems.forEach((targetId, items) -> {
            Player target = Bukkit.getPlayer(targetId);
            if (target != null && target.isOnline()) {
                items.forEach(item -> {
                    if (target.getInventory().firstEmpty() != -1) {
                        target.getInventory().addItem(item);
                        target.sendMessage(ChatColor.GREEN + "» Eşyaların gölgelerden kurtuldu!");
                    } else {
                        target.getWorld().dropItemNaturally(target.getLocation(), item);
                        target.sendMessage(ChatColor.YELLOW + "» Eşyaların yere düştü!");
                    }
                });
            }
        });
        stolenItems.clear();
        
        // Görünmezliği kaldır
        caster.removePotionEffect(PotionEffectType.INVISIBILITY);
    }
} 