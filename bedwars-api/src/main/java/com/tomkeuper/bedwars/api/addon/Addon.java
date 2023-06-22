package com.tomkeuper.bedwars.api.addon;

public abstract class Addon {
    /**
     * Get the author of an addon
     */
    public abstract String getAuthor();

    /**
     * Get the version of an addon
     */
    public abstract String getVersion();

    /**
     * Get the identifier of an addon
     */
    public abstract String getIdentifier();

    /**
     * Load everything from the addon
     * Listeners, config files, databases,...
     */
    public abstract void load();

    /**
     * Unload everything from the addon
     */
    public abstract void unload();
}
