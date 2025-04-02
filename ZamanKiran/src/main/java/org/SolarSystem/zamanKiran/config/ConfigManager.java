package org.SolarSystem.zamanKiran.config;

import org.SolarSystem.zamanKiran.ZamanKiran;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private static ConfigManager instance;
    private final ZamanKiran plugin;
    private FileConfiguration config;
    private File configFile;
    
    private ConfigManager(ZamanKiran plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public static ConfigManager getInstance(ZamanKiran plugin) {
        if (instance == null) {
            instance = new ConfigManager(plugin);
        }
        return instance;
    }
    
    private void loadConfig() {
        plugin.saveDefaultConfig();
        configFile = new File(plugin.getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Default değerleri ayarla
        setDefaults();
    }
    
    private void setDefaults() {
        config.addDefault("debug", false);
        
        // Silah ayarları
        config.addDefault("weapons.time_breaker.cooldown", 5);
        config.addDefault("weapons.time_breaker.damage", 9200000.0);
        config.addDefault("weapons.time_breaker.particles", "REDSTONE");
        
        // Yetenek ayarları
        config.addDefault("skills.time_stop.duration", 10);
        config.addDefault("skills.time_stop.radius", 8.0);
        config.addDefault("skills.time_stop.cooldown", 30);
        
        config.addDefault("skills.portal.duration", 10);
        config.addDefault("skills.portal.max_distance", 50.0);
        config.addDefault("skills.portal.cooldown", 45);
        
        config.options().copyDefaults(true);
        saveConfig();
    }
    
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Config dosyası kaydedilemedi: " + e.getMessage());
        }
    }
    
    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    public boolean isDebugEnabled() {
        return config.getBoolean("debug", false);
    }
    
    public Map<String, Object> getWeaponConfig(String weaponId) {
        Map<String, Object> weaponConfig = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("weapons." + weaponId);
        
        if (section != null) {
            weaponConfig.put("cooldown", section.getInt("cooldown"));
            weaponConfig.put("damage", section.getDouble("damage"));
            weaponConfig.put("particles", section.getString("particles"));
        }
        
        return weaponConfig;
    }
    
    public Map<String, Object> getSkillConfig(String skillId) {
        Map<String, Object> skillConfig = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("skills." + skillId);
        
        if (section != null) {
            skillConfig.put("duration", section.getInt("duration"));
            skillConfig.put("radius", section.getDouble("radius"));
            skillConfig.put("cooldown", section.getInt("cooldown"));
        }
        
        return skillConfig;
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
} 