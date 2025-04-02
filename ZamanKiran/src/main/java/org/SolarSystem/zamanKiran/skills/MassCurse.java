package org.SolarSystem.zamanKiran.skills;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class MassCurse extends Skill {
    private static final double RADIUS = 8.0;
    private static final int DURATION = 30 * 20; // 30 seconds
    private static final Map<UUID, List<CurseEffect>> activeCurses = new HashMap<>();
    private static final Random random = new Random();

    public enum CurseType {
        BLINDNESS(PotionEffectType.BLINDNESS, "Körlük", "§8"),
        CONFUSION(PotionEffectType.CONFUSION, "Karışıklık", "§5"),
        WEAKNESS(PotionEffectType.WEAKNESS, "Zayıflık", "§7"),
        SLOW(PotionEffectType.SLOW, "Yavaşlık", "§8"),
        HUNGER(PotionEffectType.HUNGER, "Açlık", "§6"),
        UNLUCK(PotionEffectType.UNLUCK, "Şanssızlık", "§4"),
        WITHER(PotionEffectType.WITHER, "Çürüme", "§0"),
        LEVITATION(PotionEffectType.LEVITATION, "Yükseliş", "§f");

        public final PotionEffectType effect;
        private final String name;
        private final String color;

        CurseType(PotionEffectType effect, String name, String color) {
            this.effect = effect;
            this.name = name;
            this.color = color;
        }
    }

    public static class CurseEffect {
        private final CurseType type;
        private final long endTime;
        private final BukkitRunnable particleTask;

        public CurseEffect(CurseType type, long endTime, BukkitRunnable particleTask) {
            this.type = type;
            this.endTime = endTime;
            this.particleTask = particleTask;
        }

        public void cancel() {
            if (particleTask != null) {
                particleTask.cancel();
            }
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= endTime;
        }
    }

    public MassCurse(Plugin plugin) {
        super(plugin, "Toplu Lanetleme", 60, 80);
    }

    @Override
    public void cast(Player caster) {
        Location center = caster.getLocation();
        Collection<Entity> nearbyEntities = center.getWorld().getNearbyEntities(center, RADIUS, RADIUS, RADIUS);

        // Visual effects at caster
        createCurseEffect(center);

        for (Entity entity : nearbyEntities) {
            if (entity instanceof Player && entity != caster) {
                Player target = (Player) entity;
                applyCurses(target);
            }
        }
    }

    private void applyCurses(Player target) {
        // Select random curses
        List<CurseType> availableCurses = new ArrayList<>(Arrays.asList(CurseType.values()));
        Collections.shuffle(availableCurses);
        List<CurseType> selectedCurses = availableCurses.subList(0, 3);

        // Apply curses
        List<CurseEffect> curseEffects = new ArrayList<>();
        long endTime = System.currentTimeMillis() + (DURATION * 50);

        for (CurseType curse : selectedCurses) {
            // Apply potion effect
            target.addPotionEffect(new PotionEffect(curse.effect, DURATION, 1));

            // Create particle effect task
            BukkitRunnable particleTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!target.isOnline() || !isActive) {
                        this.cancel();
                        return;
                    }

                    Location loc = target.getLocation().add(0, 1, 0);
                    createCurseParticles(loc, curse);
                }
            };
            particleTask.runTaskTimer(plugin, 0L, 5L);

            // Add to active curses
            curseEffects.add(new CurseEffect(curse, endTime, particleTask));
        }

        // Store active curses
        activeCurses.put(target.getUniqueId(), curseEffects);

        // Send curse message
        String curseList = selectedCurses.stream()
            .map(curse -> curse.color + curse.name)
            .reduce((a, b) -> a + "§7, " + b)
            .orElse("");

        target.sendTitle(
            "§4✟ LANETLENDİN! ✟",
            "§7Lanetler: " + curseList,
            10, 40, 10
        );

        // Play curse sounds
        playCurseSounds(target.getLocation());
    }

    private void createCurseEffect(Location location) {
        new BukkitRunnable() {
            double radius = 0;
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ > 20) {
                    this.cancel();
                    return;
                }

                radius += 0.4;
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location particleLoc = location.clone().add(x, 0.1, z);

                    location.getWorld().spawnParticle(
                        Particle.SPELL_WITCH,
                        particleLoc,
                        1, 0, 0, 0, 0
                    );
                }

                if (ticks % 4 == 0) {
                    location.getWorld().playSound(
                        location,
                        Sound.ENTITY_ELDER_GUARDIAN_CURSE,
                        0.5f,
                        0.5f + (float)radius / 10
                    );
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void createCurseParticles(Location location, CurseType curse) {
        World world = location.getWorld();
        switch (curse) {
            case BLINDNESS:
                world.spawnParticle(Particle.SMOKE_NORMAL, location, 5, 0.2, 0.2, 0.2, 0.02);
                break;
            case CONFUSION:
                world.spawnParticle(Particle.PORTAL, location, 3, 0.2, 0.2, 0.2, 0.02);
                break;
            case WEAKNESS:
                world.spawnParticle(Particle.SPELL_MOB, location, 5, 0.2, 0.2, 0.2, 0);
                break;
            case SLOW:
                world.spawnParticle(Particle.SNOW_SHOVEL, location, 3, 0.2, 0.2, 0.2, 0.02);
                break;
            case HUNGER:
                world.spawnParticle(Particle.DAMAGE_INDICATOR, location, 2, 0.2, 0.2, 0.2, 0.02);
                break;
            case UNLUCK:
                world.spawnParticle(Particle.SPELL_WITCH, location, 3, 0.2, 0.2, 0.2, 0.02);
                break;
            case WITHER:
                world.spawnParticle(Particle.SUSPENDED_DEPTH, location, 5, 0.2, 0.2, 0.2, 0.02);
                break;
            case LEVITATION:
                world.spawnParticle(Particle.CLOUD, location, 3, 0.2, 0.2, 0.2, 0.02);
                break;
        }
    }

    private void playCurseSounds(Location location) {
        World world = location.getWorld();
        world.playSound(location, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.5f);
        world.playSound(location, Sound.ENTITY_WITHER_SPAWN, 0.5f, 2.0f);
        
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count++ >= 5) {
                    this.cancel();
                    return;
                }
                
                world.playSound(
                    location,
                    Sound.ENTITY_VEX_AMBIENT,
                    0.5f,
                    0.5f + (float)random.nextDouble() * 0.5f
                );
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    public static void removeCurses(Player player) {
        List<CurseEffect> curses = activeCurses.remove(player.getUniqueId());
        if (curses != null) {
            for (CurseEffect curse : curses) {
                curse.cancel();
                player.removePotionEffect(curse.type.effect);
            }
            
            // Removal effects
            Location loc = player.getLocation().add(0, 1, 0);
            player.getWorld().spawnParticle(
                Particle.END_ROD,
                loc,
                50, 0.5, 1, 0.5, 0.1
            );
            player.playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.2f);
            player.sendMessage(ChatColor.GREEN + "» Lanetler etkisini kaybetti!");
        }
    }

    public static boolean isCursed(Player player) {
        return activeCurses.containsKey(player.getUniqueId());
    }

    public static List<CurseType> getActiveCurseTypes(Player player) {
        List<CurseEffect> effects = activeCurses.get(player.getUniqueId());
        if (effects == null) return Collections.emptyList();
        
        return effects.stream()
            .filter(effect -> !effect.isExpired())
            .map(effect -> effect.type)
            .collect(java.util.stream.Collectors.toList());
    }
} 