package org.SolarSystem.zamanKiran.modules;



import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;

public class TeamSystem {
    private final Map<UUID, List<UUID>> teams = new HashMap<>();
    private final Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

    public void addToTeam(Player leader, Player member) {
        teams.computeIfAbsent(leader.getUniqueId(), k -> new ArrayList<>()).add(member.getUniqueId());
        updateScoreboard(leader);
    }

    private void updateScoreboard(Player leader) {
        Objective obj = scoreboard.registerNewObjective("team", "dummy", "Takım");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.getScore("Lider: " + leader.getName()).setScore(1);
    }
}
