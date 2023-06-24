package com.tomkeuper.bedwars.addon;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.addon.Addon;
import com.tomkeuper.bedwars.api.addon.AddonStorer;
import com.tomkeuper.bedwars.api.addon.IAddonManager;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.stream.Collectors;

public class AddonManager extends AddonStorer implements IAddonManager {
    private static List<Addon> registeredAddons;
    private static List<Addon> unloadedAddons;
    private static List<Addon> loadedAddons;

    public AddonManager() {
        registeredAddons = registeredAddons();
        unloadedAddons = unloadedAddons();
        loadedAddons = loadedAddons();
    }


    @Override
    public List<Addon> getAddons() {
        return registeredAddons;
    }

    @Override
    public List<Addon> getLoadedAddons() {
        return loadedAddons;
    }

    @Override
    public List<Addon> getUnloadedAddons() {
        return unloadedAddons;
    }

    @Override
    public List<Addon> getAddonsByAuthor(String author) {
        return registeredAddons.stream().filter(a -> a.getAuthor().equals(author)).collect(Collectors.toList());
    }

    @Override
    public void loadAddon(Addon addon) {
        loadedAddons.add(addon);
        if (!Bukkit.getPluginManager().isPluginEnabled(addon.getPlugin()))
            Bukkit.getPluginManager().enablePlugin(addon.getPlugin());
    }

    @Override
    public void unloadAddon(Addon addon) {
        unloadedAddons.add(addon);
        addon.unload();
        Bukkit.getPluginManager().disablePlugin(addon.getPlugin());
    }

    @Override
    public void unloadAddons() {
        if (loadedAddons.isEmpty()) return;
        log("Unloading addons...");
        for (Addon addon : loadedAddons) {
            log("Unloading " + addon.getIdentifier() + " by " + addon.getAuthor());
            addon.unload();
            Bukkit.getPluginManager().disablePlugin(addon.getPlugin());
            log("Addon unloaded successfully!");
        }
    }

    @Override
    public void loadAddons() {
        String count = "";
        if (registeredAddons.size() < 1) {
            log("No addons were found!");
            return;
        }
        else if (registeredAddons.size() == 1) count = "addon";
        else if (registeredAddons.size() > 1) count = "addons";
        log(registeredAddons.size() + " " + count + " has been found!");
        log("Loading " + registeredAddons.size() + " " + count);
        for (Addon addon : registeredAddons) {
            log("Loading " + addon.getIdentifier() + " by " + addon.getAuthor()+". " + "Version " + addon.getVersion());
            loadedAddons.add(addon);
            addon.load();
            log(addon.getIdentifier() + " addon loaded and registered successfully!");
        }
    }

    private void log(String log) {
        BedWars.getPlugin(BedWars.class).getLogger().info(log);
    }
}
