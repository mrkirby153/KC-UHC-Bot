package me.mrkirby153.uhc.bot.network.comm;


import me.mrkirby153.uhc.bot.network.comm.commands.CommandMessageAck;
import me.mrkirby153.uhc.bot.network.data.RedisConnection;
import me.mrkirby153.uhc.bot.network.data.Utility;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;

public class BotCommandManager {

    private static BotCommandManager manager;

    private HashMap<String, Command> handlerList = new HashMap<>();

    private HashMap<String, BotCommand> waitingCommands = new HashMap<>();

    private JedisPool pool;

    public static BotCommandManager instance() {
        if (manager == null)
            manager = new BotCommandManager();
        return manager;
    }

    public void handle(String commandType, String serialized) {
        Command cmd = handlerList.get(commandType);
        if(cmd == null)
            return;
        Class<? extends BotCommand> command = cmd.command;
        Class<? extends BotCommandHandler> handler = cmd.handler;
        try{
            BotCommandHandler cmdHandler = handler.newInstance();
            BotCommand deserialized = Utility.deserialize(serialized, command);
            cmdHandler.handleCommand(deserialized);
            if(!(deserialized instanceof CommandMessageAck))
                publish(new CommandMessageAck(deserialized.messageId));
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void publish(BotCommand command) {
        try(Jedis j = pool.getResource()){
            String commandType = command.getClass().getSimpleName();
            command.messageId = IdGenerator.generateId();
            String serialized = Utility.serialize(command);
            String channel = CommandListener.SERVER_COMMAND_CHANNEL+":"+commandType;
            j.publish(channel, serialized);
        }
    }

    public void publishBlocking(BotCommand command){
        publish(command);
        waitingCommands.put(command.messageId, command);
    }

    public void register(Class<? extends BotCommand> command, Class<? extends BotCommandHandler> handler) {
        System.out.println("Registering "+command.getCanonicalName()+" with listener "+handler.getCanonicalName());
        this.handlerList.put(command.getSimpleName(), new Command(command, handler));
    }

    public void init(RedisConnection connection){
        pool = Utility.createPool(connection);
        Thread thread = new Thread("Redis Listener"){
            @Override
            public void run() {
                Jedis jedis = pool.getResource();
                String s = CommandListener.SERVER_COMMAND_CHANNEL +":*";
                System.out.println("Started listening on "+s);
                jedis.psubscribe(new CommandListener(), s);
            }
        };
        thread.setDaemon(true);
        thread.start();
        BotCommandManager.instance().register(CommandMessageAck.class, MessageAckHandler.class);
    }


    private class Command {
        private final Class<? extends BotCommand> command;
        private final Class<? extends BotCommandHandler> handler;

        public Command(Class<? extends BotCommand> command, Class<? extends BotCommandHandler> handler) {
            this.command = command;
            this.handler = handler;
        }
    }

    private static class IdGenerator {

        private static long lastMs = -1;

        private static int inc = 1;

        public static String generateId(){
            long timeMs = System.currentTimeMillis();
            long newTime = timeMs * (long) Math.pow(2, 12);
            long procId = (long) Math.pow(2, 8);
            if(lastMs == timeMs){
                if(inc < 255)
                    inc++;
                else
                    return null;
            }
            long finalId = newTime + procId + inc;
            return Long.toOctalString(finalId);
        }
    }

    public static class MessageAckHandler implements BotCommandHandler{

        @Override
        public void handleCommand(BotCommand command) {
            if(command instanceof CommandMessageAck){
                String id = ((CommandMessageAck) command).getMessageAcked();
                BotCommand waiting = BotCommandManager.instance().waitingCommands.get(id);
                if(waiting != null)
                    waiting.waiting = false;
            }
        }
    }
}
