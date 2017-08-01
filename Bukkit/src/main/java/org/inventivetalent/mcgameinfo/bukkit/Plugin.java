package org.inventivetalent.mcgameinfo.bukkit;

import org.bstats.bukkit.MetricsLite;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.inventivetalent.mcgameinfo.GameInfoClient;
import org.inventivetalent.mcgameinfo.GamePattern;
import org.inventivetalent.mcgameinfo.Util;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Plugin extends JavaPlugin implements Listener {

	private GameInfoClient client;

	private String serverId;
	private String serverToken;

	private boolean          enableGameDetector = false;
	private Set<GamePattern> gamePatterns       = new HashSet<>();
	private GamePattern resetPattern;

	@Override
	public void onEnable() {
		saveDefaultConfig();

		if (!getConfig().getBoolean("apiOnly")) {
			Bukkit.getPluginManager().registerEvents(this, this);
		}

		this.serverId = getConfig().getString("server.id");
		this.serverToken = getConfig().getString("server.token");

		loadConfig();

		this.client = new GameInfoClient(getLogger(), runnable -> Bukkit.getScheduler().runTaskAsynchronously(Plugin.this, runnable), this.serverId, this.serverToken, "GameInfoPlugin-Bukkit/" + getDescription().getVersion(), "bukkit");
		if (getConfig().getBoolean("debug")) { this.client.enableDebug(); }
		Bukkit.getScheduler().runTaskLater(this, () -> Util.ping(client, getLogger()), 20 * 2);

		new MetricsLite(this);
	}

	void loadConfig() {
		reloadConfig();

		this.enableGameDetector = getConfig().getBoolean("gameDetector.enabled");
		if (this.enableGameDetector) {
			Util.loadGamePatterns((List<Map>) getConfig().getList("gameDetector.patterns"), gamePatterns, getLogger());
			resetPattern = new GamePattern(getConfig().getString("gameDetector.resetPattern"), ":reset");
		}
	}

	public GameInfoClient getClient() {
		return client;
	}

	@Override
	public void onDisable() {
		super.onDisable();
	}

	@EventHandler
	public void on(PlayerJoinEvent event) {
		if (getConfig().getBoolean("apiOnly")) { return; }
		getClient().joinServer(event.getPlayer().getName(), event.getPlayer().getUniqueId());
	}

	@EventHandler
	public void on(PlayerQuitEvent event) {
		if (getConfig().getBoolean("apiOnly") || getConfig().getBoolean("bungeecord")) { return; }
		//		getClient().leaveGame(event.getPlayer().getName(), event.getPlayer().getUniqueId());
		getClient().leaveServer(event.getPlayer().getName(), event.getPlayer().getUniqueId());
	}

	@EventHandler
	public void on(PlayerKickEvent event) {
		if (getConfig().getBoolean("apiOnly") || getConfig().getBoolean("bungeecord")) { return; }
		//		getClient().leaveGame(event.getPlayer().getName(), event.getPlayer().getUniqueId());
		getClient().leaveServer(event.getPlayer().getName(), event.getPlayer().getUniqueId());
	}

	@EventHandler
	public void on(PlayerChangedWorldEvent event) {
		if (getConfig().getBoolean("apiOnly") || !enableGameDetector) { return; }
		if (resetPattern.matches(event.getPlayer().getWorld().getName())) {
			getClient().leaveGame(event.getPlayer().getName(), event.getPlayer().getUniqueId());
		} else {
			gamePatterns.stream().filter(pattern ->
					pattern.matches(event.getPlayer().getWorld().getName())).forEach(pattern ->
					getClient().joinGame(event.getPlayer().getName(), event.getPlayer().getUniqueId(), pattern.getGame()));
		}
	}

}
