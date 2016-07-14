package com.example.orchisamadas.analyse_plot;

import android.media.MediaRecorder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class RecordingAudioThreshold extends AppCompatActivity {

    // This class generates the spectrogram of a wav file
    private MediaRecorder mediaRecorder = null;
    private Timer timerThread;
    private Button startRecording, stopRecording;
    private TextView recordingThreshold, recordingThresholdDB;
    int amplitude = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_spectrogram);

        // Initialize the timer (used to cancel the thread if it's not running).
        timerThread = new Timer();

        // Method to calibrate the microphone
        startRecording = (Button) findViewById(R.id.button_startRecording);
        stopRecording = (Button) findViewById(R.id.button_stopRecording);
        recordingThreshold = (TextView) findViewById(R.id.textView_threshold);
        recordingThresholdDB = (TextView) findViewById(R.id.textView_thresholdDB);

        startRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaRecorder = new MediaRecorder();
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                mediaRecorder.setOutputFile("/dev/null");

                try {
                    mediaRecorder.prepare();
                    mediaRecorder.start();
                    System.out.println("Started Recording using Media Recorder");
                } catch (IOException e) {
                    System.out.println("Exception while recording of type : " + e.toString());
                }

                // start the timer to print the recorded values
                timerThread.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        amplitude = mediaRecorder.getMaxAmplitude();
                        recordingThreshold.post(new Runnable() {
                            @Override
                            public void run() {
                                recordingThreshold.setText("The recorded value is : " + amplitude);
                            }
                        });
                        recordingThresholdDB.post(new Runnable() {
                            @Override
                            public void run() {
                                recordingThresholdDB.setText("The decibel value is : " + 20 * Math.log10(amplitude));
                            }
                        });
                    }
                }, 0, 500);
            }
        });

        stopRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timerThread.cancel();
                if (mediaRecorder != null) {
                    mediaRecorder.release();
                    mediaRecorder = null;
                }
                recordingThreshold.setText("Calibration complete.");
                recordingThresholdDB.setText("Calibration complete.");
            }
        });
    }
}