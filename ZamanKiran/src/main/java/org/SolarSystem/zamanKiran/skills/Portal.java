package org.SolarSystem.zamanKiran.skills;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Portal extends Skill {
    private static final int PORTAL_DURATION = 10 * 20;
    private final Map<Player, Location> portalLocations = new HashMap<>();
    private final Map<Player, List<ItemDisplay>> portalDisplays = new HashMap<>();
    private Player selectedTarget = null;

    public Portal(Plugin plugin) {
        super(plugin, "Portal", 30, 75);
    }

    @Override
    public void cast(Player caster) {
        startSkill();
        openPortalAnvil(caster);
    }

    private void openPortalAnvil(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_PURPLE + "Portal Oluştur");

        int slot = 10;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target != player) {
                ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
                ItemMeta meta = playerHead.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§b" + target.getName());
                    meta.setLore(List.of(
                        "§7Oyuncuya portal aç",
                        "§8• §fTıkla ve nether yıldızına bas"
                    ));
                    playerHead.setItemMeta(meta);
                }
                gui.setItem(slot++, playerHead);
            }
        }

        ItemStack createButton = new ItemStack(Material.NETHER_STAR);
        ItemMeta createMeta = createButton.getItemMeta();
        if (createMeta != null) {
            createMeta.setDisplayName("§6Portal Oluştur");
            createMeta.setLore(List.of(
                "§7Seçili oyuncuya portal aç",
                "§8• §fPortal 10 saniye açık kalır"
            ));
            createButton.setItemMeta(createMeta);
        }
        gui.setItem(16, createButton);

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }

        player.openInventory(gui);
    }

    public void handleAnvilClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null) return;

        if (clicked.getType() == Material.PLAYER_HEAD) {

            String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            selectedTarget = Bukkit.getPlayer(name);
            
            updateSelectionGUI(event.getInventory(), selectedTarget);
            
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        }
        else if (clicked.getType() == Material.NETHER_STAR && selectedTarget != null) {
            if (selectedTarget.isOnline()) {
                createPortal(player, selectedTarget);
                player.sendMessage(ChatColor.GREEN + selectedTarget.getName() + " adlı oyuncuya portal açıldı!");
                player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.0f);
            } else {
                player.sendMessage(ChatColor.RED + "Seçili oyuncu çevrimiçi değil!");
                selectedTarget = null;
            }
            player.closeInventory();
        }
    }

    private void updateSelectionGUI(Inventory gui, Player selected) {
        for (int i = 0; i < gui.getSize(); i++) {
            ItemStack item = gui.getItem(i);
            if (item != null && item.getType() == Material.PLAYER_HEAD) {
                ItemMeta meta = item.getItemMeta();
                String name = ChatColor.stripColor(meta.getDisplayName());
                if (selected != null && name.equals(selected.getName())) {
                    meta.setDisplayName("§a✦ " + name + " §a✦");
                    List<String> lore = meta.getLore();
                    if (lore != null) {
                        lore.set(0, "§a✓ Oyuncu seçildi");
                        meta.setLore(lore);
                    }
                } else {
                    meta.setDisplayName("§b" + name);
                }
                item.setItemMeta(meta);
            }
        }
    }

    private void createPortal(Player caster, Player target) {
        Location casterLoc = caster.getLocation();
        Location targetLoc = target.getLocation();
        
        Vector direction = caster.getLocation().getDirection();
        Vector right = direction.crossProduct(new Vector(0, 1, 0)).normalize();
        Vector up = right.crossProduct(direction).normalize();
        
        List<ItemDisplay> displays = new ArrayList<>();
        int framePoints = 16;
        double portalRadius = 1.5;
        double portalHeight = 2.0;
        
        for (int i = 0; i < framePoints; i++) {
            double angle = (2 * Math.PI * i) / framePoints;
            double x = Math.cos(angle) * portalRadius;
            double y = Math.sin(angle) * portalHeight;
            
            Vector offset = right.clone().multiply(x).add(up.clone().multiply(y));
            Location frameLoc = casterLoc.clone().add(offset);
            
            ItemDisplay frame = createItemDisplay(frameLoc, Material.END_PORTAL_FRAME);
            if (frame != null) {
                Vector toCenter = casterLoc.toVector().subtract(frameLoc.toVector()).normalize();
                float yaw = (float) Math.toDegrees(Math.atan2(-toCenter.getX(), toCenter.getZ()));
                float pitch = (float) Math.toDegrees(Math.asin(toCenter.getY()));
                
                frame.setRotation(yaw, pitch);
                scaleItemDisplay(frame, 0.3f);
                displays.add(frame);
            }
        }
        
        portalLocations.put(caster, targetLoc);
        portalDisplays.put(caster, displays);
        
        new BukkitRunnable() {
            int ticks = 0;
            double baseFrequency = 2.0;
            double amplitude = 0.5;
            
            @Override
            public void run() {
                if (!isActive || ticks++ >= PORTAL_DURATION) {
                    cleanup();
                    return;
                }

                double time = ticks * 0.1;
                int particleCount = 3;
                
                for (int i = 0; i < particleCount; i++) {
                    double t = time + (2 * Math.PI * i) / particleCount;

                    double r = Math.sin(t * 0.5) * portalRadius;
                    double x = r * Math.cos(t * baseFrequency);
                    double y = amplitude * Math.sin(t * 3) + Math.sin(time * 0.2) * portalHeight;
                    double z = r * Math.sin(t * baseFrequency);
                    
                    Location particleLoc = casterLoc.clone().add(x, y, z);
                    
                    double hue = (Math.sin(time * 0.1 + i * 0.5) + 1) * 0.5;
                    // Convert HSV to RGB (assuming S=1, V=1)
                    float hAngle = (float)hue * 360f;
                    float saturation = 1.0f;
                    float value = 1.0f;
                    
                    int hi = (int)(hAngle / 60.0f) % 6;
                    float f = (hAngle / 60.0f) - hi;
                    float p = value * (1.0f - saturation);
                    float q = value * (1.0f - f * saturation);
                    float temp = value * (1.0f - (1.0f - f) * saturation);
                    
                    float red, green, blue;
                    switch(hi) {
                        case 0: red = value; green = temp; blue = p; break;
                        case 1: red = q; green = value; blue = p; break;
                        case 2: red = p; green = value; blue = temp; break;
                        case 3: red = p; green = q; blue = value; break;
                        case 4: red = temp; green = p; blue = value; break;
                        default: red = value; green = p; blue = q; break;
                    }
                    
                    Particle.DustOptions dustOptions = new Particle.DustOptions(
                        Color.fromRGB((int)(red * 255), (int)(green * 255), (int)(blue * 255)),
                        1.0f
                    );
                    
                    casterLoc.getWorld().spawnParticle(
                        Particle.REDSTONE,
                        particleLoc,
                        0, 0, 0, 0, 1,
                        dustOptions
                    );
                }

                checkPortalCollision(caster, casterLoc, targetLoc);
            }
            
            private void cleanup() {
                displays.forEach(Entity::remove);
                portalLocations.remove(caster);
                cancel();
                endSkill();
            }
        }.runTaskTimer(plugin, 0L, 1L);

        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count++ >= 5 || !isActive) {
                    cancel();
                    return;
                }
                float pitch = 0.5f + (count * 0.1f);
                casterLoc.getWorld().playSound(casterLoc, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, pitch);
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    private void checkPortalCollision(Player caster, Location portalLoc, Location targetLoc) {
        double collisionRadius = 1.5;
        double collisionHeight = 2.0;
        
        portalLoc.getWorld().getNearbyEntities(portalLoc, collisionRadius, collisionHeight, collisionRadius)
            .stream()
            .filter(entity -> entity instanceof Player)
            .filter(entity -> !entity.equals(caster))
            .forEach(entity -> {
                Player player = (Player) entity;
                
                Vector velocity = player.getVelocity();
                Location destination = targetLoc.clone();
                
                double momentumDampening = 0.8;
                velocity.multiply(momentumDampening);
                
                velocity.setY(Math.max(0.1, velocity.getY()));
                
                player.teleport(destination);
                player.setVelocity(velocity);
                
                createTeleportEffects(player.getLocation(), destination);
            });
    }

    private void createTeleportEffects(Location from, Location to) {
        from.getWorld().spawnParticle(
            Particle.END_ROD,
            from,
            20, 0.5, 1, 0.5, 0.1
        );
        
        to.getWorld().spawnParticle(
            Particle.REVERSE_PORTAL,
            to,
            30, 0.5, 1, 0.5, 0.1
        );
        
        float pitch = (float) (0.8 + Math.random() * 0.4);
        from.getWorld().playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, pitch);
        to.getWorld().playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, pitch * 1.2f);
    }
} 