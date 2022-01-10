package com.locibot.webapi.weatherData.ptv;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record PTVResult(@JsonProperty("referencePosition") Map<String, Double> referencePosition) {
}
