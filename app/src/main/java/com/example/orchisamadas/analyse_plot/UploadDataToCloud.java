package com.example.orchisamadas.analyse_plot;

import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class UploadDataToCloud extends AppCompatActivity {

    final String FILE_URL = "";
    InputStream is = null;
    Button uploadData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_data_to_cloud);

        uploadData = (Button) findViewById(R.id.button_upload_data);

        StrictMode.ThreadPolicy threadPolicy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(threadPolicy);

        uploadData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                List<NameValuePair> nameValuePairList = new ArrayList<NameValuePair>();
                nameValuePairList.add(new BasicNameValuePair("name", "Aditya Agarwal"));

                try {
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(FILE_URL);
                    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairList));
                    HttpResponse httpResponse = httpClient.execute(httpPost);
                    HttpEntity httpEntity = httpResponse.getEntity();
                    is = httpEntity.getContent();
                    Toast.makeText(UploadDataToCloud.this, "Data has been successfully inserted into the database",
                            Toast.LENGTH_SHORT).show();
                    is.close();
                } catch (UnsupportedEncodingException e){
                    System.out.println("Exception of the type : " + e.toString());
                } catch (ClientProtocolException e){
                    System.out.println("Exception of the type : " + e.toString());
                } catch (IOException e){
                    System.out.println("Exception of the type : " + e.toString());
                }
            }
        });
    }
}
