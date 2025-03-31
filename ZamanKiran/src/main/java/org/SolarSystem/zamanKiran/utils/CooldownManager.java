package org.SolarSystem.zamanKiran.utils;

import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

public class CooldownManager {
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final int cooldownSeconds;

    public CooldownManager(int cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    public boolean isOnCooldown(Player player) {
        return getRemaining(player) > 0;
    }

    public int getRemaining(Player player) {
        long endTime = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long remainingTime = (endTime - System.currentTimeMillis()) / 1000;
        return (int) Math.max(0, remainingTime);
    }

    public void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cooldownSeconds * 1000L));
    }
}