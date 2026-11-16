package pl.minestar.skypvp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Main extends JavaPlugin implements Listener, CommandExecutor {
    private enum EventType {
        DUEL("1v1"), FFA("ffa");
        final String id;
        EventType(String id) { this.id = id; }
        static EventType parse(String value) {
            for (EventType type : values()) if (type.id.equalsIgnoreCase(value)) return type;
            return null;
        }
    }

    private static final class Arena {
        final String name;
        final EventType type;
        final List<Location> spawns = new ArrayList<>();
        final LinkedHashSet<UUID> joined = new LinkedHashSet<>();
        final LinkedHashSet<UUID> alive = new LinkedHashSet<>();
        boolean running;
        BukkitTask countdown;
        Arena(String name, EventType type) { this.name = name; this.type = type; }
    }

    private static final class CostumeMenu implements InventoryHolder {
        private Inventory inventory;
        @Override public Inventory getInventory() { return inventory; }
    }

    private record Costume(String id, String label, Color color, Material icon) { }

    private final Map<String, Arena> arenas = new HashMap<>();
    private final Map<UUID, Location> returnLocations = new HashMap<>();
    private final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();
    private final Map<UUID, Costume> selectedCostumes = new HashMap<>();
    private final List<Costume> costumes = List.of(
            new Costume("ruby", "&c&lRubinowy kostium", Color.fromRGB(205, 35, 55), Material.RED_DYE),
            new Costume("ocean", "&b&lOceaniczny kostium", Color.fromRGB(35, 150, 210), Material.LIGHT_BLUE_DYE),
            new Costume("emerald", "&a&lSzmaragdowy kostium", Color.fromRGB(35, 180, 95), Material.GREEN_DYE),
            new Costume("gold", "&6&lZloty kostium", Color.fromRGB(245, 180, 35), Material.GOLD_INGOT),
            new Costume("shadow", "&8&lCienisty kostium", Color.fromRGB(55, 45, 75), Material.INK_SAC)
    );
    private Location lobbySpawn;
    private Location pvpSpawn;
    private Arena activeArena;
    private NamespacedKey costumeKey;

    @Override public void onEnable() {
        saveDefaultConfig();
        costumeKey = new NamespacedKey(this, "costume_piece");
        lobbySpawn = getConfig().getLocation("locations.lobby");
        pvpSpawn = getConfig().getLocation("locations.pvp");
        loadArenas();
        for (String name : List.of("spawn", "setspawn", "pvp", "setpvp", "kostiumy", "event")) {
            if (getCommand(name) != null) getCommand(name).setExecutor(this);
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("SkyPvP uruchomiony.");
    }

    @Override public void onDisable() {
        if (activeArena != null) finishArena(activeArena, null, false);
        for (Player player : Bukkit.getOnlinePlayers()) restoreArmor(player);
    }

    private void loadArenas() {
        var section = getConfig().getConfigurationSection("arenas");
        if (section == null) return;
        for (String name : section.getKeys(false)) {
            String path = "arenas." + name;
            EventType type = EventType.parse(getConfig().getString(path + ".type", ""));
            if (type == null) continue;
            Arena arena = new Arena(name, type);
            for (Location location : getConfig().getLocationList(path + ".spawns")) {
                if (valid(location)) arena.spawns.add(location);
            }
            arenas.put(name.toLowerCase(Locale.ROOT), arena);
        }
    }

    private void saveArena(Arena arena) {
        String path = "arenas." + arena.name;
        getConfig().set(path + ".type", arena.type.id);
        getConfig().set(path + ".spawns", arena.spawns);
        saveConfig();
    }

    private boolean valid(Location location) { return location != null && location.getWorld() != null; }
    private String color(String text) { return ChatColor.translateAlternateColorCodes('&', text); }
    private String message(String text) { return color(getConfig().getString("messages.prefix", "&3&lSKYPVP &8» &f") + text); }
    private void tell(CommandSender sender, String text) { sender.sendMessage(message(text)); }
    private boolean isAdmin(CommandSender sender) {
        if (sender.hasPermission("skypvp.admin")) return true;
        tell(sender, getConfig().getString("messages.no-permission", "&cBrak uprawnien."));
        return false;
    }
    private Arena getArena(String name) { return name == null ? null : arenas.get(name.toLowerCase(Locale.ROOT)); }
    private Arena arenaOf(UUID uuid) {
        for (Arena arena : arenas.values()) if (arena.joined.contains(uuid)) return arena;
        return null;
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase(Locale.ROOT);
        if (cmd.equals("event")) return eventCommand(sender, args);
        if (cmd.equals("setspawn") || cmd.equals("setpvp")) {
            if (!isAdmin(sender)) return true;
            if (!(sender instanceof Player player)) { tell(sender, "&cTej komendy uzywa gracz w grze."); return true; }
            if (cmd.equals("setspawn")) {
                lobbySpawn = player.getLocation().clone();
                getConfig().set("locations.lobby", lobbySpawn);
                tell(player, "&aUstawiono spawn lobby.");
            } else {
                pvpSpawn = player.getLocation().clone();
                getConfig().set("locations.pvp", pvpSpawn);
                tell(player, "&aUstawiono spawn PvP.");
            }
            saveConfig();
            return true;
        }
        if (!(sender instanceof Player player)) { tell(sender, "&cTej komendy uzywa gracz w grze."); return true; }
        switch (cmd) {
            case "spawn" -> {
                if (!valid(lobbySpawn)) tell(player, getConfig().getString("messages.no-spawn", "&cSpawn nieustawiony."));
                else { player.teleport(lobbySpawn); tell(player, "&aWitaj na lobby!"); }
            }
            case "pvp" -> {
                if (!valid(pvpSpawn)) tell(player, getConfig().getString("messages.no-pvp", "&cSpawn PvP nieustawiony."));
                else { restoreArmor(player); player.teleport(pvpSpawn); tell(player, "&cPowodzenia na PvP!"); }
            }
            case "kostiumy" -> openCostumeMenu(player);
            default -> { return false; }
        }
        return true;
    }

    private boolean eventCommand(CommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            tell(sender, "&b/event create <nazwa> <1v1|ffa> &7(admin)");
            tell(sender, "&b/event setspawn <nazwa> &7(admin; powtorz dla kolejnych spawnow)");
            tell(sender, "&b/event join <nazwa> &7| &b/event leave &7| &b/event list");
            tell(sender, "&b/event start <nazwa> [sekundy] &7| &b/event stop &7(admin)");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("create")) {
            if (!isAdmin(sender)) return true;
            if (args.length < 3) { tell(sender, "&cUzycie: /event create <nazwa> <1v1|ffa>"); return true; }
            String name = args[1].toLowerCase(Locale.ROOT);
            EventType type = EventType.parse(args[2]);
            if (!name.matches("[a-z0-9_-]+") || type == null) { tell(sender, "&cNieznana nazwa lub typ. Podaj 1v1 albo ffa."); return true; }
            if (arenas.containsKey(name)) { tell(sender, "&cTaki event juz istnieje."); return true; }
            Arena arena = new Arena(name, type);
            arenas.put(name, arena);
            saveArena(arena);
            tell(sender, "&aUtworzono event &b" + name + " &7(" + type.id + ").");
            return true;
        }
        if (sub.equals("setspawn")) {
            if (!isAdmin(sender)) return true;
            if (!(sender instanceof Player player)) { tell(sender, "&cTej komendy uzywa gracz."); return true; }
            if (args.length < 2) { tell(sender, "&cUzycie: /event setspawn <nazwa>"); return true; }
            Arena arena = getArena(args[1]);
            if (arena == null) { tell(sender, "&cNie znaleziono eventu."); return true; }
            arena.spawns.add(player.getLocation().clone());
            saveArena(arena);
            tell(player, "&aDodano spawn. Areny maja teraz &b" + arena.spawns.size() + " &aspawn(ow).");
            return true;
        }
        if (sub.equals("join")) {
            if (!(sender instanceof Player player)) { tell(sender, "&cTej komendy uzywa gracz."); return true; }
            if (!player.hasPermission("skypvp.use")) { tell(player, "&cBrak uprawnien."); return true; }
            if (args.length < 2) { tell(sender, "&cUzycie: /event join <nazwa>"); return true; }
            Arena arena = getArena(args[1]);
            if (arena == null) { tell(sender, "&cNie znaleziono eventu."); return true; }
            if (activeArena != null || arena.running) { tell(player, "&cEvent juz trwa."); return true; }
            if (arena.joined.contains(player.getUniqueId())) { tell(player, "&eJestes juz zapisany."); return true; }
            if (arena.type == EventType.DUEL && arena.joined.size() >= 2) { tell(player, "&cTen event 1v1 jest pelny."); return true; }
            arena.joined.add(player.getUniqueId());
            returnLocations.put(player.getUniqueId(), player.getLocation().clone());
            tell(player, "&aZapisano do &b" + arena.name + "&a. Uczestnicy: &b" + arena.joined.size());
            return true;
        }
        if (sub.equals("leave")) {
            if (!(sender instanceof Player player)) { tell(sender, "&cTej komendy uzywa gracz."); return true; }
            Arena arena = arenaOf(player.getUniqueId());
            if (arena == null || arena.running) { tell(player, "&cNie mozesz teraz opuscic eventu."); return true; }
            arena.joined.remove(player.getUniqueId());
            returnLocations.remove(player.getUniqueId());
            tell(player, "&eWypisano cie z eventu.");
            return true;
        }
        if (sub.equals("start")) {
            if (!isAdmin(sender)) return true;
            if (args.length < 2) { tell(sender, "&cUzycie: /event start <nazwa> [sekundy]"); return true; }
            Arena arena = getArena(args[1]);
            if (arena == null) { tell(sender, "&cNie znaleziono eventu."); return true; }
            if (activeArena != null) { tell(sender, "&cInny event juz trwa."); return true; }
            if (arena.spawns.isEmpty()) { tell(sender, "&cNajpierw ustaw spawn: /event setspawn " + arena.name); return true; }
            if (arena.type == EventType.DUEL && arena.joined.size() != 2) { tell(sender, "&c1v1 wymaga dokladnie 2 zapisanych graczy."); return true; }
            if (arena.joined.size() < 2) { tell(sender, "&cPotrzeba co najmniej 2 graczy."); return true; }
            int seconds = getConfig().getInt("settings.countdown-seconds", 10);
            if (args.length >= 3) {
                try { seconds = Math.max(1, Integer.parseInt(args[2])); }
                catch (NumberFormatException exception) { tell(sender, "&cCzas musi byc liczba."); return true; }
            }
            startCountdown(arena, seconds);
            return true;
        }
        if (sub.equals("stop")) {
            if (!isAdmin(sender)) return true;
            if (activeArena == null) { tell(sender, "&cNie ma aktywnego eventu."); return true; }
            tellAll("&cEvent &b" + activeArena.name + " &czatrzymany przez administracje.");
            finishArena(activeArena, null, false);
            return true;
        }
        if (sub.equals("list")) {
            if (arenas.isEmpty()) { tell(sender, "&7Brak skonfigurowanych eventow."); return true; }
            tell(sender, "&bDostepne eventy:");
            for (Arena arena : arenas.values()) tell(sender, "&8» &b" + arena.name + " &7(" + arena.type.id + ") uczestnicy: " + arena.joined.size() + ", spawny: " + arena.spawns.size());
            return true;
        }
        tell(sender, "&cNieznana komenda. Uzyj /event help.");
        return true;
    }

    private void startCountdown(Arena arena, int seconds) {
        activeArena = arena;
        arena.running = true;
        arena.countdown = new BukkitRunnable() {
            int left = seconds;
            @Override public void run() {
                if (left <= 0) { startArena(arena); cancel(); return; }
                if (left <= 5 || left == 10 || left == 15) tellAll("&fEvent &b" + arena.name + " &fstartuje za &b" + left + "&fs!");
                left--;
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private void startArena(Arena arena) {
        List<Location> spawns = new ArrayList<>(arena.spawns);
        Collections.shuffle(spawns);
        arena.alive.clear();
        int index = 0;
        for (UUID uuid : new ArrayList<>(arena.joined)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) { arena.joined.remove(uuid); returnLocations.remove(uuid); continue; }
            arena.alive.add(uuid);
            if (!getConfig().getBoolean("settings.costumes-in-pvp", false)) restoreArmor(player);
            player.teleport(spawns.get(index++ % spawns.size()));
            player.sendTitle(color("&a&lSTART!"), color("&7" + arena.type.id + " &8» &f" + arena.name), 5, 35, 10);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        }
        tellAll("&aEvent &b" + arena.name + " &awystartowal!");
        if (arena.alive.size() <= 1) finishArena(arena, arena.alive.stream().findFirst().orElse(null), true);
    }

    @EventHandler public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Arena arena = arenaOf(player.getUniqueId());
        if (arena == null || !arena.running || !arena.alive.remove(player.getUniqueId())) return;
        tellAll("&c" + player.getName() + " &fzostal wyeliminowany.");
        if (arena.alive.size() <= 1) finishArena(arena, arena.alive.stream().findFirst().orElse(null), true);
    }

    @EventHandler public void onRespawn(PlayerRespawnEvent event) {
        Location back = returnLocations.remove(event.getPlayer().getUniqueId());
        if (valid(back)) event.setRespawnLocation(back);
        else if (valid(lobbySpawn)) event.setRespawnLocation(lobbySpawn);
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Arena arena = arenaOf(uuid);
        if (arena != null) {
            arena.joined.remove(uuid);
            boolean wasAlive = arena.alive.remove(uuid);
            if (arena.running && wasAlive && arena.alive.size() <= 1) finishArena(arena, arena.alive.stream().findFirst().orElse(null), true);
        }
        restoreArmor(event.getPlayer());
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) {
        if (getConfig().getBoolean("settings.lobby-teleport-on-join", true) && valid(lobbySpawn)) {
            Bukkit.getScheduler().runTask(this, () -> event.getPlayer().teleport(lobbySpawn));
        }
    }

    private void finishArena(Arena arena, UUID winnerId, boolean announceWinner) {
        if (arena.countdown != null) { arena.countdown.cancel(); arena.countdown = null; }
        if (winnerId != null && announceWinner) {
            Player winner = Bukkit.getPlayer(winnerId);
            tellAll("&6&lZWYCIEZCA! &f" + (winner == null ? "Gracz" : winner.getName()) + " &7wygral event &b" + arena.name + "&7!");
        } else if (announceWinner) tellAll("&7Event &b" + arena.name + " &7zakonczyl sie bez zwyciezcy.");
        for (UUID uuid : new ArrayList<>(arena.joined)) {
            Player player = Bukkit.getPlayer(uuid);
            Location back = returnLocations.remove(uuid);
            if (player != null && player.isOnline()) {
                if (player.isDead() && valid(back)) returnLocations.put(uuid, back);
                else if (valid(back)) player.teleport(back);
                if (!getConfig().getBoolean("settings.costumes-in-pvp", false)) applySelectedCostume(player);
                tell(player, "&eEvent zakonczony.");
            }
        }
        arena.joined.clear();
        arena.alive.clear();
        arena.running = false;
        if (activeArena == arena) activeArena = null;
    }

    private void tellAll(String text) { Bukkit.broadcastMessage(message(text)); }

    private void openCostumeMenu(Player player) {
        if (!valid(lobbySpawn) || player.getWorld() != lobbySpawn.getWorld()) {
            tell(player, "&cKostiumy mozesz wybierac w lobby.");
            return;
        }
        CostumeMenu holder = new CostumeMenu();
        Inventory inventory = Bukkit.createInventory(holder, 27, color("&3&lKOSTIUMY"));
        holder.inventory = inventory;
        int[] slots = {10, 11, 12, 14, 15};
        for (int i = 0; i < costumes.size(); i++) {
            Costume costume = costumes.get(i);
            ItemStack item = new ItemStack(costume.icon);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(color(costume.label));
            meta.setLore(List.of(color("&7Kliknij, aby zalozyc"), color("&8Tylko wyglad w lobby")));
            meta.getPersistentDataContainer().set(costumeKey, PersistentDataType.STRING, costume.id);
            item.setItemMeta(meta);
            inventory.setItem(slots[i], item);
        }
        ItemStack remove = new ItemStack(Material.BARRIER);
        ItemMeta removeMeta = remove.getItemMeta();
        removeMeta.setDisplayName(color("&cZdejmij kostium"));
        remove.setItemMeta(removeMeta);
        inventory.setItem(22, remove);
        player.openInventory(inventory);
    }

    @EventHandler public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof CostumeMenu)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.BARRIER) {
            restoreArmor(player);
            selectedCostumes.remove(player.getUniqueId());
            tell(player, "&eZdjeto kostium. Przywrocono twoja zbroje.");
            player.closeInventory();
            return;
        }
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;
        String id = meta.getPersistentDataContainer().get(costumeKey, PersistentDataType.STRING);
        if (id == null) return;
        for (Costume costume : costumes) {
            if (costume.id.equals(id)) {
                saveOriginalArmor(player);
                selectedCostumes.put(player.getUniqueId(), costume);
                wearCostume(player, costume);
                tell(player, "&aZalozono &f" + color(costume.label) + "&a.");
                player.closeInventory();
                return;
            }
        }
    }

    private void saveOriginalArmor(Player player) {
        savedArmor.computeIfAbsent(player.getUniqueId(), ignored -> cloneItems(player.getInventory().getArmorContents()));
    }

    private ItemStack[] cloneItems(ItemStack[] items) {
        ItemStack[] copy = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) copy[i] = items[i] == null ? null : items[i].clone();
        return copy;
    }

    private void wearCostume(Player player, Costume costume) {
        player.getInventory().setArmorContents(new ItemStack[]{
                costumePiece(Material.LEATHER_BOOTS, costume, "boots"),
                costumePiece(Material.LEATHER_LEGGINGS, costume, "leggings"),
                costumePiece(Material.LEATHER_CHESTPLATE, costume, "chestplate"),
                costumePiece(Material.LEATHER_HELMET, costume, "helmet")
        });
    }

    private ItemStack costumePiece(Material material, Costume costume, String piece) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(costume.color);
        meta.setDisplayName(color(costume.label));
        meta.getPersistentDataContainer().set(costumeKey, PersistentDataType.STRING, piece);
        item.setItemMeta(meta);
        return item;
    }

    private void applySelectedCostume(Player player) {
        Costume costume = selectedCostumes.get(player.getUniqueId());
        if (costume != null) wearCostume(player, costume);
    }

    private void restoreArmor(Player player) {
        ItemStack[] original = savedArmor.remove(player.getUniqueId());
        if (original != null) player.getInventory().setArmorContents(cloneItems(original));
    }
}
