package me.mrkirby153.uhc.bot.network.comm.commands.team;

import me.mrkirby153.uhc.bot.network.comm.commands.TeamCommand;

import java.util.HashMap;
import java.util.UUID;

public class BotCommandAssignTeams extends TeamCommand{

    private HashMap<UUID, String> toAssign;
    public BotCommandAssignTeams(String serverId, String teamName, HashMap<UUID, String> toAssign) {
        super(serverId, teamName);
        this.toAssign = toAssign;
    }

    public HashMap<UUID, String> getTeams() {
        return toAssign;
    }
}
