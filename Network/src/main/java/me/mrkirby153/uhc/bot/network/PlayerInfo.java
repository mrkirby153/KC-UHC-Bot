package me.mrkirby153.uhc.bot.network;

import me.mrkirby153.uhc.bot.network.data.RedisData;

import java.util.UUID;

public class PlayerInfo implements RedisData {

    private final UUID uuid;
    private final String name;
    protected transient UHCNetwork network;
    private String discordUser;
    private String linkCode;
    private boolean linked;

    public PlayerInfo(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public String getDiscordUser() {
        return discordUser;
    }

    public void setDiscordUser(String discordUser) {
        this.discordUser = discordUser;
    }

    @Override
    public String getIdentifier() {
        return uuid.toString();
    }

    public String getLinkCode() {
        return linkCode;
    }

    public void setLinkCode(String linkCode) {
        this.linkCode = linkCode;
    }

    public String getName() {
        return name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isLinked() {
        return linked;
    }

    public void setLinked(boolean linked) {
        this.linked = linked;
    }

    public void update() {
        if (network != null)
            network.updatePlayer(this);
    }
}
