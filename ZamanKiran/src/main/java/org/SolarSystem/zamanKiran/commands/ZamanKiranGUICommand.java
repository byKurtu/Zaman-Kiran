package org.SolarSystem.zamanKiran.commands;

import org.SolarSystem.zamanKiran.ZamanKiran;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ZamanKiranGUICommand implements CommandExecutor {
    private final ZamanKiran plugin;

    public ZamanKiranGUICommand(ZamanKiran plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Bu komutu sadece oyuncular kullanabilir!");
            return true;
        }

        Player player = (Player) sender;
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        if (mainHand.getType() == Material.AIR) {
            plugin.getWeaponGUI().openGUI(player);
        } else if (plugin.getItemManager().isWeapon(mainHand)) {
            plugin.getSkillGUI().openGUI(player);
        } else {
            player.sendMessage(ChatColor.RED + "Zaman Kıran silahını tutarken veya eliniz boşken kullanın!");
        }

        return true;
    }
} 