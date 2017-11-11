package net.querz.openshulkerbox;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class OpenShulkerBoxPlugin extends JavaPlugin {
	private static OpenShulkerBoxPlugin instance;
	private boolean openWhileSneaking;

	public void onEnable() {
		instance = this;
		Bukkit.getPluginManager().registerEvents(new OpenShulkerBoxListener(), this);
		loadConfig();
	}

	private void loadConfig() {
		saveDefaultConfig();
		openWhileSneaking = getConfig().getBoolean("open-while-sneaking", false);
	}

	public static OpenShulkerBoxPlugin getInstance() {
		return instance;
	}

	public boolean openWhileSneaking() {
		return openWhileSneaking;
	}
}

