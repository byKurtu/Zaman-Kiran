package org.SolarSystem.zamanKiran;

import org.SolarSystem.zamanKiran.commands.*;
import org.SolarSystem.zamanKiran.gui.*;
import org.SolarSystem.zamanKiran.listeners.*;
import org.SolarSystem.zamanKiran.skills.*;
import org.SolarSystem.zamanKiran.items.ItemManager;
import org.SolarSystem.zamanKiran.systems.HealthSystem;
import org.SolarSystem.zamanKiran.systems.TeamSystem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ZamanKiran extends JavaPlugin implements Listener {
    private static ZamanKiran instance;
    private final Map<Integer, Skill> skills = new HashMap<>();
    private final Map<UUID, Map<Integer, Skill>> playerSkills = new HashMap<>();
    private final ItemManager itemManager;
    private WeaponSelectionGUI weaponGUI;
    private SkillSelectionGUI skillGUI;
    
    // Skills
    private TimeStop timeStop;
    private SoulCage soulCage;
    private ShadowInvasion shadowInvasion;
    private ShadowCouncil shadowCouncil;
    private DimensionGate dimensionGate;
    private MassCurse massCurse;
    private HealthSystem healthSystem;
    private TeamSystem teamSystem;
    private ChronoSphere chronoSphere;
    private WaveCutting waveCutting;
    private ChainLightning chainLightning;
    private ThrowableWeapon throwableWeapon;
    private Portal portal;
    private PentagramRitual pentagramRitual;
    private TimeEcho timeEcho;
    private RealityShatter realityShatter;
    private TemporalStorm temporalStorm;
    private TimeWeaver timeWeaver;

    public ZamanKiran() {
        this.itemManager = new ItemManager(this);
    }

    @Override
    public void onEnable() {
        // Initialize plugin instance
        instance = this;

        saveDefaultConfig();
        
        // Initialize systems
        healthSystem = new HealthSystem(this);
        teamSystem = new TeamSystem(this);
        
        // Initialize all skills
        chronoSphere = new ChronoSphere(this);
        timeEcho = new TimeEcho(this);
        pentagramRitual = new PentagramRitual(this);
        
        soulCage = new SoulCage(this);
        realityShatter = new RealityShatter(this);
        shadowInvasion = new ShadowInvasion(this);
        
        waveCutting = new WaveCutting(this);
        temporalStorm = new TemporalStorm(this);
        throwableWeapon = new ThrowableWeapon(this);
        
        portal = new Portal(this);
        timeWeaver = new TimeWeaver(this);
        chainLightning = new ChainLightning(this);
        
        shadowCouncil = new ShadowCouncil(this);
        dimensionGate = new DimensionGate(this);
        massCurse = new MassCurse(this);

        // Initialize LastScream ultimate skill
        new LastScream(this);

        // Initialize GUIs
        skillGUI = new SkillSelectionGUI(this);
        weaponGUI = new WeaponSelectionGUI(this);

        // Initialize ItemManager
        itemManager.init();
        
        // Initialize skills map with all skills
        initializeSkills();

        // Register commands
        getCommand("skills").setExecutor(new SkillsCommand(this));
        getCommand("zamankirangui").setExecutor(new ZamanKiranGUICommand(this));
        getCommand("zamankiran").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player && args.length > 0 && args[0].equalsIgnoreCase("ver")) {
                Player player = (Player) sender;
                ItemStack weapon = itemManager.createWeapon(player);
                player.getInventory().addItem(weapon);
                healthSystem.initializeHealth(player);
                initializePlayerSkills(player);
                player.sendMessage(ChatColor.GREEN + "Zaman Kıran silahı verildi!");
                return true;
            }
            return false;
        });

        // Register curse commands
        CurseCommands curseCommands = new CurseCommands(this);
        getCommand("lanet").setExecutor(curseCommands);
        getCommand("lanetkaldir").setExecutor(curseCommands);
        getCommand("lanetler").setExecutor(curseCommands);
        getCommand("lanetgrubu").setExecutor(curseCommands);

        // Register events
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        startActionBarTask();

        getLogger().info("ZamanKiran plugin has been enabled!");
    }

    private void initializeSkills() {
        // Zaman Yetenekleri (Slot 0)
        skills.put(0, chronoSphere);  // Primary time skill
        skills.put(4, timeEcho);      // Secondary time skill
        skills.put(8, pentagramRitual); // Ultimate time skill
        
        // Ruh Yetenekleri (Slot 1)
        skills.put(1, soulCage);       // Primary soul skill
        skills.put(5, realityShatter); // Secondary soul skill
        skills.put(9, shadowInvasion); // Ultimate soul skill
        
        // Savaş Yetenekleri (Slot 2)
        skills.put(2, waveCutting);     // Primary combat skill
        skills.put(6, temporalStorm);   // Secondary combat skill
        skills.put(10, throwableWeapon); // Ultimate combat skill
        
        // Elementel Yetenekler (Slot 3)
        skills.put(3, portal);          // Primary elemental skill
        skills.put(7, timeWeaver);      // Secondary elemental skill
        skills.put(11, chainLightning); // Ultimate elemental skill
        
        // Register all skills as event listeners
        for (Skill skill : skills.values()) {
            if (skill instanceof Listener) {
                getServer().getPluginManager().registerEvents((Listener) skill, this);
            }
        }
    }

    public void initializePlayerSkills(Player player) {
        Map<Integer, Skill> playerSkillMap = new HashMap<>(skills);
        playerSkills.put(player.getUniqueId(), playerSkillMap);

        DimensionGate dimensionGate = new DimensionGate(this);
        MassCurse massCurse = new MassCurse(this);
        
        playerSkillMap.put(6, dimensionGate);
        playerSkillMap.put(7, massCurse);
        
        // Register events
        getServer().getPluginManager().registerEvents(dimensionGate, this);
        getServer().getPluginManager().registerEvents(massCurse, this);
    }

    private void startActionBarTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers()) {
                    ItemStack mainHand = player.getInventory().getItemInMainHand();
                    if (itemManager.isWeapon(mainHand)) {
                        int selectedSlot = skillGUI.getSelectedSlot(player);
                        Map<Integer, Skill> skills = playerSkills.get(player.getUniqueId());
                        if (skills != null) {
                            if (player.isSneaking()) {
                                // Shift yetenekleri
                                String actionBar = formatShiftSkills(player, skills);
                                sendActionBar(player, actionBar);
                            } else {
                                // Normal yetenekler
                                String actionBar = formatNormalSkills(player, skills);
                                sendActionBar(player, actionBar);
                            }
                        }
                    } else {
                        // Yakındaki silahları kontrol et
                        checkNearbyWeapons(player);
                    }
                }
            }
        }.runTaskTimer(this, 0L, 5L);
    }

    private String formatNormalSkills(Player player, Map<Integer, Skill> skills) {
        StringBuilder actionBar = new StringBuilder();
        int selectedSlot = skillGUI.getSelectedSlot(player);
        Skill skill = skills.get(selectedSlot);
        
        // Sol tık yeteneği
        actionBar.append("§6Sol Tık: ");
        if (skill != null) {
            boolean canUse = !skill.isOnCooldown();
            actionBar.append(skill.getName())
                    .append(" ")
                    .append(canUse ? "§a✔" : "§c✘");
            
            if (!canUse) {
                actionBar.append(" §7(")
                        .append(formatCooldown(skill))
                        .append("s)");
            }
        }
        
        // Sağ tık yeteneği
        actionBar.append(" §8| §6Sağ Tık: ");
        if (skill != null) {
            String rightClickAction = getRightClickAction(skill, selectedSlot);
            actionBar.append(rightClickAction);
        }
        
        return actionBar.toString();
    }

    private String getRightClickAction(Skill skill, int selectedSlot) {
        if (skill instanceof ShadowCouncil) {
            return "Enerji Topu";
        } else if (skill instanceof ThrowableWeapon) {
            return "Silah Fırlat";
        } else if (skill instanceof Portal) {
            return "Portal Aç";
        } else if (skill instanceof ChainLightning) {
            return "Zincir Yıldırım";
        }
        
        // Slot based actions
        switch (selectedSlot) {
            case 0: // ChronoSphere
                return "Zaman Durdur";
            case 1: // SoulCage
                return "Ruh Kafesi";
            case 2: // WaveCutting
                return "Dalga Kes";
            case 3: // Portal
                return "Portal Aç";
            case 4: // TimeEcho
                return "Zaman Yankısı";
            case 5: // RealityShatter
                return "Gerçeklik Kır";
            case 6: // TemporalStorm
                return "Zaman Fırtınası";
            case 7: // TimeWeaver
                return "Zaman Örgüsü";
            case 8: // PentagramRitual
                return "Pentagram Ritüeli";
            case 9: // ShadowInvasion
                return "Gölge İstilası";
            case 10: // ThrowableWeapon
                return "Silah Fırlat";
            case 11: // ChainLightning
                return "Zincir Yıldırım";
            default:
                return "Yetenek Yok";
        }
    }

    private String formatShiftSkills(Player player, Map<Integer, Skill> skills) {
        StringBuilder actionBar = new StringBuilder();
        int selectedSlot = skillGUI.getSelectedSlot(player);
        Skill skill = skills.get(selectedSlot);
        
        actionBar.append("§5Shift + Sol Tık: ");
        if (skill instanceof ShadowCouncil) {
            actionBar.append("Konseyi Dağıt");
        } else {
            // Slot based shift actions
            switch (selectedSlot) {
                case 0: // ChronoSphere
                    actionBar.append("Pentagram Ritüeli");
                    break;
                case 1: // SoulCage
                    actionBar.append("Ruh Kafesi Dağıt");
                    break;
                case 2: // WaveCutting
                    actionBar.append("Dalga Modu Değiştir");
                    break;
                case 3: // Portal
                    actionBar.append("Portal Kapat");
                    break;
                case 4: // TimeEcho
                    actionBar.append("Yankı Dağıt");
                    break;
                case 5: // RealityShatter
                    actionBar.append("Gerçekliği Onar");
                    break;
                case 6: // TemporalStorm
                    actionBar.append("Fırtına Durdur");
                    break;
                case 7: // TimeWeaver
                    actionBar.append("Örgüyü Çöz");
                    break;
                case 8: // PentagramRitual
                    actionBar.append("Ritüeli Sonlandır");
                    break;
                case 9: // ShadowInvasion
                    actionBar.append("Gölgeleri Dağıt");
                    break;
                case 10: // ThrowableWeapon
                    actionBar.append("Mod Değiştir");
                    break;
                case 11: // ChainLightning
                    actionBar.append("Yıldırım Zincirini Kes");
                    break;
                default:
                    actionBar.append("Yetenek Yok");
            }
        }
        
        return actionBar.toString();
    }

    private String formatCooldown(Skill skill) {
        // Cooldown süresini formatla
        return String.format("%.1f", skill.getRemainingCooldown() / 20.0);
    }

    private void checkNearbyWeapons(Player player) {
        Location playerLoc = player.getLocation();
        World world = player.getWorld();
        
        world.getNearbyEntities(playerLoc, 5, 5, 5).forEach(entity -> {
            if (entity instanceof ArmorStand) {
                ArmorStand stand = (ArmorStand) entity;
                if (stand.getCustomName() != null && stand.getCustomName().contains("Zaman Kıran")) {
                    // Silahın sahibini bul
                    String ownerName = "???";
                    for (Map.Entry<UUID, ArmorStand> entry : throwableWeapon.getWeaponHolograms().entrySet()) {
                        if (entry.getValue().equals(stand)) {
                            Player owner = Bukkit.getPlayer(entry.getKey());
                            if (owner != null) {
                                ownerName = owner.getName();
                            }
                            break;
                        }
                    }
                    
                    String message = String.format("§b✧ %s'nin Zaman Kıran'ı §8| ", ownerName);
                    if (canPickupWeapon(player, stand)) {
                        message += "§aShift ile al";
                    } else {
                        message += "§cBu silahı alamazsın!";
                    }
                    sendActionBar(player, message);
                }
            }
        });
    }

    private boolean canPickupWeapon(Player player, ArmorStand stand) {
        // Silah alma koşullarını kontrol et
        // Örneğin: Sadece sahibi veya takım arkadaşları alabilir
        return true; // Şimdilik herkese izin ver
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        if (getItemManager().isWeapon(item)) {
            Action action = event.getAction();
            boolean isLeftClick = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
            boolean isRightClick = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
            boolean isShiftClick = player.isSneaking();

            event.setCancelled(true);
            int selectedSlot = getSkillGUI().getSelectedSlot(player);
            
            if (selectedSlot >= 0) {
                Map<Integer, Skill> playerSkillMap = getPlayerSkills(player);
                if (playerSkillMap != null) {
                    Skill selectedSkill = playerSkillMap.get(selectedSlot);
                    if (selectedSkill != null) {
                        if (isLeftClick) {
                            if (!isShiftClick) {
                                // Normal left click - Cast primary skill
                                selectedSkill.cast(player);
                            } else {
                                // Shift + Left click - Cast shift skill
                                if (selectedSkill instanceof ShadowCouncil) {
                                    ((ShadowCouncil) selectedSkill).cast(player);
                                } else {
                                    handleShiftLeftClick(player, selectedSlot);
                                }
                            }
                        } else if (isRightClick && !isShiftClick) {
                            // Normal right click - Cast secondary skill
                            handleRightClick(player, selectedSkill, selectedSlot);
                        }
                        // Shift + Right click is reserved for weapon selection GUI
                        else if (isRightClick && isShiftClick) {
                            weaponGUI.openGUI(player);
                        }
                    }
                }
            }
        }
    }

    private void handleShiftLeftClick(Player player, int selectedSlot) {
        Map<Integer, Skill> skills = getPlayerSkills(player);
        if (skills == null) return;

        switch (selectedSlot) {
            case 0: // ChronoSphere
                if (pentagramRitual != null) pentagramRitual.cast(player);
                break;
            case 1: // SoulCage
                if (soulCage != null) soulCage.cast(player);
                break;
            case 2: // WaveCutting
                if (waveCutting != null) waveCutting.cast(player);
                break;
            case 3: // Portal
                if (portal != null) portal.cast(player);
                break;
            case 4: // TimeEcho
                if (timeEcho != null) timeEcho.cast(player);
                break;
            case 5: // RealityShatter
                if (realityShatter != null) realityShatter.cast(player);
                break;
            case 6: // TemporalStorm
                if (temporalStorm != null) temporalStorm.cast(player);
                break;
            case 7: // TimeWeaver
                if (timeWeaver != null) timeWeaver.cast(player);
                break;
            case 8: // PentagramRitual
                if (pentagramRitual != null) pentagramRitual.cast(player);
                break;
            case 9: // ShadowInvasion
                if (shadowInvasion != null) shadowInvasion.cast(player);
                break;
            case 10: // ThrowableWeapon
                if (throwableWeapon != null) throwableWeapon.cast(player);
                break;
            case 11: // ChainLightning
                if (chainLightning != null) chainLightning.cast(player);
                break;
        }
    }

    private void handleRightClick(Player player, Skill skill, int selectedSlot) {
        if (skill instanceof ShadowCouncil) {
            ((ShadowCouncil) skill).cast(player);
        } else if (skill instanceof ThrowableWeapon) {
            ((ThrowableWeapon) skill).cast(player);
        } else if (skill instanceof Portal) {
            ((Portal) skill).cast(player);
        } else if (skill instanceof ChainLightning) {
            ((ChainLightning) skill).cast(player);
        } else {
            switch (selectedSlot) {
                case 0: // ChronoSphere
                    chronoSphere.cast(player);
                    break;
                case 1: // SoulCage
                    soulCage.cast(player);
                    break;
                case 2: // WaveCutting
                    waveCutting.cast(player);
                    break;
                case 4: // TimeEcho
                    timeEcho.cast(player);
                    break;
                case 5: // RealityShatter
                    realityShatter.cast(player);
                    break;
                case 6: // TemporalStorm
                    temporalStorm.cast(player);
                    break;
                case 7: // TimeWeaver
                    timeWeaver.cast(player);
                    break;
                case 8: // PentagramRitual
                    pentagramRitual.cast(player);
                    break;
                case 9: // ShadowInvasion
                    shadowInvasion.cast(player);
                    break;
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (itemManager.isWeapon(item) && event.getRightClicked() instanceof Player) {
            Player target = (Player) event.getRightClicked();
            if (player.getInventory().getHeldItemSlot() == 8) {
                teamSystem.addTeamMember(player, target);
                player.sendMessage(ChatColor.GREEN + target.getName() + " takıma eklendi!");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        if (event.getView().getTitle().startsWith(ChatColor.DARK_PURPLE + "Zaman Kıran Yetenekleri")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            skillGUI.handleClick(player, event.getSlot());
        } else if (event.getView().getTitle().equals(ChatColor.GOLD + "Zaman Kıran Silahları")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            weaponGUI.handleClick(event);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();

        if (itemManager.isWeapon(item)) {
            event.setCancelled(true);
            Location dropLoc = player.getLocation().add(0, 1, 0);
            throwableWeapon.dropWeapon(player, dropLoc);
            player.getInventory().setItemInMainHand(null);
            // Uçma özelliğini kaldır
            player.setAllowFlight(false);
            player.setFlying(false);
            player.sendMessage(ChatColor.RED + "» Zaman Kıran'ı bıraktığın için uçma yeteneğini kaybettin!");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // SoulCage yeteneği için blok kırma kontrolü
        if (soulCage != null) {
            soulCage.handleBlockBreak(player, block);
        }
    }

    @Override
    public void onDisable() {
        if (healthSystem != null) {
            getServer().getOnlinePlayers().forEach(player -> 
                healthSystem.removeBossBar(player));
        }
        
        if (playerSkills != null) {
            playerSkills.values().forEach(skillMap -> 
                skillMap.values().forEach(skill -> {
                    if (skill instanceof ChronoSphere) {
                        ((ChronoSphere) skill).endSkill();
                    }
                }));
        }
        
        getLogger().info("ZamanKiran plugin has been disabled!");
    }

    public TeamSystem getTeamSystem() {
        return teamSystem;
    }

    public SkillSelectionGUI getSkillGUI() {
        return skillGUI;
    }

    public static ZamanKiran getInstance() {
        return instance;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public WeaponSelectionGUI getWeaponGUI() {
        return weaponGUI;
    }

    public Map<Integer, Skill> getSkills() {
        return skills;
    }

    public Map<Integer, Skill> getPlayerSkills(Player player) {
        return playerSkills.get(player.getUniqueId());
    }
} 