package pl.minestar.skypvp.extras;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ExtrasPlugin extends JavaPlugin implements Listener, CommandExecutor {
    private final List<Generator> generators = new ArrayList<>();
    private NamespacedKey menuKey;

    private static final class Generator {
        final Location location;
        final Material material;
        final int seconds;
        int ticks;
        Generator(Location location, Material material, int seconds) {
            this.location = location;
            this.material = material;
            this.seconds = seconds;
            this.ticks = seconds * 20;
        }
    }

    private static final class MenuHolder implements InventoryHolder {
        private Inventory inventory;
        @Override public Inventory getInventory() { return inventory; }
    }

    private record ArmorSet(String id, String name, Color color, Material icon) { }

    private final List<ArmorSet> sets = List.of(
            new ArmorSet("iron", "&fZelazny set", Color.fromRGB(180, 185, 190), Material.IRON_CHESTPLATE),
            new ArmorSet("ruby", "&cRubinowy set", Color.fromRGB(200, 35, 45), Material.REDSTONE),
            new ArmorSet("ocean", "&bOceaniczny set", Color.fromRGB(35, 135, 205), Material.LAPIS_LAZULI),
            new ArmorSet("emerald", "&aSzmaragdowy set", Color.fromRGB(35, 175, 85), Material.EMERALD),
            new ArmorSet("gold", "&6Zloty set", Color.fromRGB(235, 175, 35), Material.GOLD_INGOT),
            new ArmorSet("shadow", "&8Cienisty set", Color.fromRGB(55, 45, 70), Material.AMETHYST_SHARD)
    );

    @Override public void onEnable() {
        saveDefaultConfig();
        menuKey = new NamespacedKey(this, "armor_set");
        loadGenerators();
        for (String command : List.of("kit", "sety", "generator")) {
            if (getCommand(command) != null) getCommand(command).setExecutor(this);
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        new BukkitRunnable() {
            @Override public void run() { tickGenerators(); }
        }.runTaskTimer(this, 20L, 1L);
        getLogger().info("SkyPvP extras (generatory, kity, sety) uruchomione.");
    }

    private void loadGenerators() {
        var section = getConfig().getConfigurationSection("generators");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            Location location = getConfig().getLocation("generators." + id + ".location");
            Material material = Material.matchMaterial(getConfig().getString("generators." + id + ".material", "IRON_INGOT"));
            int seconds = Math.max(1, getConfig().getInt("generators." + id + ".seconds", 10));
            if (location != null && location.getWorld() != null && material != null) {
                generators.add(new Generator(location, material, seconds));
            }
        }
    }

    private void saveGenerators() {
        getConfig().set("generators", null);
        for (int i = 0; i < generators.size(); i++) {
            Generator generator = generators.get(i);
            String path = "generators." + i;
            getConfig().set(path + ".location", generator.location);
            getConfig().set(path + ".material", generator.material.name());
            getConfig().set(path + ".seconds", generator.seconds);
        }
        saveConfig();
    }

    private void tickGenerators() {
        for (Generator generator : generators) {
            if (--generator.ticks > 0) continue;
            generator.ticks = generator.seconds * 20;
            Location drop = generator.location.clone().add(0.5, 1.1, 0.5);
            Item item = generator.location.getWorld().dropItem(drop, new ItemStack(generator.material));
            item.setPickupDelay(10);
        }
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "kit" -> kitCommand(sender, args);
            case "sety" -> setCommand(sender);
            case "generator" -> generatorCommand(sender, args);
            default -> false;
        };
    }

    private boolean kitCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Tej komendy uzywa gracz."); return true; }
        if (args.length != 1) { player.sendMessage("§eUzycie: /kit <11-20> — miecz Sharpness XI-XX"); return true; }
        int level;
        try { level = Integer.parseInt(args[0]); }
        catch (NumberFormatException exception) { player.sendMessage("§cWybierz poziom od 11 do 20."); return true; }
        if (level < 11 || level > 20) { player.sendMessage("§cWybierz poziom od 11 do 20."); return true; }

        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = sword.getItemMeta();
        meta.setDisplayName("§c§lMiecz SkyPvP §7Sharpness " + level);
        meta.addEnchant(Enchantment.SHARPNESS, level, true);
        sword.setItemMeta(meta);
        give(player, sword);
        give(player, new ItemStack(Material.COOKED_BEEF, 16));
        player.sendMessage("§aOtrzymales kit z mieczem Sharpness " + level + ".");
        return true;
    }

    private boolean setCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Tej komendy uzywa gracz."); return true; }
        MenuHolder holder = new MenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27, "§3§lSETY PVP");
        holder.inventory = inventory;
        int[] slots = {10, 11, 12, 13, 14, 15};
        for (int i = 0; i < sets.size(); i++) {
            ArmorSet set = sets.get(i);
            ItemStack icon = new ItemStack(set.icon());
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName(color(set.name()));
            meta.setLore(List.of("§7Kliknij, aby otrzymac zestaw", "§8Barwiona skorzana zbroja"));
            meta.getPersistentDataContainer().set(menuKey, PersistentDataType.STRING, set.id());
            icon.setItemMeta(meta);
            inventory.setItem(slots[i], icon);
        }
        player.openInventory(inventory);
        return true;
    }

    @EventHandler public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof MenuHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || !clicked.hasItemMeta()) return;
        String id = clicked.getItemMeta().getPersistentDataContainer().get(menuKey, PersistentDataType.STRING);
        if (id == null) return;
        for (ArmorSet set : sets) {
            if (set.id().equals(id)) {
                giveSet(player, set);
                player.sendMessage("§aOtrzymales: " + color(set.name()));
                player.closeInventory();
                return;
            }
        }
    }

    private void giveSet(Player player, ArmorSet set) {
        give(player, armor(Material.LEATHER_HELMET, set));
        give(player, armor(Material.LEATHER_CHESTPLATE, set));
        give(player, armor(Material.LEATHER_LEGGINGS, set));
        give(player, armor(Material.LEATHER_BOOTS, set));
    }

    private ItemStack armor(Material material, ArmorSet set) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(set.color());
        meta.setDisplayName(color(set.name()));
        item.setItemMeta(meta);
        return item;
    }

    private void give(Player player, ItemStack item) {
        var leftovers = player.getInventory().addItem(item);
        leftovers.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
    }

    private boolean generatorCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("skypvp.admin")) { sender.sendMessage("§cBrak uprawnien."); return true; }
        if (args.length == 0) {
            sender.sendMessage("§e/generator set <iron|gold|diamond> [sekundy]");
            sender.sendMessage("§e/generator remove | list");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "set" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage("Tej komendy uzywa gracz."); return true; }
                if (args.length < 2) { sender.sendMessage("§c/generator set <iron|gold|diamond> [sekundy]"); return true; }
                Material material = switch (args[1].toLowerCase(Locale.ROOT)) {
                    case "iron" -> Material.IRON_INGOT;
                    case "gold" -> Material.GOLD_INGOT;
                    case "diamond" -> Material.DIAMOND;
                    default -> null;
                };
                if (material == null) { sender.sendMessage("§cTypy: iron, gold, diamond."); return true; }
                int seconds = 10;
                if (args.length >= 3) {
                    try { seconds = Math.max(1, Math.min(300, Integer.parseInt(args[2]))); }
                    catch (NumberFormatException exception) { sender.sendMessage("§cCzas musi byc liczba sekund."); return true; }
                }
                Location location = player.getLocation().getBlock().getLocation();
                generators.add(new Generator(location, material, seconds));
                saveGenerators();
                sender.sendMessage("§aUstawiono generator " + args[1].toLowerCase(Locale.ROOT) + " co " + seconds + " s.");
                return true;
            }
            case "remove" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage("Tej komendy uzywa gracz."); return true; }
                Location here = player.getLocation().getBlock().getLocation();
                Generator nearest = generators.stream().filter(g -> g.location.getWorld().equals(here.getWorld()))
                        .filter(g -> g.location.distanceSquared(here) <= 4).findFirst().orElse(null);
                if (nearest == null) { sender.sendMessage("§cNie ma generatora w poblizu."); return true; }
                generators.remove(nearest);
                saveGenerators();
                sender.sendMessage("§aUsunieto generator.");
                return true;
            }
            case "list" -> {
                sender.sendMessage("§bGeneratory: " + generators.size());
                for (Generator generator : generators) sender.sendMessage("§7- " + generator.material + " @ "
                        + generator.location.getWorld().getName() + " " + generator.location.getBlockX() + ","
                        + generator.location.getBlockY() + "," + generator.location.getBlockZ()
                        + " co " + generator.seconds + " s");
                return true;
            }
            default -> { sender.sendMessage("§cUzyj /generator, aby zobaczyc pomoc."); return true; }
        }
    }

    private String color(String text) { return ChatColor.translateAlternateColorCodes('&', text); }
}
