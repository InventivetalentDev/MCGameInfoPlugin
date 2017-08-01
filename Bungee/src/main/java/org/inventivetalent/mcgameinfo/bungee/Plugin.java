package org.inventivetalent.mcgameinfo.bungee;

import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import org.bstats.bungeecord.MetricsLite;
import org.inventivetalent.mcgameinfo.GameInfoClient;
import org.inventivetalent.mcgameinfo.GamePattern;
import org.inventivetalent.mcgameinfo.Util;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Plugin extends net.md_5.bungee.api.plugin.Plugin implements Listener {

	private GameInfoClient client;

	private Configuration config;

	private String serverId;
	private String serverToken;

	private boolean          enableGameDetector = false;
	private Set<GamePattern> gamePatterns       = new HashSet<>();
	private GamePattern resetPattern;

	@Override
	public void onEnable() {
		saveDefaultConfig();
		try {
			config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (!config.getBoolean("apiOnly")) {
			ProxyServer.getInstance().getPluginManager().registerListener(this, this);
		}

		this.serverId = config.getString("server.id");
		this.serverToken = config.getString("server.token");

		loadConfig();

		this.client = new GameInfoClient(getLogger(), runnable -> ProxyServer.getInstance().getScheduler().runAsync(Plugin.this, runnable), this.serverId, this.serverToken, "GameInfoPlugin-Bungee/" + getDescription().getVersion(), "bungeecord");
		if (config.getBoolean("debug")) { this.client.enableDebug(); }
		ProxyServer.getInstance().getScheduler().schedule(this, () -> Util.ping(client, getLogger()), 2, TimeUnit.SECONDS);

		new MetricsLite(this);
	}

	void loadConfig() {
		this.enableGameDetector = config.getBoolean("gameDetector.enabled");
		if (this.enableGameDetector) {
			Util.loadGamePatterns((List<Map>) config.getList("gameDetector.patterns"), gamePatterns, getLogger());
			resetPattern = new GamePattern(config.getString("gameDetector.resetPattern"), ":reset");
		}
	}

	void saveDefaultConfig() {
		if (!getDataFolder().exists()) {
			getDataFolder().mkdir();
		}
		File configFile = new File(getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			try {
				configFile.createNewFile();
				try (InputStream is = getResourceAsStream("config.yml");
						OutputStream os = new FileOutputStream(configFile)) {
					ByteStreams.copy(is, os);
				}
			} catch (IOException e) {
				throw new RuntimeException("Unable to create configuration file", e);
			}
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
	public void on(PostLoginEvent event) {
		if (config.getBoolean("apiOnly")) { return; }
		getClient().joinServer(event.getPlayer().getName(), event.getPlayer().getUniqueId());
	}

	@EventHandler
	public void on(PlayerDisconnectEvent event) {
		if (config.getBoolean("apiOnly")) { return; }
		//		getClient().leaveGame(event.getPlayer().getName(), event.getPlayer().getUniqueId());
		getClient().leaveServer(event.getPlayer().getName(), event.getPlayer().getUniqueId());
	}

	@EventHandler
	public void on(ServerSwitchEvent event) {
		if (config.getBoolean("apiOnly") || !enableGameDetector) { return; }
		if (resetPattern.matches(event.getPlayer().getServer().getInfo().getName())) {
			getClient().leaveGame(event.getPlayer().getName(), event.getPlayer().getUniqueId());
		} else {
			gamePatterns.stream().filter(pattern ->
					pattern.matches(event.getPlayer().getServer().getInfo().getName())).forEach(pattern ->
					getClient().joinGame(event.getPlayer().getName(), event.getPlayer().getUniqueId(), pattern.getGame()));
		}
	}

}
