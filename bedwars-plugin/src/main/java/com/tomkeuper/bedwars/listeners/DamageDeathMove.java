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

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.arena.generator.GeneratorType;
import com.tomkeuper.bedwars.api.arena.generator.IGenHolo;
import com.tomkeuper.bedwars.api.arena.generator.IGenerator;
import com.tomkeuper.bedwars.api.arena.shop.ShopHolo;
import com.tomkeuper.bedwars.api.arena.team.IBedHolo;
import com.tomkeuper.bedwars.api.arena.team.ITeam;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.api.entity.Despawnable;
import com.tomkeuper.bedwars.api.entity.GeneratorHolder;
import com.tomkeuper.bedwars.api.events.player.PlayerInvisibilityPotionEvent;
import com.tomkeuper.bedwars.api.events.player.PlayerKillEvent;
import com.tomkeuper.bedwars.api.events.team.TeamEliminatedEvent;
import com.tomkeuper.bedwars.api.language.Language;
import com.tomkeuper.bedwars.api.language.Messages;
import com.tomkeuper.bedwars.api.server.ServerType;
import com.tomkeuper.bedwars.arena.Arena;
import com.tomkeuper.bedwars.arena.LastHit;
import com.tomkeuper.bedwars.arena.SetupSession;
import com.tomkeuper.bedwars.arena.team.BedWarsTeam;
import com.tomkeuper.bedwars.configuration.Sounds;
import com.tomkeuper.bedwars.listeners.dropshandler.PlayerDrops;
import com.tomkeuper.bedwars.support.paper.PaperSupport;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.tomkeuper.bedwars.api.language.Language.getMsg;

public class DamageDeathMove implements Listener {

    private final double tntJumpStrengthReductionConstant;
    private final double tntJumpYAxisReductionConstant;
    private final double tntJumpHorizontalForgiveness;
    private final double tntDamageSelf;
    private final double tntDamageTeammates;
    private final double tntDamageOthers;

    public DamageDeathMove() {
        this.tntJumpStrengthReductionConstant = BedWars.config.getYml().getDouble(ConfigPath.GENERAL_TNT_JUMP_STRENGTH_REDUCTION);
        this.tntJumpYAxisReductionConstant = BedWars.config.getYml().getDouble(ConfigPath.GENERAL_TNT_JUMP_Y_REDUCTION);
        this.tntJumpHorizontalForgiveness = BedWars.config.getYml().getDouble(ConfigPath.GENERAL_TNT_JUMP_HORIZONTAL_FORGIVENESS);
        this.tntDamageSelf = BedWars.config.getYml().getDouble(ConfigPath.GENERAL_TNT_JUMP_DAMAGE_SELF);
        this.tntDamageTeammates = BedWars.config.getYml().getDouble(ConfigPath.GENERAL_TNT_JUMP_DAMAGE_TEAMMATES);
        this.tntDamageOthers = BedWars.config.getYml().getDouble(ConfigPath.GENERAL_TNT_JUMP_DAMAGE_OTHERS);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        World world = e.getEntity().getLocation().getWorld();

        if (BedWars.getServerType() == ServerType.MULTIARENA) {
            if (world.getName().equalsIgnoreCase(BedWars.getLobbyWorld())) {
                e.setCancelled(true);
                return;
            }
        }

        if (!(e.getEntity() instanceof Player)) return;

        Player player = (Player) e.getEntity();
        IArena arena = Arena.getArenaByPlayer(player);

        if (arena == null) return;

        if (arena.isSpectator(player)) {
            e.setCancelled(true);
            return;
        }

        if (arena.isReSpawning(player)) {
            e.setCancelled(true);
            return;
        }

        if (arena.getStatus() != GameState.playing) {
            e.setCancelled(true);
            return;
        }

        // protection after re-spawn
        if (BedWarsTeam.reSpawnInvulnerability.containsKey(player.getUniqueId())) {
            if (BedWarsTeam.reSpawnInvulnerability.get(player.getUniqueId()) > System.currentTimeMillis()) e.setCancelled(true);
            else BedWarsTeam.reSpawnInvulnerability.remove(player.getUniqueId());
        }
    }

    //Todo show player health on bow hit
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBowHit(EntityDamageByEntityEvent e) {
        if (e.isCancelled()) return;
        if (e.getEntity().getType() != EntityType.PLAYER) return;
        if (!(e.getDamager() instanceof Projectile)) return;

        Projectile projectile = (Projectile) e.getDamager();
        if (projectile.getShooter() == null) return;
        if (!(projectile.getShooter() instanceof Player)) return;

        Player player = (Player) e.getEntity();
        Player damager = (Player) projectile.getShooter();
        IArena arena = Arena.getArenaByPlayer(player);

        if (arena == null) return;
        if (arena.getStatus() != GameState.playing) return;

        // projectile hit message #696, #711
        ITeam team = arena.getTeam(player);
        Language lang = Language.getPlayerLanguage(damager);

        if (lang.m(Messages.PLAYER_HIT_BOW).isEmpty()) return;

        String message = lang.m(Messages.PLAYER_HIT_BOW)
                .replace("%bw_damage_amount%", new DecimalFormat("00.#").format(((Player) e.getEntity()).getHealth() - e.getFinalDamage()))
                .replace("%bw_player%", player.getDisplayName())
                .replace("%bw_team%", team.getColor().chat() + team.getDisplayName(lang))
                .replace("%bw_health_remaining%", new DecimalFormat("00.#").format(Math.max(((Player) e.getEntity()).getHealth() - e.getFinalDamage(), 0)));
        damager.sendMessage(message);
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            IArena a = Arena.getArenaByPlayer(p);

            if (a != null) {
                if (a.getStatus() != GameState.playing) {
                    e.setCancelled(true);
                    return;
                }

                if (a.isSpectator(p) || a.isReSpawning(p)) {
                    e.setCancelled(true);
                    return;
                }

                Player damager = null;
                if (e.getDamager() instanceof Player) {
                    damager = (Player) e.getDamager();
                    if (a.isReSpawning(damager)) {
                        e.setCancelled(true);
                        return;
                    }
                } else if (e.getDamager() instanceof Projectile) {
                    ProjectileSource shooter = ((Projectile) e.getDamager()).getShooter();
                    if (shooter instanceof Player) damager = (Player) shooter;
                    else return;
                } else if (e.getDamager() instanceof TNTPrimed) {
                    TNTPrimed tnt = (TNTPrimed) e.getDamager();

                    if (tnt.getSource() != null) {
                        if (tnt.getSource() instanceof Player) {
                            damager = (Player) tnt.getSource();

                            if (damager == p) {
                                if (tntDamageSelf > -1) e.setDamage(tntDamageSelf);

                                LivingEntity damaged = (LivingEntity) e.getEntity();
                                Vector tntLocation = tnt.getLocation().toVector();
                                tntLocation.setX(Math.floor(tntLocation.getX()) + 0.5);
                                tntLocation.setZ(Math.floor(tntLocation.getZ()) + 0.5);
                                Vector playerLocation = damaged.getLocation().toVector();

                                // Calculate the direction vector from TNT to player
                                Vector directionToPlayer = playerLocation.clone().subtract(tntLocation);

                                // Normalize the direction and scale by forgiveness factor proportionally
                                double distanceMagnitude = directionToPlayer.length();
                                double originalDistance = directionToPlayer.length();
                                Vector forgivenessVector = directionToPlayer.clone().normalize().multiply(tntJumpHorizontalForgiveness / distanceMagnitude);

                                Vector adjustedPlayerLocation = playerLocation.clone().add(forgivenessVector);

                                Vector distance = adjustedPlayerLocation.subtract(tntLocation);
                                Vector direction = distance.clone().normalize();
                                double force = ((tnt.getYield() * tnt.getYield()) / (tntJumpStrengthReductionConstant + originalDistance));
                                Vector resultingForce = direction.clone().multiply(force);
                                resultingForce.setY(resultingForce.getY() / (originalDistance + tntJumpYAxisReductionConstant));
                                damaged.setVelocity(resultingForce);
                            } else {
                                ITeam currentTeam = a.getTeam(p);
                                ITeam damagerTeam = a.getTeam(damager);
                                if (currentTeam == damagerTeam) if (tntDamageTeammates > -1) e.setDamage(tntDamageTeammates);
                                else if (tntDamageOthers > -1) e.setDamage(tntDamageOthers);
                            }
                        } else return;
                    }
                } else if ((e.getDamager() instanceof Silverfish) || (e.getDamager() instanceof IronGolem)) {
                    LastHit lh = LastHit.getLastHit(p);
                    if (lh != null) {
                        lh.setDamager(e.getDamager());
                        lh.setTime(System.currentTimeMillis());
                    } else {
                        new LastHit(p, e.getDamager(), System.currentTimeMillis());
                    }
                }
                if (damager != null) {
                    if (a.isSpectator(damager) || a.isReSpawning(damager.getUniqueId())) {
                        e.setCancelled(true);
                        return;
                    }

                    if (a.getTeam(p) == a.getTeam(damager)) {
                        if (!(e.getDamager() instanceof TNTPrimed)) e.setCancelled(true);
                        return;
                    }

                    // If the damager is the re-spawning player, remove protection
                    BedWarsTeam.reSpawnInvulnerability.remove(damager.getUniqueId());

                    LastHit lh = LastHit.getLastHit(p);
                    if (lh != null) {
                        lh.setDamager(damager);
                        lh.setTime(System.currentTimeMillis());
                    } else new LastHit(p, damager, System.currentTimeMillis());

                    // #274
                    // if player gets hit show him
                    if (a.getShowTime().containsKey(p)) {
                        Bukkit.getScheduler().runTask(BedWars.plugin, () -> {
                            for (Player on : a.getWorld().getPlayers()) {
                                BedWars.nms.showArmor(p, on);
                                //BedWars.nms.showPlayer(p, on);
                            }
                            a.getShowTime().remove(p);
                            p.removePotionEffect(PotionEffectType.INVISIBILITY);
                            ITeam team = a.getTeam(p);
                            p.sendMessage(getMsg(p, Messages.INTERACT_INVISIBILITY_REMOVED_DAMGE_TAKEN));
                            Bukkit.getPluginManager().callEvent(new PlayerInvisibilityPotionEvent(PlayerInvisibilityPotionEvent.Type.REMOVED, team, p, a));
                        });
                    }
                    //
                }
            }
        } else if (BedWars.nms.isDespawnable(e.getEntity())) {
            Player damager;

            if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
            else if (e.getDamager() instanceof Projectile) {
                Projectile proj = (Projectile) e.getDamager();
                damager = (Player) proj.getShooter();
            } else if (e.getDamager() instanceof TNTPrimed) {
                TNTPrimed tnt = (TNTPrimed) e.getDamager();
                if (tnt.getSource() instanceof Player) damager = (Player) tnt.getSource();
                else return;
            } else return;

            IArena a = Arena.getArenaByPlayer(damager);

            if (a == null) return;

            if (a.isPlayer(damager)) {
                // do not hurt own mobs
                if (a.getTeam(damager) == BedWars.nms.getDespawnablesList().get(e.getEntity().getUniqueId()).getTeam()) {
                    e.setCancelled(true);
                }
            } else e.setCancelled(true);
        }
    }


    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity(), killer = e.getEntity().getKiller();
        ITeam killersTeam = null;
        IArena a = Arena.getArenaByPlayer(victim);
        Player bedDestroyer = a.getTeam(victim).getBedDestroyer();

        if ((BedWars.getServerType() == ServerType.MULTIARENA && BedWars.getLobbyWorld().equals(victim.getWorld().getName())) || a != null) {
            e.setDeathMessage(null);
        }


        if (a == null) return;

        if (a.isSpectator(victim)) {
            victim.spigot().respawn();
            return;
        }

        if (a.getStatus() != GameState.playing) {
            victim.spigot().respawn();
            return;
        }

        EntityDamageEvent damageEvent = victim.getLastDamageCause();

        ITeam victimsTeam = a.getTeam(victim);
        if (a.getStatus() != GameState.playing) {
            victim.spigot().respawn();
            return;
        }

        if (victimsTeam == null) {
            victim.spigot().respawn();
            return;
        }

        BedWars.nms.clearArrowsFromPlayerBody(victim);

        // Logic for determining the cause of death
        boolean victimsTeamBedDestroyed = victimsTeam.isBedDestroyed();
        String message = victimsTeamBedDestroyed ? Messages.PLAYER_DIE_UNKNOWN_REASON_FINAL_KILL : Messages.PLAYER_DIE_UNKNOWN_REASON_REGULAR;
        PlayerKillEvent.PlayerKillCause cause = victimsTeamBedDestroyed ? PlayerKillEvent.PlayerKillCause.UNKNOWN_FINAL_KILL : PlayerKillEvent.PlayerKillCause.UNKNOWN;

        if (damageEvent != null) {
            if (damageEvent.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                LastHit lh = LastHit.getLastHit(victim);
                if (lh != null) {
                    if (lh.getTime() >= System.currentTimeMillis() - 15000) {
                        if (lh.getDamager() instanceof Player) killer = (Player) lh.getDamager();
                        if (killer != null && killer.getUniqueId().equals(victim.getUniqueId())) killer = null;
                    }
                }else if(bedDestroyer != null){
                    killer = bedDestroyer;
                    if (killer != null && killer.getUniqueId().equals(victim.getUniqueId())) killer = null;
                }
                if (killer == null) message = victimsTeamBedDestroyed ? Messages.PLAYER_DIE_EXPLOSION_WITHOUT_SOURCE_FINAL_KILL : Messages.PLAYER_DIE_EXPLOSION_WITHOUT_SOURCE_REGULAR;
                else {
                    if (killer != victim) message = victimsTeamBedDestroyed ? Messages.PLAYER_DIE_EXPLOSION_WITH_SOURCE_FINAL_KILL : Messages.PLAYER_DIE_EXPLOSION_WITH_SOURCE_REGULAR_KILL;
                    else message = victimsTeamBedDestroyed ? Messages.PLAYER_DIE_EXPLOSION_WITHOUT_SOURCE_FINAL_KILL : Messages.PLAYER_DIE_EXPLOSION_WITHOUT_SOURCE_REGULAR;
                }
                cause = victimsTeamBedDestroyed ? PlayerKillEvent.PlayerKillCause.EXPLOSION_FINAL_KILL : PlayerKillEvent.PlayerKillCause.EXPLOSION;

            } else if (damageEvent.getCause() == EntityDamageEvent.DamageCause.VOID) {
                LastHit lh = LastHit.getLastHit(victim);
                if (lh != null) {
                    if (lh.getTime() >= System.currentTimeMillis() - 15000) {
                        if (lh.getDamager() instanceof Player) killer = (Player) lh.getDamager();
                        if (killer != null && killer.getUniqueId().equals(victim.getUniqueId())) killer = null;
                    }
                }else if(bedDestroyer != null){
                    killer = bedDestroyer;
                    if (killer != null && killer.getUniqueId().equals(victim.getUniqueId())) killer = null;
                }
                if (killer == null) message = victimsTeamBedDestroyed ? Messages.PLAYER_DIE_VOID_FALL_FINAL_KILL : Messages.PLAYER_DIE_VOID_FALL_REGULAR_KILL;
                else {
                    if (killer != victim) message = victimsTeamBedDestroyed ? Messages.PLAYER_DIE_KNOCKED_IN_VOID_FINAL_KILL : Messages.PLAYER_DIE_KNOCKED_IN_VOID_REGULAR_KILL;
                    else message = victimsTeamBedDestroyed ? Messages.PLAYER_DIE_VOID_FALL_FINAL_KILL : Messages.PLAYER_DIE_VOID_FALL_REGULAR_KILL;
                }
                cause = victimsTeamBedDestroyed ? PlayerKillEvent.PlayerKillCause.VOID_FINAL_KILL : PlayerKillEvent.PlayerKillCause.VOID;
            } else if (damageEvent.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                if (killer == null) {
                    LastHit lh = LastHit.getLastHit(victim);
                    if (lh != null) {
                        if (lh.getTime() >= System.currentTimeMillis() - 15000) {
                            if (BedWars.nms.isDespawnable(lh.getDamager())) {
                                Despawnable d = BedWars.nms.getDespawnablesList().get(lh.getDamager().getUniqueId());
                                killersTeam = d.getTeam();
                                message = d.getEntity().getType() == EntityType.IRON_GOLEM ? victimsTeamBedDestroyed ? Messages.PLAYER_DIE_IRON_GOLEM_FINAL_KILL : Messages.PLAYER_DIE_IRON_GOLEM_REGULAR : victimsTeamBedDestroyed ? Messages.PLAYER_DIE_DEBUG_FINAL_KILL : Messages.PLAYER_DIE_DEBUG_REGULAR;
                                cause = victimsTeamBedDestroyed ? d.getDeathFinalCause() : d.getDeathRegularCause();
                            }
                        }
                    }
                } else {
                    message = victimsTeamBedDestroyed ? Messages.PLAYER_DIE_PVP_FINAL_KILL : Messages.PLAYER_DIE_PVP_REGULAR_KILL;
                    cause = victimsTeamBedDestroyed ? PlayerKillEvent.PlayerKillCause.PVP_FINAL_KILL : PlayerKillEvent.PlayerKillCause.PVP;
                }
            } else if (damageEvent.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
                if (killer != null) {
                    message = victimsTeamBedDestroyed ? Messages.PLAYER_DIE_SHOOT_FINAL_KILL : Messages.PLAYER_DIE_SHOOT_REGULAR;
                    cause = victimsTeamBedDestroyed ? PlayerKillEvent.PlayerKillCause.PLAYER_SHOOT_FINAL_KILL : PlayerKillEvent.PlayerKillCause.PLAYER_SHOOT;
                }
            } else if (damageEvent.getCause() == EntityDamageEvent.DamageCause.FALL) {
                LastHit lh = LastHit.getLastHit(victim);
                if (lh != null) {
                    // check if kicked off in the last 10 seconds
                    if (lh.getTime() >= System.currentTimeMillis() - 10000) {
                        if (lh.getDamager() instanceof Player) killer = (Player) lh.getDamager();
                        if (killer != null && killer.getUniqueId().equals(victim.getUniqueId())) killer = null;
                        if (killer != null) {
                            if (killer != victim) message = victimsTeamBedDestroyed ? Messages.PLAYER_DIE_KNOCKED_BY_FINAL_KILL : Messages.PLAYER_DIE_KNOCKED_BY_REGULAR_KILL;
                            else message = victimsTeamBedDestroyed ? Messages.PLAYER_DIE_VOID_FALL_FINAL_KILL : Messages.PLAYER_DIE_VOID_FALL_REGULAR_KILL;
                        }
                        cause = victimsTeamBedDestroyed ? PlayerKillEvent.PlayerKillCause.PLAYER_PUSH_FINAL : PlayerKillEvent.PlayerKillCause.PLAYER_PUSH;
                    }
                } else if(bedDestroyer != null){
                    killer = bedDestroyer;
                    if (killer != null && killer.getUniqueId().equals(victim.getUniqueId())) killer = null;

                    if (killer != null) {
                        if (killer != victim) {
                            message = victimsTeam.isBedDestroyed() ? Messages.PLAYER_DIE_KNOCKED_BY_FINAL_KILL : Messages.PLAYER_DIE_KNOCKED_BY_REGULAR_KILL;
                        } else {
                            message = victimsTeam.isBedDestroyed() ? Messages.PLAYER_DIE_VOID_FALL_FINAL_KILL : Messages.PLAYER_DIE_VOID_FALL_REGULAR_KILL;
                        }
                    }
                    cause = victimsTeam.isBedDestroyed() ? PlayerKillEvent.PlayerKillCause.PLAYER_PUSH_FINAL : PlayerKillEvent.PlayerKillCause.PLAYER_PUSH;
                }
            }
        }
        // End of death determine logic

        if (killer != null) killersTeam = a.getTeam(killer);

        String finalMessage = message;
        PlayerKillEvent playerKillEvent = new PlayerKillEvent(a, victim, killer, player -> Language.getMsg(player, finalMessage), cause);
        Bukkit.getPluginManager().callEvent(playerKillEvent);

        if (killer != null && playerKillEvent.playSound()) Sounds.playSound(ConfigPath.SOUNDS_KILL, killer);

        for (Player on : a.getPlayers()) {
            Language lang = Language.getPlayerLanguage(on);
            on.sendMessage(playerKillEvent.getMessage().apply(on).
                    replace("%bw_player_color%", victimsTeam.getColor().chat().toString())
                    .replace("%bw_player%", victim.getDisplayName())
                    .replace("%bw_playername%", victim.getName())
                    .replace("%bw_team%", victimsTeam.getDisplayName(lang))
                    .replace("%bw_killer_color%", killersTeam == null ? "" : killersTeam.getColor().chat().toString())
                    .replace("%bw_killer_playername%", killer == null ? "" : killer.getName())
                    .replace("%bw_killer_name%", killer == null ? "" : killer.getDisplayName())
                    .replace("%bw_killer_team_name%", killersTeam == null ? "" : killersTeam.getDisplayName(lang)));
        }

        for (Player on : a.getSpectators()) {
            Language lang = Language.getPlayerLanguage(on);
            on.sendMessage(playerKillEvent.getMessage().apply(on).
                    replace("%bw_player_color%", victimsTeam.getColor().chat().toString())
                    .replace("%bw_player%", victim.getDisplayName())
                    .replace("%bw_playername%", victim.getName())
                    .replace("%bw_team%", victimsTeam.getDisplayName(lang))
                    .replace("%bw_killer_color%", killersTeam == null ? "" : killersTeam.getColor().chat().toString())
                    .replace("%bw_killer_playername%", killer == null ? "" : killer.getName())
                    .replace("%bw_killer_name%", killer == null ? "" : killer.getDisplayName())
                    .replace("%bw_killer_team_name%", killersTeam == null ? "" : killersTeam.getDisplayName(lang)));
        }

        // increase stats to killer
        if ((killer != null && !victimsTeam.equals(killersTeam)) && !victim.equals(killer)) {
            a.addPlayerKill(killer, cause.isFinalKill(), victim);
        }

        // handle drops
        if (PlayerDrops.handlePlayerDrops(a, victim, killer, victimsTeam, killersTeam, cause, e.getDrops())) e.getDrops().clear();
        else {
            for (ItemStack inventoryItem : e.getDrops()) {
                e.getEntity().getLocation().getWorld().dropItemNaturally(e.getEntity().getLocation(), inventoryItem);
            }
            e.getDrops().clear();
        }

        // send respawn packet
        // Needs a delay to prevent hit delay but after respawning (mainly casued by projectile hits)
        Bukkit.getScheduler().runTask(BedWars.plugin, () -> victim.spigot().respawn());
        a.addPlayerDeath(victim);

        // reset last damager
        LastHit lastHit = LastHit.getLastHit(victim);
        if (lastHit != null) lastHit.setDamager(null);

        if (victimsTeam.isBedDestroyed() && victimsTeam.getSize() == 1 && a.getConfig().getBoolean(ConfigPath.ARENA_DISABLE_GENERATOR_FOR_EMPTY_TEAMS)) {
            for (IGenerator g : victimsTeam.getGenerators()) g.disable();
            victimsTeam.getGenerators().clear();
        }
    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        IArena a = Arena.getArenaByPlayer(player);

        if (a == null) {
            SetupSession ss = SetupSession.getSession(player.getUniqueId());
            if (ss != null) e.setRespawnLocation(player.getWorld().getSpawnLocation());
            return;
        }

        if (a.isSpectator(player)) {
            e.setRespawnLocation(a.getSpectatorLocation());
            for (IGenerator o : a.getOreGenerators()) {
                GeneratorHolder holder = o.getHologramHolder();
                o.updateHolograms(player);
                if (holder != null) holder.update();
            }
            for (ITeam t : a.getTeams()) {
                for (IGenerator o : t.getGenerators()) {
                    GeneratorHolder holder = o.getHologramHolder();
                    o.updateHolograms(player);
                    if (holder != null) holder.update();
                }
            }
            for (ShopHolo sh : ShopHolo.getShopHolograms(player)) {
                sh.update();
            }

            a.sendSpectatorCommandItems(player);
            return;
        }
        ITeam t = a.getTeam(player);
        if (t == null) {
            e.setRespawnLocation(a.getReSpawnLocation());
            BedWars.plugin.getLogger().severe(e.getPlayer().getName() + " re-spawn error on " + a.getArenaName() + "[" + a.getWorldName() + "] because the team was NULL and he was not spectating!");
            BedWars.plugin.getLogger().severe("This is caused by one of your plugins: remove or configure any re-spawn related plugins.");
            a.removePlayer(player, false);
            a.removeSpectator(player, false);
            return;
        }
        if (t.isBedDestroyed()) {
            e.setRespawnLocation(a.getSpectatorLocation());
            a.addSpectator(player, true, null);
            t.getMembers().remove(player);
            player.sendMessage(getMsg(player, Messages.PLAYER_DIE_ELIMINATED_CHAT));
            if (t.getMembers().isEmpty()) {
                Bukkit.getPluginManager().callEvent(new TeamEliminatedEvent(a, t));
                for (Player p : a.getWorld().getPlayers()) {
                    p.sendMessage(getMsg(p, Messages.TEAM_ELIMINATED_CHAT).replace("%bw_team_color%", t.getColor().chat().toString()).replace("%bw_team_name%", t.getDisplayName(Language.getPlayerLanguage(p))));
                }
                Bukkit.getScheduler().runTaskLater(BedWars.plugin, a::checkWinner, 40L);
            }
        } else {
            //respawn session
            int respawnTime = BedWars.config.getInt(ConfigPath.GENERAL_CONFIGURATION_RE_SPAWN_COUNTDOWN);
            if (respawnTime > 1) {
                e.setRespawnLocation(a.getReSpawnLocation());
                a.startReSpawnSession(player, respawnTime);
            } else {
                // instant respawn configuration
                e.setRespawnLocation(t.getSpawn());
                t.respawnMember(player);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();

        if (Arena.isInArena(player)) {
            IArena a = Arena.getArenaByPlayer(player);
            if (e.getFrom().getChunk() != e.getTo().getChunk()) {
                /* update armor-stands hidden by nms */
                for (IGenerator o : a.getOreGenerators()) {
                    if (o.getType() == GeneratorType.DIAMOND || o.getType() == GeneratorType.EMERALD) {
                        if (!a.getWorld().getPlayers().contains(player)) return; // prevent location check between different worlds
                        IGenHolo h = o.getPlayerHolograms().get(player);
                        if (h != null) {
                            if (o.getLocation().distance(e.getTo()) > BedWars.hologramUpdateDistance) h.update();
                        }

                        GeneratorHolder holder = o.getHologramHolder();
                        if (holder != null) {
                            if (holder.getArmorStand().getLocation().distance(e.getTo()) > BedWars.hologramUpdateDistance) holder.update();
                        }
                    }
                }

                for (ITeam t : a.getTeams()) {
                    for (IGenerator o : t.getGenerators()) {
                        IGenHolo h = o.getPlayerHolograms().get(player);
                        if (h != null) {
                            if (o.getLocation().distance(e.getTo()) > BedWars.hologramUpdateDistance) h.update();
                        }

                        GeneratorHolder holder = o.getHologramHolder();
                        if (holder != null) {
                            if (holder.getArmorStand().getLocation().distance(e.getTo()) > BedWars.hologramUpdateDistance) holder.update();
                        }
                    }
                }

                for (ShopHolo sh : ShopHolo.getShopHolograms(player)) {
                    if (sh.getHologram().getLocation().distance(e.getTo()) > BedWars.hologramUpdateDistance) sh.update();
                }

                // hide armor for those with invisibility potions
                if (!a.getShowTime().isEmpty()) {
                    // generic hide packets
                    for (Map.Entry<Player, Integer> entry : a.getShowTime().entrySet()) {
                        if (entry.getValue() > 1) {
                            if (!a.getTeam(entry.getKey()).equals(a.getTeam(player))) {
                                BedWars.nms.hideArmor(entry.getKey(), player);
                            }
                        }
                    }
                    // if the moving player has invisible armor
                    if (a.getShowTime().containsKey(player)) {
                        for (Player p : a.getPlayers()) {
                            if (a.getTeam(player).equals(a.getTeam(p))) continue;
                            BedWars.nms.hideArmor(player, p);
                        }
                    }
                    /* hide players from spectators */
                    if (a.getShowTime().containsKey(player)) {
                        for (Player p : a.getSpectators()) {
                            BedWars.nms.hideArmor(player, p);
                        }
                    }
                }
            }

            if (a.isSpectator(player) || a.isReSpawning(player)) {
                if (e.getTo().getY() < 0) {
                    PaperSupport.teleportC(player, a.isSpectator(player) ? a.getSpectatorLocation() : a.getReSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                    player.setAllowFlight(true);
                    player.setFlying(true);
                    // how to remove fall velocity?
                }
            } else {
                if (a.getStatus() == GameState.playing) {
                    if (player.getLocation().getBlockY() <= a.getYKillHeight()) {
                        player.getInventory().clear(); //Fix issue #149
                        BedWars.nms.voidKill(player);
                    }
                    for (ITeam team : a.getTeams()) {
                        if (!(team instanceof BedWarsTeam)) continue;
                        IBedHolo bedHolo = ((BedWarsTeam) team).getBedHologram(player);
                        if (!player.getLocation().getWorld().equals(team.getBed().getWorld())) continue;
                        if (player.getLocation().distance(team.getBed()) < 4) {
                            if (team.isMember(player)) {
                                if (bedHolo == null) continue;
                                if (!bedHolo.getHologram().isShowing()) bedHolo.hide();
                            }
                        } else {
                            if (team.isMember(player)) {
                                if (bedHolo == null) continue;
                                if (!bedHolo.getHologram().isShowing()) bedHolo.show();
                            }
                        }
                    }
                    if (e.getFrom() != e.getTo()) {
                        Arena.afkCheck.remove(player.getUniqueId());
                        BedWars.getAPI().getAFKUtil().setPlayerAFK(player, false);
                    }
                } else {
                    if (player.getLocation().getBlockY() <= 0) {
                        ITeam bwt = a.getTeam(player);
                        if (bwt != null) {
                            PaperSupport.teleportC(player, bwt.getSpawn(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                        } else {
                           PaperSupport.teleportC(player, a.getSpectatorLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                        }
                    }
                }
            }
        } else {
            if (BedWars.config.getBoolean(ConfigPath.LOBBY_VOID_TELEPORT_ENABLED) && player.getWorld().getName().equalsIgnoreCase(BedWars.config.getLobbyWorldName()) && BedWars.getServerType() == ServerType.MULTIARENA) {
                if (e.getTo().getY() < BedWars.config.getInt(ConfigPath.LOBBY_VOID_TELEPORT_HEIGHT)) {
                    PaperSupport.teleportC(player, BedWars.config.getConfigLoc("lobbyLoc"), PlayerTeleportEvent.TeleportCause.PLUGIN);
                }
            }
        }
    }

    @EventHandler
    public void onProjHit(ProjectileHitEvent e) {
        Projectile proj = e.getEntity();
        if (proj == null) return;

        if (proj.getShooter() instanceof Player) {
            IArena a = Arena.getArenaByPlayer((Player) proj.getShooter());
            if (a != null) {
                if (!a.isPlayer((Player) proj.getShooter())) return;
                String utility = "";
                if (proj instanceof Snowball) utility = "silverfish";
                if (!utility.isEmpty()) {
                    spawnUtility(utility, proj.getLocation(), a.getTeam((Player) proj.getShooter()), (Player) proj.getShooter());
                }
            }
        }
    }

    @EventHandler
    public void onItemFrameDamage(EntityDamageByEntityEvent e) {
        Entity entity = e.getEntity();
        if (entity.getType() != EntityType.ITEM_FRAME) return;

        IArena a = Arena.getArenaByIdentifier(entity.getWorld().getName());
        if (a != null || (BedWars.getServerType() == ServerType.MULTIARENA && BedWars.getLobbyWorld().equals(entity.getWorld().getName()))) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if (Arena.getArenaByIdentifier(e.getEntity().getLocation().getWorld().getName()) != null) {
            if (e.getEntityType() == EntityType.IRON_GOLEM || e.getEntityType() == EntityType.SILVERFISH) {
                e.getDrops().clear();
                e.setDroppedExp(0);
            }
        }

        // clean if necessary
        BedWars.nms.getDespawnablesList().remove(e.getEntity().getUniqueId());
    }

    @EventHandler
    public void onEat(PlayerItemConsumeEvent e) {
        if (e.getItem().getType() == BedWars.nms.materialCake()) {
            if (Arena.getArenaByIdentifier(e.getPlayer().getWorld().getName()) != null) {
                e.setCancelled(true);
            }
        }
    }

    @SuppressWarnings("unused")
    private static void spawnUtility(String s, Location loc, ITeam t, Player p) {
        if ("silverfish".equalsIgnoreCase(s)) {
            BedWars.nms.spawnSilverfish(loc, t, BedWars.shop.getYml().getDouble(ConfigPath.SHOP_SPECIAL_SILVERFISH_SPEED), BedWars.shop.getYml().getDouble(ConfigPath.SHOP_SPECIAL_SILVERFISH_HEALTH),
                    BedWars.shop.getInt(ConfigPath.SHOP_SPECIAL_SILVERFISH_DESPAWN),
                    BedWars.shop.getYml().getDouble(ConfigPath.SHOP_SPECIAL_SILVERFISH_DAMAGE));
        }
    }
}
