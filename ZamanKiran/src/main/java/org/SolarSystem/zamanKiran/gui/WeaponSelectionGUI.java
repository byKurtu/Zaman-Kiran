package org.SolarSystem.zamanKiran.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

public class WeaponSelectionGUI {
    private static final String GUI_TITLE = ChatColor.GOLD + "Zaman Kıran Silahları";
    private static final int GUI_SIZE = 27;
    private final Plugin plugin;

    public WeaponSelectionGUI(Plugin plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);

        addWeaponItem(gui, 10, Material.DIAMOND_PICKAXE, "§4✧ Zaman Kıran §4✧",
            Arrays.asList(
                "§8• §7Efsanevi bir silah...",
                "§8• §cHasar: §4❁ 9.200.000",
                "§8• §bÖzellikler:",
                "§8  ⦿ §eDiken X",
                "§8  ⦿ §eKeskinlik X",
                "§8  ⦿ §eAteş Koruması X",
                "§8  ⦿ §eKırılmazlık X",
                "",
                "§6➸ Seçmek için tıkla!"
            )
        );

        addWeaponItem(gui, 12, Material.BOW, "§b✧ Zaman Yayı §b✧",
            Arrays.asList(
                "§8• §7Zamanın özünden yapılmış...",
                "§8• §cHasar: §4❁ 4.600.000",
                "§8• §bÖzellikler:",
                "§8  ⦿ §eGüç X",
                "§8  ⦿ §eAlev X",
                "§8  ⦿ §eSonsuz I",
                "§8  ⦿ §eKırılmazlık X",
                "",
                "§6➸ Yakında Gelecek!"
            )
        );

        addWeaponItem(gui, 14, Material.BLAZE_ROD, "§5✧ Zaman Asası §5✧",
            Arrays.asList(
                "§8• §7Zamanın akışını kontrol eden...",
                "§8• §cHasar: §4❁ 6.900.000",
                "§8• §bÖzellikler:",
                "§8  ⦿ §eBüyü Gücü X",
                "§8  ⦿ §eVuruş X",
                "§8  ⦿ §eAlev Görünümü II",
                "§8  ⦿ §eKırılmazlık X",
                "",
                "§6➸ Yakında Gelecek!"
            )
        );

        addWeaponItem(gui, 16, Material.NETHERITE_AXE, "§c✧ Zaman Baltası §c✧",
            Arrays.asList(
                "§8• §7Zamanı ikiye bölen...",
                "§8• §cHasar: §4❁ 8.100.000",
                "§8• §bÖzellikler:",
                "§8  ⦿ §eKeskinlik X",
                "§8  ⦿ §eServet III",
                "§8  ⦿ §eVerimlilik X",
                "§8  ⦿ §eKırılmazlık X",
                "",
                "§6➸ Yakında Gelecek!"
            )
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

        player.openInventory(gui);
    }

    private void addWeaponItem(Inventory gui, int slot, Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addEnchant(Enchantment.DURABILITY, 10, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        gui.setItem(slot, item);
    }

    public void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked != null && clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
            String name = clicked.getItemMeta().getDisplayName();
            
            if (name.contains("Zaman Kıran")) {
                Bukkit.dispatchCommand(player, "zamankiran ver");
                player.closeInventory();
            } else {
                player.sendMessage(ChatColor.RED + "Bu silah henüz mevcut değil!");
            }
        }
    }
} 