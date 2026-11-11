package com.enderdevelopment.plugin;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class BalanceStore {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Integer> balances = new HashMap<>();

    BalanceStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "balances.yml");
    }

    void load() {
        if (!file.exists()) return;
        YamlConfiguration data = new YamlConfiguration();
        try {
            data.load(file);
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().severe("Could not load Minecoins balances: " + exception.getMessage());
            return;
        }
        for (String key : data.getKeys(false)) {
            try {
                int balance = data.getInt(key, -1);
                if (balance >= 0) balances.put(UUID.fromString(key), balance);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Ignoring invalid Minecoins entry: " + key);
            }
        }
    }

    int get(UUID playerId) {
        return balances.getOrDefault(playerId, 0);
    }

    void add(UUID playerId, int amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");
        balances.merge(playerId, amount, Math::addExact);
        save();
    }

    boolean spend(UUID playerId, int amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");
        int balance = get(playerId);
        if (balance < amount) return false;
        balances.put(playerId, balance - amount);
        save();
        return true;
    }

    void save() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().severe("Could not create the plugin data folder; Minecoins were not saved.");
            return;
        }
        YamlConfiguration data = new YamlConfiguration();
        balances.forEach((playerId, balance) -> data.set(playerId.toString(), balance));
        try {
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save Minecoins balances: " + exception.getMessage());
        }
    }
}
