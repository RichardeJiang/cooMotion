package com.example.richardjiang.comotion.networkHandler.controller;

/**
 * Created by Richard Jiang on 6/27/2015.
 */
import com.example.richardjiang.comotion.networkHandler.model.Phone;
import com.example.richardjiang.comotion.activityMain.PauseResumeListener;
import android.app.Activity;

public class NetworkController implements PauseResumeListener {
    private Activity act;
    public boolean performConnectionDiscovery = true;

    private WiFiDirectBroadcastConnectionController mConnection;

    public NetworkController(Activity act){
        this.act = act;
        mConnection = WiFiDirectBroadcastConnectionController.getInstance();
    }

    @Override
    public void onResume(){
        if(performConnectionDiscovery)	act.registerReceiver(mConnection, mConnection.getIntentFilter());
    }

    @Override
    public void onPause(){
        if(performConnectionDiscovery)	act.unregisterReceiver(mConnection);
    }

    public void connectToPhone(final Phone p) {
        mConnection.connectToDevice(p.deviceInfo,  WiFiDirectBroadcastConnectionHelper.peerActionListener);
    }
}

