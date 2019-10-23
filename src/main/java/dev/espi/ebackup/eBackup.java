package dev.espi.ebackup;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
   Copyright 2019 EstiNet

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */

public class eBackup extends JavaPlugin implements CommandExecutor {

    // config options

    String crontask, backupFormat;
    File backupPath;
    int maxBackups;

    String ftpType, ftpHost, ftpUser, ftpPass, ftpPath;
    int ftpPort;
    boolean ftpEnable;

    String filePath;
    boolean fileEnable;

    boolean backupPluginJars, backupPluginConfs;
    List<String> filesToIgnore;
    List<File> ignoredFiles = new ArrayList<>();

    @Override
    public void onEnable() {
        getLogger().info("Initializing eBackup...");

        Metrics metrics = new Metrics(this);

        saveDefaultConfig();
        getConfig().options().copyDefaults(true);

        if (!getDataFolder().exists()) getDataFolder().mkdir();

        try {
            getConfig().load(getDataFolder() + "/config.yml");
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        // load config data
        crontask = getConfig().getString("crontask");
        backupFormat = getConfig().getString("backup-format");
        backupPath = new File(getConfig().getString("backup-path"));
        maxBackups = getConfig().getInt("max-backups");
        ftpEnable = getConfig().getBoolean("ftp.enable");
        ftpType = getConfig().getString("ftp.type");
        ftpHost = getConfig().getString("ftp.host");
        ftpPort = getConfig().getInt("ftp.port");
        ftpUser = getConfig().getString("ftp.user");
        ftpPass = getConfig().getString("ftp.pass");
        ftpPath = getConfig().getString("ftp.path");
        fileEnable = getConfig().getBoolean("file.enable");
        filePath =  = getConfig().getString("file.path");
        backupPluginJars = getConfig().getBoolean("backup.pluginjars");
        backupPluginConfs = getConfig().getBoolean("backup.pluginconfs");
        filesToIgnore = getConfig().getStringList("backup.ignore");
        for (String s : filesToIgnore) {
            ignoredFiles.add(new File(s));
        }
        this.getCommand("ebackup").setExecutor(this);

        if (!backupPath.exists()) backupPath.mkdir(); // make sure backup location exists

        // start cron task
        Cron.checkCron();
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (Cron.run()) {
                BackupUtil.doBackup(true);
            }
        }, 20, 20);

        getLogger().info("Plugin initialized!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabled eBackup!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.AQUA + "Do /ebackup help for help!");
            return true;
        }

        switch (args[0]) {
            case "help":
                sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "=====" + ChatColor.RESET + ChatColor.DARK_AQUA + " eBackup v" + getPlugin().getDescription().getVersion() + " Help " + ChatColor.RESET + ChatColor.GRAY + ChatColor.STRIKETHROUGH + "=====");
                sender.sendMessage(ChatColor.AQUA + "> " + ChatColor.GRAY + "/ebackup backup - Starts a backup of the server.");
                sender.sendMessage(ChatColor.AQUA + "> " + ChatColor.GRAY + "/ebackup backuplocal - Starts a backup of the server, but does not upload to FTP/SFTP.");
                sender.sendMessage(ChatColor.AQUA + "> " + ChatColor.GRAY + "/ebackup uploadfile - Starts a upload of the file.");
                sender.sendMessage(ChatColor.AQUA + "> " + ChatColor.GRAY + "/ebackup list - Lists the backups in the folder.");
                sender.sendMessage(ChatColor.AQUA + "> " + ChatColor.GRAY + "/ebackup stats - Shows disk space.");
                break;
            case "backup":
                sender.sendMessage("Starting backup...");
                Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
                    BackupUtil.doBackup(true);
                    sender.sendMessage("Finished!");
                });
                break;
            case "backuplocal":
                sender.sendMessage("Starting backup...");
                Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
                    BackupUtil.doBackup(false);
                    sender.sendMessage("Finished!");
                });
                break;
            case "uploadfile":
                sender.sendMessage("Starting file upload...");
                Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
                    BackupUtil.doFileUpload();
                    sender.sendMessage("Finished!");
                });
                break;
            case "list":
                sender.sendMessage(ChatColor.AQUA + "Local Backups:");
                for (File f : getPlugin().backupPath.listFiles()) {
                    sender.sendMessage(ChatColor.GRAY + "- " + f.getName());
                }
                break;
            case "stats":
                sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "=====" + ChatColor.RESET + ChatColor.DARK_AQUA + " Disk Stats " + ChatColor.RESET + ChatColor.GRAY + ChatColor.STRIKETHROUGH + "=====");
                sender.sendMessage(ChatColor.AQUA + "Total size: " + ChatColor.GRAY + (getPlugin().backupPath.getTotalSpace()/1024/1024/1024) + "GB");
                sender.sendMessage(ChatColor.AQUA + "Space usable: " + ChatColor.GRAY + (getPlugin().backupPath.getUsableSpace()/1024/1024/1024) + "GB");
                sender.sendMessage(ChatColor.AQUA + "Space free: " + ChatColor.GRAY + (getPlugin().backupPath.getFreeSpace()/1024/1024/1024) + "GB");
                break;
            default:
                sender.sendMessage(ChatColor.AQUA + "Do /ebackup help for help!");
                break;
        }
        return true;
    }

    public static eBackup getPlugin() {
        return (eBackup) Bukkit.getPluginManager().getPlugin("eBackup");
    }

}
