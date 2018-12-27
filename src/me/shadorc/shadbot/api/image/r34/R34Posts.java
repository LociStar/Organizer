package me.shadorc.shadbot.api.image.r34;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class R34Posts {

	@JsonProperty("post")
	private List<R34Post> posts;
	@JsonProperty("count")
	private int count;

	public List<R34Post> getPosts() {
		return this.posts;
	}

	public int getCount() {
		return this.count;
	}

}
