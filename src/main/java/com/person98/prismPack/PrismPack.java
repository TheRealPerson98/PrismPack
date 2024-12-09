package com.person98.prismPack;

import co.aikar.commands.PaperCommandManager;
import com.person98.prismPack.commands.BackPackCommand;
import com.person98.prismPack.manager.BackpackManager;
import com.person98.prismPack.manager.ConfigManager;
import com.person98.prismPack.manager.Database;
import com.person98.prismPack.manager.event.BackpackDeathHandler;
import com.person98.prismPack.util.Lang;
import com.person98.prismPack.util.PLogger;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class PrismPack extends JavaPlugin {

    @Getter
    private static PrismPack instance;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        PLogger.setup(this.getLogger(), "PrismPack" , PLogger.ConsoleColor.CYAN);

        ConfigManager.initialize();

        Lang.setup();

        Database.initialize();
        BackpackManager.initialize();

        PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new BackPackCommand());

        new BackpackDeathHandler();

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            BackpackManager.cleanupCache();
        }, 20 * 60 * 5, 20 * 60 * 5); // Run every 5 minutes

    }

    @Override
    public void onDisable() {
        Database.close();
    }

    public void reload() {
        reloadConfig();
        ConfigManager.reload();
        Lang.reloadLang();
    }
}
