package ServerManager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import DiscordBot.DiscordBot;

public class MinecraftManager extends Thread {

        private ArrayList<Long> PROCCESS_IDS = new ArrayList<Long>();

        private DiscordBot discordBot;
        private PrintWriter mineServerInput = null;

        private Process process;
        private String startScript;
        private boolean isActive = false;
        private boolean isStopping = false;

        public MinecraftManager(String minecraftScript, DiscordBot discordBot) {
                this.discordBot = discordBot;
                this.startScript = minecraftScript;
                //startScript = new File(minecraftScript);

                // Create a quick thread that will kill all the children processes when the 
                // App is closed, this however wont work if the parent process if force killed
                Thread closeChildThread = new Thread() {
                        public void run() {
                                try {
                                        System.out.println("Shutting down child processes");
                                        anakinMethod();
                                } catch (IOException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                }
                        }
                };
                    
                Runtime.getRuntime().addShutdownHook(closeChildThread); 
        }

        // Start server using start script
        public void startServer() {

                // Alert discord to try stopping the server before running it again
                if(this.isActive) { this.discordBot.sendMessage("Server is already running, try stopping it first."); return; }

                String startCommand = createStartCommand();
                ProcessBuilder newServerProcess = new ProcessBuilder();
                newServerProcess.command(startCommand);
                
                try {
                        this.process = Runtime.getRuntime().exec(this.startScript);
                        this.PROCCESS_IDS.add(this.process.pid());
                        System.out.println("Minecraft Server started with PID: " + this.process.pid());
                        this.isActive = true;
                        this.discordBot.sendMessage("Server successfully started");
                } catch (IOException e) {
                        System.err.println("Failed to start server process!!");
                        this.discordBot.sendMessage("Server failed to start!!");
                        this.isActive = false;
                        e.printStackTrace();
                }
        }

        // Check if the process running the server is alive, if so notify its up 
        public boolean isServerActive() {
                if(this.process == null) { return false; }
                return this.process.isAlive();
        }

        // This will passthrough a command to the server
        public void sendCommandToServer(String command) {
                // Check of coarse if the server is alive
                if(this.process == null || !this.process.isAlive()) {
                        this.discordBot.sendMessage("Uhhh, there is no server to send a command to...");
                        return;
                }

                this.mineServerInput.println(command); 
                this.discordBot.sendMessage("Command sent!");
        }

        // Stop the server, and make sure it only is being attempted to be stopped once at a time
        public void stopServer() {

                // Use the isStopping to make sure we don't try and close the program twice at once
                if(isStopping) { this.discordBot.sendMessage("I'm already stopping the server hold on, be patient"); return; }
                this.isStopping = true;

                // Check to see if server is already stopped, if not
                // Alert discord 
                if(this.process == null || !this.process.isAlive()) { 
                        this.discordBot.sendMessage("Server is already stopped, try starting it first."); 
                        this.discordBot.sendMessage("But just in case I will check again for you.");

                        try {
                                anakinMethod();
                        } catch (IOException e) {
                                System.err.println("Unable to kill all child processes");
                        }

                        this.isStopping = false;
                        this.isActive = false;
                        return; 
                }

                sendCommandToServer("stop");

                try {
                        // Wait for the program to stop, if it doesn't, force close it
                        boolean programStop = this.process.waitFor(5, TimeUnit.MINUTES);   

                        if (!programStop) { 
                                anakinMethod(); 
                                this.isStopping = false;
                                this.isActive = false;
                        } else { this.discordBot.sendMessage("Server stopped successfully!"); }

                } catch (InterruptedException e) {
                        this.discordBot.sendMessage("Uh oh, I can't stop the server...");
                        System.err.println("Failed to stop process!");
                        e.printStackTrace();

                } catch (IOException e) {
                        System.err.println("Failed to stop process, it may still be running");
                        e.printStackTrace();
                }
        }

        // Create the command for current platform
        private String createStartCommand() {
                // Create command for linux or windows
                String command;

                // Check first character making sure its a '/'
                if(this.startScript.charAt(0) != '/') {
                        command = "." + this.startScript;
                } else { command = "./" + this.startScript; }

                if(System.getProperty("os.name").equals("Windows 10")) {
                        command = "cmd " + command;
                }                
                
                return this.startScript;
                //return command;
        }

        // Kill all children processes
        public void anakinMethod() throws IOException {
                System.out.println("Force Kill Envoked");

                if(System.getProperty("os.name").equals("Windows 10")) {
                        for (long pid : PROCCESS_IDS) {
                                Runtime.getRuntime().exec("taskkill /f /pid " + pid); 
                        }

                        this.PROCCESS_IDS.clear();
                        return;
                }

                for (long pid : PROCCESS_IDS) {
                       Runtime.getRuntime().exec("kill -SIGKILL " + pid); 
                }
                
                this.PROCCESS_IDS.clear();
        }

        // TODO: restructure how thread is executed
        // Make it so that it can be started once and it will server as a listener always
        @Override
        public void run() {
                // Create output into the server terminal
                Scanner mineServerIN = null;
                Scanner mineServerERROR = null;
                boolean serverErrorOccur = false;

                // Create an input from the server than send each new line to the bot
                try {
                        this.mineServerInput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(this.process.getOutputStream())), true);
                        mineServerIN = new Scanner(this.process.getInputStream());
                        mineServerERROR = new Scanner(this.process.getErrorStream());
                } catch (java.lang.NullPointerException e) {
                        System.err.println("No server process available");
                }

                try {
                        // Print to discord while the server is active
                        do {

                                try {
                                        String serverMSG = mineServerIN.nextLine();
                                        if(!serverMSG.contains("Spigot Watchdog Thread/ERROR")) {
                                                this.discordBot.sendMessage("Minecraft Server: " + serverMSG);  
                                        } else if (!serverErrorOccur && serverMSG.contains("Spigot Watchdog Thread/ERROR")) {
                                                System.out.println("Minecraft Server had an error");
                                                serverErrorOccur = true;
                                        }
                                } catch (java.util.NoSuchElementException | java.lang.NullPointerException e) {
                                        System.err.println("No next line from server script can be found");
                                }

                        } while(this.process.isAlive());

                        mineServerIN.close();
                        mineServerERROR.close();
                        System.out.println("Process Died");

                } catch (java.lang.NullPointerException e) { System.err.println("There is no process"); }

                this.mineServerInput = null;
                System.out.println("Thread ended");
                this.discordBot.sendMessage("I think the process closed");
        }
}
