package com.example.richardjiang.comotion.remoteSensorHandler;

/**
 * Created by Richard Jiang on 6/27/2015.
 */
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;



import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DataStorageService extends WearableListenerService {
    private static final String TAG = "DataStorageService";

    private GoogleApiClient mGoogleApiClient;
    //private SharedPreferences preferences;
    private File directory;
    private File file;
    //private FileOutputStream stream;
    //private OutputStreamWriter writer;

    @Override
    public void onCreate() {
        super.onCreate();
        //preferences = getSharedPreferences(Utils.SHARED_PREFS_KEY, Context.MODE_PRIVATE);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        //originally from the saveData method
        directory = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), "coMotion");
        if (! directory.exists()){
            if (! directory.mkdirs()){
                Log.d(TAG, "failed to create directory");
                return;
            }
        }

        String timeStamp_1 = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        long timeStamp = System.currentTimeMillis();
        file = new File(directory.getPath()+File.separator+"wearable_data_"+timeStamp_1+".txt");

        /*
        try {
            stream = new FileOutputStream(file, true);
            writer = new OutputStreamWriter(stream);

        } catch (Exception e) {
            Log.d(TAG, "Error In Creating The File Writer");
            e.printStackTrace();
        }
        */


        //for testing purpose
        System.out.println("INSIDE THE DATA STORAGE PART!!!");
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged: " + dataEvents);
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();
        if(!mGoogleApiClient.isConnected()) {
            ConnectionResult connectionResult = mGoogleApiClient
                    .blockingConnect(30, TimeUnit.SECONDS);
            if (!connectionResult.isSuccess()) {
                Log.e(TAG, "DataLayerListenerService failed to connect to GoogleApiClient.");
                return;
            }
        }

        // Loop through the events and send a message back to the node that created the data item.
        for (DataEvent event : events) {
            Uri uri = event.getDataItem().getUri();
            String path = uri.getPath();
            if (Utils.SENSOR_DATA_PATH.equals(path) && event.getType() == DataEvent.TYPE_CHANGED) {
                byte[] rawData = event.getDataItem().getData();
                DataMap sensorData = DataMap.fromByteArray(rawData);
                //sensorData.putBoolean(Utils.SLEEPING_KEY, preferences.getBoolean(Utils.SLEEPING_KEY, false));
                Log.d(TAG, "Recording new data item: " + sensorData);

                saveData(sensorData);
            }
        }
    }

    private JSONObject dataMapAsJSONObject(DataMap data) {
        Bundle bundle = data.toBundle();
        JSONObject json = new JSONObject();
        Set<String> keys = bundle.keySet();
        for (String key : keys) {
            try {
                // json.put(key, bundle.get(key)); see edit below
                json.put(key, JSONObject.wrap(bundle.get(key)));
            } catch(JSONException e) {
                //Handle exception here
                Log.d(TAG, "Json put failed");
            }
        }
        return json;
    }

    /* Checks if external storage is available for read and write */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private void saveData(DataMap data) {
        if (!isExternalStorageWritable()) {
            Log.d(TAG, "External Storage Not Writable");
            return;
        }

        /*
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        directory.mkdirs();
        long timeStamp = System.currentTimeMillis();
        File file = new File(directory, "wearable_data_"+timeStamp+".txt");
        */

        String dataJSON = dataMapAsJSONObject(data).toString() + "\n";

        String dataToWrite = processStringForAcc(dataJSON);

        try {
            FileOutputStream stream = new FileOutputStream(file, true);
            OutputStreamWriter writer = new OutputStreamWriter(stream);
            //writer.write(dataJSON);
            writer.write(dataToWrite);
            writer.close();

        } catch (Exception e) {
            Log.d(TAG, "Error Saving");
            e.printStackTrace();
        }
    }

    @Override
    public void onPeerConnected(Node peer) {
        Log.d(TAG, "onPeerConnected: " + peer);
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        Log.d(TAG, "onPeerDisconnected: " + peer);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    //process the string before storing for direct input to Excel
    private String processStringForAcc(String rawString) {
        String result = null;
        String temp1,temp2;

        try {
            String[] temp;
            temp = rawString.split("\\[");
            temp1 = temp[1].split("\\]")[0];
            temp2 = temp[1].split(":")[1];
            temp2 = temp2.substring(0, temp2.length() - 2);

            result = temp1 + "," + temp2 + "\n";
        } catch (Exception E) {
            Log.d(TAG, "Incomplete processing");
        }
        return result;
    }

}