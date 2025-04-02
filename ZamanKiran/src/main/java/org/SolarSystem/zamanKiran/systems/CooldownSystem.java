package org.SolarSystem.zamanKiran.systems;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownSystem {
    private static CooldownSystem instance;
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    
    private CooldownSystem() {}
    
    public static CooldownSystem getInstance() {
        if (instance == null) {
            instance = new CooldownSystem();
        }
        return instance;
    }
    
    public void setCooldown(Player player, String ability, int seconds) {
        UUID playerId = player.getUniqueId();
        cooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                 .put(ability, System.currentTimeMillis() + (seconds * 1000L));
    }
    
    public boolean isOnCooldown(Player player, String ability) {
        UUID playerId = player.getUniqueId();
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        
        if (playerCooldowns == null) return false;
        
        Long cooldownUntil = playerCooldowns.get(ability);
        if (cooldownUntil == null) return false;
        
        return System.currentTimeMillis() < cooldownUntil;
    }
    
    public long getRemainingCooldown(Player player, String ability) {
        UUID playerId = player.getUniqueId();
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        
        if (playerCooldowns == null) return 0L;
        
        Long cooldownUntil = playerCooldowns.get(ability);
        if (cooldownUntil == null) return 0L;
        
        long remaining = cooldownUntil - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
    
    public void clearCooldown(Player player, String ability) {
        UUID playerId = player.getUniqueId();
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        
        if (playerCooldowns != null) {
            playerCooldowns.remove(ability);
        }
    }
    
    public void clearAllCooldowns(Player player) {
        cooldowns.remove(player.getUniqueId());
    }
    
    public String formatRemainingTime(long milliseconds) {
        if (milliseconds < 1000) return "0.0s";
        return String.format("%.1fs", milliseconds / 1000.0);
    }
} 