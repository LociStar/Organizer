package com.locibot.locibot.api.json.gamestats.overwatch.stats;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Stats(@JsonProperty("top_heroes") TopHeroes topHeroes) {

}
