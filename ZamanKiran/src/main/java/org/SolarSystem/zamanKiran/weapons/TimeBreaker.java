package org.SolarSystem.zamanKiran.weapons;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.SolarSystem.zamanKiran.utils.CooldownManager;

public class TimeBreaker {
    private final CooldownManager cooldown = new CooldownManager(5);

    public void onRightClick(Player player) {
        if(cooldown.isOnCooldown(player)) {
            player.sendMessage(ChatColor.RED + "Bekle: " + cooldown.getRemaining(player) + "s");
            return;
        }

        player.getWorld().getNearbyEntities(player.getLocation(), 10, 10, 10)
                .forEach(entity -> {
                    if(entity instanceof LivingEntity && !entity.equals(player)) {
                        ((LivingEntity)entity).addPotionEffect(
                                new PotionEffect(PotionEffectType.SLOW, 100, 2));
                    }
                });

        player.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation(), 100);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);

        cooldown.setCooldown(player);
    }
}