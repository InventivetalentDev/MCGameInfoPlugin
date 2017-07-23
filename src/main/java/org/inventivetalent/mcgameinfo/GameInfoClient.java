package org.inventivetalent.mcgameinfo;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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

	private boolean debug = false;

	public GameInfoClient(Logger logger, ScheduleCallback callback, String serverId, String serverToken, String userAgent) {
		this.logger = logger;
		this.scheduleCallback = callback;
		this.serverId = serverId;
		this.serverToken = serverToken;
		this.userAgent = userAgent;
	}

	public void enableDebug() {
		this.debug = true;
	}

	public void ping(RequestCallback callback) {
		request("POST", "/servers/ping", new JSONObject(), callback);
	}

	public void joinServer(String username, UUID uuid) {
		JSONObject json = new JSONObject();
		json.put("username", username);
		json.put("uuid", uuid.toString());
		request("POST", "/join/server", json);
	}

	public void leaveServer(String username, UUID uuid) {
		JSONObject json = new JSONObject();
		json.put("username", username);
		json.put("uuid", uuid.toString());
		request("POST", "/leave/server", json);
	}

	public void joinGame(String username, UUID uuid, String gameName) {
		JSONObject json = new JSONObject();
		json.put("username", username);
		json.put("uuid", uuid.toString());
		json.put("gameName", gameName);
		request("POST", "/join/game", json);
	}

	public void leaveGame(String username, UUID uuid) {
		JSONObject json = new JSONObject();
		json.put("username", username);
		json.put("uuid", uuid.toString());
		request("POST", "/leave/game", json);
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
				connection.setRequestProperty("Content-Type", "application/json");
				connection.setRequestProperty("User-Agent", userAgent);

				connection.setRequestProperty("Server-Id", serverId);
				connection.setRequestProperty("Server-Token", serverToken);

				connection.setDoOutput(true);
				connection.setDoInput(true);

				OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
				out.write(json.toString());
				out.flush();

				if (debug) {
					System.out.println(connection.getResponseCode());
				}
				InputStream stream;
				if (connection.getResponseCode() == 200) {
					stream = connection.getInputStream();
				} else {
					stream = connection.getErrorStream();
				}
				if (debug || callback != null) {
					BufferedReader in = new BufferedReader(new InputStreamReader(stream));
					String result = "";
					String line;
					while ((line = in.readLine()) != null) {
						result += line;
					}
					if (debug) { System.out.println(result); }
					if (callback != null) { callback.call((JSONObject) new JSONParser().parse(result)); }
				}
			} catch (Exception e) {
				logger.log(Level.WARNING, "Exception while making request to " + path, e);
			}
		});
	}

}
