package com.example.orchisamadas.analyse_plot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class StartDSP2 extends AppCompatActivity {

    public static final String PREFERENCES = "AudioRecordPrefs";
    public static final String timeStartKey = "startKey";
    public static final String timeEndKey = "endKey";
    public static final String timeOftenKey = "oftenKey";
    public static final String timeRecordingKey = "recordingKey";
    public static final String thresholdNoiseKey = "thresholdKey";
    public static final String gpsValue = "gpsKey";
    public static final String originalStoreKey = "originalKey";

    private Handler handler;

    private static final int samplingRate = 8000, numChannels = 1,
            audioEncoding = AudioFormat.ENCODING_PCM_16BIT, threshold = 0;

    TextView TextHandleRemainingImpulses, textViewTime;
    AudioRecord recorder;
    String title;
    EditText comment;
    ImageButton done;
    SharedPreferences preferences;
    private static final double REFSPL = 0.00002;
    private MediaPlayer mPlayer = null, mediaPlayer;
    int numberRecordings;
    CounterClass timer;
    CaptureAudio captureAudio;

    short[] detectBuffer;
    short[][] sampleBuffer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_dsp);

        // Media Player for playback of the recorded sound
        mediaPlayer = MediaPlayer.create(this, R.raw.boo);
        preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        timer = new CounterClass(Integer.parseInt(preferences.getString(timeRecordingKey, "5")) * 1000, 1000);
        System.out.println("The recording time is : " + preferences.getString(timeRecordingKey, "5"));
        MySQLiteDatabaseHelper databaseHelper = new MySQLiteDatabaseHelper(StartDSP2.this);
        // open or create database
        SQLiteDatabase db = openOrCreateDatabase(Environment.getExternalStorageDirectory() + File.separator +
                databaseHelper.NAME, MODE_PRIVATE, null);
        databaseHelper.onCreate(db);
    }

    @Override
    protected void onStart(){
        super.onStart();

        handler = new Handler();
        int startTime = Integer.parseInt(preferences.getString(timeStartKey, "0")) * 60;
        int endTime = Integer.parseInt(preferences.getString(timeEndKey, "2")) * 60;
        // oftenTime and duration are in seconds
        int oftenTime = Integer.parseInt(preferences.getString(timeOftenKey, "20"));
        int duration = Integer.parseInt(preferences.getString(timeRecordingKey, "5"));

        numberRecordings = (endTime - startTime) / (oftenTime + duration);
        int totalTime = endTime - startTime;
        if (totalTime - numberRecordings * (oftenTime + duration) > duration) numberRecordings += 1;
        System.out.println("The number of recordings are : " + numberRecordings);


        //allow user to enter title
        comment = (EditText) findViewById(R.id.Addcomment);
        done = (ImageButton) findViewById(R.id.Enter);
        Toast.makeText(StartDSP2.this, "Add a small description of the noise you're hearing", Toast.LENGTH_SHORT).show();

        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                title = comment.getText().toString();
                if(title == null)
                    title = " ";
                //close virtual keyboard
                InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
                Toast.makeText(StartDSP2.this, "Description saved", Toast.LENGTH_SHORT).show();
                comment.setVisibility(View.INVISIBLE);
                done.setVisibility(View.INVISIBLE);

                TextHandleRemainingImpulses = (TextView)
                        findViewById(R.id.remaining_impulses);
                TextHandleRemainingImpulses.setText(getResources().getString
                        (R.string.remaining_impulse_leadtext) + Integer.toString(getResources().getInteger(R.integer.num_impulses)));
                textViewTime = (TextView)findViewById(R.id.textViewTime);
                captureAudio = new CaptureAudio(); captureAudio.execute();
            }
        });
    }

    @Override
    protected void onPause() {
        if (captureAudio != null) captureAudio.cancel(false);
        super.onPause();
        finish();
    }

    // CaptureAudio method to start the recording
    private class CaptureAudio extends AsyncTask<Void, Integer, Integer>{
        @Override
        protected void onPreExecute() {
            int bufferSize = numberRecordings * AudioRecord.getMinBufferSize(samplingRate,
                    numChannels, audioEncoding);
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, samplingRate, numChannels,
                    audioEncoding, bufferSize);
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                Toast.makeText(StartDSP2.this, "Recording device initialization falied", Toast.LENGTH_SHORT).show();
                recorder.release();
                recorder = null;
                return;
            }
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if (recorder == null) return -1; // returns -1 to the onPostExecute() method
            int remainingImpulses = numberRecordings;
            int detectBufferLength = 256;
            final int sampleBufferLength = nearestPow2Length(samplingRate *
                    Integer.parseInt(preferences.getString(timeRecordingKey, "5")));
            detectBuffer = new short[detectBufferLength];
            sampleBuffer = new short[remainingImpulses][sampleBufferLength];

            // Start recording, if there is atleast 1 impulse
            if (remainingImpulses > 0){
                recorder.startRecording();
                remainingImpulses--;
                publishProgress(0, remainingImpulses, -1, -1);
                int samplesRead = 0;
                while (samplesRead < sampleBufferLength)
                    samplesRead += recorder.read(sampleBuffer[remainingImpulses], samplesRead, sampleBufferLength - samplesRead);
                System.out.println("Samples Read : " + samplesRead);
                System.out.println("Sample buffer length : " + sampleBufferLength);
            }

            // while there are more recordings
            /**while (remainingImpulses > 0)
            {
                remainingImpulses--;
                final int impulsesRemaining = remainingImpulses;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        publishProgress(0, impulsesRemaining, -1, -1);
                        int samplesRead = 0;
                        while (samplesRead < sampleBufferLength)
                            samplesRead += recorder.read(sampleBuffer[impulsesRemaining], samplesRead, sampleBufferLength - samplesRead);
                        System.out.println("Samples Read : " + samplesRead);
                        System.out.println("Sample Buffer Length : " + sampleBufferLength);
                    }
                }, Integer.parseInt(preferences.getString(timeOftenKey, "10")) * 1000);

                if (isCancelled()){
                    detectBuffer = null; sampleBuffer = null;
                    return -1; // -1 returned to the onPostExecute method
                }
            }*/

            while (remainingImpulses > 0){
                remainingImpulses--;
                try {
                    Thread.sleep(Integer.parseInt(preferences.getString(timeOftenKey, "10")) * 1000);
                    publishProgress(0, remainingImpulses, -1, -1);
                    int samplesRead = 0;
                    while (samplesRead < sampleBufferLength)
                        samplesRead += recorder.read(sampleBuffer[remainingImpulses], samplesRead, sampleBufferLength - samplesRead);
                } catch (InterruptedException e){
                    System.out.println("Exception while running thread of type : " + e.toString());
                }
            }

            // release the recorder to finish recording
            if (recorder != null) {
                recorder.release();
                recorder = null;
            }

            if (!isCancelled()) publishProgress(-1, -1, 0, -1);

            // save recorded audio to an external file
            saveRecord(sampleBuffer, sampleBufferLength);

           return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            if (recorder != null) {
                recorder.release();
                recorder = null;
            }
            if (integer == -1)
                Toast.makeText(StartDSP2.this, "An error occurred", Toast.LENGTH_SHORT).show();
            else{
                // Allowing user to playback the recorded sound
                ImageButton playback = (ImageButton) findViewById(R.id.playback);
                playback.setVisibility(View.VISIBLE);
                playback.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Method to playback recorded audio
                        playbackAudio();
                    }
                });
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (values[0] == 0) timer.start();
            if (values[1] != -1)
                TextHandleRemainingImpulses.setText("Number of recordings remaining : " + values[1]);
            if (values[2] != -1){
                TextHandleRemainingImpulses.setVisibility(TextView.INVISIBLE);
            }
            if (values[3] != -1){
                Toast.makeText(StartDSP2.this, "A computation error has occurred", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Method to playback recorded audio
    public void playbackAudio(){
        File file = new File(Environment.getExternalStorageDirectory(), "audioFile.wav");
        int audioLength = (int)file.length() / 2;
        short[] audio = new short[audioLength];
        try {
            DataInputStream dataInputStream = new DataInputStream(
                    new BufferedInputStream(new FileInputStream(file)));
            int n = 0;
            if (dataInputStream.available() > 0) {
                audio[n++] = dataInputStream.readShort();
            }
            dataInputStream.close();
        } catch (IOException e){
            System.out.println("Error while playback of sound of type : " + e.toString());
        }

        // Playback the sound using AudioTrack (from audio array)
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, samplingRate, numChannels,
                audioEncoding, audioLength, AudioTrack.MODE_STREAM);
        // Start playback
        audioTrack.play();
        // Write the audio buffer into the audioTrack object
        audioTrack.write(audio, 0, audioLength);
    }

    // saves audio record to a file
    public void saveRecord(short[][] sampleBuffer, int sampleBufferLength){
        File file = new File(Environment.getExternalStorageDirectory(), "audioFile.wav");
        if (file.exists()) file.delete();
        try{
            file.createNewFile();
        } catch (IOException e) {
            System.out.println("Exception while creating new file of type : " + e.toString());
        }
        try {
            DataOutputStream dataOutputStream = new DataOutputStream(
                    new BufferedOutputStream(new FileOutputStream(file)));
            for (int k=numberRecordings - 1; k>=0; k--)
                for (int n=0; n<sampleBufferLength; n++)
                    dataOutputStream.writeShort(sampleBuffer[k][n]);
        } catch (IOException e) {
            System.out.println("Exception while saving file of type : " + e.toString());
        }
    }

    // If an impulse is detected
    private boolean detectImpulse(short[] samples){
        for (int i=0; i<samples.length; i++) if (samples[i] >= threshold) return true;
        return false;
    }

    public static int nearestPow2Length(int length) {
        int temp = (int) (Math.log(length) / Math.log(2.0) + 0.5);
        length = 1;
        for (int n = 1; n <= temp; n++) length = length * 2;
        return length;
    }

    // CountDownTimer class for recording
    public class CounterClass extends CountDownTimer {
        public CounterClass(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            long millis = millisUntilFinished;
            String hms = String.format("%02d", TimeUnit.MILLISECONDS.toSeconds(millis) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
            System.out.println(hms);
            textViewTime.setText(hms);
        }

        // Sound has been captured
        @Override
        public void onFinish() {
            textViewTime.setText("Captured");
        }
    }

    // Display menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_start_ds, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // Menu options

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            // Plays the chirp (50-1000 Hz) to excite room resonance
            case R.id.GenerateChirp:
                playChirp();
                return true;
            // Option to view the recording settings
            case R.id.SettingsPreferences:
                Intent preferencesIntent = new Intent(StartDSP2.this, Preferences.class);
                startActivity(preferencesIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Method to play the chirp
    private void playChirp(){
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/MySounds/Chirp_50_1000Hz.wav");
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            System.out.println("Exception while playing chirp of type : " + e.toString());
            e.printStackTrace();
        }
    }
}