package me.mrkirby153.uhc.bot.discord;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.mrkirby153.uhc.bot.Main;
import me.mrkirby153.uhc.bot.network.BotCommandHandlers;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.*;
import net.dv8tion.jda.managers.ChannelManager;
import net.dv8tion.jda.managers.GuildManager;
import net.dv8tion.jda.managers.PermissionOverrideManager;
import net.dv8tion.jda.managers.RoleManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Internal class to handle server name to id translations
 */
public class ServerHandler {

    private final File saveLoc;

    private final JDA jda;

    private List<DiscordServer> servers = new ArrayList<>();

    private List<LinkedMinecraftServer> minecraftServers = new ArrayList<>();

    public ServerHandler(JDA jda, File saveLoc) {
        this.saveLoc = saveLoc;
        this.jda = jda;
    }

    /**
     * Initializes the server handler
     */
    public void init() {
        load();
        loadServers();
    }

    /**
     * Adds a server to be tracked by the ServerHandler
     *
     * @param guild The {@link Guild} to add
     */
    public void addConnectedServer(Guild guild) {
        DiscordServer e = new DiscordServer(guild.getName(), guild.getId());
        if (serverExists(e))
            return;
        servers.add(e);
        save();
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
     * Removes the guild from the ServerHandler tracking list
     *
     * @param guild The {@link Guild} to remove
     */
    public void removeConnectedServer(Guild guild) {
        Iterator<DiscordServer> serverIterator = servers.iterator();
        while (serverIterator.hasNext()) {
            if (serverIterator.next().getId().equals(guild.getId()))
                serverIterator.remove();
        }
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
     * Gets a {@link DiscordServer} by a {@link Guild}'s id
     *
     * @param id The id of the {@link Guild}
     * @return A {@link DiscordServer} or null if it does not exist
     */
    public DiscordServer getById(String id) {
        for (DiscordServer s : servers) {
            if (s.getId().equals(id))
                return s;
        }
        return null;
    }

    /**
     * Gets a {@link DiscordServer} by its name.
     * <b>If there is more than one server with the name, throws an {@link IllegalStateException}</b>
     *
     * @param name The name of the server
     * @return The {@link DiscordServer} corresponding to the name
     */
    public DiscordServer getByName(String name) {
        List<Guild> guilds = jda.getGuildsByName(name);
        if (guilds.size() == 0)
            return null;
        if (guilds.size() > 1)
            throw new IllegalStateException("More than one server found for the name " + name + "!");
        Guild guild = guilds.get(0);
        addConnectedServer(guild);
        return getById(guild.getId());
    }

    /**
     * Gets a list of all the connected servers
     *
     * @return All servers tracked by this ServerHandler instance
     */
    public List<DiscordServer> connectedServers() {
        return servers;
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
            servers = gson.fromJson(json, new TypeToken<ArrayList<DiscordServer>>() {
            }.getType());
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Remove servers that no longer exist
        if (servers == null)
            servers = new ArrayList<>();
        Iterator<DiscordServer> serverIterator = servers.iterator();
        while (serverIterator.hasNext()) {
            if (jda.getGuildById(serverIterator.next().getId()) == null)
                serverIterator.remove();
        }
        boolean nameChanged = false;
        Main.logger.info("Updating server names...");
        for (DiscordServer s : servers) {
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
     * Checks if a server is already being tracked
     *
     * @param s The {@link DiscordServer} to check fot tracking
     * @return True if the server is tracked, false if it isn't
     */
    public boolean serverExists(DiscordServer s) {
        for (DiscordServer s1 : servers) {
            if (s1.getId().equals(s.getId()))
                return true;
        }
        return false;
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
     * Checks if the minecraft server is already linked
     *
     * @param server The server to check
     * @return True if linked, false if otherwise
     */
    public boolean alreadyLinked(String server) {
        return getLinkedServer(server) != null;
    }

    /**
     * Unlinks a minecraft server from it's discord server
     *
     * @param server The server to unlink
     */
    public void unlinkMcServer(String server) {
        Iterator<LinkedMinecraftServer> ite = minecraftServers.iterator();
        while (ite.hasNext()) {
            if (ite.next().getId().equals(server))
                ite.remove();
        }
        saveServers();
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
     * Gets the {@link DiscordServer} for the corresponding server id
     *
     * @param serverId The server id
     * @return The server
     */
    public DiscordServer getForMineraftServer(String serverId) {
        LinkedMinecraftServer linkedServer = getLinkedServer(serverId);
        if (linkedServer == null)
            return null;
        return getById(linkedServer.getGuild());
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
     * Class to map discord servers to minecraft servers
     */
    public static class LinkedMinecraftServer {

        private String id;
        private String guild;

        public LinkedMinecraftServer(String serverId, String guild) {
            this.guild = guild;
            this.id = serverId;
        }

        public LinkedMinecraftServer(String serverId, DiscordServer server) {
            this(serverId, server.getId());
        }

        /**
         * Gets the minecraft server's id
         *
         * @return The id of the minecraft server
         */
        public String getId() {
            return id;
        }

        /**
         * Gets the guild/discord server the server is linked to
         *
         * @return The guild linked to
         */
        public String getGuild() {
            return guild;
        }
    }

    /**
     * Class to easily handle discord server names to ids and vice versa
     */
    public static class DiscordServer {

        protected static JDA jda;
        private String name;
        private final String id;

        private transient List<DiscordChannel> channels = new ArrayList<>();
        private transient List<DiscordRank> ranks = new ArrayList<>();
        private Object guild;

        private Role spectatorRole;

        private VoiceChannel spectatorVoice;
        private TextChannel spectatorText;

        public DiscordServer(String name, String id) {
            this.name = name;
            this.id = id;
        }

        public void createTeam(String team) {
            Main.logger.info("Creating team " + team);
            TextChannel c = createTextChannel("team-" + team);
            VoiceChannel v = createVoiceChannel("Team " + team);
            ServerHandler.DiscordRank rank = getRankByName(team);
            if (rank == null)
                rank = createRank(team);
            // Remove permissions
            PermissionOverrideManager textChannel = c.createPermissionOverride(rank.getRole());
            PermissionOverrideManager defaultTextChannel = c.createPermissionOverride(c.getGuild().getPublicRole());

            PermissionOverrideManager voiceChannel = v.createPermissionOverride(rank.getRole());
            PermissionOverrideManager defaultVoiceChannel = v.createPermissionOverride(v.getGuild().getPublicRole());

            if (getSpectatorRole() != null) {
                PermissionOverrideManager spectatorText = c.createPermissionOverride(getSpectatorRole());
                PermissionOverrideManager spectatorVoice = v.createPermissionOverride(getSpectatorRole());
                spectatorText.deny(Permission.MESSAGE_WRITE);
                spectatorText.grant(Permission.MESSAGE_READ);

                spectatorVoice.deny(Permission.VOICE_SPEAK);
                spectatorVoice.grant(Permission.VOICE_CONNECT);

                spectatorText.update();
                spectatorVoice.update();
            }

            textChannel.grant(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE);
            defaultTextChannel.deny(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE);

            voiceChannel.grant(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK);
            defaultVoiceChannel.deny(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK);
            textChannel.update();
            defaultTextChannel.update();
            voiceChannel.update();
            defaultVoiceChannel.update();
        }

        /**
         * Gets the name of the discord server
         *
         * @return The server's name
         */
        public String getName() {
            return name;
        }

        /**
         * Gets the id of the discord server
         *
         * @return The server's id
         */
        public String getId() {
            return id;
        }

        /**
         * Sets the name of the discord server (internally, doesn't actually set the server name)
         *
         * @param name The name to change to
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Creates or returns the text channel with the given name
         *
         * @param name The name of the text channel to get
         * @return The text channel
         */
        public TextChannel createTextChannel(String name) {
            name = name.toLowerCase().replaceAll("\\s", "-");
            if (jda == null)
                throw new IllegalStateException("Tried to create a channel before we were connected!");
            Guild g = jda.getGuildById(this.id);
            if (g == null)
                throw new IllegalStateException("No longer part of the guild " + this.getName());
            Channel existChannel = getExistingChannel(name, false);
            if (this.channels == null)
                this.channels = new ArrayList<>();
            if (existChannel != null) {
                this.channels.add(new DiscordChannel(existChannel));
                return (TextChannel) existChannel;
            }
            ChannelManager cm = g.createTextChannel(name);
            this.channels.add(new DiscordChannel(cm.getChannel()));
            return (TextChannel) cm.getChannel();
        }

        /**
         * Creates or returns the voice channel with the given name
         *
         * @param name The name of the voice channel to get
         * @return The voice channel
         */
        public VoiceChannel createVoiceChannel(String name) {
            if (jda == null)
                throw new IllegalStateException("Tried to create a channel before we were connected!");
            Guild g = jda.getGuildById(this.id);
            if (g == null)
                throw new IllegalStateException("No longer part of the guild " + this.getName());
            Channel existChannel = getExistingChannel(name, true);
            if (existChannel != null) {
                this.channels.add(new DiscordChannel(existChannel));
                return (VoiceChannel) existChannel;
            }
            ChannelManager cm = g.createVoiceChannel(name);
            this.channels.add(new DiscordChannel(cm.getChannel()));
            return (VoiceChannel) cm.getChannel();
        }

        /**
         * Destroys all channels and roles tracked by this server
         */
        public void destroy() {
            Main.logger.info("Removing channels in " + getName());
            if (channels.size() > 0)
                channels.forEach(DiscordChannel::destroy);
            Main.logger.info("Removing ranks on " + getName());
            if (ranks == null)
                ranks = new ArrayList<>();
            ranks.forEach(DiscordRank::delete);
            if (spectatorRole != null)
                spectatorRole.getManager().delete();
            if (spectatorVoice != null)
                spectatorVoice.getManager().delete();
            if (spectatorText != null)
                spectatorText.getManager().delete();
        }

        /**
         * Gets an existing text or voice channel based on its name
         *
         * @param name  The name of the channel
         * @param voice True if looking for a voice channel, false if not
         * @return The {@link Channel} or null if it doesn't exist
         */
        private Channel getExistingChannel(String name, boolean voice) {
            Guild g = jda.getGuildById(this.id);
            if (!voice)
                for (TextChannel c : g.getTextChannels()) {
                    if (c.getName().equals(name))
                        return c;
                }
            else
                for (VoiceChannel v : g.getVoiceChannels()) {
                    if (v.getName().equals(name))
                        return v;
                }
            return null;
        }

        public Guild getGuild() {
            return jda.getGuildById(this.id);
        }

        /**
         * Creates a rank on the server
         *
         * @param name The name of the rank to create
         * @return The rank
         */
        public DiscordRank createRank(String name) {
            Main.logger.info("Creating rank " + name);
            DiscordRank rank = new DiscordRank(this, name);
            rank.create();
            ranks.add(rank);
            return rank;
        }

        public void createSpectatorRole() {
            Role specRole = null;
            for (Role r : getGuild().getRoles()) {
                if (r.getName().equalsIgnoreCase("Spectators")) {
                    specRole = r;
                }
            }
            RoleManager m = (specRole != null) ? specRole.getManager() : getGuild().createRole();
            m.setName("Spectators");
            m.update();
            this.spectatorRole = m.getRole();
            TextChannel c = createTextChannel("Spectators");
            VoiceChannel v = createVoiceChannel("Spectators");

            PermissionOverrideManager textChannel = c.createPermissionOverride(spectatorRole);
            PermissionOverrideManager defaultTextChannel = c.createPermissionOverride(getGuild().getPublicRole());
            textChannel.grant(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE);
            defaultTextChannel.deny(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE);

            PermissionOverrideManager voiceChannel = v.createPermissionOverride(spectatorRole);
            PermissionOverrideManager defaultVoiceChannel = v.createPermissionOverride(getGuild().getPublicRole());

            voiceChannel.grant(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK);
            defaultVoiceChannel.deny(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK);
            textChannel.update();
            voiceChannel.update();
            defaultTextChannel.update();
            defaultVoiceChannel.update();
            spectatorVoice = v;
            spectatorText = c;
        }

        public void assignSpectatorRole(User user) {
            getGuild().getManager().addRoleToUser(user, spectatorRole);
        }

        /**
         * Gets the rank by its name
         *
         * @param name The name of rank
         * @return The rank or null, if it doesn't exist
         */
        public DiscordRank getRankByName(String name) {
            if (ranks == null)
                ranks = new ArrayList<>();
            for (DiscordRank r : ranks) {
                if (r.rankName.equals(name))
                    return r;
            }
            return null;
        }

        /**
         * Assigns the rank to the given user
         *
         * @param user     The user
         * @param rankName The rank
         */
        public void assignRank(User user, String rankName) {
            DiscordRank rankByName = getRankByName(rankName);
            if (rankByName == null)
                rankByName = createRank(rankName);
            rankByName.assign(user);
        }

        /**
         * Destroys the channel with the given name
         *
         * @param name The name
         */
        public void deleteChannel(String name) {
            Iterator<DiscordChannel> it = channels.iterator();
            while (it.hasNext()) {
                DiscordChannel next = it.next();
                if (next.getName().equalsIgnoreCase(name)) {
                    next.destroy();
                    it.remove();
                }
            }
        }

        /**
         * Gets the {@link VoiceChannel} by its name
         *
         * @param channel The channel name
         * @return The {@link VoiceChannel} or null
         */
        public VoiceChannel getVoiceChannel(String channel) {
            for (DiscordChannel c : channels) {
                if (c.getName().equals(channel)) {
                    if (c.channel instanceof VoiceChannel)
                        return (VoiceChannel) c.channel;
                    else
                        return null;
                }
            }
            return null;
        }

        /**
         * Gets the {@link TextChannel} by its name
         *
         * @param channel The channel name
         * @return The {@link TextChannel} or null
         */
        public MessageChannel getTextChannel(String channel) {
            for (DiscordChannel c : channels) {
                if (c.getName().equals(channel)) {
                    if (c.channel instanceof TextChannel)
                        return (TextChannel) c.channel;
                    else
                        return null;
                }
            }
            return null;
        }

        public void bringAllToLobby() {
            VoiceChannel vc = createVoiceChannel("Lobby");
            getGuild().getUsers().stream().filter(user -> BotCommandHandlers.AssignTeams.connectedToVice(this, user)).forEach(user -> getGuild().getManager().moveVoiceUser(user, vc));
            // Remove all roles from the user
            ranks.forEach(DiscordRank::unassignAll);
        }

        public Role getSpectatorRole() {
            return spectatorRole;
        }
    }

    /**
     * Handles information about a Discord Channel
     */
    public static class DiscordChannel {
        private final Guild owningGuild;
        private String name;
        private final String id;
        private Channel channel;

        public DiscordChannel(String id, Guild owningGuild) {
            this(owningGuild.getJDA().getTextChannelById(id));
        }

        public DiscordChannel(Channel c) {
            this.owningGuild = c.getGuild();
            this.name = c.getName();
            this.id = c.getId();
            this.channel = c;
        }

        /**
         * Gets the guild that owns this channel
         *
         * @return The channel
         */
        public Guild getOwningGuild() {
            return owningGuild;
        }

        /**
         * Gets the channel's name
         *
         * @return The channel's name
         */
        public String getName() {
            return name;
        }

        /**
         * Gets the channel's id
         *
         * @return The ID of the channel
         */
        public String getId() {
            return id;
        }

        /**
         * Gets the Channel's {@link ChannelManager}
         *
         * @return The {@link ChannelManager}
         */
        public ChannelManager getManager() {
            return channel.getManager();
        }

        /**
         * Gets the channel as a {@link TextChannel}
         *
         * @return The channel as a text channel
         */
        public TextChannel asTextChannel() {
            if (!(channel instanceof TextChannel))
                throw new IllegalAccessError("Cannot get " + getName() + " as a text channel!");
            return (TextChannel) channel;
        }

        /**
         * Gets the channel as a {@link VoiceChannel}
         *
         * @return The channel as a voice channel
         */
        public VoiceChannel asVoiceChannel() {
            if (!(channel instanceof VoiceChannel))
                throw new IllegalAccessError("Cannot get " + getName() + " as a voice channel!");
            return (VoiceChannel) channel;
        }

        /**
         * Gets the channel as a raw {@link Channel}
         *
         * @return The channel
         */
        public Channel getChannel() {
            return channel;
        }

        /**
         * Removes the channel from existence
         */
        public void destroy() {
            this.channel.getManager().delete();
        }
    }

    public static class DiscordRank {
        private String rankName;
        private RoleManager role;
        private DiscordServer server;

        private List<User> assignedUsers = new ArrayList<>();

        public DiscordRank(DiscordServer onServer, String rankName) {
            this.rankName = rankName;
            this.server = onServer;
        }

        /**
         * Creates the role on the server
         */
        public void create() {
            role = server.getGuild().createRole();
            role.setName(rankName);
            role.update();
        }

        /**
         * Deletes the role
         */
        public void delete() {
            role.delete();
        }

        /**
         * Adds the role to the given user
         *
         * @param user The user to give the role to
         */
        public void assign(User user) {
            if (user == null)
                return;
            server.getGuild().getManager().addRoleToUser(user, role.getRole());
            server.getGuild().getManager().update();
            assignedUsers.add(user);
        }

        /**
         * Removes the role from the player
         *
         * @param user The user to remove the role to
         */
        public void unassign(User user) {
            GuildManager manager = server.getGuild().getManager();
            manager.removeRoleFromUser(user, role.getRole());
            manager.update();
            assignedUsers.remove(user);
        }

        /**
         * Removes this role from all users assigned
         */
        public void unassignAll() {
            GuildManager manager = server.getGuild().getManager();
            assignedUsers.forEach(u -> manager.removeRoleFromUser(u, getRole()));
            manager.update();
        }

        /**
         * Gets the {@link Role} corresponding to the rank
         *
         * @return The role
         */
        public Role getRole() {
            return role.getRole();
        }
    }
}

