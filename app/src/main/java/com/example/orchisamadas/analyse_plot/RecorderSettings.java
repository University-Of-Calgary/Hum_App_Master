package com.example.orchisamadas.analyse_plot;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class RecorderSettings extends AppCompatActivity {

    private TextView tvStartTime, tvEndTime, tvOftenTime, tvDuration, tvThreshold, tvGPS, tvOriginal, tvCalibrated;

    private static final String PREFERENCES = "AudioRecordingPrefs";
    private static final String timeStartKey = "startKey";
    private static final String timeEndKey = "endKey";
    private static final String timeOftenKey = "oftenKey";
    private static final String timeRecordingKey = "recordingKey";
    private static final String thresholdNoiseKey = "thresholdKey";
    private static final String gpsValueKey = "gpsKey";
    private static final String originalStoreKey = "originalKey";
    private static final String calibratedValuesKey = "useCalibratedKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorder_settings);

        tvStartTime = (TextView) findViewById(R.id.recorder_tv_start);
        tvEndTime = (TextView) findViewById(R.id.recorder_tv_end);
        tvOftenTime = (TextView) findViewById(R.id.recorder_tv_often);
        tvDuration = (TextView) findViewById(R.id.recorder_tv_duration);
        tvThreshold = (TextView) findViewById(R.id.recorder_tv_threshold);
        tvGPS = (TextView) findViewById(R.id.recorder_tv_gps);
        tvOriginal = (TextView) findViewById(R.id.recorder_tv_original);
        tvCalibrated = (TextView) findViewById(R.id.recorder_tv_calibrated);

        SharedPreferences preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        int startTime = Integer.parseInt(preferences.getString(timeStartKey, "0"));
        int endTime = Integer.parseInt(preferences.getString(timeEndKey, "1"));
        int oftenTime = Integer.parseInt(preferences.getString(timeOftenKey, "10"));
        int duration = Integer.parseInt(preferences.getString(timeRecordingKey, "5"));
        int threshold = Integer.parseInt(preferences.getString(thresholdNoiseKey, "0"));
        String gpsValue = preferences.getString(gpsValueKey, "no");
        String originalValue = preferences.getString(originalStoreKey, "yes");
        String calibratedValue = preferences.getString(calibratedValuesKey, "no");

        tvStartTime.setText("Start Time : " + startTime + " minutes");
        tvEndTime.setText("End Time : " + endTime + " minutes");
        tvOftenTime.setText("Often Time : " + oftenTime + " seconds");
        tvDuration.setText("Duration : " + duration + " seconds");
        tvThreshold.setText("Threshold : " + threshold + " units");
        tvGPS.setText("GPS Turned On : " + gpsValue);
        tvOriginal.setText("Store Original Sound : " + originalValue);
        tvCalibrated.setText("Calibration on : " + calibratedValue);
    }
}
