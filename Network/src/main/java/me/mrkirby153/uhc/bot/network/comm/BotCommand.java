package me.mrkirby153.uhc.bot.network.comm;

public class BotCommand {

    protected String messageId;

    protected boolean waiting;

    private transient long waitUntil;

    public void publish(){
        BotCommandManager.instance().publish(this);
    }

    public void publishBlocking(){
        BotCommandManager.instance().publishBlocking(this);
        waiting = true;
        waitUntil = System.currentTimeMillis() + 10000;
        try {
            while(waiting) {
                Thread.sleep(1);
                if(System.currentTimeMillis() > waitUntil){
                    System.err.println("Timed out waiting for response ("+this.getClass().getCanonicalName()+")");
                    waiting = false;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
