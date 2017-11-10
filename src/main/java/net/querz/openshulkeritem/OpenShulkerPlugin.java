package net.querz.openshulkeritem;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class OpenShulkerPlugin extends JavaPlugin {
	private static OpenShulkerPlugin instance;
	private boolean openWhileSneaking;

	public void onEnable() {
		instance = this;
		Bukkit.getPluginManager().registerEvents(new OpenShulkerListener(), this);
		loadConfig();
	}

	private void loadConfig() {
		saveDefaultConfig();
		openWhileSneaking = getConfig().getBoolean("open-while-sneaking", false);
	}

	public static OpenShulkerPlugin getInstance() {
		return instance;
	}

	public boolean openWhileSneaking() {
		return openWhileSneaking;
	}
}

