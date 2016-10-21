package me.mrkirby153.uhc.bot.network.comm.commands;

import java.util.UUID;

public class BotCommandAssignSpectator extends ServerCommand {
    private final UUID user;

    public BotCommandAssignSpectator(String serverId, UUID user) {
        super(serverId);
        this.user = user;
    }

    public UUID getUser() {
        return user;
    }
}
