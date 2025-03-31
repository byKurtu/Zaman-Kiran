package org.SolarSystem.zamanKiran.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.SolarSystem.zamanKiran.weapons.WeaponManager;

public class WeaponCommand implements CommandExecutor {
    private final WeaponManager weaponManager;

    public WeaponCommand(WeaponManager weaponManager) {
        this.weaponManager = weaponManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Sadece oyuncular kullanabilir!");
            return false;
        }

        Player player = (Player) sender;
        if (args.length > 0 && args[0].equalsIgnoreCase("give")) {
            weaponManager.giveWeapon(player, "time_breaker");
            return true;
        }
        return false;
    }
}