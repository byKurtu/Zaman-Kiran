package org.SolarSystem.zamanKiran.weapons;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.SolarSystem.zamanKiran.Core;

import java.util.HashMap;
import java.util.Map;

public class WeaponManager {
    private final Core plugin;
    private final Map<String, Weapon> weapons = new HashMap<>();

    public WeaponManager(Core plugin) {
        this.plugin = plugin;
    }

    public void registerWeapons() {
        weapons.put("time_breaker", (Weapon) new TimeBreaker());
    }

    public void giveWeapon(Player player, String weaponId) {
        Weapon weapon = weapons.get(weaponId);
        if (weapon != null) {
            ItemStack item = createWeaponItem(weapon);
            player.getInventory().addItem(item);
        }
    }

    private ItemStack createWeaponItem(Weapon weapon) {
        return new ItemStack(Material.DIAMOND_PICKAXE);
    }
}