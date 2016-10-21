package me.mrkirby153.uhc.bot.network;

import me.mrkirby153.uhc.bot.network.comm.BotCommandManager;
import me.mrkirby153.uhc.bot.network.data.RedisConnection;
import me.mrkirby153.uhc.bot.network.data.RedisDataStore;

import java.util.UUID;

public class UHCNetwork {

    private RedisDataStore<PlayerInfo> playerData;

    public UHCNetwork(RedisConnection connection) {
        playerData = new RedisDataStore<>(PlayerInfo.class, "players", connection);
        BotCommandManager.instance().init(connection);
    }

    public RedisDataStore<PlayerInfo> getDatastore() {
        return playerData;
    }

    public PlayerInfo getPlayerInfo(UUID uuid) {
        PlayerInfo info = playerData.getElement(uuid.toString());
        info.network = this;
        return info;
    }

    public void updatePlayer(PlayerInfo info){
        playerData.updateElement(info.getIdentifier(), info);
    }
}
