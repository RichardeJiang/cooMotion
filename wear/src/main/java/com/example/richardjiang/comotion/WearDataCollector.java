package com.example.richardjiang.comotion;

/**
 * Created by Richard Jiang on 6/27/2015.
 */
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.util.SparseLongArray;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Richard Jiang on 6/26/2015.
 */
public class WearDataCollector extends WearableListenerService implements SensorEventListener {
    private static final String TAG = "WearDataCollector";

    private static final int TRANSMISSION_GAP = 100;
    //  1000/transmission_gap is the frequency of the collection service

    //sensor name list
    private final static int SENS_LINEAR_ACCELERATION = Sensor.TYPE_LINEAR_ACCELERATION;
    private final static int SENS_GYROSCOPE = Sensor.TYPE_GYROSCOPE;
    private final static int SENS_ROTATION_VECTOR = Sensor.TYPE_ROTATION_VECTOR;
    private final static int SENS_MAGNETIC_FIELD = Sensor.TYPE_MAGNETIC_FIELD;

    private static long currentTimeStamp;
    private static long prevTimeStamp = System.currentTimeMillis();

    private SparseLongArray lastSensorData;

    private PutDataMapRequest mSensorData;
    private GoogleApiClient mGoogleApiClient;
    private ExecutorService mExecutorService;
    private SensorManager mSensorManager;

    //sensor list
    private Sensor mSensor_LinearAcc;
    private Sensor mSensor_Gyroscope;
    private Sensor mSensor_RotationVec;
    private Sensor mSensor_MagneticField;

    @Override
    public void onCreate() {
        super.onCreate();

        //initialization
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
        mExecutorService = Executors.newCachedThreadPool();
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        lastSensorData = new SparseLongArray();

        System.out.println("INSIDE THE WEAR DATA COLLECTION METHOD!");

    }

    @Override // SensorEventListener
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        return;
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {

        //ignore this part since we are now collecting multiple sensor data
        /*
        if(event.sensor.getType() != SENS_LINEAR_ACCELERATION) {
            return;
        }
        */
        currentTimeStamp = System.currentTimeMillis();

        long lastTimestamp = lastSensorData.get(event.sensor.getType());
        long timeAgo = currentTimeStamp - lastTimestamp;

        if (lastTimestamp != 0) {
            if (timeAgo < 100) {
                return;
            }
        }

        lastSensorData.put(event.sensor.getType(), currentTimeStamp);

        //IMPORTANT: TEST WHETHER THIS TIME CHECK SHOULD BE HERE OR IN SENDSENSORDATA
        //BASED ON RETURN WILL RETURN TO WHAT?
        /*
        long timeGap = currentTimeStamp - prevTimeStamp;

        if(prevTimeStamp != 0) {
            if(timeGap < TRANSMISSION_GAP) {
                return;
            }
        }
        */

        prevTimeStamp = currentTimeStamp;

        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {

                //String tempTimeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                //long timeStamp_2 = Integer.valueOf(tempTimeStamp);
                //sendSensorData(event.sensor.getType(), event.accuracy, event.timestamp, event.values);
                //sendSensorData(event.sensor.getType(), event.accuracy, timeStamp_2, event.values);
                sendSensorData(event.sensor.getName(), event.sensor.getType(), currentTimeStamp, event.values);
            }
        });

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        // Check to see if the message is to start an activity
        String path, tempCommand;
        tempCommand = messageEvent.getPath();
        path = tempCommand.split(",")[0];

        //String path = messageEvent.getPath();
        Log.d(TAG, "onMessageReceived: " + path);
        System.out.println("INSIDE THE MESSAGE RECEIVING METHOD");

        if (path.equals(Utils.START_MEASUREMENT)) {
            startSensorListeners();
        }

        if (path.equals(Utils.STOP_MEASUREMENT)) {
            stopSensorListeners();
        }

        if (path.equals(Utils.START_PATTERN)) {
            String length = tempCommand.split(",")[1];
            int lengthOfRecord;

            switch(length) {
                case "1 sec":
                    lengthOfRecord = 1000;
                    break;
                case "2 sec":
                    lengthOfRecord = 2000;
                    break;
                case "3 sec":
                    lengthOfRecord = 3000;
                    break;
                case "4 sec":
                    lengthOfRecord = 4000;
                    break;
                default:
                    lengthOfRecord = 3000;
                    break;
            }
            startSensorListeners();
            try {
                Thread.sleep(lengthOfRecord);
            } catch(InterruptedException e) {
                Log.d(TAG, "Recording is interrupted.");
            }
            stopSensorListeners();
        }

    }

    private void sendSensorData(String sensorName, int sensorType, long timeStamp, float[] values) {

        Log.d(TAG, "sendSensorData");

        //Notice: sensorType may not be used now, but may be useful for the following developement
        //by specifying different sensor types

        //String timeStamp_1 = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        //for debugging purpose
        System.out.println("INSIDE THE SENDING METHOD");

        mSensorData = PutDataMapRequest.create(Utils.SENSOR_DATA_PATH);
        mSensorData.getDataMap().putString(Utils.NAME, sensorName);
        //mSensorData.getDataMap().putInt(Utils.TYPE, sensorType);
        mSensorData.getDataMap().putLong(Utils.TIMESTAMP, timeStamp);
        //mSensorData.getDataMap().putString(Utils.TIMESTAMP, timeStamp_1);    //put in the absolute time
        mSensorData.getDataMap().putFloatArray(Utils.VALUES, values);

        PutDataRequest putDataRequest = mSensorData.asPutDataRequest();

        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataRequest);

    }

    private void startSensorListeners() {
        Log.d(TAG, "startSensorListeners");
        System.out.println("SUCCESSFULLY STARTED SENSOR LISTENER!");
        //This is how to get all the sensors
        //here we start with the linear accelerometer
        //List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);

        //notification on the watch
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle("coMotion");
        builder.setContentText("Collecting sensor data...");
        builder.setSmallIcon(R.drawable.ic_launcher);

        startForeground(1, builder.build());

        //initialization of sensors
        mSensor_LinearAcc = mSensorManager.getDefaultSensor(SENS_LINEAR_ACCELERATION);
        mSensor_Gyroscope = mSensorManager.getDefaultSensor(SENS_GYROSCOPE);
        mSensor_RotationVec = mSensorManager.getDefaultSensor(SENS_ROTATION_VECTOR);
        mSensor_MagneticField = mSensorManager.getDefaultSensor(SENS_MAGNETIC_FIELD);


        //float[] empty = new float[0];
        if(mSensorManager != null) {
            if(mSensor_LinearAcc != null) {
                mSensorManager.registerListener(this, mSensor_LinearAcc, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Log.d(TAG, "No Linear Acceleration Sensor found");
            }

            if(mSensor_Gyroscope != null) {
                mSensorManager.registerListener(this, mSensor_Gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Log.d(TAG, "No Gyroscope Sensor found");
            }

            if(mSensor_RotationVec != null) {
                mSensorManager.registerListener(this, mSensor_RotationVec, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Log.d(TAG, "No Rotation Vector Sensor found");
            }

            if(mSensor_MagneticField != null) {
                mSensorManager.registerListener(this, mSensor_MagneticField, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Log.d(TAG, "No Magnetic Field Sensor found");
            }


        }

        //similar to the above comments
        //the for loop will help when more than one sensors are added
        /*
        for (Sensor sensor : sensors) {
            sensorData.getDataMap().putFloatArray(sensor.getName(), empty);
            sensorData.getDataMap().putInt(sensor.getName() + " Accuracy", 0);
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        */

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSensorListeners();
    }

    private void stopSensorListeners() {
        Log.d(TAG, "stopSensorListeners");

        System.out.println("Received to stop");

        if(mSensorManager != null) {
            mSensorManager.unregisterListener(this);
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


}
