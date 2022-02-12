package com.locibot.locibot.utils.weather.ptv;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PTVAddress(@JsonProperty("city") String city) {
}
