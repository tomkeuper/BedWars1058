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

package com.tomkeuper.bedwars.api.events.shop;

import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.arena.shop.IBuyItem;
import com.tomkeuper.bedwars.api.arena.shop.ICategoryContent;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents an event triggered when a player buys items from the shop.
 */
public class ShopBuyEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player buyer;
    private final IArena arena;
    private final ICategoryContent categoryContent;
    private List<IBuyItem> itemList;
    private boolean cancelled = false;

    /**
     * Creates a new ShopBuyEvent.
     *
     * @param buyer            The player who made the purchase.
     * @param arena            The arena where the purchase occurred.
     * @param categoryContent  The category content from the shop where the purchase was made.
     * @param itemList         The list of items bought by the player.
     * @param slot             The slot in the shop where the purchase was made.
     */
    public ShopBuyEvent(Player buyer, IArena arena, ICategoryContent categoryContent, List<IBuyItem> itemList) {
        this.categoryContent = categoryContent;
        this.buyer = buyer;
        this.arena = arena;
        this.itemList = itemList;
    }

    /**
     * Gets the arena where the purchase occurred.
     *
     * @return The arena.
     */
    public IArena getArena() {
        return arena;
    }

    /**
     * Gets the player who made the purchase.
     *
     * @return The player.
     */
    public Player getBuyer() {
        return buyer;
    }

    /**
     * Gets the shop category content from which items were bought.
     *
     * @return The category content.
     */
    public ICategoryContent getCategoryContent() {
        return categoryContent;
    }

    /**
     * Gets the list of items bought by the player.
     *
     * @return The list of items.
     */
    public List<IBuyItem> getItemList() {
        return itemList;
    }

    /**
     * Sets the list of items bought by the player.
     *
     * @param itemList The list of items.
     */
    public void setItemList(List<IBuyItem> itemList) {
        this.itemList = itemList;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Gets the handler list for this event.
     *
     * @return The handler list.
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * Checks if the event is cancelled.
     *
     * @return True if the event is cancelled, otherwise false.
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets whether the event is cancelled.
     *
     * @param cancelled True to cancel the event, otherwise false.
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
