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

package com.tomkeuper.bedwars.api.events.gameplay;

import com.tomkeuper.bedwars.api.arena.IArena;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PopUpTowerPlaceEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final IArena arena;
    private boolean cancelled = false;

    /**
     * Called when a player places a pop up tower
     */
    public PopUpTowerPlaceEvent(Player player, IArena arena) {
        this.player = player;
        this.arena = arena;
    }

    /**
     * Get player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Get arena
     */
    public IArena getArena() {
        return arena;
    }

    /**
     * Used to cancel the event
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Used to check if whether the event is cancelled
     * @return whether the event is cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }

    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
