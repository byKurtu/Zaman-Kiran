package org.SolarSystem.zamanKiran.skills;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.inventory.ItemStack;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

public abstract class Skill implements Listener {
    protected final Plugin plugin;
    protected final String name;
    protected final int cooldown;
    protected final double manaCost;
    protected final List<ArmorStand> activeArmorStands;
    protected final List<BlockDisplay> activeDisplays;
    protected final List<ItemDisplay> activeItemDisplays;
    protected boolean isActive = false;
    protected long lastUsed;

    public Skill(Plugin plugin, String name, int cooldown, double manaCost) {
        this.plugin = plugin;
        this.name = name;
        this.cooldown = cooldown;
        this.manaCost = manaCost;
        this.activeArmorStands = new ArrayList<>();
        this.activeDisplays = new ArrayList<>();
        this.activeItemDisplays = new ArrayList<>();
        this.lastUsed = 0;
    }

    public abstract void cast(Player caster);

    protected ArmorStand createArmorStand(Location location, boolean small, boolean visible) {
        if (!isActive || location == null || location.getWorld() == null) return null;
        
        try {
            ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class);
            if (stand != null) {
                stand.setSmall(small);
                stand.setVisible(visible);
                stand.setGravity(false);
                stand.setInvulnerable(true);
                stand.setMarker(true);
                stand.setPersistent(false);
                stand.setRemoveWhenFarAway(true);
                activeArmorStands.add(stand);
            }
            return stand;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create ArmorStand: " + e.getMessage());
            return null;
        }
    }

    protected BlockDisplay createBlockDisplay(Location location, Material material) {
        if (!isActive || location == null || location.getWorld() == null || material == null) return null;
        
        try {
            BlockDisplay display = location.getWorld().spawn(location, BlockDisplay.class);
            if (display != null) {
                display.setBlock(material.createBlockData());
                display.setPersistent(false);
                
                Transformation transformation = new Transformation(
                    new Vector3f(0, 0, 0),
                    new Quaternionf(),
                    new Vector3f(0.5f, 0.5f, 0.5f),
                    new Quaternionf()
                );
                display.setTransformation(transformation);
                
                activeDisplays.add(display);
            }
            return display;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create BlockDisplay: " + e.getMessage());
            return null;
        }
    }

    protected void rotateBlockDisplay(BlockDisplay display, float angle, float x, float y, float z) {
        if (display == null || !display.isValid()) return;
        
        try {
            Transformation current = display.getTransformation();
            Transformation newTransform = new Transformation(
                current.getTranslation(),
                new Quaternionf().rotateAxis(angle, x, y, z),
                current.getScale(),
                current.getRightRotation()
            );
            display.setTransformation(newTransform);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to rotate BlockDisplay: " + e.getMessage());
        }
    }

    protected void scaleBlockDisplay(BlockDisplay display, float scale) {
        if (display == null || !display.isValid()) return;
        
        try {
            Transformation current = display.getTransformation();
            Transformation newTransform = new Transformation(
                current.getTranslation(),
                current.getLeftRotation(),
                new Vector3f(scale, scale, scale),
                current.getRightRotation()
            );
            display.setTransformation(newTransform);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to scale BlockDisplay: " + e.getMessage());
        }
    }

    protected ItemDisplay createItemDisplay(Location location, Material material) {
        if (!isActive || location == null || location.getWorld() == null || material == null) return null;
        
        try {
            ItemDisplay display = location.getWorld().spawn(location, ItemDisplay.class);
            if (display != null) {
                display.setItemStack(new ItemStack(material));
                display.setPersistent(false);
                
                Transformation transformation = new Transformation(
                    new Vector3f(0, 0, 0),
                    new Quaternionf(),
                    new Vector3f(1.0f, 1.0f, 1.0f),
                    new Quaternionf()
                );
                display.setTransformation(transformation);
                
                activeItemDisplays.add(display);
            }
            return display;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create ItemDisplay: " + e.getMessage());
            return null;
        }
    }

    protected void rotateItemDisplay(ItemDisplay display, float angle, float x, float y, float z) {
        if (display == null || !display.isValid()) return;
        
        try {
            Transformation current = display.getTransformation();
            Transformation newTransform = new Transformation(
                current.getTranslation(),
                new Quaternionf().rotateAxis(angle, x, y, z),
                current.getScale(),
                current.getRightRotation()
            );
            display.setTransformation(newTransform);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to rotate ItemDisplay: " + e.getMessage());
        }
    }

    protected void scaleItemDisplay(ItemDisplay display, float scale) {
        if (display == null || !display.isValid()) return;
        
        try {
            Transformation current = display.getTransformation();
            Transformation newTransform = new Transformation(
                current.getTranslation(),
                current.getLeftRotation(),
                new Vector3f(scale, scale, scale),
                current.getRightRotation()
            );
            display.setTransformation(newTransform);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to scale ItemDisplay: " + e.getMessage());
        }
    }

    protected void startSkill() {
        isActive = true;
        lastUsed = System.currentTimeMillis();
        cleanupEntities();
    }

    public void endSkill() {
        isActive = false;
        cleanupEntities();
    }

    public boolean isOnCooldown() {
        return System.currentTimeMillis() - lastUsed < cooldown * 1000;
    }

    protected void cleanupEntities() {
        try {
            Iterator<ArmorStand> standIterator = activeArmorStands.iterator();
            while (standIterator.hasNext()) {
                ArmorStand stand = standIterator.next();
                if (stand != null && stand.isValid()) {
                    stand.remove();
                }
                standIterator.remove();
            }

            Iterator<BlockDisplay> displayIterator = activeDisplays.iterator();
            while (displayIterator.hasNext()) {
                BlockDisplay display = displayIterator.next();
                if (display != null && display.isValid()) {
                    display.remove();
                }
                displayIterator.remove();
            }

            Iterator<ItemDisplay> itemDisplayIterator = activeItemDisplays.iterator();
            while (itemDisplayIterator.hasNext()) {
                ItemDisplay display = itemDisplayIterator.next();
                if (display != null && display.isValid()) {
                    display.remove();
                }
                itemDisplayIterator.remove();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error during entity cleanup: " + e.getMessage());
        }
    }

    protected void spawnParticleCircle(Location center, Particle particle, double radius, int points) {
        if (!isActive) return;
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            Location point = center.clone().add(
                radius * Math.cos(angle),
                0,
                radius * Math.sin(angle)
            );
            center.getWorld().spawnParticle(particle, point, 1, 0, 0, 0, 0);
        }
    }

    protected void spawnParticleLine(Location start, Location end, Particle particle, double density) {
        if (!isActive) return;
        double distance = start.distance(end);
        int points = (int) (distance * density);
        
        for (int i = 0; i < points; i++) {
            double progress = (double) i / points;
            Location point = start.clone().add(
                end.clone().subtract(start).multiply(progress)
            );
            start.getWorld().spawnParticle(particle, point, 1, 0, 0, 0, 0);
        }
    }

    protected void playSound(Location location, Sound sound, float volume, float pitch) {
        if (!isActive) return;
        location.getWorld().playSound(location, sound, volume, pitch);
    }

    protected void createParticleHelix(Location center, double radius, double height, int points, Particle particle) {
        if (!isActive) return;
        new BukkitRunnable() {
            double y = 0;
            int ticks = 0;
            final int maxTicks = points;

            @Override
            public void run() {
                if (ticks++ >= maxTicks) {
                    this.cancel();
                    return;
                }

                double angle = (2 * Math.PI * ticks) / 20;
                Location point = center.clone().add(
                    radius * Math.cos(angle),
                    y,
                    radius * Math.sin(angle)
                );
                center.getWorld().spawnParticle(particle, point, 1, 0, 0, 0, 0);
                y += height / maxTicks;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public String getName() {
        return name;
    }

    public int getCooldown() {
        return cooldown;
    }

    public double getManaCost() {
        return manaCost;
    }

    public double getRemainingCooldown() {
        long currentTime = System.currentTimeMillis();
        long lastUsedTime = this.lastUsed;
        long cooldownMillis = this.cooldown * 1000;
        long remainingMillis = Math.max(0, cooldownMillis - (currentTime - lastUsedTime));
        return remainingMillis / 50.0; // Convert to ticks
    }
} 