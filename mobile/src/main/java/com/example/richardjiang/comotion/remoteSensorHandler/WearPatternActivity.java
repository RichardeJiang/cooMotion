package com.example.richardjiang.comotion.remoteSensorHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;

import com.example.richardjiang.comotion.R;
import com.example.richardjiang.comotion.activityMain.ApplicationHelper;

/**
 * Created by Richard Jiang on 6/29/2015.
 */
public class WearPatternActivity extends Activity implements AdapterView.OnItemSelectedListener {

    private static final String TAG = "WearPatternActivity";
    private Toolbar mToolbar;

    private Spinner spnLength,spnDelay;
    private Button btnPatternRecord;

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear_pattern);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        //IMPORTANT: this is how to set up the toolbar icon and title
        //when there is no actionbar extended
        //mToolbar.setTitle("OK");

        final Context context = this;

        addListenerOnSpinnerItemSelection();
        addListenerOnButton();

    }



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

    private void addListenerOnSpinnerItemSelection() {
        spnLength = (Spinner) findViewById(R.id.edit_length_spinner);
        spnLength.setOnItemSelectedListener(this);

        spnDelay = (Spinner) findViewById(R.id.edit_delay_spinner);
        spnDelay.setOnItemSelectedListener(this);
    }

    private void addListenerOnButton() {
        spnDelay = (Spinner) findViewById(R.id.edit_delay_spinner);
        spnLength = (Spinner) findViewById(R.id.edit_length_spinner);

        btnPatternRecord = (Button) findViewById(R.id.btnPatternWatch);
        btnPatternRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String delay = String.valueOf(spnDelay.getSelectedItem());
                String length = String.valueOf(spnLength.getSelectedItem());
                Intent intent = new Intent(ApplicationHelper.getActivityInstance(), WearableMessageService.class);
                //intent.putExtra(Utils.STORE_COMMAND, Utils.START_PATTERN + "," + delay + "," + length);
                intent.putExtra(Utils.STORE_COMMAND, Utils.START_PATTERN);
                intent.putExtra(Utils.DELAY, delay);
                intent.putExtra(Utils.LENGTH, length);

                startService(intent);
            }
        });
    }
}
