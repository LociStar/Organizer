package com.locibot.locibot.utils.weather.ptv;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PTVResponse(@JsonProperty("locations") List<PTVResult> result) {
}
