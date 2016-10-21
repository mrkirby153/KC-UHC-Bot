package me.mrkirby153.uhc.bot.network;

import me.mrkirby153.uhc.bot.network.data.RedisData;

import java.util.UUID;

public class PlayerInfo implements RedisData {

    private final UUID uuid;
    protected transient UHCNetwork network;
    private String discordUser;

    public PlayerInfo(UUID uuid) {
        this.uuid = uuid;
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

    public UUID getUuid() {
        return uuid;
    }

    public void update() {
        if (network != null)
            network.updatePlayer(this);
    }
}
