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

package com.tomkeuper.bedwars;

import com.andrei1058.vipfeatures.api.IVipFeatures;
import com.andrei1058.vipfeatures.api.MiniGameAlreadyRegistered;
import com.tomkeuper.bedwars.addon.AddonManager;
import com.tomkeuper.bedwars.api.addon.Addon;
import com.tomkeuper.bedwars.api.addon.IAddonManager;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.chat.IChat;
import com.tomkeuper.bedwars.api.configuration.ConfigManager;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.api.database.IDatabase;
import com.tomkeuper.bedwars.api.economy.IEconomy;
import com.tomkeuper.bedwars.api.items.handlers.IPermanentItem;
import com.tomkeuper.bedwars.api.items.handlers.IPermanentItemHandler;
import com.tomkeuper.bedwars.handlers.items.LobbyItem;
import com.tomkeuper.bedwars.api.hologram.IHologramManager;
import com.tomkeuper.bedwars.api.language.Language;
import com.tomkeuper.bedwars.api.levels.Level;
import com.tomkeuper.bedwars.api.party.Party;
import com.tomkeuper.bedwars.api.server.RestoreAdapter;
import com.tomkeuper.bedwars.api.server.ServerType;
import com.tomkeuper.bedwars.api.server.VersionSupport;
import com.tomkeuper.bedwars.arena.Arena;
import com.tomkeuper.bedwars.arena.ArenaManager;
import com.tomkeuper.bedwars.arena.VoidChunkGenerator;
import com.tomkeuper.bedwars.arena.despawnables.TargetListener;
import com.tomkeuper.bedwars.arena.feature.AntiDropFeature;
import com.tomkeuper.bedwars.arena.feature.GenSplitFeature;
import com.tomkeuper.bedwars.arena.feature.SpoilPlayerTNTFeature;
import com.tomkeuper.bedwars.arena.spectator.SpectatorListeners;
import com.tomkeuper.bedwars.arena.tasks.OneTick;
import com.tomkeuper.bedwars.arena.tasks.Refresh;
import com.tomkeuper.bedwars.arena.upgrades.BaseListener;
import com.tomkeuper.bedwars.arena.upgrades.HealPoolListener;
import com.tomkeuper.bedwars.commands.bedwars.MainCommand;
import com.tomkeuper.bedwars.commands.leave.LeaveCommand;
import com.tomkeuper.bedwars.commands.party.PartyCommand;
import com.tomkeuper.bedwars.commands.rejoin.RejoinCommand;
import com.tomkeuper.bedwars.commands.shout.ShoutCommand;
import com.tomkeuper.bedwars.configuration.*;
import com.tomkeuper.bedwars.database.H2;
import com.tomkeuper.bedwars.database.MySQL;
import com.tomkeuper.bedwars.database.SQLite;
import com.tomkeuper.bedwars.halloween.HalloweenSpecial;
import com.tomkeuper.bedwars.hologram.HologramManager;
import com.tomkeuper.bedwars.handlers.items.PreGameItem;
import com.tomkeuper.bedwars.handlers.items.SpectatorItem;
import com.tomkeuper.bedwars.handlers.main.CommandItemHandler;
import com.tomkeuper.bedwars.handlers.main.LeaveItemHandler;
import com.tomkeuper.bedwars.handlers.main.StatsItemHandler;
import com.tomkeuper.bedwars.language.*;
import com.tomkeuper.bedwars.levels.internal.InternalLevel;
import com.tomkeuper.bedwars.levels.internal.LevelListeners;
import com.tomkeuper.bedwars.listeners.*;
import com.tomkeuper.bedwars.listeners.arenaselector.ArenaSelectorListener;
import com.tomkeuper.bedwars.listeners.blockstatus.BlockStatusListener;
import com.tomkeuper.bedwars.listeners.chat.ChatAFK;
import com.tomkeuper.bedwars.listeners.chat.ChatFormatting;
import com.tomkeuper.bedwars.listeners.joinhandler.*;
import com.tomkeuper.bedwars.connectionmanager.LoadedUsersCleaner;
import com.tomkeuper.bedwars.connectionmanager.redis.RedisArenaListeners;
import com.tomkeuper.bedwars.connectionmanager.redis.RedisConnection;
import com.tomkeuper.bedwars.maprestore.internal.InternalAdapter;
import com.tomkeuper.bedwars.money.internal.MoneyListeners;
import com.tomkeuper.bedwars.shop.OverrideShop;
import com.tomkeuper.bedwars.shop.ShopCache;
import com.tomkeuper.bedwars.shop.ShopManager;
import com.tomkeuper.bedwars.shop.quickbuy.PlayerQuickBuyCache;
import com.tomkeuper.bedwars.sidebar.BoardManager;
import com.tomkeuper.bedwars.stats.StatsManager;
import com.tomkeuper.bedwars.support.citizens.CitizensListener;
import com.tomkeuper.bedwars.support.citizens.JoinNPC;
import com.tomkeuper.bedwars.support.papi.PAPISupport;
import com.tomkeuper.bedwars.support.papi.SupportPAPI;
import com.tomkeuper.bedwars.support.party.*;
import com.tomkeuper.bedwars.support.vault.NoChat;
import com.tomkeuper.bedwars.support.vault.NoEconomy;
import com.tomkeuper.bedwars.support.vault.WithChat;
import com.tomkeuper.bedwars.support.vault.WithEconomy;
import com.tomkeuper.bedwars.support.vipfeatures.VipFeatures;
import com.tomkeuper.bedwars.support.vipfeatures.VipListeners;
import com.tomkeuper.bedwars.upgrades.UpgradesManager;
import com.tomkeuper.bedwars.utils.SlimLogger;
import de.dytanic.cloudnet.wrapper.Wrapper;
import io.github.slimjar.app.builder.ApplicationBuilder;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.nametag.UnlimitedNameTagManager;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@SuppressWarnings("WeakerAccess")
public class BedWars extends JavaPlugin {

    private static ServerType serverType = ServerType.MULTIARENA;
    public static boolean debug = true, autoscale = false, isPaper = false, tabUnlimitedNameTagSupport = false;
    public static int hologramUpdateDistance = 50; // DEFAULT DISTANCE (update distance measured in blocks)
    public static String mainCmd = "bw", link = "https://polymart.org/resource/bedwars2023.5702";
    public static ConfigManager signs, generators;
    public static MainConfig config;
    public static ShopManager shop;
    private static UpgradesManager upgradesManager;
    public static PlayerQuickBuyCache playerQuickBuyCache;
    public static ShopCache shopCache;
    public static StatsManager statsManager;
    public static BedWars plugin;
    public static VersionSupport nms;

    private static Party partyManager = new NoParty();
    private static IChat chat = new NoChat();
    protected static Level level;
    private static IEconomy economy;
    private static String nmsVersion = Bukkit.getServer().getClass().getName().split("\\.")[3];
    private static final String minecraftVersion = Bukkit.getServer().getBukkitVersion().split("-")[0];
    private static String lobbyWorld = "";
    private static boolean shuttingDown = false;

    public static ArenaManager arenaManager = new ArenaManager();
    public static IAddonManager addonManager = new AddonManager();
    public static IHologramManager hologramManager = new HologramManager();

    // BedWars Items;
    private static Collection<IPermanentItem> lobbyItems = new ArrayList<>();
    private static Collection<IPermanentItem> spectatorItems = new ArrayList<>();
    private static Collection<IPermanentItem> preGameItems = new ArrayList<>();
    private static Map<String, IPermanentItemHandler> itemHandlers = new HashMap<>();

    //remote database
    private static IDatabase remoteDatabase;

    private static RedisConnection redisConnection;

    private boolean serverSoftwareSupport = true, papiSupportLoaded = false, vaultEconomyLoaded = false, vaultChatLoaded = false;

    private static com.tomkeuper.bedwars.api.BedWars api;

    @Override
    public void onLoad() {

        //Spigot support
        try {
            Class.forName("org.spigotmc.SpigotConfig");
        } catch (Exception ignored) {
            this.getLogger().severe("I can't run on your server software. Please check:");
            this.getLogger().severe("https://wiki.tomkeuper.com/docs/BedWars2023/compatibility");
            serverSoftwareSupport = false;
            return;
        }

        try {
            Path downloadPath = Paths.get(getDataFolder().getPath() + File.separator + "libs");
            ApplicationBuilder.appending("BedWars2023")
                    .logger(new SlimLogger(this))
                    .downloadDirectoryPath(downloadPath)
                    .mirrorSelector((a, b) -> a)
                    .build();
        } catch (IOException | ReflectiveOperationException | URISyntaxException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            isPaper = true;
        } catch (ClassNotFoundException e) {
            isPaper = false;
        }

        plugin = this;

        /* Load version support */
        //noinspection rawtypes
        switch (minecraftVersion){
            case "1.20.4":
                nmsVersion = "v1_20_R3";
                break;
            case "1.20.5":
                nmsVersion = "v1_20_R4";
                break;
            case "1.20.6":
                nmsVersion = "v1_20_R4";
                break;
            case "1.21":
            case "1.21.1":
                nmsVersion = "v1_21_R1";
                break;
            default:
                break;
        }
        Class supp;

        try {
            supp = Class.forName("com.tomkeuper.bedwars.support.version." + nmsVersion + "." + nmsVersion);
        } catch (ClassNotFoundException e) {
            serverSoftwareSupport = false;
            this.getLogger().severe("I can't run on your version: " + minecraftVersion);
            return;
        }

        api = new API();
        Bukkit.getServicesManager().register(com.tomkeuper.bedwars.api.BedWars.class, api, this, ServicePriority.Highest);

        try {
            //noinspection unchecked
            nms = (VersionSupport) supp.getConstructor(Class.forName("org.bukkit.plugin.Plugin"), String.class).newInstance(this, nmsVersion);
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                 ClassNotFoundException e) {
            e.printStackTrace();
            serverSoftwareSupport = false;
            this.getLogger().severe("Could not load support for server version: " + minecraftVersion);
            return;
        }

        this.getLogger().info("Loading support for paper/spigot: " + minecraftVersion);

        // Setup languages
        new English();
        new Romanian();
        new Italian();
        new Polish();
        new Spanish();
        new Russian();
        new Bangla();
        new Persian();
        new Hindi();
        new Indonesia();
        new Portuguese();
        new SimplifiedChinese();
        new Turkish();

        config = new MainConfig(this, "config");
        hologramUpdateDistance = config.getInt(ConfigPath.GENERAL_CONFIGURATION_HOLOGRAM_UPDATE_DISTANCE);

        generators = new GeneratorsConfig(this, "generators", this.getDataFolder().getPath());
        // Initialize signs config after the main config
        if (getServerType() != ServerType.BUNGEE) {
            signs = new SignsConfig(this, "signs", this.getDataFolder().getPath());
        }
    }

    @Override
    public void onEnable() {
        if (!serverSoftwareSupport) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        nms.registerVersionListeners();

        if (Bukkit.getPluginManager().getPlugin("Multiverse-Core") != null){
            plugin.getLogger().warning("-=-=-=-=-=-=-=- Multiverse has been found! -=-=-=-=-=-=-=-");
            plugin.getLogger().warning("");
            plugin.getLogger().warning(" Unless properly configured, multiverse will cause issues!");
            plugin.getLogger().warning("");
            plugin.getLogger().warning("      Make sure that MV does NOT touch any BW maps.");
            plugin.getLogger().warning("");
            plugin.getLogger().warning("_________________________________________________________");
        }

        if (!this.handleWorldAdapter()) {
            api.setRestoreAdapter(new InternalAdapter(this));
        }

        /* Register commands */
        nms.registerCommand(mainCmd, new MainCommand(mainCmd));

        // newer versions do not seem to like delayed registration of commands
        if (nms.getVersion() >= 9) {
            this.registerDelayedCommands();
        } else {
            Bukkit.getScheduler().runTaskLater(this, this::registerDelayedCommands, 20L);
        }

        /* Setup plugin messaging channel */
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        /* Check if lobby location is set. Required for non Bungee servers */
        if (config.getLobbyWorldName().isEmpty() && serverType != ServerType.BUNGEE) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Lobby location is not set!");
        }

        /* Check if CloudNet support is requested (replaces server-id name the CloudNet service ID) */
        if (config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_CLOUDNET_SUPPORT) && serverType == ServerType.BUNGEE) {
            plugin.getLogger().log(java.util.logging.Level.INFO, "CloudNet Service ID = " + Wrapper.getInstance().getServiceId().getName());
            config.set(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_SERVER_ID, Wrapper.getInstance().getServiceId().getName());
        }

        /* Load lobby world if not main level
         * when the server finishes loading. */
        if (getServerType() == ServerType.MULTIARENA)
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (!config.getLobbyWorldName().isEmpty()) {
                    if (Bukkit.getWorld(config.getLobbyWorldName()) == null && new File(Bukkit.getWorldContainer(), config.getLobbyWorldName() + "/level.dat").exists()) {
                        if (!config.getLobbyWorldName().equalsIgnoreCase(Bukkit.getServer().getWorlds().get(0).getName())) {
                            Bukkit.getScheduler().runTaskLater(this, () -> {
                                Bukkit.createWorld(new WorldCreator(config.getLobbyWorldName()));

                                if (Bukkit.getWorld(config.getLobbyWorldName()) != null) {
                                    Bukkit.getScheduler().runTaskLater(plugin, () -> Objects.requireNonNull(Bukkit.getWorld(config.getLobbyWorldName()))
                                            .getEntities().stream().filter(e -> e instanceof Monster).forEach(Entity::remove), 20L);
                                }
                            }, 100L);
                        }
                    }
                    Location l = config.getConfigLoc("lobbyLoc");
                    if (l != null) {
                        World w = Bukkit.getWorld(config.getLobbyWorldName());
                        if (w != null) {
                            w.setSpawnLocation(l.getBlockX(), l.getBlockY(), l.getBlockZ());
                        }
                    }
                }
            }, 1L);

        // Register events
        registerEvents(new EnderPearlLanded(), new QuitAndTeleportListener(), new BreakPlace(), new DamageDeathMove(), new Inventory(), new Interact(), new RefreshGUI(), new HungerWeatherSpawn(), new CmdProcess(),
                new FireballListener(), new EggBridge(), new SpectatorListeners(), new BaseListener(), new TargetListener(), new LangListener(), new Warnings(this), new ChatAFK(), new GameEndListener());

        if (config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_HEAL_POOL_ENABLE)) {
            registerEvents(new HealPoolListener());
        }

        if (getServerType() == ServerType.BUNGEE) {
            if (autoscale) {
                redisConnection = new RedisConnection();
                registerEvents(new RedisArenaListeners(redisConnection));
                if (!redisConnection.connect()){
                    getLogger().severe("Could not connect to redis server! Please check the redis configuration and make sure the redis server is running! Disabling the plugin...");
                    setEnabled(false);
                    return;
                }
                registerEvents(new AutoscaleListener(), new JoinListenerBungee());
                Bukkit.getScheduler().runTaskTimerAsynchronously(this, new LoadedUsersCleaner(), 60L, 60L);
            } else {
                registerEvents(new ServerPingListener(), new JoinListenerBungeeLegacy());
            }
        } else if (getServerType() == ServerType.MULTIARENA || getServerType() == ServerType.SHARED) {
            registerEvents(new ArenaSelectorListener(), new BlockStatusListener());
            if (getServerType() == ServerType.MULTIARENA) {
                registerEvents(new JoinListenerMultiArena());
            } else {
                registerEvents(new JoinListenerShared());
            }
        }

        registerEvents(new WorldLoadListener());

        if (!(getServerType() == ServerType.BUNGEE && autoscale)) {
            registerEvents(new JoinHandlerCommon());
        }

        // Register setup-holograms fix
        registerEvents(new ChunkLoad());

        registerEvents(new InvisibilityPotionListener());

        statsManager = new StatsManager();

        /* Party support */
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (config.getYml().getBoolean(ConfigPath.GENERAL_CONFIGURATION_ALLOW_PARTIES)) {

                if (getServer().getPluginManager().isPluginEnabled("Parties")) {
                    getLogger().info("Hook into Parties (by AlessioDP) support!");
                    partyManager = new PartiesAdapter();
                } else if (Bukkit.getServer().getPluginManager().isPluginEnabled("PartyAndFriends")) {
                    getLogger().info("Hook into Party and Friends for Spigot (by Simonsator) support!");
                    partyManager = new PAF();
                } else if (Bukkit.getServer().getPluginManager().isPluginEnabled("Spigot-Party-API-PAF")) {
                    getLogger().info("Hook into Spigot Party API for Party and Friends Extended (by Simonsator) support!");
                    partyManager = new PAFBungeecordRedisApi();
                }

                if (partyManager instanceof NoParty) {
                    partyManager = new Internal();
                    getLogger().info("Loading internal Party system. /party");
                }
            } else {
                partyManager = new NoParty();
            }
        }, 10L);

        /* Levels support */
        setLevelAdapter(new InternalLevel());

        /* Register tasks */
        Bukkit.getScheduler().runTaskTimer(this, new Refresh(), 20L, 20L);
        //new Refresh().runTaskTimer(this, 20L, 20L);

        if (config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_PERFORMANCE_ROTATE_GEN)) {
            //new OneTick().runTaskTimer(this, 120, 1);
            Bukkit.getScheduler().runTaskTimer(this, new OneTick(), 120, 1);
        }

        /* Register NMS entities */
        nms.registerEntities();

        /* Database support */
        if (config.getString(ConfigPath.GENERAL_CONFIGURATION_DATABASE_TYPE).equalsIgnoreCase("mysql")) {
            MySQL mySQL = new MySQL();
            long time = System.currentTimeMillis();
            if (!mySQL.connect()) {
                this.getLogger().severe("Could not connect to database! Please verify your credentials and make sure that the server IP is whitelisted in MySQL.");
                remoteDatabase = new SQLite();
            } else {
                remoteDatabase = mySQL;
            }
            if (System.currentTimeMillis() - time >= 5000) {
                this.getLogger().severe("It took " + ((System.currentTimeMillis() - time) / 1000) + " ms to establish a database connection!\n" +
                        "Using this remote connection is not recommended!");
            }
            remoteDatabase.init();
        } else if (config.getString(ConfigPath.GENERAL_CONFIGURATION_DATABASE_TYPE).equalsIgnoreCase("sqlite")){
            remoteDatabase = new SQLite();
            remoteDatabase.init();
        } else if (config.getString(ConfigPath.GENERAL_CONFIGURATION_DATABASE_TYPE).equalsIgnoreCase("h2")){
            remoteDatabase = new H2();
            remoteDatabase.init();
        }

        /* Citizens support */
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (this.getServer().getPluginManager().getPlugin("Citizens") != null) {
                JoinNPC.setCitizensSupport(true);
                getLogger().info("Hook into Citizens support. /bw npc");
                registerEvents(new CitizensListener());
            }

            //spawn NPCs
            try {
                JoinNPC.spawnNPCs();
            } catch (Exception e) {
                this.getLogger().severe("Could not spawn CmdJoin NPCs. Make sure you have right version of Citizens for your server!");
                JoinNPC.setCitizensSupport(false);
            }
        }, 40L);

        /* Save messages for stats gui items if custom items added, for each language */
        Language.setupCustomStatsMessages();


        /* PlaceholderAPI Support */
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPISupport().register();
            SupportPAPI.setSupportPAPI(new SupportPAPI.withPAPI());
            papiSupportLoaded = true;
        }
        /*
         * Vault support
         * The task is to initialize after all plugins have loaded,
         *  to make sure any economy/chat plugins have been loaded and registered.
         */
        Bukkit.getScheduler().runTask(this, () -> {
            if (this.getServer().getPluginManager().getPlugin("Vault") != null) {
                try {
                    //noinspection rawtypes
                    RegisteredServiceProvider rsp = this.getServer().getServicesManager().getRegistration(net.milkbowl.vault.chat.Chat.class);
                    if (rsp != null) {
                        WithChat.setChat((net.milkbowl.vault.chat.Chat) rsp.getProvider());
                        vaultChatLoaded = true;
                        chat = new WithChat();
                    } else {
                        plugin.getLogger().info("Vault found, but no chat provider!");
                        chat = new NoChat();
                    }
                } catch (Exception var2_2) {
                    chat = new NoChat();
                }
                try {
                    registerEvents(new MoneyListeners());
                    RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = this.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                    if (rsp != null) {
                        WithEconomy.setEconomy(rsp.getProvider());
                        vaultEconomyLoaded = true;
                        economy = new WithEconomy();
                    } else {
                        plugin.getLogger().info("Vault found, but no economy provider!");
                        economy = new NoEconomy();
                    }
                } catch (Exception var2_2) {
                    economy = new NoEconomy();
                }
            } else {
                chat = new NoChat();
                economy = new NoEconomy();
            }
        });

        /* Chat support */
        if (config.getBoolean(ConfigPath.GENERAL_CHAT_FORMATTING)) {
            registerEvents(new ChatFormatting());
        }

        /* Protect glass walls from tnt explosion */
        nms.registerTntWhitelist(
                (float) config.getDouble(ConfigPath.GENERAL_TNT_PROTECTION_END_STONE_BLAST),
                (float) config.getDouble(ConfigPath.GENERAL_TNT_PROTECTION_GLASS_BLAST)
        );

        /* Prevent issues on reload */
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.kickPlayer("BedWars2023 was RELOADED! (do not reload plugins)");
        }

        /* Load sounds configuration */
        Sounds.init();

        /* Initialize shop */
        shop = new ShopManager();
        shop.loadShop();

        /* Load shop overrides */
        File dir = new File(BedWars.plugin.getDataFolder(), "/Shops");
        if (dir.exists()) {
            List<File> files = new ArrayList<>();
            File[] fls = dir.listFiles();
            for (File fl : Objects.requireNonNull(fls)) {
                if (fl.isFile()) {
                    if (fl.getName().endsWith(".yml")) {
                        files.add(fl);
                    }
                }
            }
            for (File file : files) {
                if (file.getName().equalsIgnoreCase("default-shop.yml")) continue;
                new OverrideShop(shop, file.getName().replace(".yml", ""));
            }
        }

        registerItemHandlers(new StatsItemHandler("stats", this, api), new CommandItemHandler("command", this, api), new LeaveItemHandler("leave", this, api));

        /* Initialize instances */
        shopCache = new ShopCache();
        playerQuickBuyCache = new PlayerQuickBuyCache();

        //Leave this code at the end of the enable method
        for (Language l : Language.getLanguages()) {
            l.setupUnSetCategories();
            Language.addDefaultMessagesCommandItems(l);
        }

        LevelsConfig.init();

        /* Load Money Configuration */
        MoneyConfig.init();

        // bStats metrics
        Metrics metrics = new Metrics(this, 18317);
        metrics.addCustomChart(new SimplePie("server_type", () -> getServerType().toString()));
        metrics.addCustomChart(new SimplePie("default_language", () -> Language.getDefaultLanguage().getIso()));
        metrics.addCustomChart(new SimplePie("auto_scale", () -> String.valueOf(autoscale)));
        metrics.addCustomChart(new SimplePie("party_adapter", () -> partyManager.getClass().getSimpleName()));
        metrics.addCustomChart(new SimplePie("chat_adapter", () -> chat.getClass().getSimpleName()));
        metrics.addCustomChart(new SimplePie("level_adapter", () -> getLevelSupport().getClass().getSimpleName()));
        metrics.addCustomChart(new SimplePie("db_adapter", () -> getRemoteDatabase().getClass().getSimpleName()));
        metrics.addCustomChart(new SimplePie("map_adapter", () -> String.valueOf(getAPI().getRestoreAdapter().getOwner().getName())));

        if (Bukkit.getPluginManager().getPlugin("VipFeatures") != null) {
            try {
                IVipFeatures vf = Bukkit.getServicesManager().getRegistration(IVipFeatures.class).getProvider();
                vf.registerMiniGame(new VipFeatures(this));
                registerEvents(new VipListeners(vf));
                getLogger().log(java.util.logging.Level.INFO, "Hook into VipFeatures support.");
            } catch (Exception e) {
                getLogger().warning("Could not load support for VipFeatures.");
            } catch (MiniGameAlreadyRegistered miniGameAlreadyRegistered) {
                miniGameAlreadyRegistered.printStackTrace();
            }
        }

        // Initialize team upgrades
        upgradesManager = new UpgradesManager();
        upgradesManager.init();

        // Initialize sidebar manager
        Bukkit.getScheduler().runTask(this, () -> {
            if (Bukkit.getPluginManager().getPlugin("TAB") != null) {
                getLogger().info("Hooking into TAB support!");
                if (!checkTABVersion(Bukkit.getPluginManager().getPlugin("TAB").getDescription().getVersion())){
                    this.getLogger().severe("Invalid TAB version, you are using v" + Bukkit.getPluginManager().getPlugin("TAB").getDescription().getVersion() + " but v4.0.2 or higher is required!" );
                    Bukkit.getPluginManager().disablePlugin(this);
                    return;
                }
                if (BoardManager.init()) {
                    getLogger().info("TAB support has been loaded");

                    /* Load join signs. */
                    loadArenasAndSigns();

                } else {
                    this.getLogger().severe("Tab scoreboard is not enabled! please enable this in the tab configuration file!");
                    Bukkit.getPluginManager().disablePlugin(this);
                }
            } else {
                this.getLogger().severe("TAB by NEZNAMY could not be hooked!");
                Bukkit.getPluginManager().disablePlugin(this);
            }
        });

        // Load items after plugin is initialized, this gives addons a chance to register handlers.
        Bukkit.getScheduler().runTask(this, () -> {
            debug("Loading item + handlers");
            /* Load permanent join items */
            loadLobbyItems();
            loadSpectatorItems();
            loadPreGameItems();
        });

//        registerEvents(new ScoreboardListener()); #Disabled for now

        if (config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_ENABLE_HALLOWEEN)) {
            // Halloween Special
            HalloweenSpecial.init();
        }

        // Register features
        SpoilPlayerTNTFeature.init();
        GenSplitFeature.init();
        AntiDropFeature.init();

        // Initialize the addons
        Bukkit.getScheduler().runTaskLater(this, () -> addonManager.loadAddons(), 60L);

        // Check config settings
        if (redisConnection != null){
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (redisConnection.checkSettings("default_rankup_cost", String.valueOf(LevelsConfig.getNextCost(1)))){
                    Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Redis settings match the default values.");
                } else {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "The network settings do not match the set values! Please check the configuration!");
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Want to set value as default? Use '" + ChatColor.WHITE + "/bw redisUpdate default_rankup_cost" + ChatColor.RED + "' from the console!");
                }
            }, 70L);
        }

        // Send startup message, delayed to make sure everything is loaded and registered.
        Bukkit.getScheduler().runTaskLater(this, () -> {
            this.getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            this.getLogger().info("BedWars2023 v"+ plugin.getDescription().getVersion()+" has been enabled!");
            this.getLogger().info("");
            this.getLogger().info("Server Type: " + getServerType().toString() + (getServerType() == ServerType.BUNGEE ? " (ServerID: " + config.getString(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_SERVER_ID) + ")" : ""));
            this.getLogger().info("Auto Scale: " + autoscale);
            this.getLogger().info("Datasource: " + remoteDatabase.getClass().getSimpleName());
            this.getLogger().info("Restore Adapter: " + api.getRestoreAdapter().getDisplayName());
            this.getLogger().info("NMS version: " + nms.getClass().getSimpleName());
            this.getLogger().info("");

            StringJoiner arenaString = new StringJoiner(", ");
            arenaString.setEmptyValue("None");
            for (IArena arena : api.getArenaUtil().getArenas()) {
                arenaString.add(arena.getArenaName());
            }

            this.getLogger().info("Arena" + (api.getArenaUtil().getArenas().isEmpty() || api.getArenaUtil().getArenas().size() > 1 ? "s" : "") + " (" + api.getArenaUtil().getArenas().size() + "): " + arenaString);

            StringJoiner addonString = new StringJoiner(", ");
            addonString.setEmptyValue("None");
            for (Addon addon : api.getAddonsUtil().getAddons()){
                addonString.add(addon.getName());
            }

            this.getLogger().info("Addon" + (addonManager.getAddons().isEmpty() || addonManager.getAddons().size() > 1 ? "s" : "") + " (" + addonManager.getAddons().size() + "): " + addonString);
            this.getLogger().info("");
            this.getLogger().info("PAPI Support: " + papiSupportLoaded);
            this.getLogger().info("Vault Chat Hook: " + vaultChatLoaded);
            this.getLogger().info("Vault Economy Hook: " + vaultEconomyLoaded);
            this.getLogger().info("");
            this.getLogger().info("TAB Version: " + Bukkit.getPluginManager().getPlugin("TAB").getDescription().getVersion());
            this.getLogger().info("TAB Features: ");
            this.getLogger().info("  - Scoreboard: " + (TabAPI.getInstance().getScoreboardManager() == null ? "false" : "true"));
            try {
                this.getLogger().info("  - UnlimitedNameTag: " + ((TabAPI.getInstance().getNameTagManager() instanceof UnlimitedNameTagManager)  ? "true" : "false"));
                tabUnlimitedNameTagSupport = true;
            } catch (NoClassDefFoundError e) {
                this.getLogger().info("  - UnlimitedNameTag: not supported!");
                tabUnlimitedNameTagSupport = false;
            }
            this.getLogger().info("  - BossBar: " + ((TabAPI.getInstance().getBossBarManager() == null)  ? "false" : "true"));
            this.getLogger().info("  - TablistNameFormatting: " + ((TabAPI.getInstance().getTabListFormatManager() == null)  ? "false" : "true"));
            this.getLogger().info("  - HeaderFooterFormatting: " + ((TabAPI.getInstance().getHeaderFooterManager() == null)  ? "false" : "true"));
            this.getLogger().info("");
            this.getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }, 80L);
    }

    private void registerDelayedCommands() {
        if (!nms.isBukkitCommandRegistered("shout")) {
            nms.registerCommand("shout", new ShoutCommand("shout"));
        }
        nms.registerCommand("rejoin", new RejoinCommand("rejoin"));
        if (!(nms.isBukkitCommandRegistered("leave") && getServerType() == ServerType.BUNGEE)) {
            nms.registerCommand("leave", new LeaveCommand("leave"));
        }
        if (getServerType() != ServerType.BUNGEE && config.getBoolean(ConfigPath.GENERAL_ENABLE_PARTY_CMD)) {
            Bukkit.getLogger().info("Registering /party command..");
            nms.registerCommand("party", new PartyCommand("party"));
        }
    }

    public void onDisable() {
        shuttingDown = true;
        addonManager.unloadAddons();
        if (!serverSoftwareSupport) return;
        if (getServerType() == ServerType.BUNGEE) {
            redisConnection.close();
        }
        for (IArena a : new LinkedList<>(Arena.getArenas())) {
            try {
                a.disable();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

    private void loadArenasAndSigns() {

        api.getRestoreAdapter().convertWorlds();

        File dir = new File(plugin.getDataFolder(), "/Arenas");
        if (dir.exists()) {
            List<File> files = new ArrayList<>();
            File[] fls = dir.listFiles();
            for (File fl : Objects.requireNonNull(fls)) {
                if (fl.isFile()) {
                    if (fl.getName().endsWith(".yml")) {
                        files.add(fl);
                    }
                }
            }

            if (serverType == ServerType.BUNGEE && !autoscale) {
                if (files.isEmpty()) {
                    this.getLogger().log(java.util.logging.Level.WARNING, "Could not find any arena!");
                    return;
                }
                Random r = new Random();
                int x = r.nextInt(files.size());
                String name = files.get(x).getName().replace(".yml", "");
                new Arena(name, null);
            } else {
                for (File file : files) {
                    new Arena(file.getName().replace(".yml", ""), null);
                }
            }
        }
    }

    public static void registerEvents(Listener... listeners) {
        Arrays.stream(listeners).forEach(l -> plugin.getServer().getPluginManager().registerEvents(l, plugin));
    }

    public static void setDebug(boolean value) {
        debug = value;
    }

    public static void setServerType(ServerType serverType) {
        BedWars.serverType = serverType;
        if (serverType == ServerType.BUNGEE) autoscale = true;
    }

    public static void setAutoscale(boolean autoscale) {
        BedWars.autoscale = autoscale;
    }

    public static void debug(String message) {
        if (debug) {
            plugin.getLogger().info("DEBUG: " + message);
        }
    }

    public static String getForCurrentVersion(String v18, String v12, String v13) {
        switch (getServerVersion()) {
            case "v1_8_R3":
                return v18;
            case "v1_12_R1":
                return v12;
        }
        return v13;
    }

    public static ServerType getServerType() {
        return serverType;
    }

    public static Party getPartyManager() {
        return partyManager;
    }

    public static IChat getChatSupport() {
        return chat;
    }

    /**
     * Get current levels manager.
     */
    public static Level getLevelSupport() {
        return level;
    }

    /**
     * Set the levels manager.
     * You can use this to add your own levels manager just implement
     * the Level interface so the plugin will be able to display
     * the level internally.
     */

    public static void setLevelAdapter(Level levelsManager) {
        if (levelsManager instanceof InternalLevel) {
            if (LevelListeners.instance == null) {
                Bukkit.getPluginManager().registerEvents(new LevelListeners(), BedWars.plugin);
            }
        } else {
            if (LevelListeners.instance != null) {
                PlayerJoinEvent.getHandlerList().unregister(LevelListeners.instance);
                PlayerQuitEvent.getHandlerList().unregister(LevelListeners.instance);
                LevelListeners.instance = null;
            }
        }
        level = levelsManager;
    }

    public static IEconomy getEconomy() {
        return economy;
    }

    public static ConfigManager getGeneratorsCfg() {
        return generators;
    }

    public static void setLobbyWorld(String lobbyWorld) {
        BedWars.lobbyWorld = lobbyWorld;
    }

    /**
     * Get the server version
     * Ex: v1_8_R3
     *
     * @since v0.6.5beta
     */
    public static String getServerVersion() {
        return nmsVersion;
    }

    public static String getLobbyWorld() {
        return lobbyWorld;
    }

    /**
     * Get remote database.
     */
    public static IDatabase getRemoteDatabase() {
        return remoteDatabase;
    }

    /**
     * Get redis connection.
     */
    public static RedisConnection getRedisConnection() {
        return redisConnection;
    }

    public static StatsManager getStatsManager() {
        return statsManager;
    }

    public static UpgradesManager getUpgradeManager(){
        return upgradesManager;
    }

    public static com.tomkeuper.bedwars.api.BedWars getAPI() {
        return api;
    }

    /**
     * Try loading custom adapter support.
     * @return true when custom adapter was registered.
     */
    private boolean handleWorldAdapter() { //todo fix version check because current check is limited
        Plugin swmPlugin = Bukkit.getPluginManager().getPlugin("SlimeWorldManager");

        if (null == swmPlugin){
            return false;
        }
        PluginDescriptionFile pluginDescription = swmPlugin.getDescription();
        if (null == pluginDescription) {
            return false;
        }

        String[] versionString = pluginDescription.getVersion().split("\\.");


        try {
            int major = Integer.parseInt(versionString[0]);
            int minor = Integer.parseInt(versionString[1]);
            int release = versionString.length >= 3 ? Integer.parseInt(versionString[2]) : 0;

            String adapterPath;
            if (((major == 2 && minor == 2 && release == 1) || swmPlugin.getDescription().getVersion().equals("2.3.0-SNAPSHOT")) && (nms.getVersion() == 0 || nms.getVersion() == 5)) {
                adapterPath = "com.tomkeuper.bedwars.arena.mapreset.slime.SlimeAdapter";
            } else if ((major == 2 && (minor >= 8 && minor <= 10) && (release >= 0 && release <= 9)) && (nms.getVersion() == 8)) {
                adapterPath = "com.tomkeuper.bedwars.arena.mapreset.slime.AdvancedSlimeAdapter";
            } else if ((major > 2 || major == 2 && minor >= 10) && nms.getVersion() == 9) {
                adapterPath = "com.tomkeuper.bedwars.arena.mapreset.slime.SlimePaperAdapter";
            } else {
                this.getLogger().warning("Could not find adapter path for SWM version, is it unsupported?");
                return false;
            }

            Constructor<?> constructor = Class.forName(adapterPath).getConstructor(Plugin.class);
            getLogger().info("Loading restore adapter: "+adapterPath+" ...");

            RestoreAdapter candidate = (RestoreAdapter) constructor.newInstance(this);
            api.setRestoreAdapter(candidate);
            getLogger().info("Hook into "+candidate.getDisplayName()+" as restore adapter.");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            this.getLogger().warning("Something went wrong! Using internal reset adapter...");
        }
        return false;
    }

    public static boolean isShuttingDown() {
        return shuttingDown;
    }

    public static void setPartyManager(Party partyManager) {
        BedWars.partyManager = partyManager;
    }

    public static void setEconomy(IEconomy economy) {
        BedWars.economy = economy;
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return new VoidChunkGenerator();
    }

    public static void setRemoteDatabase(IDatabase database){
        remoteDatabase = database;
    }


    private boolean checkTABVersion(String version) {
        String targetVersion = "4.0.2";

        String[] currentParts = version.split("\\.");
        String[] targetParts = targetVersion.split("\\.");

        // Compare major version
        int currentMajor = Integer.parseInt(currentParts[0]);
        int targetMajor = Integer.parseInt(targetParts[0]);
        if (currentMajor < targetMajor) {
            return false;
        } else if (currentMajor > targetMajor) {
            return true;
        }

        // Compare minor version
        int currentMinor = Integer.parseInt(currentParts[1]);
        int targetMinor = Integer.parseInt(targetParts[1]);
        if (currentMinor < targetMinor) {
            return false;
        } else if (currentMinor > targetMinor) {
            return true;
        }

        // Compare patch version
        int currentPatch = Integer.parseInt(currentParts[2]);
        int targetPatch = Integer.parseInt(targetParts[2]);

        return currentPatch >= targetPatch;
    }

    public static Collection<IPermanentItem> getLobbyItems() {
        return lobbyItems;
    }

    public static Collection<IPermanentItem> getSpectatorItems() {
        return spectatorItems;
    }

    public static Collection<IPermanentItem> getPreGameItems() {
        return preGameItems;
    }

    private void loadPreGameItems() {

        if (config.getYml().get(ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_PATH) == null) return;

        for (String item : config.getYml().getConfigurationSection(ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_PATH).getKeys(false)) {
            if (!checkConfigEntries(item,
                    ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_MATERIAL,
                    ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_DATA,
                    ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_SLOT,
                    ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_ENCHANTED)) {
                continue;
            }

            ItemStack i = new ItemStack(Material.valueOf(config.getYml().getString(ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_MATERIAL.replace("%path%", item))), 1, (byte) config.getInt(ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_DATA.replace("%path%", item)));
            ItemMeta im = i.getItemMeta();
            if (config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_ENCHANTED.replace("%path%", item))) {
                im.addEnchant(Enchantment.LUCK, 1, true);
                im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            i.setItemMeta(im);

            PreGameItem preGameItem;
            IPermanentItemHandler handler = itemHandlers.get(item);
            if (handler != null) {
                preGameItem = new PreGameItem(
                        handler,
                        nms.addCustomData(i, "preGameItem"),
                        config.getInt(ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_SLOT.replace("%path%", item)),
                        item);
            } else {
                // Check if item has a command listed instead
                if (config.getYml().getString(ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_COMMAND.replace("%path%", item)) != null){
                    preGameItem = new PreGameItem(
                            itemHandlers.get("command"),
                            nms.addCustomData(i, "preGameItem"),
                            config.getInt(ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_SLOT.replace("%path%", item)),
                            item);
                } else {
                    this.getLogger().severe("No handler or command found for pre-game item: " + item);
                    continue;
                }
            }

            debug("Loaded pre-game item: " + preGameItem.getIdentifier());
            preGameItems.add(preGameItem);
        }
    }

    void loadSpectatorItems(){
        if (config.getYml().get(ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_PATH) == null) return;

        for (String item : config.getYml().getConfigurationSection(ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_PATH).getKeys(false)) {
            if (!checkConfigEntries(item,
                    ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_MATERIAL,
                    ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_DATA,
                    ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_SLOT,
                    ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_ENCHANTED)) {
                continue;
            }

            ItemStack i = new ItemStack(Material.valueOf(config.getYml().getString(ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_MATERIAL.replace("%path%", item))), 1, (byte) config.getInt(ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_DATA.replace("%path%", item)));
            ItemMeta im = i.getItemMeta();
            if (config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_ENCHANTED.replace("%path%", item))) {
                im.addEnchant(Enchantment.LUCK, 1, true);
                im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            i.setItemMeta(im);

            SpectatorItem spectatorItem;
            IPermanentItemHandler handler = itemHandlers.get(item);
            if (handler != null) {
                spectatorItem = new SpectatorItem(
                        handler,
                        nms.addCustomData(i, "spectatorItem"),
                        config.getInt(ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_SLOT.replace("%path%", item)),
                        item);
            } else {
                // Check if item has a command listed instead
                if (config.getYml().getString(ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_COMMAND.replace("%path%", item)) != null){
                    spectatorItem = new SpectatorItem(
                            itemHandlers.get("command"),
                            nms.addCustomData(i, "spectatorItem"),
                            config.getInt(ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_SLOT.replace("%path%", item)),
                            item);
                } else {
                    this.getLogger().severe("No handler or command found for spectator item: " + item);
                    continue;
                }
            }

            debug("Loaded spectator item: " + spectatorItem.getIdentifier());
            spectatorItems.add(spectatorItem);
        }
    }

    void loadLobbyItems(){
        if (config.getYml().get(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH) == null) return;

        for (String item : config.getYml().getConfigurationSection(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH).getKeys(false)) {

            if (!checkConfigEntries(item,
                    ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_MATERIAL,
                    ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_DATA,
                    ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_SLOT,
                    ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_ENCHANTED)) {
                continue;
            }

            ItemStack i = new ItemStack(Material.valueOf(config.getYml().getString(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_MATERIAL.replace("%path%", item))), 1, (byte) config.getInt(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_DATA.replace("%path%", item)));
            ItemMeta im = i.getItemMeta();
            if (config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_ENCHANTED.replace("%path%", item))) {
                im.addEnchant(Enchantment.LUCK, 1, true);
                im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            i.setItemMeta(im);

            LobbyItem lobbyItem;

            IPermanentItemHandler handler = itemHandlers.get(item);
            if (handler != null) {
                lobbyItem = new LobbyItem(
                        handler,
                        nms.addCustomData(i, "lobbyItem"),
                        config.getInt(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_SLOT.replace("%path%", item)),
                        item);
            } else {
                // Check if item has a command listed instead
                if (config.getYml().getString(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_COMMAND.replace("%path%", item)) != null){
                    lobbyItem = new LobbyItem(
                            itemHandlers.get("command"),
                            nms.addCustomData(i, "lobbyItem"),
                            config.getInt(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_SLOT.replace("%path%", item)),
                            item);
                } else {
                    this.getLogger().severe("No handler or command found for lobby item: " + item);
                    continue;
                }
            }

            debug("Loaded lobby item: " + lobbyItem.getIdentifier());
            lobbyItems.add(lobbyItem);
        }
    }

    private boolean checkConfigEntries(String item, String... paths) {
        boolean valid = true;
        for (String path : paths) {
            if (config.getYml().get(path.replace("%path%", item)) == null) {
                BedWars.plugin.getLogger().severe(path.replace("%path%", item) + " is not set!");
                valid = false;
            }
        }
        return valid;
    }

    public static boolean registerItemHandler(IPermanentItemHandler handler){
        if (itemHandlers.containsKey(handler.getId())){
            return false;
        }
        itemHandlers.put(handler.getId(), handler);
        return true;
    }

    private void registerItemHandlers(IPermanentItemHandler... handlers){
        for (IPermanentItemHandler handler : handlers){
            if (registerItemHandler(handler)) {
                getLogger().info("Registered item handler: " + handler.getId());
            } else {
                getLogger().warning("Could not register item handler: " + handler.getId());
            }
        }
    }

    public static Map<String, IPermanentItemHandler> getItemHandlers(){
        return itemHandlers;
    }
}
