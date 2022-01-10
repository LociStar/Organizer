package com.locibot.webapi.weatherData.ptv;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PTVResponse(@JsonProperty("locations") List<PTVResult> result) {
}
