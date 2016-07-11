package com.example.orchisamadas.analyse_plot;


import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import com.example.orchisamadas.analyse_plot.MySQLiteDatabaseContract.TableEntry;

public class MySQLiteDatabaseHelper extends SQLiteOpenHelper{
    public static final String NAME="DataAnalysis3.db";
    public static final int VERSION=1;
    public static Context mContext;

    public static final String PREFERENCES = "AudioRecordingPrefs";
    public static final String timeStartKey = "startKey";
    public static final String timeEndKey = "endKey";
    public static final String timeOftenKey = "oftenKey";
    public static final String timeRecordingKey = "recordingKey";
    public static final String thresholdNoiseKey = "thresholdKey";
    public static final String gpsValueKey = "gpsKey";
    public static final String originalStoreKey = "originalKey";

    public MySQLiteDatabaseHelper(Context context){
        super(context,NAME,null,VERSION);mContext=context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // analysis_data -> stores analysis results (dsp results)

        String create = "CREATE TABLE IF NOT EXISTS " + TableEntry.TABLE_NAME + " (" + TableEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + TableEntry.COLUMN_NAME+ " TEXT, "
                + TableEntry.COLUMN_COMMENT + " TEXT, "
                + TableEntry.COLUMN_DATE + " TEXT, "
                + TableEntry.COLUMN_MAX_SIGNAL + " REAL, "
                + TableEntry.COLUMN_PERCENTAGE_WORSE_CASE + " REAL, "
                + TableEntry.COLUMN_RATIO_BACKGROUND_NOSE + " REAL";
        create = create + ")";
        db.execSQL(create);

        // fft_data -> stores the FFT (Fast Fourier Transform) results

        int numImpulses = StartDSP.numberImpulses;
        System.out.println("The number of impulses in the MySQLiteDatabaseHelper are : " + numImpulses);
        Log.e("NUMBER_IMPULSES", "The number of impulses are : " + numImpulses);

        db.execSQL("DROP TABLE IF EXISTS " + TableEntry.TABLE_NAME_FFT);

        create = "CREATE TABLE IF NOT EXISTS " + TableEntry.TABLE_NAME_FFT + " ("
                + TableEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + TableEntry.COLUMN_NAME_DATE + " TEXT, "
                + TableEntry.COLUMN_NAME_COMMENT+ " TEXT, "
                + TableEntry.COLUMN_NAME_FILENAME + " TEXT, "
                + TableEntry.COLUMN_NAME_LATITUDE + " REAL, "
                + TableEntry.COLUMN_NAME_LONGITUDE + " REAL, "
                + TableEntry.COLUMN_NAME_XVALS + " BLOB, "
                + TableEntry.COLUMN_NAME_YVALS + " BLOB";

        for(int k = 0; k < numImpulses; k++)
            create = create + ", " + TableEntry.COLUMN_NAME_IMPULSE + Integer.toString(k) + " BLOB";

        create = create + ")";
        db.execSQL(create);

    }
    public void deleteTable(SQLiteDatabase db, String tableName){
        final String delete="DROP TABLE IF EXISTS "+tableName;
        db.execSQL(delete);
    }
    public void deleteAllEntries(SQLiteDatabase db,String tableName){
        db.delete(tableName, null, null);}

    public void deleteDatabase(){mContext.deleteDatabase(NAME);}

    @Override
    public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) {
        if (newVersion <= oldVersion)
            return;
        deleteDatabase();
        onCreate(db);
        return;
    }
}


