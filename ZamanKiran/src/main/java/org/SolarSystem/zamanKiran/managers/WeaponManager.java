package org.SolarSystem.zamanKiran.managers;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;

public class WeaponManager {
    private static WeaponManager instance;
    private final Plugin plugin;
    private final Map<UUID, ItemStack> playerWeapons = new HashMap<>();
    private final Map<String, WeaponData> registeredWeapons = new HashMap<>();

    private WeaponManager(Plugin plugin) {
        this.plugin = plugin;
        initializeDefaultWeapons();
    }

    public static WeaponManager getInstance(Plugin plugin) {
        if (instance == null) {
            instance = new WeaponManager(plugin);
        }
        return instance;
    }

    private void initializeDefaultWeapons() {
        registerWeapon("zaman_kiran", new WeaponData(
            Material.DIAMOND_PICKAXE,
            "§b✧ Zaman Kıran §b✧",
            Arrays.asList(
                "§8• §7Efsanevi bir silah...",
                "§8• §cHasar: §4❁ 9.200.000",
                "§8• §bÖzellikler:",
                "§8  ⦿ §eDiken X",
                "§8  ⦿ §eKeskinlik X",
                "§8  ⦿ §eAteş Koruması X",
                "§8  ⦿ §eKırılmazlık X"
            ),
            9200000.0
        ));
    }

    public ItemStack createWeapon(Player player, String weaponId) {
        WeaponData data = registeredWeapons.get(weaponId);
        if (data == null) return null;

        ItemStack weapon = new ItemStack(data.material);
        ItemMeta meta = weapon.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(data.name);
            meta.setLore(data.lore);
            
            meta.addEnchant(Enchantment.DAMAGE_ALL, 10, true);
            meta.addEnchant(Enchantment.THORNS, 10, true);
            meta.addEnchant(Enchantment.FIRE_ASPECT, 10, true);
            meta.addEnchant(Enchantment.DURABILITY, 10, true);
            
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            
            weapon.setItemMeta(meta);
        }

        playerWeapons.put(player.getUniqueId(), weapon);
        return weapon;
    }

    public boolean isWeapon(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && 
               registeredWeapons.values().stream()
                   .anyMatch(data -> meta.getDisplayName().equals(data.name));
    }

    public void registerWeapon(String id, WeaponData data) {
        registeredWeapons.put(id, data);
    }

    public WeaponData getWeaponData(String id) {
        return registeredWeapons.get(id);
    }

    public ItemStack getPlayerWeapon(UUID playerId) {
        return playerWeapons.get(playerId);
    }

    public static class WeaponData {
        private final Material material;
        private final String name;
        private final List<String> lore;
        private final double damage;

        public WeaponData(Material material, String name, List<String> lore, double damage) {
            this.material = material;
            this.name = name;
            this.lore = lore;
            this.damage = damage;
        }

        public Material getMaterial() { return material; }
        public String getName() { return name; }
        public List<String> getLore() { return lore; }
        public double getDamage() { return damage; }
    }
} 