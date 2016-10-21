package me.mrkirby153.uhc.bot.network.comm;

public class BotCommand {

    public void publish(){
        BotCommandManager.instance().publish(this);
    }
}
