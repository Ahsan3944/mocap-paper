package com.ultraop.mocap.playback;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.exceptions.MinecraftClientHttpException;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.authlib.yggdrasil.response.NameAndId;
import com.mojang.util.UndashedUuid;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.util.StringUtil;

import java.net.Proxy;
import java.util.Locale;
import java.util.UUID;

/** Profile lookup compatible with the upstream MoCap profile-loading behavior. */
public final class ProfileUtils {
    private static final MinecraftClient CLIENT = new MinecraftClient(null, Proxy.NO_PROXY);
    private static final int CACHE_SIZE = 4096;
    private static final java.util.LinkedHashMap<String, Profile> CACHE = new java.util.LinkedHashMap<>(128, 0.75f, true) {
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<String, Profile> eldest) { return size() > CACHE_SIZE; }
    };
    private static final java.util.LinkedHashMap<String, Profile> CACHE_INSENSITIVE = new java.util.LinkedHashMap<>(128, 0.75f, true) {
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<String, Profile> eldest) { return size() > CACHE_SIZE; }
    };

    private ProfileUtils() {}

    public static synchronized Profile getProfile(MinecraftServer server, String mode, String name, boolean withSkin, boolean useAuthlibServices) {
        if ("disable_loading_profiles".equalsIgnoreCase(mode)) return Profile.withoutSkin(name);
        Profile cached = getFromCache(mode, name, withSkin);
        if (cached != null) return cached;
        if (!StringUtil.isValidPlayerName(name)) return cacheAndReturn(Profile.withoutSkin(name));

        Services services = server.services();
        NameAndIdResult result = fetchNameAndId(services, name, useAuthlibServices);
        if (result.value() == null) {
            return cacheAndReturn(result.rateLimited() ? Profile.rateLimited(name) : Profile.withoutSkin(name));
        }

        NameAndId nameAndId = result.value();
        if ("match_exact_name".equalsIgnoreCase(mode) && !name.equals(nameAndId.name())) {
            cacheAndReturn(Profile.fromPartialFetch(nameAndId));
            Profile exact = Profile.withoutSkin(name);
            CACHE.put(name, exact);
            return exact;
        }

        Profile profile = cacheAndReturn(withSkin
                ? fetchFullProfile(services, nameAndId, useAuthlibServices)
                : Profile.fromPartialFetch(nameAndId));
        return "ignore_casing".equalsIgnoreCase(mode) ? profile.withName(name) : profile;
    }

    public static synchronized void clearCache() {
        CACHE.clear();
        CACHE_INSENSITIVE.clear();
    }

    public static GameProfile toGameProfile(Profile profile) {
        Multimap<String, Property> properties = HashMultimap.create();
        if (profile.skin() != null) properties.put(profile.skin().name(), profile.skin());
        return new GameProfile(UUID.randomUUID(), profile.name(), new PropertyMap(properties));
    }

    public static GameProfile createGameProfile(String name, Property skin) {
        Multimap<String, Property> properties = HashMultimap.create();
        if (skin != null) properties.put(skin.name(), skin);
        return new GameProfile(UUID.randomUUID(), name, new PropertyMap(properties));
    }

    private static NameAndIdResult fetchNameAndId(Services services, String name, boolean useAuthlibServices) {
        if (useAuthlibServices) {
            return new NameAndIdResult(services.profileRepository().findProfileByName(name).orElse(null), false);
        }
        try {
            return new NameAndIdResult(CLIENT.get(
                    HttpAuthenticationService.constantURL("https://api.mojang.com/users/profiles/minecraft/" + name.toLowerCase(Locale.ROOT)),
                    NameAndId.class), false);
        } catch (MinecraftClientException e) {
            boolean rateLimited = e instanceof MinecraftClientHttpException http && http.getStatus() == 429;
            return new NameAndIdResult(null, rateLimited);
        }
    }

    private static Profile fetchFullProfile(Services services, NameAndId nameAndId, boolean useAuthlibServices) {
        if (useAuthlibServices) {
            ProfileResult result = services.sessionService().fetchProfile(nameAndId.id(), true);
            return result != null
                    ? Profile.withSkin(nameAndId.name(), services.sessionService().getPackedTextures(result.profile()))
                    : Profile.withoutSkin(nameAndId.name());
        }
        try {
            MinecraftProfilePropertiesResponse response = CLIENT.get(
                    HttpAuthenticationService.constantURL("https://sessionserver.mojang.com/session/minecraft/profile/"
                            + UndashedUuid.toString(nameAndId.id()) + "?unsigned=false"),
                    MinecraftProfilePropertiesResponse.class);
            Property skin = response == null ? null : response.properties().get("textures").stream().findFirst().orElse(null);
            return response != null ? Profile.withSkin(nameAndId.name(), skin) : Profile.withoutSkin(nameAndId.name());
        } catch (MinecraftClientException e) {
            boolean rateLimited = e instanceof MinecraftClientHttpException http && http.getStatus() == 429;
            return rateLimited ? Profile.rateLimited(nameAndId.name()) : Profile.withoutSkin(nameAndId.name());
        }
    }

    private static Profile getFromCache(String mode, String name, boolean withSkin) {
        Profile profile;
        synchronized (ProfileUtils.class) {
            profile = switch (mode.toLowerCase(Locale.ROOT)) {
                case "ignore_casing", "ignore_and_replace_casing" -> CACHE_INSENSITIVE.get(name.toLowerCase(Locale.ROOT));
                case "match_exact_name" -> CACHE.get(name);
                default -> null;
            };
        }
        if (profile == null || (profile.partialFetch() && withSkin)) return null;
        if (profile.skin() != null && !withSkin) profile = profile.withoutSkin();
        return "ignore_casing".equalsIgnoreCase(mode) ? profile.withName(name) : profile;
    }

    private static Profile cacheAndReturn(Profile profile) {
        if (!profile.rateLimited()) {
            Profile previous = CACHE.get(profile.name());
            if (previous == null || previous.partialFetch()) CACHE.put(profile.name(), profile);
            String lower = profile.name().toLowerCase(Locale.ROOT);
            Profile previousInsensitive = CACHE_INSENSITIVE.get(lower);
            if (previousInsensitive == null || previousInsensitive.partialFetch()) CACHE_INSENSITIVE.put(lower, profile);
        }
        return profile;
    }

    private record NameAndIdResult(NameAndId value, boolean rateLimited) {}

    public record Profile(String name, Property skin, boolean rateLimited, boolean partialFetch) {
        static Profile withoutSkin(String name) { return new Profile(name, null, false, false); }
        static Profile withSkin(String name, Property skin) { return new Profile(name, skin, false, false); }
        static Profile rateLimited(String name) { return new Profile(name, null, true, false); }
        static Profile fromPartialFetch(NameAndId id) { return new Profile(id.name(), null, false, true); }
        Profile withName(String name) { return new Profile(name, skin, rateLimited, partialFetch); }
        Profile withoutSkin() { return new Profile(name, null, rateLimited, partialFetch); }
    }
}
