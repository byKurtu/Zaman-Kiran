package org.SolarSystem.zamanKiran.skills;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class PentagramRitual extends Skill {
    private static final double PENTAGRAM_RADIUS = 7.0;
    private static final int RITUAL_DURATION = 30 * 20;
    private static final int PHASE_1_DURATION = 100;  // 5 saniye
    private static final int PHASE_2_DURATION = 200;  // 10 saniye
    private static final int PHASE_3_DURATION = 300;  // 15 saniye
    private static final int PHASE_4_DURATION = 400;  // 20 saniye
    private static final double ROTATION_SPEED = Math.PI / 30;
    private static final int HORROR_EFFECT_RADIUS = 30;
    private static final int SPECIAL_ITEM_DROP_CHANCE = 5; // %5
    
    private static final String[] HORROR_MESSAGES = {
        "§4☠ Ruhun artık bana ait...",
        "§4☠ Karanlık güçler etrafını sarıyor...",
        "§4☠ Cehennemin kapıları açılıyor...",
        "§4☠ Kurtu'nun laneti üzerinde...",
        "§4☠ Şeytani varlıklar seni izliyor...",
        "§4☠ Ruhun sonsuza dek lanetlendi...",
        "§4☠ Karanlığın içinde kaybolacaksın...",
        "§4☠ Pentagram kanınla besleniyor...",
        "§4☠ Kurtu'nun gazabı üzerinde...",
        "§4☠ Artık geri dönüş yok..."
    };

    private final Random random = ThreadLocalRandom.current();
    private final Set<BlockDisplay> pentagramPoints = ConcurrentHashMap.newKeySet();
    private final Set<BlockDisplay> fireCircle = ConcurrentHashMap.newKeySet();
    private final Set<ItemDisplay> starDisplays = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> horrorEffectTasks = new ConcurrentHashMap<>();
    
    private BossBar ritualBar;
    private int mainTaskId = -1;
    private boolean isCleanedUp = false;

    public PentagramRitual(Plugin plugin) {
        super(plugin, "Kurtu'nun Pentagram Ritüeli", 60, 100);
    }

    @Override
    public void cast(Player caster) {
        if (!canCast(caster)) return;
        
        startSkill();
        Location center = caster.getLocation().clone();
        
        initializeRitual(center);
        createBossBar(caster);
        startMainRitualTask(center);
    }

    private boolean canCast(Player player) {
        if (isOnCooldown()) {
            player.sendMessage(ChatColor.RED + "» Bu yetenek şu anda kullanılamaz! (" + 
                String.format("%.1f", getRemainingCooldown() / 20.0) + "s)");
            return false;
        }
        return true;
    }

    private void createBossBar(Player caster) {
        ritualBar = Bukkit.createBossBar(
            "§4✠ Kurtu'nun Pentagram Ritüeli ✠",
            BarColor.RED,
            BarStyle.SEGMENTED_20
        );
        ritualBar.setProgress(1.0);
        addNearbyPlayersToBossBar(caster.getLocation());
    }

    private void startMainRitualTask(Location center) {
        RitualState state = new RitualState();
        
        mainTaskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive || state.ticks >= RITUAL_DURATION) {
                    cleanup();
                    endSkill();
                    this.cancel();
                    return;
                }

                state.update(center);
                updatePhase(state, center);
                updateEffects(state, center);
                
                state.ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L).getTaskId();
    }

    private void updatePhase(RitualState state, Location center) {
        if (state.ticks == 0) {
            // Phase 1 start
            changeSkyToBloodMoon(center.getWorld());
            playRitualSounds(center);
        }
        else if (state.ticks == PHASE_1_DURATION) {
            // Phase 2 start
            state.phase = 2;
            startHorrorPhase(center);
            updateBossBarTitle("§4✠ Karanlık Güçler Yükseliyor ✠");
        }
        else if (state.ticks == PHASE_2_DURATION) {
            // Phase 3 start
            state.phase = 3;
            startExplosionPhase(center);
            updateBossBarTitle("§4✠ Kurtu'nun Gazabı ✠");
        }
        else if (state.ticks == PHASE_3_DURATION) {
            // Phase 4 start
            state.phase = 4;
            finishRitual(center);
            updateBossBarTitle("§4✠ Ritüel Tamamlandı ✠");
        }
    }

    private void updateEffects(RitualState state, Location center) {
        // Update boss bar progress
        ritualBar.setProgress(1.0 - ((double) state.ticks / RITUAL_DURATION));
        
        // Regular effects
        if (state.ticks % 2 == 0) {
            spawnAmbientParticles(center, state.rotation);
        }

        // Phase specific effects
        switch (state.phase) {
            case 2:
                if (state.ticks % 40 == 0) {
                    sendHorrorMessage(center);
                    applyHorrorEffects(center);
                }
                break;
            case 3:
            case 4:
                if (state.ticks % 10 == 0) {
                    strikeLightning(center);
                }
                updateVortex(center, state.rotation);
                
                // Special item drops
                if (random.nextInt(100) < SPECIAL_ITEM_DROP_CHANCE) {
                    dropSpecialItem(center);
                }
                break;
        }

        state.rotation += ROTATION_SPEED;
    }

    private void cleanup() {
        if (isCleanedUp) return;
        
        isCleanedUp = true;
        
        // Cancel all tasks
        if (mainTaskId != -1) {
            Bukkit.getScheduler().cancelTask(mainTaskId);
        }
        horrorEffectTasks.values().forEach(taskId -> 
            Bukkit.getScheduler().cancelTask(taskId));
        
        // Remove all entities
        pentagramPoints.forEach(Entity::remove);
        fireCircle.forEach(Entity::remove);
        starDisplays.forEach(Entity::remove);
        
        // Clear collections
        pentagramPoints.clear();
        fireCircle.clear();
        starDisplays.clear();
        horrorEffectTasks.clear();
        
        // Reset world effects
        if (ritualBar != null) {
            ritualBar.removeAll();
            ritualBar = null;
        }
    }

    private class RitualState {
        int ticks = 0;
        int phase = 1;
        double rotation = 0;
        
        void update(Location center) {
            // Update pentagram
            updatePentagramPoints(center, rotation);
            updateStarDisplays(center, rotation);
            rotation += ROTATION_SPEED;
        }
    }
    
    private void updatePentagramPoints(Location center, double rotation) {
        List<BlockDisplay> points = getPentagramPointsList();
        for (int i = 0; i < points.size(); i++) {
            BlockDisplay point = points.get(i);
            if (point != null && point.isValid()) {
                double angle = rotation + (2 * Math.PI * i) / 5;
                Location newLoc = center.clone().add(
                    Math.cos(angle) * PENTAGRAM_RADIUS,
                    0.1 + Math.sin(rotation * 0.5) * 0.2,
                    Math.sin(angle) * PENTAGRAM_RADIUS
                );
                point.teleport(newLoc);
                rotateBlockDisplay(point, (float)angle, 0, 1, 0);
            }
        }
    }
    
    private void updateStarDisplays(Location center, double rotation) {
        List<ItemDisplay> stars = getStarDisplaysList();
        for (int i = 0; i < stars.size(); i++) {
            ItemDisplay star = stars.get(i);
            if (star != null && star.isValid()) {
                double angle = rotation + (2 * Math.PI * i) / 5;
                Location newLoc = center.clone().add(
                    Math.cos(angle) * PENTAGRAM_RADIUS,
                    1 + Math.sin(rotation) * 0.5,
                    Math.sin(angle) * PENTAGRAM_RADIUS
                );
                star.teleport(newLoc);
                rotateItemDisplay(star, (float)(rotation * 2), 0, 1, 0);
            }
        }
    }

    private void addNearbyPlayersToBossBar(Location center) {
        for (Player player : center.getWorld().getPlayers()) {
            if (player.getLocation().distance(center) <= 30) {
                ritualBar.addPlayer(player);
            }
        }
    }

    private void dropSpecialItem(Location center) {
        ItemStack specialItem = new ItemStack(Material.NETHERITE_INGOT);
        World world = center.getWorld();
        Location dropLoc = center.clone().add(
            (random.nextDouble() - 0.5) * PENTAGRAM_RADIUS,
            1,
            (random.nextDouble() - 0.5) * PENTAGRAM_RADIUS
        );
        
        world.dropItemNaturally(dropLoc, specialItem);
        world.spawnParticle(Particle.TOTEM, dropLoc, 20, 0.5, 0.5, 0.5, 0.1);
        playSound(dropLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.5f);
    }

    private void initializeRitual(Location center) {
        // Ateş çemberi oluştur
        for (int i = 0; i < 36; i++) {
            double angle = (2 * Math.PI * i) / 36;
            Location fireLoc = center.clone().add(
                Math.cos(angle) * PENTAGRAM_RADIUS,
                0,
                Math.sin(angle) * PENTAGRAM_RADIUS
            );
            
            BlockDisplay fire = createBlockDisplay(fireLoc, Material.FIRE);
            if (fire != null) {
                fireCircle.add(fire);
                scaleBlockDisplay(fire, 0.8f);
            }
        }
    }

    private void createPentagram(Location center) {
        // Pentagram köşeleri
        for (int i = 0; i < 5; i++) {
            double angle = (2 * Math.PI * i) / 5;
            Location pointLoc = center.clone().add(
                Math.cos(angle) * PENTAGRAM_RADIUS,
                0.1,
                Math.sin(angle) * PENTAGRAM_RADIUS
            );
            
            BlockDisplay point = createBlockDisplay(pointLoc, Material.OBSIDIAN);
            if (point != null) {
                pentagramPoints.add(point);
                scaleBlockDisplay(point, 0.5f);
            }

            // Yıldız efekti
            ItemDisplay star = createItemDisplay(pointLoc.clone().add(0, 1, 0), Material.NETHER_STAR);
            if (star != null) {
                starDisplays.add(star);
                scaleItemDisplay(star, 0.5f);
            }
        }
    }

    private void startHorrorPhase(Location center) {
        World world = center.getWorld();
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distance(center) <= 30) {
                // Korku efektleri
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 200, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 2));
                
                // Ekran titreme efekti
                new BukkitRunnable() {
                    int count = 0;
                    @Override
                    public void run() {
                        if (count++ >= 20) {
                            this.cancel();
                            return;
                        }
                        player.playSound(player.getLocation(), Sound.AMBIENT_CRIMSON_FOREST_MOOD, 1.0f, 0.5f);
                        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 0.5f, 0.5f);
                        
                        // Rastgele bakış açısı değişimi
                        Location loc = player.getLocation();
                        loc.setYaw(loc.getYaw() + (float)(Math.random() * 10 - 5));
                        loc.setPitch(loc.getPitch() + (float)(Math.random() * 10 - 5));
                        player.teleport(loc);
                    }
                }.runTaskTimer(plugin, 0L, 5L);
            }
        }
    }

    private void sendHorrorMessage(Location center) {
        String message = HORROR_MESSAGES[random.nextInt(HORROR_MESSAGES.length)];
        for (Entity entity : center.getWorld().getNearbyEntities(center, 20, 20, 20)) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                player.sendTitle("", message, 10, 20, 10);
            }
        }
    }

    private void applyHorrorEffects(Location center) {
        center.getWorld().spawnParticle(
            Particle.DRAGON_BREATH,
            center,
            50, 5, 2, 5, 0.1
        );
        
        for (Entity entity : center.getWorld().getNearbyEntities(center, 15, 15, 15)) {
            if (entity instanceof Player) {
                Location playerLoc = entity.getLocation();
                playerLoc.getWorld().spawnParticle(
                    Particle.SMOKE_LARGE,
                    playerLoc.add(0, 1, 0),
                    20, 0.5, 1, 0.5, 0.05
                );
            }
        }
    }

    private void startExplosionPhase(Location center) {
        createExplosionPhase(center);
    }

    private void strikeLightning(Location center) {
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = random.nextDouble() * PENTAGRAM_RADIUS;
        Location strikeLoc = center.clone().add(
            Math.cos(angle) * distance,
            0,
            Math.sin(angle) * distance
        );
        
        center.getWorld().strikeLightningEffect(strikeLoc);
    }

    private void updateVortex(Location center, double rotation) {
        for (int i = 0; i < 8; i++) {
            double angle = rotation + (2 * Math.PI * i) / 8;
            double height = 5 + Math.sin(rotation + i) * 2;
            Location particleLoc = center.clone().add(
                Math.cos(angle) * (PENTAGRAM_RADIUS * 0.5),
                height,
                Math.sin(angle) * (PENTAGRAM_RADIUS * 0.5)
            );
            
            center.getWorld().spawnParticle(
                Particle.REVERSE_PORTAL,
                particleLoc,
                3, 0.2, 0.2, 0.2, 0.05
            );
        }
    }

    private void finishRitual(Location center) {
        World world = center.getWorld();
        
        // Final sesleri
        world.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.5f);
        world.playSound(center, Sound.AMBIENT_WARPED_FOREST_MOOD, 2.0f, 0.6f);
        
        // Geçici pentagram efekti
        createTemporaryPentagram(center);
    }

    private void createTemporaryPentagram(Location center) {
        Material[] materials = {
            Material.CRYING_OBSIDIAN,
            Material.ANCIENT_DEBRIS,
            Material.NETHERITE_BLOCK
        };
        
        List<BlockDisplay> pentagramBlocks = new ArrayList<>();
        
        // Pentagram köşeleri
        for (int i = 0; i < 5; i++) {
            double angle = (2 * Math.PI * i) / 5;
            Location point = center.clone().add(
                Math.cos(angle) * PENTAGRAM_RADIUS,
                0.1,
                Math.sin(angle) * PENTAGRAM_RADIUS
            );
            
            BlockDisplay cornerBlock = createBlockDisplay(point, materials[random.nextInt(materials.length)]);
            if (cornerBlock != null) {
                pentagramBlocks.add(cornerBlock);
                scaleBlockDisplay(cornerBlock, 0.8f);
            }
            
            // Noktalar arası çizgiler
            Location nextPoint = center.clone().add(
                Math.cos((angle + 4 * Math.PI / 5) % (2 * Math.PI)) * PENTAGRAM_RADIUS,
                0.1,
                Math.sin((angle + 4 * Math.PI / 5) % (2 * Math.PI)) * PENTAGRAM_RADIUS
            );
            
            drawTemporaryLine(point, nextPoint, materials[random.nextInt(materials.length)], pentagramBlocks);
        }
        
        // 10 saniye sonra pentagramı kaldır
        new BukkitRunnable() {
            @Override
            public void run() {
                pentagramBlocks.forEach(block -> {
                    if (block != null && block.isValid()) {
                        block.remove();
                    }
                });
            }
        }.runTaskLater(plugin, 200L); // 10 saniye = 200 tick
    }

    private void drawTemporaryLine(Location start, Location end, Material material, List<BlockDisplay> blocks) {
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(end);
        
        for (double d = 0; d < distance; d += 0.5) {
            Location point = start.clone().add(direction.clone().multiply(d));
            BlockDisplay lineBlock = createBlockDisplay(point, material);
            if (lineBlock != null) {
                blocks.add(lineBlock);
                scaleBlockDisplay(lineBlock, 0.4f);
            }
        }
    }

    private void spawnAmbientParticles(Location center, double rotation) {
        // Pentagram çizgileri
        for (int i = 0; i < 5; i++) {
            double angle1 = (2 * Math.PI * i) / 5;
            double angle2 = (2 * Math.PI * ((i + 2) % 5)) / 5;
            
            Location point1 = center.clone().add(
                Math.cos(angle1) * PENTAGRAM_RADIUS,
                0.1,
                Math.sin(angle1) * PENTAGRAM_RADIUS
            );
            
            Location point2 = center.clone().add(
                Math.cos(angle2) * PENTAGRAM_RADIUS,
                0.1,
                Math.sin(angle2) * PENTAGRAM_RADIUS
            );
            
            Vector line = point2.toVector().subtract(point1.toVector());
            double length = line.length();
            line.normalize();
            
            for (double d = 0; d < length; d += 0.5) {
                Location particleLoc = point1.clone().add(line.clone().multiply(d));
                particleLoc.getWorld().spawnParticle(
                    Particle.FLAME,
                    particleLoc,
                    1, 0.1, 0.1, 0.1, 0
                );
            }
        }
    }

    private void changeSkyToBloodMoon(World world) {
        for (Player player : world.getPlayers()) {
            player.setPlayerTime(18000, false); // Gece vakti
            player.setPlayerWeather(WeatherType.CLEAR);
        }
        
        new BukkitRunnable() {
            @Override
            public void run() {
                world.setTime(18000);
                world.setStorm(false);
                world.setThundering(false);
                
                // Kırmızı gökyüzü efekti
                for (Player player : world.getPlayers()) {
                    Location loc = player.getLocation();
                    world.spawnParticle(Particle.REDSTONE, 
                        loc.clone().add(0, 50, 0), 
                        50, 20, 10, 20, 
                        new Particle.DustOptions(Color.RED, 2));
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void resetSky(World world) {
        for (Player player : world.getPlayers()) {
            player.resetPlayerTime();
            player.resetPlayerWeather();
        }
    }

    private void playRitualSounds(Location location) {
        World world = location.getWorld();
        
        // Başlangıç sesleri
        world.playSound(location, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        world.playSound(location, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.5f);
        
        // Sürekli uğultu sesi
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive) {
                    this.cancel();
                    return;
                }
                world.playSound(location, Sound.AMBIENT_NETHER_WASTES_MOOD, 2.0f, 0.5f);
                world.playSound(location, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 1.0f, 0.5f);
                world.playSound(location, Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 0.5f);
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    private void createAmbientEffects(Player player) {
        new BukkitRunnable() {
            double angle = 0;
            List<ArmorStand> spirits = new ArrayList<>();
            
            @Override
            public void run() {
                if (!isActive) {
                    spirits.forEach(Entity::remove);
                    this.cancel();
                    return;
                }
                
                // Oyuncunun etrafında dönen alevler
                Location playerLoc = player.getLocation();
                for (int i = 0; i < 4; i++) {
                    double circleX = Math.cos(angle + (Math.PI * 2 * i / 4)) * 2;
                    double circleZ = Math.sin(angle + (Math.PI * 2 * i / 4)) * 2;
                    Location flameLoc = playerLoc.clone().add(circleX, 1, circleZ);
                    
                    player.getWorld().spawnParticle(Particle.FLAME, flameLoc, 3, 0.1, 0.1, 0.1, 0.02);
                    player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, flameLoc, 1, 0.1, 0.1, 0.1, 0);
                }
                
                // Hayalet ArmorStand'ler
                if (random.nextDouble() < 0.1) {
                    Location spawnLoc = playerLoc.clone().add(
                        random.nextDouble() * 10 - 5,
                        0,
                        random.nextDouble() * 10 - 5
                    );
                    
                    ArmorStand spirit = createArmorStand(spawnLoc, true, false);
                    spirit.setHelmet(new ItemStack(Material.WITHER_SKELETON_SKULL));
                    spirit.setGravity(false);
                    spirit.setSmall(true);
                    spirits.add(spirit);
                    
                    // Hayaleti 3 saniye sonra kaldır
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            spirit.remove();
                            spirits.remove(spirit);
                        }
                    }.runTaskLater(plugin, 60L);
                }
                
                angle += 0.2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void createExplosionPhase(Location center) {
        World world = center.getWorld();
        
        // Patlama sesleri ve efektler
        world.playSound(center, Sound.ENTITY_WITHER_DEATH, 2.0f, 0.5f);
        world.playSound(center, Sound.AMBIENT_BASALT_DELTAS_MOOD, 2.0f, 0.6f);
        world.playSound(center, Sound.ENTITY_GHAST_SCREAM, 1.5f, 0.7f);
        
        world.spawnParticle(Particle.EXPLOSION_HUGE, center, 5, 3, 3, 3, 0);
        world.spawnParticle(Particle.DRAGON_BREATH, center, 100, 3, 3, 3, 0.1);
        
        // Yıldırım çarpması
        for (int i = 0; i < 5; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Location strike = center.clone().add(
                        (Math.random() - 0.5) * 10,
                        0,
                        (Math.random() - 0.5) * 10
                    );
                    world.strikeLightning(strike);
                    world.playSound(strike, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.5f);
                }
            }.runTaskLater(plugin, i * 20L);
        }
    }

    private void updateBossBarTitle(String title) {
        if (ritualBar != null) {
            ritualBar.setTitle(title);
        }
    }
    
    private List<BlockDisplay> getPentagramPointsList() {
        return new ArrayList<>(pentagramPoints);
    }
    
    private List<ItemDisplay> getStarDisplaysList() {
        return new ArrayList<>(starDisplays);
    }
} 