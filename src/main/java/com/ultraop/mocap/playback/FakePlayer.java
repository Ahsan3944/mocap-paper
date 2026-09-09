package com.ultraop.mocap.playback;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.UUID;

/** Server-side player entity used for MoCap playback without a real client connection. */
public final class FakePlayer extends ServerPlayer {
    private static final ClientInformation DEFAULT_CLIENT_INFO = ClientInformation.createDefault();

    private FakePlayer(ServerLevel level, GameProfile profile) {
        super(((CraftServer) org.bukkit.Bukkit.getServer()).getServer(), level, profile, DEFAULT_CLIENT_INFO);
        this.connection = new FakeConnectionHandler(((CraftServer) org.bukkit.Bukkit.getServer()).getServer(), this, profile);
        this.invulnerableTime = 0;
    }

    public static FakePlayer spawn(Location location, UUID uuid, String name) {
        return spawn(location, new GameProfile(uuid, name), 1.0);
    }

    public static FakePlayer spawn(Location location, UUID uuid, String name, double scale) {
        return spawn(location, new GameProfile(uuid, name), scale);
    }

    /**
     * Spawns a fake player with the supplied profile. The profile may contain the
     * Mojang "textures" property, which is how playback skins are applied.
     */
    public static FakePlayer spawn(Location location, GameProfile profile, double scale) {
        if (location.getWorld() == null) throw new IllegalArgumentException("Playback location has no world");
        if (profile == null) throw new IllegalArgumentException("Playback profile cannot be null");
        if (!Double.isFinite(scale) || scale <= 0.0) throw new IllegalArgumentException("Player scale must be finite and greater than zero");
        ServerLevel level = ((CraftWorld) location.getWorld()).getHandle();
        FakePlayer player = new FakePlayer(level, profile);
        player.setPos(location.getX(), location.getY(), location.getZ());
        player.setYRot(location.getYaw());
        player.setXRot(location.getPitch());
        level.addNewPlayer(player);
        player.setScale(scale);
        return player;
    }

    /** Apply the scale attribute without depending on a version-specific NMS attribute key. */
    public void setScale(double scale) {
        if (!Double.isFinite(scale) || scale <= 0.0) return;
        Player player = getBukkitEntity();
        AttributeInstance instance = player.getAttribute(Attribute.SCALE);
        if (instance != null) instance.setBaseValue(scale);
    }

    public void apply(PlayerStateAdapter frame) {
        Location location = new Location(getBukkitEntity().getWorld(), frame.x(), frame.y(), frame.z(), frame.yaw(), frame.pitch());
        Player player = getBukkitEntity();
        player.teleport(location);
        player.setVelocity(new Vector(frame.velocityX(), frame.velocityY(), frame.velocityZ()));
        player.setSprinting(frame.sprinting());
        player.setSneaking(frame.sneaking());
        player.setSwimming(frame.swimming());
        player.setGliding(frame.gliding());
        player.setFlying(frame.flying());
        player.setInvisible(frame.invisible());
        player.setGlowing(frame.glowing());
        player.setInvulnerable(frame.invulnerable());
        player.setFireTicks(frame.fireTicks());
        if (player.getHealth() > 0 && frame.health() > 0) {
            player.setHealth(Math.min(player.getMaxHealth(), frame.health()));
        }
        player.getInventory().setItemInMainHand(frame.mainHand());
        player.getInventory().setItemInOffHand(frame.offHand());
        player.getInventory().setArmorContents(frame.armor());
    }

    public void remove() {
        if (!isRemoved()) remove(RemovalReason.DISCARDED);
    }

    public interface PlayerStateAdapter {
        double x(); double y(); double z();
        float yaw(); float pitch();
        double velocityX(); double velocityY(); double velocityZ();
        boolean sprinting(); boolean sneaking(); boolean swimming(); boolean gliding(); boolean flying();
        boolean invisible(); boolean glowing(); boolean invulnerable();
        int fireTicks(); double health();
        org.bukkit.inventory.ItemStack mainHand();
        org.bukkit.inventory.ItemStack offHand();
        org.bukkit.inventory.ItemStack[] armor();
    }

    private static final class FakeConnectionHandler extends ServerGamePacketListenerImpl {
        private static final net.minecraft.network.Connection DUMMY_CONNECTION = new DummyConnection(PacketFlow.CLIENTBOUND);

        private FakeConnectionHandler(MinecraftServer server, ServerPlayer player, GameProfile profile) {
            super(server, DUMMY_CONNECTION, player, CommonListenerCookie.createInitial(profile, false));
        }

        @Override public boolean hasClientLoaded() { return true; }
        @Override public void tick() { }
        @Override public void disconnect(net.minecraft.network.chat.Component message) { }
        @Override public void send(Packet<?> packet) { }
    }

    private static final class DummyConnection extends net.minecraft.network.Connection {
        private DummyConnection(PacketFlow packetFlow) { super(packetFlow); }
    }
}
