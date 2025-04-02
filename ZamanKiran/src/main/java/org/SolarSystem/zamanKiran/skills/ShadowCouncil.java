package org.SolarSystem.zamanKiran.skills;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ShadowCouncil extends Skill {
    private static final int DURATION = 30 * 20; // 30 seconds
    private static final double MELEE_DAMAGE = 10.0;
    private static final double EXPLOSION_DAMAGE = 15.0;
    private static final double FORMATION_RADIUS = 3.0;
    private static final double ATTACK_RANGE = 15.0;

    private final Map<String, ArmorStand> npcs = new ConcurrentHashMap<>();
    private final Map<String, Location> targetLocations = new ConcurrentHashMap<>();
    private int mainTaskId = -1;
    private boolean isActive = false;

    public ShadowCouncil(Plugin plugin) {
        super(plugin, "Shadow Council", 60, 100);
    }

    @Override
    public void cast(Player caster) {
        if (!canCast(caster)) {
            caster.sendMessage(ChatColor.RED + "» Yetenek kullanılamıyor!");
            return;
        }

        caster.sendMessage(ChatColor.GREEN + "» Shadow Council yeteneği başlatılıyor...");
        
        startSkill();
        isActive = true;
        Location center = caster.getLocation();
        
        caster.sendMessage(ChatColor.YELLOW + "» NPC'ler oluşturuluyor...");
        spawnNPCs(center);
        
        if (npcs.isEmpty()) {
            caster.sendMessage(ChatColor.RED + "» NPC'ler oluşturulamadı!");
            return;
        }
        
        caster.sendMessage(ChatColor.GREEN + "» Ana görev başlatılıyor...");
        startMainTask(caster);
        
        // Başlangıç efektleri
        center.getWorld().strikeLightningEffect(center);
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
    }

    private void spawnNPCs(Location center) {
        Bukkit.broadcastMessage(ChatColor.DARK_PURPLE + "» Gölge Konseyi çağırılıyor...");
        
        // NPC'leri oluştur
        double angle = 0;
        Location nullLoc = center.clone().add(
            Math.cos(angle) * FORMATION_RADIUS,
            0,
            Math.sin(angle) * FORMATION_RADIUS
        );
        spawnNPC("Null", nullLoc, Material.SKELETON_SKULL);

        angle += (2 * Math.PI / 3);
        Location entity303Loc = center.clone().add(
            Math.cos(angle) * FORMATION_RADIUS,
            0,
            Math.sin(angle) * FORMATION_RADIUS
        );
        spawnNPC("Entity303", entity303Loc, Material.PLAYER_HEAD);

        angle += (2 * Math.PI / 3);
        Location herobrineLoc = center.clone().add(
            Math.cos(angle) * FORMATION_RADIUS,
            0,
            Math.sin(angle) * FORMATION_RADIUS
        );
        spawnNPC("Herobrine", herobrineLoc, Material.PLAYER_HEAD);

        // Efektler
        center.getWorld().strikeLightningEffect(center);
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        
        for (int i = 0; i < 3; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    center.getWorld().strikeLightningEffect(center);
                }
            }.runTaskLater(plugin, i * 10L);
        }
    }

    private void spawnNPC(String name, Location loc, Material headMaterial) {
        // ArmorStand oluştur
        ArmorStand stand = loc.getWorld().spawn(loc, ArmorStand.class);
        stand.setCustomName(ChatColor.DARK_RED + "❖ " + name + " ❖");
        stand.setCustomNameVisible(true);
        stand.setVisible(true);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setSmall(false);
        stand.setArms(true);
        stand.setBasePlate(false);
        
        // Ekipmanları ayarla
        stand.getEquipment().setHelmet(new ItemStack(headMaterial));
        stand.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
        stand.getEquipment().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
        stand.getEquipment().setBoots(new ItemStack(Material.NETHERITE_BOOTS));
        
        if (name.equals("Null")) {
            stand.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
        } else if (name.equals("Entity303")) {
            stand.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
        } else {
            stand.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
        }

        // Efektler
        loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 50, 0.5, 1, 0.5, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SKELETON_AMBIENT, 1.0f, 0.5f);
        
        npcs.put(name, stand);
        targetLocations.put(name, loc);
        
        // Sürekli efektler
        new BukkitRunnable() {
            double angle = 0;
            @Override
            public void run() {
                if (!stand.isValid() || !isActive) {
                    this.cancel();
                    return;
                }
                
                Location particleLoc = stand.getLocation().add(0, 1, 0);
                particleLoc.getWorld().spawnParticle(
                    name.equals("Null") ? Particle.SOUL : 
                    name.equals("Entity303") ? Particle.CRIMSON_SPORE : 
                    Particle.WARPED_SPORE,
                    particleLoc,
                    3, 0.2, 0.4, 0.2, 0
                );
                
                // Hover efekti
                stand.teleport(stand.getLocation().add(0, Math.sin(angle) * 0.05, 0));
                angle += 0.1;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void startMainTask(Player caster) {
        try {
            caster.sendMessage(ChatColor.GREEN + "» Ana görev başlatılıyor...");
            isActive = true;
            
            new BukkitRunnable() {
                int ticks = 0;
                double angle = 0;
                
                @Override
                public void run() {
                    if (!isActive || ticks >= DURATION || !caster.isValid()) {
                        cleanup();
                        this.cancel();
                        caster.sendMessage(ChatColor.RED + "» Ana görev sonlandırıldı!");
                        return;
                    }

                    // Update NPC positions
                    updateFormation(caster.getLocation(), angle);
                    
                    // Spawn ambient particles
                    spawnAmbientParticles();
                    
                    // Play ambient sounds
                    if (ticks % 40 == 0) {
                        playSound(caster.getLocation(), Sound.AMBIENT_CAVE, 1.0f, 0.5f);
                    }
                    
                    angle += Math.PI / 30;
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
            
            caster.sendMessage(ChatColor.GREEN + "» Ana görev başarıyla başlatıldı!");
        } catch (Exception e) {
            caster.sendMessage(ChatColor.RED + "» Ana görev başlatılırken hata: " + e.getMessage());
            cleanup();
        }
    }

    private void updateFormation(Location center, double angle) {
        // Update Null position
        Location nullLoc = center.clone().add(
            Math.cos(angle) * FORMATION_RADIUS,
            Math.sin(angle * 0.5) + 1,
            Math.sin(angle) * FORMATION_RADIUS
        );
        moveNPC("Null", nullLoc);
        
        // Update Entity303 position
        Location entity303Loc = center.clone().add(
            Math.cos(angle + (2 * Math.PI / 3)) * FORMATION_RADIUS,
            Math.sin(angle * 0.5) + 1.5,
            Math.sin(angle + (2 * Math.PI / 3)) * FORMATION_RADIUS
        );
        moveNPC("Entity303", entity303Loc);
        
        // Update Herobrine position
        Location herobrineLoc = center.clone().add(
            Math.cos(angle + (4 * Math.PI / 3)) * FORMATION_RADIUS,
            Math.sin(angle * 0.5) + 1,
            Math.sin(angle + (4 * Math.PI / 3)) * FORMATION_RADIUS
        );
        moveNPC("Herobrine", herobrineLoc);
    }

    private void moveNPC(String name, Location newLoc) {
        ArmorStand npc = npcs.get(name);
        if (npc != null && npc.isValid()) {
            // Create trail effect
            Location oldLoc = npc.getLocation();
            createTrailEffect(oldLoc, newLoc);
            
            // Update position
            npc.teleport(newLoc);
            targetLocations.put(name, newLoc);
        }
    }

    private void createTrailEffect(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize();
        
        for (double d = 0; d < distance; d += 0.5) {
            Location loc = from.clone().add(direction.clone().multiply(d));
            from.getWorld().spawnParticle(
                Particle.SPELL_WITCH,
                loc,
                1, 0.1, 0.1, 0.1, 0
            );
        }
    }

    private void spawnAmbientParticles() {
        npcs.values().forEach(npc -> {
            if (npc != null && npc.isValid()) {
                Location loc = npc.getLocation();
                switch (npc.getCustomName()) {
                    case "Null":
                        loc.getWorld().spawnParticle(
                            Particle.SMOKE_NORMAL,
                            loc,
                            5, 0.2, 0.4, 0.2, 0.02
                        );
                        break;
                    case "Entity303":
                        loc.getWorld().spawnParticle(
                            Particle.REDSTONE,
                            loc,
                            5, 0.2, 0.4, 0.2, 0
                        );
                        break;
                    case "Herobrine":
                        loc.getWorld().spawnParticle(
                            Particle.END_ROD,
                            loc,
                            5, 0.2, 0.4, 0.2, 0.02
                        );
                        break;
                }
            }
        });
    }

    private void cleanup() {
        try {
            Bukkit.broadcastMessage(ChatColor.YELLOW + "» Gölge Konseyi temizleniyor...");
            
            npcs.values().forEach(npc -> {
                if (npc != null && npc.isValid()) {
                    Location loc = npc.getLocation();
                    loc.getWorld().spawnParticle(
                        Particle.EXPLOSION_HUGE,
                        loc,
                        1, 0, 0, 0, 0
                    );
                    playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
                    npc.remove();
                }
            });
            
            npcs.clear();
            targetLocations.clear();
            isActive = false;
            
            Bukkit.broadcastMessage(ChatColor.GREEN + "» Gölge Konseyi başarıyla temizlendi!");
        } catch (Exception e) {
            Bukkit.broadcastMessage(ChatColor.RED + "» Gölge Konseyi temizlenirken hata: " + e.getMessage());
        }
    }

    private boolean canCast(Player player) {
        if (isActive) {
            player.sendMessage(ChatColor.RED + "» Gölge Konseyi zaten aktif!");
            return false;
        }
        return true;
    }

    public void handleLeftClick(Player player) {
        if (!canCast(player)) return;
        startSkill();

        // Sıralı saldırı animasyonu
        new BukkitRunnable() {
            int phase = 0;
            final String[] sequence = {"Null", "Entity303", "Herobrine"};

            @Override
            public void run() {
                if (phase >= sequence.length || !isActive) {
                    this.cancel();
                    return;
                }

                String npcName = sequence[phase];
                ArmorStand npc = npcs.get(npcName);
                if (npc != null && npc.isValid()) {
                    Location npcLoc = npc.getLocation();
                    Vector direction = player.getLocation().subtract(npcLoc).toVector().normalize();
                    
                    // Saldırı animasyonu
                    npcLoc.getWorld().spawnParticle(
                        Particle.SWEEP_ATTACK,
                        npcLoc.add(0, 1, 0),
                        10, 0.5, 0.5, 0.5, 0
                    );
                    
                    // Ses efekti
                    npcLoc.getWorld().playSound(
                        npcLoc,
                        Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                        1.0f,
                        1.0f
                    );
                    
                    // Yakındaki oyunculara hasar ver
                    npcLoc.getWorld().getNearbyEntities(npcLoc, 2, 2, 2).forEach(entity -> {
                        if (entity instanceof LivingEntity && entity != player) {
                            ((LivingEntity) entity).damage(MELEE_DAMAGE, player);
                        }
                    });
                }
                phase++;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    public void handleRightClick(Player player) {
        if (!canCast(player)) return;
        startSkill();

        // Uzaktan saldırı
        for (String npcName : npcs.keySet()) {
            ArmorStand npc = npcs.get(npcName);
            if (npc != null && npc.isValid()) {
                Location start = npc.getLocation().add(0, 1, 0);
                Vector direction = player.getLocation().getDirection();

                new BukkitRunnable() {
                    Location currentLoc = start.clone();
                    int ticks = 0;

                    @Override
                    public void run() {
                        if (ticks++ > 40 || !isActive) {
                            this.cancel();
                            return;
                        }

                        // Parçacık efekti
                        currentLoc.getWorld().spawnParticle(
                            Particle.SOUL_FIRE_FLAME,
                            currentLoc,
                            5, 0.1, 0.1, 0.1, 0.05
                        );

                        // Ses efekti
                        if (ticks % 5 == 0) {
                            currentLoc.getWorld().playSound(
                                currentLoc,
                                Sound.ENTITY_BLAZE_SHOOT,
                                0.5f,
                                1.5f
                            );
                        }

                        // Hasar kontrolü
                        currentLoc.getWorld().getNearbyEntities(currentLoc, 1, 1, 1).forEach(entity -> {
                            if (entity instanceof LivingEntity && entity != player && entity != npc) {
                                ((LivingEntity) entity).damage(EXPLOSION_DAMAGE, player);
                                createExplosion(currentLoc);
                                this.cancel();
                            }
                        });

                        currentLoc.add(direction.multiply(0.5));
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
        }
    }

    public void handleShiftLeftClick(Player player) {
        if (!canCast(player)) return;
        startSkill();

        // Özel saldırı efekti
        Location center = player.getLocation();
        createExplosion(center);
        
        // Yakındaki düşmanlara hasar ver
        center.getWorld().getNearbyEntities(center, 5, 5, 5).forEach(entity -> {
            if (entity instanceof LivingEntity && entity != player) {
                ((LivingEntity) entity).damage(EXPLOSION_DAMAGE * 1.5, player);
                Vector knockback = entity.getLocation().subtract(center).toVector().normalize().multiply(2);
                entity.setVelocity(knockback);
            }
        });
    }

    private void createExplosion(Location location) {
        // Visual explosion
        location.getWorld().spawnParticle(
            Particle.EXPLOSION_HUGE,
            location,
            1, 0, 0, 0, 0
        );
        
        // Damage nearby entities
        for (Entity entity : location.getWorld().getNearbyEntities(location, 2.5, 2.5, 2.5)) {
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).damage(EXPLOSION_DAMAGE);
                Vector knockback = entity.getLocation().toVector()
                    .subtract(location.toVector())
                    .normalize()
                    .multiply(1.5);
                entity.setVelocity(knockback);
            }
        }
        
        // Sound and particles
        playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
        location.getWorld().spawnParticle(
            Particle.FLAME,
            location,
            50, 1, 1, 1, 0.1
        );
    }

    private void createBloodEffect(Location location) {
        location.getWorld().spawnParticle(
            Particle.BLOCK_CRACK,
            location.add(0, 1, 0),
            50, 0.5, 0.5, 0.5, 0.1,
            Material.REDSTONE_BLOCK.createBlockData()
        );
    }
} 