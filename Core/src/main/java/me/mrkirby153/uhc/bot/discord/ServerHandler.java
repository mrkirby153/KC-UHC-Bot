package me.mrkirby153.uhc.bot.discord;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.mrkirby153.uhc.bot.Main;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.Guild;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Handles all the server translations
 */
public class ServerHandler {

    /**
     * The save location of the servers
     */
    private final File saveLoc;


    /**
     * The global JDA instance
     */
    private final JDA jda;

    /**
     * A list of guilds that are tracked by the handler
     */
    private HashSet<DiscordGuild> servers = new HashSet<>();

    /**
     * A list of minecraft servers that are linked
     */
    private List<LinkedMinecraftServer> minecraftServers = new ArrayList<>();

    public ServerHandler(JDA jda, File saveLoc) {
        this.saveLoc = saveLoc;
        this.jda = jda;
    }

    /**
     * Adds a server to be tracked by the ServerHandler
     *
     * @param guild The {@link Guild} to add
     */
    public DiscordGuild addConnectedServer(Guild guild) {
        DiscordGuild e = new DiscordGuild(guild.getName(), guild.getId());
        servers.add(e);
        save();
        return e;
    }

    /**
     * Adds a server by its id to be tracked by the ServerHandler
     *
     * @param guildId The {@link Guild} to add
     */
    public void addConnectedServer(String guildId) {
        Guild g = jda.getGuildById(guildId);
        if (g == null)
            return;
        addConnectedServer(g);
    }

    /**
     * Checks if the minecraft server is already linked
     *
     * @param server The server to check
     * @return True if linked, false if otherwise
     */
    public boolean alreadyLinked(String server) {
        return getLinkedServer(server) != null;
    }

    /**
     * Gets a list of all the connected servers
     *
     * @return All servers tracked by this ServerHandler instance
     */
    public HashSet<DiscordGuild> connectedServers() {
        return servers;
    }

    /**
     * Gets a {@link DiscordGuild} by a {@link Guild}'s id
     *
     * @param id The id of the {@link Guild}
     * @return A {@link DiscordGuild} or null if it does not exist
     */
    public DiscordGuild getById(String id) {
        for (DiscordGuild s : servers) {
            if (s.getId().equals(id))
                return s;
        }
        return null;
    }

    /**
     * Gets the {@link DiscordGuild} for the corresponding server id
     *
     * @param serverId The server id
     * @return The server
     */
    public DiscordGuild getForMineraftServer(String serverId) {
        LinkedMinecraftServer linkedServer = getLinkedServer(serverId);
        if (linkedServer == null)
            return null;
        return getById(linkedServer.getGuild());
    }

    /**
     * Gets a {@link LinkedMinecraftServer} map of the guild
     *
     * @param server The unique minecraft server id
     * @return The server
     */
    public LinkedMinecraftServer getLinkedServer(String server) {
        if (minecraftServers == null)
            minecraftServers = new ArrayList<>();
        for (LinkedMinecraftServer m : minecraftServers) {
            if (m.getId().equals(server))
                return m;
        }
        return null;
    }

    /**
     * Initializes the server handler
     */
    public void init() {
        load();
        loadServers();
        // Initialize the discord guild
        this.servers.forEach(DiscordGuild::create);
    }

    /**
     * Links a minecraft server and a discord server.
     *
     * @param server The minecraft server's unique id
     * @param guild  The guild's id
     * @return True if the server link was successful, false if it wasn't
     */
    public boolean linkMcServer(String server, String guild) {
        if (alreadyLinked(server))
            return false;
        if (getById(guild) == null)
            return false;
        LinkedMinecraftServer lmcs = new LinkedMinecraftServer(server, guild);
        minecraftServers.add(lmcs);
        saveServers();
        return true;
    }

    /**
     * Attempts to load the servers from json
     */
    public void load() {
        try {
            if (!saveLoc.exists()) {
                saveLoc.getParentFile().mkdirs();
                saveLoc.createNewFile();
                load();
                return;
            }
            Gson gson = new Gson();
            String json = new String(Files.readAllBytes(saveLoc.toPath()));
            servers = gson.fromJson(json, new TypeToken<HashSet<DiscordGuild>>() {
            }.getType());
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Remove servers that no longer exist
        if (servers == null)
            servers = new HashSet<>();
        servers.removeIf(discordServer -> jda.getGuildById(discordServer.getId()) == null);
        boolean nameChanged = false;
        Main.logger.info("Updating server names...");
        for (DiscordGuild s : servers) {
            String name = jda.getGuildById(s.getId()).getName();
            String storedName = s.getName();
            Main.logger.info("Stored name: " + storedName + ", Name: " + name);
            if (!s.getName().equals(name)) {
                Main.logger.info("Guild changed!");
                nameChanged = true;
                s.setName(name);
            }
        }
        if (nameChanged)
            save();
    }

    /**
     * Loads the discord servers from a file
     */
    public void loadServers() {
        try {
            File mcServers = new File("mcServers.json");
            if (!mcServers.exists())
                mcServers.createNewFile();
            Gson gson = new Gson();
            String json = new String(Files.readAllBytes(mcServers.toPath()));
            minecraftServers = gson.fromJson(json, new TypeToken<ArrayList<LinkedMinecraftServer>>() {
            }.getType());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes the guild from the ServerHandler tracking list
     *
     * @param guild The {@link Guild} to remove
     */
    public void removeConnectedServer(Guild guild) {
        servers.removeIf(discordServer -> discordServer.getId().equals(guild.getId()));
        save();
    }

    /**
     * Removes the guild from the ServerHandler tracking list with the given id.
     * If the guild has been deleted this will fail silently
     *
     * @param id The id of the guild
     */
    public void removeConnectedServer(String id) {
        Guild g = jda.getGuildById(id);
        if (g == null)
            return;
        removeConnectedServer(g);
    }

    /**
     * Saves the servers to a file in json format
     */
    public void save() {
        Gson gson = new Gson();
        String json = gson.toJson(servers, List.class);
        FileOutputStream fos;
        try {
            if (!saveLoc.exists()) {
                saveLoc.getParentFile().mkdirs();
                saveLoc.createNewFile();
            }
            fos = new FileOutputStream(saveLoc);
            fos.write(json.getBytes());
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves the linked servers to a file
     */
    public void saveServers() {
        Gson gson = new Gson();
        String json = gson.toJson(minecraftServers, List.class);
        try {
            File servers = new File("mcServers.json");
            if (!servers.exists()) {
                servers.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(servers);
            fos.write(json.getBytes());
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Unlinks a minecraft server from it's discord server
     *
     * @param server The server to unlink
     */
    public void unlinkMcServer(String server) {
        minecraftServers.removeIf(linkedMinecraftServer -> linkedMinecraftServer.getId().equals(server));
        saveServers();
    }
}
