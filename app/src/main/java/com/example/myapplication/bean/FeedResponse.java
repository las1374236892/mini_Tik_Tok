package com.example.myapplication.bean;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * @author Xavier.S
 * @date 2019.01.20 14:17
 */
public class FeedResponse {

    // TODO-C2 (2) Implement your FeedResponse Bean here according to the response json
    @SerializedName("feeds")
    List<Feed> feeds;
    @SerializedName("success")
    boolean success;

    @Override
    public String toString() {
        return "feeds"+feeds+", success"+success;
    }

    public List<Feed> getFeeds() {
        return feeds;
    }
}
