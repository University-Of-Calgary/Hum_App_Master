package com.example.orchisamadas.analyse_plot;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public class StartApp extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_app);
    }

    public void StartDSP(View v){
        Intent intent=new Intent(this,StartDSP.class);
        startActivity(intent);
    }

    /*Starts the activity DisplayGraph to view previous graphs
    We can either view previous FFT graphs or previous analysis histograms
    depending on which button is pressed */
    public void gotoGraphFFT(View v)
    {
        Bundle bundle = new Bundle();
        bundle.putString("button_pressed", "1");
        Intent intent = new Intent(this, DisplayGraph.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    public void gotoHistogram(View v)
    {
        Bundle bundle = new Bundle();
        bundle.putString("button_pressed", "2");
        Intent intent = new Intent(this, DisplayGraph.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_start_app, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item){
        //Handle presses on the action bar items
        /**
        if (item.getItemId() == R.id.recording_Threshold){
            Intent thresholdIntent = new Intent(StartApp.this, RecordingAudioThreshold.class);
            startActivity(thresholdIntent);
        }

        else if (item.getItemId() == R.id.recording_Value){
            Intent valueIntent = new Intent(StartApp.this, RecorderActivitySettings.class);
            startActivity(valueIntent);
        }*/

        if (item.getItemId() == R.id.calibrate_microphone){
            Intent calibrateIntent = new Intent(StartApp.this, CalibrateMicrophone.class);
            startActivity(calibrateIntent);
        }
        return super.onOptionsItemSelected(item);
    }
}