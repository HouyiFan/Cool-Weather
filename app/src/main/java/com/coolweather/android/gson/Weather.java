package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Weather {

    public String status;

    public Basic basic;

    public Update update;

    public Air_now_city airnowcity;

    public Now now;

    @SerializedName("lifestyle")
    public List<Lifestyle> lifestyleList;

    @SerializedName("daily_forecast")
    public List<Forecast> dailyForecastList;

    @SerializedName("hourly")
    public List<Hourly> hourlyForecastList;

}
