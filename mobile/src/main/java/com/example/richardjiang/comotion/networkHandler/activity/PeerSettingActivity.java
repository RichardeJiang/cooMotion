package com.example.richardjiang.comotion.networkHandler.activity;

/**
 * Created by Richard Jiang on 6/27/2015.
 */
import com.example.richardjiang.comotion.R;
import com.example.richardjiang.comotion.activityMain.ApplicationHelper;
import com.example.richardjiang.comotion.networkHandler.controller.WiFiDirectBroadcastConnectionController;
import com.example.richardjiang.comotion.networkHandler.model.Phone;
import com.example.richardjiang.comotion.networkHandler.view.WifiListAdapter;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

//import com.example.sesame_social_gaming_project.R;

import com.example.richardjiang.comotion.networkHandler.NetworkActivityTemplate;

public class PeerSettingActivity extends NetworkActivityTemplate implements OnItemClickListener{
    private static final int ACTION_DISCONNECT = 1;
    private static final int ACTION_CONNECT = 2;
    private ListView lv;
    WifiListAdapter adapter;

    @Override
    protected boolean performConnectionDiscovery(){return true;}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peer_setting);

        lv = (ListView) findViewById(R.id.lvPhonesList);
        adapter = new WifiListAdapter(this);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);
        WiFiDirectBroadcastConnectionController.getInstance().discoverPeers();
    }


    //Notice that there is no listener for the button here
    //instead in the layout file an onClick is set for the button
    public void clickToRefreshPeerList(View v){
        WiFiDirectBroadcastConnectionController.getInstance().discoverPeers();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_peer_activity, menu);
        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        final Phone p = (Phone) adapter.getItem(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final int action;

        if(p.deviceInfo.status == WifiP2pDevice.CONNECTED){
            action = ACTION_DISCONNECT;
        } else{
            action = ACTION_CONNECT;
        }

        builder.setMessage(getActionStr(action, true) + " phone?")
                .setPositiveButton(getActionStr(action,false), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        switch(action){
                            case ACTION_CONNECT:
                                Log.v("DEBUG", "Going to connect the phone");
                                networkController.connectToPhone(p);
                                break;
                            case ACTION_DISCONNECT:
                                Log.v("DEBUG", "Going to disconnect the phone");
                                break;
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {}
                });
        builder.show();
    }

    private String getActionStr(int actionType, boolean isMessage){
        switch(actionType){
            case ACTION_CONNECT: return "Connect" + ((isMessage) ? " to" : "");
            case ACTION_DISCONNECT: return "Disconnect" + ((isMessage) ? " from" : "");
            default: return "OK";
        }
    }
}


