package org.SolarSystem.zamanKiran.skills;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class TimeFreezePasif {
    private final Plugin plugin;
    private final Random random = new Random();
    private static final double TRIGGER_CHANCE = 0.25; // 25% şans
    private static final int FREEZE_DURATION = 3 * 20; // 3 saniye

    public TimeFreezePasif(Plugin plugin) {
        this.plugin = plugin;
    }

    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        Player attacker = (Player) event.getDamager();
        LivingEntity victim = (LivingEntity) event.getEntity();

        // Silahı kontrol et
        if (!isHoldingTimeWeapon(attacker)) return;

        // Şans hesapla
        if (random.nextDouble() <= TRIGGER_CHANCE) {
            freezeTarget(victim);
        }
    }

    private boolean isHoldingTimeWeapon(Player player) {
        return player.getInventory().getItemInMainHand().hasItemMeta() &&
               player.getInventory().getItemInMainHand().getItemMeta().hasDisplayName() &&
               player.getInventory().getItemInMainHand().getItemMeta().getDisplayName().contains("Zaman Kıran");
    }

    private void freezeTarget(LivingEntity target) {
        Location loc = target.getLocation();
        
        // Efektler
        target.getWorld().spawnParticle(Particle.SNOWFLAKE, loc.add(0, 1, 0), 50, 0.5, 1, 0.5, 0.1);
        target.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
        target.getWorld().playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 1.0f, 2.0f);

        // Zaman dondurma efektleri
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, FREEZE_DURATION, 7, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, FREEZE_DURATION, 128, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, FREEZE_DURATION, 1, false, false));

        // Buz parçacık efekti
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks++ >= FREEZE_DURATION || !target.isValid()) {
                    this.cancel();
                    return;
                }

                if (ticks % 5 == 0) {
                    Location particleLoc = target.getLocation().add(0, 1, 0);
                    target.getWorld().spawnParticle(
                        Particle.CLOUD,
                        particleLoc,
                        3,
                        0.2, 0.4, 0.2,
                        0.02
                    );
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Bildirim
        if (target instanceof Player) {
            ((Player) target).sendMessage(ChatColor.AQUA + "» Zaman Kıran'ın pasif etkisiyle donuyorsun!");
        }
    }
} 