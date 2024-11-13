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

package com.tomkeuper.bedwars.listeners.chat;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.arena.team.ITeam;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.api.language.Language;
import com.tomkeuper.bedwars.api.language.Messages;
import com.tomkeuper.bedwars.api.server.ServerType;
import com.tomkeuper.bedwars.arena.Arena;
import com.tomkeuper.bedwars.commands.party.PartyCommand;
import com.tomkeuper.bedwars.commands.shout.ShoutCommand;
import com.tomkeuper.bedwars.configuration.Permissions;
import com.tomkeuper.bedwars.support.papi.SupportPAPI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.tomkeuper.bedwars.api.language.Language.getMsg;
import static com.tomkeuper.bedwars.api.language.Language.getPlayerLanguage;

public class ChatFormatting implements Listener {

    private static final List<Player> recipients = new ArrayList<>();

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        if (e == null) return;
        Player p = e.getPlayer();

        if (PartyCommand.chatToggle.getOrDefault(p.getUniqueId(), false) && BedWars.getPartyManager().hasParty(p)) {
            // Send the message only to party members
            e.getRecipients().clear();
            e.getRecipients().addAll(BedWars.getPartyManager().getMembers(p));
            String format = ChatColor.translateAlternateColorCodes('&', "&e&lPARTY &8&l┃ &b%bw_playername% &7→ ") + ChatColor.GRAY + "%bw_message%";
            e.setFormat(parsePHolders(format, e.getMessage(), p, null, null));
            return;
        }

        // in shared mode we don't want messages from outside the arena to be seen in game
        if (BedWars.getServerType() == ServerType.SHARED && Arena.getArenaByPlayer(p) == null) {
            e.getRecipients().removeIf(pl -> Arena.getArenaByPlayer(pl) != null);
            return;
        }

        // handle chat color. we would need to work on permission inheritance
        if (Permissions.hasPermission(p, Permissions.PERMISSION_CHAT_COLOR, Permissions.PERMISSION_VIP, Permissions.PERMISSION_ALL)) {
            e.setMessage(ChatColor.translateAlternateColorCodes('&', e.getMessage()));
        }

        // handle lobby world for multi arena
        if (BedWars.getServerType() == ServerType.MULTIARENA && Arena.getArenaByIdentifier(p.getWorld().getName()) == null) {
            setRecipients(e, p.getWorld().getPlayers());
        }

        Language language = getPlayerLanguage(p);
        // handle arena chat
        if (Arena.getArenaByPlayer(p) != null) {
            IArena a = Arena.getArenaByPlayer(p);

            // spectator chat
            if (a.isSpectator(p)) {
                setRecipients(e, a.getSpectators());
                sendMessage(e, language.m(Messages.FORMATTING_CHAT_SPECTATOR), e.getMessage(), p, null);
                return;
            }

            // arena lobby chat
            if (a.getStatus() == GameState.waiting || a.getStatus() == GameState.starting) {
                setRecipients(e, a.getPlayers());
                sendMessage(e, language.m(Messages.FORMATTING_CHAT_WAITING), e.getMessage(), p, null);
                return;
            }

            ITeam team = a.getTeam(p);
            String msg = e.getMessage();

            // shout format
            if (isShouting(msg, language)) {
                if (!(p.hasPermission(Permissions.PERMISSION_SHOUT_COMMAND) || p.hasPermission(Permissions.PERMISSION_ALL))) {
                    e.setCancelled(true);
                    p.sendMessage(Language.getMsg(p, Messages.COMMAND_NOT_FOUND_OR_INSUFF_PERMS));
                    return;
                }
                if (ShoutCommand.isShoutCooldown(p)) {
                    e.setCancelled(true);
                    p.sendMessage(language.m(Messages.COMMAND_COOLDOWN)
                            .replace("%bw_seconds%", String.valueOf(Math.round(ShoutCommand.getShoutCooldown(p))))
                    );
                    return;
                }
                ShoutCommand.updateShout(p);
                setRecipients(e, a.getPlayers(), a.getSpectators());
                msg = clearShout(msg, language);
                if (msg.isEmpty()) {
                    e.setCancelled(true);
                    return;
                }
                e.setMessage(msg);
                sendMessage(e, language.m(Messages.FORMATTING_CHAT_SHOUT), e.getMessage(), p, team);
                return;
            }

            // player team chat
            if (a.getMaxInTeam() == 1) {
                setRecipients(e, a.getPlayers(), a.getSpectators());
            } else {
                setRecipients(e, team.getMembers());
            }
            sendMessage(e, language.m(Messages.FORMATTING_CHAT_TEAM), e.getMessage(), p, team);
            return;
        }

        // multi arena lobby chat
        sendMessage(e, language.m(Messages.FORMATTING_CHAT_LOBBY), e.getMessage(), p, null);
    }

    private static String parsePHolders(String format, String msg, Player eventTriggerPlayer, Player recipientPlayer, @Nullable ITeam team) {
        format = format
                .replace("%bw_v_prefix%", BedWars.getChatSupport().getPrefix(eventTriggerPlayer))
                .replace("%bw_v_suffix%", BedWars.getChatSupport().getSuffix(eventTriggerPlayer))
                .replace("%bw_playername%", eventTriggerPlayer.getName())
                .replace("%bw_level%", BedWars.getLevelSupport().getLevel(eventTriggerPlayer))
                .replace("%bw_player%", eventTriggerPlayer.getDisplayName());
        if (team != null) {
            String teamFormat = getMsg(recipientPlayer, Messages.FORMAT_PAPI_PLAYER_TEAM_TEAM)
                    .replace("%bw_team_color%", String.valueOf(team.getColor().chat()))
                    .replace("%bw_team_name%", team.getDisplayName(Language.getPlayerLanguage(recipientPlayer)).toUpperCase());
            format = format.replace("%bw_team_format%", teamFormat);
        }
        return SupportPAPI.getSupportPAPI().replace(eventTriggerPlayer, format).replace("%bw_message%", msg);
    }

    private static boolean isShouting(String msg, Language lang) {
        return msg.startsWith("!") || msg.startsWith("shout") ||
                msg.startsWith("SHOUT") || msg.startsWith(lang.m(Messages.MEANING_SHOUT));
    }

    private static String clearShout(String msg, Language lang) {
        if (msg.startsWith("!")) msg = msg.replaceFirst("!", "");
        if (msg.startsWith("SHOUT")) msg = msg.replaceFirst("SHOUT", "");
        if (msg.startsWith("shout")) msg = msg.replaceFirst("shout", "");
        if (msg.startsWith(lang.m(Messages.MEANING_SHOUT))) {
            msg = msg.replaceFirst(lang.m(Messages.MEANING_SHOUT), "");
        }
        return msg.trim();
    }

    @SafeVarargs
    public static void setRecipients(AsyncPlayerChatEvent event, List<Player>... target) {
        if (!BedWars.config.getBoolean(ConfigPath.GENERAL_CHAT_GLOBAL)) {
            recipients.clear();
            for (List<Player> list : target) {
                recipients.addAll(list);
            }
        } else {
            recipients.clear();
            recipients.addAll(event.getRecipients());
        }
        event.getRecipients().clear(); // Used for console message only.
    }

    public void sendMessage(AsyncPlayerChatEvent e, String format, String msg, Player eventTriggerPlayer, ITeam team){
        e.setFormat(parsePHolders(format, msg, eventTriggerPlayer,null, team).replaceAll("%","%%")); // Used for console message only.
        for (Player player : recipients) {
            player.sendMessage(parsePHolders(format, msg, eventTriggerPlayer, player, team));
        }
    }
}
