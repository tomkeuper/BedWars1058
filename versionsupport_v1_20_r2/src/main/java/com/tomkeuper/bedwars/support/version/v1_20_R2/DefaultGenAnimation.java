package com.tomkeuper.bedwars.support.version.v1_20_R2;

import com.tomkeuper.bedwars.api.arena.generator.IGeneratorAnimation;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketListenerPlayOut;
import net.minecraft.network.protocol.game.PacketPlayOutEntity;
import net.minecraft.network.protocol.game.PacketPlayOutEntityTeleport;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftArmorStand;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class DefaultGenAnimation implements IGeneratorAnimation {
    private final Entity armorStand;
    private final Location loc;
    private int tickCount = 0; // A counter to keep track of the ticks since the animation started.

    // Constants for the sinusoidal motion
    final double frequency = 0.035; // Controls the oscillation speed.
    final double amplitude = 260; // Controls the range of YAW motion.
    final double verticalAmplitude = 10; // Controls the range of vertical motion.

    public DefaultGenAnimation(ArmorStand armorStand) {
        this.armorStand = ((CraftArmorStand) armorStand).getHandle();
        this.loc = armorStand.getLocation();
        setArmorStandYAW(0);
        setArmorStandMotY(0);
    }

    @Override
    public String getIdentifier() {
        return "bw2023:default";
    }

    @Override
    public Plugin getPlugin() {
        return Bukkit.getPluginManager().getPlugin("BedWars2023");
    }

    @Override
    public void run() {
        // Calculate sinusoidal values for YAW and MotY
        float sinusoidalYaw = (float) (Math.sin(frequency * tickCount) * amplitude);
        float sinusoidalMotY = (float) (Math.sin(frequency * tickCount) * verticalAmplitude);

        // Update the armor stand's YAW and MotY based on the sinusoidal functions
        setArmorStandYAW(sinusoidalYaw);
        setArmorStandMotY(sinusoidalMotY);

        armorStand.p(loc.getX(), loc.getY(), loc.getZ()); // SETTING NEW LOCATION
        armorStand.aJ = false; // SETTING ON GROUND TO FALSE
        PacketPlayOutEntityTeleport teleportPacket = new PacketPlayOutEntityTeleport(armorStand);
        PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook moveLookPacket = new PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook(armorStand.ah(), (byte) 0, (byte) getArmorStandMotY(), (byte) 0, (byte) getArmorStandYAW(), (byte) 0, false);

        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            sendPackets(p, teleportPacket, moveLookPacket);
        }
        tickCount++;
    }

    private void sendPacket(Player p, Packet<PacketListenerPlayOut> packet) {
        ((CraftPlayer) p).getHandle().c.a(packet);
    }

    @SafeVarargs
    private void sendPackets(Player p, Packet<PacketListenerPlayOut>... packets) {
        PlayerConnection connection = ((CraftPlayer) p).getHandle().c;
        for (Packet<PacketListenerPlayOut> packet : packets) {
            connection.a(packet);
        }
    }

    private void setArmorStandYAW(float yaw) {
        armorStand.r(yaw);
    }

    private void addArmorStandYAW(float yaw) {
        armorStand.r(getArmorStandYAW() + yaw);
    }

    private float getArmorStandYAW() {
        return armorStand.dB();
    }

    private void setArmorStandMotY(double y) {
        armorStand.f(new Vec3D(0, y, 0));
    }

    private void addArmorStandMotY(double y) {
        armorStand.f(new Vec3D(0, getArmorStandMotY() + y, 0));
    }

    private double getArmorStandMotY() {
        // THE METHOD IS IMPOSSIBLE TO ACCESS DUE TO BEING CALLED A PRIMITIVE TYPE (in this case "do") SO WE HAVE TO USE REFLECTION
        String motVectorMethod = "do";
        try {
            return ((Vec3D) armorStand.getClass().getMethod(motVectorMethod).invoke(armorStand)).d;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
