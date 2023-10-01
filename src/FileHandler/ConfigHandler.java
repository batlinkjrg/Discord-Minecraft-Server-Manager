package FileHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.simpleyaml.configuration.file.YamlFile;

public class ConfigHandler {
        
        private CONFIG_INFO CONFIG = new CONFIG_INFO();

        public ConfigHandler() {
                // Initialize the config file object
                File configFile = new File("conf//config.yml");

                // Check if it exists, if not create a new one
                if (!configFile.exists()) {
                        try {
                                Files.createDirectories(configFile.toPath().getParent());
                                Files.createFile(configFile.toPath());
                        } catch (IOException e) {
                                System.err.println("Error: Failed to create config file!!");
                                e.printStackTrace();
                        }
                }

                // Initialize yaml
                final YamlFile yamlFile = new YamlFile(configFile);

                try {
                        yamlFile.loadWithComments();
                } catch (IOException e) {
                        // TODO Auto-generated catch block
                        System.err.println("Failed to load config file!!");
                        e.printStackTrace();
                }

                // Check and read the yaml file
                checkDefualts(yamlFile);
                readFile(yamlFile);
        }

        private void checkDefualts(YamlFile yamlFile)  {
                // Check and set the header
                String prefix_suffix = "###############################################";
                yamlFile.options().headerFormatter().prefixFirst(prefix_suffix).commentPrefix("##  ").commentSuffix("  ##").suffixLast(prefix_suffix);
                yamlFile.setHeader("Discord Minecraft Manager Configuration");

                // Check and/or add defualt values
                yamlFile.options().copyDefaults();
                yamlFile.addDefault("DiscordBot_token", "<Bot Token>");
                yamlFile.addDefault("Discord_Guild_ID", "<Name of DiscordServer>");
                yamlFile.addDefault("Discord_TextChannel_ID", "<Discord TextChannel Name>");
                yamlFile.addDefault("Minecraft_Start_Script", "<Script Path>");

                // Save any changes
                try {
                        yamlFile.save();
                } catch (IOException e) {
                        System.err.println("ERROR: Failed to save ymal file!");
                        e.printStackTrace();
                }
        }

        private void readFile(YamlFile yamlFile) {
                // Load values into ram from config
                CONFIG.DiscordBot_token = (String) yamlFile.get("DiscordBot_token");
                CONFIG.Discord_Guild_ID = (Long) yamlFile.get("Discord_Guild_ID");
                CONFIG.Discord_TextChannel_ID = (Long) yamlFile.get("Discord_TextChannel_ID");
                CONFIG.Minecraft_Start_Script = (String) yamlFile.get("Minecraft_Start_Script");
        }

        // Get the config info
        public CONFIG_INFO getConfigInfo() {
                return this.CONFIG;
        }

        // This will be a container for information from the config file
        public class CONFIG_INFO {
                public String DiscordBot_token;
                public Long Discord_Guild_ID;
                public Long Discord_TextChannel_ID;
                public String Minecraft_Start_Script;
        }

}
