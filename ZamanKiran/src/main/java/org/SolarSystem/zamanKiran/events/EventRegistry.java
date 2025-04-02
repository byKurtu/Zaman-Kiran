package org.SolarSystem.zamanKiran.events;

import org.SolarSystem.zamanKiran.ZamanKiran;
import org.SolarSystem.zamanKiran.listeners.*;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

public class EventRegistry {
    private final ZamanKiran plugin;
    
    public EventRegistry(ZamanKiran plugin) {
        this.plugin = plugin;
    }
    
    public void registerAllEvents() {
        List<Listener> listeners = Arrays.asList(
            new PlayerInteractListener(plugin),
            new InventoryClickListener(plugin),
            new EntityDamageListener(plugin),
            new PlayerJoinListener(plugin)
        );
        
        listeners.forEach(listener -> 
            plugin.getServer().getPluginManager().registerEvents(listener, plugin)
        );
        
        plugin.getLogger().info("Registered " + listeners.size() + " event listeners");
    }
    
    public void unregisterAllEvents() {
        org.bukkit.event.HandlerList.unregisterAll((Plugin)plugin);
        plugin.getLogger().info("Unregistered all event listeners");
    }
} 