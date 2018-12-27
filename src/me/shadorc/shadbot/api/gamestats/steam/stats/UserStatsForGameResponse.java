package me.shadorc.shadbot.api.gamestats.steam.stats;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserStatsForGameResponse {

	@Nullable
	@JsonProperty("playerstats")
	private PlayerStats playerStats;

	public PlayerStats getPlayerStats() {
		return this.playerStats;
	}

}
