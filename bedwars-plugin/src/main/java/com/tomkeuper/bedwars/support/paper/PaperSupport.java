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

package com.tomkeuper.bedwars.support.paper;

import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import io.papermc.lib.PaperLib;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;

import static com.tomkeuper.bedwars.BedWars.config;
import static com.tomkeuper.bedwars.BedWars.isPaper;

public final class PaperSupport {

    public static void teleport(Entity entity, Location location){
        if (isPaper && config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_PERFORMANCE_PAPER_FEATURES)){
            PaperLib.teleportAsync(entity, location);
        } else {
            entity.teleport(location);
        }
    }

    public static void teleportC(Entity entity, Location location, PlayerTeleportEvent.TeleportCause cause){
        if (isPaper){
            PaperLib.teleportAsync(entity, location, cause);
        } else {
            entity.teleport(location, cause);
        }
    }

}
