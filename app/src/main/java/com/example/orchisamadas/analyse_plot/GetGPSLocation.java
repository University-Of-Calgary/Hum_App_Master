package com.example.orchisamadas.analyse_plot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class GetGPSLocation extends AppCompatActivity {

    private LocationManager locationManager;
    private LocationListener locationListener;
    private final int GPSPermissionCode = 10;
    private TextView tvLocation;
    private Button getLocation;
    Location myLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_gpslocation);

        initializeLocationListener();
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0, locationListener);
        getGPSLocation();

        tvLocation = (TextView) findViewById(R.id.gps_coordinates);
        getLocation = (Button) findViewById(R.id.gps_getlocation);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    public void initializeLocationListener(){
        locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                tvLocation.setText("");
                tvLocation.append("Location, Latitude : " + location.getLatitude() + "," +
                        " Longitude : " + location.getLongitude());
                location.setLatitude(location.getLatitude());
                location.setLongitude(location.getLongitude());
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent gpsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(gpsIntent);
            }
        };
    }

    public void getGPSLocation(){
        System.out.println("Latitude : " + myLocation.getLatitude());
        System.out.println("Longitude : " + myLocation.getLongitude());
    }
}
