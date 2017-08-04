package org.inventivetalent.mcgameinfo;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameInfoClient {

	private final Logger           logger;
	private final ScheduleCallback scheduleCallback;
	private final String           serverId;
	private final String           serverToken;
	private final String           userAgent;
	private final String           serverType;

	/**
	 * Set after the first 'ping' request. <code>true</code>, if the ping was successful and the API is online
	 **/
	private boolean ready = false;

	private boolean debug = false;

	public GameInfoClient(Logger logger, ScheduleCallback callback, String serverId, String serverToken, String userAgent, String serverType) {
		this.logger = logger;
		this.scheduleCallback = callback;
		this.serverId = serverId;
		this.serverToken = serverToken;
		this.userAgent = userAgent;
		this.serverType = serverType;
	}

	public void enableDebug() {
		this.debug = true;
	}

	/**
	 * Ping the API
	 *
	 * @param callback the ping result
	 */
	public void ping(RequestCallback callback) {
		request("POST", "/ping/server", new JSONObject(), data -> {
			GameInfoClient.this.ready = "ok".equals(data.get("status"));
			callback.call(data);
		});
	}

	/**
	 * @return <code>true</code>, if the client was able to ping the API and is ready to make further requests
	 */
	public boolean isReady() {
		return ready;
	}

	public boolean joinServer(String username, UUID uuid) {
		if (!this.ready) { return false; }
		JSONObject json = new JSONObject();
		json.put("username", username);
		json.put("uuid", uuid.toString());
		request("POST", "/join/server", json);
		return true;
	}

	public boolean leaveServer(String username, UUID uuid) {
		if (!this.ready) { return false; }
		JSONObject json = new JSONObject();
		json.put("username", username);
		json.put("uuid", uuid.toString());
		request("POST", "/leave/server", json);
		return true;
	}

	public boolean joinGame(String username, UUID uuid, String gameName) {
		if (!this.ready) { return false; }
		JSONObject json = new JSONObject();
		json.put("username", username);
		json.put("uuid", uuid.toString());
		json.put("gameName", gameName);
		request("POST", "/join/game", json);
		return true;
	}

	public boolean leaveGame(String username, UUID uuid) {
		if (!this.ready) { return false; }
		JSONObject json = new JSONObject();
		json.put("username", username);
		json.put("uuid", uuid.toString());
		request("POST", "/leave/game", json);
		return true;
	}

	void request(String method, String path, JSONObject json) {
		request(method, path, json, null);
	}

	void request(String method, String path, JSONObject json, RequestCallback callback) {
		scheduleCallback.schedule(() -> {
			try {
				URL url = new URL("https://api.mcgame.info" + path);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod(method);

				connection.setDoOutput(true);
				connection.setDoInput(true);

				connection.setRequestProperty("Server-Id", serverId);
				connection.setRequestProperty("Server-Token", serverToken);
				connection.setRequestProperty("Server-Type", serverType);
				connection.setRequestProperty("User-Agent", userAgent);

				byte[] data = json.toString().getBytes("UTF-8");
				byte[] compressedData = Util.gzip(data);

				connection.setRequestProperty("Content-Type", "application/json");
				connection.setRequestProperty("Content-Encoding", "gzip");
				connection.setRequestProperty("Content-Length", Integer.toString(compressedData.length));

				try (OutputStream out = connection.getOutputStream()) {
					out.write(compressedData);
					out.flush();
				}

				if (debug) {
					System.out.println(connection.getResponseCode());
				}
				try (InputStream stream = (connection.getResponseCode() == 200 ? connection.getInputStream() : connection.getErrorStream())) {
					if (debug || callback != null) {
						try (BufferedReader in = new BufferedReader(new InputStreamReader(stream))) {
							String result = "";
							String line;
							while ((line = in.readLine()) != null) {
								result += line;
							}
							if (debug) { System.out.println(result); }
							if (callback != null) { callback.call((JSONObject) new JSONParser().parse(result)); }
						}
					}
				}
				connection.disconnect();
			} catch (Exception e) {
				logger.log(Level.WARNING, "Exception while making request to " + path, e);
				if (callback != null) { callback.call(new JSONObject()); }
			}
		});
	}

}
