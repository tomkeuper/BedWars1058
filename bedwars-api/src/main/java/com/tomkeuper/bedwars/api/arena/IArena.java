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

package com.tomkeuper.bedwars.api.arena;

import com.tomkeuper.bedwars.api.arena.generator.IGenerator;
import com.tomkeuper.bedwars.api.arena.team.ITeam;
import com.tomkeuper.bedwars.api.arena.team.ITeamAssigner;
import com.tomkeuper.bedwars.api.configuration.ConfigManager;
import com.tomkeuper.bedwars.api.language.Language;
import com.tomkeuper.bedwars.api.region.Region;
import com.tomkeuper.bedwars.api.tasks.AnnouncementTask;
import com.tomkeuper.bedwars.api.tasks.PlayingTask;
import com.tomkeuper.bedwars.api.tasks.RestartingTask;
import com.tomkeuper.bedwars.api.tasks.StartingTask;
import me.neznamy.tab.api.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;

public interface IArena {

    /**
     * Check if a player is spectating on this arena.
     *
     * @param player The player to check.
     * @return `true` if the player is spectating, `false` otherwise.
     */
    boolean isSpectator(Player player);

    /**
     * Check if a player is spectating on this arena.
     *
     * @param player The UUID of the player to check.
     * @return `true` if the player is spectating, `false` otherwise.
     */
    boolean isSpectator(UUID player);

    /**
     * Check if a player is respawning in this arena.
     *
     * @param player The UUID of the player to check.
     * @return `true` if the player is respawning, `false` otherwise.
     */
    boolean isReSpawning(UUID player);

    /**
     * Get the used world name for the arena.
     *
     * @return The name of the world used by the arena.
     */
    String getArenaName();

    /**
     * Initialize the arena after loading the world.
     * This needs to be called in order to allow players to join.
     *
     * @param world The world object associated with the arena.
     */
    void init(World world);

    /**
     * Get the configuration manager for the arena.
     *
     * @return The configuration manager.
     */
    ConfigManager getConfig();

    /**
     * Check if a player is currently playing in the arena.
     *
     * @param player The player to check.
     * @return `true` if the player is playing, `false` otherwise.
     */
    boolean isPlayer(Player player);

    /**
     * Get a list of spectators in the arena.
     *
     * @return The list of spectators.
     */
    List<Player> getSpectators();

    /**
     * Get the team of a player.
     * This method will only work if the player is alive.
     * <p>
     * Use {@link #getExTeam(UUID)} to get the team the player played for in the current match
     * if the player has been eliminated.
     *
     * @param player The player to get the team for.
     * @return The player's team.
     */
    ITeam getTeam(Player player);

    /**
     * Get the team the player played for in the current match.
     * This should be used if the player has been eliminated.
     *
     * @param player The UUID of the player.
     * @return The team the player played for in the current match.
     */
    ITeam getExTeam(UUID player);

    /**
     * Get the arena name as a message that can be used on signs, etc.
     * This replaces '-' and '_' with spaces in the arena name.
     *
     * @return The arena name with '-' and '_' replaced by spaces.
     */
    String getDisplayName();

    /**
     * Change the world name.
     *
     * @param name The new world name.
     */
    void setWorldName(String name);

    /**
     * Get the current status of the arena.
     *
     * @return The current status of the arena.
     */
    GameState getStatus();

    /**
     * Get a list of players currently in the arena.
     *
     * @return The list of players in the arena.
     */
    List<Player> getPlayers();

    /**
     * Get the maximum number of players allowed in the arena.
     *
     * @return The maximum number of players allowed.
     */
    int getMaxPlayers();

    /**
     * Get the group of the arena.
     *
     * @return The group of the arena.
     */
    String getGroup();

    /**
     * Get the maximum number of players allowed in a team.
     *
     * @return The maximum number of players allowed in a team.
     */
    int getMaxInTeam();

    /**
     * Get a map of players in the respawn screen and their remaining time.
     * The map key is the player in the respawn screen, and the value is the remaining time.
     *
     * @return The map of players in the respawn screen and their remaining time.
     */
    ConcurrentHashMap<Player, Integer> getRespawnSessions();

    /**
     * This will attempt to upgrade the next event if it is the case.
     */
    void updateNextEvent();

    /**
     * Add a player to the arena
     *
     * @param p              Player to add.
     * @param skipOwnerCheck True if you want to skip the party checking for this player. This
     * @return true if was added.
     */
    boolean addPlayer(Player p, boolean skipOwnerCheck);

    /**
     * Add a player as Spectator
     *
     * @param p            Player to be added
     * @param playerBefore True if the player has played in this arena before and he died so now should be a spectator.
     */
    boolean addSpectator(Player p, boolean playerBefore, Location staffTeleport);

    /**
     * Remove a player from the arena and execute party checks.
     *
     * @param p          Player to be removed
     * @param disconnect True if the player was disconnected
     */
    void removePlayer(Player p, boolean disconnect);

    /**
     * Remove a player from the arena
     *
     * @param p              Player to be removed
     * @param disconnect     True if the player was disconnected
     * @param skipPartyCheck (default false) True if you want to skip the party checking for this player. This will stop the player
     *                       from leaving a party if he is in one. or will stop the party from being disbanded if the
     *                       player is the owner.
     */
    void removePlayer(Player p, boolean disconnect, boolean skipPartyCheck);

    /**
     * Remove a spectator from the arena
     *
     * @param p          Player to be removed
     * @param disconnect True if the player was disconnected
     */
    void removeSpectator(Player p, boolean disconnect);

    /**
     * Remove a spectator from the arena
     *
     * @param p          Player to be removed
     * @param disconnect True if the player was disconnected
     * @param skipPartyCheck (default false) True if you want to skip the party checking for this player. This will stop the player
     *                       from leaving a party if he is in one. or will stop the party from being disbanded if the
     *                       player is the owner.
     */
    void removeSpectator(Player p, boolean disconnect, boolean skipPartyCheck);

    /**
     * Rejoin an arena.
     *
     * @param p The player who wants to rejoin the arena.
     * @return `true` if the player can rejoin the arena, `false` otherwise.
     */
    boolean reJoin(Player p);

    /**
     * Disable the arena.
     * This will automatically kick/remove the players from the arena.
     */
    void disable();

    /**
     * Restart the arena.
     */
    void restart();

    /**
     * Get the arena world.
     *
     * @return The world associated with the arena.
     */
    World getWorld();

    /**
     * Get the display status for the arena.
     * This returns a message that can be used on signs, etc.
     *
     * @param lang The language used to retrieve the display status message.
     * @return The display status message for the arena.
     */
    String getDisplayStatus(Language lang);

    /**
     * Get the arena display group for the given player.
     *
     * @param player The player for which to retrieve the display group.
     * @return The translated display group for the player.
     */
    String getDisplayGroup(Player player);

    /**
     * Get the arena display group for the given language.
     *
     * @param language The language for which to retrieve the display group.
     * @return The translated display group for the language.
     */
    @SuppressWarnings("unused")
    String getDisplayGroup(Language language);

    /**
     * Get the list of teams in the arena.
     *
     * @return The list of teams in the arena.
     */
    List<ITeam> getTeams();

    /**
     * Add a placed block to the cache.
     * This allows players to remove blocks placed by other players.
     *
     * @param block The placed block to add to the cache.
     */
    void addPlacedBlock(Block block);

    /**
     * Gets the cooldowns for fireballs in the arena.
     *
     * @return The cooldowns for fireballs, mapping player UUIDs to their respective cooldown expiration timestamps.
     */
    Map<UUID, Long> getFireballCooldowns();

    /**
     * Remove the specified placed block from the arena.
     *
     * @param block The block to remove.
     */
    void removePlacedBlock(Block block);

    /**
     * Check if the given block is placed in the arena.
     *
     * @param block The block to check.
     * @return `true` if the block is placed in the arena, `false` otherwise.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isBlockPlaced(Block block);

    /**
     * Get the number of kills for a player.
     *
     * @param p          The target player.
     * @param finalKills True to get the number of final kills, false for regular kills.
     * @return The number of kills for the player.
     */
    int getPlayerKills(Player p, boolean finalKills);

    /**
     * Get the number of beds destroyed by a player.
     *
     * @param p The target player.
     * @return The number of beds destroyed by the player.
     */
    int getPlayerBedsDestroyed(Player p);

    /**
     * Get the join signs for this arena.
     *
     * @return A list of join signs for the arena.
     */
    List<Block> getSigns();

    /**
     * Get the radius of the islands in the arena.
     *
     * @return The island radius.
     */
    int getIslandRadius();

    /**
     * Set the arena group of the arena.
     *
     * @param group The arena group name to set.
     */
    void setGroup(String group);

    /**
     * Set the game status of the arena without starting the corresponding tasks.
     *
     * @param status The game status to set.
     */
    void setStatus(GameState status);

    /**
     * Change the game status of the arena and start the corresponding tasks.
     *
     * @param status The game status to change to.
     */
    void changeStatus(GameState status);

    /**
     * Add a join sign for the arena at the specified location.
     *
     * @param loc The location of the join sign.
     */
    void addSign(Location loc);

    /**
     * Refresh the join signs for the arena.
     */
    void refreshSigns();

    /**
     * Add a player kill to the game statistics.
     *
     * @param p          The player who made the kill.
     * @param finalKill  Whether it was a final kill.
     * @param victim     The player who was killed.
     */
    void addPlayerKill(Player p, boolean finalKill, Player victim);

    /**
     * Add a bed destroyed event to the temporary statistics of a player.
     *
     * @param p The player who destroyed a bed.
     */
    void addPlayerBedDestroyed(Player p);

    /**
     * Check for a winner in the arena.
     * This method should be used to manage your win conditions and trigger the arena restart if necessary.
     */
    void checkWinner();

    /**
     * Add a player death to the temporary statistics.
     *
     * @param p The player who died.
     */
    void addPlayerDeath(Player p);

    /**
     * Set the next event for the arena.
     *
     * @param nextEvent The next event to set.
     */
    void setNextEvent(NextEvent nextEvent);

    /**
     * Get the next event for the arena.
     *
     * @return The next event.
     */
    NextEvent getNextEvent();

    /**
     * Give the pre-game command items to a player.
     * This will clear the player's inventory first.
     *
     * @param p The player to give the pre-game command items to.
     */
    void sendPreGameCommandItems(Player p);

    /**
     * Give the spectator command items to a player.
     * This will clear the player's inventory first.
     *
     * @param p The player to give the spectator command items to.
     */
    void sendSpectatorCommandItems(Player p);

    /**
     * Get a team in the arena by its name.
     *
     * @param name The name of the team.
     * @return The team with the specified name, or null if not found.
     */
    ITeam getTeam(String name);

    /**
     * Get the starting task for the arena.
     *
     * @return The starting task.
     */
    StartingTask getStartingTask();

    /**
     * Get the playing task for the arena.
     *
     * @return The playing task.
     */
    PlayingTask getPlayingTask();

    /**
     * Get the restarting task for the arena.
     *
     * @return The restarting task.
     */
    RestartingTask getRestartingTask();

    /**
     * Get the announcement task for the arena.
     *
     * @return The announcement task.
     */
    AnnouncementTask getAnnouncementTask();

    /**
     * Get the ore generators in the arena.
     *
     * @return The list of ore generators.
     */
    List<IGenerator> getOreGenerators();

    /**
     * Get the list of next events to come in the arena.
     * Note: The events are not ordered.
     *
     * @return The list of next events.
     */
    @SuppressWarnings("unused")
    List<String> getNextEvents();

    /**
     * Get the number of deaths for a specific player in the arena.
     *
     * @param p            The player.
     * @param finalDeaths  Whether to retrieve final deaths or all deaths.
     * @return The number of deaths for the player.
     */
    int getPlayerDeaths(Player p, boolean finalDeaths);

    /**
     * Show upgrade announcement to players.
     * Change diamondTier value first.
     */
    void sendDiamondsUpgradeMessages();

    /**
     * Show upgrade announcement to players.
     * Change emeraldTier value first.
     */
    void sendEmeraldsUpgradeMessages();

    /**
     * Get a list of placed blocks in the arena.
     *
     * @return A linked list of vectors representing the locations of the placed blocks.
     */
    LinkedList<Vector> getPlaced();

    /**
     * This is used to destroy arena data when it restarts.
     */
    void destroyData();

    /**
     * Get the count of upgrade diamonds.
     *
     * @return The count of upgrade diamonds.
     */
    int getUpgradeDiamondsCount();

    /**
     * Get the count of upgrade emeralds.
     *
     * @return The count of upgrade emeralds.
     */
    int getUpgradeEmeraldsCount();

    /**
     * Get the list of regions for the arena.
     *
     * @return The list of regions.
     */
    List<Region> getRegionsList();

    /**
     * Get the show time map for armor invisibility.
     *
     * @return The show time map.
     */
    ConcurrentHashMap<Player, Integer> getShowTime();

    /**
     * Set whether spectating is allowed in the arena.
     *
     * @param allowSpectate `true` to allow spectating, `false` to disallow.
     */
    @SuppressWarnings("unused")
    void setAllowSpectate(boolean allowSpectate);

    /**
     * Check if spectating is allowed in the arena.
     *
     * @return `true` if spectating is allowed, `false` otherwise.
     */
    boolean isAllowSpectate();

    /**
     * Get the name of the world associated with the arena.
     *
     * @return The name of the world.
     */
    String getWorldName();

    /**
     * Get the player's render distance in blocks.
     *
     * @return The render distance in blocks.
     */
    int getRenderDistance();

    /**
     * Put a player in a re-spawning countdown.
     *
     * @param player  The target player.
     * @param seconds The countdown in seconds. Use 0 for instant re-spawn.
     * @return `true` if the player is successfully put in a re-spawn session, `false` otherwise.
     */
    @SuppressWarnings("UnusedReturnValue")
    boolean startReSpawnSession(Player player, int seconds);

    /**
     * Check if a player is in the re-spawning screen/countdown.
     *
     * @param player The player to check.
     * @return `true` if the player is in the re-spawning screen/countdown, `false` otherwise.
     */
    boolean isReSpawning(Player player);

    /**
     * Get the re-spawning screen location.
     *
     * @return The re-spawning screen location.
     */
    Location getReSpawnLocation();

    /**
     * Get the location where spectators will spawn.
     *
     * @return The spectator spawn location.
     */
    Location getSpectatorLocation();

    /**
     * Set whether map break is allowed or not.
     *
     * @param value `true` to allow map break, `false` otherwise.
     */
    @SuppressWarnings("unused")
    void setAllowMapBreak(boolean value);

    /**
     * Check if map break is enabled.
     *
     * @return `true` if map break is enabled, `false` otherwise.
     */
    boolean isMapBreakable();

    /**
     * Get the location where players will spawn when joining (waiting/starting).
     *
     * @return The waiting location.
     */
    Location getWaitingLocation();

    /**
     * Check if the given location is protected.
     * This includes border checks, regions, island spawn protection, NPC protections, and generator protections.
     *
     * @param location The location to check.
     * @return `true` if the location is protected, `false` otherwise.
     */
    @SuppressWarnings("unused")
    boolean isProtected(Location location);

    /**
     * Triggered when a player has abandoned a game.
     * This should remove their assist from the existing team and re-join the session.
     * Note: This does not replace the `removePlayer(Player, boolean)` method.
     *
     * @param player The player who has abandoned the game.
     */
    void abandonGame(Player player);

    /**
     * Get the Y-coordinate height at which players are instantly killed.
     * Use -1 to disable void kills.
     *
     * @return The Y-coordinate height for instant kills.
     */
    int getYKillHeight();

    /**
     * Get the start time of the arena.
     *
     * @return The start time.
     */
    Instant getStartTime();

    /**
     * Get the team assigner used for assigning teams in an arena.
     *
     * @return The team assigner.
     */
    ITeamAssigner getTeamAssigner();

    /**
     * Set the team assigner used for assigning teams in an arena.
     *
     * @param teamAssigner The team assigner to be set.
     */
    @SuppressWarnings("unused")
    void setTeamAssigner(ITeamAssigner teamAssigner);

    /**
     * Get the list of dragon boss bars in the arena.
     *
     * @return The list of dragon boss bars.
     */
    List<BossBar> getDragonBossbars();

    /**
     * Check if breaking map is allowed, otherwise only placed blocks are allowed.
     * Some blocks like have a special protections, like blocks under shopkeepers, bed, etc.
     *
     * @return true if the map break is enabled, false otherwise.
     */
    boolean isAllowMapBreak();

    /**
     * Check if there is a player bed at the specified location.
     *
     * @param location The location to check for a bed.
     * @return true if there is a bed at the given location, false otherwise.
     */
    boolean isTeamBed(Location location);

    /**
     * Get the team that owns the bed at the specified location.
     *
     * @param location The location to check for a bed.
     * @return The team that owns the bed at the given location, or null if there is no bed or if the location is not in this arena's world.
     */
    @Nullable ITeam getBedsTeam(Location location);

    /**
     * Enable or disable the ender dragon from destroying blocks even when protected.
     *
     * @return true if the ender dragon can destroy blocks, false otherwise.
     */
    boolean isAllowEnderDragonDestroy();

    /**
     * Enable or disable the ender dragon from destroying blocks even when protected.
     *
     * @param allowDestory true if the ender dragon can destroy blocks, false otherwise.
     */
    @SuppressWarnings("unused")
    void setAllowEnderDragonDestroy(boolean allowDestory);

     /**
     * Get the time in seconds when the magic milk effect will expire.
     *
     * @return The time when the magic milk effect will expire.
     */
    int getMagicMilkTime();
}