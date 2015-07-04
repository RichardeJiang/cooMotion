package com.example.richardjiang.comotion.cameraHandler;

import android.app.IntentService;
import android.content.Intent;
import android.media.MediaPlayer;
import android.util.Log;

import com.example.richardjiang.comotion.R;
import com.example.richardjiang.comotion.networkHandler.controller.WiFiDirectBroadcastConnectionController;

/**
 * Created by Richard Jiang on 7/3/2015.
 */

//Notice for the IntentService class: must have a default constructor
public class MusicFlagService extends IntentService {
    private static final String TAG = "MUSIC_FLAG";
    @Override
    public void onCreate() {
        super.onCreate();
    }

    public MusicFlagService() {
        super("MusicFlagService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(WiFiDirectBroadcastConnectionController.getInstance().getIsGroupOwner()) {

            try {
                Thread.sleep(3000);
            }catch(InterruptedException e) {
                Log.d(TAG, "Cannot go into the sleep part");
            }
            //final MediaPlayer mMediaPlayer = MediaPlayer.create(this, R.raw.start_recording);
            final MediaPlayer mMediaPlayer = MediaPlayer.create(this, R.raw.start_recording_1);
            mMediaPlayer.start();

        }
        else {
            return;
        }
    }
}
