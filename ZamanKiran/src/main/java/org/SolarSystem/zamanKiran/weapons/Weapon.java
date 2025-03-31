package org.SolarSystem.zamanKiran.weapons;

import org.bukkit.entity.Player;

public interface Weapon {
    void onRightClick(Player player);
    String getName();
}