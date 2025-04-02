package org.SolarSystem.zamanKiran.commands;

import org.SolarSystem.zamanKiran.ZamanKiran;
import org.SolarSystem.zamanKiran.skills.MassCurse;
import org.SolarSystem.zamanKiran.skills.MassCurse.CurseType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.*;
import java.util.stream.Collectors;

public class CurseCommands implements CommandExecutor, TabCompleter {
    private final ZamanKiran plugin;

    public CurseCommands(ZamanKiran plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Bu komutu sadece oyuncular kullanabilir!");
            return true;
        }

        Player player = (Player) sender;

        switch (command.getName().toLowerCase()) {
            case "lanet":
                return handleCurseCommand(player, args);
            case "lanetkaldir":
                return handleRemoveCurseCommand(player, args);
            case "lanetler":
                return handleListCursesCommand(player, args);
            case "lanetgrubu":
                return handleGroupCurseCommand(player, args);
            default:
                return false;
        }
    }

    private boolean handleCurseCommand(Player sender, String[] args) {
        if (!sender.hasPermission("zamankiran.curse")) {
            sender.sendMessage(ChatColor.RED + "Bu komutu kullanma yetkiniz yok!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Kullanım: /lanet <oyuncu> <lanet_türü>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Oyuncu bulunamadı: " + args[0]);
            return true;
        }

        try {
            CurseType curseType = CurseType.valueOf(args[1].toUpperCase());
            applyCurse(target, curseType);
            sender.sendMessage(ChatColor.GREEN + target.getName() + " oyuncusuna " + 
                curseType.name().toLowerCase() + " laneti uygulandı!");
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Geçersiz lanet türü! Mevcut lanetler:");
            for (CurseType type : CurseType.values()) {
                sender.sendMessage(ChatColor.GRAY + "- " + type.name().toLowerCase());
            }
        }

        return true;
    }

    private boolean handleRemoveCurseCommand(Player sender, String[] args) {
        if (!sender.hasPermission("zamankiran.curse.remove")) {
            sender.sendMessage(ChatColor.RED + "Bu komutu kullanma yetkiniz yok!");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Kullanım: /lanetkaldir <oyuncu>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Oyuncu bulunamadı: " + args[0]);
            return true;
        }

        if (!MassCurse.isCursed(target)) {
            sender.sendMessage(ChatColor.RED + target.getName() + " oyuncusu lanetli değil!");
            return true;
        }

        MassCurse.removeCurses(target);
        sender.sendMessage(ChatColor.GREEN + target.getName() + " oyuncusunun lanetleri kaldırıldı!");
        return true;
    }

    private boolean handleListCursesCommand(Player sender, String[] args) {
        if (args.length > 0) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Oyuncu bulunamadı: " + args[0]);
                return true;
            }

            List<CurseType> curses = MassCurse.getActiveCurseTypes(target);
            if (curses.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + target.getName() + " oyuncusu üzerinde aktif lanet yok.");
            } else {
                sender.sendMessage(ChatColor.GOLD + target.getName() + " oyuncusunun lanetleri:");
                for (CurseType curse : curses) {
                    sender.sendMessage(ChatColor.GRAY + "- " + curse.name().toLowerCase());
                }
            }
        } else {
            // List all cursed players
            boolean found = false;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (MassCurse.isCursed(online)) {
                    if (!found) {
                        sender.sendMessage(ChatColor.GOLD + "Lanetli oyuncular:");
                        found = true;
                    }
                    List<CurseType> curses = MassCurse.getActiveCurseTypes(online);
                    sender.sendMessage(ChatColor.GRAY + "- " + online.getName() + ": " + 
                        curses.stream()
                            .map(c -> c.name().toLowerCase())
                            .collect(Collectors.joining(", ")));
                }
            }
            if (!found) {
                sender.sendMessage(ChatColor.YELLOW + "Şu anda lanetli oyuncu yok.");
            }
        }
        return true;
    }

    private boolean handleGroupCurseCommand(Player sender, String[] args) {
        if (!sender.hasPermission("zamankiran.curse.group")) {
            sender.sendMessage(ChatColor.RED + "Bu komutu kullanma yetkiniz yok!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Kullanım: /lanetgrubu <yarıçap> <lanet_türü>");
            return true;
        }

        try {
            double radius = Double.parseDouble(args[0]);
            CurseType curseType = CurseType.valueOf(args[1].toUpperCase());

            Collection<Entity> nearbyEntities = sender.getWorld().getNearbyEntities(
                sender.getLocation(), radius, radius, radius,
                entity -> entity instanceof Player && entity != sender
            );

            if (nearbyEntities.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "Belirtilen yarıçapta oyuncu bulunamadı!");
                return true;
            }

            for (Entity entity : nearbyEntities) {
                Player target = (Player) entity;
                applyCurse(target, curseType);
            }

            sender.sendMessage(ChatColor.GREEN + "" + nearbyEntities.size() + 
                " oyuncuya " + curseType.name().toLowerCase() + " laneti uygulandı!");

        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Geçersiz yarıçap değeri!");
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Geçersiz lanet türü! Mevcut lanetler:");
            for (CurseType type : CurseType.values()) {
                sender.sendMessage(ChatColor.GRAY + "- " + type.name().toLowerCase());
            }
        }

        return true;
    }

    private void applyCurse(Player target, CurseType curseType) {
        target.addPotionEffect(new PotionEffect(curseType.effect, 30 * 20, 1));
        target.sendTitle(
            ChatColor.DARK_PURPLE + "LANETLENDİN!",
            ChatColor.GRAY + curseType.name().toLowerCase() + " laneti etkisi altındasın",
            10, 40, 10
        );
        target.getWorld().playSound(target.getLocation(), 
            Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 0.8f);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        switch (command.getName().toLowerCase()) {
            case "lanet":
            case "lanetgrubu":
                if (args.length == 1) {
                    if (command.getName().equalsIgnoreCase("lanet")) {
                        // Complete player names for /lanet
                        return null; // Let Bukkit handle player name completion
                    } else {
                        // Complete radius for /lanetgrubu
                        completions.addAll(Arrays.asList("5", "10", "15", "20"));
                    }
                } else if (args.length == 2) {
                    // Complete curse types
                    String input = args[1].toLowerCase();
                    for (CurseType type : CurseType.values()) {
                        String name = type.name().toLowerCase();
                        if (name.startsWith(input)) {
                            completions.add(name);
                        }
                    }
                }
                break;

            case "lanetkaldir":
            case "lanetler":
                if (args.length == 1) {
                    // Complete player names
                    return null; // Let Bukkit handle player name completion
                }
                break;
        }

        return completions;
    }
} 