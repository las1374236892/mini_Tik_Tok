package com.example.myapplication.bean;

/**
 * @author Xavier.S
 * @date 2019.01.18 17:53
 */
public class PostVideoResponse {

    // TODO-C2 (3) Implement your PostVideoResponse Bean here according to the response json
    // response
    // {
    //    "result": {},
    //    "url": "https://lf1-hscdn-tos.pstatp
    //    .com/obj/developer-baas/baas/tt7217xbo2wz3cem41/a8efa55c5c22de69_1560563154288.mp4",
    //    "success": true
    //}

    private Object result;
    private String url;
    private boolean success;

    public Object getResult() {
        return result;
    }

    public String getUrl() {
        return url;
    }

    public boolean isSuccess() {
        return success;
    }

}
