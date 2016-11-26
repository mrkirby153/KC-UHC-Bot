package me.mrkirby153.uhc.bot.discord;

/**
 * Class to map discord servers to minecraft servers
 */
public class LinkedMinecraftServer {

    private String id;
    private String guild;

    public LinkedMinecraftServer(String serverId, String guild) {
        this.guild = guild;
        this.id = serverId;
    }

    public LinkedMinecraftServer(String serverId, DiscordGuild server) {
        this(serverId, server.getId());
    }

    /**
     * Gets the guild/discord server the server is linked to
     *
     * @return The guild linked to
     */
    public String getGuild() {
        return guild;
    }

    /**
     * Gets the minecraft server's id
     *
     * @return The id of the minecraft server
     */
    public String getId() {
        return id;
    }
}