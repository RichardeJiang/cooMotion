package com.example.richardjiang.comotion.shareHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.richardjiang.comotion.R;
import com.example.richardjiang.comotion.activityMain.ApplicationHelper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Richard Jiang on 7/10/2015.
 */
public class UploadActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "UploadActivity";
    private GoogleApiClient mGoogleApiClient;
    private static final int REQUEST_CODE_CHOOSE_FILE = 1;
    private static final int REQUEST_CODE_CREATOR = 2;
    private static final int REQUEST_CODE_RESOLUTION = 3;

    private String[] mFileList;
    private File mPath;
    private File fileToSave;
    private String mChosenFile;
    private String FTYPE;

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient == null) {
            // Create the API client and bind it to an instance variable.
            // We use this instance as the callback for connection and connection
            // failures.
            // Since no account name is passed, the user is prompted to choose.
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        // Connect the client. Once connected, the camera is launched.
        mGoogleApiClient.connect();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_upload);

        if (mGoogleApiClient == null) {
            // Create the API client and bind it to an instance variable.
            // We use this instance as the callback for connection and connection
            // failures.
            // Since no account name is passed, the user is prompted to choose.
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        // Connect the client. Once connected, the camera is launched.
        mGoogleApiClient.connect();

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

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "API client connected.");
        if (fileToSave == null) {

            Button btnVideo = (Button) findViewById(R.id.btnVideo);
            btnVideo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPath = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES) + File.separator + "CoMotion");
                    FTYPE = ".mp4";

                    if(mPath.exists()) {
                        loadFileList();
                        showFileChoosingDialog();
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "No file to upload.",
                                Toast.LENGTH_LONG).show();
                    }

                }
            });

            Button btnWatch = (Button) findViewById(R.id.btnWatch);
            btnWatch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPath = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOCUMENTS) + File.separator + "CoMotion");
                    FTYPE = ".txt";

                    if(mPath.exists()) {
                        loadFileList();
                        showFileChoosingDialog();
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "No file to upload.",
                                Toast.LENGTH_LONG).show();
                    }
                    
                }
            });
        }
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
                    //mChosenFile = mPath + File.separator + mFileList[which];
                    mChosenFile = mFileList[which];
                    fileToSave = new File(mPath.getPath() + File.separator + mChosenFile);
                    //you can do stuff with the file here too
                    System.out.println("File chosen");
                    saveFileToDrive();

                }
            });

            alertDialog = builder.create();
            alertDialog.show();
        }

    }

    private void saveFileToDrive() {
        Log.d(TAG, "Inside the saving process");
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {

                    @Override
                    public void onResult(DriveApi.DriveContentsResult result) {
                        // If the operation was not successful, we cannot do anything
                        // and must fail.
                        if (!result.getStatus().isSuccess()) {
                            Log.i(TAG, "Failed to create new contents.");
                            return;
                        }
                        // Otherwise, we can write our data to the new contents.
                        Log.i(TAG, "New contents created.");
                        // Get an output stream for the contents.
                        OutputStream outputStream = result.getDriveContents().getOutputStream();

                        if (!fileToSave.exists()) {
                            Log.d(TAG, "file not found!");
                            return;
                        }

                        FileInputStream fis;
                        try {
                            fis = new FileInputStream(fileToSave.getPath());
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            byte[] buf = new byte[1024];
                            int n;
                            while (-1 != (n = fis.read(buf)))
                                baos.write(buf, 0, n);
                            byte[] photoBytes = baos.toByteArray();
                            outputStream.write(photoBytes);

                            outputStream.close();
                            outputStream = null;
                            fis.close();
                            fis = null;

                        } catch (FileNotFoundException e) {
                            Log.w(TAG, "FileNotFoundException: " + e.getMessage());
                        } catch (IOException e1) {
                            Log.w(TAG, "Unable to write file contents." + e1.getMessage());
                        }

                        String title = fileToSave.getName();

                        MetadataChangeSet metadataChangeSet;

                        if (FTYPE.equals(".mp4")) {
                            metadataChangeSet = new MetadataChangeSet.Builder()
                                    .setMimeType("video/mp4").setTitle(title).build();
                        } else { //if it is ".txt"
                            metadataChangeSet = new MetadataChangeSet.Builder()
                                    .setMimeType("text/plain").setTitle(title).build();
                        }

                        // Create an intent for the file chooser, and start it.
                        IntentSender intentSender = Drive.DriveApi
                                .newCreateFileActivityBuilder()
                                .setInitialMetadata(metadataChangeSet)
                                .setInitialDriveContents(result.getDriveContents())
                                .build(mGoogleApiClient);
                        try {
                            startIntentSenderForResult(
                                    intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(TAG, "Failed to launch file chooser.");
                        }

                        /*
                        this part is from the internet

                        if (mime.equals(MIME_PHOTO)) {
                            if (VERBOSE)
                                Log.i(TAG, "Creating new photo on Drive (" + title
                                        + ")");
                            Drive.DriveApi.getFolder(mDriveClient,
                                    mPicFolderDriveId).createFile(mDriveClient,
                                    metadataChangeSet,
                                    result.getDriveContents());
                        } else if (mime.equals(MIME_VIDEO)) {
                            Log.i(TAG, "Creating new video on Drive (" + title
                                    + ")");
                            Drive.DriveApi.getFolder(mDriveClient,
                                    mVidFolderDriveId).createFile(mDriveClient,
                                    metadataChangeSet,
                                    result.getDriveContents());
                        }
                        */

                    }

                });

    }

    @Override
    protected void onPause() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Called whenever the API client fails to connect.
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, 0).show();
            return;
        }
        // The failure has a resolution. Resolve it.
        // Called typically when the app is not yet authorized, and an
        // authorization dialog is displayed to the user.
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

}
