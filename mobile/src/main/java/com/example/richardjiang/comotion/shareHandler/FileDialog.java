package com.example.richardjiang.comotion.shareHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.richardjiang.comotion.R;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by Richard Jiang on 7/9/2015.
 */
public class FileDialog extends Activity {
    private static final String TAG = "FileDialog";
    private String[] mFileList;
    //private File mPath = new File(Environment.getExternalStorageDirectory() + "//yourdir//");
    private File mPath;
    private String mChosenFile;
    //private static final String FTYPE = ".txt";
    private static String FTYPE;
    private static final int DIALOG_LOAD_FILE = 1000;
    private static final int REQUEST_CODE_CHOOSE_FILE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_upload);

        Button btnVideo = (Button) findViewById(R.id.btnVideo);
        btnVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPath = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES) + File.separator + "CoMotion");
                FTYPE = ".mp4";

                loadFileList();
                showFileChoosingDialog();

            }
        });

        Button btnWatch = (Button) findViewById(R.id.btnWatch);
        btnWatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPath = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOCUMENTS) + File.separator + "CoMotion");
                FTYPE = ".txt";

                loadFileList();

                showFileChoosingDialog();

            }
        });

    }

    private void showFileChoosingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog alertDialog;

        builder.setTitle("Choose your file");

        if(mFileList == null) {
            Log.d(TAG, "no file list initialized");
            alertDialog = builder.create();
            return;
        }
        else {
            builder.setItems(mFileList, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    mChosenFile = mPath + File.separator + mFileList[which];
                    //you can do stuff with the file here too

                    System.out.println("File chosen");
                    Intent intent = new Intent();
                    intent.putExtra("MESSAGE", mChosenFile);
                    intent.putExtra("MIMEType", FTYPE);
                    setResult(REQUEST_CODE_CHOOSE_FILE, intent);
                    finish();
                }
            });

            alertDialog = builder.create();
            alertDialog.show();
        }

    }

    private void loadFileList() {
        try {
            mPath.mkdirs();
        }
        catch(SecurityException e) {
            Log.e(TAG, "unable to write on the sd card " + e.toString());
        }
        if(mPath.exists()) {
            FilenameFilter filter = new FilenameFilter() {

                @Override
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    return filename.contains(FTYPE) || sel.isDirectory();
                }

            };
            mFileList = mPath.list(filter);
        }
        else {
            mFileList= new String[0];
        }
    }

}
