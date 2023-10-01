# Discord Minecraft Server Manager

Setup: 
1. Put downloaded jar inside same folder as server jar
2. Run the DiscordManagerJar, it will create a conf folder
3. Open the config file inside and edit the feilds
4. Set the discord bot token
5. Set guild ID for discord
6. set channel ID for discord
7. Set command used to run minecraft server
8. Run DiscordManagerJar again this time you should see an "I'm awake now" from the bot set up

Usage:
1. Go to discord and you will have "/minecraft" as an usable command
2. When you type "/minecraft" you will be given an manger and command option, make sure to use the options, and only one at at time
3. The manager option will allow you to start, stop, or check an minecraft server
4. The command option will passthrough anything you type to the server terminal for you, this is to be able to use minecraft server commands from discord
5. You cannot start multiple servers at once, you can start a new one once the current on is stopped   

Example of config - 

DiscordBot_token: Discord-.Bot-Token-Pasted_In
Discord_Guild_ID: 11738476463788374
Discord_TextChannel_ID: 11234838374633
Minecraft_Start_Script: java -Xmx8192M -Xms4096M -jar server.jar nogui
