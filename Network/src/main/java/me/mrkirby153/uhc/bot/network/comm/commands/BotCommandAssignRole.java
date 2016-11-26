package me.mrkirby153.uhc.bot.network.comm.commands;

import java.util.UUID;

@Deprecated
public class BotCommandAssignRole extends ServerCommand {

    private final UUID user;
    private final String role;

    public BotCommandAssignRole(String serverId, UUID user, String role) {
        super(serverId);
        this.user = user;
        this.role = role;
    }

    public UUID getUser() {
        return user;
    }

    public String getRole() {
        return role;
    }
}
