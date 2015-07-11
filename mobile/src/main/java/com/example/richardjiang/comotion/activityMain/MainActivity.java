package com.example.richardjiang.comotion.activityMain;

/**
 * Created by Richard Jiang on 6/27/2015.
 */
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.support.v7.widget.Toolbar;

import com.example.richardjiang.comotion.R;
import com.example.richardjiang.comotion.cameraHandler.CameraActivity;
import com.example.richardjiang.comotion.networkHandler.NetworkActivityTemplate;
import com.example.richardjiang.comotion.networkHandler.NetworkService;
import com.example.richardjiang.comotion.networkHandler.activity.PeerSettingActivity;
import com.example.richardjiang.comotion.networkHandler.controller.WiFiDirectBroadcastConnectionController;
import com.example.richardjiang.comotion.networkHandler.impl.InternalMessage;
import com.example.richardjiang.comotion.networkHandler.impl.NetworkMessageObject;
import com.example.richardjiang.comotion.remoteSensorHandler.DataStorageService;
import com.example.richardjiang.comotion.remoteSensorHandler.Utils;
import com.example.richardjiang.comotion.remoteSensorHandler.WearPatternActivity;
import com.example.richardjiang.comotion.remoteSensorHandler.WearableMessageService;
import com.example.richardjiang.comotion.shareHandler.UploadActivity;


public class MainActivity extends NetworkActivityTemplate {

    private ActivityStatus activityStatus = ActivityStatus.NONE;
    private String debugTag = "MAIN_ACTIVITY";
    private Toolbar mToolbar;

    //IMPORTANT
    //For every class using network override this method
    //override this method to start discovery peers automatically
    @Override
    protected boolean performConnectionDiscovery() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        //IMPORTANT: this is how to set up the toolbar icon and title
        //when there is no actionbar extended
        //mToolbar.setTitle("OK");

        final Context context = this;
        Button btnFindPeer = (Button) findViewById(R.id.btnPeerSettings);
        btnFindPeer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ApplicationHelper.getActivityInstance(), PeerSettingActivity.class);
                startActivity(intent);
            }
        });

        Button btnHelp = (Button) findViewById(R.id.btnHelp);
        btnHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                new AlertDialog.Builder(context)
                        //.setMessage(R.string.intro_message)
                        .setMessage(R.string.help_message)
                        .setPositiveButton(R.string.got_it, null)
                        .show();
                        */

                //for testing purpose
                try{
                    byte[] targetIP = com.example.richardjiang.comotion.networkHandler.Utils.getBytesFromIp("255.255.255.255");
                    byte[] myIP = WiFiDirectBroadcastConnectionController.getNetworkService().getMyIp();
                    String messageToSend = "test whether the handler is working hahahaha";
                    WiFiDirectBroadcastConnectionController.getNetworkService().sendMessage(
                            new NetworkMessageObject(
                                    messageToSend.getBytes(),
                                    InternalMessage.testNetworkHandler,
                                    myIP,
                                    targetIP));
                    ApplicationHelper.showToastMessage("I send " + messageToSend);
                }catch(Exception e){
                    ApplicationHelper.showToastMessage("Failed to send: test network handler");
                }
            }
        });

        Button btnStartCapturing = (Button) findViewById(R.id.btnStartCapturing);
        btnStartCapturing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ApplicationHelper.getActivityInstance(), CameraActivity.class);
                startActivity(intent);
            }
        });

        Button btnStartWatch = (Button) findViewById(R.id.btnStartWatch);
        btnStartWatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //android wear storage service to start
                Intent intent_wear_storage = new Intent(ApplicationHelper.getActivityInstance(), DataStorageService.class);
                startService(intent_wear_storage);

                Intent intent = new Intent(ApplicationHelper.getActivityInstance(), WearableMessageService.class);
                intent.putExtra(Utils.STORE_COMMAND, Utils.START_MEASUREMENT);
                startService(intent);
            }
        });

        Button btnStopWatch = (Button) findViewById(R.id.btnStopWatch);
        btnStopWatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ApplicationHelper.getActivityInstance(), WearableMessageService.class);
                intent.putExtra(Utils.STORE_COMMAND, Utils.STOP_MEASUREMENT);
                startService(intent);

                //android wear storage service to stop
                Intent intent_wear_storage_stop = new Intent(ApplicationHelper.getActivityInstance(), DataStorageService.class);
                stopService(intent_wear_storage_stop);
            }
        });

        Button btnPatternWatch = (Button) findViewById(R.id.btnPatternWatch);
        btnPatternWatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ApplicationHelper.getActivityInstance(), WearPatternActivity.class);
                startActivity(intent);
            }
        });

        Button btnShare = (Button) findViewById(R.id.btnShare);
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ApplicationHelper.getActivityInstance(), UploadActivity.class);
                startActivity(intent);
            }
        });

        WiFiDirectBroadcastConnectionController.getInstance().discoverPeers();
        NetworkService.registerMessageHandler(internalMessageListener);
    }

    private NetworkService.MessageHandleListener internalMessageListener = new NetworkService.MessageHandleListener() {

        /**/
        @Override
        public boolean handleMessage(NetworkMessageObject message) {
            String messageContent = "";
            messageContent = InternalMessage.getMessageString(message);
            System.out.println(message.getSourceIP() + " says: " + messageContent);

            switch(message.code){

                case InternalMessage.testNetworkHandler: {
                    ApplicationHelper.showToastMessage(message.getSourceIP() + " sends to "
                            + com.example.richardjiang.comotion.networkHandler.Utils.getIpAddressAsString(message.getTargetIP())
                            + " and says "
                            + messageContent);
                }
            }
            return false;
        }
        /**/

        /*
        @Override
        public boolean handleMessage(NetworkMessageObject message) {
            if(mIsRecordingVideo){

                ApplicationHelper.showToastMessage("Received to stop");
                stopRecordingVideo();
                return true;
            } else{

                ApplicationHelper.showToastMessage("Received to start");
                startRecordingVideo();
                return true;
            }

        }
        */
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
