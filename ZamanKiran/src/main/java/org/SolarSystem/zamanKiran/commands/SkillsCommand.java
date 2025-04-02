package org.SolarSystem.zamanKiran.commands;

import org.SolarSystem.zamanKiran.ZamanKiran;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SkillsCommand implements CommandExecutor {
    private final ZamanKiran plugin;

    public SkillsCommand(ZamanKiran plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Bu komutu sadece oyuncular kullanabilir!");
            return true;
        }

        Player player = (Player) sender;
        plugin.getSkillGUI().openGUI(player);
        return true;
    }
} 