package me.mrkirby153.uhc.bot.discord;

import me.mrkirby153.uhc.bot.Main;
import me.mrkirby153.uhc.bot.network.BotCommandHandlers;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageHistory;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.managers.ChannelManager;
import net.dv8tion.jda.core.managers.PermOverrideManagerUpdatable;

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

    /**
     * Flag determining if all messages sent while the bot is running should be queued for deletion
     */
    private transient boolean deleteAllMessages = false;


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
        getGuild().getMembers().stream().filter(member -> BotCommandHandlers.AssignTeams.connectedToVice(this, member)).forEach(member -> getGuild().getController().moveVoiceMember(member, vc).queue());
    }

    /**
     * Creates the spectator role on the server
     */
    public void create() {
        Main.logger.info("Initializing server " + this.name);
        if (guild == null) {
            guild = jda.getGuildById(this.id);
        }
        guild.getController().createRole().queue(role -> role.getManager().setName("Spectators").queue((Void) -> {
            this.spectatorRole = role;
            this.spectatorVoiceChannel = (VoiceChannel) getOrCreateChannel("Spectators", ChannelType.VOICE);
            this.denyDefault(spectatorVoiceChannel.getManager(), Permission.VOICE_CONNECT);
            this.grant(spectatorRole, spectatorVoiceChannel.getManager(), Permission.VOICE_CONNECT);
        }));
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
                c.delete().queue();
            }
        }
    }

    /**
     * Deletes all the messages created by the robot
     */
    public void deleteMessages(boolean block) {
        Thread clearThread = new Thread(() -> {
            HashMap<MessageChannel, ArrayList<Message>> messages = new HashMap<>();
            if (messagesToDelete != null) {
                if (messagesToDelete.size() == 1) {
                    messagesToDelete.get(0).deleteMessage().queue();
                } else {
                    for (Message m : messagesToDelete) {
                        ArrayList<Message> arr = messages.get(m.getChannel());
                        if (arr == null)
                            arr = new ArrayList<>();
                        arr.add(m);
                        messages.put(m.getChannel(), arr);
                    }

                    for (Map.Entry<MessageChannel, ArrayList<Message>> e : messages.entrySet()) {
                        if (e.getValue().size() > 1) {
                            ((TextChannel) e.getKey()).deleteMessages(e.getValue()).queue();
                        } else {
                            e.getValue().get(0).deleteMessage().queue();
                        }
                    }
                }
                messagesToDelete.clear();
            }
            // Scan the last 100 messages in each channel for UHCBot messages and delete those
            getAllChannels().stream().filter(c -> c instanceof MessageChannel).map(c -> (MessageChannel) c).forEach(channel -> {
                MessageHistory history = new MessageHistory(channel);
                List<Message> toDelete = new ArrayList<>();
                history.retrievePast(100).queue(m -> {
                    m.stream().filter(m1 -> m1.getAuthor().getId().equals(jda.getSelfUser().getId())).forEach(toDelete::add);
                    if (toDelete.size() > 1) {
                        ((TextChannel) channel).deleteMessages(toDelete).queue();
                    } else if (toDelete.size() == 1) {
                        toDelete.get(0).deleteMessage().queue();
                    }
                });

            });
        });
        clearThread.setName("DeleteMessageThread");
        clearThread.start();
        if(block)
            try {
                clearThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
    }

    public void deleteMessages(){
        this.deleteMessages(false);
    }

    /**
     * Denies permissions for the given role on the server
     *
     * @param role        The role to deny
     * @param channel     The channel to deny in
     * @param permissions The permissions to deny
     */
    public void deny(Role role, ChannelManager channel, Permission... permissions) {
        boolean updated = false;
        for (PermissionOverride o : channel.getChannel().getRolePermissionOverrides()) {
            if (o.getRole().equals(role)) {
                updated = true;
                o.getManager().deny(permissions).queue();
                break;
            }
        }
        if (!updated)
            channel.getChannel().createPermissionOverride(role).queue(p -> p.getManager().deny(permissions).queue());
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
            spectatorRole.delete().queue();
        if (spectatorVoiceChannel != null)
            spectatorVoiceChannel.delete().queue();
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
                Channel channel = null;
                while (channel == null) {
                    try {
                        channel = guild.getController().createVoiceChannel(name).block();
                    } catch (RateLimitedException e) {
                        try {
                            Thread.sleep(e.getRetryAfter());
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
                return channel;
            case TEXT:
                channel = null;
                while (channel == null) {
                    try {
                        channel = guild.getController().createTextChannel(name).block();
                    } catch (RateLimitedException e) {
                        try {
                            Thread.sleep(e.getRetryAfter());
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
                return channel;
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
        boolean updated = false;
        for (PermissionOverride o : channel.getChannel().getRolePermissionOverrides()) {
            if (o.getRole().equals(role)) {
                updated = true;
                o.getManager().grant(permissions).queue();
            }
        }
        if (!updated)
            channel.getChannel().createPermissionOverride(role).queue(p -> p.getManagerUpdatable().grant(permissions).update().queue());
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
        boolean updated = false;
        for (PermissionOverride o : channel.getMemberPermissionOverrides()) {
            if (o.getMember().equals(guild.getMember(jda.getSelfUser()))) {
                updated = true;
                o.getManager().grant(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE).queue();
                break;
            }
        }
        if (!updated)
            channel.createPermissionOverride(guild.getMember(jda.getSelfUser())).queue(o ->
                    o.getManagerUpdatable().grant(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE).update().queue());
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

    public void setDeleteAllMessages(boolean deleteAllMessages) {
        this.deleteAllMessages = deleteAllMessages;
    }

    public boolean shouldDeleteAllMessages() {
        return deleteAllMessages;
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
            PermOverrideManagerUpdatable mg = permissionOverride.getManagerUpdatable();
            if (permissionOverride.isRoleOverride() && permissionOverride.getRole().equals(guild.getPublicRole())) {
                mg.getPermissionOverride().delete().queue();
            }
            if (permissionOverride.isMemberOverride() && permissionOverride.getMember().equals(guild.getMember(jda.getSelfUser()))) {
                mg.getPermissionOverride().delete().queue();
            }
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
