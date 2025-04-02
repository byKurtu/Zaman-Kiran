package org.SolarSystem.zamanKiran.skills;

import org.SolarSystem.zamanKiran.ZamanKiran;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.RayTraceResult;
import org.joml.Vector3d;
import org.SolarSystem.zamanKiran.math.VectorUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.block.Block;
import org.SolarSystem.zamanKiran.systems.TeamSystem;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

public class ThrowableWeapon extends Skill implements Listener {
    private static final double THROW_SPEED = 1.5;
    private static final double GRAVITY = -0.05;
    private static final double AIR_RESISTANCE = 0.02;
    private static final double BOUNCE_FACTOR = 0.6;
    private static final double ROTATION_SPEED = Math.PI / 8;
    private static final double DAMAGE = 15.0;
    private static final double MAX_DISTANCE = 30.0;
    private static final double PICKUP_DISTANCE = 2.0;
    private static final int MAX_BOUNCES = 3;
    private static final double RECALL_SPEED = 2.0;
    private static final double AUTO_TARGET_RANGE = 10.0;

    private final Map<UUID, ArmorStand> weaponHolograms = new HashMap<>();
    private final Map<UUID, ArmorStand> weaponHolders = new HashMap<>();
    private final Map<UUID, Location> weaponLocations = new HashMap<>();
    private final Map<UUID, Vector> weaponVelocities = new HashMap<>();
    private final Map<UUID, BukkitRunnable> flyTasks = new HashMap<>();
    private final Map<UUID, ItemStack> storedWeapons = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, ThrowMode> playerModes = new HashMap<>();
    private final Map<UUID, List<ArmorStand>> trajectoryHolograms = new HashMap<>();
    private final Map<UUID, BukkitRunnable> trajectoryPreviewTasks = new HashMap<>();

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

    public ThrowableWeapon(Plugin plugin) {
        super(plugin, "Throwable Weapon", 5, 30);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        
        if (!player.isSneaking()) return;
        
        event.setCancelled(true);
        cycleThrowMode(player);

        // Hesaplamalı mod için sürekli yörünge gösterimi başlat
        if (playerModes.get(player.getUniqueId()) == ThrowMode.CALCULATIVE) {
            startTrajectoryPreview(player);
        } else {
            stopTrajectoryPreview(player);
        }
    }

    public void cycleThrowMode(Player player) {
        ThrowMode currentMode = playerModes.getOrDefault(player.getUniqueId(), ThrowMode.DIRECTIVE);
        ThrowMode nextMode = getNextMode(currentMode);
        playerModes.put(player.getUniqueId(), nextMode);
        
        // Action bar mesajı
        String message = ChatColor.AQUA + "» Fırlatma Modu: " + ChatColor.YELLOW + nextMode.getDisplayName();
        player.spigot().sendMessage(
            net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message)
        );
    }

    private ThrowMode getNextMode(ThrowMode currentMode) {
        switch (currentMode) {
            case AUTO:
                return ThrowMode.DIRECTIVE;
            case DIRECTIVE:
                return ThrowMode.CALCULATIVE;
            case CALCULATIVE:
                return ThrowMode.AUTO;
            default:
                return ThrowMode.AUTO;
        }
    }

    @Override
    public void cast(Player caster) {
        if (!isActive) return;
        
        ItemStack mainHand = caster.getInventory().getItemInMainHand();
        if (!isWeapon(mainHand)) return;

        // Store the weapon before removing it
        storedWeapons.put(caster.getUniqueId(), mainHand.clone());
        
        // Remove the weapon from inventory
        caster.getInventory().setItemInMainHand(null);

        // Rest of the casting logic
        Location start = caster.getEyeLocation();
        Vector direction = start.getDirection();
        
        ArmorStand hologram = createArmorStand(start, true, false);
        ArmorStand holder = createArmorStand(start, false, false);
        
        if (hologram == null || holder == null) {
            // If armor stands couldn't be created, return the weapon
            caster.getInventory().setItemInMainHand(mainHand);
            return;
        }
        
        setupArmorStands(hologram, holder);
        weaponHolograms.put(caster.getUniqueId(), hologram);
        weaponHolders.put(caster.getUniqueId(), holder);
        weaponLocations.put(caster.getUniqueId(), start.clone());
        weaponVelocities.put(caster.getUniqueId(), direction.clone().multiply(THROW_SPEED));

        // Get the throw mode
        ThrowMode mode = playerModes.getOrDefault(caster.getUniqueId(), ThrowMode.DIRECTIVE);
        
        switch (mode) {
            case AUTO:
                castAutoMode(caster, start, direction, hologram, holder);
                break;
            case DIRECTIVE:
                castDirectiveMode(caster, start, direction, hologram, holder);
                break;
            case CALCULATIVE:
                castCalculativeMode(caster, start, direction, hologram, holder);
                break;
        }

        startCooldown(caster);
    }

    private void setupArmorStands(ArmorStand hologram, ArmorStand holder) {
        if (hologram != null) {
            hologram.setVisible(false);
            hologram.setGravity(false);
            hologram.setSmall(false);
            hologram.setCustomNameVisible(true);
        }
        
        if (holder != null) {
            holder.setVisible(false);
            holder.setGravity(false);
            holder.setSmall(false);
            holder.setMarker(true);
        }
    }

    private void castAutoMode(Player caster, Location start, Vector direction, ArmorStand hologram, ArmorStand holder) {
        Entity target = findNearestTarget(caster, start);
        if (target == null) {
            castDirectiveMode(caster, start, direction, hologram, holder);
            return;
        }

        Location targetLoc = target.getLocation().add(0, 1, 0);
        Vector toTarget = targetLoc.toVector().subtract(start.toVector());
        double distance = toTarget.length();

        if (distance > AUTO_TARGET_RANGE) {
            castDirectiveMode(caster, start, direction, hologram, holder);
            return;
        }

        new BukkitRunnable() {
            private double progress = 0;
            private final Location currentLoc = start.clone();
            private final Vector velocity = toTarget.normalize().multiply(THROW_SPEED);

            @Override
            public void run() {
                if (!isActive || progress >= 1.0) {
                    cleanup();
                    return;
                }

                progress += 0.05;
                currentLoc.add(velocity);
                
                updateWeaponPosition(hologram, holder, currentLoc, VectorUtils.toVec3d(velocity));
                createAutoModeParticles(currentLoc);

                if (checkTargetHit(caster, hologram, currentLoc, target)) {
                    cleanup();
                }
            }

            private void cleanup() {
                cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void castDirectiveMode(Player caster, Location start, Vector direction, ArmorStand hologram, ArmorStand holder) {
        new BukkitRunnable() {
            private double progress = 0;
            private final Location currentLoc = start.clone();
            private final Vector velocity = direction.clone().multiply(THROW_SPEED);
            private boolean hasCollided = false;

            @Override
            public void run() {
                if (!isActive || hasCollided) {
                    if (!hasCollided) {
                        dropWeapon(hologram, currentLoc);
                    }
                    this.cancel();
                    return;
                }

                // Update position
                currentLoc.add(velocity);
                progress += velocity.length();

                // Check for collision
                if (checkCollision(currentLoc)) {
                    hasCollided = true;
                    return;
                }

                // Update armor stands
                if (hologram.isValid() && holder.isValid()) {
                    hologram.teleport(currentLoc);
                    holder.teleport(currentLoc);
                }

                // Cancel if max distance reached
                if (progress >= MAX_DISTANCE) {
                    dropWeapon(hologram, currentLoc);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void dropWeapon(ArmorStand hologram, Location location) {
        if (hologram != null && hologram.isValid()) {
            hologram.teleport(location);
            hologram.setGravity(true);
            hologram.setVisible(true);
        }
    }

    private void cleanupExistingArmorStands(UUID playerId) {
        // Eski hologramı temizle
        ArmorStand oldHologram = weaponHolograms.remove(playerId);
        if (oldHologram != null && oldHologram.isValid()) {
            oldHologram.remove();
        }
        
        // Eski holder'ı temizle
        ArmorStand oldHolder = weaponHolders.remove(playerId);
        if (oldHolder != null && oldHolder.isValid()) {
            oldHolder.remove();
        }
        
        // Eski task'ı iptal et
        BukkitRunnable oldTask = flyTasks.remove(playerId);
        if (oldTask != null) {
            oldTask.cancel();
        }
    }

    private void castCalculativeMode(Player caster, Location start, Vector direction, ArmorStand hologram, ArmorStand holder) {
        // Yörünge gösterimi zaten aktif, direkt fırlat
        castDirectiveMode(caster, start, direction, hologram, holder);
    }

    private void startTrajectoryPreview(Player player) {
        stopTrajectoryPreview(player); // Önceki görevi temizle

        BukkitRunnable previewTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || 
                    playerModes.get(player.getUniqueId()) != ThrowMode.CALCULATIVE) {
                    stopTrajectoryPreview(player);
                    return;
                }

                // Mevcut yörünge hologramlarını temizle
                clearTrajectoryHolograms(player);

                // Yeni yörünge hesapla ve göster
                Location start = player.getEyeLocation();
                Vector direction = start.getDirection();
                showCalculativeTrajectory(player, start, direction);
            }
        };

        previewTask.runTaskTimer(plugin, 0L, 2L); // Her 2 tick'te bir güncelle
        trajectoryPreviewTasks.put(player.getUniqueId(), previewTask);
    }

    private void stopTrajectoryPreview(Player player) {
        BukkitRunnable task = trajectoryPreviewTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        clearTrajectoryHolograms(player);
    }

    private void showCalculativeTrajectory(Player player, Location start, Vector direction) {
        List<ArmorStand> trajectoryPoints = new ArrayList<>();
        Location predictedLoc = start.clone();
        Vector velocity = direction.clone().multiply(THROW_SPEED);
        
        for (int i = 0; i < 20; i++) {
            // Yörünge noktası oluştur
            ArmorStand point = createTrajectoryStand(predictedLoc, getTrajectorySymbol(i, velocity));
            trajectoryPoints.add(point);

            // Bir sonraki noktayı hesapla
            velocity.setY(velocity.getY() + GRAVITY);
            velocity.multiply(1 - AIR_RESISTANCE);
            predictedLoc.add(velocity);

            // Duvar kontrolü
            if (predictedLoc.getBlock().getType().isSolid()) {
                ArmorStand bouncePoint = createTrajectoryStand(predictedLoc, "§c✧");
                trajectoryPoints.add(bouncePoint);
                break;
            }
        }

        trajectoryHolograms.put(player.getUniqueId(), trajectoryPoints);
    }

    private ArmorStand createTrajectoryStand(Location location, String symbol) {
        ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setSmall(false); // Büyük ArmorStand
        stand.setMarker(true);
        stand.setCustomName(symbol);
        stand.setCustomNameVisible(true);
        return stand;
    }

    private String getTrajectorySymbol(int index, Vector velocity) {
        if (index == 0) return "§a⚡"; // Başlangıç
        if (index % 3 == 0) { // Her 3 noktada bir büyük sembol
            if (velocity.getY() > 0) return "§e⇗";
            if (velocity.getY() < 0) return "§e⇘";
            return "§e⇒";
        }
        // Ara noktalarda küçük semboller
        if (velocity.getY() > 0) return "§e↗";
        if (velocity.getY() < 0) return "§e↘";
        return "§e→";
    }

    private void clearTrajectoryHolograms(Player player) {
        List<ArmorStand> holograms = trajectoryHolograms.remove(player.getUniqueId());
        if (holograms != null) {
            holograms.forEach(Entity::remove);
        }
    }

    private Entity findNearestTarget(Player caster, Location location) {
        double closestDistance = 10.0;
        Entity closestEntity = null;

        for (Entity entity : location.getWorld().getNearbyEntities(location, 10, 10, 10)) {
            if (entity instanceof LivingEntity && entity != caster && !(entity instanceof ArmorStand)) {
                double distance = entity.getLocation().distance(location);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestEntity = entity;
                }
            }
        }

        return closestEntity;
    }

    private Vector3d calculateControlPoint(Vector3d start, Vector3d end) {
        Vector3d mid = start.add(end, new Vector3d()).mul(0.5);
        mid.y += 5.0; // Yay yüksekliği
        return mid;
    }

    private Vector3d calculateBezierPoint(Vector3d p0, Vector3d p1, Vector3d p2, double t) {
        double mt = 1 - t;
        return new Vector3d(
            mt * mt * p0.x + 2 * mt * t * p1.x + t * t * p2.x,
            mt * mt * p0.y + 2 * mt * t * p1.y + t * t * p2.y,
            mt * mt * p0.z + 2 * mt * t * p1.z + t * t * p2.z
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

    private void createAutoModeParticles(Location location) {
        location.getWorld().spawnParticle(
            Particle.CRIT_MAGIC,
            location,
            3, 0.1, 0.1, 0.1, 0.05
        );
    }

    private void createDirectiveModeParticles(Location location, Vector velocity) {
        location.getWorld().spawnParticle(
            Particle.CRIT,
            location,
            1, 0.1, 0.1, 0.1, 0.05
        );
    }

    private void showTrajectory(Location start, Vector velocity) {
        new BukkitRunnable() {
            private int ticks = 0;
            private final Vector vel = velocity.clone();
            private final Location loc = start.clone();

            @Override
            public void run() {
                if (ticks++ > 40) {
                    cancel();
                    return;
                }

                vel.add(new Vector(0, GRAVITY, 0));
                vel.multiply(1 - AIR_RESISTANCE);
                loc.add(vel);

                loc.getWorld().spawnParticle(
                    Particle.END_ROD,
                    loc,
                    1, 0, 0, 0, 0
                );

                if (loc.getBlock().getType().isSolid()) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!event.isSneaking()) return;
        
        // Check for weapon pickup
        ArmorStand stand = weaponHolders.get(player.getUniqueId());
        if (stand != null && stand.isValid() && stand.getLocation().distance(player.getLocation()) <= PICKUP_DISTANCE) {
            ItemStack weapon = stand.getEquipment().getHelmet();
            if (weapon != null) {
                player.getInventory().setItemInMainHand(weapon);
                stand.remove();
                weaponHolders.remove(player.getUniqueId());
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.0f, 1.0f);
            }
        }
        
        // Handle weapon recall
        UUID playerId = player.getUniqueId();
        if (weaponLocations.containsKey(playerId)) {
            Location weaponLoc = weaponLocations.get(playerId);
            if (weaponLoc.distance(player.getLocation()) <= PICKUP_DISTANCE) {
                giveWeaponToPlayer(player);
            } else {
                startWeaponRecall(player, weaponLoc);
            }
        }
    }

    private void updateWeaponPosition(ArmorStand hologram, ArmorStand holder, Location loc, Vector3d vel) {
        if (hologram != null && hologram.isValid()) {
            hologram.teleport(loc.clone().add(0, 0.3, 0));
        }
        
        if (holder != null && holder.isValid()) {
            holder.teleport(loc);
            
            // Hareket yönüne göre dönüş hesapla
            Vector bukkitVel = VectorUtils.toBukkitVector(vel);
            double yaw = Math.atan2(bukkitVel.getZ(), bukkitVel.getX());
            double pitch = Math.atan2(bukkitVel.getY(), 
                Math.sqrt(bukkitVel.getX() * bukkitVel.getX() + bukkitVel.getZ() * bukkitVel.getZ()));
            
            // Silahın dönüşünü ayarla
            holder.setRotation((float)Math.toDegrees(yaw), (float)Math.toDegrees(pitch));
            
            // Silahın eğimini hareket yönüne göre ayarla
            EulerAngle armPose = new EulerAngle(
                pitch, // Yukarı/aşağı eğim
                yaw,   // Yatay dönüş
                Math.PI / 2 // Sabit yan dönüş
            );
            holder.setRightArmPose(armPose);
        }
    }

    private boolean checkCollision(Location location) {
        Block block = location.getBlock();
        return block.getType().isSolid();
    }

    private void giveWeaponToPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        
        // ArmorStand'ları temizle
        cleanupExistingArmorStands(playerId);

        // Silahı geri ver
        ItemStack weapon = storedWeapons.remove(playerId);
        if (weapon != null) {
            player.getInventory().setItemInMainHand(weapon);
            // Uçma özelliği ver
            player.setAllowFlight(true);
            player.setFlying(true);
            player.sendMessage(ChatColor.GREEN + "» Zaman Kıran'ın gücüyle uçabilirsin!");
        }

        // Efektler
        Location loc = player.getLocation();
        playSound(loc, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.0f, 1.0f);
        loc.getWorld().spawnParticle(
            Particle.CLOUD,
            loc.clone().add(0, 1, 0),
            20, 0.2, 0.2, 0.2, 0.1
        );

        // Temizlik
        weaponLocations.remove(playerId);
        weaponVelocities.remove(playerId);

        player.sendMessage(ChatColor.GREEN + "» Zaman Kıran'ı geri aldın!");
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

    private void createHitEffect(Location location) {
        location.getWorld().spawnParticle(
            Particle.EXPLOSION_NORMAL,
            location,
            10, 0.2, 0.2, 0.2, 0.1
        );
        playSound(location, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.0f);
    }

    private boolean isWeapon(ItemStack item) {
        return item != null && item.hasItemMeta() && 
               item.getItemMeta().hasDisplayName() && 
               item.getItemMeta().getDisplayName().contains("Zaman Kıran");
    }

    public ArmorStand createArmorStand(Location location, boolean isVisible, boolean isGravity) {
        ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class);
        stand.setVisible(false);
        stand.setGravity(true);
        stand.setSmall(false);
        stand.setMarker(false);
        stand.setBasePlate(false);
        stand.setArms(true);
        
        return stand;
    }

    public void playSound(Location location, Sound sound, float volume, float pitch) {
        location.getWorld().playSound(location, sound, volume, pitch);
    }

    // Getter ekle
    public Map<UUID, ArmorStand> getWeaponHolograms() {
        return weaponHolograms;
    }

    @Override
    public double getRemainingCooldown() {
        long currentTime = System.currentTimeMillis();
        long lastUsedTime = this.lastUsed;
        long cooldownMillis = this.cooldown * 1000;
        long remainingMillis = Math.max(0, cooldownMillis - (currentTime - lastUsedTime));
        return remainingMillis / 50.0; // Convert to ticks
    }

    private void handlePickupAttempt(Player player, UUID weaponOwnerId) {
        if (player.getUniqueId().equals(weaponOwnerId)) {
            // Silahın sahibi
            return;
        }
        
        // Takım kontrolü
        TeamSystem teamSystem = ((ZamanKiran) plugin).getTeamSystem();
        Player owner = Bukkit.getPlayer(weaponOwnerId);
        
        if (owner != null && teamSystem.isInTeam(owner, player)) {
            // Takım arkadaşı
            return;
        }
        
        // Silahı alamaz
        player.sendMessage(ChatColor.RED + "» Bu silahı sadece sahibi veya takım arkadaşları alabilir!");
        Location loc = player.getLocation();
        playSound(loc, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        loc.getWorld().spawnParticle(
            Particle.VILLAGER_ANGRY,
            loc.clone().add(0, 2, 0),
            5, 0.2, 0.2, 0.2, 0
        );
    }

    public void dropWeapon(Player player, Location location) {
        UUID playerId = player.getUniqueId();
        
        // Önce eski ArmorStand'leri temizle
        cleanupExistingArmorStands(playerId);
        
        // Silahı sakla
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (isWeapon(weapon)) {
            storedWeapons.put(playerId, weapon.clone());
        }
        
        // Oyuncunun baktığı yönü al
        Vector direction = player.getLocation().getDirection();
        double yaw = Math.atan2(direction.getZ(), direction.getX());
        
        // Düzgün bir konum belirle - Yere yakın olsun
        Location dropLoc = location.clone();
        // RayTrace ile yere olan mesafeyi bul
        RayTraceResult rayTrace = location.getWorld().rayTraceBlocks(
            location,
            new Vector(0, -1, 0),
            2.0
        );
        
        if (rayTrace != null && rayTrace.getHitBlock() != null) {
            dropLoc = rayTrace.getHitPosition().toLocation(location.getWorld());
            dropLoc.add(0, 0.1, 0); // Yerden biraz yukarıda
        } else {
            dropLoc.setY(dropLoc.getBlockY() + 0.1);
        }
        
        // Hologramları oluştur
        ArmorStand hologram = createArmorStand(dropLoc.clone().add(0, 0.3, 0), false, false);
        ArmorStand holder = createArmorStand(dropLoc, false, false);
        
        // Hologram ayarları
        hologram.setCustomName("§b✧ Zaman Kıran §b✧");
        hologram.setCustomNameVisible(true);
        hologram.setGravity(false);
        hologram.setSmall(false);
        
        // Holder ayarları - Gerçekçi pozisyon
        holder.setVisible(false);
        holder.setGravity(false);
        holder.setSmall(false);
        
        // Silahın yönünü ayarla - Oyuncunun baktığı yöne göre
        double angle = Math.toRadians(player.getLocation().getYaw() + 90);
        holder.setRightArmPose(new EulerAngle(Math.PI / 4, angle, 0));
        holder.setRotation(player.getLocation().getYaw(), 0);
        holder.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_PICKAXE));
        
        weaponHolograms.put(playerId, hologram);
        weaponHolders.put(playerId, holder);
        weaponLocations.put(playerId, dropLoc);
        
        // Düşme efekti
        dropLoc.getWorld().spawnParticle(
            Particle.CLOUD,
            dropLoc,
            5, 0.2, 0.1, 0.2, 0.05
        );
        playSound(dropLoc, Sound.BLOCK_CHAIN_BREAK, 0.5f, 1.0f);
    }

    public void castRecursion(Player player) {
        if (isOnCooldown()) {
            player.sendMessage(ChatColor.RED + "» Bu yetenek şu anda kullanılamaz! (" + 
                String.format("%.1f", getRemainingCooldown() / 20.0) + "s)");
            return;
        }

        startSkill();
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection();
        
        new BukkitRunnable() {
            private int step = 0;
            private final int maxSteps = 20;
            private Location currentLoc = start.clone();
            
            @Override
            public void run() {
                if (step >= maxSteps) {
                    cancel();
                    return;
                }
                
                // Portal efekti
                createPortalEffect(currentLoc);
                
                // Her adımda blok fırlatma
                if (step % 2 == 0) {
                    launchBlock(currentLoc, direction);
                }
                
                currentLoc.add(direction);
                step++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
        
        lastUsed = System.currentTimeMillis();
    }

    private void createPortalEffect(Location location) {
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
            double x = Math.cos(angle);
            double z = Math.sin(angle);
            Location particleLoc = location.clone().add(x, 0, z);
            
            location.getWorld().spawnParticle(
                Particle.PORTAL,
                particleLoc,
                2, 0, 0, 0, 0.05
            );
        }
        
        playSound(location, Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 2.0f);
    }

    private void launchBlock(Location location, Vector direction) {
        FallingBlock block = location.getWorld().spawnFallingBlock(
            location,
            Material.OBSIDIAN.createBlockData()
        );
        
        Vector velocity = direction.clone()
            .add(new Vector(
                Math.random() * 0.4 - 0.2,
                Math.random() * 0.4,
                Math.random() * 0.4 - 0.2
            ))
            .multiply(0.5);
        
        block.setVelocity(velocity);
        block.setDropItem(false);
        
        // 2 saniye sonra bloğu yok et
        new BukkitRunnable() {
            @Override
            public void run() {
                if (block.isValid()) {
                    block.remove();
                    block.getLocation().getWorld().spawnParticle(
                        Particle.BLOCK_CRACK,
                        block.getLocation(),
                        20,
                        0.5, 0.5, 0.5,
                        0,
                        Material.OBSIDIAN.createBlockData()
                    );
                }
            }
        }.runTaskLater(plugin, 40L);
    }

    private void startCooldown(Player player) {
        long cooldownTime = 5000; // 5 saniye cooldown
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + cooldownTime);
        player.sendMessage(ChatColor.RED + "» Silah fırlatma yeteneği bekleme süresinde!");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ItemStack weapon = player.getInventory().getItemInMainHand();
        
        if (isWeapon(weapon)) {
            Location dropLoc = player.getLocation();
            player.getInventory().setItemInMainHand(null);
            
            ArmorStand stand = createArmorStand(dropLoc, false, false);
            if (stand != null) {
                stand.setVisible(false);
                stand.setGravity(false);
                stand.setSmall(false);
                stand.getEquipment().setHelmet(weapon);
                stand.setCustomName("§b✧ Zaman Kıran §b✧");
                stand.setCustomNameVisible(true);
                weaponHolders.put(player.getUniqueId(), stand);
            }
        }
    }

    private void startWeaponRecall(Player player, Location weaponLoc) {
        UUID playerId = player.getUniqueId();
        BukkitRunnable currentTask = flyTasks.get(playerId);
        if (currentTask != null) {
            currentTask.cancel();
        }

        // Yeni geri çağırma görevi başlat
        BukkitRunnable recallTask = new BukkitRunnable() {
            private Location currentLoc = weaponLoc.clone();
            private boolean isRecalling = true;

            @Override
            public void run() {
                if (!isRecalling || !player.isSneaking()) {
                    this.cancel();
                    return;
                }

                // Oyuncuya doğru hareket
                Vector toPlayer = player.getLocation().add(0, 1, 0).toVector()
                    .subtract(currentLoc.toVector())
                    .normalize()
                    .multiply(RECALL_SPEED);

                currentLoc.add(toPlayer);

                // ArmorStand'ları güncelle
                ArmorStand hologram = weaponHolograms.get(playerId);
                ArmorStand holder = weaponHolders.get(playerId);
                updateWeaponPosition(hologram, holder, currentLoc, VectorUtils.toVec3d(toPlayer));

                // Efektler
                currentLoc.getWorld().spawnParticle(
                    Particle.PORTAL,
                    currentLoc,
                    5, 0.2, 0.2, 0.2, 0.1
                );

                // Oyuncuya yakınsa al
                if (currentLoc.distance(player.getLocation()) < 1.5) {
                    giveWeaponToPlayer(player);
                    this.cancel();
                }
            }
        };

        recallTask.runTaskTimer(plugin, 0L, 1L);
        flyTasks.put(playerId, recallTask);

        // Geri çağırma efektleri
        playSound(weaponLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 2.0f);
        weaponLoc.getWorld().spawnParticle(
            Particle.PORTAL,
            weaponLoc,
            20, 0.5, 0.5, 0.5, 0.1
        );
    }
} 