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

package com.tomkeuper.bedwars.arena;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.arena.NextEvent;
import com.tomkeuper.bedwars.api.arena.generator.GeneratorType;
import com.tomkeuper.bedwars.api.arena.generator.IGenerator;
import com.tomkeuper.bedwars.api.arena.shop.ShopHolo;
import com.tomkeuper.bedwars.api.arena.team.ITeam;
import com.tomkeuper.bedwars.api.arena.team.ITeamAssigner;
import com.tomkeuper.bedwars.api.arena.team.TeamColor;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.api.entity.Despawnable;
import com.tomkeuper.bedwars.api.events.gameplay.GameEndEvent;
import com.tomkeuper.bedwars.api.events.gameplay.GameStateChangeEvent;
import com.tomkeuper.bedwars.api.events.gameplay.NextEventChangeEvent;
import com.tomkeuper.bedwars.api.events.player.PlayerJoinArenaEvent;
import com.tomkeuper.bedwars.api.events.player.PlayerKillEvent;
import com.tomkeuper.bedwars.api.events.player.PlayerLeaveArenaEvent;
import com.tomkeuper.bedwars.api.events.player.PlayerReJoinEvent;
import com.tomkeuper.bedwars.api.events.server.ArenaDisableEvent;
import com.tomkeuper.bedwars.api.events.server.ArenaEnableEvent;
import com.tomkeuper.bedwars.api.events.server.ArenaRestartEvent;
import com.tomkeuper.bedwars.api.events.server.ArenaSpectateEvent;
import com.tomkeuper.bedwars.api.items.handlers.IPermanentItem;
import com.tomkeuper.bedwars.api.language.Language;
import com.tomkeuper.bedwars.api.language.Messages;
import com.tomkeuper.bedwars.api.region.Region;
import com.tomkeuper.bedwars.api.server.ServerType;
import com.tomkeuper.bedwars.api.tasks.AnnouncementTask;
import com.tomkeuper.bedwars.api.tasks.PlayingTask;
import com.tomkeuper.bedwars.api.tasks.RestartingTask;
import com.tomkeuper.bedwars.api.tasks.StartingTask;
import com.tomkeuper.bedwars.arena.tasks.*;
import com.tomkeuper.bedwars.arena.team.BedWarsTeam;
import com.tomkeuper.bedwars.arena.team.TeamAssigner;
import com.tomkeuper.bedwars.configuration.ArenaConfig;
import com.tomkeuper.bedwars.configuration.Sounds;
import com.tomkeuper.bedwars.levels.internal.InternalLevel;
import com.tomkeuper.bedwars.levels.internal.PerMinuteTask;
import com.tomkeuper.bedwars.listeners.blockstatus.BlockStatusListener;
import com.tomkeuper.bedwars.listeners.dropshandler.PlayerDrops;
import com.tomkeuper.bedwars.money.internal.MoneyPerMinuteTask;
import com.tomkeuper.bedwars.shop.ShopCache;
import com.tomkeuper.bedwars.sidebar.BoardManager;
import com.tomkeuper.bedwars.support.citizens.JoinNPC;
import com.tomkeuper.bedwars.support.paper.PaperSupport;
import com.tomkeuper.bedwars.support.papi.SupportPAPI;
import com.tomkeuper.bedwars.support.vault.WithEconomy;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.bossbar.BossBar;
import me.neznamy.tab.api.placeholder.PlayerPlaceholder;
import me.neznamy.tab.api.placeholder.ServerPlaceholder;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static com.tomkeuper.bedwars.BedWars.*;
import static com.tomkeuper.bedwars.api.language.Language.*;
import static com.tomkeuper.bedwars.arena.upgrades.BaseListener.isOnABase;

@SuppressWarnings("WeakerAccess")
public class Arena implements IArena {

    private static final HashMap<String, IArena> arenaByName = new HashMap<>();
    private static final HashMap<Player, IArena> arenaByPlayer = new HashMap<>();
    private static final HashMap<String, IArena> arenaByIdentifier = new HashMap<>();
    private static final LinkedList<IArena> arenas = new LinkedList<>();
    private static int gamesBeforeRestart = config.getInt(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_GAMES_BEFORE_RESTART);
    public static HashMap<UUID, Integer> afkCheck = new HashMap<>();
    public static HashMap<UUID, Integer> magicMilk = new HashMap<>();


    private List<Player> players = new ArrayList<>();
    private List<Player> spectators = new ArrayList<>();
    private List<Block> signs = new ArrayList<>();
    private GameState status = GameState.restarting;
    private YamlConfiguration yml;
    private ArenaConfig cm;
    private int minPlayers = 2, maxPlayers = 10, maxInTeam = 1, islandRadius = 10;
    public int upgradeDiamondsCount = 0, upgradeEmeraldsCount = 0;
    public boolean allowSpectate = true, allowMapBreak = false, enderDragonDestory = false;
    private World world;
    private String group = "Default", arenaName, worldName;
    private List<ITeam> teams = new ArrayList<>();
    private LinkedList<org.bukkit.util.Vector> placed = new LinkedList<>();
    private List<String> nextEvents = new ArrayList<>();
    private List<String> shopOverrideCategories = new ArrayList<>();
    private List<Region> regionsList = new ArrayList<>();
    private List<ServerPlaceholder> serverPlaceholders = new ArrayList<>();
    private List<BossBar> dragonBossbars = new ArrayList<>();
    private int renderDistance, magicMilkTime = 30;

    private final List<Player> leaving = new ArrayList<>();

    /**
     * Current event, used at scoreboard
     */
    private NextEvent nextEvent = NextEvent.DIAMOND_GENERATOR_TIER_II;
    private int diamondTier = 1, emeraldTier = 1;

    /**
     * Players in respawn session
     */
    private ConcurrentHashMap<Player, Integer> respawnSessions = new ConcurrentHashMap<>();

    /**
     * Invisibility for armor when you drink an invisibility potion
     */
    private ConcurrentHashMap<Player, Integer> showTime = new ConcurrentHashMap<>();

    /**
     * Player location before joining.
     * The player is teleported to this location if the server is running in SHARED mode.
     */
    private static final HashMap<Player, Location> playerLocation = new HashMap<>();

    /**
     * temp stats. some of them use player name as key to keep names of players who left. at checkWinners for example.
     * Those maps are not used for db stats but is for internal use only.
     */
    private HashMap<String, Integer> playerKills = new HashMap<>();
    private HashMap<Player, Integer> playerBedsDestroyed = new HashMap<>();
    private HashMap<Player, Integer> playerFinalKills = new HashMap<>();
    private HashMap<Player, Integer> playerDeaths = new HashMap<>();
    private HashMap<Player, Integer> playerFinalKillDeaths = new HashMap<>();


    /* ARENA TASKS */
    private StartingTask startingTask = null;
    private PlayingTask playingTask = null;
    private RestartingTask restartingTask = null;

    private AnnouncementTask announcementTask;

    /* ARENA GENERATORS */
    private List<IGenerator> oreGenerators = new ArrayList<>();

    private PerMinuteTask perMinuteTask;

    private MoneyPerMinuteTask moneyperMinuteTask;

    private static final LinkedList<IArena> enableQueue = new LinkedList<>();

    private Location respawnLocation, spectatorLocation, waitingLocation;
    private int yKillHeight;
    private Instant startTime;
    private ITeamAssigner teamAssigner = new TeamAssigner();

    /**
     * Load an arena.
     * This will check if it was set up right.
     *
     * @param name - world name
     * @param p    - This will send messages to the player if something went wrong while loading the arena. Can be NULL.
     */
    public Arena(String name, @Nullable CommandSender p) {
        if (!autoscale) {
            for (IArena mm : enableQueue) {
                if (mm.getArenaName().equalsIgnoreCase(name)) {
                    plugin.getLogger().severe("Tried to load arena " + name + " but it is already in the enable queue.");
                    if (p != null)
                        p.sendMessage(ChatColor.RED + "Tried to load arena " + name + " but it is already in the enable queue.");
                    return;
                }
            }
            if (getArenaByName(name) != null) {
                plugin.getLogger().severe("Tried to load arena " + name + " but it is already enabled.");
                if (p != null)
                    p.sendMessage(ChatColor.RED + "Tried to load arena " + name + " but it is already enabled.");
                return;
            }
        }
        this.arenaName = name;
        if (autoscale) {
            this.worldName = BedWars.arenaManager.generateGameID();
        } else {
            this.worldName = arenaName;
        }

        cm = new ArenaConfig(BedWars.plugin, name, plugin.getDataFolder().getPath() + "/Arenas");

        yml = cm.getYml();
        if (yml.get("Team") == null) {
            if (p != null) p.sendMessage("You didn't set any team for arena: " + name);
            plugin.getLogger().severe("You didn't set any team for arena: " + name);
            return;
        }
        if (yml.getConfigurationSection("Team").getKeys(false).size() < 2) {
            if (p != null) p.sendMessage("§cYou must set at least 2 teams on: " + name);
            plugin.getLogger().severe("You must set at least 2 teams on: " + name);
            return;
        }
        maxInTeam = yml.getInt("maxInTeam");
        maxPlayers = yml.getConfigurationSection("Team").getKeys(false).size() * maxInTeam;
        minPlayers = yml.getInt("minPlayers");
        allowSpectate = yml.getBoolean("allowSpectate");
        enderDragonDestory = yml.getBoolean(ConfigPath.ARENA_ALLOW_DRAGON_DESTROY_WHEN_PROTECTED);
        allowMapBreak = yml.getBoolean(ConfigPath.ARENA_ALLOW_MAP_BREAK);
        magicMilkTime = yml.getInt(ConfigPath.ARENA_MAGIC_MILK_TIME);
        islandRadius = yml.getInt(ConfigPath.ARENA_ISLAND_RADIUS);
        if (config.getYml().get("arenaGroups") != null) {
            if (config.getYml().getStringList("arenaGroups").contains(yml.getString("group"))) {
                group = yml.getString("group");
            }
        }


        if (!BedWars.getAPI().getRestoreAdapter().isWorld(name)) {
            if (p != null) p.sendMessage(ChatColor.RED + "There isn't any map called " + name);
            plugin.getLogger().log(Level.WARNING, "There isn't any map called " + name);
            return;
        }

        boolean error = false;
        for (String team : yml.getConfigurationSection("Team").getKeys(false)) {
            String colorS = yml.getString("Team." + team + ".Color");
            if (colorS == null) continue;
            colorS = colorS.toUpperCase();
            try {
                TeamColor.valueOf(colorS);
            } catch (Exception e) {
                if (p != null) p.sendMessage("§cInvalid color at team: " + team + " in arena: " + name);
                plugin.getLogger().severe("Invalid color at team: " + team + " in arena: " + name);
                error = true;
            }
            for (String stuff : Arrays.asList("Color", "Spawn", "Bed", "Shop", "Upgrade", "Iron", "Gold")) {
                if (yml.get("Team." + team + "." + stuff) == null) {
                    if (p != null) p.sendMessage("§c" + stuff + " not set for " + team + " team on: " + name);
                    plugin.getLogger().severe(stuff + " not set for " + team + " team on: " + name);
                    error = true;
                }
            }
        }
        if (yml.get("generator.Diamond") == null) {
            if (p != null) p.sendMessage("§cThere isn't set any Diamond generator on: " + name);
            plugin.getLogger().severe("There isn't set any Diamond generator on: " + name);
        }
        if (yml.get("generator.Emerald") == null) {
            if (p != null) p.sendMessage("§cThere isn't set any Emerald generator on: " + name);
            plugin.getLogger().severe("There isn't set any Emerald generator on: " + name);
        }
        if (yml.get("waiting.Loc") == null) {
            if (p != null) p.sendMessage("§cWaiting spawn not set on: " + name);
            plugin.getLogger().severe("Waiting spawn not set on: " + name);
            return;
        }
        if (error) return;
        yKillHeight = yml.getInt(ConfigPath.ARENA_Y_LEVEL_KILL);
        addToEnableQueue(this);
        Language.saveIfNotExists(Messages.ARENA_DISPLAY_GROUP_PATH + getGroup().toLowerCase(), String.valueOf(getGroup().charAt(0)).toUpperCase() + group.substring(1).toLowerCase());
    }

    /**
     * Use this method when the world was loaded successfully.
     */
    @Override
    public void init(World world) {
        if (!autoscale) {
            if (getArenaByName(arenaName) != null) return;
        }
        removeFromEnableQueue(this);
        debug("Initialized arena " + getArenaName() + " with map " + world.getName());
        this.world = world;
        this.worldName = world.getName();
        getConfig().setName(worldName);
        world.getEntities().stream().filter(e -> e.getType() != EntityType.PLAYER)
                .filter(e -> e.getType() != EntityType.PAINTING).filter(e -> e.getType() != EntityType.ITEM_FRAME)
                .forEach(Entity::remove);
        for (String s : getConfig().getList(ConfigPath.ARENA_GAME_RULES)) {
            String[] rule = s.split(":");
            if (rule.length == 2) world.setGameRuleValue(rule[0], rule[1]);
        }
        world.setAutoSave(false);

        /* Clear setup armor-stands */
        for (Entity e : world.getEntities()) {
            if (e.getType() == EntityType.ARMOR_STAND) {
                if (!((ArmorStand) e).isVisible()) e.remove();
            }
        }

        //Create teams
        for (String team : yml.getConfigurationSection("Team").getKeys(false)) {
            if (getTeam(team) != null) {
                BedWars.plugin.getLogger().severe("A team with name: " + team + " was already loaded for arena: " + getArenaName());
                continue;
            }
            BedWarsTeam bwt = new BedWarsTeam(team, TeamColor.valueOf(yml.getString("Team." + team + ".Color").toUpperCase()), cm.getArenaLoc("Team." + team + ".Spawn"),
                    cm.getArenaLoc("Team." + team + ".Bed"), cm.getArenaLoc("Team." + team + ".Shop"), cm.getArenaLoc("Team." + team + ".Upgrade"), this);
            teams.add(bwt);
            bwt.spawnGenerators();
        }

        //Load diamond/ emerald generators
        Location location;
        for (String type : Arrays.asList("Diamond", "Emerald")) {
            if (yml.get("generator." + type) != null) {
                for (String s : yml.getStringList("generator." + type)) {
                    location = cm.convertStringToArenaLocation(s);
                    if (location == null) {
                        plugin.getLogger().severe("Invalid location for " + type + " generator: " + s);
                        continue;
                    }
                    oreGenerators.add(new OreGenerator(location, this, GeneratorType.valueOf(type.toUpperCase()), null, true));
                }
            }
        }

        arenas.add(this);
        arenaByName.put(getArenaName(), this);
        arenaByIdentifier.put(worldName, this);
        world.getWorldBorder().setCenter(cm.getArenaLoc("waiting.Loc"));
        world.getWorldBorder().setSize(yml.getInt("worldBorder"));

        /* Check if lobby removal is set */
        if (!getConfig().getYml().isSet(ConfigPath.ARENA_WAITING_POS1) && getConfig().getYml().isSet(ConfigPath.ARENA_WAITING_POS2)) {
            plugin.getLogger().severe("Lobby Pos1 isn't set! The arena's lobby won't be removed!");
        }
        if (getConfig().getYml().isSet(ConfigPath.ARENA_WAITING_POS1) && !getConfig().getYml().isSet(ConfigPath.ARENA_WAITING_POS2)) {
            plugin.getLogger().severe("Lobby Pos2 isn't set! The arena's lobby won't be removed!");
        }

        /* Register arena signs */
        registerSigns();
        //Call event
        Bukkit.getPluginManager().callEvent(new ArenaEnableEvent(this));

        // Re Spawn Session Location
        respawnLocation = cm.getArenaLoc(ConfigPath.ARENA_SPEC_LOC);
        if (respawnLocation == null) {
            respawnLocation = cm.getArenaLoc("waiting.Loc");
        }
        if (respawnLocation == null) {
            respawnLocation = world.getSpawnLocation();
        }
        //

        // Spectator location
        spectatorLocation = cm.getArenaLoc(ConfigPath.ARENA_SPEC_LOC);
        if (spectatorLocation == null) {
            spectatorLocation = cm.getArenaLoc("waiting.Loc");
        }
        if (spectatorLocation == null) {
            spectatorLocation = world.getSpawnLocation();
        }
        //

        // Waiting location
        waitingLocation = cm.getArenaLoc("waiting.Loc");
        if (waitingLocation == null) {
            waitingLocation = world.getSpawnLocation();
        }
        //

        changeStatus(GameState.waiting);

        //
        for (NextEvent ne : NextEvent.values()) {
            nextEvents.add(ne.toString());
        }

        upgradeDiamondsCount = getGeneratorsCfg().getInt(getGeneratorsCfg().getYml().get(getGroup() + "." + ConfigPath.GENERATOR_DIAMOND_TIER_II_START) == null ?
                "Default." + ConfigPath.GENERATOR_DIAMOND_TIER_II_START : getGroup() + "." + ConfigPath.GENERATOR_DIAMOND_TIER_II_START);
        upgradeEmeraldsCount = getGeneratorsCfg().getInt(getGeneratorsCfg().getYml().get(getGroup() + "." + ConfigPath.GENERATOR_EMERALD_TIER_II_START) == null ?
                "Default." + ConfigPath.GENERATOR_EMERALD_TIER_II_START : getGroup() + "." + ConfigPath.GENERATOR_EMERALD_TIER_II_START);
        plugin.getLogger().info("Load done: " + getArenaName());


        // entity tracking range - player
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File("spigot.yml"));
        renderDistance = yaml.get("world-settings." + getWorldName() + ".entity-tracking-range.players") == null ?
                yaml.getInt("world-settings.default.entity-tracking-range.players") : yaml.getInt("world-settings." + getWorldName() + ".entity-tracking-range.players");

        //register scoreboards
        BoardManager.getInstance().registerArenaScoreboards(this);
    }

    /**
     * Add a player to the arena
     *
     * @param p              - Player to add.
     * @param skipOwnerCheck - True if you want to skip the party checking for this player. This
     * @return true if was added.
     */
    public boolean addPlayer(Player p, boolean skipOwnerCheck) {
        if (p == null) return false;
        debug("Player added: " + p.getName() + " arena: " + getArenaName());

//        Used to check if a sidebar must be given or not
        boolean isStatusChange = false;

        /* used for base enter/leave event */
        isOnABase.remove(p);
        //
        if (getArenaByPlayer(p) != null) {
            return false;
        }
        if (getPartyManager().hasParty(p)) {
            if (!skipOwnerCheck) {
                if (!getPartyManager().isOwner(p)) {
                    p.sendMessage(getMsg(p, Messages.COMMAND_JOIN_DENIED_NOT_PARTY_LEADER));
                    return false;
                }
                int partySize = (int) getPartyManager().getMembers(p).stream().filter(member -> {
                    IArena arena = Arena.getArenaByPlayer(member);
                    if (arena == null) {
                        return true;
                    }
                    return arena.isSpectator(member);
                }).count();

                if (partySize > maxInTeam * getTeams().size() - getPlayers().size()) {
                    p.sendMessage(getMsg(p, Messages.COMMAND_JOIN_DENIED_PARTY_TOO_BIG));
                    return false;
                }
                for (Player mem : getPartyManager().getMembers(p)) {
                    if (mem == p) continue;
                    IArena a = Arena.getArenaByPlayer(mem);
                    if (a != null) {
                        /*if (a.isPlayer(mem)) {
                            a.removePlayer(mem, false);
                        } else */
                        if (a.isSpectator(mem)) {
                            a.removeSpectator(mem, false);
                        }
                    }
                    addPlayer(mem, true);
                }
            }
        }

        leaving.remove(p);

        if (status == GameState.waiting || (status == GameState.starting && (startingTask != null && startingTask.getCountdown() > 1))) {
            if (players.size() >= maxPlayers && !isVip(p)) {
                TextComponent text = new TextComponent(getMsg(p, Messages.COMMAND_JOIN_DENIED_IS_FULL));
                text.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, config.getYml().getString("storeLink")));
                p.spigot().sendMessage(text);
                return false;
            } else if (players.size() >= maxPlayers && isVip(p)) {
                boolean canJoin = false;
                for (Player on : new ArrayList<>(players)) {
                    if (!isVip(on)) {
                        canJoin = true;
                        removePlayer(on, false);
                        TextComponent vipKick = new TextComponent(getMsg(p, Messages.ARENA_JOIN_VIP_KICK));
                        vipKick.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, config.getYml().getString("storeLink")));
                        p.spigot().sendMessage(vipKick);
                        break;
                    }
                }
                if (!canJoin) {
                    p.sendMessage(getMsg(p, Messages.COMMAND_JOIN_DENIED_IS_FULL_OF_VIPS));
                    return false;
                }
            }

            PlayerJoinArenaEvent ev = new PlayerJoinArenaEvent(this, p, false);
            Bukkit.getPluginManager().callEvent(ev);
            if (ev.isCancelled()) return false;

            //Remove from ReJoin
            ReJoin rejoin = ReJoin.getPlayer(p);
            if (rejoin != null) {
                rejoin.destroy(true);
            }

            p.closeInventory();
            players.add(p);
            p.setFlying(false);
            p.setAllowFlight(false);
            p.setHealth(p.getMaxHealth());
            for (Player on : players) {
                Language language = Language.getPlayerLanguage(on);
                if (ev.getMessage().equals("")) {
                    on.sendMessage(getMsg(language, p, Messages.COMMAND_JOIN_PLAYER_JOIN_MSG)
                            .replace("%bw_v_prefix%", getChatSupport().getPrefix(p))
                            .replace("%bw_v_suffix%", getChatSupport().getSuffix(p))
                            .replace("%bw_playername%", p.getName())
                            .replace("%bw_player%", p.getDisplayName())
                            .replace("%bw_on%", String.valueOf(getPlayers().size()))
                            .replace("%bw_max%", String.valueOf(getMaxPlayers()))
                    );
                } else {
                    if (ev.getMessage() != null) on.sendMessage(ev.getMessage());
                }
            }
            setArenaByPlayer(p, this);

            /* check if you can start the arena */
            if (status == GameState.waiting) {
                int teams = 0, teammates = 0, partyMembers = 0;

                for (Player on : getPlayers()) {
                    if (getPartyManager().isOwner(on)) {
                        teams++;
                    }
                    if (getPartyManager().hasParty(on)) {
                        teammates++;
                        partyMembers += getPartyManager().getMembers(on).size();
                    }
                }

                // Check if the party fills the arena
                if (partyMembers >= maxPlayers) {
                    Bukkit.getScheduler().runTaskLater(BedWars.plugin, () -> changeStatus(GameState.starting), 10L);
                    isStatusChange = true;
                } else if (minPlayers <= players.size() && teams > 0 && players.size() != teammates / teams) {
                    Bukkit.getScheduler().runTaskLater(BedWars.plugin, () -> changeStatus(GameState.starting), 10L);
                    isStatusChange = true;
                } else if (players.size() >= minPlayers && teams == 0) {
                    Bukkit.getScheduler().runTaskLater(BedWars.plugin, () -> changeStatus(GameState.starting), 10L);
                    isStatusChange = true;
                }
            }

            //half full arena time shorten
            if (players.size() >= getMaxPlayers() / 2 && players.size() > minPlayers) {
                if (startingTask != null) {
                    if (Bukkit.getScheduler().isCurrentlyRunning(startingTask.getTask())) {
                        if (startingTask.getCountdown() > getConfig().getInt(ConfigPath.GENERAL_CONFIGURATION_START_COUNTDOWN_HALF)) {
                            startingTask.setCountdown(BedWars.config.getInt(ConfigPath.GENERAL_CONFIGURATION_START_COUNTDOWN_HALF));
                        }
                    }
                }
            }

            /* save player inventory etc */
            if (getServerType() != ServerType.BUNGEE) {
                new PlayerGoods(p, true);
                playerLocation.put(p, p.getLocation());
            }
            PaperSupport.teleportC(p, getWaitingLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);

            sendPreGameCommandItems(p);
            for (PotionEffect pf : p.getActivePotionEffects()) {
                p.removePotionEffect(pf.getType());
            }
        } else if (status == GameState.playing || status == GameState.starting && (startingTask != null && startingTask.getCountdown() <= 1)) {
            addSpectator(p, false, null);
            /* stop code if status playing*/
            return false;
        }

        p.getInventory().setArmorContents(null);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // bungee mode invisibility issues
            if (getServerType() == ServerType.BUNGEE) {
                // fix invisibility issue
                //if (BedWars.nms.getVersion() == 7) {
                BedWars.nms.sendPlayerSpawnPackets(p, this);
                //}
            }
            for (Player on : Bukkit.getOnlinePlayers()) {
                if (on == null) continue;
                if (on.equals(p)) continue;
                if (isPlayer(on)) {
                    BedWars.nms.spigotShowPlayer(p, on);
                    BedWars.nms.spigotShowPlayer(on, p);
                } else {
                    BedWars.nms.spigotHidePlayer(p, on);
                    BedWars.nms.spigotHidePlayer(on, p);
                }
            }

            if (getServerType() == ServerType.BUNGEE) {
                // fix invisibility issue
                //if (BedWars.nms.getVersion() == 7) {
                BedWars.nms.sendPlayerSpawnPackets(p, this);
                //}
            }
        }, 17L);

        if (getServerType() == ServerType.BUNGEE) {
            p.getEnderChest().clear();
        }

        if (getPlayers().size() >= getMaxPlayers()) {
            if (startingTask != null) {
                if (Bukkit.getScheduler().isCurrentlyRunning(startingTask.getTask())) {
                    if (startingTask.getCountdown() > BedWars.config.getInt(ConfigPath.GENERAL_CONFIGURATION_START_COUNTDOWN_SHORTENED)) {
                        startingTask.setCountdown(BedWars.config.getInt(ConfigPath.GENERAL_CONFIGURATION_START_COUNTDOWN_SHORTENED));
                    }
                }
            }
        }
        if (!isStatusChange)
            if (BedWars.getServerType() == ServerType.MULTIARENA || BedWars.getServerType() == ServerType.SHARED) {
                BoardManager.getInstance().giveTabFeatures(p, this, false);
            }

        refreshSigns();
        JoinNPC.updateNPCs(getGroup());
        return true;
    }

    /**
     * Add a player as Spectator
     *
     * @param p            Player to be added
     * @param playerBefore True if the player has played in this arena before and he died so now should be a spectator.
     */
    public boolean addSpectator(@NotNull Player p, boolean playerBefore, Location staffTeleport) {
        if (allowSpectate || playerBefore || staffTeleport != null) {

            ArenaSpectateEvent spectateEvent = new ArenaSpectateEvent(p, this);
            Bukkit.getPluginManager().callEvent(spectateEvent);
            if (spectateEvent.isCancelled()) return false;

            debug("Spectator added: " + p.getName() + " arena: " + getArenaName());

            if (!playerBefore) {
                PlayerJoinArenaEvent ev = new PlayerJoinArenaEvent(this, p, true);
                Bukkit.getPluginManager().callEvent(ev);
                if (ev.isCancelled()) return false;
            }

            //Remove from ReJoin
            ReJoin reJoin = ReJoin.getPlayer(p);
            if (reJoin != null) {
                reJoin.destroy(true);
            }

            p.closeInventory();
            spectators.add(p);
            players.remove(p);

            if (!playerBefore) {
                /* save player inv etc if isn't saved yet*/
                if (getServerType() != ServerType.BUNGEE) {
                    new PlayerGoods(p, true);
                    playerLocation.put(p, p.getLocation());
                }
                setArenaByPlayer(p, this);
            }

            BoardManager.getInstance().giveTabFeatures(p, this, false);
            nms.setCollide(p, this, false);

            if (!playerBefore) {
                if (staffTeleport == null) {
                    PaperSupport.teleportC(p, getSpectatorLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                } else {
                    PaperSupport.teleportC(p, staffTeleport, PlayerTeleportEvent.TeleportCause.PLUGIN);
                }
            }

            p.setGameMode(GameMode.ADVENTURE);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (leaving.contains(p)) return;
                p.setAllowFlight(true);
                p.setFlying(true);
            }, 5L);

            if (p.getPassenger() != null && p.getPassenger().getType() == EntityType.ARMOR_STAND)
                p.getPassenger().remove();

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (leaving.contains(p)) return;
                for (Player on : Bukkit.getOnlinePlayers()) {
                    if (on == p) continue;
                    if (getSpectators().contains(on)) {
                        BedWars.nms.spigotShowPlayer(p, on);
                        BedWars.nms.spigotShowPlayer(on, p);
                    } else if (getPlayers().contains(on)) {
                        BedWars.nms.spigotHidePlayer(p, on);
                        BedWars.nms.spigotShowPlayer(on, p);
                    } else {
                        BedWars.nms.spigotHidePlayer(p, on);
                        BedWars.nms.spigotHidePlayer(on, p);
                    }
                }


                if (!playerBefore) {
                    if (staffTeleport == null) {
                        PaperSupport.teleportC(p, getSpectatorLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                    } else {
                        PaperSupport.teleport(p, staffTeleport);
                    }
                } else {
                    PaperSupport.teleport(p, getSpectatorLocation());
                }

                p.setAllowFlight(true);
                p.setFlying(true);

                /* Spectator items */
                sendSpectatorCommandItems(p);
                // make invisible because it is annoying whene there are many spectators around the map
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false));

                p.getInventory().setArmorContents(null);
            });

            leaving.remove(p);

            p.sendMessage(getMsg(p, Messages.COMMAND_JOIN_SPECTATOR_MSG).replace("%bw_arena%", this.getDisplayName()));

            /* update generator holograms for spectators */
            for (IGenerator o : getOreGenerators()) {
                o.updateHolograms(p);
            }
            for (ITeam t : getTeams()) {
                if (!t.isShopSpawned()) continue;
                nms.spawnShopHologram(getConfig().getArenaLoc("Team." + t.getName() + ".Upgrade"), (getMaxInTeam() > 1 ? Messages.NPC_NAME_TEAM_UPGRADES.replace("%group%", group) : Messages.NPC_NAME_SOLO_UPGRADES.replace("%group%", group)), Collections.singletonList(p), this, t);
                nms.spawnShopHologram(getConfig().getArenaLoc("Team." + t.getName() + ".Shop"), (getMaxInTeam() > 1 ? Messages.NPC_NAME_TEAM_SHOP.replace("%group%", group) : Messages.NPC_NAME_SOLO_SHOP.replace("%group%", group)), Collections.singletonList(p), this, t);
                for (IGenerator o : t.getGenerators()) {
                    o.updateHolograms(p);
                }
            }

        } else {
            p.sendMessage(getMsg(p, Messages.COMMAND_JOIN_SPECTATOR_DENIED_MSG));
            return false;
        }

        showTime.remove(p);
        refreshSigns();
        JoinNPC.updateNPCs(getGroup());
        return true;
    }

    /**
     * Remove a player from the arena
     *
     * @param p          Player to be removed
     * @param disconnect True if the player was disconnected
     */
    public void removePlayer(@NotNull Player p, boolean disconnect) {
        removePlayer(p, disconnect, false);
    }

    /**
     * Remove a player from the arena
     *
     * @param p          Player to be removed
     * @param disconnect True if the player was disconnected
     * @param skipPartyCheck (default false) True if you want to skip the party checking for this player. This will stop the player
     *                       from leaving a party if he is in one. or will stop the party from being disbanded if the
     *                       player is the owner.
     */
    public void removePlayer(@NotNull Player p, boolean disconnect, boolean skipPartyCheck) {
        if (leaving.contains(p)) {
            return;
        } else {
            leaving.add(p);
        }
        debug("Player removed: " + p.getName() + " arena: " + getArenaName());
        respawnSessions.remove(p);

        ITeam team = null;

        Arena.afkCheck.remove(p.getUniqueId());
        BedWars.getAPI().getAFKUtil().setPlayerAFK(p, false);

        if (status == GameState.playing) {
            for (ITeam t : getTeams()) {
                if (t.isMember(p)) {
                    team = t;
                    t.getMembers().remove(p);
                    t.destroyBedHolo(p);
                }
            }
        }

        List<ShopCache.CachedItem> cacheList = new ArrayList<>();
        if (ShopCache.getInstance().getShopCache(p.getUniqueId()) != null) {
            //noinspection ConstantConditions
            cacheList = ShopCache.getInstance().getShopCache(p.getUniqueId()).getCachedPermanents();
        }

        LastHit lastHit = LastHit.getLastHit(p);
        Player lastDamager = (lastHit == null) ? null :
                (lastHit.getDamager() instanceof Player) ? (Player) lastHit.getDamager() : null;
        if (lastHit != null) {
            // accept damager in last 13 seconds only.
            if (lastHit.getTime() < System.currentTimeMillis() - 13_000) {
                lastDamager = null;
            }
        }
        Bukkit.getPluginManager().callEvent(new PlayerLeaveArenaEvent(p, this, lastDamager));
        //players.remove must be under call event in order to check if the player is a spectator or not
        players.remove(p);
        removeArenaByPlayer(p, this);

        for (PotionEffect pf : p.getActivePotionEffects()) {
            p.removePotionEffect(pf.getType());
        }

        if (p.getPassenger() != null && p.getPassenger().getType() == EntityType.ARMOR_STAND) p.getPassenger().remove();

        boolean teamuri = false;
        for (Player on : getPlayers()) {
            if (getPartyManager().hasParty(on)) {
                teamuri = true;
            }
        }
        if (status == GameState.starting && (maxInTeam > players.size() && teamuri || players.size() < minPlayers && !teamuri)) {
            changeStatus(GameState.waiting);
            for (Player on : players) {
                on.sendMessage(getMsg(on, Messages.ARENA_START_COUNTDOWN_STOPPED_INSUFF_PLAYERS_CHAT));
            }
        } else if (status == GameState.playing) {
            int alive_teams = 0;
            for (ITeam t : getTeams()) {
                if (t == null) continue;
                if (!t.getMembers().isEmpty()) {
                    alive_teams++;
                }
            }
            if (alive_teams == 1 && !BedWars.isShuttingDown()) {
                checkWinner();
                Bukkit.getScheduler().runTaskLater(BedWars.plugin, () -> changeStatus(GameState.restarting), 10L);
                if (team != null) {
                    if (!team.isBedDestroyed()) {
                        for (Player p2 : this.getPlayers()) {
                            p2.sendMessage(getMsg(p2, Messages.TEAM_ELIMINATED_CHAT).replace("%bw_team_color%", team.getColor().chat().toString())
                                    .replace("%bw_team_name%", team.getDisplayName(Language.getPlayerLanguage(p2))));
                        }
                        for (Player p2 : this.getSpectators()) {
                            p2.sendMessage(getMsg(p2, Messages.TEAM_ELIMINATED_CHAT).replace("%bw_team_color%", team.getColor().chat().toString())
                                    .replace("%bw_team_name%", team.getDisplayName(Language.getPlayerLanguage(p2))));
                        }
                    }
                }
            } else if (alive_teams == 0 && !BedWars.isShuttingDown()) {
                Bukkit.getScheduler().runTaskLater(BedWars.plugin, () -> changeStatus(GameState.restarting), 10L);
            } else if (!BedWars.isShuttingDown()) {
                //ReJoin feature
                new ReJoin(p, this, team, cacheList);
            }

            // pvp log out
            if (team != null) {
                ITeam killerTeam = getTeam(lastDamager);
                if (lastDamager != null && isPlayer(lastDamager) && killerTeam != null) {
                    String message;
                    PlayerKillEvent.PlayerKillCause cause;
                    if (team.isBedDestroyed()) {
                        cause = PlayerKillEvent.PlayerKillCause.PLAYER_DISCONNECT_FINAL;
                        message = Messages.PLAYER_DIE_PVP_LOG_OUT_FINAL;
                    } else {
                        message = Messages.PLAYER_DIE_PVP_LOG_OUT_REGULAR;
                        cause = PlayerKillEvent.PlayerKillCause.PLAYER_DISCONNECT;
                    }
                    PlayerKillEvent event = new PlayerKillEvent(this, p, lastDamager, player -> Language.getMsg(player, message), cause);
                    for (Player inGame : getPlayers()) {
                        Language lang = Language.getPlayerLanguage(inGame);
                        inGame.sendMessage(event.getMessage().apply(inGame)
                                .replace("%bw_team_name%", team.getDisplayName(lang))
                                .replace("%bw_player_color%", team.getColor().chat().toString()).replace("%bw_player%", p.getDisplayName()).replace("%bw_playername%", p.getName())
                                .replace("%bw_killer_color%", killerTeam.getColor().chat().toString())
                                .replace("%bw_killer_name%", lastDamager.getDisplayName())
                                .replace("%bw_killer_team_name%", killerTeam.getDisplayName(lang)));
                    }
                    for (Player inGame : getSpectators()) {
                        Language lang = Language.getPlayerLanguage(inGame);
                        inGame.sendMessage(event.getMessage().apply(inGame)
                                .replace("%bw_team_name%", team.getDisplayName(lang))
                                .replace("%bw_player_color%", team.getColor().chat().toString()).replace("%bw_player%", p.getDisplayName()).replace("%bw_playername%", p.getName())
                                .replace("%bw_killer_color%", killerTeam.getColor().chat().toString())
                                .replace("%bw_killer_name%", lastDamager.getDisplayName())
                                .replace("%bw_killer_team_name%", killerTeam.getDisplayName(lang)));
                    }
                    PlayerDrops.handlePlayerDrops(this, p, lastDamager, team, killerTeam, cause, new ArrayList<>(Arrays.asList(p.getInventory().getContents())));
                }
            }
        }
        for (Player on : getPlayers()) {
            Language language = Language.getPlayerLanguage(on);
            on.sendMessage(getMsg(language, p, Messages.COMMAND_LEAVE_MSG)
                    .replace("%bw_v_prefix%", getChatSupport().getPrefix(p))
                    .replace("%bw_v_suffix%", getChatSupport().getSuffix(p))
                    .replace("%bw_playername%", p.getName())
                    .replace("%bw_player%", p.getDisplayName()
                            .replace("%bw_on%", String.valueOf(getPlayers().size()))
                            .replace("%bw_max%", String.valueOf(getMaxPlayers()))
                    )
            );
        }
        for (Player on : getSpectators()) {
            Language language = Language.getPlayerLanguage(on);
            on.sendMessage(getMsg(language, p, Messages.COMMAND_LEAVE_MSG)
                    .replace("%bw_v_prefix%", getChatSupport().getPrefix(p))
                    .replace("%bw_v_suffix%", getChatSupport().getSuffix(p))
                    .replace("%bw_playername%", p.getName())
                    .replace("%bw_player%", p.getDisplayName()
                            .replace("%bw_on%", String.valueOf(getPlayers().size()))
                            .replace("%bw_max%", String.valueOf(getMaxPlayers()))
                    ));

        }

        if (getServerType() == ServerType.SHARED) {
            BoardManager.getInstance().remove(p);
            this.sendToMainLobby(p);

        } else if (getServerType() == ServerType.BUNGEE) {
            Misc.moveToLobbyOrKick(p, this, true);
            return;
        } else {
            this.sendToMainLobby(p);
        }

        // Clear shop holo's for leaving players.
        ShopHolo.clearForPlayer(p);

        /**
         * Below is *only* executed if serverType != BUNGEE
         */

        /* restore player inventory */
        PlayerGoods pg = PlayerGoods.getPlayerGoods(p);
        if (pg == null) {
            // if there is no previous backup of the inventory send lobby items if multi arena
            if (BedWars.getServerType() == ServerType.MULTIARENA) {
                // Send items
                Arena.sendLobbyCommandItems(p);
            }
        } else {
            pg.restore();
        }
        playerLocation.remove(p);
        for (PotionEffect pf : p.getActivePotionEffects()) {
            p.removePotionEffect(pf.getType());
        }

        if (!BedWars.isShuttingDown()) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                for (Player on : Bukkit.getOnlinePlayers()) {
                    if (on.equals(p)) continue;
                    if (getArenaByPlayer(on) == null) {
                        BedWars.nms.spigotShowPlayer(p, on);
                        BedWars.nms.spigotShowPlayer(on, p);
                    } else {
                        BedWars.nms.spigotHidePlayer(p, on);
                        BedWars.nms.spigotHidePlayer(on, p);
                    }
                }
                if (!disconnect) BoardManager.getInstance().giveTabFeatures(p, null, false);
            }, 5L);
        }

        /* Check if party need to be left */
        if (!skipPartyCheck) {
            if (getPartyManager().hasParty(p)) {
                if (getPartyManager().isOwner(p)) {
                    if (status != GameState.restarting) {
                        // prevent arena from staring with a single player
                        teamuri = false;
                        for (Player on : getPlayers()) {
                            if (getPartyManager().hasParty(on)) {
                                teamuri = true;
                            }
                        }
                        if (status == GameState.starting && (maxInTeam > players.size() && teamuri || players.size() < minPlayers && !teamuri)) {
                            changeStatus(GameState.waiting);
                            for (Player on : players) {
                                on.sendMessage(getMsg(on, Messages.ARENA_START_COUNTDOWN_STOPPED_INSUFF_PLAYERS_CHAT));
                            }
                        }
                    }
                } else {
                    getPartyManager().removeFromParty(p);
                }
            }
        }
        p.setFlying(false);
        p.setAllowFlight(false);

        //Remove from ReJoin if game ended
        if (status == GameState.restarting) {
            if (ReJoin.exists(p)) {
                //noinspection ConstantConditions
                if (ReJoin.getPlayer(p).getArena() == this) {
                    //noinspection ConstantConditions
                    ReJoin.getPlayer(p).destroy(false);
                }
            }
        }

        //Remove from magic milk
        if (magicMilk.containsKey(p.getUniqueId())) {
            int taskId = magicMilk.remove(p.getUniqueId());
            if (taskId > 0) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
        }

        showTime.remove(p);

        refreshSigns();
        JoinNPC.updateNPCs(getGroup());

        if (lastHit != null) {
            lastHit.remove();
        }
    }


    /**
     * Remove a spectator from the arena
     *
     * @param p          Player to be removed
     * @param disconnect True if the player was disconnected
     */
    public void removeSpectator(@NotNull Player p, boolean disconnect) {
        removeSpectator(p, disconnect, false);
    }

    /**
     * Remove a spectator from the arena
     *
     * @param p              Player to be removed
     * @param disconnect     True if the player was disconnected
     * @param skipPartyCheck (default false) True if you want to skip the party checking for this player. This will stop the player
     *                       from leaving a party if he is in one. or will stop the party from being disbanded if the
     *                       player is the owner.
     */
    public void removeSpectator(@NotNull Player p, boolean disconnect, boolean skipPartyCheck) {
        if (leaving.contains(p)) {
            return;
        } else {
            leaving.add(p);
        }
        debug("Spectator removed: " + p.getName() + " arena: " + getArenaName());

        Bukkit.getPluginManager().callEvent(new PlayerLeaveArenaEvent(p, this, null));
        spectators.remove(p);
        removeArenaByPlayer(p, this);
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        nms.setCollide(p, this, true);

        Arena.afkCheck.remove(p.getUniqueId());
        BedWars.getAPI().getAFKUtil().setPlayerAFK(p, false);

        if (getServerType() == ServerType.SHARED) {
            BoardManager.getInstance().remove(p);
            this.sendToMainLobby(p);
        } else if (getServerType() == ServerType.MULTIARENA) {
            this.sendToMainLobby(p);

        }
        for (PotionEffect pf : p.getActivePotionEffects()) {
            p.removePotionEffect(pf.getType());
        }

        /* restore player inventory */
        PlayerGoods pg = PlayerGoods.getPlayerGoods(p);
        if (pg == null) {
            // if there is no previous backup of the inventory send lobby items if multi arena
            if (BedWars.getServerType() == ServerType.MULTIARENA) {
                // Send items
                Arena.sendLobbyCommandItems(p);
            }
        } else {
            pg.restore();
        }
        if (getServerType() == ServerType.BUNGEE) {
            Misc.moveToLobbyOrKick(p, this, true);
            return;
        }
        playerLocation.remove(p);

        if (!BedWars.isShuttingDown()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Player on : Bukkit.getOnlinePlayers()) {
                    if (on.equals(p)) continue;
                    if (getArenaByPlayer(on) == null) {
                        BedWars.nms.spigotShowPlayer(p, on);
                        BedWars.nms.spigotShowPlayer(on, p);
                    } else {
                        BedWars.nms.spigotHidePlayer(p, on);
                        BedWars.nms.spigotHidePlayer(on, p);
                    }
                }
                if (!disconnect) BoardManager.getInstance().giveTabFeatures(p, null, false);
            });
        }

        /* Check if party need to be left */
        if (!skipPartyCheck) {
            if (getPartyManager().hasParty(p)) {
                if (!getPartyManager().isOwner(p)) {
                    getPartyManager().removeFromParty(p);
                }
            }
        }

        p.setFlying(false);
        p.setAllowFlight(false);

        //Remove from ReJoin if game ended
        if (ReJoin.exists(p)) {
            //noinspection ConstantConditions
            if (ReJoin.getPlayer(p).getArena() == this) {
                //noinspection ConstantConditions
                ReJoin.getPlayer(p).destroy(false);
            }
        }

        //Remove from magic milk
        if (magicMilk.containsKey(p.getUniqueId())) {
            int taskId = magicMilk.get(p.getUniqueId());
            if (taskId > 0) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
        }

        refreshSigns();
        JoinNPC.updateNPCs(getGroup());
    }

    /**
     * Rejoin an arena
     */
    public boolean reJoin(Player p) {
        ReJoin reJoin = ReJoin.getPlayer(p);
        if (reJoin == null) return false;
        if (reJoin.getArena() != this) return false;
        if (!reJoin.canReJoin()) return false;

        if (reJoin.getTask() != null) {
            reJoin.getTask().destroy();
        }

        PlayerReJoinEvent ev = new PlayerReJoinEvent(p, this, BedWars.config.getInt(ConfigPath.GENERAL_CONFIGURATION_REJOIN_RE_SPAWN_COUNTDOWN));
        Bukkit.getPluginManager().callEvent(ev);
        if (ev.isCancelled()) return false;

        for (Player on : Bukkit.getOnlinePlayers()) {
            if (on.equals(p)) continue;
            if (!isInArena(on)) {
                BedWars.nms.spigotHidePlayer(on, p);
                BedWars.nms.spigotHidePlayer(p, on);
            }
        }

        p.closeInventory();
        players.add(p);
        leaving.remove(p);

        for (Player on : players) {
            on.sendMessage(getMsg(on, Messages.COMMAND_REJOIN_PLAYER_RECONNECTED).replace("%bw_playername%", p.getName()).replace("%bw_player%", p.getDisplayName()).replace("%bw_on%", String.valueOf(getPlayers().size())).replace("%bw_max%", String.valueOf(getMaxPlayers())));
        }
        for (Player on : spectators) {
            on.sendMessage(getMsg(on, Messages.COMMAND_REJOIN_PLAYER_RECONNECTED).replace("%bw_playername%", p.getName()).replace("%bw_player%", p.getDisplayName()).replace("%bw_on%", String.valueOf(getPlayers().size())).replace("%bw_max%", String.valueOf(getMaxPlayers())));
        }
        setArenaByPlayer(p, this);
        /* save player inventory etc */
        if (BedWars.getServerType() != ServerType.BUNGEE) {
            // no need to backup inventory because it's empty
            //new PlayerGoods(p, true, true);
            playerLocation.put(p, p.getLocation());
        }
        PaperSupport.teleportC(p, getSpectatorLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        p.getInventory().clear();

        //restore items before re-spawning in team
        ShopCache sc = ShopCache.getInstance().getShopCache(p.getUniqueId());
        if (sc != null) sc.destroy();
        sc = new ShopCache(p.getUniqueId());
        for (ShopCache.CachedItem ci : reJoin.getPermanentsAndNonDowngradables()) {
            sc.getCachedItems().add(ci);
        }

        reJoin.getBedWarsTeam().reJoin(p, ev.getRespawnTime());
        reJoin.destroy(false);
        Bukkit.getScheduler().runTaskLater(BedWars.plugin, () -> {
            BoardManager.getInstance().giveTabFeatures(p, this, true);
        }, 10L);//todo check if can be pulled out to listeners.
        return true;
    }

    /**
     * Disable the arena.
     * This will automatically kick/ remove the people from the arena.
     */
    public void disable() {
        for (Player p : new ArrayList<>(players)) {
            removePlayer(p, false);
        }
        for (Player p : new ArrayList<>(spectators)) {
            removeSpectator(p, false);
        }
        if (getRestartingTask() != null) getRestartingTask().cancel();
        if (getStartingTask() != null) getStartingTask().cancel();
        if (getPlayingTask() != null) getPlayingTask().cancel();
        if (getAnnouncementTask() != null) getAnnouncementTask().cancel();
        plugin.getLogger().log(Level.WARNING, "Disabling arena: " + getArenaName());
        for (Player inWorld : getWorld().getPlayers()) {
            inWorld.kickPlayer("You're not supposed to be here.");
        }
        BedWars.getAPI().getRestoreAdapter().onDisable(this);
        Bukkit.getPluginManager().callEvent(new ArenaDisableEvent(getArenaName(), getWorldName()));
        destroyData();
    }

    /**
     * Restart the arena.
     */
    public void restart() {
        if (getRestartingTask() != null) getRestartingTask().cancel();
        if (getStartingTask() != null) getStartingTask().cancel();
        if (getPlayingTask() != null) getPlayingTask().cancel();
        if (getAnnouncementTask() != null) getAnnouncementTask().cancel();
        if (null != moneyperMinuteTask) {
            moneyperMinuteTask.cancel();
        }
        if (null != perMinuteTask) {
            perMinuteTask.cancel();
        }
        plugin.getLogger().log(Level.FINE, "Restarting arena: " + getArenaName());
        Bukkit.getPluginManager().callEvent(new ArenaRestartEvent(getArenaName(), getWorldName()));
        for (Player inWorld : getWorld().getPlayers()) {
            inWorld.kickPlayer("You're not supposed to be here.");
        }
        BedWars.getAPI().getRestoreAdapter().onRestart(this);
        destroyData();
    }

    //GETTER METHODS

    /**
     * Get the arena world
     */
    public World getWorld() {
        return world;
    }

    /**
     * Get the max number of teammates in a team
     */

    @Override
    public int getMaxInTeam() {
        return maxInTeam;
    }

    /**
     * Get an arena by arena name
     *
     * @param arenaName arena name
     */
    public static IArena getArenaByName(String arenaName) {
        return arenaByName.get(arenaName);
    }

    /**
     * Get an arena by world name
     *
     * @param worldName world name
     */
    public static IArena getArenaByIdentifier(String worldName) {
        return arenaByIdentifier.get(worldName);
    }

    /**
     * Get an arena by a player. Spectator or Player.
     *
     * @param p Target player
     * @return The arena where the player is in. Can be NULL.
     */
    public static @Nullable IArena getArenaByPlayer(Player p) {
        return arenaByPlayer.get(p);
    }

    /**
     * Get an arenas list
     */
    public static LinkedList<IArena> getArenas() {
        return arenas;
    }

    /**
     * Get the display status for an arena.
     * A message that can be used on signs etc.
     */
    public String getDisplayStatus(Language lang) {
        String s = "";
        switch (status) {
            case waiting:
                s = lang.m(Messages.ARENA_STATUS_WAITING_NAME);
                break;
            case starting:
                s = lang.m(Messages.ARENA_STATUS_STARTING_NAME);
                break;
            case restarting:
                s = lang.m(Messages.ARENA_STATUS_RESTARTING_NAME);
                break;
            case playing:
                s = lang.m(Messages.ARENA_STATUS_PLAYING_NAME);
                break;
        }
        return s.replace("%bw_full%", this.getPlayers().size() == this.getMaxPlayers() ? lang.m(Messages.MEANING_FULL) : "");
    }

    @Override
    public String getDisplayGroup(Player player) {
        return getPlayerLanguage(player).m(Messages.ARENA_DISPLAY_GROUP_PATH + getGroup().toLowerCase());
    }

    @Override
    public String getDisplayGroup(@NotNull Language language) {
        return language.m(Messages.ARENA_DISPLAY_GROUP_PATH + getGroup().toLowerCase());
    }

    /**
     * Get the players list
     */
    @Override
    public List<Player> getPlayers() {
        return players;
    }

    /**
     * Get the max number of players that can play on this arena.
     */
    @Override
    public int getMaxPlayers() {
        return maxPlayers;
    }

    /**
     * Get the arena name as a message that can be used on signs etc.
     *
     * @return A string with - and _ replaced by a space.
     */
    @Override
    public String getDisplayName() {
        return getConfig().getYml().getString(ConfigPath.ARENA_DISPLAY_NAME, (Character.toUpperCase(arenaName.charAt(0)) + arenaName.substring(1)).replace("_", " ").replace("-", " ")).trim().isEmpty() ?
                (Character.toUpperCase(arenaName.charAt(0)) + arenaName.substring(1)).replace("_", " ").replace("-", " ")
                : getConfig().getString(ConfigPath.ARENA_DISPLAY_NAME);
    }

    @Override
    public void setWorldName(String name) {
        this.worldName = name;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public String getArenaName() {
        return arenaName;
    }

    @Override
    public List<ITeam> getTeams() {
        return teams;
    }

    @Override
    public ArenaConfig getConfig() {
        return cm;
    }

    @Override
    public void addPlacedBlock(Block block) {
        if (block == null) return;
        placed.add(new org.bukkit.util.Vector(block.getX(), block.getY(), block.getZ()));
    }

    @Override
    public void removePlacedBlock(Block block) {
        if (block == null) return;
        if (!isBlockPlaced(block)) return;
        placed.remove(new org.bukkit.util.Vector(block.getX(), block.getY(), block.getZ()));
    }

    @Override
    public boolean isBlockPlaced(Block block) {
        for (org.bukkit.util.Vector v : getPlaced()) {
            if (v.getX() == block.getX() && v.getY() == block.getY() && v.getZ() == block.getZ()) return true;
        }
        return false;
    }

    /**
     * Get a player kills count.
     *
     * @param p          Target player
     * @param finalKills True if you want to get the Final Kills. False for regular kills.
     */
    public int getPlayerKills(Player p, boolean finalKills) {
        if (finalKills) return playerFinalKills.getOrDefault(p, 0);
        return playerKills.getOrDefault(p.getName(), 0);
    }

    /**
     * Get the player beds destroyed count
     *
     * @param p Target player
     */
    public int getPlayerBedsDestroyed(Player p) {
        if (playerBedsDestroyed.containsKey(p)) return playerBedsDestroyed.get(p);
        return 0;
    }

    /**
     * Get the join signs for this arena
     *
     * @return signs.
     */
    public List<Block> getSigns() {
        return signs;
    }

    /**
     * Get the island radius
     */
    public int getIslandRadius() {
        return islandRadius;
    }

    //SETTER METHODS
    @Override
    public void setGroup(String group) {
        this.group = group;
        BoardManager.getInstance().registerArenaScoreboards(this);
    }

    public static void setArenaByPlayer(Player p, IArena arena) {
        arenaByPlayer.put(p, arena);
        arena.refreshSigns();
        JoinNPC.updateNPCs(arena.getGroup());
    }

    public static void setArenaByName(IArena arena) {
        arenaByName.put(arena.getArenaName(), arena);
    }

    public static void removeArenaByName(@NotNull String arena) {
        arenaByName.remove(arena.replace("_clone", ""));
    }

    public static void removeArenaByPlayer(Player p, @NotNull IArena arena) {
        arenaByPlayer.remove(p);
        arena.refreshSigns();
        JoinNPC.updateNPCs(arena.getGroup());
    }

    /**
     * Set game status without starting stats.
     */
    public void setStatus(GameState status) {
        if (this.status != GameState.playing && status == GameState.playing) {
            startTime = Instant.now();
        }
        // if countdown cancelled
        if (this.status == GameState.starting && status == GameState.waiting) {
            for (Player player : getPlayers()) {
                Language playerLang = Language.getPlayerLanguage(player);
                nms.sendTitle(player, playerLang.m(Messages.ARENA_STATUS_START_COUNTDOWN_CANCELLED_TITLE), playerLang.m(Messages.ARENA_STATUS_START_COUNTDOWN_CANCELLED_SUB_TITLE), 0, 40, 10);
            }
        }
        this.status = status;
    }

    /**
     * Change game status starting tasks.
     */
    public void changeStatus(GameState status) {
        if (this.status != GameState.playing && status == GameState.playing) {
            startTime = Instant.now();
        }
        this.status = status;
        Bukkit.getPluginManager().callEvent(new GameStateChangeEvent(this, status, status));
        refreshSigns();
        if (status == GameState.playing) {
            for (Player p : players) {
                Arena.afkCheck.remove(p.getUniqueId());
                BedWars.getAPI().getAFKUtil().setPlayerAFK(p, false);
            }
            for (Player p : spectators) {
                Arena.afkCheck.remove(p.getUniqueId());
                BedWars.getAPI().getAFKUtil().setPlayerAFK(p, false);
            }
        }

        //Stop active tasks to prevent issues
        BukkitScheduler bs = Bukkit.getScheduler();
        if (startingTask != null) {
            if (bs.isCurrentlyRunning(startingTask.getTask()) || bs.isQueued(startingTask.getTask()))
                startingTask.cancel();
        }
        startingTask = null;

        if (playingTask != null) {
            if (bs.isCurrentlyRunning(playingTask.getTask()) || bs.isQueued(playingTask.getTask()))
                playingTask.cancel();
        }
        playingTask = null;

        if (restartingTask != null) {
            if (bs.isCurrentlyRunning(restartingTask.getTask()) || bs.isQueued(restartingTask.getTask()))
                restartingTask.cancel();
        }
        restartingTask = null;
        if (null != moneyperMinuteTask) {
            moneyperMinuteTask.cancel();
        }
        if (null != perMinuteTask) {
            perMinuteTask.cancel();
        }

        if (status == GameState.starting) {
            startingTask = new GameStartingTask(this);
        } else if (status == GameState.playing) {
            if (BedWars.getLevelSupport() instanceof InternalLevel) {
                perMinuteTask = new PerMinuteTask(this);
            }
            if (BedWars.getEconomy() instanceof WithEconomy) {
                moneyperMinuteTask = new MoneyPerMinuteTask(this);
            }
            playingTask = new GamePlayingTask(this);
            if (config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_IN_GAME_ANNOUNCEMENT_ENABLE)) {
                announcementTask = new GameAnnouncementTask(this);
            }
        } else if (status == GameState.restarting) {
            restartingTask = new GameRestartingTask(this);
        }
        PlayerPlaceholder prefixPlaceholder = (PlayerPlaceholder) TabAPI.getInstance().getPlaceholderManager().getPlaceholder("%bw_prefix%");
        PlayerPlaceholder suffixPlaceholder = (PlayerPlaceholder) TabAPI.getInstance().getPlaceholderManager().getPlaceholder("%bw_suffix%");
        players.forEach(c -> {
            BoardManager.getInstance().giveTabFeatures(c, this, false);
            TabPlayer tabPlayer = TabAPI.getInstance().getPlayer(c.getUniqueId());
            assert tabPlayer != null;
            prefixPlaceholder.updateValue(tabPlayer, BoardManager.getInstance().getPrefix(tabPlayer));
            suffixPlaceholder.updateValue(tabPlayer, BoardManager.getInstance().getSuffix(tabPlayer));
        });

        spectators.forEach(c -> {
            BoardManager.getInstance().giveTabFeatures(c, this, false);
            TabPlayer tabPlayer = TabAPI.getInstance().getPlayer(c.getUniqueId());
            assert tabPlayer != null;
            prefixPlaceholder.updateValue(tabPlayer, BoardManager.getInstance().getPrefix(tabPlayer));
            suffixPlaceholder.updateValue(tabPlayer, BoardManager.getInstance().getSuffix(tabPlayer));
        });
    }

    /**
     * Check if a player has vip perms
     */
    public static boolean isVip(Player p) {
        return p.hasPermission(mainCmd + ".*") || p.hasPermission(mainCmd + ".vip");
    }

    /**
     * Check if a player is playing.
     */
    @Override
    public boolean isPlayer(Player p) {
        return players.contains(p);
    }

    /**
     * Check if a player is spectating.
     */
    @Override
    public boolean isSpectator(Player p) {
        return spectators.contains(p);
    }

    @Override
    public boolean isSpectator(UUID player) {
        for (Player p : getSpectators()) {
            if (p.getUniqueId().equals(player)) return true;
        }
        return false;
    }

    @Override
    public boolean isReSpawning(UUID player) {
        if (player == null) return false;
        for (Player reSpawnSession : respawnSessions.keySet()) {
            if (reSpawnSession.getUniqueId().equals(player)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add a join sign for the arena.
     */
    public void addSign(Location loc) {
        if (loc == null) return;
        if (loc.getBlock().getType().toString().endsWith("_SIGN") || loc.getBlock().getType().toString().endsWith("_WALL_SIGN")) {
            signs.add(loc.getBlock());
            refreshSigns();
            BlockStatusListener.updateBlock(this);
        }
    }

    /**
     * Get game stage.
     */
    @Override
    public GameState getStatus() {
        return status;
    }


    /**
     * Refresh signs.
     */
    public synchronized void refreshSigns() {
        for (Block b : getSigns()) {
            if (b == null) continue;
            if (!(b.getType().toString().endsWith("_SIGN") || b.getType().toString().endsWith("_WALL_SIGN"))) continue;
            if (!(b.getState() instanceof Sign)) continue;
            Sign s = (Sign) b.getState();
            if (s == null) return;
            int line = 0;
            for (String string : BedWars.signs.getList("format")) {
                if (string == null) continue;
                if (getPlayers() == null) continue;
                s.setLine(line, string.replace("[on]", String.valueOf(getPlayers().size()))
                        .replace("[max]", String.valueOf(getMaxPlayers())).replace("[arena]", getDisplayName())
                        .replace("[status]", getDisplayStatus(Language.getDefaultLanguage()))
                        .replace("[type]", String.valueOf(getMaxInTeam())));
                line++;
            }
            try {
                s.update(true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Get a list of spectators for this arena.
     */
    @Override
    public List<Player> getSpectators() {
        return spectators;
    }

    /**
     * Add a kill point to the game stats.
     */
    public void addPlayerKill(Player p, boolean finalKill, Player victim) {
        if (p == null) return;
        if (playerKills.containsKey(p.getName())) {
            playerKills.replace(p.getName(), playerKills.get(p.getName()) + 1);
        } else {
            playerKills.put(p.getName(), 1);
        }
        if (finalKill) {
            if (playerFinalKills.containsKey(p)) {
                playerFinalKills.replace(p, playerFinalKills.get(p) + 1);
            } else {
                playerFinalKills.put(p, 1);
            }
            playerFinalKillDeaths.put(victim, 1);
        }
    }

    /**
     * Add a destroyed bed point to the player temp stats.
     */
    public void addPlayerBedDestroyed(Player p) {
        if (playerBedsDestroyed.containsKey(p)) {
            playerBedsDestroyed.replace(p, playerBedsDestroyed.get(p) + 1);
            return;
        }
        playerBedsDestroyed.put(p, 1);
    }

    /**
     * This will give the lobby items to the player.
     * Not used in serverType BUNGEE.
     * This will clear the inventory first.
     */
    public static void sendLobbyCommandItems(Player p) {
        if (!BedWars.config.getLobbyWorldName().equalsIgnoreCase(p.getWorld().getName())) return;
        p.getInventory().clear();

        for (IPermanentItem lobbyItem : BedWars.getAPI().getItemUtil().getLobbyItems()) {
            ItemStack item = lobbyItem.getItem();
            ItemMeta itemMeta = lobbyItem.getItem().getItemMeta();
            if (itemMeta != null) {
                String name;
                List<String> lore;

                // Add correct name and lore for the player language
                name = SupportPAPI.getSupportPAPI().replace(p, getMsg(p, Messages.GENERAL_CONFIGURATION_LOBBY_ITEMS_NAME.replace("%path%", lobbyItem.getIdentifier())));
                lore = SupportPAPI.getSupportPAPI().replace(p, getList(p, Messages.GENERAL_CONFIGURATION_LOBBY_ITEMS_LORE.replace("%path%", lobbyItem.getIdentifier())));

                itemMeta.setDisplayName(name);
                itemMeta.setLore(lore);

                item.setItemMeta(itemMeta);
            }
            if (lobbyItem.getHandler().isVisible(p, null)) p.getInventory().setItem(lobbyItem.getSlot(), item);
        }
    }

    /**
     * This will give the pre-game command Items.
     * This will clear the inventory first.
     */
    public void sendPreGameCommandItems(Player p) {
        p.getInventory().clear();

        for (IPermanentItem preGameItem : BedWars.getAPI().getItemUtil().getPreGameItems()) {
            ItemStack item = preGameItem.getItem();
            ItemMeta itemMeta = preGameItem.getItem().getItemMeta();
            if (itemMeta != null) {
                String name;
                List<String> lore;

                // Add correct name and lore for the player language
                name = SupportPAPI.getSupportPAPI().replace(p, getMsg(p, Messages.GENERAL_CONFIGURATION_WAITING_ITEMS_NAME.replace("%path%", preGameItem.getIdentifier())));
                lore = SupportPAPI.getSupportPAPI().replace(p, getList(p, Messages.GENERAL_CONFIGURATION_WAITING_ITEMS_LORE.replace("%path%", preGameItem.getIdentifier())));

                itemMeta.setDisplayName(name);
                itemMeta.setLore(lore);

                item.setItemMeta(itemMeta);
            }
            if (preGameItem.getHandler().isVisible(p, this))
                p.getInventory().setItem(preGameItem.getSlot(), item);
        }
    }

    /**
     * This will give the spectator command Items.
     * This will clear the inventory first.
     */
    public void sendSpectatorCommandItems(Player p) {
        p.getInventory().clear();

        for (IPermanentItem lobbyItem : BedWars.getAPI().getItemUtil().getSpectatorItems()) {
            ItemStack item = lobbyItem.getItem();
            ItemMeta itemMeta = lobbyItem.getItem().getItemMeta();
            if (itemMeta != null) {
                String name;
                List<String> lore;

                // Add correct name and lore for the player language
                name = SupportPAPI.getSupportPAPI().replace(p, getMsg(p, Messages.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_NAME.replace("%path%", lobbyItem.getIdentifier())));
                lore = SupportPAPI.getSupportPAPI().replace(p, getList(p, Messages.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_LORE.replace("%path%", lobbyItem.getIdentifier())));

                itemMeta.setDisplayName(name);
                itemMeta.setLore(lore);

                item.setItemMeta(itemMeta);
            }
            if (lobbyItem.getHandler().isVisible(p, this))
                p.getInventory().setItem(lobbyItem.getSlot(), item);
        }
    }

    /**
     * Check if a player is in the arena.
     *
     * @return true if is playing or spectating.
     */
    public static boolean isInArena(Player p) {
        return arenaByPlayer.containsKey(p);
    }

    /**
     * Get team by player.
     * Make sure the player is in this arena first.
     */
    @Override
    public ITeam getTeam(Player p) {
        for (ITeam t : getTeams()) {
            if (t.isMember(p)) {
                return t;
            }
        }
        return null;
    }

    /**
     * Get ex team by player.
     * Check the team where he played before leaving or losing.
     */
    @Override
    public ITeam getExTeam(UUID p) {
        for (ITeam t : getTeams()) {
            if (t.wasMember(p)) {
                return t;
            }
        }
        return null;
    }

    /**
     * Check winner. You can always do that.
     * It will manage the arena restart and the needed stuff.
     */
    public void checkWinner() {
        if (status != GameState.restarting) {
            int max = getTeams().size(), eliminated = 0;
            ITeam winner = null;
            for (ITeam t : getTeams()) {
                if (t.getMembers().isEmpty()) {
                    eliminated++;
                } else {
                    winner = t;
                }
            }
            if (max - eliminated == 1) {
                if (winner != null) {
                    if (!winner.getMembers().isEmpty()) {
                        for (Player p : winner.getMembers()) {
                            if (!p.isOnline()) continue;
                            p.getInventory().clear();
                        }
                    }

                    Player firstPlayer = null;
                    Player secondPlayer = null;
                    Player thirdPlayer = null;
                    StringBuilder winners = new StringBuilder();

                    for (int i = 0; i < winner.getMembers().size(); i++) {
                        Player p = winner.getMembers().get(i);
                        //Send winning title to the winner
                        if (p.getWorld().equals(getWorld())) {
                            nms.sendTitle(p, getMsg(p, Messages.GAME_END_VICTORY_PLAYER_TITLE), null, 0, 70, 20);
                        }
                        //Build the winner format message
                        if (!winners.toString().contains(p.getDisplayName())) {
                            if (winner.getSize() > 1 && i + 1 != winner.getMembers().size()) {
                                winners.append(getMsg(p, Messages.FORMATTING_EACH_WINNER)
                                        .replace("%bw_v_prefix%", getChatSupport().getPrefix(p))
                                        .replace("%bw_v_suffix%", getChatSupport().getSuffix(p))
                                        .replace("%bw_playername%", p.getName())
                                        .replace("%bw_player%", p.getDisplayName())).append("§7, ");
                            } else {
                                winners.append(getMsg(p, Messages.FORMATTING_EACH_WINNER)
                                        .replace("%bw_v_prefix%", getChatSupport().getPrefix(p))
                                        .replace("%bw_v_suffix%", getChatSupport().getSuffix(p))
                                        .replace("%bw_playername%", p.getName())
                                        .replace("%bw_player%", p.getDisplayName()));
                            }
                        }
                    }

                    int first = 0, second = 0, third = 0;
                    if (!playerKills.isEmpty()) {

                        LinkedHashMap<String, Integer> reverseSortedMap = new LinkedHashMap<>();

                        //Use Comparator.reverseOrder() for reverse ordering
                        playerKills.entrySet()
                                .stream()
                                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                                .forEachOrdered(x -> reverseSortedMap.put(x.getKey(), x.getValue()));

                        int entry = 0;
                        for (Map.Entry<String, Integer> e : reverseSortedMap.entrySet()) {
                            if (entry == 0) {
                                Player onlinePlayer = Bukkit.getPlayerExact(e.getKey());
                                if (onlinePlayer != null) {
                                    firstPlayer = onlinePlayer;
                                }
                                first = e.getValue();
                            } else if (entry == 1) {
                                Player onlinePlayer = Bukkit.getPlayerExact(e.getKey());
                                if (onlinePlayer != null) {
                                    secondPlayer = onlinePlayer;
                                }
                                second = e.getValue();
                            } else if (entry == 2) {
                                Player onlinePlayer = Bukkit.getPlayerExact(e.getKey());
                                if (onlinePlayer != null) {
                                    thirdPlayer = onlinePlayer;
                                }
                                third = e.getValue();
                                break;
                            }
                            entry++;
                        }
                    }
                    for (Player p : world.getPlayers()) {
                        p.sendMessage(getMsg(p, Messages.GAME_END_TEAM_WON_CHAT)
                                .replace("%bw_team_color%", winner.getColor().chat().toString())
                                .replace("%bw_team_name%", winner.getDisplayName(Language.getPlayerLanguage(p))));

                        if (!winner.getMembers().contains(p)) {
                            nms.sendTitle(p, getMsg(p, Messages.GAME_END_GAME_OVER_PLAYER_TITLE), null, 0, 70, 20);
                        }

                        for (String s : getList(p, Messages.GAME_END_TOP_PLAYER_CHAT)) {
                            String message = s
                                    .replace("%bw_first_format%", firstPlayer == null ? getMsg(p, Messages.MEANING_NOBODY) : getMsg(firstPlayer, Messages.GAME_END_FIRST_KILLER)
                                            .replace("%bw_v_prefix%", getChatSupport().getPrefix(firstPlayer))
                                            .replace("%bw_v_suffix%", getChatSupport().getSuffix(firstPlayer))
                                            .replace("%bw_playername%", firstPlayer.getName())
                                            .replace("%bw_player%", firstPlayer.getDisplayName())).replace("%bw_first_kills%", String.valueOf(first))

                                    .replace("%bw_second_format%", secondPlayer == null ? getMsg(p, Messages.MEANING_NOBODY) : getMsg(secondPlayer, Messages.GAME_END_SECOND_KILLER)
                                            .replace("%bw_v_prefix%", getChatSupport().getPrefix(secondPlayer))
                                            .replace("%bw_v_suffix%", getChatSupport().getSuffix(secondPlayer))
                                            .replace("%bw_playername%", secondPlayer.getName())
                                            .replace("%bw_player%", secondPlayer.getDisplayName())).replace("%bw_second_kills%", String.valueOf(second))

                                    .replace("%bw_third_format%", thirdPlayer == null ? getMsg(p, Messages.MEANING_NOBODY) : getMsg(thirdPlayer, Messages.GAME_END_THIRD_KILLER)
                                            .replace("%bw_v_prefix%", getChatSupport().getPrefix(thirdPlayer))
                                            .replace("%bw_v_suffix%", getChatSupport().getSuffix(thirdPlayer))
                                            .replace("%bw_playername%", thirdPlayer.getName())
                                            .replace("%bw_player%", thirdPlayer.getDisplayName())).replace("%bw_third_kills%", String.valueOf(third))

                                    .replace("%bw_winner_format%", getMaxInTeam() > 1 ? getMsg(p, Messages.FORMATTING_TEAM_WINNER_FORMAT).replace("%bw_winner_members%", winners.toString()) : getMsg(p, Messages.FORMATTING_SOLO_WINNER_FORMAT).replace("%bw_winner_members%", winners.toString()))
                                    .replace("%bw_team_color%", winner.getColor().chat().toString()).replace("%bw_team_name%", winner.getDisplayName(Language.getPlayerLanguage(p)));
                            p.sendMessage(SupportPAPI.getSupportPAPI().replace(p, message));
                        }
                    }
                }
                changeStatus(GameState.restarting);

                //Game end event
                List<UUID> winners = new ArrayList<>(), losers = new ArrayList<>(), aliveWinners = new ArrayList<>();
                for (Player p : getPlayers()) {
                    aliveWinners.add(p.getUniqueId());
                }
                if (winner != null) {
                    for (Player p : winner.getMembersCache()) {
                        winners.add(p.getUniqueId());
                    }
                }
                for (ITeam bwt : getTeams()) {
                    if (winner != null) {
                        if (bwt == winner) continue;
                    }
                    //noinspection deprecation
                    for (Player p : bwt.getMembersCache()) {
                        losers.add(p.getUniqueId());
                    }
                }
                Bukkit.getPluginManager().callEvent(new GameEndEvent(this, winners, losers, winner, aliveWinners));
                //

            }
            if (players.size() == 0 && status != GameState.restarting) {
                changeStatus(GameState.restarting);
            }
        }
    }

    /**
     * Add a kill to the player temp stats.
     */
    public void addPlayerDeath(Player p) {
        if (playerDeaths.containsKey(p)) {
            playerDeaths.replace(p, playerDeaths.get(p) + 1);
        } else {
            playerDeaths.put(p, 1);
        }
    }


    /**
     * Set next event for the arena.
     */
    public void setNextEvent(NextEvent nextEvent) {
        if (this.nextEvent != null) {
            Sounds.playSound(this.nextEvent.getSoundPath(), getPlayers());
            Sounds.playSound(this.nextEvent.getSoundPath(), getSpectators());
        }
        Bukkit.getPluginManager().callEvent(new NextEventChangeEvent(this, nextEvent, this.nextEvent));
        this.nextEvent = nextEvent;
    }

    @Override
    public void updateNextEvent() {

        debug("---");
        debug("updateNextEvent called");
        if (nextEvent == NextEvent.EMERALD_GENERATOR_TIER_II && upgradeEmeraldsCount == 0) {
            // next diamond time < next emerald time
            int next = getGeneratorsCfg().getInt(getGeneratorsCfg().getYml().get(getGroup() + "." + ConfigPath.GENERATOR_EMERALD_TIER_III_START) == null ?
                    "Default." + ConfigPath.GENERATOR_EMERALD_TIER_III_START : getGroup() + "." + ConfigPath.GENERATOR_EMERALD_TIER_III_START);
            if (upgradeDiamondsCount < next && diamondTier == 1) {
                setNextEvent(NextEvent.DIAMOND_GENERATOR_TIER_II);
            } else if (upgradeDiamondsCount < next && diamondTier == 2) {
                setNextEvent(NextEvent.DIAMOND_GENERATOR_TIER_III);
            } else {
                setNextEvent(NextEvent.EMERALD_GENERATOR_TIER_III);
            }
            upgradeEmeraldsCount = next;
            emeraldTier = 2;
            sendEmeraldsUpgradeMessages();
            for (IGenerator o : getOreGenerators()) {
                if (o.getType() == GeneratorType.EMERALD && o.getBedWarsTeam() == null) {
                    o.upgrade();
                }
            }
        } else if (nextEvent == NextEvent.DIAMOND_GENERATOR_TIER_II && upgradeDiamondsCount == 0) {
            int next = getGeneratorsCfg().getInt(getGeneratorsCfg().getYml().get(getGroup() + "." + ConfigPath.GENERATOR_DIAMOND_TIER_III_START) == null ?
                    "Default." + ConfigPath.GENERATOR_DIAMOND_TIER_III_START : getGroup() + "." + ConfigPath.GENERATOR_DIAMOND_TIER_III_START);
            if (upgradeEmeraldsCount < next && emeraldTier == 1) {
                setNextEvent(NextEvent.EMERALD_GENERATOR_TIER_II);
            } else if (upgradeEmeraldsCount < next && emeraldTier == 2) {
                setNextEvent(NextEvent.EMERALD_GENERATOR_TIER_III);
            } else {
                setNextEvent(NextEvent.DIAMOND_GENERATOR_TIER_III);
            }
            upgradeDiamondsCount = next;
            diamondTier = 2;
            sendDiamondsUpgradeMessages();
            for (IGenerator o : getOreGenerators()) {
                if (o.getType() == GeneratorType.DIAMOND && o.getBedWarsTeam() == null) {
                    o.upgrade();
                }
            }
        } else if (nextEvent == NextEvent.EMERALD_GENERATOR_TIER_III && upgradeEmeraldsCount == 0) {
            emeraldTier = 3;
            sendEmeraldsUpgradeMessages();
            if (diamondTier == 1 && upgradeDiamondsCount > 0) {
                setNextEvent(NextEvent.DIAMOND_GENERATOR_TIER_II);
            } else if (diamondTier == 2 && upgradeDiamondsCount > 0) {
                setNextEvent(NextEvent.DIAMOND_GENERATOR_TIER_III);
            } else {
                setNextEvent(NextEvent.BEDS_DESTROY);
            }
            for (IGenerator o : getOreGenerators()) {
                if (o.getType() == GeneratorType.EMERALD && o.getBedWarsTeam() == null) {
                    o.upgrade();
                }
            }
        } else if (nextEvent == NextEvent.DIAMOND_GENERATOR_TIER_III && upgradeDiamondsCount == 0) {
            diamondTier = 3;
            sendDiamondsUpgradeMessages();
            if (emeraldTier == 1 && upgradeEmeraldsCount > 0) {
                setNextEvent(NextEvent.EMERALD_GENERATOR_TIER_II);
            } else if (emeraldTier == 2 && upgradeEmeraldsCount > 0) {
                setNextEvent(NextEvent.EMERALD_GENERATOR_TIER_III);
            } else {
                setNextEvent(NextEvent.BEDS_DESTROY);
            }
            for (IGenerator o : getOreGenerators()) {
                if (o.getType() == GeneratorType.DIAMOND && o.getBedWarsTeam() == null) {
                    o.upgrade();
                }
            }
        } else if (nextEvent == NextEvent.BEDS_DESTROY && getPlayingTask().getBedsDestroyCountdown() == 0) {
            setNextEvent(NextEvent.ENDER_DRAGON);
        } else if (nextEvent == NextEvent.ENDER_DRAGON && getPlayingTask().getDragonSpawnCountdown() == 0) {
            setNextEvent(NextEvent.GAME_END);
        }

        debug("---");

        debug(nextEvent.toString());
    }

    /**
     * Get arena by players list.
     */
    public static HashMap<Player, IArena> getArenaByPlayer() {
        return arenaByPlayer;
    }

    /**
     * Get next event.
     */
    public NextEvent getNextEvent() {
        return nextEvent;
    }

    /**
     * Get players count for a group
     */
    public static int getPlayers(@NotNull String group) {
        int i = 0;

        String[] groups = group.split("\\+");
        for (String g : groups) {
            for (IArena a : getArenas()) {
                if (a.getGroup().equalsIgnoreCase(g)) i += a.getPlayers().size();
            }
        }

        return i;
    }

    /**
     * Register join-signs for arena
     */
    private void registerSigns() {
        if (getServerType() != ServerType.BUNGEE) {
            if (BedWars.signs.getYml().get("locations") != null) {
                for (String st : BedWars.signs.getYml().getStringList("locations")) {
                    String[] data = st.split(",");
                    if (data[0].equals(getArenaName())) {
                        Location l;
                        try {
                            l = new Location(Bukkit.getWorld(data[6]), Double.parseDouble(data[1]), Double.parseDouble(data[2]), Double.parseDouble(data[3]));
                        } catch (Exception e) {
                            //noinspection ImplicitArrayToString
                            plugin.getLogger().severe("Could not load sign at: " + data.toString());
                            continue;
                        }
                        addSign(l);
                    }
                }
            }
        }
    }

    /**
     * Get a team by name
     */
    public ITeam getTeam(String name) {
        for (ITeam bwt : getTeams()) {
            if (bwt.getName().equals(name)) return bwt;
        }
        return null;
    }

    /**
     * Get respawn sessions.
     */
    @Override
    public ConcurrentHashMap<Player, Integer> getRespawnSessions() {
        return respawnSessions;
    }

    /**
     * Get invisibility for armor
     */
    public ConcurrentHashMap<Player, Integer> getShowTime() {
        return showTime;
    }

    /**
     * Get instance of the starting task.
     */
    public StartingTask getStartingTask() {
        return startingTask;
    }

    /**
     * Get instance of the playing task.
     */
    public PlayingTask getPlayingTask() {
        return playingTask;
    }

    /**
     * Get instance of the game restarting task.
     */
    public RestartingTask getRestartingTask() {
        return restartingTask;
    }

    /**
     * Get instance of the game announcement task.
     */
    @Override
    public AnnouncementTask getAnnouncementTask() {
        return announcementTask;
    }

    /**
     * Get arena ore generators Ore Generators.
     */
    public List<IGenerator> getOreGenerators() {
        return oreGenerators;
    }

    /**
     * Add a player to the most filled arena.
     * Check if is the party owner first.
     */
    public static boolean joinRandomArena(Player p) {
        List<IArena> arenas = getSorted(getArenas());

        int amount = getPartyManager().hasParty(p) ? (int) getPartyManager().getMembers(p).stream().filter(member -> {
            IArena arena = Arena.getArenaByPlayer(member);
            if (arena == null) {
                return true;
            }
            return arena.isSpectator(member);
        }).count() : 1;

        for (IArena a : arenas) {
            if (a.getPlayers().size() == a.getMaxPlayers()) continue;
            if (a.getMaxPlayers() - a.getPlayers().size() >= amount) {
                if (a.addPlayer(p, false)) break;
            }
        }
        return true;
    }

    public static List<IArena> getSorted(List<IArena> arenas) {
        List<IArena> sorted = new ArrayList<>(arenas);
        Collections.shuffle(sorted); // pre shuffle arena list
        sorted.sort(new Comparator<>() {
            @Override
            public int compare(IArena o1, IArena o2) {
                if (o1.getStatus() == GameState.starting && o2.getStatus() == GameState.starting) {
                    return Integer.compare(o2.getPlayers().size(), o1.getPlayers().size());
                } else if (o1.getStatus() == GameState.starting && o2.getStatus() != GameState.starting) {
                    return -1;
                } else if (o2.getStatus() == GameState.starting && o1.getStatus() != GameState.starting) {
                    return 1;
                } else if (o1.getStatus() == GameState.waiting && o2.getStatus() == GameState.waiting) {
                    return Integer.compare(o2.getPlayers().size(), o1.getPlayers().size());
                } else if (o1.getStatus() == GameState.waiting && o2.getStatus() != GameState.waiting) {
                    return -1;
                } else if (o2.getStatus() == GameState.waiting && o1.getStatus() != GameState.waiting) {
                    return 1;
                } else if (o1.getStatus() == GameState.playing && o2.getStatus() == GameState.playing) {
                    return 0;
                } else if (o1.getStatus() == GameState.playing && o2.getStatus() != GameState.playing) {
                    return -1;
                } else return 1;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof IArena;
            }
        });
        return sorted;
    }

    /**
     * Add a player to the most filled arena from a group.
     */
    public static boolean joinRandomFromGroup(Player p, @NotNull String group) {

        List<IArena> arenas = getSorted(getArenas());

        int amount = getPartyManager().hasParty(p) ? (int) getPartyManager().getMembers(p).stream().filter(member -> {
            IArena arena = Arena.getArenaByPlayer(member);
            if (arena == null) {
                return true;
            }
            return arena.isSpectator(member);
        }).count() : 1;

        String[] groups = group.split("\\+");
        for (IArena a : arenas) {
            if (a.getPlayers().size() == a.getMaxPlayers()) continue;
            for (String g : groups) {
                if (a.getGroup().equalsIgnoreCase(g)) {
                    if (a.getMaxPlayers() - a.getPlayers().size() >= amount) {
                        if (a.addPlayer(p, false)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Get the list of next events to come.
     * Not ordered.
     */
    public List<String> getNextEvents() {
        return new ArrayList<>(nextEvents);
    }

    public List<String> getShopOverrideCategories() {
        return shopOverrideCategories;
    }

    public void addShopOverrideCategory(String shopOverrideCategory) {
        this.shopOverrideCategories.add(shopOverrideCategory);
    }

    /**
     * Get player deaths.
     */
    public int getPlayerDeaths(Player p, boolean finalDeaths) {
        if (finalDeaths) return playerFinalKillDeaths.getOrDefault(p, 0);
        return playerDeaths.getOrDefault(p, 0);
    }

    /**
     * Show upgrade announcement to players.
     * Change diamondTier value first.
     */
    public void sendDiamondsUpgradeMessages() {
        for (Player p : getPlayers()) {
            p.sendMessage(getMsg(p, Messages.GENERATOR_UPGRADE_CHAT_ANNOUNCEMENT).replace("%bw_generator_type%",
                    getMsg(p, Messages.GENERATOR_HOLOGRAM_TYPE_DIAMOND)).replace("%bw_tier%", getMsg(p, (diamondTier == 2 ? Messages.FORMATTING_GENERATOR_TIER2 : Messages.FORMATTING_GENERATOR_TIER3))));
        }
        for (Player p : getSpectators()) {
            p.sendMessage(getMsg(p, Messages.GENERATOR_UPGRADE_CHAT_ANNOUNCEMENT).replace("%bw_generator_type%",
                    getMsg(p, Messages.GENERATOR_HOLOGRAM_TYPE_DIAMOND)).replace("%bw_tier%", getMsg(p, (diamondTier == 2 ? Messages.FORMATTING_GENERATOR_TIER2 : Messages.FORMATTING_GENERATOR_TIER3))));
        }
    }

    /**
     * Show upgrade announcement to players.
     * Change emeraldTier value first.
     */
    public void sendEmeraldsUpgradeMessages() {
        for (Player p : getPlayers()) {
            p.sendMessage(getMsg(p, Messages.GENERATOR_UPGRADE_CHAT_ANNOUNCEMENT).replace("%bw_generator_type%",
                    getMsg(p, Messages.GENERATOR_HOLOGRAM_TYPE_EMERALD)).replace("%bw_tier%", getMsg(p, (emeraldTier == 2 ? Messages.FORMATTING_GENERATOR_TIER2 : Messages.FORMATTING_GENERATOR_TIER3))));
        }
        for (Player p : getSpectators()) {
            p.sendMessage(getMsg(p, Messages.GENERATOR_UPGRADE_CHAT_ANNOUNCEMENT).replace("%bw_generator_type%",
                    getMsg(p, Messages.GENERATOR_HOLOGRAM_TYPE_EMERALD)).replace("%bw_tier%", getMsg(p, (emeraldTier == 2 ? Messages.FORMATTING_GENERATOR_TIER2 : Messages.FORMATTING_GENERATOR_TIER3))));
        }
    }


    public static int getGamesBeforeRestart() {
        return gamesBeforeRestart;
    }

    public static void setGamesBeforeRestart(int gamesBeforeRestart) {
        Arena.gamesBeforeRestart = gamesBeforeRestart;
    }

    public List<Region> getRegionsList() {
        return regionsList;
    }

    public LinkedList<Vector> getPlaced() {
        return placed;
    }

    public static LinkedList<IArena> getEnableQueue() {
        return enableQueue;
    }

    private final Map<UUID, Long> fireballCooldowns = new HashMap<>();

    public Map<UUID, Long> getFireballCooldowns() {
        return fireballCooldowns;
    }

    public void destroyData() {
        destroyReJoins();
        if (worldName != null) arenaByIdentifier.remove(worldName);
        arenas.remove(this);
        for (ReJoinTask rjt : ReJoinTask.getReJoinTasks()) {
            if (rjt.getArena() == this) {
                rjt.destroy();
            }
        }
        for (Despawnable despawnable : new ArrayList<>(BedWars.nms.getDespawnablesList().values())) {
            if (despawnable.getTeam().getArena() == this) {
                despawnable.destroy();
            }
        }
        for (ServerPlaceholder placeholder : serverPlaceholders) {
            TabAPI.getInstance().getPlaceholderManager().unregisterPlaceholder(placeholder);
        }
        if (TabAPI.getInstance().getBossBarManager() != null) {
            for (BossBar bossBar : dragonBossbars) {
                bossBar.getPlayers().forEach(bossBar::removePlayer);
            }
            dragonBossbars = null;
        }
        arenaByName.remove(arenaName);
        arenaByPlayer.entrySet().removeIf(entry -> entry.getValue() == this);
        players = null;
        spectators = null;
        signs = null;
        yml = null;
        cm = null;
        world = null;
        for (IGenerator og : oreGenerators) {
            og.destroyData();
        }
        isOnABase.entrySet().removeIf(entry -> entry.getValue().getArena().equals(this));
        for (ITeam bwt : teams) {
            bwt.destroyData();
        }
        playerLocation.entrySet().removeIf(e -> Objects.requireNonNull(e.getValue().getWorld()).getName().equalsIgnoreCase(worldName));
        teams = null;
        placed = null;
        nextEvents = null;
        regionsList = null;
        respawnSessions = null;
        showTime = null;
        playerKills = null;
        playerBedsDestroyed = null;
        playerFinalKills = null;
        playerDeaths = null;
        playerFinalKillDeaths = null;
        startingTask = null;
        playingTask = null;
        restartingTask = null;
        oreGenerators = null;
        perMinuteTask = null;
        moneyperMinuteTask = null;
        fireballCooldowns.clear();

        // Cleanup remote data.
        if (BedWars.getRedisConnection() != null) BedWars.getRedisConnection().cleanupRedisEntry(this);
    }

    /**
     * Remove an arena from the enable queue.
     */
    public static void removeFromEnableQueue(IArena a) {
        enableQueue.remove(a);
        if (!enableQueue.isEmpty()) {
            BedWars.getAPI().getRestoreAdapter().onEnable(enableQueue.get(0));
            plugin.getLogger().info("Loading arena: " + enableQueue.get(0).getWorldName());
        }
    }

    public static void addToEnableQueue(IArena a) {
        enableQueue.add(a);
        plugin.getLogger().info("Arena " + a.getWorldName() + " was added to the enable queue.");
        if (enableQueue.size() == 1) {
            BedWars.getAPI().getRestoreAdapter().onEnable(a);
            plugin.getLogger().info("Loading arena: " + a.getWorldName());
        }
    }

    public int getUpgradeDiamondsCount() {
        return upgradeDiamondsCount;
    }

    public int getUpgradeEmeraldsCount() {
        return upgradeEmeraldsCount;
    }

    public void setAllowSpectate(boolean allowSpectate) {
        this.allowSpectate = allowSpectate;
    }

    public boolean isAllowSpectate() {
        return allowSpectate;
    }

    public String getWorldName() {
        return worldName;
    }

    @Override
    public int getRenderDistance() {
        return renderDistance;
    }

    @Override
    public Location getReSpawnLocation() {
        return respawnLocation;
    }

    @Override
    public Location getSpectatorLocation() {
        return spectatorLocation;
    }

    @Override
    public void setAllowMapBreak(boolean allowMapBreak) {
        this.allowMapBreak = allowMapBreak;
    }

    @Override
    public boolean isTeamBed(Location location) {
        return null != getBedsTeam(location);
    }

    @Override
    public @Nullable ITeam getBedsTeam(@NotNull Location location) {
        if (!location.getWorld().getName().equals(this.worldName)) {
            throw new RuntimeException("Given location is not on this game world.");
        }

        if (!nms.isBed(location.getBlock().getType())) {
            return null;
        }

        for (ITeam team : this.teams) {
            if (team.isBed(location)) {
                return team;
            }
        }
        return null;
    }

    @Override
    public boolean isAllowEnderDragonDestroy() {
        return enderDragonDestory;
    }

    @Override
    public void setAllowEnderDragonDestroy(boolean allowDestory) {
        this.enderDragonDestory = allowDestory;
    }

    @Override
    public int getMagicMilkTime() {
        return magicMilkTime;
    }

    @Override
    public boolean isMapBreakable() {
        return allowMapBreak;
    }

    @Override
    public Location getWaitingLocation() {
        return waitingLocation;
    }

    @Override
    public boolean startReSpawnSession(Player player, int seconds) {
        if (respawnSessions.get(player) != null) {
            return false;
        }
        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null) {
            return false;
        }
        if (!arena.isPlayer(player)) {
            return false;
        }
        player.getInventory().clear();
        if (seconds > 1) {
            // hide to others
            for (Player playing : arena.getPlayers()) {
                if (playing.equals(player)) continue;
                BedWars.nms.spigotHidePlayer(player, playing);
            }
            PaperSupport.teleportC(player, getReSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
            player.setAllowFlight(true);
            player.setFlying(true);

            respawnSessions.put(player, seconds);
            Bukkit.getScheduler().runTaskLater(BedWars.plugin, () -> {
                player.setAllowFlight(true);
                player.setFlying(true);
                player.setFireTicks(0);

                nms.setCollide(player, this, false);
                for (Player invisible : getShowTime().keySet()) {
                    BedWars.nms.hideArmor(invisible, player);
                }
            }, 5L);
        } else {
            ITeam team = getTeam(player);
            team.respawnMember(player);
        }
        return true;
    }

    @Override
    public boolean isReSpawning(Player player) {
        return respawnSessions.containsKey(player);
    }

    // used for auto scale conditions
    public static boolean canAutoScale(String arenaName) {
        if (!autoscale) return true;

        if (Arena.getArenas().isEmpty()) return true;

        for (IArena ar : Arena.getEnableQueue()) {
            if (ar.getArenaName().equalsIgnoreCase(arenaName)) return false;
        }

        if (Arena.getGamesBeforeRestart() != -1 && Arena.getArenas().size() >= Arena.getGamesBeforeRestart())
            return false;

        int activeClones = 0;
        for (IArena ar : Arena.getArenas()) {
            if (ar.getArenaName().equalsIgnoreCase(arenaName)) {
                // clone this arena only if there aren't available arena of the same kind
                if (ar.getStatus() == GameState.waiting || ar.getStatus() == GameState.starting) return false;
            }
            // count active clones
            if (ar.getArenaName().equals(arenaName)) {
                activeClones++;
            }
        }

        // check amount of active clones
        return config.getInt(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_AUTO_SCALE_LIMIT) > activeClones;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj instanceof IArena) {
            return ((IArena) obj).getWorldName().equals(this.getWorldName());
        }
        return false;
    }

    private void destroyReJoins() {
        List<ReJoin> reJoins = new ArrayList<>(ReJoin.getReJoinList());
        for (ReJoin reJoin : reJoins) {
            if (reJoin.getArena() == this) {
                reJoin.destroy(true);
            }
        }
    }

    @Override
    public boolean isProtected(Location location) {
        return Misc.isBuildProtected(location, this);
    }

    @Override
    public void abandonGame(Player player) {
        if (player == null) return;

        //this.playerKills.remove(player.getName());
        this.playerBedsDestroyed.remove(player);
        this.playerFinalKills.remove(player);
        this.playerDeaths.remove(player);
        this.playerFinalKillDeaths.remove(player);

        ITeam team = getTeams().stream().filter(team1 -> team1.wasMember(player.getUniqueId())).findFirst().orElse(null);
        if (team != null) {
            //noinspection deprecation
            team.getMembersCache().removeIf(cachedPlayer -> cachedPlayer.getUniqueId().equals(player.getUniqueId()));
            ReJoin rejoin = ReJoin.getPlayer(player);
            if (rejoin != null) {
                rejoin.destroy(team.getMembers().isEmpty());
            }
        }
    }

    @Override
    public int getYKillHeight() {
        return yKillHeight;
    }

    @Override
    public Instant getStartTime() {
        return startTime;
    }

    @Override
    public ITeamAssigner getTeamAssigner() {
        return teamAssigner;
    }

    @Override
    public void setTeamAssigner(ITeamAssigner teamAssigner) {
        if (teamAssigner == null) {
            this.teamAssigner = new TeamAssigner();
            plugin.getLogger().info("Using Default team assigner on arena: " + this.getArenaName());
        } else {
            this.teamAssigner = teamAssigner;
            plugin.getLogger().warning("Using " + teamAssigner.getClass().getSimpleName() + " team assigner on arena: " + this.getArenaName());
        }
    }

    @Override
    public List<BossBar> getDragonBossbars() {
        return dragonBossbars;
    }

    public boolean isAllowMapBreak() {
        return allowMapBreak;
    }

    /**
     * Remove player from world.
     * Contains fall-backs.
     */
    private void sendToMainLobby(Player player) {
        if (BedWars.getServerType() == ServerType.SHARED) {
            Location loc = playerLocation.get(player);
            if (loc == null) {
                PaperSupport.teleportC(player, Bukkit.getWorlds().get(0).getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                plugin.getLogger().log(Level.SEVERE, player.getName() + " was teleported to the main world because lobby location is not set!");
            } else {
                player.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
        } else if (BedWars.getServerType() == ServerType.MULTIARENA) {
            if (BedWars.getLobbyWorld().isEmpty()) {
                PaperSupport.teleportC(player, Bukkit.getWorlds().get(0).getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                plugin.getLogger().log(Level.SEVERE, player.getName() + " was teleported to the main world because lobby location is not set!");
            } else {
                PaperSupport.teleportC(player, config.getConfigLoc("lobbyLoc"), PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
        }
    }

    /**
     * Dragon bossbar custom names
     * Use only for 1.8 servers as they don't support BossBar packets
     */
    public void set1_8BossBarName(ITeam team, EnderDragon dragon) {
        for (Player player : team.getArena().getPlayers()) {
            String name = Language.getMsg(player, Messages.FORMATTING_BOSSBAR_DRAGON).replace("%bw_team%", team.getColor().chat() + team.getName()).replace("%bw_team_color%", String.valueOf(team.getColor().chat())).replace("%bw_team_name%", team.getDisplayName(getPlayerLanguage(player))).replace("%bw_team_letter%", String.valueOf(team.getName().length() != 0 ? team.getName().charAt(0) : ""));
            dragon.setCustomName(name);
        }
    }

    /**
     * TAB V4 Custom Bossbar naming
     * Can be used on all versions but will show double bossbars on 1.8 servers as vanilla bossbar is client sided
     */
    public void createTABTeamDragonBossBar(ITeam team, int dragonNumber) {
        if (TabAPI.getInstance().getBossBarManager() == null) {
            BedWars.plugin.getLogger().warning("BossBar is disabled in TAB config! Please enable it there.\n Make sure to remove the ServerInfo default config if you want to use dragon bossbars");
            return;
        }
        String dragonPlaceholderName = "%bw_" + team.getArena().getWorldName() + "_" + team.getName() + "+" + dragonNumber + "%";
        ServerPlaceholder dragonPlaceholder = TabAPI.getInstance().getPlaceholderManager().registerServerPlaceholder(dragonPlaceholderName, 500, () -> team.getDragons().get(dragonNumber).getHealth() / team.getDragons().get(dragonNumber).getMaxHealth() * 100);
        serverPlaceholders.add(dragonPlaceholder);
        for (Player player : team.getArena().getPlayers()) {
            String name = Language.getMsg(player, Messages.FORMATTING_BOSSBAR_DRAGON).replace("%bw_team%", team.getColor().chat() + team.getName()).replace("%bw_team_color%", String.valueOf(team.getColor().chat())).replace("%bw_team_name%", team.getDisplayName(getPlayerLanguage(player))).replace("%bw_team_letter%", String.valueOf(team.getName().length() != 0 ? team.getName().charAt(0) : ""));
            BossBar bb = TabAPI.getInstance().getBossBarManager().createBossBar(name, dragonPlaceholderName, String.valueOf(team.getColor()), "PROGRESS");
            bb.addPlayer(Objects.requireNonNull(TabAPI.getInstance().getPlayer(player.getUniqueId())));
            dragonBossbars.add(bb);
        }
    }


}
