package org.inventivetalent.mcgameinfo;

import java.util.regex.Pattern;

public class GamePattern {

	private final Pattern pattern;
	private final String game;

	public GamePattern(Pattern pattern, String game) {
		this.pattern = pattern;
		this.game = game;
	}

	public GamePattern(String pattern, String game) {
		this.pattern = Pattern.compile(pattern);
		this.game = game;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public String getGame() {
		return game;
	}

	public boolean matches(String target) {
		return this.pattern.matcher(target).find();
	}
}
