package me.mrkirby153.uhc.bot.discord;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.mrkirby153.uhc.bot.Main;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.Channel;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.VoiceChannel;
import net.dv8tion.jda.managers.ChannelManager;

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

    public ServerHandler(JDA jda, File saveLoc) {
        this.saveLoc = saveLoc;
        this.jda = jda;
    }

    /**
     * Initializes the server handler
     */
    public void init() {
        load();
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
            if(!saveLoc.exists()){
                saveLoc.getParentFile().mkdirs();
                saveLoc.createNewFile();
                load();
                return;
            }
            Gson gson = new Gson();
            String json = new String(Files.readAllBytes(saveLoc.toPath()));
            servers = gson.fromJson(json, new TypeToken<ArrayList<DiscordServer>>(){}.getType());
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Remove servers that no longer exist
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
     * Class to easily handle discord server names to ids and vice versa
     */
    public static class DiscordServer {

        protected static JDA jda;
        private String name;
        private final String id;

        private transient List<DiscordChannel> channels = new ArrayList<>();
        private Object guild;

        public DiscordServer(String name, String id) {
            this.name = name;
            this.id = id;
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
            if(this.channels == null)
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
         * Destroys all channels tracked by this server
         */
        public void destroyAllChannels() {
            Main.logger.info("Removing channels in " + getName());
            for (DiscordChannel c : channels) {
                c.destroy();
            }
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
}
