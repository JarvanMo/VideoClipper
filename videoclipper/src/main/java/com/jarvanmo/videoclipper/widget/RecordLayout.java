package com.jarvanmo.videoclipper.widget;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.jarvanmo.ffmpeg.DeviceUtils;
import com.jarvanmo.ffmpeg.Log;
import com.jarvanmo.ffmpeg.MediaRecorderBase;
import com.jarvanmo.ffmpeg.MediaRecorderNative;
import com.jarvanmo.ffmpeg.RecordProgressView;
import com.jarvanmo.ffmpeg.model.MediaObject;
import com.jarvanmo.videoclipper.R;

import java.io.File;
import java.lang.ref.WeakReference;

public class RecordLayout extends FrameLayout implements
        MediaRecorderBase.OnErrorListener, MediaRecorderBase.OnPreparedListener,
        MediaRecorderBase.OnEncodeListener {

    /**
     * 刷新进度条
     */
    public static final int HANDLE_INVALIDATE_PROGRESS = 0;
    /**
     * 延迟拍摄停止
     */
    public static final int HANDLE_STOP_RECORD = 1;


    private int minRecordTime = 3 * 1000;
    private int maxRecordTime = 15 * 1000;


    private CheckBox isFrontCamera;
    private ImageButton switchFlash;
    private View next;


    private TextView pressToRecord;
    private SurfaceView surfaceView;


    /**
     * SDK视频录制对象
     */
    private MediaRecorderBase mMediaRecorder;

    private MediaObject mMediaObject;

    private RecordProgressView recordProgress;


    private boolean startState;
    private volatile boolean mPressedStatus;

    private Handler handler = new RecordHandler(this);


    private View.OnTouchListener pressToRecordOnTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (mMediaRecorder == null) {
                return false;
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 检测是否手动对焦
                    // 判断是否已经超时
                    if (mMediaObject.getDuration() >= maxRecordTime) {
                        return true;
                    }

                    // 取消回删
                    if (cancelDelete())
                        return true;
                    if (!startState) {
                        startState = true;
                        startRecord();
                    } else {
                        mMediaObject.buildMediaPart(mMediaRecorder.mCameraId);
                        recordProgress.setData(mMediaObject);
                        setStartUI();
                        mMediaRecorder.setRecordState(true);
                    }

                    break;

                case MotionEvent.ACTION_UP:

                    mMediaRecorder.setRecordState(false);
                    if (mMediaObject.getDuration() >= maxRecordTime) {
                        next.performClick();
                    } else {
                        mMediaRecorder.setStopDate();
                        setStopUI();
                    }


                    // 暂停
/*                    if (mPressedStatus) {

                        // 检测是否已经完成
                        if (mMediaObject.getDuration() >= RECORD_TIME_MAX) {
                            mTitleNext.performClick();
                        }
                    }*/
                    break;
            }
            return true;
        }

    };


    public RecordLayout(@NonNull Context context) {
        this(context, null);
    }

    public RecordLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecordLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        findViews(context);
        setupPressToRecord();
        setupRecordPreview();
        setupCameraController();
        setupRecordProgress();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public RecordLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        findViews(context);
        setupPressToRecord();
        setupRecordPreview();
        setupCameraController();
        setupRecordProgress();
    }


    private void findViews(Context context) {
        LayoutInflater.from(context).inflate(R.layout.layout_record, this, true);
        isFrontCamera = findViewById(R.id.isFrontCamera);
        pressToRecord = findViewById(R.id.pressToRecord);
        surfaceView = findViewById(R.id.recordPreview);
        recordProgress = findViewById(R.id.recordProgress);
        switchFlash = findViewById(R.id.switchFlash);
        next = findViewById(R.id.next);
    }


    @SuppressLint("ClickableViewAccessibility")
    private void setupPressToRecord() {
        pressToRecord.setOnTouchListener(pressToRecordOnTouchListener);
    }

    private void setupRecordPreview() {

    }

    private void setupCameraController() {
        if (MediaRecorderBase.isSupportFrontCamera()) {
            isFrontCamera.setOnCheckedChangeListener((buttonView, isChecked) -> {
//                if (mRecordLed.isChecked()) {
//                    if (mMediaRecorder != null) {
//                        mMediaRecorder.toggleFlashMode();
//                    }
//                    mRecordLed.setChecked(false);
//                }

                if (mMediaRecorder != null) {
                    mMediaRecorder.switchCamera();
                    switchFlash.setEnabled(!mMediaRecorder.isFrontCamera());
                }


            });
        } else {
            isFrontCamera.setVisibility(View.GONE);
        }

        // 是否支持闪光灯
        if (DeviceUtils.isSupportCameraLedFlash(getContext().getPackageManager())) {
            switchFlash.setOnClickListener(v -> switchFlash());
        } else {
            switchFlash.setVisibility(View.GONE);
        }

    }

    private void setupRecordProgress() {
        recordProgress.setMaxDuration(maxRecordTime);
        recordProgress.setMinTime(minRecordTime);
    }

    /**
     * 初始化拍摄SDK
     */
    private void setupMediaRecorder() {
        mMediaRecorder = new MediaRecorderNative();

        mMediaRecorder.setOnErrorListener(this);
        mMediaRecorder.setOnEncodeListener(this);
        mMediaRecorder.setOnPreparedListener(this);

//        File f = new File(JianXiCamera.getVideoCachePath());
//        if (!FileUtils.checkFile(f)) {
//            f.mkdirs();
//        }

        File file = getContext().getExternalCacheDir();
        if (file == null) {
            return;
        }

        File videoCache = new File(file, "videoCache");
        if (!videoCache.exists()) {
            if (!videoCache.mkdirs()) {
                return;
            }

        }


        String key = String.valueOf(System.currentTimeMillis());
        mMediaObject = mMediaRecorder.setOutputDirectory(key,
                videoCache.getAbsolutePath() + key);
        mMediaRecorder.setSurfaceHolder(surfaceView.getHolder());
        mMediaRecorder.autoFocus((success, camera) -> {

        });
        mMediaRecorder.prepare();
    }


    private void switchFlash() {

        int level = switchFlash.getDrawable().getLevel() + 1;
        if (level > 2) {
            level = 0;
        }


        switchFlash.setImageLevel(level);
        String flashMode = Camera.Parameters.FLASH_MODE_AUTO;
        if (level == 1) {
            flashMode = Camera.Parameters.FLASH_MODE_TORCH;
        } else if (level == 2) {
            flashMode = Camera.Parameters.FLASH_MODE_OFF;
        }

        mMediaRecorder.setFlashMode(flashMode);
    }

    /**
     * 取消回删
     */
    private boolean cancelDelete() {
        if (mMediaObject != null) {
            MediaObject.MediaPart part = mMediaObject.getCurrentPart();
            if (part != null && part.remove) {
                part.remove = false;
                if (recordProgress != null)
                    recordProgress.invalidate();

                return true;
            }
        }
        return false;
    }


    private void startRecord() {
        if (mMediaRecorder != null) {

            MediaObject.MediaPart part = mMediaRecorder.startRecord();
            if (part == null) {
                return;
            }

            recordProgress.setData(mMediaObject);
        }

        setStartUI();
    }


    private void setStartUI() {
        mPressedStatus = true;
        pressToRecord.animate()
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(500)
                .start();


        if (handler != null) {
            handler.removeMessages(HANDLE_INVALIDATE_PROGRESS);
            handler.sendEmptyMessage(HANDLE_INVALIDATE_PROGRESS);

            handler.removeMessages(HANDLE_STOP_RECORD);
            handler.sendEmptyMessageDelayed(HANDLE_STOP_RECORD,
                    maxRecordTime - mMediaObject.getDuration());
        }
//        mRecordDelete.setVisibility(View.GONE);
        isFrontCamera.setEnabled(false);
        switchFlash.setEnabled(false);
    }

    private void setStopUI() {
        mPressedStatus = false;
        pressToRecord.animate().scaleX(1).scaleY(1).setDuration(500).start();


//        mRecordDelete.setVisibility(View.VISIBLE);
        isFrontCamera.setEnabled(true);
        switchFlash.setEnabled(true);

        handler.removeMessages(HANDLE_STOP_RECORD);
        checkViewsVisibility();
    }

    /**
     * 停止录制
     */
    private void stopRecord() {
        if (mMediaRecorder != null) {
            mMediaRecorder.stopRecord();
        }
        setStopUI();
    }


    private void checkViewsVisibility() {
        int duration = 0;

        Context context = getContext();
        if (!(context instanceof Activity)) {
            return;
        }

        if (!((Activity) context).isFinishing() && mMediaObject != null) {
            duration = mMediaObject.getDuration();
            if (duration < minRecordTime) {
                if (duration == 0) {
//                    isFrontCamera.setVisibility(View.VISIBLE);
                } else {
//                    isFrontCamera.setVisibility(View.GONE);
                }
                // 视频必须大于3秒
//                if (next.getVisibility() != View.INVISIBLE) {
//                    next.setVisibility(View.INVISIBLE);
//                }
                next.setEnabled(false);
            } else {
                // 下一步
//                if (next.getVisibility() != View.VISIBLE) {
//                    next.setVisibility(View.VISIBLE);
//                }
                next.setEnabled(true);
            }
        }
    }

    public void onResume() {
        if (mMediaRecorder == null) {
            setupMediaRecorder();
        } else {
//            mRecordLed.setChecked(false);
            mMediaRecorder.prepare();
            recordProgress.setData(mMediaObject);
        }
    }

    public void onDestroy() {
        mMediaRecorder.release();
    }

    public void onStop() {
        if (mMediaRecorder instanceof MediaRecorderNative) {
            ((MediaRecorderNative) mMediaRecorder).activityStop();
        }
//        hideProgress();
//        mProgressDialog = null;

    }


    @Override
    public void onPrepared() {
    }

    @Override
    public void onVideoError(int what, int extra) {
        Log.e("---", "-onVideoError");
    }

    @Override
    public void onAudioError(int what, String message) {
        Log.e("---", "-onAudioError");
    }

    @Override
    public void onEncodeStart() {
        Log.e("---", "-start");
    }

    @Override
    public void onEncodeProgress(int progress) {
        Log.e("---", "-onEncodeProgress");
    }

    @Override
    public void onEncodeComplete() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String path = mMediaObject.getOutputTempTranscodingVideoPath();//该路径可以自定义
        File file = new File(path);
        Uri uri = Uri.fromFile(file);
        intent.setDataAndType(uri, "video/*");
        getContext().startActivity(intent);
    }

    @Override
    public void onEncodeError() {
    }


    private static class RecordHandler extends Handler {
        private WeakReference<RecordLayout> weakReference;

        RecordHandler(RecordLayout recordLayout) {

            weakReference = new WeakReference<>(recordLayout);

        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLE_INVALIDATE_PROGRESS:

                    Context context = weakReference.get().getContext();

                    if (!(context instanceof Activity)) {
                        return;
                    }

                    if (weakReference.get().pressToRecord != null && !((Activity) context).isFinishing()) {
//                        && !isFinishing()
                        if (weakReference.get().mMediaObject != null
                                && weakReference.get().mMediaObject.getMedaParts() != null
                                && weakReference.get().mMediaObject.getDuration() >= weakReference.get().maxRecordTime) {

                            weakReference.get().next.performClick();
                            weakReference.get().stopRecord();
                            return;
                        }
                        if (weakReference.get().recordProgress != null)
                            weakReference.get().recordProgress.invalidate();
                        // if (mPressedStatus)
                        // titleText.setText(String.format("%.1f",
                        // mMediaRecorder.getDuration() / 1000F));
                        if (weakReference.get().mPressedStatus) {
                            sendEmptyMessageDelayed(HANDLE_INVALIDATE_PROGRESS, 30);
                        }
                    }
                    break;
            }
        }
    }
}
