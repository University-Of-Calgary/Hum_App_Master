package com.example.orchisamadas.analyse_plot;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * This activity calibrates an android device's microphone.
 * It makes a recording using 1. An external microphone.
 * 2. Using the android's microphone
 * After converting the data from time domain into frequency domain,
 * the gain for each frequency band is calculated.
 */

public class CalibrateMicrophone extends AppCompatActivity {

    final int SAMPLING_RATE = 8000, CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_DEFAULT,
            AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT, RECORDING_DURATION = 5;
    final String EXTERNAL_AUDIO_FILENAME = "external_audio.wav", ANDROID_AUDIO_FILENAME = "android_audio.wav";
    AudioRecord recorder;
    Button startCalibration, startRecording, startPlayback, androidRecording, calculateFrequencyGain;
    TextView timer;
    CounterClass counterClass = new CounterClass(RECORDING_DURATION * 1000, 1000);
    double[] externalMicValues, androidMicValues;
    final String EXTERNAL_MIC_RECORDING = "External Microphone",
            ANDROID_MIC_RECORDING = "Android Microphone";
    double[] frequencyGain, frequencyAveragedExternal, frequencyAveragedAndroid;
    double REFSPL = 0.00002; // Reference Sound Pressure Level equal to 20 uPa.
    final String DIRECTORY_NAME = "Hum_Application", GAIN_FILENAME = "frequency_gain_values.txt";
    CaptureAudio captureAudio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibrate_microphone);

        startCalibration = (Button) findViewById(R.id.button_calibrate_microphone);
        startRecording = (Button) findViewById(R.id.button_start_recording);
        startPlayback = (Button) findViewById(R.id.button_start_playback);
        androidRecording = (Button) findViewById(R.id.button_microphone_recording);
        calculateFrequencyGain = (Button) findViewById(R.id.button_frequency_gain);
        timer = (TextView) findViewById(R.id.textView_timer);

        startCalibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Please insert an external microphone to start recording
                externalMicrophoneRecording();
                androidMicrophoneRecording();
            }
        });

        calculateFrequencyGain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Method to calculate the frequency gain from the prerecorded values
                // from the external microphone and the internal android microphone
                calculateFrequencyGain();
                // Print frequency gain values
                for (int i = 0; i < frequencyGain.length; i++)
                    System.out.print(frequencyGain[i] + " ");
                System.out.println();
                // Frequency Gain values needs to be saved to a file
                saveGainValuesToFile();
            }
        });

    }

    @Override
    protected void onPause() {
        if(captureAudio!=null) captureAudio.cancel(false);
        super.onPause();
        finish();
    }

    // Method to save the frequency gain values to a file
    public void saveGainValuesToFile(){
        File humDir = new File(Environment.getExternalStorageDirectory(), DIRECTORY_NAME);
        if(!humDir.exists()) humDir.mkdir();
        File file = new File(humDir + File.separator + GAIN_FILENAME);
        System.out.println("File checked for existence : " + humDir + File.separator + GAIN_FILENAME);
        if(!file.exists()){
            try {
                PrintWriter printWriter = new PrintWriter(file);
                for(double gainValue : frequencyGain)
                    printWriter.println(Double.toString(gainValue));
                printWriter.flush();
                printWriter.close();
            } catch(IOException e){
                System.out.println("Exception of type : " + e.toString());
            }
        } else{
            System.out.println("File already exists");
        }
    }

    public void calculateFrequencyGain(){
        // Calculate the frequency gain from the two recordings
        frequencyGain = new double[frequencyAveragedExternal.length];
        for(int i=0; i<frequencyAveragedExternal.length; i++)
            frequencyGain[i] = frequencyAveragedExternal[i] / frequencyAveragedAndroid[i];
        System.out.println("The length of the frequency gain calculated is : " + frequencyGain.length);
    }

    public class CounterClass extends CountDownTimer{
        public CounterClass(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            String hms = String.format("%02d", TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)));
            timer.setText("Time remaining : " + hms);
        }

        @Override
        public void onFinish() {
            timer.setText("Captured Sound");
        }
    }

    public void externalMicrophoneRecording(){
        Toast.makeText(CalibrateMicrophone.this,
                "Please insert an external microphone to continue", Toast.LENGTH_SHORT).show();
        startRecording.setVisibility(View.VISIBLE);
        startRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // new CaptureAudio().execute(EXTERNAL_MIC_RECORDING);
                captureAudio = new CaptureAudio(); captureAudio.execute(EXTERNAL_MIC_RECORDING);
            }
        });
    }

    public void androidMicrophoneRecording(){
        Toast.makeText(CalibrateMicrophone.this, "Continue to record using the Android microphone",
                Toast.LENGTH_SHORT).show();
        androidRecording.setVisibility(View.VISIBLE);
        androidRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // new CaptureAudio().execute(ANDROID_MIC_RECORDING);
                captureAudio = new CaptureAudio(); captureAudio.execute(ANDROID_MIC_RECORDING);
            }
        });
    }

    public class CaptureAudio extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLING_RATE, CHANNEL_CONFIG, AUDIO_ENCODING);
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLING_RATE,
                    CHANNEL_CONFIG, AUDIO_ENCODING, bufferSize);
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                Toast.makeText(CalibrateMicrophone.this, "Recording device initialization failed",
                        Toast.LENGTH_SHORT).show();
                recorder.release();
                recorder=null;
            }
        }

        @Override
        protected String doInBackground(String... params) {
            int samplesRead = 0;
            int sampleBufferLength = nearestPow2Length(SAMPLING_RATE * RECORDING_DURATION);
            short[] sampleBuffer = new short[sampleBufferLength];
            counterClass.start();
            recorder.startRecording();
            while (samplesRead < sampleBufferLength)
                samplesRead += recorder.read(sampleBuffer, samplesRead, sampleBufferLength - samplesRead);
            System.out.println("The total number of samples read is : " + samplesRead);
            double max = calculateMax(sampleBuffer);
            System.out.println("The mic recording max value is : " + max);
            if(params[0].equals(EXTERNAL_MIC_RECORDING)) {
                System.out.println("Printing external mic values normally");
                for(int i=0; i<30; i++) System.out.print(sampleBuffer[i] + " ");
                for(int i=1111; i<1200; i++) System.out.print(sampleBuffer[i] + " ");
                System.out.println();
                externalMicValues = normalizeTimeDomainData(sampleBuffer, max);
                System.out.println("Printing external mic values after normalization");
                for(int i=0; i<30; i++) System.out.print(externalMicValues[i] + " ");
                for(int i=1111; i<1200; i++) System.out.print(externalMicValues[i] + " ");
                System.out.println();
                applyBasicWindow(externalMicValues);
                System.out.println("Printing external mic values after applying basic window");
                for(int i=0; i<30; i++) System.out.print(externalMicValues[i] + " ");
                for(int i=1111; i<1200; i++) System.out.print(externalMicValues[i] + " ");
                System.out.println();

                int error = doubleFFT(externalMicValues);
                System.out.println("The length of the external mic value after fft : " +
                        externalMicValues.length);
                System.out.println("Printing external mic values after applying fft");
                for(int i=0; i<30; i++) System.out.print(externalMicValues[i] + " ");
                for(int i=1111; i<1200; i++) System.out.print(externalMicValues[i] + " ");
                System.out.println();
                // Divide the externalMicValues by the Reference Sound Pressure Level
                for(int i=0; i<externalMicValues.length; i++) externalMicValues[i] /= REFSPL;
                System.out.println("Printing external mic values after dividing by REFSPL");
                for(int i=0; i<30; i++) System.out.print(externalMicValues[i] + " ");
                for(int i=1111; i<1200; i++) System.out.print(externalMicValues[i] + " ");
                System.out.println();
                Log.e("EXTERNAL_MIC_LENGTH", "The length of the external Microphone recording is : " +
                        externalMicValues.length);
                System.out.println("The error associated with External microphone recording is : " + error);
            }
            else if (params[0].equals(ANDROID_MIC_RECORDING)){
                androidMicValues = normalizeTimeDomainData(sampleBuffer, max);
                applyBasicWindow(androidMicValues);
                int error = doubleFFT(androidMicValues);
                System.out.println("Printing internal mic values after applying fft" );
                for(int i=0; i<30; i++) System.out.print(androidMicValues[i] + " ");
                for(int i=1111; i<1200; i++) System.out.print(androidMicValues[i] + " ");
                System.out.println();
                // Divide the androidMicValues by the Reference Sound Pressure Level
                for(int i=0; i<androidMicValues.length; i++) androidMicValues[i] /= REFSPL;
                System.out.println("Printing internal mic values after dividing by REFSPL");
                for(int i=0; i<30; i++) System.out.print(androidMicValues[i] + " ");
                for(int i=1111; i<1200; i++) System.out.print(androidMicValues[i] + " ");
                System.out.println();
                Log.e("INTERNAL_MIC_LENGTH", "The length of the internal Microphone recording is : " +
                        androidMicValues.length);
                System.out.println("The error associated with Android microphone recording is : " + error);
            }
            // Clear the recorder
            if (recorder != null) {recorder.release(); recorder=null;}
            if(params[0].equals(EXTERNAL_MIC_RECORDING)) saveRecording(sampleBuffer, EXTERNAL_AUDIO_FILENAME);
            else if (params[0].equals(ANDROID_MIC_RECORDING)) saveRecording(sampleBuffer, ANDROID_AUDIO_FILENAME);

            int samplesPerPoint = 32; // samples per point

            // Calculate the amplitude values as Samples_Per_Bin
            if(params[0].equals(EXTERNAL_MIC_RECORDING)){
                int width = externalMicValues.length / samplesPerPoint / 2;
                System.out.println("The width of the externalMicValues is : " + width);
                double maxYvalExternal = 0; // Stores the max amplitude (external microphone)
                // Stores the amplitude values (external microphone)
                double[] tempBufferExternal = new double[width];
                for(int k=0; k<tempBufferExternal.length; k++){
                    for(int n=0; n<samplesPerPoint; n++)
                        tempBufferExternal[k] += externalMicValues[k*samplesPerPoint + n];
                    tempBufferExternal[k] /= (double)samplesPerPoint;
                    if(maxYvalExternal < tempBufferExternal[k]) maxYvalExternal = tempBufferExternal[k];
                }

                System.out.println("The maxYvalExternal is : " + maxYvalExternal);
                System.out.println("The external temp Buffer values are : ");
                for(int i=0; i<tempBufferExternal.length; i++) System.out.print(tempBufferExternal[i] + " ");
                System.out.println();

                // Stores the frequency values (external microphone)
                double[] xValsExternal = new double[tempBufferExternal.length];
                for(int k=0; k<xValsExternal.length; k++)
                    xValsExternal[k] = k * SAMPLING_RATE / (2*xValsExternal.length);

                System.out.println("The external frequency values are : " );
                for(int i=0; i<xValsExternal.length; i++) System.out.print(xValsExternal[i] + " ");
                System.out.println();

                int j = 0;
                ArrayList<Double> frequencyAveraged =  new ArrayList<Double>();
                // Calculate the frequency gain in each frequency band
                for(int i=0; i<SAMPLING_RATE/2; i+=25){
                    double temp = 0.0; int count = 0;
                    while(j<tempBufferExternal.length && xValsExternal[j] >= i && xValsExternal[j] < (i+25)) {
                        temp += tempBufferExternal[j];
                        count ++;
                        j++;
                    }
                    frequencyAveraged.add(temp/(double)count);
                    System.out.print(i + " : " + temp/(double)count);
                }
                System.out.println();

                // Convert the ArrayList into a double array
                // The frequency ranges are
                // (0-25) Hz - amplitude_average1
                // (25-50) Hz - amplitude_average2
                // (50-75) Hz - amplitude_average3
                // ...
                frequencyAveragedExternal = new double[frequencyAveraged.size()];
                for(int i=0; i<frequencyAveraged.size(); i++) frequencyAveragedExternal[i] = frequencyAveraged.get(i);
                System.out.println("The frequency averaged values for external microphone are : " );
                for(int i=0; i<frequencyAveragedExternal.length; i++) System.out.print(frequencyAveragedExternal[i] + " ");
                System.out.println();
                System.out.println("The length of the external recording array is : " + frequencyAveragedExternal.length);
            }

            else if(params[0].equals(ANDROID_MIC_RECORDING)){
                int width = androidMicValues.length / samplesPerPoint / 2;
                System.out.println("The width of the internal microphone values is : " + width);
                double maxYvalAndroid = 0; // Stores the max amplitude (android microphone)
                // Stores the amplitude values (android microphone)
                double[] tempBufferAndroid = new double[width];

                for(int k=0; k<tempBufferAndroid.length; k++){
                    for(int n=0; n<samplesPerPoint; n++)
                        tempBufferAndroid[k] += androidMicValues[k*samplesPerPoint + n];
                    tempBufferAndroid[k] /= (double) samplesPerPoint;
                    if(maxYvalAndroid < tempBufferAndroid[k]) maxYvalAndroid = tempBufferAndroid[k];
                }

                System.out.println("The maxYvalAndroid : " + maxYvalAndroid);

                // Stores the frequency values (android microphone)
                double[] xValsAndroid = new double[tempBufferAndroid.length];
                for(int k=0; k<xValsAndroid.length; k++)
                    xValsAndroid[k] = k * SAMPLING_RATE / (2*xValsAndroid.length);

                System.out.println("The android frequency values are : " );
                for(int i=0; i<xValsAndroid.length; i++) System.out.print(xValsAndroid[i] + " ");
                System.out.println();

                // Apply the frequency gain in each frequency band
                int j = 0;
                ArrayList<Double> frequencyAveraged = new ArrayList<Double>();
                // Calculate the frequency gain in each frequency band
                for(int i=0; i<SAMPLING_RATE/2; i+=25){
                    double temp = 0.0; int count = 0;
                    while(j<tempBufferAndroid.length && xValsAndroid[j] >= i && xValsAndroid[j] < (i+25)){
                        temp += tempBufferAndroid[j];
                        count++;
                        j++;
                    }
                    frequencyAveraged.add(temp/count);
                }

                // Convert the Array List into a double array
                frequencyAveragedAndroid = new double[frequencyAveraged.size()];
                for(int i=0; i<frequencyAveraged.size(); i++)  frequencyAveragedAndroid[i] = frequencyAveraged.get(i);
                System.out.println("The length of the frequency averaged values for the android internal microphone are : " + frequencyAveragedAndroid.length);
                System.out.println("The frequency averaged values for the android microphone are ");
                for(int i=0; i<frequencyAveragedAndroid.length; i++) System.out.print(frequencyAveragedAndroid[i] + " ");
                System.out.println();
            }

            return params[0];
        }

        @Override
        protected void onPostExecute(String aVoid) {
            final String fileName = aVoid;
            startPlayback.setVisibility(View.VISIBLE);
            startPlayback.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(fileName.equals(EXTERNAL_MIC_RECORDING))
                       playbackRecording(EXTERNAL_AUDIO_FILENAME);
                    else if(fileName.equals(ANDROID_MIC_RECORDING))
                        playbackRecording(ANDROID_AUDIO_FILENAME);
                }
            });
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }
    }

    public int doubleFFT(double[] samples){
        double[] real = new double[samples.length], imag = new double[samples.length];
        System.arraycopy(samples, 0, real, 0, samples.length);
        for(int n=0; n<samples.length; n++) imag[n] = 0;
        int error = FFTbase.fft(real, imag, true);
        if(error==-1) return -1;
        for(int n=0; n<samples.length; n++) samples[n] = Math.sqrt(real[n]*real[n] + imag[n]*imag[n]);
        return 0;
    }

    public static void applyBasicWindow(double[] samples){
        samples[0] *= 0.0625;
        samples[1] *= 0.125;
        samples[2] *= 0.25;
        samples[3] *= 0.5;
        samples[4] *= 0.75;
        samples[5] *= 0.875;
        samples[6] *= 0.9375;

        samples[samples.length - 1] *= 0.0625;
        samples[samples.length - 2] *= 0.125;
        samples[samples.length - 3] *= 0.25;
        samples[samples.length - 4] *= 0.5;
        samples[samples.length - 5] *= 0.75;
        samples[samples.length - 6] *= 0.875;
        samples[samples.length - 7] *= 0.9375;
    }

    public double[] normalizeTimeDomainData(short[] sampleBuffer, double max){
        double[] samples = new double[sampleBuffer.length];
        for (int i=0; i<sampleBuffer.length; i++) samples[i] = sampleBuffer[i] / max;
        return samples;
    }

    public double calculateMax(short[] sampleBuffer){
        double max = 0.0;
        for (int i=0; i<sampleBuffer.length; i++)
            if (sampleBuffer[i] > max) max=sampleBuffer[i];
        return max;
    }

    public int nearestPow2Length(int length){
        int temp = (int) (Math.log(length) / Math.log(2.0) + 0.5); length = 1;
        for (int n=1; n<=temp; n++) length*=2;
        return length;
    }

    // Method for saving the recorded file into the phone's memory
    public void saveRecording(short[] sampleBuffer, String filename){
        /**File file = new File(Environment.getExternalStorageDirectory(),
                new SimpleDateFormat("yyyyMMddhhmmss'.wav'").format(new Date()));*/
        File file = new File(Environment.getExternalStorageDirectory(), filename);
        if (file.exists()) file.delete();
        try {
            file.createNewFile();
            DataOutputStream dataOutputStream =
                    new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            for(int n=0; n<sampleBuffer.length; n++) dataOutputStream.writeShort(sampleBuffer[n]);
        } catch (IOException e) {
            System.out.println("Exception while saving record of type : " + e.toString());
        }
    }

    // Method for playing back the recorded audio
    public void playbackRecording(String filename){
        File file = new File(Environment.getExternalStorageDirectory(), filename);
        int audioLength = (int)file.length()/2;
        short[] audio = new short[audioLength];
        try {
            DataInputStream dataInputStream =
                    new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
            int audioRead = 0;
            while (dataInputStream.available() > 0 ){
                audio[audioRead++] = dataInputStream.readShort();
            }
        } catch (IOException e) {
            System.out.println("Exception while playback record of type : " + e.toString());
        }

        // Create an AudioTrack object for playback
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLING_RATE,
                CHANNEL_CONFIG, AUDIO_ENCODING, audioLength, AudioTrack.MODE_STREAM);
        audioTrack.play();
        audioTrack.write(audio, 0, audioLength);
    }
}