package org.SolarSystem.zamanKiran.items;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ItemManager {
    private final Plugin plugin;
    private final Map<UUID, ItemStack> playerWeapons = new HashMap<>();
    private final Map<Location, ItemStack> droppedWeapons = new HashMap<>();

    public ItemManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        startUpdateTask();
    }

    public ItemStack createWeapon(Player player) {
        ItemStack weapon = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = weapon.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', 
                "&b✧ Zaman Kıran &b✧"));
            
            meta.setLore(Arrays.asList(
                "§8• §7Efsanevi bir silah...",
                "§8• §cHasar: §4❁ 9.200.000",
                "§8• §bÖzellikler:",
                "§8  ⦿ §eDiken X",
                "§8  ⦿ §eKeskinlik X",
                "§8  ⦿ §eAteş Koruması X",
                "§8  ⦿ §eKırılmazlık X",
                "",
                "§8⚡ §eHız: §60.0",
                "§8↗ §eYön: §6" + getDirectionArrow(player.getLocation())
            ));

            meta.addEnchant(Enchantment.DAMAGE_ALL, 10, true);
            meta.addEnchant(Enchantment.THORNS, 10, true);
            meta.addEnchant(Enchantment.FIRE_ASPECT, 10, true);
            meta.addEnchant(Enchantment.DURABILITY, 10, true);
            
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            
            weapon.setItemMeta(meta);
        }

        playerWeapons.put(player.getUniqueId(), weapon);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.sendMessage(ChatColor.GREEN + "» Zaman Kıran'ın gücüyle uçabilirsin!");
        
        return weapon;
    }

    public boolean isWeapon(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && 
               meta.getDisplayName().contains("Zaman Kıran");
    }

    public void trackDroppedWeapon(Location location, ItemStack weapon) {
        droppedWeapons.put(location, weapon);
        updateDroppedWeaponHologram(location);
    }

    public void removeDroppedWeapon(Location location) {
        droppedWeapons.remove(location);
    }

    private void updateDroppedWeaponHologram(Location location) {

        String direction = getDirectionArrow(location);
        String displayText = ChatColor.translateAlternateColorCodes('&',
            "&4✧ Zaman Kıran &4✧\n" +
            "&8⚡ &eHız: &60.0\n" +
            "&8↗ &eYön: &6" + direction);
        

    }

    private String getDirectionArrow(Location location) {
        double yaw = location.getYaw();
        while (yaw < 0) yaw += 360;
        while (yaw > 360) yaw -= 360;

        if (yaw >= 337.5 || yaw < 22.5) return "↑";
        if (yaw >= 22.5 && yaw < 67.5) return "↗";
        if (yaw >= 67.5 && yaw < 112.5) return "→";
        if (yaw >= 112.5 && yaw < 157.5) return "↘";
        if (yaw >= 157.5 && yaw < 202.5) return "↓";
        if (yaw >= 202.5 && yaw < 247.5) return "↙";
        if (yaw >= 247.5 && yaw < 292.5) return "←";
        if (yaw >= 292.5 && yaw < 337.5) return "↖";
        return "↑";
    }

    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (Map.Entry<Location, ItemStack> entry : droppedWeapons.entrySet()) {
                        updateDroppedWeaponHologram(entry.getKey());
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error updating weapon displays: " + e.getMessage());
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void updateWeaponLore(ItemStack weapon, Player player) {
        if (!isWeapon(weapon)) return;

        ItemMeta meta = weapon.getItemMeta();
        if (meta == null) return;

        String direction = getDirectionArrow(player.getLocation());
        List<String> lore = meta.getLore();
        if (lore != null && lore.size() >= 10) {
            lore.set(9, "§8↗ §eYön: §6" + direction);
            meta.setLore(lore);
            weapon.setItemMeta(meta);
        }
    }
} 