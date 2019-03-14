package com.fandevcoolweather.android.gson;

import com.google.gson.annotations.SerializedName;

public class Hourly {

    @SerializedName("cond_txt")
    public String info;

    @SerializedName("tmp")
    public String temperature;

    @SerializedName("wind_deg")
    public String windDegree;

}
