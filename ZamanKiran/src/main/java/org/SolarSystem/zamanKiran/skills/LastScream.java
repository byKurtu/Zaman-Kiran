package org.SolarSystem.zamanKiran.skills;

import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.util.EulerAngle;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.*;

public class LastScream extends Skill implements Listener {
    private final List<ArmorStand> effectEntities = new ArrayList<>();
    private BossBar apocalypseBossBar;
    private boolean isApocalypseActive = false;
    private static final double EFFECT_RADIUS = 30.0;
    private static final Sound[] HORROR_SOUNDS = {
        Sound.AMBIENT_CAVE,
        Sound.ENTITY_ENDER_DRAGON_GROWL,
        Sound.ENTITY_WITHER_SPAWN,
        Sound.ENTITY_ELDER_GUARDIAN_CURSE,
        Sound.ENTITY_GHAST_SCREAM,
        Sound.AMBIENT_NETHER_WASTES_MOOD,
        Sound.BLOCK_END_PORTAL_SPAWN,
        Sound.ENTITY_LIGHTNING_BOLT_THUNDER
    };

    public LastScream(Plugin plugin) {
        super(plugin, "Kurtu'nun Son Çığlığı", 0, 0);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        apocalypseBossBar = Bukkit.createBossBar(
            "§4☠ KURTU'NUN ÖFKE DOLU SON ÇIĞLIĞI ☠",
            BarColor.RED,
            BarStyle.SEGMENTED_20
        );
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player.getName().equals("ByKurtu") && !isApocalypseActive) {
            event.setCancelled(true);
            startApocalypse(player);
        }
    }

    private void startApocalypse(Player player) {
        if (isApocalypseActive) return;
        
        isApocalypseActive = true;
        Location loc = player.getLocation();
        World world = loc.getWorld();

        // Vanish effect
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(player)) {
                p.hidePlayer(plugin, player);
            }
        }

        // Initial effects
        player.playEffect(EntityEffect.TOTEM_RESURRECT);
        world.strikeLightningEffect(loc);
        
        // Epic title sequence
        String[] titles = {
            "§4ÖLÜYORUM?!",
            "§4HER ŞEY BİTTİ...",
            "§4KURTU'NUN SON ÇIĞLIĞI!"
        };
        
        for (int i = 0; i < titles.length; i++) {
            final int index = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.sendTitle(titles[index], "", 10, 30, 10);
                    playRandomHorrorSound(loc);
                }
            }.runTaskLater(plugin, i * 40L);
        }

        // Show boss bar to all players
        apocalypseBossBar.setProgress(1.0);
        Bukkit.getOnlinePlayers().forEach(p -> {
            apocalypseBossBar.addPlayer(p);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 0.5f);
        });

        // Start the apocalypse sequence
        new BukkitRunnable() {
            int stage = 0;
            int countdown = 5;

            @Override
            public void run() {
                if (!isApocalypseActive) {
                    this.cancel();
                    return;
                }

                switch (stage) {
                    case 0: // Blood Lightning stage
                        createBloodLightning(loc);
                        break;
                    case 1: // Blood clouds and spirits
                        createBloodCloudsAndSpirits(loc);
                        break;
                    case 2: // Nuclear explosion and final scene
                        createEpicExplosion(loc);
                        this.cancel();
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                finalizeApocalypse(player);
                            }
                        }.runTaskLater(plugin, 100L);
                        return;
                }

                // Update boss bar and action bar
                apocalypseBossBar.setProgress(1.0 - (stage / 2.0));
                String actionBar = "§c§lKıyametin Kopmasına: §4" + countdown + "...";
                Bukkit.getOnlinePlayers().forEach(p -> {
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                        TextComponent.fromLegacyText(actionBar));
                    if (countdown <= 3) {
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    }
                });

                countdown--;
                if (countdown < 0) {
                    countdown = 5;
                    stage++;
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void createBloodLightning(Location center) {
        for (int i = 0; i < 8; i++) {
            final int index = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    double angle = (Math.PI * 2 * index) / 8;
                    Location lightningLoc = center.clone().add(
                        Math.cos(angle) * EFFECT_RADIUS,
                        0,
                        Math.sin(angle) * EFFECT_RADIUS
                    );

                    // Create red lightning effect using armor stands
                    ArmorStand lightning = center.getWorld().spawn(lightningLoc, ArmorStand.class);
                    lightning.setGravity(false);
                    lightning.setVisible(false);
                    lightning.setBasePlate(false);
                    lightning.setInvulnerable(true);
                    
                    ItemStack redGlass = new ItemStack(Material.RED_STAINED_GLASS);
                    lightning.setHelmet(redGlass);
                    
                    effectEntities.add(lightning);

                    // Animate the lightning
                    new BukkitRunnable() {
                        double height = 0;
                        int ticks = 0;
                        
                        @Override
                        public void run() {
                            if (ticks++ > 20 || !isApocalypseActive) {
                                lightning.remove();
                                this.cancel();
                                return;
                            }

                            height += 0.5;
                            Location newLoc = lightningLoc.clone().add(0, height, 0);
                            lightning.teleport(newLoc);
                            
                            // Add particle effects
                            center.getWorld().spawnParticle(
                                Particle.REDSTONE, 
                                newLoc,
                                10,
                                0.2, 0.2, 0.2,
                                0,
                                new Particle.DustOptions(Color.RED, 2)
                            );
                        }
                    }.runTaskTimer(plugin, 0L, 1L);

                    // Play thunder sound
                    playSound(lightningLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2f, 0.5f);
                }
            }.runTaskLater(plugin, i * 5L);
        }
    }

    private void createBloodCloudsAndSpirits(Location center) {
        // Create massive blood cloud circle
        for (int i = 0; i < 36; i++) {
            double angle = (Math.PI * 2 * i) / 36;
            Location cloudLoc = center.clone().add(
                Math.cos(angle) * EFFECT_RADIUS,
                10,
                Math.sin(angle) * EFFECT_RADIUS
            );

            ArmorStand cloud = center.getWorld().spawn(cloudLoc, ArmorStand.class);
            cloud.setGravity(false);
            cloud.setVisible(false);
            cloud.setSmall(false);
            cloud.setHelmet(new ItemStack(Material.RED_STAINED_GLASS));
            effectEntities.add(cloud);

            // Animate cloud
            new BukkitRunnable() {
                double localAngle = angle;
                @Override
                public void run() {
                    if (!isApocalypseActive) {
                        cloud.remove();
                        this.cancel();
                        return;
                    }

                    localAngle += 0.05;
                    double x = Math.cos(localAngle) * EFFECT_RADIUS;
                    double z = Math.sin(localAngle) * EFFECT_RADIUS;
                    double y = 10 + Math.sin(localAngle * 2) * 2;
                    
                    Location newLoc = center.clone().add(x, y, z);
                    cloud.teleport(newLoc);

                    // Add particle effects
                    center.getWorld().spawnParticle(
                        Particle.CRIMSON_SPORE,
                        newLoc,
                        5,
                        0.5, 0.5, 0.5,
                        0
                    );
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }

        // Create flying spirits
        for (int i = 0; i < 12; i++) {
            Location spiritLoc = center.clone().add(
                Math.random() * EFFECT_RADIUS * 2 - EFFECT_RADIUS,
                5,
                Math.random() * EFFECT_RADIUS * 2 - EFFECT_RADIUS
            );

            ArmorStand spirit = center.getWorld().spawn(spiritLoc, ArmorStand.class);
            spirit.setGravity(false);
            spirit.setVisible(false);
            spirit.setHelmet(new ItemStack(Material.SKELETON_SKULL));
            effectEntities.add(spirit);

            // Animate spirit
            new BukkitRunnable() {
                double time = Math.random() * Math.PI * 2;
                @Override
                public void run() {
                    if (!isApocalypseActive) {
                        spirit.remove();
                        this.cancel();
                        return;
                    }

                    time += 0.1;
                    double x = Math.sin(time) * 5;
                    double y = Math.sin(time * 2) * 2 + 5;
                    double z = Math.cos(time) * 5;

                    Location newLoc = spiritLoc.clone().add(x, y, z);
                    spirit.teleport(newLoc);
                    
                    // Rotate the skull
                    spirit.setHeadPose(new EulerAngle(time * 0.5, time * 0.3, 0));

                    // Add particle effects
                    center.getWorld().spawnParticle(
                        Particle.SOUL,
                        newLoc,
                        2,
                        0.2, 0.2, 0.2,
                        0.02
                    );
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }

    private void createEpicExplosion(Location center) {
        // Create massive mushroom cloud using armor stands
        for (int layer = 0; layer < 3; layer++) {
            final int finalLayer = layer;
            new BukkitRunnable() {
                @Override
                public void run() {
                    double radius = EFFECT_RADIUS * (1 - finalLayer * 0.2);
                    int points = 36;
                    
                    // Create the main cloud layer
                    for (int i = 0; i < points; i++) {
                        double angle = (Math.PI * 2 * i) / points;
                        Location cloudLoc = center.clone().add(
                            Math.cos(angle) * radius,
                            finalLayer * 10,
                            Math.sin(angle) * radius
                        );

                        ArmorStand cloud = center.getWorld().spawn(cloudLoc, ArmorStand.class);
                        cloud.setGravity(false);
                        cloud.setVisible(false);
                        cloud.setHelmet(new ItemStack(Material.GRAY_CONCRETE));
                        effectEntities.add(cloud);

                        // Animate expansion
                        new BukkitRunnable() {
                            int ticks = 0;
                            double expandRadius = radius;
                            
                            @Override
                            public void run() {
                                if (!isApocalypseActive || ticks++ > 100) {
                                    cloud.remove();
                                    this.cancel();
                                    return;
                                }

                                expandRadius += 0.2;
                                double x = Math.cos(angle) * expandRadius;
                                double z = Math.sin(angle) * expandRadius;
                                double y = finalLayer * 10 + Math.sin(ticks * 0.1) * 2;

                                Location newLoc = center.clone().add(x, y, z);
                                cloud.teleport(newLoc);

                                // Add particle effects
                                center.getWorld().spawnParticle(
                                    Particle.SMOKE_LARGE,
                                    newLoc,
                                    5,
                                    1, 1, 1,
                                    0.05
                                );
                            }
                        }.runTaskTimer(plugin, 0L, 1L);
                    }

                    // Create colored glass rings
                    Material[] ringMaterials = {
                        Material.YELLOW_STAINED_GLASS,
                        Material.RED_STAINED_GLASS,
                        Material.BLACK_STAINED_GLASS
                    };

                    for (int ring = 0; ring < 3; ring++) {
                        final int ringIndex = ring;
                        double ringRadius = radius * (1.2 + ring * 0.2);
                        Material ringMaterial = ringMaterials[ring];

                        for (int i = 0; i < points; i++) {
                            double angle = (Math.PI * 2 * i) / points;
                            Location ringLoc = center.clone().add(
                                Math.cos(angle) * ringRadius,
                                finalLayer * 10 - 2 + ring * 4,
                                Math.sin(angle) * ringRadius
                            );

                            ArmorStand ringPiece = center.getWorld().spawn(ringLoc, ArmorStand.class);
                            ringPiece.setGravity(false);
                            ringPiece.setVisible(false);
                            ringPiece.setSmall(true);
                            ringPiece.setHelmet(new ItemStack(ringMaterial));
                            effectEntities.add(ringPiece);

                            // Animate ring rotation and expansion
                            new BukkitRunnable() {
                                int ticks = 0;
                                double localAngle = angle;
                                double expandRadius = ringRadius;
                                
                                @Override
                                public void run() {
                                    if (!isApocalypseActive || ticks++ > 100) {
                                        ringPiece.remove();
                                        this.cancel();
                                        return;
                                    }

                                    expandRadius += 0.3;
                                    localAngle += 0.02 * (ringIndex + 1) * (ringIndex % 2 == 0 ? 1 : -1);
                                    
                                    double x = Math.cos(localAngle) * expandRadius;
                                    double z = Math.sin(localAngle) * expandRadius;
                                    double y = finalLayer * 10 - 2 + ringIndex * 4 + Math.sin(ticks * 0.1) * 2;
                                    
                                    Location newLoc = center.clone().add(x, y, z);
                                    ringPiece.teleport(newLoc);

                                    // Add colored particles based on ring material
                                    Color particleColor;
                                    switch (ringMaterial) {
                                        case YELLOW_STAINED_GLASS:
                                            particleColor = Color.YELLOW;
                                            break;
                                        case RED_STAINED_GLASS:
                                            particleColor = Color.RED;
                                            break;
                                        default:
                                            particleColor = Color.BLACK;
                                    }

                                    center.getWorld().spawnParticle(
                                        Particle.REDSTONE,
                                        newLoc,
                                        2,
                                        0.2, 0.2, 0.2,
                                        0,
                                        new Particle.DustOptions(particleColor, 1)
                                    );
                                }
                            }.runTaskTimer(plugin, 0L, 1L);
                        }
                    }
                }
            }.runTaskLater(plugin, layer * 20L);
        }

        // Create ground destruction effect
        new BukkitRunnable() {
            int wave = 0;
            @Override
            public void run() {
                if (wave++ > 5 || !isApocalypseActive) {
                    this.cancel();
                    return;
                }

                double radius = wave * 5;
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                    Location loc = center.clone().add(
                        Math.cos(angle) * radius,
                        0,
                        Math.sin(angle) * radius
                    );

                    // Create explosion effect
                    center.getWorld().spawnParticle(
                        Particle.EXPLOSION_HUGE,
                        loc,
                        1,
                        0, 0, 0,
                        0
                    );

                    // Spawn burning skeletons
                    if (Math.random() < 0.3) {
                        Skeleton skeleton = loc.getWorld().spawn(loc, Skeleton.class);
                        skeleton.setFireTicks(Integer.MAX_VALUE);
                        skeleton.setHealth(1);
                    }
                }

                // Play explosion sound
                playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                playRandomHorrorSound(center);
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void finalizeApocalypse(Player player) {
        // Clean up effects
        effectEntities.forEach(Entity::remove);
        effectEntities.clear();
        apocalypseBossBar.removeAll();

        // Final message and effects
        Bukkit.broadcastMessage("§4§l☠ " + player.getName() + " SON ÇIĞLIĞINI ATTI! ☠");
        player.getWorld().strikeLightningEffect(player.getLocation());
        
        // Play final sounds
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 0.5f);
        }

        // Kill the player
        new BukkitRunnable() {
            @Override
            public void run() {
                player.setHealth(0);
                isApocalypseActive = false;
                
                // Show player to everyone again
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.showPlayer(plugin, player);
                }
            }
        }.runTaskLater(plugin, 40L);
    }

    private void playRandomHorrorSound(Location location) {
        Sound randomSound = HORROR_SOUNDS[new Random().nextInt(HORROR_SOUNDS.length)];
        location.getWorld().playSound(
            location,
            randomSound,
            1.0f,
            0.5f + (float)(Math.random() * 0.5)
        );
    }

    @Override
    public void cast(Player caster) {
        // This skill cannot be cast normally
    }
} 