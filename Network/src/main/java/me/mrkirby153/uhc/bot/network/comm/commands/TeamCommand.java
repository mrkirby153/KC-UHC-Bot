package me.mrkirby153.uhc.bot.network.comm.commands;

public class TeamCommand extends ServerCommand {

    private final String teamName;

    public TeamCommand(String serverId, String teamName) {
        super(serverId);
        this.teamName = teamName;
    }

    public String getTeamName() {
        return teamName;
    }
}
