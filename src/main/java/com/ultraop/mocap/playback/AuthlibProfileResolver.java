package com.ultraop.mocap.playback;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.ProfileLookupCallback;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;

import java.lang.reflect.Method;
import java.util.UUID;

/** Uses the Minecraft server's bundled authlib services without depending on unstable NMS accessor names. */
public final class AuthlibProfileResolver {
    private AuthlibProfileResolver() {}

    public static GameProfile resolve(GameProfile profile) {
        if (profile == null || profile.name() == null || profile.name().isBlank()) return profile;
        try {
            CraftServer craftServer = (CraftServer) Bukkit.getServer();
            Object server = craftServer.getServer();

            UUID[] resolvedId = new UUID[1];
            ProfileLookupCallback callback = new ProfileLookupCallback() {
                @Override
                public void onProfileLookupSucceeded(String name, UUID id) {
                    resolvedId[0] = id;
                }

                @Override
                public void onProfileLookupFailed(String name, Exception exception) {
                    // Fall back to the supplied profile below.
                }
            };

            Method repositoryMethod = server.getClass().getMethod("getProfileRepository");
            Object repository = repositoryMethod.invoke(server);
            Method lookup = repository.getClass().getMethod("findProfilesByNames", String[].class, ProfileLookupCallback.class);
            lookup.invoke(repository, new Object[]{new String[]{profile.name()}, callback});

            UUID id = resolvedId[0] != null ? resolvedId[0] : profile.id();
            GameProfile resolved = id == null ? profile : new GameProfile(id, profile.name());

            Method sessionMethod = server.getClass().getMethod("getSessionService");
            Object sessionService = sessionMethod.invoke(server);
            Method fill = sessionService.getClass().getMethod("fillProfileProperties", GameProfile.class, boolean.class);
            Object filled = fill.invoke(sessionService, resolved, true);
            return filled instanceof GameProfile result ? result : resolved;
        } catch (Exception ignored) {
            return profile;
        }
    }
}
