package me.mrkirby153.uhc.bot;


import jline.console.ConsoleReader;
import me.mrkirby153.uhc.bot.discord.DiscordHandler;
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

    public static void main(String[] args) throws Exception{
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
        Main.logger.info("Initializing discord bot");
        loadConfiguration();

        String apiKey = config.getProperty("botKey");
        if(apiKey == null || apiKey.isEmpty()){
            Main.logger.warn("No API key set! Shutting down");
            System.exit(0);
            return;
        }
        discordHandler = new DiscordHandler(apiKey, new File("."));
        discordHandler.init();
        Main.logger.info("Initializing console...");
        ConsoleReader consoleReader = new ConsoleReader();
        consoleReader.setBellEnabled(false);

        String line;
        while(isRunning && (line = consoleReader.readLine()) != null){
            if(line.equalsIgnoreCase("shutdown")){
                System.exit(0);
                return;
            }
        }
    }


    private static void loadConfiguration(){
        if(!new File("config.properties").exists()){
            try {
                config.put("botKey", "");
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
            if(config == null)
                config = new Properties();
            config.load(fos);
            logger.info("Loaded configuraiton successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static class ShutdownHook extends Thread{

        public ShutdownHook(){
            setName("KC-UHC Shutdown");
        }
        @Override
        public void run() {
            if(Main.discordHandler != null)
                Main.discordHandler.shutdown();
            Main.logger.info("Goodbye");
        }
    }
}
