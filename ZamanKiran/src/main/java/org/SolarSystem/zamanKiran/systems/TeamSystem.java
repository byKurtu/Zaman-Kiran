package org.SolarSystem.zamanKiran.systems;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class TeamSystem {
    private final Plugin plugin;
    private final Map<UUID, Set<UUID>> teams = new HashMap<>();
    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();

    public TeamSystem(Plugin plugin) {
        this.plugin = plugin;
        startUpdateTask();
    }

    public void addTeamMember(Player leader, Player member) {
        UUID leaderId = leader.getUniqueId();
        teams.computeIfAbsent(leaderId, k -> new HashSet<>()).add(member.getUniqueId());
        updateScoreboard(leader);
    }

    public void removeTeamMember(Player leader, Player member) {
        UUID leaderId = leader.getUniqueId();
        if (teams.containsKey(leaderId)) {
            teams.get(leaderId).remove(member.getUniqueId());
            updateScoreboard(leader);
        }
    }

    public boolean isInTeam(Player leader, Player member) {
        UUID leaderId = leader.getUniqueId();
        return teams.containsKey(leaderId) && teams.get(leaderId).contains(member.getUniqueId());
    }

    private void updateScoreboard(Player leader) {
        UUID leaderId = leader.getUniqueId();
        Set<UUID> teamMembers = teams.getOrDefault(leaderId, new HashSet<>());

        String actionBarMessage = ChatColor.translateAlternateColorCodes('&',
            "&b---Takım---\n" +
            "&bTakımınOyuncuları\n");
        
        StringBuilder memberInfo = new StringBuilder();
        for (UUID memberId : teamMembers) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                String healthBar = generateHealthBar(member);
                memberInfo.append(String.format("&f%s\n%s\n", 
                    member.getName(), healthBar));
            }
        }
        
        leader.spigot().sendMessage(
            net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                actionBarMessage + memberInfo.toString()
            )
        );

        Scoreboard scoreboard = scoreboards.get(leaderId);
        if (scoreboard == null) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            scoreboards.put(leaderId, scoreboard);
        }

        if (scoreboard.getObjective("team") != null) {
            scoreboard.getObjective("team").unregister();
        }

        Objective objective = scoreboard.registerNewObjective("team", "dummy",
            ChatColor.AQUA + "Takımın");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int score = teamMembers.size();
        for (UUID memberId : teamMembers) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                String healthBar = generateHealthBar(member);
                String entry = ChatColor.WHITE + member.getName() + "\n" + healthBar;
                objective.getScore(entry).setScore(score--);
            }
        }

        leader.setScoreboard(scoreboard);
    }

    private String generateHealthBar(Player player) {
        double health = player.getHealth();
        double maxHealth = player.getMaxHealth();
        int bars = 10;
        int filledBars = (int) ((health / maxHealth) * bars);
        
        StringBuilder healthBar = new StringBuilder();
        for (int i = 0; i < bars; i++) {
            if (i < filledBars) {
                healthBar.append("§c|");
            } else {
                healthBar.append("§8|");
            }
        }
        
        return healthBar.toString();
    }

    private String getCardinalDirection(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double angle = Math.atan2(dz, dx);
        angle = angle * 180 / Math.PI;
        angle = (angle + 360) % 360;

        if (angle >= 337.5 || angle < 22.5) return "→";
        if (angle >= 22.5 && angle < 67.5) return "↘";
        if (angle >= 67.5 && angle < 112.5) return "↓";
        if (angle >= 112.5 && angle < 157.5) return "↙";
        if (angle >= 157.5 && angle < 202.5) return "←";
        if (angle >= 202.5 && angle < 247.5) return "↖";
        if (angle >= 247.5 && angle < 292.5) return "↑";
        return "↗";
    }

    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID leaderId : teams.keySet()) {
                    Player leader = Bukkit.getPlayer(leaderId);
                    if (leader != null && leader.isOnline()) {
                        updateScoreboard(leader);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Update every second
    }
} 