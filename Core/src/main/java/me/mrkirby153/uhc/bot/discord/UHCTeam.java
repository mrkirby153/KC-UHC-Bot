package me.mrkirby153.uhc.bot.discord;


import me.mrkirby153.uhc.bot.Main;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

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
    private Channel textChannel;
    /**
     * The voice channel assigned to this team
     */
    private Channel voiceChannel;

    /**
     * The team role on the discord server
     */
    private Role teamRole;

    private HashSet<Member> assignedUsers = new HashSet<>();


    public UHCTeam(DiscordGuild guild, String name) {
        this.name = name;
        this.guild = guild;
    }

    /**
     * Assigns the user this team's role
     *
     * @param member The user to assign
     */
    public void assignUser(Member member) {
        if (member == null)
            return;
        this.guild.getGuild().getController().addRolesToMember(member, teamRole).queue();
        this.assignedUsers.add(member);
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
        teamRole.delete().queue();
        textChannel.delete().queue();
        voiceChannel.delete().queue();
    }

    /**
     * Removes the user from this team
     *
     * @param user The user to remove
     */
    public void unassign(Member user) {
        this.assignedUsers.remove(user);
        this.guild.getGuild().getController().removeRolesFromMember(user, teamRole).queue();
    }

    /**
     * Creates the text and voice channels on the guild
     */
    private void createChannels() {
        Main.logger.info("Creating team " + this.name);

        textChannel = this.guild.getOrCreateChannel("team-" + this.name.toLowerCase().replaceAll("\\s", "-"), DiscordGuild.ChannelType.TEXT);
        voiceChannel = this.guild.getOrCreateChannel("Team " + this.name, DiscordGuild.ChannelType.VOICE);

        // Assign permissions
        guild.denyDefault(textChannel.getManager(), Permission.MESSAGE_READ, Permission.MESSAGE_WRITE);
        guild.denyDefault(voiceChannel.getManager(), Permission.VOICE_CONNECT, Permission.VOICE_SPEAK);

        guild.grant(this.teamRole, textChannel.getManager(), Permission.MESSAGE_READ, Permission.MESSAGE_WRITE);
        guild.grant(this.teamRole, voiceChannel.getManager(), Permission.VOICE_CONNECT, Permission.VOICE_SPEAK);
    }

    /**
     * Creates the roles on the server
     */
    private void createRole() {
        while(teamRole == null){
            try {
                teamRole = this.guild.getGuild().getController().createRole().block();
            } catch (RateLimitedException e) {
                try {
                    Thread.sleep(e.getRetryAfter());
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
        teamRole.getManager().setName(this.name).queue();
    }


}
