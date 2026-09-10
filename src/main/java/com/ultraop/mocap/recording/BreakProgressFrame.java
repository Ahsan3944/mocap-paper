package com.ultraop.mocap.recording;

import org.bukkit.Location;

/** A server-side block breaking progress update recorded for the source player. */
public record BreakProgressFrame(long tick,String worldKey,int x,int y,int z,int progress){
    public static BreakProgressFrame create(long tick,Location location,int progress){return new BreakProgressFrame(tick,location.getWorld().getKey().toString(),location.getBlockX(),location.getBlockY(),location.getBlockZ(),Math.max(0,Math.min(10,progress)));}
}
