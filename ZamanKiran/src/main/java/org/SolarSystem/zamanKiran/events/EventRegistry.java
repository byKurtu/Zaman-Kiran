package org.SolarSystem.zamanKiran.events;

import org.SolarSystem.zamanKiran.ZamanKiran;
import org.SolarSystem.zamanKiran.listeners.*;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.event.HandlerList;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public class EventRegistry {
    private final ZamanKiran plugin;
    private final List<Listener> registeredListeners;
    
    public EventRegistry(ZamanKiran plugin) {
        this.plugin = plugin;
        this.registeredListeners = Arrays.asList(
            new PlayerInteractListener(plugin),
            new InventoryClickListener(plugin),
            new EntityDamageListener(plugin),
            new PlayerJoinListener(plugin)
        );
    }
    
    public void registerAllEvents() {
        try {
            PluginManager pm = plugin.getServer().getPluginManager();
            
            for (Listener listener : registeredListeners) {
                try {
                    pm.registerEvents(listener, plugin);
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("Registered listener: " + listener.getClass().getSimpleName());
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, 
                        "Failed to register listener: " + listener.getClass().getSimpleName(), e);
                }
            }
            
            plugin.getLogger().info("Successfully registered " + registeredListeners.size() + " event listeners");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Critical error during event registration", e);
        }
    }
    
    public void unregisterAllEvents() {
        try {
            // Explicitly cast to Plugin to resolve ambiguity
            HandlerList.unregisterAll((Plugin)plugin);
            
            if (plugin.getConfig().getBoolean("debug", false)) {
                registeredListeners.forEach(listener -> 
                    plugin.getLogger().info("Unregistered listener: " + 
                        listener.getClass().getSimpleName()));
            }
            
            plugin.getLogger().info("Successfully unregistered all event listeners");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error during event unregistration", e);
        }
    }
    
    public List<Listener> getRegisteredListeners() {
        return registeredListeners;
    }
} 