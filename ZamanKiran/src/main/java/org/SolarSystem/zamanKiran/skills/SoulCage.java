package org.SolarSystem.zamanKiran.skills;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.block.data.BlockData;

import java.util.*;

public class SoulCage extends Skill {
    private static final int CAGE_SIZE = 3;
    private static final int DURATION = 15 * 20; // 15 saniye
    private static final int BLOCKS_TO_BREAK = 3;
    private static final double DAMAGE_PER_TICK = 1.0;
    private final Map<UUID, Set<Location>> cageBlocks = new HashMap<>();
    private final Map<UUID, Integer> brokenBlocks = new HashMap<>();
    private final Random random = new Random();

    public SoulCage(Plugin plugin) {
        super(plugin, "Soul Cage", 45, 80);
    }

    @Override
    public void cast(Player caster) {
        Player target = findNearestTarget(caster);
        if (target == null) {
            caster.sendMessage(ChatColor.RED + "» Yakında hedef bulunamadı!");
            return;
        }

        startSkill();
        createCage(target);
        startTormentEffect(target);
        
        // Başlangıç efektleri
        Location loc = target.getLocation();
        target.getWorld().strikeLightningEffect(loc);
        target.sendMessage(ChatColor.DARK_RED + "» Ruhlar seni yiyor... Kaçamazsın!");
        playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, 0.5f);
    }

    private void createCage(Player target) {
        Location center = target.getLocation();
        Set<Location> blocks = new HashSet<>();
        
        // Kafesi oluştur
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0 && y != 2) continue; // İç kısmı boş bırak
                    
                    Location loc = center.clone().add(x, y, z);
                    Block block = loc.getBlock();
                    
                    if (y == 2 || y == 0 || Math.abs(x) == 1 || Math.abs(z) == 1) {
                        if (random.nextBoolean()) {
                            block.setType(Material.BONE_BLOCK);
                        } else {
                            block.setType(Material.IRON_BARS);
                        }
                        blocks.add(loc);
                        
                        // Kemik parçacıkları
                        loc.getWorld().spawnParticle(
                            Particle.BLOCK_CRACK,
                            loc.clone().add(0.5, 0.5, 0.5),
                            10, 0.2, 0.2, 0.2, 0,
                            Material.BONE_BLOCK.createBlockData()
                        );
                    }
                }
            }
        }
        
        cageBlocks.put(target.getUniqueId(), blocks);
        brokenBlocks.put(target.getUniqueId(), 0);
    }

    private void startTormentEffect(Player target) {
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (!isActive || ticks >= DURATION || !target.isValid() || 
                    brokenBlocks.get(target.getUniqueId()) >= BLOCKS_TO_BREAK) {
                    cleanup(target);
                    this.cancel();
                    return;
                }

                // Her 2 saniyede bir efektler
                if (ticks % 40 == 0) {
                    Location loc = target.getLocation();
                    
                    // Kan parçacıkları
                    loc.getWorld().spawnParticle(
                        Particle.BLOCK_CRACK,
                        loc.clone().add(0, 1, 0),
                        20, 0.5, 1, 0.5, 0,
                        Material.REDSTONE_BLOCK.createBlockData()
                    );
                    
                    // Korkunç sesler
                    playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, random.nextFloat() * 0.5f + 0.5f);
                    
                    // Açlık efekti
                    target.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 60, 1));
                    target.damage(DAMAGE_PER_TICK);
                    
                    // Rastgele mesajlar
                    if (random.nextInt(3) == 0) {
                        String[] messages = {
                            "§4Ruhlar açlıklarını senin canınla gideriyor...",
                            "§4Kemiklerin çürüyor...",
                            "§4Kaçış yok... Sadece ızdırap var..."
                        };
                        target.sendMessage(messages[random.nextInt(messages.length)]);
                    }
                }
                
                // Ruh parçacıkları
                Location loc = target.getLocation();
                for (int i = 0; i < 3; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double radius = random.nextDouble() * 2;
                    Location particleLoc = loc.clone().add(
                        Math.cos(angle) * radius,
                        random.nextDouble() * 2,
                        Math.sin(angle) * radius
                    );
                    
                    loc.getWorld().spawnParticle(
                        Particle.SOUL,
                        particleLoc,
                        1, 0, 0, 0, 0
                    );
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void cleanup(Player target) {
        Set<Location> blocks = cageBlocks.remove(target.getUniqueId());
        if (blocks != null) {
            blocks.forEach(loc -> {
                loc.getBlock().setType(Material.AIR);
                loc.getWorld().spawnParticle(
                    Particle.SOUL,
                    loc.clone().add(0.5, 0.5, 0.5),
                    10, 0.2, 0.2, 0.2, 0.1
                );
            });
        }
        
        brokenBlocks.remove(target.getUniqueId());
        target.sendMessage(ChatColor.DARK_PURPLE + "» Ruhlar... şimdilik seni bıraktı.");
        
        Location loc = target.getLocation();
        playSound(loc, Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);
        loc.getWorld().spawnParticle(
            Particle.EXPLOSION_HUGE,
            loc,
            3, 1, 1, 1, 0
        );
    }

    private Player findNearestTarget(Player caster) {
        double nearestDistance = Double.MAX_VALUE;
        Player nearestPlayer = null;
        
        for (Entity entity : caster.getNearbyEntities(10, 10, 10)) {
            if (entity instanceof Player && entity != caster) {
                double distance = caster.getLocation().distance(entity.getLocation());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestPlayer = (Player) entity;
                }
            }
        }
        
        return nearestPlayer;
    }

    public void handleBlockBreak(Player player, Block block) {
        UUID playerId = player.getUniqueId();
        if (cageBlocks.containsKey(playerId) && cageBlocks.get(playerId).contains(block.getLocation())) {
            int broken = brokenBlocks.get(playerId) + 1;
            brokenBlocks.put(playerId, broken);
            
            if (broken >= BLOCKS_TO_BREAK) {
                cleanup(player);
            } else {
                player.sendMessage(ChatColor.GOLD + "» Kafesten çıkmak için " + 
                    (BLOCKS_TO_BREAK - broken) + " blok daha kırmalısın!");
            }
        }
    }
} 