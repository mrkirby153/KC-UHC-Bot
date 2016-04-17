package me.mrkirby153.uhc.bot.discord;

import me.mrkirby153.uhc.bot.Main;
import me.mrkirby153.uhc.bot.cache.Cache;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.entities.*;
import net.dv8tion.jda.events.guild.GuildJoinEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.exceptions.PermissionException;
import net.dv8tion.jda.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.HashMap;
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
    private HashMap<String, UUID> codeToPlayerMap = new HashMap<>();
    private HashMap<UUID, String> uuidToNameMap = new HashMap<>();
    private Cache<UUID, User> uuidToDiscordCache = new Cache<>(1000 * 60 * 60 * 6);

    public DiscordHandler(String botToken, File workingDir) {
        this.token = botToken;
        this.workingDir = workingDir;
    }

    /**
     * Initializes the robot
     */
    public void init() {
        try {
            jda = new JDABuilder(this.token).addListener(this).buildBlocking();
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }
        servers = new ServerHandler(this.jda, new File(workingDir, "servers.json"));
        ServerHandler.DiscordServer.jda = this.jda;
        servers.init();
        Main.logger.info("Creating discord channels");
        for (ServerHandler.DiscordServer s : servers.connectedServers()) {
            initChannels(s);
        }
        ready = true;
    }

    /**
     * Creates the channels on the given server
     *
     * @param s The server
     */
    public void initChannels(ServerHandler.DiscordServer s, boolean triedBefore) {
        try {
            TextChannel tc = s.createTextChannel("uhc-link");
            tc.getManager().setTopic("[KC-UHC] Please log into the server and follow the instructions!");
            tc.getManager().update();
        } catch (PermissionException e) {
            // Delay execution
            if (triedBefore) {
                servers.getById(s.getId()).getGuild().getPublicChannel().sendMessage("**ERROR:** __Could not create the required channels because I do not have permission!__");
                return;
            }
            Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        Main.logger.info("Delaying for 1 second to allow role to propagate");
                        Thread.sleep(1000);
                        initChannels(s, true);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            };
            t.setName("ChannelCreator");
            t.start();
        }
    }

    /**
     * Creates channels on the given server
     *
     * @param s The server
     */
    public void initChannels(ServerHandler.DiscordServer s) {
        initChannels(s, false);
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
        ServerHandler.DiscordServer ds = getServerHandler().getById(event.getGuild().getId());
        if (message.startsWith("!uhcbot")) {
            m.deleteMessage();
            // hardcode link commands now
            String[] parts = message.split(" ");
            if (parts.length == 0)
                return;
            if(parts[1].equalsIgnoreCase("linked")){
                if(uuidToDiscordCache.containsvalue(event.getAuthor())){
                    event.getChannel().sendMessage(event.getAuthor().getAsMention()+", you have linked your discord account!");
                } else {
                    event.getChannel().sendMessage(event.getAuthor().getAsMention()+", you **haven't** linked your discord account!");
                }
            }
            if (parts[1].equalsIgnoreCase("relink")) {
                linkGuild(event.getGuild());
            }
            if (parts[1].equalsIgnoreCase("id")) {
                event.getGuild().getPublicChannel().sendMessage("The server id is `" + event.getGuild().getId() + "`");
            }
            if (parts[1].equalsIgnoreCase("link")) {
                if (event.getChannel() != ds.getTextChannel("uhc-link"))
                    return;
                String code = parts[2];
                link(code, event.getAuthor(), (Channel) event.getChannel());
            }
            if (parts[1].equalsIgnoreCase("part")) {
                Main.logger.info("Leaving server " + event.getGuild().getName());
                if(ds != null)
                    ds.destroy();
                servers.removeConnectedServer(event.getGuild().getId());
                event.getGuild().getPublicChannel().sendMessage("Goodbye.");
                event.getGuild().getManager().leave();
            }
        }
    }

    /**
     * Generates a link code. (Used to link minecraft names to the server)
     *
     * @param forPlayer  The player to generate the code for
     * @param playerName The player's name
     * @return The player's unique lnik code
     */
    public String generateLinkCode(UUID forPlayer, String playerName) {
        String code = generateCode(5);
        codeToPlayerMap.put(code, forPlayer);
        uuidToNameMap.put(forPlayer, playerName);
        return code;
    }

    /**
     * Checks if the link exists
     *
     * @param code The code to verify existence
     * @return True if the code exists, false if it doesn't
     */
    public boolean linkExists(String code) {
        return codeToPlayerMap.containsKey(code);
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
        if (!linkExists(code)) {
            ((MessageChannel) inChannel).sendMessage(user.getAsMention() + ", that code was invalid!");
            return false;
        }
        UUID u = codeToPlayerMap.remove(code);
        String name = uuidToNameMap.get(u);
        long expiresOn = System.currentTimeMillis() + uuidToDiscordCache.getExpireTime();
        uuidToDiscordCache.put(u, user);
        if (inChannel instanceof MessageChannel) {
            ((MessageChannel) inChannel).sendMessage(user.getAsMention() + ", you have linked your account to the minecraft name " + name + ". This link is valid for 6 hours, until at which, it will" +
                    " automatically expire");
            SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss");
            ((MessageChannel) inChannel).sendMessage("This link will expire on " + sdf.format(expiresOn));
        }
        return true;
    }

    /**
     * Gets the Discord {@link User} whose account is linked to the given UUID
     *
     * @param u The uuid
     * @return The user
     */
    public User getUser(UUID u) {
        return uuidToDiscordCache.get(u);
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
    private void linkGuild(Guild guild) {
        Main.logger.info("Linked server " + guild.getName() + "!");
        guild.getPublicChannel().sendMessage("ID retrieval successful! For reference, this guild's id is `" + guild.getId() + "`");
        servers.addConnectedServer(guild);
        initChannels(servers.getById(guild.getId()));
    }

    public void shutdown() {
        for (ServerHandler.DiscordServer s : servers.connectedServers()) {
            s.destroy();
        }
        Main.logger.info("Disconnecting from Discord...");
        jda.shutdown();
    }

    /**
     * Gets the {@link ServerHandler} handler used for this discord handler
     *
     * @return A {@link ServerHandler}
     */
    public ServerHandler getServerHandler() {
        return servers;
    }
}
