package org.SolarSystem.zamanKiran.abilities;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;

public abstract class Ability {
    protected final Plugin plugin;
    protected final String name;
    protected final int cooldown;
    protected final int manaCost;
    
    public Ability(Plugin plugin, String name, int cooldown, int manaCost) {
        this.plugin = plugin;
        this.name = name;
        this.cooldown = cooldown;
        this.manaCost = manaCost;
    }
    
    public abstract void cast(Player player);
    
    protected void startCooldown(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Remove cooldown
            }
        }.runTaskLater(plugin, cooldown * 20L);
    }
    
    public String getName() {
        return name;
    }
    
    public int getCooldown() {
        return cooldown;
    }
    
    public int getManaCost() {
        return manaCost;
    }
} 