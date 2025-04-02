package org.SolarSystem.zamanKiran.skills;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ThrowModeManager implements Listener {
    private final Plugin plugin;
    private final Map<UUID, ThrowSkill.ThrowMode> playerModes = new HashMap<>();
    private final Map<UUID, Long> lastModeSwitch = new HashMap<>();
    private static final long MODE_SWITCH_COOLDOWN = 500; // 0.5 saniye

    public ThrowModeManager(Plugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        
        if (!player.isSneaking()) return;
        
        long currentTime = System.currentTimeMillis();
        long lastSwitch = lastModeSwitch.getOrDefault(player.getUniqueId(), 0L);
        
        if (currentTime - lastSwitch < MODE_SWITCH_COOLDOWN) {
            return;
        }
        
        event.setCancelled(true);
        cycleThrowMode(player);
        lastModeSwitch.put(player.getUniqueId(), currentTime);
    }

    public void cycleThrowMode(Player player) {
        ThrowSkill.ThrowMode currentMode = playerModes.getOrDefault(
            player.getUniqueId(), 
            ThrowSkill.ThrowMode.DIRECTIVE
        );
        
        ThrowSkill.ThrowMode nextMode = getNextMode(currentMode);
        playerModes.put(player.getUniqueId(), nextMode);
        
        // Action bar mesajı
        String message = ChatColor.AQUA + "» Fırlatma Modu: " + 
                        ChatColor.YELLOW + nextMode.getDisplayName();
        
        player.spigot().sendMessage(
            net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message)
        );
    }

    private ThrowSkill.ThrowMode getNextMode(ThrowSkill.ThrowMode currentMode) {
        switch (currentMode) {
            case AUTO:
                return ThrowSkill.ThrowMode.DIRECTIVE;
            case DIRECTIVE:
                return ThrowSkill.ThrowMode.CALCULATIVE;
            case CALCULATIVE:
                return ThrowSkill.ThrowMode.AUTO;
            default:
                return ThrowSkill.ThrowMode.DIRECTIVE;
        }
    }

    public ThrowSkill.ThrowMode getPlayerMode(Player player) {
        return playerModes.getOrDefault(player.getUniqueId(), ThrowSkill.ThrowMode.DIRECTIVE);
    }

    public void setPlayerMode(Player player, ThrowSkill.ThrowMode mode) {
        playerModes.put(player.getUniqueId(), mode);
    }

    public void clearPlayerMode(Player player) {
        playerModes.remove(player.getUniqueId());
        lastModeSwitch.remove(player.getUniqueId());
    }
} 