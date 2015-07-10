package com.example.richardjiang.comotion.shareHandler;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;

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
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Richard Jiang on 7/9/2015.
 */
public class ShareActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "ShareActivity";
    private GoogleApiClient mGoogleApiClient;
    private static final int REQUEST_CODE_CHOOSE_FILE = 1;
    private static final int REQUEST_CODE_CREATOR = 2;
    private static final int REQUEST_CODE_RESOLUTION = 3;

    //here nameOfFileToSave is actually the directory of the file to be saved
    //it is given by FileDialog.class, containing the full directory of the file
    private String nameOfFileToSave = null;
    private String MIME = null;

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
    protected void onPause() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
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
        // authorization
        // dialog is displayed to the user.
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "API client connected.");
        if (nameOfFileToSave == null) {
            // This activity has no UI of its own. Just start the camera.
            /*
            startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                    REQUEST_CODE_CHOOSE_FILE);
                    */
            Intent intentToPickFile = new Intent(ApplicationHelper.getActivityInstance(), FileDialog.class);
            startActivityForResult(intentToPickFile, REQUEST_CODE_CHOOSE_FILE);
            return;
        }
        saveFileToDrive();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_CHOOSE_FILE:
                if (resultCode == Activity.RESULT_OK) {
                    nameOfFileToSave = data.getStringExtra("MESSAGE");
                    MIME = data.getStringExtra("MIMEType");
                }
                break;
            case REQUEST_CODE_CREATOR:
                // Called after a file is saved to Drive.
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "File successfully saved.");

                    nameOfFileToSave = null;
                    MIME = null;
                }
                break;
        }
    }

    private void saveFileToDrive() {
        Log.d(TAG, "Inside the saving process");
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {

                    @Override
                    public void onResult(DriveApi.DriveContentsResult result) {
                        // If the operation was not successful, we cannot do anything
                        // and must
                        // fail.
                        if (!result.getStatus().isSuccess()) {
                            Log.i(TAG, "Failed to create new contents.");
                            return;
                        }
                        // Otherwise, we can write our data to the new contents.
                        Log.i(TAG, "New contents created.");
                        // Get an output stream for the contents.
                        OutputStream outputStream = result.getDriveContents().getOutputStream();

                        File fileToSave = new File(nameOfFileToSave);

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
                        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                                .setMimeType("video/mp4").setTitle(title).build();

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

}
