package com.ultraop.mocap.playback;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.ProfileLookupCallback;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;

/** Uses the Minecraft server's bundled authlib services for profile lookup/filling. */
public final class AuthlibProfileResolver {
    private AuthlibProfileResolver() {}

    public static GameProfile resolve(GameProfile profile) {
        if (profile == null) return null;
        if (profile.getName() == null || profile.getName().isBlank()) return profile;
        try {
            CraftServer craftServer = (CraftServer) Bukkit.getServer();
            var server = craftServer.getServer();
            GameProfile[] result = new GameProfile[1];
            server.getProfileRepository().findProfilesByNames(
                    new String[]{profile.getName()},
                    new ProfileLookupCallback() {
                        @Override
                        public void onProfileLookupSucceeded(GameProfile found) {
                            result[0] = found;
                        }

                        @Override
                        public void onProfileLookupFailed(String name, Exception exception) {
                            // Fall back to the supplied profile below.
                        }
                    });
            GameProfile resolved = result[0] != null ? result[0] : profile;
            return server.getSessionService().fillProfileProperties(resolved, true);
        } catch (Exception ignored) {
            return profile;
        }
    }
}
