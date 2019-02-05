package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

public class Lifestyle {

    public String type;

    @SerializedName("brf")
    public String brief;

    @SerializedName("txt")
    public String suggestion;


}
