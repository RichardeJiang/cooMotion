package com.example.richardjiang.comotion.cameraHandler;

/**
 * Created by Richard Jiang on 6/27/2015.
 */
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Network;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

//wearable
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.NotificationCompat.WearableExtender;

import com.example.richardjiang.comotion.R;

import com.example.richardjiang.comotion.activityMain.ApplicationHelper;
import com.example.richardjiang.comotion.activityMain.PauseResumeListener;
import com.example.richardjiang.comotion.networkHandler.NetworkService;
import com.example.richardjiang.comotion.networkHandler.NetworkService.MessageHandleListener;
import com.example.richardjiang.comotion.networkHandler.NetworkActivityTemplate;
import com.example.richardjiang.comotion.networkHandler.Utils;
import com.example.richardjiang.comotion.networkHandler.controller.NetworkController;
import com.example.richardjiang.comotion.networkHandler.controller.WiFiDirectBroadcastConnectionController;
import com.example.richardjiang.comotion.networkHandler.impl.InternalMessage;
import com.example.richardjiang.comotion.networkHandler.impl.NetworkMessageObject;

import com.example.richardjiang.comotion.cameraHandler.CameraActivity;
import com.example.richardjiang.comotion.remoteSensorHandler.DataStorageService;
import com.example.richardjiang.comotion.remoteSensorHandler.WearableMessageService;


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CameraFragment extends Fragment implements View.OnClickListener {

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    private static final String TAG = "Camera2VideoFragment";

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }


    /**
     * An indicator to indicate which mode the camera is in (single:1 or group:2)
     */
    private int captureMode;

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;

    /**
     * Button to record video
     */
    private Button mButtonVideo;

    //Button to start group recording
    private Button mGroupVideo;

    //help messages for the image button
    private static String CAMERA_HELP_MESSAGES = "Record Single will record videos only on this device. " + "\n" +
            "Record Group will broadcast \"record video\" message to all connected devices.";

    /**
     * A refernce to the opened {@link android.hardware.camera2.CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * A reference to the current {@link android.hardware.camera2.CameraCaptureSession} for preview.
     */
    private CameraCaptureSession mPreviewSession;

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
                                              int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
                                                int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }

    };

    /**
     * The {@link android.util.Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * The {@link android.util.Size} of video recording.
     */
    private Size mVideoSize;

    /**
     * Camera preview.
     */
    private CaptureRequest.Builder mPreviewBuilder;

    /**
     * MediaRecorder
     */
    private MediaRecorder mMediaRecorder;

    /**
     * Whether the app is recording video now
     */
    private boolean mIsRecordingVideo;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its status.
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            startPreview();
            mCameraOpenCloseLock.release();
            if (null != mTextureView) {
                configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
            }
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };

    public static CameraFragment newInstance() {
        CameraFragment fragment = new CameraFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    /**
     * In this sample, we choose a video size with 3x4 aspect ratio. Also, we don't use sizes larger
     * than 1080p, since MediaRecorder cannot handle such a high-resolution video.
     *
     * @param choices The list of available sizes
     * @return The video size
     */
    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        Log.e(TAG, "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<Size>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    private NetworkService.MessageHandleListener internalMessageListener = new NetworkService.MessageHandleListener() {

        /*
        @Override
        public boolean handleMessage(NetworkMessageObject message) {
            String messageContent = "";
            messageContent = InternalMessage.getMessageString(message);
            System.out.println(message.getSourceIP() + " says: " + messageContent);

            switch(message.code){
                case InternalMessage.startNow: {
                    ApplicationHelper.showToastMessage(message.getSourceIP() + " send to "
                            + Utils.getIpAddressAsString(message.getTargetIP())
                            + " and says "
                            + messageContent);
                    if (mIsRecordingVideo) {
                        return false;
                    } else {
                        startRecordingVideo();
                        return true;
                    }
                }

                case InternalMessage.stopNow: {
                    ApplicationHelper.showToastMessage(message.getSourceIP() + " send to "
                        + Utils.getIpAddressAsString(message.getTargetIP())
                        + " and says "
                        +messageContent);
                    if (mIsRecordingVideo) {
                        stopRecordingVideo();
                        return true;
                    } else {
                        return false;
                    }
                }
            }
            return false;
        }
        */

        @Override
        public boolean handleMessage(NetworkMessageObject message) {
            if(mIsRecordingVideo){

                ApplicationHelper.showToastMessage("Received to stop");
                stopRecordingVideo();
                //return true;
            } else{

                ApplicationHelper.showToastMessage("Received to start");
                captureMode = 2;
                startRecordingVideo();
                //return true;
            }

            //newly added return statement
            return false;

        }
    };

    //for declaration of the network part
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WiFiDirectBroadcastConnectionController.getInstance().discoverPeers();

        NetworkService.registerMessageHandler(internalMessageListener);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        mButtonVideo = (Button) view.findViewById(R.id.video);
        mButtonVideo.setOnClickListener(this);

        mGroupVideo = (Button) view.findViewById(R.id.group);
        mGroupVideo.setOnClickListener(this);

        view.findViewById(R.id.info).setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.video: {
                if (mIsRecordingVideo) {

                    //android wear command to stop
                    Intent intent = new Intent(ApplicationHelper.getActivityInstance(), WearableMessageService.class);
                    intent.putExtra(com.example.richardjiang.comotion.remoteSensorHandler.Utils.STORE_COMMAND, com.example.richardjiang.comotion.remoteSensorHandler.Utils.STOP_MEASUREMENT);
                    getActivity().startService(intent);

                    stopRecordingVideo();
                } else {
                    captureMode = 1;

                    //android wear command to start
                    Intent intent = new Intent(ApplicationHelper.getActivityInstance(), WearableMessageService.class);
                    intent.putExtra(com.example.richardjiang.comotion.remoteSensorHandler.Utils.STORE_COMMAND, com.example.richardjiang.comotion.remoteSensorHandler.Utils.START_MEASUREMENT);
                    getActivity().startService(intent);

                    startRecordingVideo();

                }
                break;
            }

            case R.id.group: {
                if (!mIsRecordingVideo) {
                    try {
                        byte[] targetIP = Utils.getBytesFromIp("255.255.255.255");
                        byte[] myIP = WiFiDirectBroadcastConnectionController.getNetworkService().getMyIp();
                        String messageToSend = "startNow";
                        WiFiDirectBroadcastConnectionController.getNetworkService().sendMessage(
                                new NetworkMessageObject(
                                        messageToSend.getBytes(),
                                        InternalMessage.startNow,
                                        myIP,
                                        targetIP));
                        ApplicationHelper.showToastMessage("I send " + messageToSend + " from " + myIP.toString());
                        captureMode = 2;

                        //android wear storage service to start
                        Intent intent_wear_storage = new Intent(ApplicationHelper.getActivityInstance(), DataStorageService.class);
                        getActivity().startService(intent_wear_storage);

                        //android wear command to start
                        Intent intent = new Intent(ApplicationHelper.getActivityInstance(), WearableMessageService.class);
                        intent.putExtra(com.example.richardjiang.comotion.remoteSensorHandler.Utils.STORE_COMMAND, com.example.richardjiang.comotion.remoteSensorHandler.Utils.START_MEASUREMENT);
                        getActivity().startService(intent);

                        //background music play service command to start
                        Intent intent_music_flag = new Intent(ApplicationHelper.getActivityInstance(), MusicFlagService.class);
                        getActivity().startService(intent_music_flag);

                        //IMPORATNT: PUT STARTRECORDINGVIDEO AT THE END; OTHERWISE SOME CODE WON'T BE EXECUTED UNTIL RECORDING IS FINISHED
                        //SINCE WE ONLY HAVE ONE THREAD HERE
                        startRecordingVideo();

                    } catch (Exception e) {
                        ApplicationHelper.showToastMessage("Failed to send: startNow");
                    }
                } else {
                    try {
                        byte[] targetIP = Utils.getBytesFromIp("255.255.255.255");
                        byte[] myIP = WiFiDirectBroadcastConnectionController.getNetworkService().getMyIp();
                        String messageToSend = "stopNow";
                        WiFiDirectBroadcastConnectionController.getNetworkService().sendMessage(
                                new NetworkMessageObject(
                                        messageToSend.getBytes(),
                                        InternalMessage.stopNow,
                                        myIP,
                                        targetIP));
                        ApplicationHelper.showToastMessage("I send " + messageToSend + " from " + myIP.toString());

                        //android wear command to stop
                        Intent intent = new Intent(ApplicationHelper.getActivityInstance(), WearableMessageService.class);
                        intent.putExtra(com.example.richardjiang.comotion.remoteSensorHandler.Utils.STORE_COMMAND, com.example.richardjiang.comotion.remoteSensorHandler.Utils.STOP_MEASUREMENT);
                        getActivity().startService(intent);

                        //android wear storage service to stop
                        Intent intent_wear_storage_stop = new Intent(ApplicationHelper.getActivityInstance(), DataStorageService.class);
                        getActivity().stopService(intent_wear_storage_stop);

                        stopRecordingVideo();
                    } catch (Exception e) {
                        ApplicationHelper.showToastMessage("Failed to send: stopNow");
                    }
                }
                break;
            }


            case R.id.info: {
                Activity activity = getActivity();
                if (null != activity) {
                    new AlertDialog.Builder(activity)
                            //.setMessage(R.string.intro_message)
                            //.setMessage("@string/camera_help")
                            .setMessage(CAMERA_HELP_MESSAGES)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
                break;
            }

        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tries to open a {@link CameraDevice}. The result is listened by `mStateCallback`.
     */
    private void openCamera(int width, int height) {
        final Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            String cameraId = manager.getCameraIdList()[0];

            // Choose the sizes for camera preview and video recording
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    width, height, mVideoSize);

            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }
            configureTransform(width, height);
            mMediaRecorder = new MediaRecorder();
            manager.openCamera(cameraId, mStateCallback, null);
        } catch (CameraAccessException e) {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            activity.finish();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            new ErrorDialog().show(getFragmentManager(), "dialog");
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mMediaRecorder) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Start the camera preview.
     */
    private void startPreview() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            setUpMediaRecorder();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<Surface>();

            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);

            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewBuilder.addTarget(recorderSurface);

            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Activity activity = getActivity();
                    if (null != activity) {
                        Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
     */
    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should not to be called until the camera preview size is determined in
     * openCamera, or until the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    private void setUpMediaRecorder() throws IOException {
        final Activity activity = getActivity();
        if (null == activity) {
            return;
        }
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(getVideoFile(activity).getAbsolutePath());
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int orientation = ORIENTATIONS.get(rotation);
        mMediaRecorder.setOrientationHint(orientation);
        mMediaRecorder.prepare();
    }

    private File getVideoFile(Context context) {

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "coMotion");

        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("CREATION OF NEW DIR", "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        File videoFile;

        videoFile = new File(mediaStorageDir.getPath() + File.separator +
                "VID_"+ timeStamp + ".mp4");
        return videoFile;

        //return new File(context.getExternalFilesDir(null), "video.mp4");
    }



    private void startRecordingVideo() {

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                try {

                    //IMPORTANT: THIS IS RUNNING IN BACKGROUND
                    //SO CANNOT TOUCH UI COMPONENTS
                    //THUS RUNONUITHREAD IS USED
                    // UI

                    //TODO: maybe disable one when the other is pressed? Using animation?
                    if(captureMode == 1) {
                        mButtonVideo.setText(R.string.stop);
                    } else if(captureMode == 2) {
                        mGroupVideo.setText(R.string.stop);
                    }

                    mIsRecordingVideo = true;

                    //Notification trial part
                    /*
                    int notificationID = 001;
                    NotificationCompat.Builder notificationBuilder =
                            new NotificationCompat.Builder(ApplicationHelper.getActivityInstance())
                                    .setSmallIcon(R.drawable.ic_notification)
                                    .setContentTitle("coMotion")
                                    .setContentText("Recording video...");

                    NotificationManagerCompat notificationManager =
                            NotificationManagerCompat.from(ApplicationHelper.getActivityInstance());

                    notificationManager.notify(notificationID,notificationBuilder.build());
                    */

                    // Start recording
                    mMediaRecorder.start();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }

            }
        });

    }

    private void stopRecordingVideo() {

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                //IMPORTANT: THIS IS RUNNING IN BACKGROUND
                //SO CANNOT TOUCH UI COMPONENTS
                // UI
                mIsRecordingVideo = false;

                if(captureMode == 1) {
                    mButtonVideo.setText(R.string.record);
                } else if(captureMode == 2){
                    mGroupVideo.setText(R.string.group_record);
                }

                // Stop recording
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                Activity activity = getActivity();
                if (null != activity) {
                    Toast.makeText(activity, "Video saved: " + getVideoFile(activity),
                            Toast.LENGTH_SHORT).show();
                }
                startPreview();

            }
        });

    }


    // TODO: swipe to choose single or group capture
    /*
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }
    */

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    public static class ErrorDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage("This device doesn't support Camera2 API.")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

}


