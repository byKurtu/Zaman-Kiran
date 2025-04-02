package org.SolarSystem.zamanKiran.skills;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import org.SolarSystem.zamanKiran.ZamanKiran;
import org.SolarSystem.zamanKiran.skills.ThrowableWeapon;

import java.util.*;

public class Portal extends Skill {
    private static final int PORTAL_DURATION = 10 * 20;
    private final Map<Player, Location> portalLocations = new HashMap<>();
    private final Map<Location, List<ArmorStand>> portalDisplays = new HashMap<>();
    private final Map<UUID, PortalCreationState> creationStates = new HashMap<>();
    private final Map<Location, Location> linkedPortals = new HashMap<>();
    private final Random random = new Random();

    private static class PortalCreationState {
        World selectedWorld;
        Player selectedPlayer;
        Location selectedLocation;
        PortalType type = PortalType.PLAYER;
        CoordinateType coordinateType = null;
        double x, y, z;
        boolean xSet, ySet, zSet;

        enum PortalType {
            PLAYER,
            LOCATION,
            WORLD
        }

        enum CoordinateType {
            X, Y, Z
        }
    }

    public Portal(Plugin plugin) {
        super(plugin, "Portal", 30, 75);
    }

    @Override
    public void cast(Player caster) {
        startSkill();
        openPortalMainMenu(caster);
    }

    @EventHandler
    public void onAnvilClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;

        Player player = (Player) event.getWhoClicked();
        PortalCreationState state = creationStates.get(player.getUniqueId());
        if (state == null || state.coordinateType == null) return;

        String input = event.getCurrentItem().getItemMeta().getDisplayName();
        try {
            double value = Double.parseDouble(input);
            switch (state.coordinateType) {
                case X:
                    state.x = value;
                    state.xSet = true;
                    openLocationInput(player);
                    break;
                case Y:
                    state.y = value;
                    state.ySet = true;
                    openLocationInput(player);
                    break;
                case Z:
                    state.z = value;
                    state.zSet = true;
                    openLocationInput(player);
                    break;
            }

            if (state.xSet && state.ySet && state.zSet) {
                Location loc = new Location(player.getWorld(), state.x, state.y, state.z);
                createPortal(player, loc);
            }
        } catch (NumberFormatException ignored) {
            player.sendMessage(ChatColor.RED + "Lütfen geçerli bir sayı girin!");
        }
        event.setCancelled(true);
    }

    private void createPortal(Player player, Location destination) {
        Location playerLoc = player.getLocation();
        ItemStack weapon = player.getInventory().getItemInMainHand().clone();
        player.getInventory().setItemInMainHand(null);

        // İlk snowball ve yükselme animasyonu
        Location startLoc = playerLoc.clone().add(0, 3, 0);
        new BukkitRunnable() {
            double height = 0;
            @Override
            public void run() {
                if (height >= 3) {
                    this.cancel();
                    createPortalEffect(startLoc, destination, weapon);
                    return;
                }
                
                Location spawnLoc = playerLoc.clone().add(0, height, 0);
                Snowball snowball = (Snowball) playerLoc.getWorld().spawnEntity(spawnLoc, EntityType.SNOWBALL);
                snowball.setVelocity(new Vector(0, 0.5, 0));
                height += 0.5;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void createPortalEffect(Location location, Location destination, ItemStack weapon) {
        List<ArmorStand> portalStands = new ArrayList<>();

        // Portal camları için 24 ArmorStand
        for (int i = 0; i < 24; i++) {
            ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setSmall(false);
            stand.setMarker(true);
            stand.getEquipment().setHelmet(new ItemStack(Material.PURPLE_STAINED_GLASS));
            portalStands.add(stand);
        }

        // Silah fırlatma
        if (weapon != null) {
            Location weaponLoc = location.clone().add(0, 2, 0);
            Item droppedItem = weaponLoc.getWorld().dropItem(weaponLoc, weapon);
            droppedItem.setVelocity(new Vector(0, 0.5, 0));
            droppedItem.setGlowing(true);
            droppedItem.setCustomName("§b✧ Zaman Kıran §b✧");
            droppedItem.setCustomNameVisible(true);
        }

        // Karşı portal oluştur
        if (destination != null) {
            linkedPortals.put(location, destination);
            // Karşı portal efekti
            createPortalEffect(destination, null, null);
        }

        // Portal efekti
        new BukkitRunnable() {
            double angle = 0;
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ > PORTAL_DURATION || !isActive) {
                    closePortal(location, portalStands);
                    this.cancel();
                    return;
                }

                // Portal camları dönme efekti - tam dikey
                for (int i = 0; i < portalStands.size(); i++) {
                    ArmorStand stand = portalStands.get(i);
                    if (stand.isValid()) {
                        double portalAngle = angle + (2 * Math.PI * i) / portalStands.size();
                        double radius = 1.5;
                        double height = 3.0 * Math.sin(portalAngle); // Tam dikey hareket
                        
                        Location portalLoc = location.clone().add(
                            Math.cos(portalAngle) * radius,
                            height + 1.5, // Yükseklik ayarı
                            Math.sin(portalAngle) * radius
                        );
                        stand.teleport(portalLoc);
                        stand.setRotation((float) Math.toDegrees(portalAngle), 90); // Tam dikey rotasyon
                    }
                }

                // Parçacık ve ses efektleri
                if (ticks % 5 == 0) {
                    location.getWorld().spawnParticle(
                        Particle.PORTAL,
                        location.clone().add(0, 1.5, 0),
                        50, 1.5, 2.0, 1.5, 0
                    );
                    
                    if (random.nextInt(3) == 0) {
                        location.getWorld().playSound(
                            location,
                            Sound.BLOCK_PORTAL_AMBIENT,
                            0.5f,
                            1.5f
                        );
                    }
                }

                angle += Math.PI / 16;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        portalDisplays.put(location, portalStands);
    }

    private void closePortal(Location location, List<ArmorStand> stands) {
        new BukkitRunnable() {
            double radius = 1.5;
            double angle = 0;
            double inwardSpeed = 0.1;
            double height = 3.0;

            @Override
            public void run() {
                if (radius <= 0 || stands.isEmpty() || height <= 0) {
                    stands.forEach(Entity::remove);
                    portalDisplays.remove(location);
                    linkedPortals.remove(location);
                    this.cancel();
                    return;
                }

                // Dikey spiral hareket - tam dikey
                for (int i = 0; i < stands.size(); i++) {
                    ArmorStand stand = stands.get(i);
                    if (stand != null && stand.isValid()) {
                        double standAngle = angle + (2 * Math.PI * i) / stands.size();
                        
                        Location spiralLoc = location.clone().add(
                            Math.cos(standAngle) * radius,
                            height + Math.sin(standAngle) * 0.5, // Tam dikey spiral
                            Math.sin(standAngle) * radius
                        );
                        
                        stand.teleport(spiralLoc);
                        stand.setRotation((float) Math.toDegrees(standAngle), 90);
                    }
                }

                // Parçacık efekti
                location.getWorld().spawnParticle(
                    Particle.PORTAL,
                    location.clone().add(0, height, 0),
                    20, radius, 0.5, radius, 0
                );

                // Ses efekti
                if (random.nextInt(3) == 0) {
                    location.getWorld().playSound(
                        location,
                        Sound.BLOCK_PORTAL_TRIGGER,
                        0.5f,
                        (float)(0.5f + (1.5f - radius))
                    );
                }

                radius -= inwardSpeed;
                height -= inwardSpeed * 0.5;
                angle += Math.PI / 8;
                inwardSpeed += 0.01;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void dropWeapon(Location location, ItemStack weapon) {
        ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setSmall(false);
        stand.getEquipment().setHelmet(weapon);
        stand.setCustomName("§b✧ Zaman Kıran §b✧");
        stand.setCustomNameVisible(true);
    }

    private void openPortalMainMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§5§lPortal Oluştur");
        UUID playerId = player.getUniqueId();
        creationStates.put(playerId, new PortalCreationState());

        // Oyuncu Seçme Butonu
        ItemStack playerButton = createGuiItem(Material.PLAYER_HEAD, 
            "§b§lOyuncuya Portal", 
            Arrays.asList(
                "§7Başka bir oyuncuya portal aç",
                "§8• §fTıkla ve oyuncu seç"
            )
        );
        gui.setItem(11, playerButton);

        // Koordinat Seçme Butonu
        ItemStack locationButton = createGuiItem(Material.COMPASS,
            "§e§lKoordinata Portal",
            Arrays.asList(
                "§7Belirli bir konuma portal aç",
                "§8• §fTıkla ve koordinat gir"
            )
        );
        gui.setItem(13, locationButton);

        // Dünya Seçme Butonu
        ItemStack worldButton = createGuiItem(Material.GRASS_BLOCK,
            "§a§lDünyaya Portal",
            Arrays.asList(
                "§7Başka bir dünyaya portal aç",
                "§8• §fTıkla ve dünya seç"
            )
        );
        gui.setItem(15, worldButton);

        // Dolgu itemleri
        fillEmptySlots(gui);
        player.openInventory(gui);
    }

    private void openPlayerSelectionMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§b§lOyuncu Seç");
        int slot = 0;

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target != player) {
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                meta.setOwningPlayer(target);
                meta.setDisplayName("§b" + target.getName());
                meta.setLore(Arrays.asList(
                    "§7Oyuncuya portal aç",
                    "§8• §fTıkla ve seç",
                    "",
                    "§8▪ §7Dünya: §f" + target.getWorld().getName(),
                    "§8▪ §7Konum: §f" + formatLocation(target.getLocation())
                ));
                head.setItemMeta(meta);
                gui.setItem(slot++, head);
            }
        }

        // Geri Dönüş Butonu
        ItemStack backButton = createGuiItem(Material.BARRIER,
            "§c§lGeri Dön",
            Collections.singletonList("§7Ana menüye dön")
        );
        gui.setItem(49, backButton);

        fillEmptySlots(gui);
        player.openInventory(gui);
    }

    private void openWorldSelectionMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§a§lDünya Seç");
        int slot = 10;

        // Normal Dünya
        ItemStack normalWorld = createGuiItem(Material.GRASS_BLOCK,
            "§a§lNormal Dünya",
            Arrays.asList(
                "§7Normal dünyaya portal aç",
                "§8• §fTıkla ve seç",
                "",
                "§8▪ §7İsim: §fworld"
            )
        );
        gui.setItem(slot++, normalWorld);

        // Nether
        ItemStack netherWorld = createGuiItem(Material.NETHERRACK,
            "§c§lNether",
            Arrays.asList(
                "§7Nether dünyasına portal aç",
                "§8• §fTıkla ve seç",
                "",
                "§8▪ §7İsim: §fworld_nether"
            )
        );
        gui.setItem(slot++, netherWorld);

        // End
        ItemStack endWorld = createGuiItem(Material.END_STONE,
            "§5§lEnd",
            Arrays.asList(
                "§7End dünyasına portal aç",
                "§8• §fTıkla ve seç",
                "",
                "§8▪ §7İsim: §fworld_the_end"
            )
        );
        gui.setItem(slot++, endWorld);

        // Geri Dönüş Butonu
        ItemStack backButton = createGuiItem(Material.BARRIER,
            "§c§lGeri Dön",
            Collections.singletonList("§7Ana menüye dön")
        );
        gui.setItem(22, backButton);

        fillEmptySlots(gui);
        player.openInventory(gui);
    }

    private void openLocationInput(Player player) {
        Inventory gui = Bukkit.createInventory(null, InventoryType.HOPPER, "§e§lKoordinat Gir");
        UUID playerId = player.getUniqueId();
        creationStates.get(playerId).coordinateType = PortalCreationState.CoordinateType.X;

        ItemStack xInput = createGuiItem(Material.RED_CONCRETE,
            "§c§lX Koordinatı",
            Arrays.asList(
                "§7X koordinatını gir",
                "§8• §fSohbete sayı yaz"
            )
        );
        gui.setItem(0, xInput);

        ItemStack yInput = createGuiItem(Material.GREEN_CONCRETE,
            "§a§lY Koordinatı",
            Arrays.asList(
                "§7Y koordinatını gir",
                "§8• §fSohbete sayı yaz"
            )
        );
        gui.setItem(1, yInput);

        ItemStack zInput = createGuiItem(Material.BLUE_CONCRETE,
            "§b§lZ Koordinatı",
            Arrays.asList(
                "§7Z koordinatını gir",
                "§8• §fSohbete sayı yaz"
            )
        );
        gui.setItem(2, zInput);

        ItemStack confirmButton = createGuiItem(Material.LIME_CONCRETE,
            "§a§lOnayla",
            Arrays.asList(
                "§7Koordinatları onayla",
                "§8• §fTıkla ve portal aç"
            )
        );
        gui.setItem(4, confirmButton);

        player.openInventory(gui);
    }

    private String formatLocation(Location loc) {
        return String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
    }

    private ItemStack createGuiItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void fillEmptySlots(Inventory inventory) {
        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", Collections.emptyList());
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location playerLoc = player.getLocation();

        for (Map.Entry<Location, Location> entry : linkedPortals.entrySet()) {
            Location portalLoc = entry.getKey();
            Location destLoc = entry.getValue();

            if (portalLoc.getWorld().equals(playerLoc.getWorld())) {
                double distance = portalLoc.distance(playerLoc);
                if (distance <= 1.5) {
                    // Teleport öncesi efektler
                    player.playSound(playerLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    playerLoc.getWorld().spawnParticle(Particle.PORTAL, playerLoc, 50, 0.5, 0.5, 0.5, 0.5);
                    
                    // Teleport
                    Location safeDest = findSafeLocation(destLoc);
                    player.teleport(safeDest);
                    
                    // Varış efektleri
                    player.playSound(safeDest, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    safeDest.getWorld().spawnParticle(Particle.PORTAL, safeDest, 50, 0.5, 0.5, 0.5, 0.5);
                    break;
                }
            }
        }
    }

    private Location findSafeLocation(Location loc) {
        Location safe = loc.clone();
        while (!safe.getBlock().getType().isAir() && safe.getY() < loc.getWorld().getMaxHeight()) {
            safe.add(0, 1, 0);
        }
        return safe;
    }

    public void handleInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        // Ana Portal Menüsü
        if (title.equals("§5§lPortal Oluştur")) {
            switch (clicked.getType()) {
                case PLAYER_HEAD:
                    openPlayerSelectionMenu(player);
                    break;
                case COMPASS:
                    openLocationInput(player);
                    break;
                case GRASS_BLOCK:
                    openWorldSelectionMenu(player);
                    break;
            }
            return;
        }

        // Oyuncu Seçme Menüsü
        if (title.equals("§b§lOyuncu Seç")) {
            if (clicked.getType() == Material.BARRIER) {
                openPortalMainMenu(player);
                return;
            }
            if (clicked.getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = (SkullMeta) clicked.getItemMeta();
                if (meta != null && meta.getOwningPlayer() != null) {
                    Player target = Bukkit.getPlayer(meta.getOwningPlayer().getUniqueId());
                    if (target != null && target.isOnline()) {
                        createPortal(player, target.getLocation());
                        player.closeInventory();
                    }
                }
            }
            return;
        }

        // Dünya Seçme Menüsü
        if (title.equals("§a§lDünya Seç")) {
            if (clicked.getType() == Material.BARRIER) {
                openPortalMainMenu(player);
                return;
            }

            World targetWorld = null;
            Location spawnLoc = null;

            switch (clicked.getType()) {
                case GRASS_BLOCK:
                    targetWorld = Bukkit.getWorld("world");
                    break;
                case NETHERRACK:
                    targetWorld = Bukkit.getWorld("world_nether");
                    break;
                case END_STONE:
                    targetWorld = Bukkit.getWorld("world_the_end");
                    break;
            }

            if (targetWorld != null) {
                spawnLoc = targetWorld.getSpawnLocation();
                createPortal(player, spawnLoc);
                player.closeInventory();
            }
            return;
        }

        // Koordinat Girme Menüsü
        if (title.equals("§e§lKoordinat Gir")) {
            PortalCreationState state = creationStates.get(player.getUniqueId());
            if (state == null) return;

            switch (clicked.getType()) {
                case RED_CONCRETE:
                    state.coordinateType = PortalCreationState.CoordinateType.X;
                    break;
                case GREEN_CONCRETE:
                    state.coordinateType = PortalCreationState.CoordinateType.Y;
                    break;
                case BLUE_CONCRETE:
                    state.coordinateType = PortalCreationState.CoordinateType.Z;
                    break;
                case LIME_CONCRETE:
                    if (state.xSet && state.ySet && state.zSet) {
                        Location loc = new Location(player.getWorld(), state.x, state.y, state.z);
                        createPortal(player, loc);
                        player.closeInventory();
                    } else {
                        player.sendMessage(ChatColor.RED + "» Lütfen önce tüm koordinatları girin!");
                    }
                    break;
            }
        }
    }
} 