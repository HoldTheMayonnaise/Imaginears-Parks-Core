package club.imaginears.core;

import club.imaginears.core.commands.*;
import club.imaginears.core.commands.Build;
import club.imaginears.core.commands.Warps;
import club.imaginears.core.commands.WhitelistManager;
import club.imaginears.core.events.*;
import club.imaginears.core.objects.Shop;
import club.imaginears.core.objects.User;
import club.imaginears.core.utils.*;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;


public class Core extends JavaPlugin implements PluginMessageListener {


    private static Core instance;
    public static Boolean debug = false;
    private static HashMap<UUID, User> users = new HashMap<>();
    public static HashMap<Location, Shop> enabledShops = new HashMap<>();
    public static HashMap<Location, Shop> inProgressShops = new HashMap<>();
    public static HashMap<Player, Shop> playerOnShop = new HashMap<>();
    public File warpsFile;
    public FileConfiguration warps;

    public File inventoriesFile;
    public FileConfiguration inventories;

    public File whitelistFile;
    public FileConfiguration whitelist;

    public File shopsFile;
    public FileConfiguration shops;



    @Override
    public void onEnable() {

        instance = this;
        Console.Log("Starting core..", Console.types.LOG);
        createFiles();
        registerCommands();
        registerEvents();
        ShopSigns.loadShops();
        for (Player p : Bukkit.getOnlinePlayers()) {
            Players.joinSetup(p);
        }

        startMainTimer();
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);

    }

    @Override
    public void onDisable() {
        for (Player pl : Bukkit.getOnlinePlayers()) {
            if (Build.checkBuildMode(pl)) {
                Chat.sendMessage(pl, "Staff", "The plugin is being reloaded, you will be taken out of build mode");
                Build.disableBuildMode(pl);
            } else {
                InventoryManager.savePlayInventory(pl);
            }
        }
        club.imaginears.core.utils.WhitelistManager.saveFile();
        instance = null;
        Console.Log("Stopping core..", Console.types.LOG);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();
        if (subchannel.equals("test")) {
            // Use the code sample in the 'Response' sections below to read
            // the data.
        }
    }

    public static Core getInstance() {
        return instance;
    }

    public static HashMap<UUID, User> getUserMap() {
        return new HashMap<>(users);
    }

    public static List<User> getUsers() {
        return new ArrayList<>(users.values());
    }

    public static void addUser(User user) {
        Preconditions.checkNotNull(user.getUniqueId());
        users.remove(user.getUniqueId());
        users.put(user.getUniqueId(), user);
    }

    public static User getUser(String name) {
        for (User user : new ArrayList<>(users.values())) {
            if (user.getName().equalsIgnoreCase(name)) {
                return user;
            }
        }
        return null;
    }

    public static User getUser(UUID uuid) {
        return users.get(uuid);
    }

    public static void deleteUser(UUID uuid) {
        users.remove(uuid);
    }

    public void createFiles() {
        Console.Log("Loading files", Console.types.LOG);
        createWarpsFile();
        Console.Log("Loaded warps file", Console.types.LOG);
        createInventoriesFile();
        Console.Log("Loaded inventories file", Console.types.LOG);
        createWhitelistFile();
        Console.Log("Loaded whitelist file", Console.types.LOG);
        createShopsFile();
        Console.Log("Loaded shops file", Console.types.LOG);
    }

    public FileConfiguration getWarpsFile() {
        return this.warps;
    }

    private void createWarpsFile() {
        warpsFile = new File(getDataFolder(), "warps.yml");
        if (!warpsFile.exists()) {
            warpsFile.getParentFile().mkdirs();
            saveResource("warps.yml", false);
        }

        warps = new YamlConfiguration();
        try {
            warps.load(warpsFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getShopsFile() {
        return this.shops;
    }

    private void createShopsFile() {
        shopsFile = new File(getDataFolder(), "shops.yml");
        if (!shopsFile.exists()) {
            shopsFile.getParentFile().mkdirs();
            saveResource("shops.yml", false);
        }

        shops = new YamlConfiguration();
        try {
            shops.load(shopsFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getWhitelistFile() {
        return this.whitelist;
    }

    private void createWhitelistFile() {
        whitelistFile = new File(getDataFolder(), "whitelist.yml");
        if (!whitelistFile.exists()) {
            whitelistFile.getParentFile().mkdirs();
            saveResource("whitelist.yml", false);
        }

        whitelist = new YamlConfiguration();
        try {
            whitelist.load(whitelistFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getInventoriesFile() {
        return this.inventories;
    }

    public void createInventoriesFile() {
        inventoriesFile = new File(getDataFolder(), "inventories.yml");
        if (!inventoriesFile.exists()) {
            inventoriesFile.getParentFile().mkdirs();
            saveResource("inventories.yml", false);
        }

        inventories = new YamlConfiguration();
        try {
            inventories.load(inventoriesFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void saveInventories() {
        for (Player pname : Bukkit.getOnlinePlayers()) {
            Player p = Bukkit.getPlayer(pname.getName());
            if (Build.buildMode.contains(p.getName())) {
                Chat.sendMessage(p, "Staff", "The plugin is being reloaded, you will be taken out of build mode");
                InventoryManager.saveBuildInventory(p);
                Chat.sendMessage(p, "Staff", "Saved build");
                p.setGameMode(GameMode.ADVENTURE);
                InventoryManager.loadPlayInventory(p);
                Chat.sendMessage(p, "Staff", "Out of build");
                Build.buildMode.remove(p.getName());
            } else {
                InventoryManager.savePlayInventory(p);
            }
        }
    }

    public void startMainTimer() {
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            public void run() {
                for (Player pl : Bukkit.getOnlinePlayers()) {
                    if (Vanish.vanished.contains(pl.getName())) {
                        pl.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(Chat.sendColorFree("&b&lYou are vanished")));
                    }
                }
            }
        }, 20L, 20L);
    }

    public void registerCommands() {
        Console.Log("Loading commands..", Console.types.LOG);
        getCommand("edituser").setExecutor(new EditUser());
        Console.Log("Loaded edituser command", Console.types.DEBUG);
        getCommand("fly").setExecutor(new Fly());
        Console.Log("Loaded fly command", Console.types.DEBUG);
        getCommand("vanish").setExecutor(new Vanish());
        Console.Log("Loaded vanish command", Console.types.DEBUG);
        getCommand("vanished").setExecutor(new Vanished());
        Console.Log("Loaded vanished command", Console.types.DEBUG);
        getCommand("nightvision").setExecutor(new Nightvision());
        Console.Log("Loaded nv command", Console.types.DEBUG);
        getCommand("teleport").setExecutor(new Teleport());
        Console.Log("Loaded teleport command", Console.types.DEBUG);
        getCommand("teleporthere").setExecutor(new TeleportHere());
        Console.Log("Loaded teleporthere command", Console.types.DEBUG);
        getCommand("setwarp").setExecutor(new SetWarp());
        Console.Log("Loaded setwarp command", Console.types.DEBUG);
        getCommand("delwarp").setExecutor(new DelWarp());
        Console.Log("Loaded delwarp command", Console.types.DEBUG);
        getCommand("warp").setExecutor(new Warp());
        Console.Log("Loaded warp command", Console.types.DEBUG);
        getCommand("warps").setExecutor(new Warps());
        Console.Log("Loaded warps command", Console.types.DEBUG);
        getCommand("tptoggle").setExecutor(new TPToggle());
        Console.Log("Loaded tptoggle command", Console.types.DEBUG);
        getCommand("flyspeed").setExecutor(new FlightSpeed());
        Console.Log("Loaded flyspeed command", Console.types.DEBUG);
        getCommand("walkspeed").setExecutor(new WalkSpeed());
        Console.Log("Loaded walkspeed command", Console.types.DEBUG);
        getCommand("build").setExecutor(new Build());
        Console.Log("Loaded build command", Console.types.DEBUG);
        getCommand("tpcoord").setExecutor(new TPCoord());
        Console.Log("Loaded tpcoord command", Console.types.DEBUG);
        getCommand("buildoffall").setExecutor(new BuildOffAll());
        Console.Log("Loaded buildoffall command", Console.types.DEBUG);
        getCommand("reloadinventories").setExecutor(new ReloadInventories());
        Console.Log("Loaded reloadinventories command", Console.types.DEBUG);
        getCommand("getuuid").setExecutor(new GetUUID());
        Console.Log("Loaded getuuid command", Console.types.DEBUG);
        getCommand("fixinventory").setExecutor(new FixInventory());
        Console.Log("Loaded fixinventory command", Console.types.DEBUG);
        getCommand("heal").setExecutor(new Heal());
        Console.Log("Loaded heal command", Console.types.DEBUG);
        getCommand("speed").setExecutor(new Speed());
        Console.Log("Loaded speed command", Console.types.DEBUG);
        getCommand("skull").setExecutor(new Skull());
        Console.Log("Loaded skull command", Console.types.DEBUG);
        getCommand("invsee").setExecutor(new InvSee());
        Console.Log("Loaded invsee command", Console.types.DEBUG);
        getCommand("whitelistmanager").setExecutor(new WhitelistManager());
        Console.Log("Loaded whitelistmanager command", Console.types.DEBUG);
        getCommand("balance").setExecutor(new Balance());
        Console.Log("Loaded balance command", Console.types.DEBUG);
        getCommand("charge").setExecutor(new Charge());
        Console.Log("Loaded charge command", Console.types.DEBUG);
        getCommand("setbalance").setExecutor(new SetBalance());
        Console.Log("Loaded setbalance command", Console.types.DEBUG);
        getCommand("pay").setExecutor(new Pay());
        Console.Log("Loaded pay command", Console.types.DEBUG);
        getCommand("reward").setExecutor(new Reward());
        Console.Log("Loaded reward command", Console.types.DEBUG);
        getCommand("transactions").setExecutor(new Transactions());
        Console.Log("Loaded transactions command", Console.types.DEBUG);
        getCommand("pin").setExecutor(new Pin());
        Console.Log("Loaded pin command", Console.types.DEBUG);
        getCommand("spawn").setExecutor(new Spawn());
        Console.Log("Loaded spawn command", Console.types.DEBUG);
        getCommand("kick").setExecutor(new Kick());
        Console.Log("Loaded kick command", Console.types.DEBUG);
        getCommand("profile").setExecutor(new Profile());
        Console.Log("Loaded profile command", Console.types.DEBUG);
        getCommand("security").setExecutor(new Security());
        Console.Log("Loaded security command", Console.types.DEBUG);
        Console.Log("Loaded commands..", Console.types.LOG);
    }

    public void registerEvents() {
        Console.Log("Loading events..", Console.types.LOG);
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerJoin(), this);
        pm.registerEvents(new PlayerLeave(), this);
        pm.registerEvents(new GUIs(), this);
        pm.registerEvents(new club.imaginears.core.events.Build(), this);
        pm.registerEvents(new SpecialSigns(), this);
        pm.registerEvents(new PlayerCommandPreprocess(), this);
        pm.registerEvents(new AsyncPlayerChat(), this);
        pm.registerEvents(new InventoryManager(), this);
        Console.Log("Loaded events..", Console.types.LOG);
    }

    public void sendMessageToBungee(String topic, String msg) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF(topic);

        ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
        DataOutputStream msgout = new DataOutputStream(msgbytes);
        try {
            msgout.writeUTF(msg);
        } catch (
                IOException exception){
            exception.printStackTrace();
        }

        out.writeShort(msgbytes.toByteArray().length);
        out.write(msgbytes.toByteArray());
    }

}
