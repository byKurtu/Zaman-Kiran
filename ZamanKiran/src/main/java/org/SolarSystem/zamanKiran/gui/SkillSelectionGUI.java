package org.SolarSystem.zamanKiran.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkillSelectionGUI {
    private static final String GUI_TITLE = ChatColor.DARK_PURPLE + "Zaman Kıran Yetenekleri";
    private static final int GUI_SIZE = 27;
    private final Map<UUID, Integer> selectedSlots = new HashMap<>();
    private static final Map<Integer, Integer> GUI_TO_SKILL_SLOT = new HashMap<>();
    
    static {
        GUI_TO_SKILL_SLOT.put(10, 0); // First row, second slot -> Skill slot 0
        GUI_TO_SKILL_SLOT.put(12, 1); // First row, fourth slot -> Skill slot 1
        GUI_TO_SKILL_SLOT.put(14, 2); // First row, sixth slot -> Skill slot 2
        GUI_TO_SKILL_SLOT.put(16, 3); // First row, eighth slot -> Skill slot 3
    }

    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);

        addSkillItem(gui, 10, Material.CLOCK, "§6Slot 0: Zaman Yetenekleri",
            "§7ChronoSphere (Shift + Sağ Tık)",
            "§8• §fZaman duracak ve düşmanlar donar",
            "§8• §fSüre: §e5 saniye",
            "§8• §fBekleme: §c30 saniye",
            "",
            "§7Recursion (Sağ Tık) §8[Yakında]",
            "§8• §fZamanı tekrarla",
            "",
            "§7Döndürme Işınları (Shift + Sol Tık) §8[Yakında]",
            "§8• §fDönen ışın saldırısı"
        );

        addSkillItem(gui, 12, Material.LIGHTNING_ROD, "§6Slot 1: Yıldırım Yetenekleri",
            "§7Ball Lightning (Sağ Tık)",
            "§8• §fYıldırım topu fırlat",
            "§8• §fHasar: §c10",
            "§8• §fBekleme: §c15 saniye",
            "",
            "§7Zamanı Geriye Sar (Shift + Sol Tık) §8[Yakında]",
            "§8• §fSon konuma dön",
            "",
            "§7Portal (Shift + Sağ Tık) §8[Yakında]",
            "§8• §fIşınlanma portalı"
        );

        addSkillItem(gui, 14, Material.NETHERITE_SWORD, "§6Slot 2: Kılıç Yetenekleri",
            "§7Wave Cutting (Sol Tık)",
            "§8• §fEnerji dalgası gönder",
            "§8• §fHasar: §c15",
            "§8• §fBekleme: §c10 saniye",
            "",
            "§7Silah Fırlat (Sağ Tık)",
            "§8• §fSilahını fırlat",
            "§8• §fHasar: §c15",
            "§8• §fBekleme: §c5 saniye"
        );

        addSkillItem(gui, 16, Material.GOLDEN_CARROT, "§6Slot 3: Özel Yetenekler",
            "§7Weapon Throw (Sağ Tık)",
            "§8• §fSilahını fırlat",
            "§8• §fHasar: §c15",
            "§8• §fBekleme: §c5 saniye"
        );

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < GUI_SIZE; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }

        int currentSlot = getSelectedSlot(player);
        int guiSlot = getGUISlotFromSkillSlot(currentSlot);
        ItemStack currentItem = gui.getItem(guiSlot);
        if (currentItem != null && currentItem.hasItemMeta()) {
            ItemMeta meta = currentItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§a✦ " + meta.getDisplayName() + " §a✦");
                currentItem.setItemMeta(meta);
            }
        }

        player.openInventory(gui);
    }

    private void addSkillItem(Inventory gui, int slot, Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        gui.setItem(slot, item);
    }

    public void selectSlot(Player player, int skillSlot) {
        if (skillSlot >= 0 && skillSlot <= 3) {
            selectedSlots.put(player.getUniqueId(), skillSlot);
        }
    }

    public int getSelectedSlot(Player player) {
        return selectedSlots.getOrDefault(player.getUniqueId(), 0);
    }

    private int getGUISlotFromSkillSlot(int skillSlot) {
        return 10 + (skillSlot * 2);
    }

    private int getSkillSlotFromGUISlot(int guiSlot) {
        return GUI_TO_SKILL_SLOT.getOrDefault(guiSlot, 0);
    }

    public int convertGUISlotToSkillSlot(int rawSlot) {
        return GUI_TO_SKILL_SLOT.getOrDefault(rawSlot, 0);
    }
} 