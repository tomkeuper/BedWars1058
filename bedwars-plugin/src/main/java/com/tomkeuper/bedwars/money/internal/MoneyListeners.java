package com.tomkeuper.bedwars.money.internal;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.arena.team.ITeam;
import com.tomkeuper.bedwars.api.events.gameplay.GameEndEvent;
import com.tomkeuper.bedwars.api.events.player.PlayerBedBreakEvent;
import com.tomkeuper.bedwars.api.events.player.PlayerKillEvent;
import com.tomkeuper.bedwars.api.events.player.PlayerMoneyGainEvent;
import com.tomkeuper.bedwars.api.language.Language;
import com.tomkeuper.bedwars.api.language.Messages;
import com.tomkeuper.bedwars.configuration.MoneyConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public class MoneyListeners implements Listener {

    /**
     * Create a new winner / loser money reward.
     */
    @EventHandler
    public void onGameEnd(GameEndEvent e) {
        for (UUID p : e.getWinners()) {
            Player player = Bukkit.getPlayer(p);
            if (player == null) continue;
            int gameWin = MoneyConfig.money.getInt("money-rewards.game-win");
            if (gameWin > 0) {
                PlayerMoneyGainEvent event = new PlayerMoneyGainEvent(player, gameWin, PlayerMoneyGainEvent.MoneySource.GAME_WIN);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) return;
                BedWars.getEconomy().giveMoney(player, event.getAmount());
                player.sendMessage(Language.getMsg(player, Messages.MONEY_REWARD_WIN).replace("%bw_money%", String.valueOf(event.getAmount())));
            }
            ITeam bwt = e.getArena().getExTeam(player.getUniqueId());
            IArena arena = e.getArena();
            if (bwt != null) {
                if (arena.getMaxInTeam() > 1) {
                    int teamMate = MoneyConfig.money.getInt("money-rewards.per-teammate");
                    if (teamMate > 0) {
                        PlayerMoneyGainEvent event = new PlayerMoneyGainEvent(player, teamMate, PlayerMoneyGainEvent.MoneySource.PER_TEAMMATE);
                        Bukkit.getPluginManager().callEvent(event);
                        if (event.isCancelled()) return;
                        BedWars.getEconomy().giveMoney(player, event.getAmount());
                        player.sendMessage(Language.getMsg(player, Messages.MONEY_REWARD_PER_TEAMMATE).replace("%bw_money%", String.valueOf(event.getAmount())));
                    }
                }
            }
        }
        for (UUID p : e.getLosers()) {
            Player player = Bukkit.getPlayer(p);
            if (player == null) continue;
            ITeam bwt = e.getArena().getExTeam(player.getUniqueId());
            IArena arena = e.getArena();
            if (bwt != null) {
                if (arena.getMaxInTeam() > 1) {
                    int teamMate = MoneyConfig.money.getInt("money-rewards.per-teammate");
                    if (teamMate > 0) {
                        PlayerMoneyGainEvent event = new PlayerMoneyGainEvent(player, teamMate, PlayerMoneyGainEvent.MoneySource.PER_TEAMMATE);
                        Bukkit.getPluginManager().callEvent(event);
                        if (event.isCancelled()) return;
                        BedWars.getEconomy().giveMoney(player, event.getAmount());
                        player.sendMessage(Language.getMsg(player, Messages.MONEY_REWARD_PER_TEAMMATE).replace("%bw_money%", String.valueOf(event.getAmount())));
                    }
                }
            }
        }
    }

    /**
     * Create a new bed destroyed money reward.
     */
    @EventHandler
    public void onBreakBed(PlayerBedBreakEvent e) {
        Player player = e.getPlayer ();
        if (player == null) return;
        int bedDestroy = MoneyConfig.money.getInt("money-rewards.bed-destroyed");
        if (bedDestroy > 0) {
            PlayerMoneyGainEvent event = new PlayerMoneyGainEvent(player, bedDestroy, PlayerMoneyGainEvent.MoneySource.BED_DESTROYED);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return;
            BedWars.getEconomy().giveMoney(player, event.getAmount());
            player.sendMessage(Language.getMsg(player, Messages.MONEY_REWARD_BED_DESTROYED).replace("%bw_money%", String.valueOf(event.getAmount())));
        }
    }

    /**
     * Create a kill money reward.
     */
    @EventHandler
    public void onKill(PlayerKillEvent e) {
        Player player = e.getKiller();
        Player victim = e.getVictim();
        if (player == null || victim.equals(player)) return;
        int finalKill = MoneyConfig.money.getInt("money-rewards.final-kill");
        int regularKill = MoneyConfig.money.getInt("money-rewards.regular-kill");
        if (e.getCause().isFinalKill()) {
            if (finalKill > 0) {
                PlayerMoneyGainEvent event = new PlayerMoneyGainEvent(player, finalKill, PlayerMoneyGainEvent.MoneySource.FINAL_KILL);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) return;
                BedWars.getEconomy().giveMoney(player, event.getAmount());
                player.sendMessage(Language.getMsg(player, Messages.MONEY_REWARD_FINAL_KILL).replace("%bw_money%", String.valueOf(event.getAmount())));
            }
        } else {
            if (regularKill > 0) {
                PlayerMoneyGainEvent event = new PlayerMoneyGainEvent(player, regularKill, PlayerMoneyGainEvent.MoneySource.REGULAR_KILL);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) return;
                BedWars.getEconomy().giveMoney(player, event.getAmount());
                player.sendMessage(Language.getMsg(player, Messages.MONEY_REWARD_REGULAR_KILL).replace("%bw_money%", String.valueOf(event.getAmount())));
            }
        }
    }
}