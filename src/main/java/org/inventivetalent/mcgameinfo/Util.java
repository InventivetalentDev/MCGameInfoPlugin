package org.inventivetalent.mcgameinfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

public class Util {

	public static void ping(GameInfoClient client, Logger logger) {
		final long start = System.currentTimeMillis();
		client.ping(data -> {
			if (!"ok".equals(data.get("status"))) {
				logger.warning("Got non-OK ping response from the API");
				logger.warning(data.toJSONString());
			} else {
				long localPing = System.currentTimeMillis() - start;
				long serverPing = System.currentTimeMillis() - (long) data.get("ts");
				logger.info("API Ping: " + localPing + "ms/" + serverPing + "ms");
			}
		});
	}

	public static void loadGamePatterns(List<Map> source, Set<GamePattern> gamePatterns, Logger logger) {
		gamePatterns.clear();
		for (Map map : source) {
			if (!map.containsKey("pattern")) {
				logger.warning("Missing 'pattern' key");
				continue;
			}
			if (!map.containsKey("game")) {
				logger.warning("Missing 'game' key");
				continue;
			}

			GamePattern pattern = new GamePattern((String) map.get("pattern"), (String) map.get("game"));
			gamePatterns.add(pattern);
			logger.info(pattern.getPattern().pattern() + " -> " + pattern.getGame());
		}
	}

	public static byte[] gzip(byte[] data) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gzos = null;

		try {
			gzos = new GZIPOutputStream(baos);
			gzos.write(data);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (gzos != null) {
				try {
					gzos.close();
				} catch (IOException ignore) {
				}
			}
		}

		return baos.toByteArray();
	}

}
