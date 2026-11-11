package com.enderdevelopment.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class Main extends JavaPlugin implements Listener {
    private final BalanceStore balanceStore = new BalanceStore(this);
    private final Set<UUID> mazePlayers = new HashSet<>();
    private final Set<UUID> mazeMobs = new HashSet<>();
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> collected = new HashMap<>();
    private final Map<UUID, Location> returnPoints = new HashMap<>();
    private NamespacedKey itemKey;
    private Location mazeOrigin;
    private static final int SIZE = 21;

    @Override
    public void onEnable() {
        balanceStore.load();
        itemKey = new NamespacedKey(this, "maze_item");
        Bukkit.getPluginManager().registerEvents(this, this);
        World world = Bukkit.getWorlds().get(0);
        mazeOrigin = world.getSpawnLocation().getBlock().getLocation().add(40, 0, 0);
        buildMaze();
        getLogger().info("MazeCoins enabled. Use /maze and /shop.");
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (mazePlayers.contains(player.getUniqueId())) returnFromMaze(player);
        }
        balanceStore.save();
    }

    private void buildMaze() {
        World world = mazeOrigin.getWorld();
        int baseX = mazeOrigin.getBlockX(), baseY = mazeOrigin.getBlockY(), baseZ = mazeOrigin.getBlockZ();
        for (int x = -1; x <= SIZE; x++) for (int z = -1; z <= SIZE; z++) {
            for (int y = 0; y < 4; y++) world.getBlockAt(baseX + x, baseY + y, baseZ + z).setType(y == 0 ? Material.STONE_BRICKS : Material.AIR);
            if (x == -1 || z == -1 || x == SIZE || z == SIZE) for (int y = 1; y <= 3; y++) world.getBlockAt(baseX + x, baseY + y, baseZ + z).setType(Material.MOSSY_STONE_BRICKS);
        }
        Random random = new Random(37);
        for (int x = 2; x < SIZE - 1; x += 2) for (int z = 0; z < SIZE; z++) {
            if (random.nextInt(4) == 0) continue;
            for (int y = 1; y <= 2; y++) world.getBlockAt(baseX + x, baseY + y, baseZ + z).setType(Material.STONE_BRICKS);
            int opening = random.nextInt(SIZE);
            world.getBlockAt(baseX + x, baseY + 1, baseZ + opening).setType(Material.AIR);
            world.getBlockAt(baseX + x, baseY + 2, baseZ + opening).setType(Material.AIR);
        }
        world.getBlockAt(baseX, baseY + 1, baseZ).setType(Material.AIR);
        world.getBlockAt(baseX, baseY + 2, baseZ).setType(Material.AIR);
        world.getBlockAt(baseX + SIZE - 1, baseY + 1, baseZ + SIZE - 1).setType(Material.GOLD_BLOCK);
        world.getBlockAt(baseX + SIZE - 1, baseY + 2, baseZ + SIZE - 1).setType(Material.GOLD_BLOCK);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("exitserver")) {
            if (sender instanceof Player player) player.kick(Component.text("You have left the server.", NamedTextColor.YELLOW));
            else sender.sendMessage("This command is for players.");
            return true;
        }
        if (!(sender instanceof Player player)) { sender.sendMessage("This command is for players."); return true; }
        UUID id = player.getUniqueId();
        switch (command.getName().toLowerCase()) {
            case "maze" -> enterMaze(player);
            case "shop" -> openShop(player);
            case "minecoins" -> player.sendMessage(Component.text("Minecoins: " + balanceStore.get(id), NamedTextColor.GOLD));
            default -> { return false; }
        }
        return true;
    }

    private void enterMaze(Player player) {
        UUID id = player.getUniqueId();
        returnPoints.put(id, player.getLocation());
        mazePlayers.add(id);
        kills.put(id, 0);
        collected.put(id, 0);
        player.teleport(mazeOrigin.clone().add(0.5, 1, 0.5));
        player.sendMessage(Component.text("Maze task: defeat 3 monsters, collect 3 emeralds, then reach the gold exit.", NamedTextColor.GREEN));
        EntityType[] types = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.ZOMBIE};
        Random random = new Random();
        for (EntityType type : types) {
            int x = 3 + random.nextInt(SIZE - 6), z = 3 + random.nextInt(SIZE - 6);
            var mob = mazeOrigin.getWorld().spawnEntity(mazeOrigin.clone().add(x + 0.5, 1, z + 0.5), type);
            mazeMobs.add(mob.getUniqueId());
        }
        for (int i = 0; i < 3; i++) {
            int x = 2 + random.nextInt(SIZE - 4), z = 2 + random.nextInt(SIZE - 4);
            mazeOrigin.getWorld().dropItemNaturally(mazeOrigin.clone().add(x + 0.5, 1, z + 0.5), new ItemStack(Material.EMERALD));
        }
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        UUID mobId = event.getEntity().getUniqueId();
        if (!mazeMobs.remove(mobId) || event.getEntity().getKiller() == null) return;
        Player killer = event.getEntity().getKiller();
        if (!mazePlayers.contains(killer.getUniqueId())) return;
        int count = kills.merge(killer.getUniqueId(), 1, Integer::sum);
        killer.sendMessage(Component.text("Maze monsters defeated: " + count + "/3", NamedTextColor.GREEN));
    }

    @EventHandler
    public void onEmeraldPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player) || !mazePlayers.contains(player.getUniqueId())) return;
        if (event.getItem().getItemStack().getType() != Material.EMERALD) return;
        int count = collected.merge(player.getUniqueId(), 1, Integer::sum);
        player.sendMessage(Component.text("Emeralds collected: " + count + "/3", NamedTextColor.AQUA));
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (!mazePlayers.contains(id) || event.getTo() == null) return;
        Block block = event.getTo().getBlock();
        int dx = block.getX() - mazeOrigin.getBlockX(), dz = block.getZ() - mazeOrigin.getBlockZ();
        if (dx == SIZE - 1 && dz == SIZE - 1) {
            if (kills.getOrDefault(id, 0) >= 3 && collected.getOrDefault(id, 0) >= 3) {
                balanceStore.add(id, 25);
                player.sendMessage(Component.text("Maze complete! You earned 25 Minecoins. Total: " + balanceStore.get(id), NamedTextColor.GOLD));
                returnFromMaze(player);
            } else player.sendMessage(Component.text("Finish the tasks first: defeat 3 monsters and collect 3 emeralds.", NamedTextColor.RED));
        }
    }

    private void returnFromMaze(Player player) {
        Location destination = returnPoints.remove(player.getUniqueId());
        mazePlayers.remove(player.getUniqueId());
        kills.remove(player.getUniqueId());
        collected.remove(player.getUniqueId());
        if (destination != null && player.isOnline()) player.teleport(destination);
    }

    private ItemStack taggedItem(Material material, String title, String key) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(title, NamedTextColor.AQUA));
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, key);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void openShop(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, Component.text("Minecoins Shop", NamedTextColor.GOLD));
        inventory.setItem(10, taggedItem(Material.ENDER_PEARL, "Maze Blink - 10 Minecoins", "blink"));
        inventory.setItem(13, taggedItem(Material.GOLDEN_APPLE, "Maze Snack - 8 Minecoins", "snack"));
        inventory.setItem(16, taggedItem(Material.IRON_SWORD, "Maze Blade - 15 Minecoins", "blade"));
        inventory.setItem(22, taggedItem(Material.GOLD_INGOT, "Balance: " + balanceStore.get(player.getUniqueId()) + " Minecoins", "balance"));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onShopClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !event.getView().title().equals(Component.text("Minecoins Shop", NamedTextColor.GOLD))) return;
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        String key = item.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
        if (key == null || key.equals("balance")) return;
        int cost = switch (key) { case "blink" -> 10; case "snack" -> 8; case "blade" -> 15; default -> 0; };
        UUID id = player.getUniqueId();
        if (cost == 0) return;
        if (!balanceStore.spend(id, cost)) {
            player.sendMessage(Component.text("Not enough Minecoins. Complete the maze to earn more.", NamedTextColor.RED));
            return;
        }
        Material material = switch (key) { case "blink" -> Material.ENDER_PEARL; case "snack" -> Material.GOLDEN_APPLE; default -> Material.IRON_SWORD; };
        player.getInventory().addItem(new ItemStack(material));
        player.sendMessage(Component.text("Purchased " + key + " for " + cost + " Minecoins.", NamedTextColor.GREEN));
        openShop(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        mazePlayers.remove(event.getPlayer().getUniqueId());
        kills.remove(event.getPlayer().getUniqueId());
        collected.remove(event.getPlayer().getUniqueId());
        returnPoints.remove(event.getPlayer().getUniqueId());
    }
}
