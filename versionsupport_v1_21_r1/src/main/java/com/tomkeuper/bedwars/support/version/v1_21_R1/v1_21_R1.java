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

package com.tomkeuper.bedwars.support.version.v1_21_R1;

import com.mojang.datafixers.util.Pair;
import com.saicone.rtag.RtagItem;
import com.saicone.rtag.util.OptionalType;
import com.saicone.rtag.util.SkullTexture;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.arena.generator.IGeneratorAnimation;
import com.tomkeuper.bedwars.api.arena.shop.ShopHolo;
import com.tomkeuper.bedwars.api.arena.team.ITeam;
import com.tomkeuper.bedwars.api.arena.team.TeamColor;
import com.tomkeuper.bedwars.api.entity.Despawnable;
import com.tomkeuper.bedwars.api.entity.GeneratorHolder;
import com.tomkeuper.bedwars.api.events.player.PlayerKillEvent;
import com.tomkeuper.bedwars.api.hologram.containers.IHoloLine;
import com.tomkeuper.bedwars.api.hologram.containers.IHologram;
import com.tomkeuper.bedwars.api.language.Messages;
import com.tomkeuper.bedwars.api.server.VersionSupport;
import com.tomkeuper.bedwars.support.version.common.VersionCommon;
import com.tomkeuper.bedwars.support.version.v1_21_R1.despawnable.DespawnableAttributes;
import com.tomkeuper.bedwars.support.version.v1_21_R1.despawnable.DespawnableFactory;
import com.tomkeuper.bedwars.support.version.v1_21_R1.despawnable.DespawnableType;
import com.tomkeuper.bedwars.support.version.v1_21_R1.hologram.HoloLine;
import com.tomkeuper.bedwars.support.version.v1_21_R1.hologram.Hologram;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.core.particles.ParticleParamRedstone;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import net.minecraft.world.entity.item.EntityTNTPrimed;
import net.minecraft.world.entity.projectile.EntityFireball;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBase;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Ladder;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.command.Command;
import org.bukkit.craftbukkit.v1_21_R1.CraftServer;
import org.bukkit.craftbukkit.v1_21_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R1.entity.*;
import org.bukkit.craftbukkit.v1_21_R1.inventory.CraftItemStack;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import static com.tomkeuper.bedwars.api.language.Language.getList;

public final class v1_21_R1 extends VersionSupport {

    private final DespawnableFactory despawnableFactory;

    public v1_21_R1(Plugin plugin, String name) {
        super(plugin, name);
        loadDefaultEffects();
        this.despawnableFactory = new DespawnableFactory(this);
    }

    @Override
    public void registerCommand(String name, Command cmd) {
        ((CraftServer) getPlugin().getServer()).getCommandMap().register(name, cmd);
    }

    @Override
    public void sendTitle(Player p, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        p.sendTitle(title == null ? " " : title, subtitle == null ? " " : subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public void playAction(Player p, String text) {
        p.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                new TextComponent(ChatColor.translateAlternateColorCodes('&', text)
                )
        );
    }

    @Override
    public boolean isBukkitCommandRegistered(String name) {
        return ((CraftServer) getPlugin().getServer()).getCommandMap().getCommand(name) != null;
    }

    @Override
    public org.bukkit.inventory.ItemStack getItemInHand(@NotNull Player p) {
        return p.getInventory().getItemInMainHand();
    }

    @Override
    public void hideEntity(Entity e, Player p) {
        PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(e.getEntityId());
        sendPacket(p, packet);
    }

    @Override
    public boolean isArmor(org.bukkit.inventory.ItemStack itemStack) {
        var i = getItem(itemStack);
        if (null == i) return false;
        return i instanceof ItemArmor || i instanceof ItemElytra;
    }

    @Override
    public boolean isTool(org.bukkit.inventory.ItemStack itemStack) {
        var i = getItem(itemStack);
        if (null == i) return false;
        return i instanceof ItemTool;
    }

    @Override
    public boolean isSword(org.bukkit.inventory.ItemStack itemStack) {
        var i = getItem(itemStack);
        if (null == i) return false;
        return i instanceof ItemSword;
    }

    @Override
    public boolean isAxe(org.bukkit.inventory.ItemStack itemStack) {
        var i = getItem(itemStack);
        if (null == i) return false;
        return i instanceof ItemAxe;
    }

    @Override
    public boolean isBow(org.bukkit.inventory.ItemStack itemStack) {
        var i = getItem(itemStack);
        if (null == i) return false;
        return i instanceof ItemBow;
    }


    @Override
    public boolean isProjectile(org.bukkit.inventory.ItemStack itemStack) {
        var entity = getEntity(itemStack);
        if (null == entity) return false;
        return entity instanceof IProjectile;
    }

    @Override
    public boolean isInvisibilityPotion(org.bukkit.inventory.@NotNull ItemStack itemStack) {
        if (!itemStack.getType().equals(org.bukkit.Material.POTION)) return false;

        org.bukkit.inventory.meta.PotionMeta pm = (org.bukkit.inventory.meta.PotionMeta) itemStack.getItemMeta();

        return pm != null && pm.hasCustomEffects() && pm.hasCustomEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
    }

    @Override
    public void registerEntities() {

    }

    @Override
    public void spawnShop(Location loc, String name1, List<Player> players, IArena arena) {
        Location l = loc.clone();

        if (l.getWorld() == null) return;
        Villager vlg = (Villager) l.getWorld().spawnEntity(loc, EntityType.VILLAGER);
        vlg.setAI(false);
        vlg.setRemoveWhenFarAway(false);
        vlg.setCollidable(false);
        vlg.setInvulnerable(true);
        vlg.setSilent(true);
    }

    @Override
    public void spawnShopHologram(Location loc, String name1, List<Player> players, IArena arena, ITeam team) {
        for (Player p : players) {
            String[] nume = (getList(p, name1) == null || getList(p, name1).isEmpty() ? getList(p, name1.replace(name1.split("\\.")[2], "default")) : getList(p, name1)).toArray(new String[0]);
            IHologram h = createHologram(p, loc, nume);

            new ShopHolo(h, loc, arena, team);
        }

        for (Player p : players) {
            ShopHolo.getShopHolograms(p).forEach(ShopHolo::update);
        }
    }

    @Override
    public double getDamage(org.bukkit.inventory.ItemStack i) {
        var tag = getTag(i);
        if (null == tag) {
            throw new RuntimeException("Provided item has no Tag");
        }
        return tag.k("generic.attackDamage");
    }

    @Override
    public void spawnSilverfish(Location loc, ITeam bedWarsTeam, double speed, double health, int despawn, double damage) {
        var attr = new DespawnableAttributes(DespawnableType.SILVERFISH, speed, health, damage, despawn);
        var entity = despawnableFactory.spawn(attr, loc, bedWarsTeam);

        new Despawnable(
                entity,
                bedWarsTeam, despawn,
                Messages.SHOP_UTILITY_NPC_SILVERFISH_NAME,
                PlayerKillEvent.PlayerKillCause.SILVERFISH_FINAL_KILL,
                PlayerKillEvent.PlayerKillCause.SILVERFISH
        );
    }

    @Override
    public void spawnIronGolem(Location loc, ITeam bedWarsTeam, double speed, double health, int despawn) {
        var attr = new DespawnableAttributes(DespawnableType.IRON_GOLEM, speed, health, 4, despawn);
        var entity = despawnableFactory.spawn(attr, loc, bedWarsTeam);
        new Despawnable(
                entity,
                bedWarsTeam, despawn,
                Messages.SHOP_UTILITY_NPC_IRON_GOLEM_NAME,
                PlayerKillEvent.PlayerKillCause.IRON_GOLEM_FINAL_KILL,
                PlayerKillEvent.PlayerKillCause.IRON_GOLEM
        );
    }

    @Override
    public void minusAmount(Player p, org.bukkit.inventory.@NotNull ItemStack i, int amount) {
        if (i.getAmount() - amount <= 0) {
            if (p.getInventory().getItemInOffHand().equals(i)) {
                p.getInventory().setItemInOffHand(null);
            } else {
                p.getInventory().removeItem(i);
            }
            return;
        }
        i.setAmount(i.getAmount() - amount);
        //noinspection UnstableApiUsage
        p.updateInventory();
    }

    @Override
    public void setSource(TNTPrimed tnt, Player owner) {
        EntityLiving nmsEntityLiving = (((CraftLivingEntity) owner).getHandle());
        EntityTNTPrimed nmsTNT = (((CraftTNTPrimed) tnt).getHandle());
        try {
            Field sourceField = EntityTNTPrimed.class.getDeclaredField("h");
            sourceField.setAccessible(true);
            sourceField.set(nmsTNT, nmsEntityLiving);
        } catch (Exception ex) {
            //noinspection CallToPrintStackTrace
            ex.printStackTrace();
        }
    }

    @Override
    public void voidKill(Player p) {
        @SuppressWarnings("UnstableApiUsage")
        EntityDamageEvent event = new EntityDamageEvent(p, EntityDamageEvent.DamageCause.VOID, DamageSource.builder(DamageType.GENERIC).build(), 1000.0);
        //noinspection removal
        p.setLastDamageCause(event);
        p.setHealth(0);
    }

    @Override
    public void hideArmor(@NotNull Player victim, Player receiver) {
        List<Pair<EnumItemSlot, ItemStack>> items = new ArrayList<>();
        items.add(new Pair<>(EnumItemSlot.f, new ItemStack(Item.b(0))));
        items.add(new Pair<>(EnumItemSlot.e, new ItemStack(Item.b(0))));
        items.add(new Pair<>(EnumItemSlot.d, new ItemStack(Item.b(0))));
        items.add(new Pair<>(EnumItemSlot.c, new ItemStack(Item.b(0))));
        PacketPlayOutEntityEquipment packet = new PacketPlayOutEntityEquipment(victim.getEntityId(), items);
        sendPacket(receiver, packet);
    }

    @Override
    public void showArmor(Player victim, Player receiver) {
        List<Pair<EnumItemSlot, ItemStack>> items = new ArrayList<>();
        items.add(new Pair<>(EnumItemSlot.f, CraftItemStack.asNMSCopy(victim.getInventory().getHelmet())));
        items.add(new Pair<>(EnumItemSlot.e, CraftItemStack.asNMSCopy(victim.getInventory().getChestplate())));
        items.add(new Pair<>(EnumItemSlot.d, CraftItemStack.asNMSCopy(victim.getInventory().getLeggings())));
        items.add(new Pair<>(EnumItemSlot.c, CraftItemStack.asNMSCopy(victim.getInventory().getBoots())));
        PacketPlayOutEntityEquipment packet = new PacketPlayOutEntityEquipment(victim.getEntityId(), items);
        sendPacket(receiver, packet);
    }

    @Override
    public EnderDragon spawnDragon(Location l, ITeam team) {
        if (l == null || l.getWorld() == null) {
            getPlugin().getLogger().log(Level.WARNING, "Could not spawn Dragon. Location is null");
            return null;
        }
        EnderDragon ed = (EnderDragon) l.getWorld().spawnEntity(l, EntityType.ENDER_DRAGON);
        ed.setPhase(EnderDragon.Phase.CIRCLING);
        return ed;
    }

    @Override
    public void colorBed(ITeam bwt) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockState bed = bwt.getBed().clone().add(x, 0, z).getBlock().getState();
                if (bed instanceof Bed) {
                    bed.setType(bwt.getColor().bedMaterial());
                    bed.update();
                }
            }
        }
    }

    @Override
    public void registerTntWhitelist(float endStoneBlast, float glassBlast) {
        try {
            // blast resistance
            Field field = BlockBase.class.getDeclaredField("aH");
            field.setAccessible(true);
            // end stone
            field.set(Blocks.fz, endStoneBlast);
            // obsidian
            field.set(Blocks.co, glassBlast);
            // standard glass
            field.set(Blocks.aQ, glassBlast);

            var coloredGlass = new net.minecraft.world.level.block.Block[]{
                    Blocks.ei, Blocks.ej, Blocks.ek, Blocks.el,
                    Blocks.em, Blocks.en, Blocks.eo, Blocks.ep,
                    Blocks.eq, Blocks.er, Blocks.es, Blocks.et,
                    Blocks.eu, Blocks.ev, Blocks.ew, Blocks.ex,

                    Blocks.aQ,
            };

            Arrays.stream(coloredGlass).forEach(
                    glass -> {
                        try {
                            field.set(glass, glassBlast);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );
        } catch (NoSuchFieldException | IllegalAccessException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

    @Override
    public void setBlockTeamColor(Block block, TeamColor teamColor) {
        if (block.getType().toString().contains("STAINED_GLASS") || block.getType().toString().equals("GLASS")) {
            block.setType(teamColor.glassMaterial());
        } else if (block.getType().toString().contains("_TERRACOTTA")) {
            block.setType(teamColor.glazedTerracottaMaterial());
        } else if (block.getType().toString().contains("_WOOL")) {
            block.setType(teamColor.woolMaterial());
        }
    }

    @Override
    public void setCollide(Player p, IArena a, boolean value) {
        p.setCollidable(value);
    }

    @Override
    public org.bukkit.inventory.ItemStack addCustomData(org.bukkit.inventory.ItemStack i, String data) {
        return RtagItem.edit(i, tag -> {
            tag.set(data, VersionSupport.PLUGIN_TAG_GENERIC_KEY);
        });
    }

    @Override
    public org.bukkit.inventory.ItemStack setTag(org.bukkit.inventory.ItemStack itemStack, String key, String value) {
        return RtagItem.edit(itemStack, tag -> {
            tag.set(value, key);
        });
    }

    @Override
    public String getTag(org.bukkit.inventory.ItemStack itemStack, String key) {
        var tag = getTag(itemStack);
        return tag == null ? null : tag.e(key) ? tag.l(key) : null;
    }

    @Override
    public boolean isCustomBedWarsItem(org.bukkit.inventory.ItemStack i) {
        RtagItem rtagItem = new RtagItem(i);
        OptionalType tag = rtagItem.getOptional(VersionSupport.PLUGIN_TAG_GENERIC_KEY);
        return tag.isNotEmpty();
    }

    @Override
    public String getCustomData(org.bukkit.inventory.ItemStack i) {
        RtagItem rtagItem = new RtagItem(i);
        OptionalType tag = rtagItem.getOptional(VersionSupport.PLUGIN_TAG_GENERIC_KEY);
        return (tag.isEmpty() || tag.isNotInstance(String.class)) ? null : tag.asString(null);
    }

    @Override
    public org.bukkit.inventory.ItemStack colourItem(org.bukkit.inventory.ItemStack itemStack, ITeam bedWarsTeam) {
        if (itemStack == null) return null;
        String type = itemStack.getType().toString();
        if (isBed(itemStack.getType())) {
            return new org.bukkit.inventory.ItemStack(bedWarsTeam.getColor().bedMaterial(), itemStack.getAmount());
        } else if (type.contains("_STAINED_GLASS_PANE")) {
            return new org.bukkit.inventory.ItemStack(bedWarsTeam.getColor().glassPaneMaterial(), itemStack.getAmount());
        } else if (type.contains("STAINED_GLASS") || type.equals("GLASS")) {
            return new org.bukkit.inventory.ItemStack(bedWarsTeam.getColor().glassMaterial(), itemStack.getAmount());
        } else if (type.contains("_TERRACOTTA")) {
            return new org.bukkit.inventory.ItemStack(bedWarsTeam.getColor().glazedTerracottaMaterial(), itemStack.getAmount());
        } else if (type.contains("_WOOL")) {
            return new org.bukkit.inventory.ItemStack(bedWarsTeam.getColor().woolMaterial(), itemStack.getAmount());
        }
        return itemStack;
    }

    @Override
    public org.bukkit.inventory.ItemStack createItemStack(String material, int amount, short data) {
        org.bukkit.inventory.ItemStack i;
        try {
            i = new org.bukkit.inventory.ItemStack(org.bukkit.Material.valueOf(material), amount);
        } catch (Exception ex) {
            getPlugin().getLogger().log(Level.WARNING, material + " is not a valid " + getName() + " material!");
            i = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BEDROCK);
        }
        return i;
    }

    @Override
    public Material materialFireball() {
        return org.bukkit.Material.FIRE_CHARGE;
    }

    @Override
    public org.bukkit.Material materialPlayerHead() {
        return org.bukkit.Material.PLAYER_HEAD;
    }

    @Override
    public org.bukkit.Material materialSnowball() {
        return org.bukkit.Material.SNOWBALL;
    }

    @Override
    public org.bukkit.Material materialGoldenHelmet() {
        return org.bukkit.Material.GOLDEN_HELMET;
    }

    @Override
    public org.bukkit.Material materialGoldenChestPlate() {
        return org.bukkit.Material.GOLDEN_CHESTPLATE;
    }

    @Override
    public org.bukkit.Material materialGoldenLeggings() {
        return org.bukkit.Material.GOLDEN_LEGGINGS;
    }

    @Override
    public org.bukkit.Material materialNetheriteHelmet() {
        return Material.NETHERITE_HELMET;
    }

    @Override
    public org.bukkit.Material materialNetheriteChestPlate() {
        return Material.NETHERITE_CHESTPLATE;
    }

    @Override
    public org.bukkit.Material materialNetheriteLeggings() {
        return Material.NETHERITE_LEGGINGS;
    }

    @Override
    public org.bukkit.Material materialElytra() {
        return Material.ELYTRA;
    }

    @Override
    public org.bukkit.Material materialCake() {
        return org.bukkit.Material.CAKE;
    }

    @Override
    public org.bukkit.Material materialCraftingTable() {
        return org.bukkit.Material.CRAFTING_TABLE;
    }

    @Override
    public org.bukkit.Material materialEnchantingTable() {
        return org.bukkit.Material.ENCHANTING_TABLE;
    }

    @Override
    public org.bukkit.Material materialEndStone() {
        return Material.END_STONE;
    }

    @Override
    public org.bukkit.Material woolMaterial() {
        return org.bukkit.Material.WHITE_WOOL;
    }

    @Override
    public void setJoinSignBackground(BlockState b, Material material) {
        if (b.getBlockData() instanceof WallSign) {
            b.getBlock().getRelative(((WallSign) b.getBlockData()).getFacing().getOppositeFace()).setType(material);
        }
    }

    @Override
    public org.bukkit.inventory.ItemStack redGlassPane(int amount) {
        return new org.bukkit.inventory.ItemStack(Material.RED_STAINED_GLASS_PANE, amount);
    }

    @Override
    public org.bukkit.inventory.ItemStack greenGlassPane(int amount) {
        return new org.bukkit.inventory.ItemStack(Material.LIME_STAINED_GLASS_PANE, amount);
    }

    @Override
    public String getShopUpgradeIdentifier(org.bukkit.inventory.ItemStack itemStack) {
        RtagItem item = new RtagItem(itemStack);
        OptionalType tag = item.getOptional(VersionSupport.PLUGIN_TAG_TIER_KEY);
        return tag.isEmpty() ? "null" : tag.asString("null");
    }

    @Override
    public org.bukkit.inventory.ItemStack setShopUpgradeIdentifier(org.bukkit.inventory.ItemStack itemStack, String identifier) {
        RtagItem item = new RtagItem(itemStack);
        item.set(identifier, VersionSupport.PLUGIN_TAG_TIER_KEY);
        item.load();
        return item.getItem();
    }

    @Override
    public org.bukkit.inventory.ItemStack getPlayerHead(Player player, org.bukkit.inventory.ItemStack copyTagFrom) {
        org.bukkit.inventory.ItemStack head = SkullTexture.getTexturedHead(player.getUniqueId().toString());

        if (copyTagFrom != null) {
            var tag = getTag(copyTagFrom);
            RtagItem rtagItem = new RtagItem(head);
            rtagItem.set(tag, VersionSupport.PLUGIN_TAG_GENERIC_KEY);
            rtagItem.load();
            head = rtagItem.getItem();
        }

        return head;
    }

    @Override
    public void sendPlayerSpawnPackets(Player respawned, IArena arena) {
        if (respawned == null) return;
        if (arena == null) return;
        if (!arena.isPlayer(respawned)) return;

        // if method was used when the player was still in re-spawning screen
        if (arena.getRespawnSessions().containsKey(respawned)) return;

        EntityPlayer entityPlayer = getPlayer(respawned);
        PacketPlayOutSpawnEntity show = newPacketPlayOutSpawnEntity(entityPlayer);
        PacketPlayOutEntityVelocity playerVelocity = new PacketPlayOutEntityVelocity(entityPlayer);
        // we send head rotation packet because sometimes on respawn others see him with bad rotation
        PacketPlayOutEntityHeadRotation head = new PacketPlayOutEntityHeadRotation(entityPlayer, getCompressedAngle(entityPlayer.getBukkitYaw()));

        // retrieve current armor and in-hand items
        // we send a packet later for timing issues where other players do not see them
        List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> list = getPlayerEquipment(entityPlayer);


        for (Player p : arena.getPlayers()) {
            if (p == null) continue;
            if (p.equals(respawned)) continue;
            // if p is in re-spawning screen continue
            if (arena.getRespawnSessions().containsKey(p)) continue;

            EntityPlayer boundTo = getPlayer(p);
            if (p.getWorld().equals(respawned.getWorld())) {
                if (respawned.getLocation().distance(p.getLocation()) <= arena.getRenderDistance()) {

                    // send respawned player to regular players
                    sendPackets(
                            p, show, head, playerVelocity,
                            new PacketPlayOutEntityEquipment(respawned.getEntityId(), list)
                    );

                    // send nearby players to respawned player
                    // if the player has invisibility hide armor
                    if (p.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                        hideArmor(p, respawned);
                    } else {
                        PacketPlayOutSpawnEntity show2 = newPacketPlayOutSpawnEntity(boundTo);

                        PacketPlayOutEntityVelocity playerVelocity2 = new PacketPlayOutEntityVelocity(boundTo);
                        PacketPlayOutEntityHeadRotation head2 = new PacketPlayOutEntityHeadRotation(boundTo, getCompressedAngle(boundTo.getBukkitYaw()));
                        sendPackets(respawned, show2, playerVelocity2, head2);
                        showArmor(p, respawned);
                    }
                }
            }
        }

        for (Player spectator : arena.getSpectators()) {
            if (spectator == null) continue;
            if (spectator.equals(respawned)) continue;
            respawned.hidePlayer(getPlugin(), spectator);
            if (spectator.getWorld().equals(respawned.getWorld())) {
                if (respawned.getLocation().distance(spectator.getLocation()) <= arena.getRenderDistance()) {

                    // send respawned player to spectator
                    sendPackets(
                            spectator, show, playerVelocity,
                            new PacketPlayOutEntityEquipment(respawned.getEntityId(), list),
                            new PacketPlayOutEntityHeadRotation(entityPlayer, getCompressedAngle(entityPlayer.getBukkitYaw()))
                    );
                }
            }
        }
    }

    @Override
    public String getInventoryName(@NotNull InventoryEvent e) {
        return e.getView().getTitle();
    }

    @Override
    public void setUnbreakable(ItemMeta itemMeta) {
        itemMeta.setUnbreakable(true);
    }

    @Override
    public int getVersion() {
        return 12;
    }

    @Override
    public void registerVersionListeners() {
        new VersionCommon(this);
    }

    @Override
    public String getMainLevel() {
        //noinspection deprecation
        return ((DedicatedServer) MinecraftServer.getServer()).a().n;
    }

    @Override
    public Fireball setFireballDirection(Fireball fireball, Vector vector) {
        fireball.setDirection(vector);
        return fireball;
    }

    @Override
    public void playRedStoneDot(Player player) {
        Color color = Color.RED;
        PacketPlayOutWorldParticles particlePacket = new PacketPlayOutWorldParticles(
                new ParticleParamRedstone(
                        new Vector3f((float) color.getRed(),
                                (float) color.getGreen(),
                                (float) color.getBlue()), (float) 1
                ),
                true,
                player.getLocation().getX(),
                player.getLocation().getY() + 2.6,
                player.getLocation().getZ(),
                0, 0, 0, 0, 0
        );
        for (Player inWorld : player.getWorld().getPlayers()) {
            if (inWorld.equals(player)) continue;
            sendPacket(inWorld, particlePacket);
        }
    }

    @Override
    public void clearArrowsFromPlayerBody(Player player) {
        // minecraft clears them on death on newer version
    }

    @Override
    public Block placeTowerBlocks(@NotNull Block b, @NotNull IArena a, @NotNull TeamColor color, int x, int y, int z) {
        b.getRelative(x, y, z).setType(color.woolMaterial());
        a.addPlacedBlock(b.getRelative(x, y, z));
        return b.getRelative(x, y, z);
    }

    @Override
    public Block placeLadder(@NotNull Block b, int x, int y, int z, @NotNull IArena a, int ladderData) {
        Block block = b.getRelative(x, y, z);  //ladder block
        block.setType(Material.LADDER);
        Ladder ladder = (Ladder) block.getBlockData();
        a.addPlacedBlock(block);
        switch (ladderData) {
            case 2 -> {
                ladder.setFacing(BlockFace.NORTH);
                block.setBlockData(ladder);
            }
            case 3 -> {
                ladder.setFacing(BlockFace.SOUTH);
                block.setBlockData(ladder);
            }
            case 4 -> {
                ladder.setFacing(BlockFace.WEST);
                block.setBlockData(ladder);
            }
            case 5 -> {
                ladder.setFacing(BlockFace.EAST);
                block.setBlockData(ladder);
            }
        }
        return b;
    }

    @Override
    public void playVillagerEffect(Player player, Location location) {
        player.spawnParticle(Particle.HAPPY_VILLAGER, location, 1);
    }

    @Override
    public void updatePacketArmorStand(GeneratorHolder generatorHolder) {
        ArmorStand armorStand = generatorHolder.getArmorStand();
        EntityArmorStand nmsEntity = ((CraftArmorStand) armorStand).getHandle();
        PacketPlayOutSpawnEntity spawn = newPacketPlayOutSpawnEntity(nmsEntity);
        PacketPlayOutEntityMetadata metadata = new PacketPlayOutEntityMetadata(armorStand.getEntityId(), ((CraftArmorStand) armorStand).getHandle().ar().c());
        Pair<EnumItemSlot, net.minecraft.world.item.ItemStack> equip = new Pair<>(EnumItemSlot.f, CraftItemStack.asNMSCopy(generatorHolder.getHelmet()));
        PacketPlayOutEntityEquipment equipment = new PacketPlayOutEntityEquipment(armorStand.getEntityId(), Collections.singletonList(equip));

        for (Player p : armorStand.getWorld().getPlayers()) {
            sendPackets(p, spawn, metadata, equipment);
        }
    }

    @Override
    public void setGeneratorHolderHelmet(GeneratorHolder generatorHolder, org.bukkit.inventory.ItemStack helmet) {
        ArmorStand armorStand = generatorHolder.getArmorStand();
        generatorHolder.setHelmet(helmet, false);
        Pair<EnumItemSlot, ItemStack> equip = new Pair<>(EnumItemSlot.f, CraftItemStack.asNMSCopy(helmet));
        PacketPlayOutEntityEquipment equipment = new PacketPlayOutEntityEquipment(armorStand.getEntityId(), Collections.singletonList(equip));
        for (Player p : armorStand.getWorld().getPlayers()) {
            sendPacket(p, equipment);
        }
    }

    @Override
    public IHologram createHologram(Player p, Location location, String... lines) {
        List<String> linesList = new ArrayList<>(Arrays.asList(lines));
        // holograms are reversed, correcting that here
        Collections.reverse(linesList);
        return new Hologram(p, location, linesList);
    }

    @Override
    public IHologram createHologram(Player p, Location location, IHoloLine... lines) {
        List<IHoloLine> linesList = new ArrayList<>(Arrays.asList(lines));
        // holograms are reversed, correcting that here
        Collections.reverse(linesList);
        return new Hologram(p, linesList, location);
    }

    @Override
    public IHoloLine lineFromText(String text, @NotNull IHologram hologram) {
        return new HoloLine(text, hologram);
    }

    @Override
    public IGeneratorAnimation createDefaultGeneratorAnimation(ArmorStand armorStand) {
        return new DefaultGenAnimation(armorStand);
    }

    @Override
    public void destroyPacketArmorStand(GeneratorHolder generatorHolder) {
        ArmorStand armorStand = generatorHolder.getArmorStand();
        PacketPlayOutEntityDestroy destroy = new PacketPlayOutEntityDestroy(armorStand.getEntityId());
        for (Player p : armorStand.getWorld().getPlayers()) {
            sendPacket(p, destroy);
        }
    }

    @Override
    public ArmorStand createPacketArmorStand(Location loc) {
        if (loc.getWorld() == null) {
            throw new RuntimeException("World of a location should not be null.");
        }
        EntityArmorStand nmsEntity = new EntityArmorStand(((CraftWorld) loc.getWorld()).getHandle(), loc.getX(), loc.getY(), loc.getZ());
        nmsEntity.p(loc.getX(), loc.getY(), loc.getZ());
        PacketPlayOutSpawnEntity spawn = newPacketPlayOutSpawnEntity(nmsEntity);

        for (Player p : loc.getWorld().getPlayers()) {
            sendPacket(p, spawn);
        }
        return new CraftArmorStand((CraftServer) getPlugin().getServer(), nmsEntity);
    }

    public static void sendPacket(Player player, Packet<?> packet) {
        ((CraftPlayer) player).getHandle().c.b(packet);
    }

    public static void sendPackets(Player player, Packet<?> @NotNull ... packets) {
        PlayerConnection connection = ((CraftPlayer) player).getHandle().c;
        for (Packet<?> p : packets) {
            connection.b(p);
        }
    }

    /**
     * Gets the NMS Item from ItemStack
     */
    private @Nullable Item getItem(org.bukkit.inventory.ItemStack itemStack) {
        var i = CraftItemStack.asNMSCopy(itemStack);
        if (null == i) {
            return null;
        }
        return i.g();
    }

    /**
     * Gets the NMS Entity from ItemStack
     */
    private @Nullable net.minecraft.world.entity.Entity getEntity(org.bukkit.inventory.ItemStack itemStack) {
        var i = CraftItemStack.asNMSCopy(itemStack);
        if (null == i) {
            return null;
        }
        return i.E();
    }

    private @Nullable NBTTagCompound getTag(@NotNull org.bukkit.inventory.ItemStack itemStack) {
        RtagItem rtagItem = new RtagItem(itemStack);
        return (NBTTagCompound) rtagItem.getTag();
    }

    public EntityPlayer getPlayer(Player player) {
        return ((CraftPlayer) player).getHandle();
    }

    public List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> getPlayerEquipment(@NotNull EntityPlayer entityPlayer) {
        List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> list = new ArrayList<>();
        list.add(new Pair<>(EnumItemSlot.a, entityPlayer.a(EnumItemSlot.a)));
        list.add(new Pair<>(EnumItemSlot.b, entityPlayer.a(EnumItemSlot.b)));
        list.add(new Pair<>(EnumItemSlot.f, entityPlayer.a(EnumItemSlot.f)));
        list.add(new Pair<>(EnumItemSlot.e, entityPlayer.a(EnumItemSlot.e)));
        list.add(new Pair<>(EnumItemSlot.d, entityPlayer.a(EnumItemSlot.d)));
        list.add(new Pair<>(EnumItemSlot.c, entityPlayer.a(EnumItemSlot.c)));

        return list;
    }

    public static PacketPlayOutSpawnEntity newPacketPlayOutSpawnEntity(net.minecraft.world.entity.Entity nmsEntity) {
        return new PacketPlayOutSpawnEntity(
                nmsEntity.hashCode(),
                nmsEntity.cz(),
                nmsEntity.dt(),
                nmsEntity.dv(),
                nmsEntity.dz(),
                nmsEntity.dG(),
                nmsEntity.getBukkitYaw(),
                nmsEntity.am(),
                0,
                nmsEntity.dr(),
                nmsEntity.ct()
        );
    }
}
