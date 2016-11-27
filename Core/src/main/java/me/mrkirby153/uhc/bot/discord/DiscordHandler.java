package me.mrkirby153.uhc.bot.discord;

import me.mrkirby153.uhc.bot.Main;
import me.mrkirby153.uhc.bot.network.PlayerInfo;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.RestAction;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;

/**
 * Handles interaction between the bot manager and the discord server
 */
public class DiscordHandler extends ListenerAdapter {

    private final String token;
    private final File workingDir;
    private final Random random = new SecureRandom();

    private JDA jda;
    private ServerHandler servers;
    private boolean ready = false;

    public DiscordHandler(String botToken, File workingDir) {
        this.token = botToken;
        this.workingDir = workingDir;
    }

    /**
     * Gets the {@link User} linked to this {@link UUID}
     *
     * @param uuid The uuid to check
     * @return The user
     */
    public User getLinkedUser(UUID uuid) {
        return jda.getUserById(Main.uhcNetwork.getPlayerInfo(uuid).getDiscordUser());
    }

    /**
     * Gets the {@link ServerHandler} handler used for this discord handler
     *
     * @return A {@link ServerHandler}
     */
    public ServerHandler getServerHandler() {
        return servers;
    }

    /**
     * Gets the Discord {@link User} whose account is linked to the given UUID
     *
     * @param u The uuid
     * @return The user
     */
    public User getUser(UUID u) {
        PlayerInfo info = Main.uhcNetwork.getPlayerInfo(u);
        if (info != null) {
            return jda.getUserById(info.getDiscordUser());
        } else {
            return null;
        }
    }

    /**
     * Initializes the robot
     */
    public void init() {
        try {
            jda = new JDABuilder(AccountType.BOT).setToken(this.token).addListener(this).buildBlocking();
        } catch (LoginException | InterruptedException | RateLimitedException e) {
            e.printStackTrace();
        }
        servers = new ServerHandler(this.jda, new File(workingDir, "servers.json"));
        DiscordGuild.setJda(this.jda);
        servers.init();
    }

    /**
     * Performs the link of the discord name to the uuid
     *
     * @param code      The code used in the linking process
     * @param user      The user to link
     * @param inChannel The channel this link is occurring in
     * @return True if the link was successful, fase if it wasn't
     */
    public boolean link(String code, User user, Channel inChannel) {
        PlayerInfo info = Main.uhcNetwork.getPlayerByLinkCode(code);
        if (info == null) {
            ((MessageChannel) inChannel).sendMessage(user.getAsMention() + ", that code was invalid!");
            return false;
        }
        info.setDiscordUser(user.getId());
        info.setLinked(true);
        info.setLinkCode("");
        info.update();
        RestAction<Message> m = ((MessageChannel) inChannel).sendMessage(user.getAsMention() + ", you have linked your account to the minecraft name `" + info.getName() + "`");
        m.queue();
        return true;
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        Guild guild = event.getGuild();
        guild.getPublicChannel().sendMessage("Greetings! I am KC UHC Bot and I will be facilitating the movement of users around your discord server. In order to function" +
                " properly, I need the following permissions: ` Manage Roles, Manage Channels, Kick Members, Create Instant Invite, Read Messages, Send Messages, Manage Messages, " +
                "Connect to voice, and Use Voice Activity`");
        guild.getPublicChannel().sendMessage("Please verify that I have these permissions.");
        linkGuild(guild);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message m = event.getMessage();
        String message = m.getRawContent();
        DiscordGuild ds = getServerHandler().getById(event.getGuild().getId());
        if(ds.shouldDeleteAllMessages())
            ds.queueForDelete(m);
        if(m.getAuthor().getId().equals(jda.getSelfUser().getId())){
            ds.queueForDelete(m);
        }
        if (message.startsWith("!uhcbot")) {
            m.deleteMessage().queue();
            // hardcode link commands now
            String[] parts = message.split(" ");
            if (parts.length == 0)
                return;
            if (parts[1].equalsIgnoreCase("relink")) {
                DiscordGuild guild = linkGuild(event.getGuild());
                guild.create();
            }
            if(ds == null)
                return;
            if (parts[1].equalsIgnoreCase("linked")) {
                PlayerInfo playerInfo = Main.uhcNetwork.getPlayerInfo(event.getAuthor().getId());
                if (playerInfo != null) {
                    event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", you " + ((playerInfo.isLinked()) ? "have " : "**haven't** ") + "linked your discord account!").queue();
                } else {
                    event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", you **haven't** linked your discord account!").queue();
                }
            }
            if (parts[1].equalsIgnoreCase("id")) {
                event.getGuild().getPublicChannel().sendMessage("The server id is `" + event.getGuild().getId() + "`").queue();
            }
            if (parts[1].equalsIgnoreCase("link")) {
                String code = parts[2];
                link(code, event.getAuthor(), (Channel) event.getChannel());
            }
            if (parts[1].equalsIgnoreCase("part")) {
                Main.logger.info("Leaving server " + event.getGuild().getName());
                ds.deleteMessages();
                ds.destroy();
                servers.removeConnectedServer(event.getGuild().getId());
                event.getGuild().getPublicChannel().sendMessage("Goodbye.").queue();
                event.getGuild().leave().queue();
            }
            if (parts[1].equalsIgnoreCase("clearMessages")) {
                ds.deleteMessages();
                event.getChannel().sendMessage("Deleting messages on server...").queue();
            }
            if(parts[1].equalsIgnoreCase("lockChannels")){
                event.getChannel().sendMessage("Locking all channels on the server").queue();
                ds.lockChannels();
            }
            if(parts[1].equalsIgnoreCase("unlockChannels")){
                event.getChannel().sendMessage("Unlocking all channels on the server").queue();
                ds.unlockChannels();
            }
            if(parts[1].equalsIgnoreCase("toggleDeleteAll")){
                ds.setDeleteAllMessages(!ds.shouldDeleteAllMessages());
                event.getChannel().sendMessage("Deleting all messages set to `"+ds.shouldDeleteAllMessages()+"`").queue();
            }
            if(parts[1].equalsIgnoreCase("unlockChannel") || parts[1].equalsIgnoreCase("lockChannel")){
                String channel = "";
                for(int i = 2; i < parts.length; i++){
                    channel += parts[i]+" ";
                }
                channel = channel.trim();
                if(parts[1].equalsIgnoreCase("unlockChannel")) {
                    event.getChannel().sendMessage("Unlocking channel `" + channel + "`").queue();
                    ds.unlockChannel(channel);
                } else {
                    event.getChannel().sendMessage("Locking channel `"+channel+"`").queue();
                    ds.lockChannel(channel);
                }
            }
            ds.queueForDelete(m);
        }
    }

    public void shutdown() {
        for (DiscordGuild s : servers.connectedServers()) {
            s.destroy();
        }
        Main.logger.info("Disconnecting from Discord...");
        jda.shutdown();
    }

    /**
     * Generates a unique code
     *
     * @param length The size of the code
     * @return The code's size
     */
    private String generateCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        String code = "";
        for (int i = 0; i < length; i++) {
            code += chars.charAt(random.nextInt(chars.length()));
        }
        return code;
    }

    /**
     * Retrieves the guild's ID
     *
     * @param guild The guild
     */
    private DiscordGuild linkGuild(Guild guild) {
        Main.logger.info("Linked server " + guild.getName() + "!");
        guild.getPublicChannel().sendMessage("ID retrieval successful! For reference, this guild's id is `" + guild.getId() + "`").queue();
        return servers.addConnectedServer(guild);
    }
}
