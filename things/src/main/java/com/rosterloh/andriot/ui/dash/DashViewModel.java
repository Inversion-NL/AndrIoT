package com.rosterloh.andriot.ui.dash;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import com.rosterloh.andriot.db.WeatherRepository;
import com.rosterloh.andriot.sensors.SensorHub;
import com.rosterloh.andriot.vo.Sensors;
import com.rosterloh.andriot.db.Weather;
import com.rosterloh.andriot.AppExecutors;
import com.rosterloh.andriot.util.NetworkUtils;

import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

public class DashViewModel extends ViewModel {

    private static final int POLL_RATE = 5 * 60 * 1000;
    private static final int INIT_DELAY = 5 * 1000;

    private final AppExecutors appExecutors;
    private final SensorHub sensorHub;

    private final MutableLiveData<Sensors> sensors = new MutableLiveData<>();
    private final LiveData<Weather> weather;

    @Inject
    DashViewModel(WeatherRepository weatherRepository, AppExecutors appExecutors, SensorHub sensorHub) {
        this.appExecutors = appExecutors;
        this.sensorHub = sensorHub;
        weather = weatherRepository.loadWeather();

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                appExecutors.diskIO().execute(() -> {
                    float[] data = sensorHub.getSensorData();
                    if(data != null) {
                        appExecutors.mainThread().execute(() -> {
                            sensors.setValue(new Sensors(data[0], data[1], null,
                                    NetworkUtils.getIPAddress(true), null));
                        });
                    }
                });
            }
        }, INIT_DELAY, POLL_RATE);
    }

    LiveData<Boolean> getMotion() {
        return sensorHub.pirData;
    }

    LiveData<Sensors> getSensorData() {
        return sensors;
    }

    LiveData<Weather> getWeather() {
        return weather;
    }
}
