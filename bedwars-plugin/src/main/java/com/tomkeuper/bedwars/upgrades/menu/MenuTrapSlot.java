/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: andrew.dascalu@gmail.com
 */

package com.tomkeuper.bedwars.upgrades.menu;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.team.ITeam;
import com.tomkeuper.bedwars.api.language.Language;
import com.tomkeuper.bedwars.api.language.Messages;
import com.tomkeuper.bedwars.api.upgrades.EnemyBaseEnterTrap;
import com.tomkeuper.bedwars.api.upgrades.MenuContent;
import com.tomkeuper.bedwars.upgrades.UpgradesManager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MenuTrapSlot implements MenuContent {

    private ItemStack displayItem;
    private String name;
    private int trap;

    /**
     * @param displayItem display item.
     */
    public MenuTrapSlot(String name, ItemStack displayItem) {
        this.displayItem = BedWars.nms.addCustomData(displayItem, "MCONT_" + name);
        this.name = name;
        Language.saveIfNotExists(Messages.UPGRADES_TRAP_SLOT_ITEM_NAME_PATH + name.replace("trap-slot-", ""), "&cName not set");
        Language.saveIfNotExists(Messages.UPGRADES_TRAP_SLOT_ITEM_LORE1_PATH + name.replace("trap-slot-", ""), Collections.singletonList("&cLore1 not set"));
        Language.saveIfNotExists(Messages.UPGRADES_TRAP_SLOT_ITEM_LORE2_PATH + name.replace("trap-slot-", ""), Collections.singletonList("&cLore2 not set"));
        trap = BedWars.getUpgradeManager().getConfiguration().getInt(name + ".trap");
        if (trap < 0) trap = 0;
        if (trap != 0) trap -= 1;
    }

    @Override
    public ItemStack getDisplayItem(Player player, ITeam team) {
        ItemStack i = displayItem.clone();
        EnemyBaseEnterTrap ebe = null;
        if (!team.getActiveTraps().isEmpty()) {
            if (team.getActiveTraps().size() > trap) {
                ebe = team.getActiveTraps().get(trap);
            }
        }
        if (ebe != null){
            i = ebe.getItemStack().clone();
        }
        i.setAmount(trap+1);
        ItemMeta im = i.getItemMeta();
        if (im == null) return i;
        im.setDisplayName(Language.getMsg(player, Messages.UPGRADES_TRAP_SLOT_ITEM_NAME_PATH + name.replace("trap-slot-", ""))
                .replace("%bw_name%", Language.getMsg(player, ebe == null ? Messages.MEANING_NO_TRAP : ebe.getNameMsgPath()))
                .replace("%bw_color%", Language.getMsg(player, ebe == null ? Messages.FORMAT_UPGRADE_COLOR_CANT_AFFORD : Messages.FORMAT_UPGRADE_COLOR_UNLOCKED)));
        List<String> lore = new ArrayList<>();
        if (ebe == null) {
            int cost = BedWars.getUpgradeManager().getConfiguration().getInt(team.getArena().getArenaName().toLowerCase() + "-upgrades-settings.trap-start-price");
            if (cost == 0) {
                cost = BedWars.getUpgradeManager().getConfiguration().getInt("default-upgrades-settings.trap-start-price");
            }
            String curr = BedWars.getUpgradeManager().getConfiguration().getString(team.getArena().getArenaName().toLowerCase() + "-upgrades-settings.trap-currency");
            if (curr == null) {
                curr = BedWars.getUpgradeManager().getConfiguration().getString("default-upgrades-settings.trap-currency");
            }
            String currency = BedWars.getUpgradeManager().getCurrencyMsg(player, cost, curr);
            if (!team.getActiveTraps().isEmpty()) {
                int multiplier = BedWars.getUpgradeManager().getConfiguration().getInt(team.getArena().getArenaName().toLowerCase() + "-upgrades-settings.trap-increment-price");
                if (multiplier == 0) {
                    multiplier = BedWars.getUpgradeManager().getConfiguration().getInt("default-upgrades-settings.trap-increment-price");
                }
                cost = cost + (team.getActiveTraps().size() * multiplier);
            }
            for (String s : Language.getList(player, Messages.UPGRADES_TRAP_SLOT_ITEM_LORE1_PATH + name.replace("trap-slot-", ""))) {
                lore.add(s.replace("%bw_cost%", String.valueOf(cost)).replace("%bw_currency%", currency));
            }
            for (String s : Language.getList(player, Messages.UPGRADES_TRAP_SLOT_ITEM_LORE2_PATH + name.replace("trap-slot-", ""))) {
                lore.add(s.replace("%bw_cost%", String.valueOf(cost)).replace("%bw_currency%", currency));
            }
        } else {
            lore.addAll(Language.getList(player, ebe.getLoreMsgPath()));
            lore.addAll(Language.getList(player, Messages.UPGRADES_TRAP_SLOT_ITEM_LORE1_PATH + name.replace("trap-slot-", "")));
        }
        im.setLore(lore);
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        i.setItemMeta(im);
        return i;
    }

    @Override
    public boolean onClick(Player player, ClickType clickType, ITeam team, boolean forFree, boolean announcePurchase, boolean announceAlreadyUnlocked, boolean openInv) {
        return false;
    }

    @Override
    public String getName() {
        return name;
    }
}
