package DiscordBot;

import java.io.IOException;

import FileHandler.ConfigHandler.CONFIG_INFO;
import ServerManager.MinecraftManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class DiscordBot extends Thread {
        
        // Final Values
        private final String TOKEN;
        private final Long GUILDID;
        private final Long CHANNELID;
        private final String MINECRAFT_SERVER_PATH;

        private boolean isOnline = false;

        // Discord Bot
        private JDA discordBot;
        private Guild currentServer;
        private TextChannel textChannel;

        // Minecraft Server
        MinecraftManager minecraftManager;

        public DiscordBot(CONFIG_INFO configInfo) {
                TOKEN = configInfo.DiscordBot_token;
                GUILDID = configInfo.Discord_Guild_ID;
                CHANNELID = configInfo.Discord_TextChannel_ID;
                MINECRAFT_SERVER_PATH = configInfo.Minecraft_Start_Script;

                initBot();
                setCorrectServerAndChannel();
                createCommandSet();
                prepareMinecraftController();
                sendMessage("I am awake now ;)"); // Test connection

                this.isOnline = true; // After set up is done and test complete, set status to online
        }

        // Initialize bot
        private void initBot() {
                        discordBot = JDABuilder.createDefault(TOKEN).setActivity(Activity.listening("your minecraft server!")).build();
                        
                        // Wait for bot to connect and initialize
                        try {
                                discordBot.awaitReady();
                            } catch (InterruptedException e) {
                                // Throw initialization exception
                                throw new ExceptionInInitializerError("Failed to connect discord bot!!!");
                        }
        }

        // Get correct text channel from options
        private void setCorrectServerAndChannel() {
                this.currentServer = this.discordBot.getGuildById(GUILDID);
                this.textChannel = this.currentServer.getTextChannelById(CHANNELID);

                // Check that channel was acquired
                if (this.textChannel == null) {
                        throw new ExceptionInInitializerError("Unable to connect to text channel :(");
                }
        }

        private void createCommandSet() {
                this.currentServer.updateCommands().addCommands(
                        Commands.slash("echo", "Repeats messages back to you.")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL, Permission.MODERATE_MEMBERS))
                        .addOption(OptionType.STRING, "message", "The message to repeat.")
                        .addOption(OptionType.INTEGER, "times", "The number of times to repeat the message.")
                        .addOption(OptionType.BOOLEAN, "ephemeral", "Whether or not the message should be sent as an ephemeral message."),

                        Commands.slash("minecraft", "Command the minecraft server")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL, Permission.MODERATE_MEMBERS))
                        .addOption(OptionType.STRING, "command", "This command will be passed to the minecaft server.")
                        .addOption(OptionType.STRING, "manager", "Start, Stop, or Check on minecraft server status.")
                ).queue();

                discordBot.addEventListener(new OnSlashCommand()); // Set command listeners
        }

        private void prepareMinecraftController() {
                this.minecraftManager = new MinecraftManager(MINECRAFT_SERVER_PATH, this);
        }

        private void destroyMinecraftController() {
                try {
                        this.minecraftManager.anakinMethod();
                        this.minecraftManager = null;
                        Runtime.getRuntime().runFinalization(); // Just make sure the entire process is fully removed and we are ready from scratch asap

                } catch (IOException e) {
                        System.err.println("Fatal Error: Failed to destroy minecraft method!!");
                        e.printStackTrace();
                }
        }

        private void startMinecraftServer() {
                try {
                        prepareMinecraftController();
                        this.minecraftManager.startServer();
                        this.minecraftManager.start();
                         
                } catch (java.lang.IllegalThreadStateException e) {
                        sendMessage("Already have an active server output");
                }

                sendMessage("Server start attempted");
        }

        private void checkMinecraftServer() {
                // First check if its down for sure
                if(this.minecraftManager == null) {
                        sendMessage("Server is down right now");
                }

                // Now check to see if the server is active (Just to be thorough)
                boolean isAlive = this.minecraftManager.isAlive();
                sendMessage("Server checked");

                if(isAlive) {
                        sendMessage("The server should be up");
                } else {
                        sendMessage("The server is currently down");
                }
        }

        private void sendCommandMinecraftServer(String cmd) {
                this.minecraftManager.sendCommandToServer(cmd);
        }

        private void stopMinecraftServer() {
                this.minecraftManager.stopServer();
                destroyMinecraftController();
        }

        // Send messages, keep it synchronized for other threads
        public synchronized void sendMessage(String msg) {
                this.textChannel.sendMessage(msg).queue();
        }

        // Listen for commands from server admins
        @Override
        public void run() {
                // Run until discord bot is shutdown
                try {
                        this.discordBot.awaitShutdown();
                } catch (InterruptedException e) {
                        System.err.println("Failed to wait for discord bot to shutdown ;(");
                        e.printStackTrace();
                }
        }


// -------------------------------- Event classes to handle interactions --------------------------------

// ----------------------------- Handle Minecraft Commands -----------------------------
    private class OnSlashCommand extends ListenerAdapter {

        @Override
        public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

            // Will add more to gaurd clause as more commands become used
            if (!event.getName().equals("minecraft")) {
                event.reply("Please you use an available command").queue();
                return;
            }

            // Check if empty or more than one argument
            if (event.getOptions().isEmpty() || event.getOptions().size() > 1) {
                event.reply("Please use options. Note: only use one at a time.").queue();
                return;
            }

            // Check if command option
            if (event.getOptions().get(0).getName().equals("command")) {
                String minecraftcmd = event.getOption("command").getAsString();

                if(minecraftcmd.toLowerCase().equals("stop")) { 
                        event.reply("Unable to stop the server like this, use manager option instead").queue();
                        return; 
                }

                DiscordBot.this.sendCommandMinecraftServer(minecraftcmd);

                event.reply("Command will be forwarded to the minecraft server!").queue();
                return;
            }

            // Check if manager option
            if (event.getOptions().get(0).getName().equals("manager")) {
                // Tell discord we received the command, send a thinking... message to the user
                event.deferReply().queue(); 

                // Check the server in whatever way requested
                switch (event.getOption("manager").getAsString().toLowerCase()) {
                    case "start":
                        event.getHook().sendMessage("Starting your server for you right now...").queue();
                        DiscordBot.this.startMinecraftServer();
                        return;

                    case "check":
                        event.getHook().sendMessage("Lemme go check for you ;)").queue();
                        DiscordBot.this.checkMinecraftServer();
                        return;

                    case "stop":
                        event.getHook().sendMessage("Give me a sec to stop it!").queue();
                        DiscordBot.this.stopMinecraftServer();
                        return;
                                
                    default:
                        event.getHook().sendMessage("I cant do that sorry").queue();
                        return;
                }
            }

            // If somehow the event made it to the end of the method
            event.reply("I do not know what to do!").queue();
        } 
    }
}