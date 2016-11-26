package me.mrkirby153.uhc.bot.discord;

import me.mrkirby153.uhc.bot.Main;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.managers.ChannelManager;
import net.dv8tion.jda.managers.RoleManager;

import java.util.HashSet;


/**
 * A UHC team on the Discord guild
 */
public class UHCTeam {

    /**
     * The name of the team
     */
    private final String name;
    /**
     * The guild this team belongs to
     */
    private final DiscordGuild guild;

    /**
     * The text channel assigned to this team
     */
    private ChannelManager textChannel;
    /**
     * The voice channel assigned to this team
     */
    private ChannelManager voiceChannel;

    /**
     * The team role on the discord server
     */
    private RoleManager teamRole;

    private HashSet<User> assignedUsers = new HashSet<>();

    public UHCTeam(DiscordGuild guild, String name) {
        this.name = name;
        this.guild = guild;
    }

    /**
     * Assigns the user this team's role
     *
     * @param user The user to assign
     */
    public void assignUser(User user) {
        if (user == null)
            return;
        this.guild.getGuild().getManager().addRoleToUser(user, teamRole.getRole()).update();
        this.assignedUsers.add(user);
    }

    /**
     * Creates the team on the server
     */
    public void create() {
        this.createRole();
        this.createChannels();
    }

    /**
     * Destroys the team on the server
     */
    public void destroy() {
        teamRole.delete();
        textChannel.delete();
        voiceChannel.delete();
    }

    /**
     * Removes the user from this team
     *
     * @param user The user to remove
     */
    public void unassign(User user) {
        this.assignedUsers.remove(user);
        this.guild.getGuild().getManager().removeRoleFromUser(user, teamRole.getRole()).update();
    }

    /**
     * Removes this role from all the assigned
     */
    public void unassignAll() {
        this.assignedUsers.forEach(this::unassign);
    }

    /**
     * Creates the text and voice channels on the guild
     */
    private void createChannels() {
        Main.logger.info("Creating team " + this.name);

        textChannel = this.guild.getOrCreateChannel("team-" + this.name.toLowerCase().replaceAll("\\s", "-"), DiscordGuild.ChannelType.TEXT).getManager();
        voiceChannel = this.guild.getOrCreateChannel("Team " + this.name, DiscordGuild.ChannelType.VOICE).getManager();

        // Assign permissions
        guild.denyDefault(textChannel, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE);
        guild.denyDefault(voiceChannel, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK);

        guild.grant(this.teamRole.getRole(), textChannel, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE);
        guild.grant(this.teamRole.getRole(), voiceChannel, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK);
    }

    /**
     * Creates the roles on the server
     */
    private void createRole() {
        teamRole = this.guild.getGuild().createRole();
        teamRole.setName(this.name);
        teamRole.update();
    }


}
