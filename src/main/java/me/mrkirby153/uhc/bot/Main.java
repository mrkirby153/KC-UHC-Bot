package me.mrkirby153.uhc.bot;


import jline.console.ConsoleReader;
import me.mrkirby153.uhc.bot.discord.DiscordHandler;
import me.mrkirby153.uhc.bot.network.BotCommandHandlers;
import me.mrkirby153.uhc.bot.network.UHCNetwork;
import me.mrkirby153.uhc.bot.network.comm.BotCommandManager;
import me.mrkirby153.uhc.bot.network.comm.commands.*;
import me.mrkirby153.uhc.bot.network.comm.commands.team.BotCommandAssignTeams;
import me.mrkirby153.uhc.bot.network.comm.commands.team.BotCommandNewTeam;
import me.mrkirby153.uhc.bot.network.comm.commands.team.BotCommandRemoveTeam;
import me.mrkirby153.uhc.bot.network.data.RedisConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Main {

    public static Logger logger = LogManager.getLogger("KC-UHC Bot");

    public static boolean isRunning = true;
    public static DiscordHandler discordHandler;
    private static Properties config = new Properties();
//    private static NetworkHandler networkHandler;
    public static UHCNetwork uhcNetwork;

    public static void main(String[] args) throws Exception {
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
        Main.logger.info("Initializing discord bot");
        loadConfiguration();

        String apiKey = config.getProperty("botKey");
        if (apiKey == null || apiKey.isEmpty()) {
            Main.logger.warn("No API key set! Shutting down");
            System.exit(0);
            return;
        }
        discordHandler = new DiscordHandler(apiKey, new File("."));
        discordHandler.init();
        Main.logger.info("Initializing network handler");
        String redisHost = config.getProperty("redis-host");
        int redisPort = Integer.parseInt(config.getProperty("redis-port"));
        String redisPassword = config.getProperty("redis-password");
        uhcNetwork = new UHCNetwork(new RedisConnection(redisHost, redisPort, !redisPassword.equals("")? redisPassword : null));
//        networkHandler = new NetworkHandler(6969);
//        networkHandler.start();
        Main.logger.info("Registering network commands");
        registerCommands();
        Main.logger.info("Initializing console...");
        ConsoleReader consoleReader = new ConsoleReader();
        consoleReader.setBellEnabled(false);

        String line;
        while (isRunning && (line = consoleReader.readLine()) != null) {
            if (line.equalsIgnoreCase("shutdown")) {
                System.exit(0);
                return;
            }
        }
    }


    private static void loadConfiguration() {
        if (!new File("config.properties").exists()) {
            try {
                config.put("botKey", "");
                config.put("redis-host", "localhost");
                config.put("redis-port", "6379");
                config.put("redis-password", "");
                config.store(new FileOutputStream("config.properties"), "KC UHC Bot");
                logger.info("Saved default values, loading configuration");
                loadConfiguration();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileInputStream fos = new FileInputStream("config.properties");
            if (config == null)
                config = new Properties();
            config.load(fos);
            logger.info("Loaded configuraiton successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void registerCommands() {
/*        CommandHandler.registerCommand("link", new LinkServer());
        CommandHandler.registerCommand("newTeam", new CreateTeam());
        CommandHandler.registerCommand("removeTeam", new RemoveTeam());
        CommandHandler.registerCommand("assignTeams", new AssignTeams());
        CommandHandler.registerCommand("assignRole", new AssignRole());
        CommandHandler.registerCommand("linkCode", new GetLinkCode());
        CommandHandler.registerCommand("toLobby", new MoveToLobby());
        CommandHandler.registerCommand("isLinked", new GetLinked());
        CommandHandler.registerCommand("createSpectator", new CreateSpectatorRole());
        CommandHandler.registerCommand("assignSpectator", new AssignSpectatorRole());*/
        BotCommandManager.instance().register(BotCommandLink.class, BotCommandHandlers.LinkServer.class);
        BotCommandManager.instance().register(BotCommandNewTeam.class, BotCommandHandlers.CreateTeam.class);
        BotCommandManager.instance().register(BotCommandRemoveTeam.class, BotCommandHandlers.RemoveTeam.class);
        BotCommandManager.instance().register(BotCommandAssignTeams.class, BotCommandHandlers.AssignTeams.class);
        BotCommandManager.instance().register(BotCommandAssignRole.class, BotCommandHandlers.AssignRole.class);
        BotCommandManager.instance().register(BotCommandToLobby.class, BotCommandHandlers.ToLobby.class);
        BotCommandManager.instance().register(BotCommandCreateSpectator.class, BotCommandHandlers.CreateSpectator.class);
        BotCommandManager.instance().register(BotCommandAssignSpectator.class, BotCommandHandlers.AssignSpectator.class);
    }


    public static class ShutdownHook extends Thread {

        public ShutdownHook() {
            setName("KC-UHC Shutdown");
        }

        @Override
        public void run() {
            if (Main.discordHandler != null)
                Main.discordHandler.shutdown();
/*            if (networkHandler != null)
                Main.networkHandler.shutdown();*/
            Main.logger.info("Goodbye");
        }
    }
}
