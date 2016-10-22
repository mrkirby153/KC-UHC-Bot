package me.mrkirby153.uhc.bot.network.comm;


import me.mrkirby153.uhc.bot.network.data.RedisConnection;
import me.mrkirby153.uhc.bot.network.data.Utility;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;

public class BotCommandManager {

    private static BotCommandManager manager;

    private HashMap<String, Command> handlerList = new HashMap<>();

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
            cmdHandler.handleCommand(Utility.deserialize(serialized, command));
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void publish(BotCommand command) {
        try(Jedis j = pool.getResource()){
            String commandType = command.getClass().getSimpleName();
            String serialized = Utility.serialize(command);
            String channel = CommandListener.SERVER_COMMAND_CHANNEL+":"+commandType;
            j.publish(channel, serialized);
        }
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
    }


    private class Command {
        private final Class<? extends BotCommand> command;
        private final Class<? extends BotCommandHandler> handler;

        public Command(Class<? extends BotCommand> command, Class<? extends BotCommandHandler> handler) {
            this.command = command;
            this.handler = handler;
        }
    }
}
