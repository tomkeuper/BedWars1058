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

package com.tomkeuper.bedwars.api.arena.shop;

import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.shop.IShopCache;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
/**
 * Represents a category content in a shop.
 */
public interface ICategoryContent {

    /**
     * Get the slot of the content within its category.
     *
     * @return The slot number.
     */
    int getSlot();

    /**
     * Get the preview item of the content in the player's language.
     *
     * @param player The player for whom the item is being retrieved.
     * @return The ItemStack representing the content's preview item.
     */
    ItemStack getItemStack(Player player);

    /**
     * Check if the content is available for quick buy for the player.
     *
     * @param player The player to check for.
     * @return {@code true} if the player has the content available for quick buy, {@code false} otherwise.
     */
    boolean hasQuick(Player player);

    /**
     * Check if the content is permanent.
     *
     * @return {@code true} if the content is permanent, {@code false} otherwise.
     */
    boolean isPermanent();

    /**
     * Check if the content is downgradable.
     *
     * @return {@code true} if the content is downgradable, {@code false} otherwise.
     */
    boolean isDowngradable();

    /**
     * Check if the content is upgradeable
     *
     * @return if the code is upgradable (Has more than 1 tier)
     */
    boolean isUpgradable();

    /**
     * Get the identifier of the category content.
     *
     * @return The identifier of the category content.
     */
    String getIdentifier();

    /**
     * Get the path name of the category content.
     *
     * @return The path name of the category content.
     */
    String getCategoryIdentifier();

    /**
     * Get the list of content tiers for the category content.
     *
     * @return The list of content tiers.
     */
    List<IContentTier> getContentTiers();

    /**
     * Execute the category content action for the player.
     *
     * @param player     The player who is executing the action.
     * @param shopCache  The shop cache containing the player's data.
     * @param slot       The slot number of the content.
     */
    boolean execute(Player player, IShopCache shopCache, int slot);

    /**
     * Give the items associated with the category content to the player.
     *
     * @param player     The player to give the items to.
     * @param shopCache  The shop cache containing the player's data.
     * @param arena      The arena context for the shop.
     */
    void giveItems(Player player, IShopCache shopCache, IArena arena);

    /**
     * Get the ItemStack representation of the category content for the player, using the shop cache.
     *
     * @param player     The player for whom the ItemStack is being retrieved.
     * @param shopCache  The shop cache containing the player's data.
     * @return The ItemStack representing the category content.
     */
    ItemStack getItemStack(Player player, IShopCache shopCache);
}
