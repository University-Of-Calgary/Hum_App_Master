package com.example.orchisamadas.analyse_plot;

import android.app.ProgressDialog;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

public class RecorderActivitySettings extends AppCompatActivity {

    final double REFERENCE_PRESSURE = 0.00002;
    final double MAX_REFERENCE_PRESSURE = 0.6325;
    final double REFERENCE_ESTIMATE = Math.pow(2, 16) / MAX_REFERENCE_PRESSURE;
    final int recordingTimeCalibration = 5; // In s
    int samplingRate=44100, channelConfig = AudioFormat.CHANNEL_IN_DEFAULT,
            audioFormat= AudioFormat.ENCODING_PCM_16BIT;
    CounterClass counter = new CounterClass(5000, 1000);
    ProgressDialog progressDialog;
    Button calibrationButton;
    TextView averagedDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorder_activity_settings);

        calibrationButton = (Button) findViewById(R.id.button_recordingThreshold);
        averagedDB = (TextView) findViewById(R.id.textView_averagedDB);
        calibrationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double averageddB = getNoiseLevel();
                averagedDB.setText("The averaged DB value is : " + averageddB);
            }
        });
    }

    public double getNoiseLevel(){
        int bufferSize = AudioRecord.getMinBufferSize(samplingRate, channelConfig, audioFormat);
        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, samplingRate,
                channelConfig, audioFormat, bufferSize);
        int sampleBufferLength = samplingRate * recordingTimeCalibration;
        short[] sampleBuffer = new short[sampleBufferLength];
        int samplesRead = 0;
        counter.start();
        while(samplesRead<sampleBufferLength) samplesRead+=audioRecord.read(sampleBuffer,
                samplesRead, sampleBufferLength-samplesRead);
        // average over all the samples
        if (audioRecord!=null) {audioRecord.release(); audioRecord=null;}
        double average=0.0;
        for (short s : sampleBuffer) average+=Math.abs(s);
        average/=bufferSize;
        return average;
    }

    public class CounterClass extends CountDownTimer{
        public CounterClass(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            //progressDialog.setMessage("Time Remaining : " + String.format("%02d",
                    //TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished)));
            averagedDB.setText("Time Remaining : " + String.format("%02d",
                    TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished)));
        }

        @Override
        public void onFinish() {
            //progressDialog.dismiss();
            averagedDB.setText("Finished");
        }
    }

    public double getNoiseLevel1(){
        int bufferSize= AudioRecord.getMinBufferSize(samplingRate, channelConfig, audioFormat);
        AudioRecord audioRecord=new AudioRecord(MediaRecorder.AudioSource.MIC, samplingRate,
                channelConfig, audioFormat, bufferSize);
        // bufferSize*=4;
        short[] data=new short[bufferSize];
        double average=0.0;
        audioRecord.startRecording();
        audioRecord.read(data, 0, bufferSize);
        audioRecord.stop();
        for (short s: data){
            if (s>0) average+=Math.abs(s);
            else bufferSize--;
        }
        average=average/bufferSize;
        Log.e("AVERAGE_BUFFER", "The average buffer size is : " + average);
        audioRecord.release();
        audioRecord=null;
        double pressure=average/REFERENCE_ESTIMATE;
        Log.e("PRESSURE_MEASURED", "The measured pressure is : " + pressure);
        double dB=20*Math.log10(pressure/REFERENCE_PRESSURE);
        Toast.makeText(RecorderActivitySettings.this, "The dB value is : " + dB, Toast.LENGTH_SHORT).show();
        return dB;
    }
}
