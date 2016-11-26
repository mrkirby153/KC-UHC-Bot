package me.mrkirby153.uhc.bot.discord;

import me.mrkirby153.uhc.bot.Main;
import me.mrkirby153.uhc.bot.network.BotCommandHandlers;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.*;
import net.dv8tion.jda.managers.ChannelManager;
import net.dv8tion.jda.managers.PermissionOverrideManager;
import net.dv8tion.jda.managers.RoleManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class representing a Discord guild
 */
public class DiscordGuild {

    /**
     * A static reference to the main JDA
     */
    private static JDA jda;

    /**
     * The Discord id of this server
     */
    private final String id;
    /**
     * The JDA guild instance
     */
    private transient Guild guild;
    /**
     * The server's name
     */
    private String name;
    /**
     * A list of all the created roles
     */
    private transient Map<String, UHCTeam> teams = new HashMap<>();

    /**
     * The spectator role on the server
     */
    private transient Role spectatorRole;

    /**
     * The Spectator's voice channel
     */
    private transient VoiceChannel spectatorVoiceChannel;

    private transient ArrayList<Message> messagesToDelete = new ArrayList<>();


    public DiscordGuild(String name, String id) {
        this.name = name;
        this.id = id;
        if (jda != null) {
            this.guild = jda.getGuildById(id);
        } else {
            this.guild = null;
        }
    }

    /**
     * Gets the JDA instance
     *
     * @return The {@link JDA} instance
     */
    public static JDA getJda() {
        return jda;
    }

    /**
     * Sets the {@link JDA} instance
     *
     * @param jda The {@link JDA} instance to set
     */
    public static void setJda(JDA jda) {
        DiscordGuild.jda = jda;
    }

    /**
     * Brings everyone to the lobby channel
     */
    public void bringAllToLobby() {
        VoiceChannel vc = (VoiceChannel) getOrCreateChannel("Lobby", ChannelType.VOICE);
        getGuild().getUsers().stream().filter(user -> BotCommandHandlers.AssignTeams.connectedToVice(this, user)).forEach(user -> getGuild().getManager().moveVoiceUser(user, vc));
    }

    /**
     * Creates the spectator role on the server
     */
    public void create() {
        Main.logger.info("Initializing server " + this.name);
        if (guild == null) {
            guild = jda.getGuildById(this.id);
        }
        RoleManager spectatorRm = guild.createRole();
        spectatorRm.setName("Spectators");
        spectatorRm.update();
        this.spectatorRole = spectatorRm.getRole();

        this.spectatorVoiceChannel = (VoiceChannel) getOrCreateChannel("Spectators", ChannelType.VOICE);
        this.denyDefault(spectatorVoiceChannel.getManager(), Permission.VOICE_CONNECT);
        this.grant(spectatorRole, spectatorVoiceChannel.getManager(), Permission.VOICE_CONNECT);
    }

    /**
     * Creates a new team
     *
     * @param team The team name to create
     * @throws IllegalArgumentException If the team already exists
     */
    public void createTeam(String team) {
        if (teams == null)
            teams = new HashMap<>();
        if (teams.containsKey(team.toLowerCase())) {
            teams.remove(team.toLowerCase()).destroy();
        }
        UHCTeam uhcTeam = new UHCTeam(this, team);
        uhcTeam.create();
        this.teams.put(team.toLowerCase(), uhcTeam);
    }

    public void deleteChannel(String s) {
        for (Channel c : getAllChannels()) {
            if (c.getName().equalsIgnoreCase(s)) {
                c.getManager().delete();
            }
        }
    }

    /**
     * Deletes all the messages created by the robot
     */
    public void deleteMessages() {
        Thread clearThread = new Thread(() -> {
            HashMap<MessageChannel, ArrayList<Message>> messages = new HashMap<>();
            if (messagesToDelete.size() == 0) {
                messagesToDelete.get(0).deleteMessage();
            } else {
                for (Message m : messagesToDelete) {
                    ArrayList<Message> arr = messages.get(m.getChannel());
                    if (arr == null)
                        arr = new ArrayList<>();
                    arr.add(m);
                    messages.put(m.getChannel(), arr);
                }

                messages.forEach((c, m) -> {
                    ((TextChannel) c).deleteMessages(m);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
            messagesToDelete.clear();
        });
        clearThread.setName("DeleteMessageThread");
        clearThread.start();
    }

    /**
     * Denies permissions for the given role on the server
     *
     * @param role        The role to deny
     * @param channel     The channel to deny in
     * @param permissions The permissions to deny
     */
    public void deny(Role role, ChannelManager channel, Permission... permissions) {
        PermissionOverrideManager permissionManager = channel.getChannel().createPermissionOverride(role);
        permissionManager.deny(permissions);
        permissionManager.update();
    }

    /**
     * Denies permissions for the default role in the channel
     *
     * @param channel     The channel to deny
     * @param permissions The permissions ti deny
     */
    public void denyDefault(ChannelManager channel, Permission... permissions) {
        this.deny(this.guild.getPublicRole(), channel, permissions);
    }

    /**
     * Destroys the UHC setup on this server
     */
    public void destroy() {
        if (Main.logger != null)
            Main.logger.info("Destroying server " + this.name);
        if (spectatorRole != null)
            spectatorRole.getManager().delete();
        if (spectatorVoiceChannel != null)
            spectatorVoiceChannel.getManager().delete();
        unlockChannels();
        if (teams != null)
            teams.values().forEach(UHCTeam::destroy);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DiscordGuild && (this == obj || ((DiscordGuild) obj).getId().equals(this.getId()));
    }

    /**
     * Gets a channel by its name
     *
     * @param name        The name of the channel
     * @param channelType The type of chanenl
     * @return The channel
     */
    public Channel getChannel(String name, ChannelType channelType) {
        switch (channelType) {
            case VOICE:
                for (VoiceChannel voiceChannel : this.guild.getVoiceChannels())
                    if (voiceChannel.getName().equalsIgnoreCase(name)) {
                        return voiceChannel;
                    }
                break;
            case TEXT:
                for (TextChannel textChannel : this.guild.getTextChannels())
                    if (textChannel.getName().equalsIgnoreCase(name)) {
                        return textChannel;
                    }
                break;
        }
        return null;
    }

    /**
     * Gets the guild
     *
     * @return The guild
     */
    public Guild getGuild() {
        return guild;
    }

    /**
     * Gets the guild's id
     *
     * @return The guild's id
     */
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets or creates a Discord channel
     *
     * @param name        The name of the channel
     * @param channelType The channel type
     * @return The channel
     */
    public Channel getOrCreateChannel(String name, ChannelType channelType) {
        Channel foundChannel = null;
        switch (channelType) {
            case VOICE:
                for (VoiceChannel voiceChannel : this.guild.getVoiceChannels())
                    if (voiceChannel.getName().equalsIgnoreCase(name)) {
                        foundChannel = voiceChannel;
                        break;
                    }
                break;
            case TEXT:
                for (TextChannel textChannel : this.guild.getTextChannels())
                    if (textChannel.getName().equalsIgnoreCase(name)) {
                        foundChannel = textChannel;
                        break;
                    }
                break;
        }
        if (foundChannel != null)
            return foundChannel;
        switch (channelType) {
            case VOICE:
                return guild.createVoiceChannel(name).getChannel();
            case TEXT:
                return guild.createTextChannel(name).getChannel();
        }
        return null;
    }

    /**
     * Gets the spectators role
     *
     * @return The spectator role
     */
    public Role getSpectatorRole() {
        return spectatorRole;
    }

    /**
     * Gets the {@link UHCTeam} by its name
     *
     * @param name The name of the team
     * @return The {@link UHCTeam} or null, if it doesn't exist
     */
    public UHCTeam getTeam(String name) {
        return this.teams.get(name.toLowerCase());
    }

    /**
     * Grants permissions for the given role on the given channel
     *
     * @param role        The role to grant permissions for
     * @param channel     The channel to grant permissions in
     * @param permissions The permissions to grant
     */
    public void grant(Role role, ChannelManager channel, Permission... permissions) {
        PermissionOverrideManager permissionManager = channel.getChannel().createPermissionOverride(role);
        permissionManager.grant(permissions);
        permissionManager.update();
    }

    /**
     * Locks (prevents users from joining) a channel by its name
     *
     * @param channel The channel name to lock
     */
    public void lockChannel(String channel) {
        getAllChannels().stream().filter(c -> c.getName().equalsIgnoreCase(channel)).forEach(this::lockChannel);
    }

    /**
     * Locks a channel
     *
     * @param channel The channel to lock
     */
    public void lockChannel(Channel channel) {
        deny(guild.getPublicRole(), channel.getManager(), Permission.VOICE_CONNECT, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE);
        // Allow the UHC bot to still send messages
        PermissionOverrideManager self = channel.createPermissionOverride(jda.getSelfInfo());
        self.grant(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE);
        self.update();
    }

    /**
     * Locks all the channels on the discord server
     */
    public void lockChannels() {
        getAllChannels().forEach(this::lockChannel);
    }

    public void queueForDelete(Message message) {
        if (this.messagesToDelete == null)
            this.messagesToDelete = new ArrayList<>();
        if (messagesToDelete.stream().map(Message::getId).filter(id -> id.equalsIgnoreCase(message.getId())).count() == 0)
            this.messagesToDelete.add(message);
    }

    /**
     * Removes the team fron the server
     *
     * @param name The name of the team to remove
     * @throws IllegalArgumentException If the team does not exist on the server
     */
    public void removeTeam(String name) {
        if (!teams.containsKey(name.toLowerCase())) {
            throw new IllegalArgumentException("The provided team does not exist!");
        }
        this.teams.remove(name.toLowerCase()).destroy();
    }

    /**
     * Unlocks a channel by its name
     *
     * @param channel The name of the channel to unlock
     */
    public void unlockChannel(String channel) {
        for (Channel c : getAllChannels()) {
            if (c.getName().equalsIgnoreCase(channel)) {
                unlockChannel(c);
            }
        }
    }

    /**
     * Unlocks a channel
     *
     * @param channel The {@link Channel} to unlock
     */
    public void unlockChannel(Channel channel) {
        for (PermissionOverride permissionOverride : channel.getPermissionOverrides()) {
            PermissionOverrideManager mg = permissionOverride.getManager();
            if (permissionOverride.isRoleOverride() && permissionOverride.getRole().equals(guild.getPublicRole()))
                mg.delete();
            if (permissionOverride.isUserOverride() && permissionOverride.getUser().equals(jda.getSelfInfo()))
                mg.delete();
        }
    }

    public void unlockChannels() {
        getAllChannels().stream().map(Channel::getName).forEach(this::unlockChannel);
    }

    /**
     * Gets all the channels on the discord server
     *
     * @return A {@link List} of all the channels on the discord server
     */
    private List<Channel> getAllChannels() {
        ArrayList<Channel> channels = new ArrayList<>();
        channels.addAll(guild.getVoiceChannels());
        channels.addAll(guild.getTextChannels());
        return channels;
    }

    /**
     * Represents the type of channel
     */
    public enum ChannelType {
        VOICE,
        TEXT
    }
}
