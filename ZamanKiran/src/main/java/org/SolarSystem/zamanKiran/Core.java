package org.SolarSystem.zamanKiran;

import org.SolarSystem.zamanKiran.commands.WeaponCommand;
import org.SolarSystem.zamanKiran.listeners.WeaponInteractListener;
import org.SolarSystem.zamanKiran.weapons.WeaponManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Core extends JavaPlugin {
    @Override
    public void onEnable() {
        WeaponManager weaponManager = new WeaponManager(this);
        weaponManager.registerWeapons();

        getCommand("zaman").setExecutor(new WeaponCommand(weaponManager));
        getServer().getPluginManager().registerEvents(new WeaponInteractListener(this), this);

        getLogger().info("Zaman Kıran aktif!");
    }
}