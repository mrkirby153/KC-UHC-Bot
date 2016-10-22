package me.mrkirby153.uhc.bot.network.comm.commands;

import me.mrkirby153.uhc.bot.network.comm.BotCommand;

public class CommandMessageAck extends BotCommand {

    private final String messageAcked;

    public CommandMessageAck(String messageAcked) {
        this.messageAcked = messageAcked;
    }

    public String getMessageAcked() {
        return messageAcked;
    }
}
