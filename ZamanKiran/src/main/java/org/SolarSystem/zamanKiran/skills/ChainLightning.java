package org.SolarSystem.zamanKiran.skills;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

public class ChainLightning extends Skill {
    private static final double DAMAGE = 25.0;
    private static final double RANGE = 15.0;
    private static final int MAX_CHAINS = 5;
    private static final double CHAIN_RANGE = 5.0;
    private static final double DAMAGE_REDUCTION = 0.8; // Her zincirde hasar %20 azalır

    public ChainLightning(Plugin plugin) {
        super(plugin, "Chain Lightning", 20, 80);
    }

    @Override
    public void cast(Player caster) {
        startSkill();
        Location start = caster.getEyeLocation();
        Vector direction = start.getDirection();

        // İlk hedefi bul
        RayTraceResult trace = caster.getWorld().rayTraceEntities(
            start,
            direction,
            RANGE,
            1.0,
            entity -> entity instanceof LivingEntity && entity != caster
        );

        if (trace != null && trace.getHitEntity() != null) {
            LivingEntity firstTarget = (LivingEntity) trace.getHitEntity();
            Set<Entity> hitEntities = new HashSet<>();
            hitEntities.add(firstTarget);

            // Zincir yıldırımı başlat
            chainLightning(caster, start, firstTarget.getLocation(), firstTarget, hitEntities, 0, DAMAGE);
        } else {
            // Hedef bulunamadıysa boşa yıldırım efekti
            createLightningEffect(start, start.add(direction.multiply(RANGE)));
        }
    }

    private void chainLightning(Player caster, Location from, Location to, LivingEntity target, 
                              Set<Entity> hitEntities, int chainCount, double currentDamage) {
        // Yıldırım efekti
        createLightningEffect(from, to);

        // Hasar ver
        target.damage(currentDamage, caster);
        playSound(to, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.0f + (chainCount * 0.1f));

        // Maksimum zincir sayısını kontrol et
        if (chainCount >= MAX_CHAINS) return;

        // Yakındaki hedefleri bul
        List<Entity> nearbyEntities = new ArrayList<>(to.getWorld().getNearbyEntities(to, CHAIN_RANGE, CHAIN_RANGE, CHAIN_RANGE));
        nearbyEntities.removeIf(entity -> 
            !(entity instanceof LivingEntity) || 
            entity == caster || 
            hitEntities.contains(entity)
        );

        if (!nearbyEntities.isEmpty()) {
            // En yakın hedefi seç
            nearbyEntities.sort((e1, e2) -> 
                Double.compare(e1.getLocation().distanceSquared(to), 
                             e2.getLocation().distanceSquared(to)));
            
            Entity nextTarget = nearbyEntities.get(0);
            hitEntities.add(nextTarget);

            // Gecikme ile sonraki zinciri başlat
            new BukkitRunnable() {
                @Override
                public void run() {
                    chainLightning(
                        caster,
                        to,
                        nextTarget.getLocation(),
                        (LivingEntity) nextTarget,
                        hitEntities,
                        chainCount + 1,
                        currentDamage * DAMAGE_REDUCTION
                    );
                }
            }.runTaskLater(plugin, 3L);
        }
    }

    private void createLightningEffect(Location from, Location to) {
        double distance = from.distance(to);
        Vector direction = to.toVector().subtract(from.toVector()).normalize();
        int points = (int) (distance * 2);
        
        // Ana yıldırım çizgisi
        for (int i = 0; i < points; i++) {
            double progress = (double) i / points;
            Location point = from.clone().add(direction.clone().multiply(distance * progress));
            
            // Rastgele sapma ekle
            double offset = 0.2;
            point.add(
                (Math.random() - 0.5) * offset,
                (Math.random() - 0.5) * offset,
                (Math.random() - 0.5) * offset
            );
            
            // Parçacık efektleri
            from.getWorld().spawnParticle(
                Particle.ELECTRIC_SPARK,
                point,
                1, 0, 0, 0, 0
            );
            
            // Yan dallar
            if (Math.random() < 0.1) {
                Vector branchDir = direction.clone()
                    .rotateAroundAxis(new Vector(Math.random(), Math.random(), Math.random()), Math.random() * Math.PI)
                    .multiply(0.5);
                
                Location branchEnd = point.clone().add(branchDir);
                createBranchEffect(point, branchEnd);
            }
        }
    }

    private void createBranchEffect(Location from, Location to) {
        double distance = from.distance(to);
        Vector direction = to.toVector().subtract(from.toVector()).normalize();
        int points = (int) (distance * 2);
        
        for (int i = 0; i < points; i++) {
            double progress = (double) i / points;
            Location point = from.clone().add(direction.clone().multiply(distance * progress));
            
            point.add(
                (Math.random() - 0.5) * 0.1,
                (Math.random() - 0.5) * 0.1,
                (Math.random() - 0.5) * 0.1
            );
            
            from.getWorld().spawnParticle(
                Particle.ELECTRIC_SPARK,
                point,
                1, 0, 0, 0, 0
            );
        }
    }
} 