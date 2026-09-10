package com.ultraop.mocap.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;

/** Cached tab-completion entries for MoCap skin files and skin lists. */
public final class SkinSuggestionCache {
    private final List<String> values = new ArrayList<>();
    public synchronized void refresh() {
        values.clear();
        Path root = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0).getWorldFolder().toPath().resolve("mocap_files/skins");
        if (root == null) return;
        addDirectory(root, "");
        addDirectory(root.resolve("slim"), "slim/");
        addDirectory(root.resolve("list"), "list/");
    }
    private void addDirectory(Path dir, String prefix) {
        if (!Files.isDirectory(dir)) return;
        try (var stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile).filter(p -> p.getFileName().toString().toLowerCase().endsWith(".png") || (prefix.equals("list/") && p.getFileName().toString().toLowerCase().endsWith(".txt")))
                    .forEach(p -> values.add(prefix + p.getFileName().toString().replaceFirst("\\.[^.]+$", "")));
        } catch (IOException ignored) { }
    }
    public synchronized List<String> get(String input) { String lower = input == null ? "" : input.toLowerCase(); return Collections.unmodifiableList(values.stream().filter(v -> v.toLowerCase().startsWith(lower)).sorted().toList()); }
    public synchronized void clear() { values.clear(); }
}
