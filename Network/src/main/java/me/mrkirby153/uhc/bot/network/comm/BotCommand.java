package me.mrkirby153.uhc.bot.network.comm;

public class BotCommand {

    protected String messageId;

    protected boolean waiting;

    public void publish(){
        BotCommandManager.instance().publish(this);
    }

    public void publishBlocking(){
        BotCommandManager.instance().publishBlocking(this);
        waiting = true;
        try {
            while(waiting)
                Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
