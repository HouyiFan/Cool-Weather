package com.fandevcoolweather.android;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.fandevcoolweather.android.gson.Forecast;
import com.fandevcoolweather.android.gson.Lifestyle;
import com.fandevcoolweather.android.gson.Weather;
import com.fandevcoolweather.android.service.AutoUpdateService;
import com.fandevcoolweather.android.util.HttpUtil;
import com.fandevcoolweather.android.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity implements LocationListener {

    private static final String TAG = "WeatherActivity";

    public DrawerLayout drawerLayout;

    private Button navButton;

    public SwipeRefreshLayout swipeRefresh;

    private String mWeatherId;

    private ImageView bingPicImg;

    private ScrollView weatherLayout;

    private TextView titleCity;

    private TextView titleUpdateTime;

    private TextView degreeText;

    private TextView weatherInfoText;

    private LinearLayout forecastLayout;

    private TextView aqiText;

    private TextView pm25Text;

    private TextView comfortText;

    private TextView dressText;

    private TextView fluText;

    private TextView sportText;

    private TextView travelText;

    private TextView uvText;

    private TextView carWashText;

    private TextView airText;

    private LocationManager locationManager;

    private Button getCurrentLocationWeatherButton;

    private SharedPreferences prefs;

    private EditText userInputLocationText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        //初始化各控件
        bingPicImg = findViewById(R.id.bing_pic_img);
        weatherLayout = findViewById(R.id.weather_layout);
        titleCity = findViewById(R.id.title_city);
        titleUpdateTime = findViewById(R.id.title_update_time);
        degreeText = findViewById(R.id.degree_text);
        weatherInfoText = findViewById(R.id.weather_info_text);
        forecastLayout = findViewById(R.id.forecast_layout);
        aqiText = findViewById(R.id.aqi_text);
        pm25Text = findViewById(R.id.pm25_text);
        comfortText = findViewById(R.id.comfort_text);
        dressText = findViewById(R.id.dress_text);
        fluText = findViewById(R.id.flu_text);
        sportText = findViewById(R.id.sport_text);
        travelText = findViewById(R.id.travel_text);
        uvText = findViewById(R.id.ultraviolet_text);
        carWashText = findViewById(R.id.car_wash_text);
        airText = findViewById(R.id.air_text);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        drawerLayout = findViewById(R.id.drawer_layout);
        navButton = findViewById(R.id.nav_button);
        getCurrentLocationWeatherButton = findViewById(R.id.get_current_location_weather);
        userInputLocationText = findViewById(R.id.input_location_name);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        if (weatherString != null) {
            //有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            if (weather != null) {
                mWeatherId = weather.basic.weatherId;
                showWeatherInfo(weather);
                Log.d(TAG, "onCreate: " + mWeatherId + " have cache");
            } else {
                Toast.makeText(this, "Unexpected error happens", Toast.LENGTH_LONG).show();
            }
        } else {
            //无缓存时去服务器查询天气
            mWeatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
            Log.d(TAG, "onCreate: " + mWeatherId + " no cache");
        }
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
                Log.d(TAG, "onRefresh: " + mWeatherId);
            }
        });
        String bingPic = prefs.getString("bing_pic",  null);
        if (bingPic != null) {
            Glide.with(getApplicationContext()).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
        }

        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        getCurrentLocationWeatherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(WeatherActivity.this, Manifest.permission
                        .ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(WeatherActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                } else {
                    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 600000, 0, WeatherActivity.this);
                }
            }
        });

        userInputLocationText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent keyEvent) {
                if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN)
                        && (keyCode == KeyEvent.KEYCODE_ENTER)) {
//                    Toast.makeText(getApplicationContext(), userInputLocationText.getText().toString(), Toast.LENGTH_LONG).show();
                    String userInputLocation = userInputLocationText.getText().toString().trim();
                    requestWeather(userInputLocation);

                    return true;
                }
                return false;
            }
        });

    }

    /**
     * 根据天气id请求城市天气信息
     */
    public void requestWeather(final String weatherId) {
        String weatherUrl = "https://free-api.heweather.net/s6/weather?location=" + weatherId
                + "&key=" + BuildConfig.API_KEY;

//        Log.d(TAG, "requestWeather: " + weatherId);
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.body() == null) {
                    Toast.makeText(WeatherActivity.this, "Empty HTTP response", Toast.LENGTH_LONG).show();
                    return;
                }
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences.Editor editor = PreferenceManager.
                                    getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            mWeatherId = weather.basic.weatherId;
//                            Log.d(TAG, "run: " + mWeatherId);
                            showWeatherInfo(weather);
//                            Log.d(TAG, "run: After show " + mWeatherId);
                        } else if (weather != null && "unknown location".equals(weather.status)){
                            Toast.makeText(WeatherActivity.this, "Your location is unknown",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(WeatherActivity.this, "Failed to get weather information",
                                    Toast.LENGTH_SHORT).show();
                        }
                        userInputLocationText.setText("");
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "Failed to get weather information",
                                Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
        loadBingPic();
    }

    /**
     * 加载必应每日一图
     */
    private void loadBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.body() == null) {
                    Toast.makeText(WeatherActivity.this, "Empty HTTP response", Toast.LENGTH_LONG).show();
                    return;
                }
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.
                        getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(getApplicationContext()).load(bingPic).into(bingPicImg);
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 处理并展示Weather实体类中的数据
     */
    private void showWeatherInfo(Weather weather) {

        String cityName = weather.basic.cityName;
        String updateTime = weather.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for (Forecast forecast: weather.dailyForecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,
                    forecastLayout, false);
            TextView dateText = view.findViewById(R.id.date_text);
            TextView infoText = view.findViewById(R.id.info_text);
            TextView maxText = view.findViewById(R.id.max_text);
            TextView minText = view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.daytimeCond);
            maxText.setText(forecast.max);
            minText.setText(forecast.min);
            forecastLayout.addView(view);
        }

        requestAirCondition(weather.basic.parentCityName);

        if (weather.lifestyleList == null) {
            Toast.makeText(this, "Lifestyle suggestion only works for China", Toast.LENGTH_LONG).show();
            comfortText.setText("");
            dressText.setText("");
            fluText.setText("");
            sportText.setText("");
            travelText.setText("");
            uvText.setText("");
            carWashText.setText("");
            airText.setText("");
            return;
        }
        for (Lifestyle lifestyle: weather.lifestyleList) {
            switch (lifestyle.type) {
                case "comf":
                    String comfort = "舒适度: " + lifestyle.suggestion;
                    comfortText.setText(comfort);
                    break;
                case "drsg":
                    String dress = "穿衣指数: " + lifestyle.suggestion;
                    dressText.setText(dress);
                    break;
                case "flu":
                    String flu = "感冒指数: " + lifestyle.suggestion;
                    fluText.setText(flu);
                    break;
                case "sport":
                    String sport = "运动建议: " + lifestyle.suggestion;
                    sportText.setText(sport);
                    break;
                case "trav":
                    String travel = "旅游指数: " + lifestyle.suggestion;
                    travelText.setText(travel);
                    break;
                case "uv":
                    String uv = "紫外线指数: " + lifestyle.suggestion;
                    uvText.setText(uv);
                    break;
                case "cw":
                    String carWash = "洗车指数: " + lifestyle.suggestion;
                    carWashText.setText(carWash);
                    break;
                case "air":
                    String air = "空气指数: " + lifestyle.suggestion;
                    airText.setText(air);
                    break;
                default:
                    break;
            }
        }
        weatherLayout.setVisibility(View.VISIBLE);
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }

    /**
     * 请求天气质量状况（aqi和pm2.5指数)
     * Request the air condition, including aqi and pm 2.5
     */
    private void requestAirCondition(final String weatherId) {
        String airUrl = "https://free-api.heweather.net/s6/air/now?location=" + weatherId
                + "&key=" + BuildConfig.API_KEY;

        Log.d(TAG, "requestAirCondition: " + weatherId );
        Log.d(TAG, "requestAirCondition: " + airUrl);

        HttpUtil.sendOkHttpRequest(airUrl, new Callback() {

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.body() == null) {
                    Toast.makeText(WeatherActivity.this, "Empty HTTP response", Toast.LENGTH_LONG).show();
                    return;
                }
                final String responseText = response.body().string();
//                Log.d(TAG, "onResponse: " + responseText);
                final Weather air = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (air != null && "ok".equals(air.status)) {
//                            mWeatherId = air.basic.weatherId;
                            getAirCondition(air);
                        } else {
                            Toast.makeText(WeatherActivity.this, "Failed to get air condition\n"
                                    + "Possible reason: Your area is not covered",
                                    Toast.LENGTH_LONG).show();
                            aqiText.setText("");
                            pm25Text.setText("");
                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "Failed to get air condition. Unexpected Error happens.",
                                Toast.LENGTH_LONG).show();
                        aqiText.setText("");
                        pm25Text.setText("");
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });

    }

    /**
     * 得到天气质量状况（aqi和pm2.5指数)
     */
    private void getAirCondition(Weather air) {
//        Log.d(TAG, "getAirConidtion: " + air.airnowcity.quality);
        if (air.airnowcity != null) {
            aqiText.setText(air.airnowcity.aqi);
            pm25Text.setText(air.airnowcity.pm25);
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "location provider enabled");
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, "location provider disabled");
    }

    @Override
    public void onLocationChanged(Location location) {
        Toast.makeText(this, "Detect your location: " + location.getLongitude() + ", "
                + location.getLatitude(), Toast.LENGTH_LONG).show();
        String weatherId = location.getLongitude() + "," + location.getLatitude();
        requestWeather(weatherId);
        String bingPic = prefs.getString("bing_pic",  null);
        if (bingPic != null) {
            Glide.with(getApplicationContext()).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
        }
        Log.d(TAG, "location changed to : Longitude" + location.getLongitude()
                + ", Latitude: " + location.getLatitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(TAG, "status changed to " + status);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 600000, 0, this);
                } else {
                    Toast.makeText(this, "Cannot use this button without authorization", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }
}
