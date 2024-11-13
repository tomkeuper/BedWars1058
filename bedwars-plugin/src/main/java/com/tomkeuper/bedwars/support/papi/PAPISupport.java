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

package com.tomkeuper.bedwars.support.papi;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.arena.team.ITeam;
import com.tomkeuper.bedwars.api.language.Language;
import com.tomkeuper.bedwars.api.language.Messages;
import com.tomkeuper.bedwars.api.party.Party;
import com.tomkeuper.bedwars.api.stats.IPlayerStats;
import com.tomkeuper.bedwars.arena.Arena;
import com.tomkeuper.bedwars.commands.shout.ShoutCommand;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.tomkeuper.bedwars.api.language.Language.getMsg;


public class PAPISupport extends PlaceholderExpansion {

    @NotNull
    @Override
    public String getIdentifier() {
        return "bw2023";
    }

    @NotNull
    @Override
    public String getAuthor() {
        return "MrCeasar";
    }

    @NotNull
    @Override
    public String getVersion() {
        return BedWars.plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }
    
    private final SimpleDateFormat nextEventFormat = new SimpleDateFormat("mm:ss");
    
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String s) {

        /* Non-Player required placeholders */

        if (s.startsWith("arena_status_")) {
            IArena a = Arena.getArenaByName(s.replace("arena_status_", ""));
            if (a == null) {
                return player == null ? Language.getDefaultLanguage().m(Messages.ARENA_STATUS_RESTARTING_NAME) :
                        Language.getMsg(player, Messages.ARENA_STATUS_RESTARTING_NAME);
            }
            return a.getDisplayStatus(Language.getDefaultLanguage());
        }

        if (s.startsWith("arena_count_")) {
            int players = 0;

            String[] arenas = s.replace("arena_count_", "").split("\\+");
            IArena a;
            for (String arena : arenas) {
                a = Arena.getArenaByName(arena);
                if (a != null) {
                    players += a.getPlayers().size();
                }
            }

            return String.valueOf(players);
        }

        if (s.startsWith("group_count_")) {
            return String.valueOf(Arena.getPlayers(s.replace("group_count_", "")));
        }

        if (s.startsWith("arena_group_")) {
            String a = s.replace("arena_group_", "");
            IArena arena = Arena.getArenaByName(a);
            if (arena != null) {
                return arena.getGroup();
            }
            return "-";
        }

        /* Player required placeholders */
        if (player == null) return null;

        /* Arena required placeholders */
        IArena arena = BedWars.getAPI().getArenaUtil().getArenaByPlayer(player);
        
        // stats placeholders
        if(s.startsWith("stats_")) {
            String targetedStat = s.replaceFirst("stats_", "");
            if(targetedStat.isEmpty() || targetedStat.isBlank()) {
                return null;
            }
            IPlayerStats stats = BedWars.getStatsManager().getUnsafe(player.getUniqueId());
            if(stats == null) {
                return null;
            }
            switch (targetedStat) {
                case "firstplay":
                    Instant firstPlay = stats.getFirstPlay();
                    return new SimpleDateFormat(getMsg(player, Messages.FORMATTING_STATS_DATE_FORMAT)).format(firstPlay != null ? Timestamp.from(firstPlay) : null);
                case "lastplay":
                    Instant lastPlay = stats.getLastPlay();
                    return new SimpleDateFormat(getMsg(player, Messages.FORMATTING_STATS_DATE_FORMAT)).format(lastPlay != null ? Timestamp.from(lastPlay) : null);
                case "total_kills":
                    return String.valueOf(stats.getTotalKills());
                case "kills":
                    return String.valueOf(stats.getKills());
                case "wins":
                    return String.valueOf(stats.getWins());
                case "finalkills":
                    return String.valueOf(stats.getFinalKills());
                case "deaths":
                    return String.valueOf(stats.getDeaths());
                case "losses":
                    return String.valueOf(stats.getLosses());
                case "finaldeaths":
                    return String.valueOf(stats.getFinalDeaths());
                case "bedsdestroyed":
                    return String.valueOf(stats.getBedsDestroyed());
                case "gamesplayed":
                    return String.valueOf(stats.getGamesPlayed());
            }
        }

        // party placeholders
        Party party = BedWars.getAPI().getPartyUtil();
        if (s.equalsIgnoreCase("party_has"))
            return String.valueOf(party.hasParty(player));
        if (s.startsWith("party_members")) {
            StringBuilder output = new StringBuilder();
            List<Player> list = new ArrayList<>(party.getMembers(player));
            if (s.equalsIgnoreCase("party_members_amount"))
                output = new StringBuilder(String.valueOf(list.size()));
            else if (s.equalsIgnoreCase("party_members")) {
                for (Player pl : list) {
                    output.append(pl.getName());
                    if (list.indexOf(pl) != list.size() - 1) output.append(", ");
                }
            }
            return output.toString();
        }
        if (s.startsWith("party_in_yours_")) {
            Player p2 = Bukkit.getServer().getPlayer(s.replace("party_in_yours_",""));
            if (p2 == null) return "false";
            return String.valueOf(party.isMember(player, p2));
        }
        if (s.startsWith("party_in_his_")) {
            Player p2 = Bukkit.getServer().getPlayer(s.replace("party_in_his_",""));
            if (p2 == null) return "false";
            return String.valueOf(party.isMember(p2, player));
        }
        if (s.startsWith("party_is_owner")) {
            if (s.equalsIgnoreCase("party_is_owner")) return String.valueOf(party.isOwner(player));
            if (s.startsWith("party_is_owner_")) {
                Player p2 = Bukkit.getServer().getPlayer(s.replace("party_is_owner_", ""));
                if (p2 == null) return "false";
                return String.valueOf(party.isOwner(p2));
            }
        }

        Language lang = BedWars.getAPI().getPlayerLanguage(player);
        if (s.startsWith("lang"))
            return lang.getLangName();

        // team placeholders
        if (s.startsWith("team_status")) {
            ITeam team;
            if (arena == null) return "";
            if (s.equalsIgnoreCase("team_status"))
                team = arena.getTeam(player);
            else team = arena.getTeam(s.replace("team_status_", ""));
            if (team != null)
                return !team.isBedDestroyed() ? lang.getString(Messages.FORMATTING_SCOREBOARD_TEAM_ALIVE) : !team.getMembers().isEmpty() ? String.valueOf(team.getMembers().size()) : lang.getString(Messages.FORMATTING_SCOREBOARD_TEAM_ELIMINATED);
        }
        if (s.startsWith("team_color")) {
            ITeam team;
            if (arena == null) return "";
            if (s.equalsIgnoreCase("team_color"))
                team = arena.getTeam(player);
            else team = arena.getTeam(s.replace("team_color_", ""));
            if (team != null)
                return team.getColor().chat().toString();
        }
        if (s.startsWith("team_letter")) {
            ITeam team;
            if (arena == null) return "";
            if (s.equalsIgnoreCase("team_letter"))
                team = arena.getTeam(player);
            else team = arena.getTeam(s.replace("team_letter_", ""));
            if (team != null)
                return team.getName().substring(0,1).toUpperCase();
        }
        if (s.startsWith("team_players_amount")) {
            ITeam team;
            if (arena == null) return "";
            if (s.equalsIgnoreCase("team_players_amount"))
                team = arena.getTeam(player);
            else team = arena.getTeam(s.replace("team_players_amount_", ""));
            if (team != null)
                return String.valueOf(team.getMembers().size());
        }
        if (s.startsWith("team_players")) {
            ITeam team;
            if (arena == null) return "";
            if (s.equalsIgnoreCase("team_players"))
                team = arena.getTeam(player);
            else team = arena.getTeam(s.replace("team_players_", ""));
            if (team != null) {
                StringBuilder output = new StringBuilder();
                List<Player> list = new ArrayList<>(team.getMembers());
                for (Player pl : list) {
                    output.append(pl.getName());
                    if (list.indexOf(pl) != list.size()-1) output.append(", ");
                }
                return output.toString();
            }
        }

        // arena info placeholders
        switch (s) {
            case "arena_nextevent_name":
                return (arena != null) ? arena.getNextEvent().toString().toLowerCase().replace("_", " ") : "";
            case "arena_nextevent_time":
                return (arena != null) ? String.valueOf(getNextEventTime(arena)) : "";
            case "arena_nextevent_time_formatted":
                return (arena != null) ? nextEventFormat.format(new Date(getNextEventTime(arena) * 1000L)) : "";
            case "arena_name":
                return (arena != null) ? arena.getArenaName() : "";
            case "arena_display_name":
                return (arena != null) ? arena.getDisplayName() : "";
            case "arena_group":
                return (arena != null) ? arena.getGroup() : "";
            case "arena_world":
                return (arena != null) ? arena.getWorldName() : "";
            case "arena_status_plocale":
                return (arena != null) ? arena.getDisplayStatus(lang) : "";
            case "arena_status":
                return (arena != null) ? arena.getDisplayStatus(BedWars.getAPI().getDefaultLang()) : "";
        }
        
        // inside arena stats placeholders
        switch (s) {
            case "player_kills": return (arena != null) ? String.valueOf(arena.getPlayerKills(player, false)) : "";
            case "player_kills_total": return (arena != null) ? String.valueOf(arena.getPlayerKills(player,true)+arena.getPlayerKills(player,false)) : "";
            case "player_kills_final": return (arena != null) ? String.valueOf(arena.getPlayerKills(player, true)) : "";
            case "player_deaths": return (arena != null) ? String.valueOf(arena.getPlayerDeaths(player, false)) : "";
            case "player_deaths_total": return (arena != null) ? String.valueOf(arena.getPlayerDeaths(player,true)+arena.getPlayerDeaths(player,false)) : "";
            case "player_deaths_final": return (arena != null) ? String.valueOf(arena.getPlayerDeaths(player, true)) : "";
            case "player_beds": return (arena != null) ? String.valueOf(arena.getPlayerBedsDestroyed(player)) : "";
            case "status_color": return (arena != null) ? (arena.isSpectator(player) ? ChatColor.GRAY : arena.getTeam(player).getColor().chat()).toString() : "";
            case "status_letter": return (arena != null) ? arena.isSpectator(player) ? getSpectatorLetter(lang) : arena.getTeam(player).getName().substring(0,1).toUpperCase() : "";
        }
        if (s.startsWith("players")) {
            StringBuilder output = new StringBuilder();
            List<Player> list = new ArrayList<>(arena.getPlayers());
            if (s.equalsIgnoreCase("players_amount"))
                output = new StringBuilder(String.valueOf(list.size()));
            else if (s.equalsIgnoreCase("players")) {
                for (Player pl : list) {
                    output.append(pl.getName());
                    if (list.indexOf(pl) != list.size() - 1) output.append(", ");
                }
            }
            return output.toString();
        }
        // other placeholders
        String response = "";
        IArena a = Arena.getArenaByPlayer(player);
        switch (s) {
            case "current_online":
                response = String.valueOf(Arena.getArenaByPlayer().size());
                break;
            case "current_arenas":
                response = String.valueOf(Arena.getArenas().size());
                break;
            case "current_playing":
                if (a != null) {
                    response = String.valueOf(a.getPlayers().size());
                }
                break;
            case "player_team_color":
                if (a != null && a.isPlayer(player) && a.getStatus() == GameState.playing) {
                    ITeam team = a.getTeam(player);
                    if (team != null) {
                        response += String.valueOf(team.getColor().chat());
                    }
                }
                break;
            case "player_team":
                if (a != null) {
                    if (ShoutCommand.isShout(player)) {
                        response += Language.getMsg(player, Messages.FORMAT_PAPI_PLAYER_TEAM_SHOUT);
                    }
                    if (a.isPlayer(player)) {
                        if (a.getStatus() == GameState.playing) {
                            ITeam bwt = a.getTeam(player);
                            if (bwt != null) {
                                response += Language.getMsg(player, Messages.FORMAT_PAPI_PLAYER_TEAM_TEAM).replace("{TeamName}",
                                        bwt.getDisplayName(Language.getPlayerLanguage(player))).replace("{TeamColor}", String.valueOf(bwt.getColor().chat()));
                            }
                        }
                    } else {
                        response += Language.getMsg(player, Messages.FORMAT_PAPI_PLAYER_TEAM_SPECTATOR);
                    }
                }
                break;
            case "player_level":
                response = BedWars.getLevelSupport().getLevel(player);
                break;
            case "player_level_trim":
                response = BedWars.getLevelSupport().getLevel(player).trim();
                break;
            case "player_level_strip":
                response = BedWars.getLevelSupport().getLevel(player).replaceAll("\\[", "").replaceAll("]","");
                break;
            case "player_level_strip_trim":
                response = BedWars.getLevelSupport().getLevel(player).replaceAll("\\[", "").replaceAll("]","").trim();
                break;
            case "player_level_raw":
                response = String.valueOf(BedWars.getLevelSupport().getPlayerLevel(player));
                break;
            case "player_progress":
                response = BedWars.getLevelSupport().getProgressBar(player);
                break;
            case "player_xp_formatted":
                response = BedWars.getLevelSupport().getCurrentXpFormatted(player);
                break;
            case "player_xp":
                response = String.valueOf(BedWars.getLevelSupport().getCurrentXp(player));
                break;
            case "player_rerq_xp_formatted":
                response = BedWars.getLevelSupport().getRequiredXpFormatted(player);
                break;
            case "player_rerq_xp":
                response = String.valueOf(BedWars.getLevelSupport().getRequiredXp(player));
                break;
            case "player_status":
                if(a != null) {
                    switch (a.getStatus()) {
                        case waiting:
                        case starting:
                            response = "WAITING";
                            break;
                        case playing:
                            if(a.isPlayer(player)) {
                                response = "PLAYING";
                            } else if(a.isSpectator(player)) {
                                response = "SPECTATING";
                            } else {
                                response = "IN_GAME_BUT_NOT"; // this shouldn't happen
                            }
                            break;
                        case restarting:
                            response = "RESTARTING";
                            break;
                    }
                } else {
                    response = "NONE";
                }
                break;
            case "current_arena_group":
                if (a != null) {
                    response = a.getGroup();
                }
                break;
            case "elapsed_time":
                if (a != null) {
                    Instant startTime = a.getStartTime();
                    if (null != startTime){
                        Duration time = Duration.ofMillis(Instant.now().minusMillis(startTime.toEpochMilli()).toEpochMilli());
                        if (time.toHours() == 0){
                            response = String.format("%02d:%02d", time.toMinutes() % 60, time.toSeconds() % 60);
                        } else {
                            response = String.format("%02d:%02d:%02d", time.toHours(), time.toMinutes() % 60, time.toSeconds() % 60);
                        }
                    } else response = "";
                }
                break;
        }
        return response;
    }

    private String getSpectatorLetter(Language lang) {
        String letter = lang.m(Messages.FORMAT_PAPI_PLAYER_TEAM_SPECTATOR+"-letter");
        return letter.equals("MISSING_LANG") ? "S" : letter;
    }
    
    private int getNextEventTime(IArena arena) {
        switch (arena.getNextEvent()) {
            case EMERALD_GENERATOR_TIER_II:
            case EMERALD_GENERATOR_TIER_III: return arena.getUpgradeEmeraldsCount();
            case DIAMOND_GENERATOR_TIER_II:
            case DIAMOND_GENERATOR_TIER_III: return arena.getUpgradeDiamondsCount();
            case BEDS_DESTROY: return arena.getPlayingTask().getBedsDestroyCountdown();
            case ENDER_DRAGON: return arena.getPlayingTask().getDragonSpawnCountdown();
            case GAME_END: return arena.getPlayingTask().getGameEndCountdown();
            default: return 0;
        }
    }
}
