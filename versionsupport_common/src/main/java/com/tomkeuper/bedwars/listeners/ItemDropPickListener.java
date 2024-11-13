/*
 * BedWars2023 - A bed wars mini-game.
 * Copyright (C) 2024 Tomas Keuper
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
 * Contact e-mail: contact@fyreblox.com
 */

package com.tomkeuper.bedwars.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import static com.tomkeuper.bedwars.support.version.common.VersionCommon.api;
import static com.tomkeuper.bedwars.utils.MainUtils.*;

public class ItemDropPickListener {

    // 1.11 or older
    public static class PlayerDrop implements Listener {
        @EventHandler
        public void onDrop(PlayerDropItemEvent e){
            if (manageDrop(e.getPlayer(), e.getItemDrop())) e.setCancelled(true);
        }
    }

    // 1.11 or older
    public static class PlayerPickup implements Listener {
        @SuppressWarnings("deprecation")
        @EventHandler
        public void onPickUp(PlayerPickupItemEvent e) {
            if (managePickup(e.getItem(), e.getPlayer(), getSimilarItemsAround(e.getItem()).size())) e.setCancelled(true);
        }
    }

    // 1.13 or newer
    public static class EntityDrop implements Listener {
        @EventHandler
        public void onDrop(EntityDropItemEvent e){
            if (manageDrop(e.getEntity(), e.getItemDrop())) e.setCancelled(true);
        }
    }

    // 1.12 or newer
    public static class EntityPickup implements Listener {
        @EventHandler
        public void onPickup(EntityPickupItemEvent e) {
            if (!(e.getEntity() instanceof Player)) return;
            if (managePickup(e.getItem(), e.getEntity(), getSimilarItemsAround(e.getItem()).size())) e.setCancelled(true);
        }
    }

    // 1.9 or newer
    public static class ArrowCollect implements Listener {
        @EventHandler
        public void onArrowPick(PlayerPickupArrowEvent e){
            Bukkit.getLogger().info("onArrowPick");
            if (api.getArenaUtil().isSpectating(e.getPlayer())) {
                e.setCancelled(true);
            }
        }
    }
}
