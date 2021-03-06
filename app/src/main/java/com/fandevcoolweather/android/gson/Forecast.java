package com.fandevcoolweather.android.gson;

import com.google.gson.annotations.SerializedName;

public class Forecast {

    public String date;

    @SerializedName("tmp_max")
    public String max;

    @SerializedName("tmp_min")
    public String min;

    @SerializedName("cond_txt_d")
    public String daytimeCond;

    @SerializedName("cond_txt_n")
    public String nightCond;

}
