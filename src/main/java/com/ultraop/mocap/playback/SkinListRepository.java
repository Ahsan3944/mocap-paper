package com.ultraop.mocap.playback;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Loads MoCap-compatible skin lists from world/mocap_files/skins/list/*.txt. */
public final class SkinListRepository {
    private SkinListRepository() {}

    public static PlayerSkin loadRandom(Path worldFolder, String listPath, Random random) {
        if (worldFolder == null || listPath == null || random == null) return null;
        String name = listPath.startsWith("list/") ? listPath.substring("list/".length()) : listPath;
        if (name.isBlank() || !name.matches("[A-Za-z0-9._-]+")) return null;
        Path file = worldFolder.resolve("mocap_files").resolve("skins").resolve("list").resolve(name + ".txt").normalize();
        Path root = worldFolder.resolve("mocap_files").resolve("skins").resolve("list").normalize();
        try { Files.createDirectories(root); } catch (IOException ignored) { return null; }
        if (!file.startsWith(root) || !Files.isRegularFile(file)) return null;
        return resolve(file, root, random, new ArrayList<>());
    }

    private static PlayerSkin resolve(Path file, Path root, Random random, List<String> stack) {
        String key = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (stack.contains(key)) return null;
        stack.add(key);
        try {
            List<PlayerSkin> skins = new ArrayList<>();
            for (String raw : Files.readAllLines(file)) {
                String line = raw.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(" ", 2);
                if (parts.length != 2) return null;
                PlayerSkin.Source source;
                try { source = PlayerSkin.Source.valueOf(parts[0].toUpperCase(Locale.ROOT)); }
                catch (IllegalArgumentException ignored) { return null; }
                try { skins.add(new PlayerSkin(source, parts[1])); }
                catch (IllegalArgumentException ignored) { return null; }
            }
            if (skins.isEmpty()) return null;
            PlayerSkin selected = skins.get(random.nextInt(skins.size()));
            if (!selected.isSkinList()) return selected;
            String nested = selected.path().substring("list/".length());
            if (!nested.matches("[A-Za-z0-9._-]+")) return null;
            Path nestedFile = root.resolve(nested + ".txt").normalize();
            if (!nestedFile.startsWith(root)) return null;
            return resolve(nestedFile, root, random, stack);
        } catch (IOException ignored) {
            return null;
        } finally {
            stack.remove(key);
        }
    }
}
