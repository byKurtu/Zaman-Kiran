package org.SolarSystem.zamanKiran.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SkillSelectionGUI {
    private static final String GUI_TITLE = ChatColor.DARK_PURPLE + "Zaman Kıran Yetenekleri";
    private static final int GUI_SIZE = 54;
    private final Map<UUID, Integer> selectedSlots = new HashMap<>();
    private final Map<UUID, Integer> currentPage = new HashMap<>();
    private static final Map<Integer, Integer> GUI_TO_SKILL_SLOT = new HashMap<>();
    private final Plugin plugin;
    
    static {
        // Page 1
        GUI_TO_SKILL_SLOT.put(10, 0); // Zaman
        GUI_TO_SKILL_SLOT.put(12, 1); // Ruh
        GUI_TO_SKILL_SLOT.put(14, 2); // Savaş
        GUI_TO_SKILL_SLOT.put(16, 3); // Elementel
        
        // Page 2
        GUI_TO_SKILL_SLOT.put(28, 4); // Shadow Council
        GUI_TO_SKILL_SLOT.put(30, 5); // Soul Cage
        GUI_TO_SKILL_SLOT.put(32, 6); // Dimension Gate
        GUI_TO_SKILL_SLOT.put(34, 7); // Mass Curse
    }

    public SkillSelectionGUI(Plugin plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player player) {
        int page = currentPage.getOrDefault(player.getUniqueId(), 1);
        openGUIPage(player, page);
    }

    private void openGUIPage(Player player, int page) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE + " - Sayfa " + page);
        currentPage.put(player.getUniqueId(), page);

        // Fill empty slots with black glass
        ItemStack emptySlot = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta emptyMeta = emptySlot.getItemMeta();
        emptyMeta.setDisplayName(" ");
        emptySlot.setItemMeta(emptyMeta);

        for (int i = 0; i < GUI_SIZE; i++) {
            gui.setItem(i, emptySlot);
        }

        // Add skills based on current page
        if (page == 1) {
            addSkillItems(gui, player, true);
        } else if (page == 2) {
            addSkillItems(gui, player, false);
        }

        // Add navigation buttons
        addNavigationButtons(gui, page);

        player.openInventory(gui);
    }

    private void addNavigationButtons(Inventory gui, int currentPage) {
        // Previous page button
        if (currentPage > 1) {
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevButton.getItemMeta();
            prevMeta.setDisplayName(ChatColor.YELLOW + "« Önceki Sayfa");
            prevButton.setItemMeta(prevMeta);
            gui.setItem(45, prevButton);
        }

        // Next page button
        if (currentPage < 2) {
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextButton.getItemMeta();
            nextMeta.setDisplayName(ChatColor.YELLOW + "Sonraki Sayfa »");
            nextButton.setItemMeta(nextMeta);
            gui.setItem(53, nextButton);
        }

        // Page indicator
        ItemStack pageIndicator = new ItemStack(Material.PAPER);
        ItemMeta pageMeta = pageIndicator.getItemMeta();
        pageMeta.setDisplayName(ChatColor.GOLD + "Sayfa " + currentPage + "/2");
        pageIndicator.setItemMeta(pageMeta);
        gui.setItem(49, pageIndicator);
    }

    private void addSkillItems(Inventory gui, Player player, boolean isFirstPage) {
        int selectedSlot = getSelectedSlot(player);

        if (isFirstPage) {
            // Page 1 Skills
            addSkillItem(gui, 10, Material.CLOCK, "§d§lZaman Yetenekleri",
                    Arrays.asList(
                        "§e§lSol Tık §7- §b§lChronoSphere",
                        "§e§lSağ Tık §7- §b§lTime Echo",
                        "§e§lShift + Sol Tık §7- §b§lPentagram Ritüeli",
                        "",
                        selectedSlot == 0 ? "§a§l» Seçili" : "§e§lSeçmek için tıkla"
                    ), selectedSlot == 0);

            addSkillItem(gui, 12, Material.SOUL_LANTERN, "§d§lRuh Yetenekleri",
                    Arrays.asList(
                        "§e§lSol Tık §7- §b§lRuh Kafesi",
                        "§e§lSağ Tık §7- §b§lReality Shatter",
                        "§e§lShift + Sol Tık §7- §b§lGölge İstilası",
                        "",
                        selectedSlot == 1 ? "§a§l» Seçili" : "§e§lSeçmek için tıkla"
                    ), selectedSlot == 1);

            addSkillItem(gui, 14, Material.NETHERITE_SWORD, "§d§lSavaş Yetenekleri",
                    Arrays.asList(
                        "§e§lSol Tık §7- §b§lWave Cutting",
                        "§e§lSağ Tık §7- §b§lTemporal Storm",
                        "§e§lShift + Sol Tık §7- §b§lMod Değiştir",
                        "",
                        selectedSlot == 2 ? "§a§l» Seçili" : "§e§lSeçmek için tıkla"
                    ), selectedSlot == 2);

            addSkillItem(gui, 16, Material.LIGHTNING_ROD, "§d§lElementel Yetenekler",
                    Arrays.asList(
                        "§e§lSol Tık §7- §b§lPortal",
                        "§e§lSağ Tık §7- §b§lTime Weaver",
                        "§e§lShift + Sol Tık §7- §b§lChain Lightning",
                        "",
                        selectedSlot == 3 ? "§a§l» Seçili" : "§e§lSeçmek için tıkla"
                    ), selectedSlot == 3);

        } else {
            // Page 2 Skills
            addSkillItem(gui, 28, Material.WITHER_SKELETON_SKULL, "§d§lGölge Konseyi",
                    Arrays.asList(
                        "§e§lSol Tık §7- §b§lNPC'leri çağır",
                        "§e§lSağ Tık §7- §b§lEnerji topu",
                        "§e§lShift + Sol Tık §7- §b§lKonseyi dağıt",
                        "",
                        "§c§lBekleme: §e60 saniye",
                        selectedSlot == 4 ? "§a§l» Seçili" : "§e§lSeçmek için tıkla"
                    ), selectedSlot == 4);

            addSkillItem(gui, 30, Material.SOUL_LANTERN, "§d§lRuh Kafesi",
                    Arrays.asList(
                        "§e§lSol Tık §7- §b§lRuhları hapset",
                        "§e§lSağ Tık §7- §b§lRuhları serbest bırak",
                        "",
                        "§c§lBekleme: §e45 saniye",
                        selectedSlot == 5 ? "§a§l» Seçili" : "§e§lSeçmek için tıkla"
                    ), selectedSlot == 5);

            addSkillItem(gui, 32, Material.END_PORTAL_FRAME, "§d§lBoyut Kapısı",
                    Arrays.asList(
                        "§e§lSol Tık §7- §b§lBoyut kapısı aç",
                        "§e§lSağ Tık §7- §b§lDüşmanları içeri çek",
                        "",
                        "§c§lBekleme: §e90 saniye",
                        selectedSlot == 6 ? "§a§l» Seçili" : "§e§lSeçmek için tıkla"
                    ), selectedSlot == 6);

            addSkillItem(gui, 34, Material.WITHER_ROSE, "§d§lToplu Lanetleme",
                    Arrays.asList(
                        "§e§lSol Tık §7- §b§lYakındakileri lanetle",
                        "§e§lSağ Tık §7- §b§lLanetleri kaldır",
                        "",
                        "§c§lBekleme: §e120 saniye",
                        selectedSlot == 7 ? "§a§l» Seçili" : "§e§lSeçmek için tıkla"
                    ), selectedSlot == 7);
        }
    }

    private void addSkillItem(Inventory gui, int slot, Material material, String name, List<String> lore, boolean isSelected) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((isSelected ? "§a" : "") + name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        if (isSelected) {
            // Add glowing effect for selected skill
            item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.LUCK, 1);
            ItemMeta itemMeta = item.getItemMeta();
            itemMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(itemMeta);
        }
        
        gui.setItem(slot, item);
    }

    public void handleClick(Player player, int slot) {
        if (slot >= 45 && slot <= 53) {
            handleNavigationClick(player, slot);
            return;
        }

        Integer skillSlot = GUI_TO_SKILL_SLOT.get(slot);
        if (skillSlot != null) {
            selectedSlots.put(player.getUniqueId(), skillSlot);
            openGUI(player);
        }
    }

    private void handleNavigationClick(Player player, int slot) {
        int currentPage = this.currentPage.getOrDefault(player.getUniqueId(), 1);
        
        if (slot == 45 && currentPage > 1) {
            openGUIPage(player, currentPage - 1);
        } else if (slot == 53 && currentPage < 2) {
            openGUIPage(player, currentPage + 1);
        }
    }

    public int getSelectedSlot(Player player) {
        return selectedSlots.getOrDefault(player.getUniqueId(), -1);
    }
} 