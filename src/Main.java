import java.io.IOException;
import java.util.Scanner;


import DiscordBot.DiscordBot;
import FileHandler.ConfigHandler;

public class Main {
    public static void main(String[] args) throws InterruptedException {


        // Get info from the config file
        // And
        // Connect Discord Bot
        DiscordBot bot = new DiscordBot(new ConfigHandler().getConfigInfo());
        bot.start();

        // Add thread to listen for user exit
        Thread exitListener = new Thread() {

            public void run() {
                System.out.println("Type 'exit' to close program");
                Scanner userIN = new Scanner(System.in);

                while(true) {

                    String userCommand = userIN.nextLine();

                    if (userCommand.toLowerCase().equals("exit")) {
                        Runtime.getRuntime().exit(0);
                    }
                }
            }
        };

        exitListener.start();
    }
}
