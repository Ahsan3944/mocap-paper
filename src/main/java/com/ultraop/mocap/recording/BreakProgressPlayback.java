package com.ultraop.mocap.recording;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.craftbukkit.entity.CraftPlayer;

/** Sends recorded block-destruction progress to viewers. */
public final class BreakProgressPlayback {
    private BreakProgressPlayback(){}
    public static void send(BreakProgressFrame frame,Location location,int breakerId){
        if(frame==null||location==null||location.getWorld()==null)return;
        ClientboundBlockDestructionPacket packet=new ClientboundBlockDestructionPacket(breakerId,new BlockPos(location.getBlockX(),location.getBlockY(),location.getBlockZ()),frame.progress());
        for(Player player:Bukkit.getOnlinePlayers())if(player.getWorld()==location.getWorld()&&player.getLocation().distanceSquared(location)<=4096.0)try{((CraftPlayer)player).getHandle().connection.send(packet);}catch(Exception ignored){}
    }
}