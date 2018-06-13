package com.jarvanmo.videoclipper.widget;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.jarvanmo.ffmpeg.DeviceUtils;
import com.jarvanmo.ffmpeg.MediaRecorderBase;
import com.jarvanmo.ffmpeg.MediaRecorderNative;
import com.jarvanmo.ffmpeg.RecordProgressView;
import com.jarvanmo.ffmpeg.model.MediaObject;
import com.jarvanmo.videoclipper.R;

import java.io.File;
import java.lang.ref.WeakReference;

public class RecordLayout extends FrameLayout implements
        MediaRecorderBase.OnErrorListener, MediaRecorderBase.OnPreparedListener,
        MediaRecorderBase.OnEncodeListener,MediaRecorderBase.ContextAttachment {




    public interface OnNextClickListener{
        void OnEncodeStart();
//        void OnEncoding(int progress);
        void OnEncodeSuccess(Uri uri);
        void OnEncodeFail();
    }

    public interface OnCloseClickListener{
        void OnPositiveClicked();
        void OnNegativeClicked();
    }


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

    private int defaultDelayMills = 1000;

    private CheckBox isFrontCamera;
    private ImageButton switchFlash;
    private View next;
    private View close;

    private TextView pressToRecord;
    private SurfaceView surfaceView;
    private ImageView clickToRecord;
    private RadioGroup recordModeRadioGroup;
    private TextView recordInfo;

    private ProgressDialog encodeProgressDialog;
    private boolean isResumed;

    private int encodeStatusUnknown = -2;
    private int encodeStatusFail = encodeStatusUnknown+1;
    private int encodeStatusEncoding = encodeStatusFail+ 1;
    private int encodeStatusSuccess = encodeStatusEncoding+1;

    private int encodeStatus  = encodeStatusUnknown;//-2未知,-1失败,0进行中，１成功
    private Uri encodeResult;

    /**
     * SDK视频录制对象
     */
    private MediaRecorderBase mMediaRecorder;

    private MediaObject mMediaObject;

    private RecordProgressView recordProgress;


    private OnNextClickListener onNextClickListener;
    private OnCloseClickListener onCloseClickListener;


    private boolean startState;
    private volatile boolean mPressedStatus;

    private Handler handler = new RecordHandler(this);

    private Runnable removeRecordInfoAction = () -> recordInfo.setText("");


    private View.OnTouchListener pressToRecordOnTouchListener = new View.OnTouchListener() {

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (mMediaRecorder == null) {

                return false;
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 检测是否手动对焦
                    // 判断是否已经超时
                     startRecordCheck();
                    return  true;
                case MotionEvent.ACTION_UP:
                    stopRecordCheck();

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
        setup(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public RecordLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setup(context);
    }


    private void setup(Context context) {
        findViews(context);
        setupPressToRecord();
        setupClickToRecord();
        setupMediaRecorder();
        setupRecordPreview();
        setupCameraController();
        setupRecordProgress();
        setupRecordMode();
        setupConfig();
        setupNext();
        setupClose();
    }

    private void findViews(Context context) {
        LayoutInflater.from(context).inflate(R.layout.layout_record, this, true);
        isFrontCamera = findViewById(R.id.isFrontCamera);
        pressToRecord = findViewById(R.id.pressToRecord);
        surfaceView = findViewById(R.id.recordPreview);
        recordProgress = findViewById(R.id.recordProgress);
        switchFlash = findViewById(R.id.switchFlash);
        next = findViewById(R.id.next);
        clickToRecord = findViewById(R.id.clickToRecord);
        recordModeRadioGroup = findViewById(R.id.recordMode);
        recordInfo = findViewById(R.id.recordInfo);
        close = findViewById(R.id.back);
    }

    private void setupRecordMode() {
        recordModeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.pressMode) {
                pressToRecord.setVisibility(View.VISIBLE);
                clickToRecord.setVisibility(View.GONE);
            } else {
                pressToRecord.setVisibility(View.GONE);
                clickToRecord.setVisibility(View.VISIBLE);
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupPressToRecord() {
        pressToRecord.setOnTouchListener(pressToRecordOnTouchListener);
    }

    private void setupClickToRecord() {
        clickToRecord.setOnClickListener(v -> {

            if (mMediaRecorder == null) {
                return;
            }

            int level = clickToRecord.getDrawable().getLevel();


            if (level == 0) {
                if (startRecordCheck()) {
                    return;
                }
            } else {
                stopRecordCheck();
            }


            level++;


            if (level > 1) {
                level = 0;
            }

            clickToRecord.setImageLevel(level);


        });

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
        recordProgress.invalidate();
        recordProgress.setMaxDuration(maxRecordTime);
        recordProgress.setMinTime(minRecordTime);
    }

    /**
     * 初始化拍摄SDK
     */
    private void setupMediaRecorder() {
        mMediaRecorder = new MediaRecorderNative();

        mMediaRecorder.setVideoBitRate((int) (580000*2.0));

        mMediaRecorder.setOnErrorListener(this);
        mMediaRecorder.setOnEncodeListener(this);
        mMediaRecorder.setOnPreparedListener(this);

        mMediaRecorder.setContextAttachment(this);

//        File f = new File(JianXiCamera.getVideoCachePath());
//        if (!FileUtils.checkFile(f)) {
//            f.mkdirs();
//        }

        File file = getContext().getExternalCacheDir();
        if (file == null) {
            return;
        }

        File videoCache = new File(file.getAbsolutePath()+"/videoCache/");
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


    private void setupNext(){
        next.setOnClickListener(v-> stopRecord());
    }


    private void setupClose(){
        close.setOnClickListener(v-> onBackPressed());
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

    private void setupConfig() {
        MediaRecorderBase.NEED_FULL_SCREEN = true;
        MediaRecorderBase.SMALL_VIDEO_HEIGHT = DeviceUtils.getScreenHeight(getContext());
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



    private boolean startRecordCheck() {
        if (mMediaObject.getDuration() >= maxRecordTime) {

            recordInfo.setText(R.string.record_too_long_try_again);
            postDelayed(removeRecordInfoAction,defaultDelayMills);

            return true;
        }

        // 取消回删
        if (cancelDelete()) {
            return true;
        }

        if (!startState) {
            startState = true;
            startRecord();
        } else {
            mMediaObject.buildMediaPart(mMediaRecorder.mCameraId);
            recordProgress.setData(mMediaObject);
            setStartUI();
            mMediaRecorder.setRecordState(true);
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

        recordModeRadioGroup.setVisibility(View.INVISIBLE);

        if (pressToRecord.getVisibility() == View.VISIBLE) {
            pressToRecord.animate()
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(500)
                    .start();
        }

        if (clickToRecord.getVisibility() == View.VISIBLE) {
            clickToRecord.animate()
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(500)
                    .start();
        }


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
        next.setEnabled(false);
        close.setVisibility(INVISIBLE);
    }

    private void setStopUI() {

        recordModeRadioGroup.setVisibility(View.VISIBLE);

        mPressedStatus = false;
        if (pressToRecord.getVisibility() == View.VISIBLE) {
            pressToRecord.animate().scaleX(1).scaleY(1).setDuration(500).start();
        }

        if (clickToRecord.getVisibility() == View.VISIBLE) {
            pressToRecord.animate().scaleX(1).scaleY(1).setDuration(500).start();
        }


//        mRecordDelete.setVisibility(View.VISIBLE);
        isFrontCamera.setEnabled(true);
        switchFlash.setEnabled(true);
        next.setEnabled(true);
        close.setVisibility(VISIBLE);


        handler.removeMessages(HANDLE_STOP_RECORD);
        checkViewsVisibility();
    }



    private void stopRecordCheck() {
        mMediaRecorder.setRecordState(false);
        if (mMediaObject.getDuration() >= maxRecordTime) {
            next.performClick();
        } else {
            mMediaRecorder.setStopDate();
            setStopUI();
        }
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
            mMediaRecorder.startPreview();

        }


        isResumed = true;
        handleNextCallback();
        dismissProgressDialogIfNeeded();

    }

    public void onDestroy() {
        mMediaRecorder.release();
    }

    public void onPause() {

        isResumed = false;
        if (mMediaRecorder instanceof MediaRecorderNative) {
            ((MediaRecorderNative) mMediaRecorder).activityStop();
        }

        mMediaRecorder.stopPreview();
//        hideProgress();
//        mProgressDialog = null;

    }


    @Override
    public void onPrepared() {
//        MediaRecorderBase.mSupportedPreviewWidth = DeviceUtils.getScreenWidth(getContext());

        recordInfo.setText(R.string.ready_to_record);
        postDelayed(removeRecordInfoAction, defaultDelayMills);
    }

    @Override
    public void onVideoError(int what, int extra) {

    }

    @Override
    public void onAudioError(int what, String message) {

    }

    @Override
    public void onEncodeStart() {

        if (encodeProgressDialog == null) {
            encodeProgressDialog = new ProgressDialog(getContext());
            encodeProgressDialog.setMessage(getContext().getString(R.string.video_ffmpeg_encoding));
            encodeProgressDialog.setCancelable(false);
            encodeProgressDialog.show();
        }

        encodeStatus = encodeStatusEncoding;

        encodeResult = null;

        if (onNextClickListener != null) {
            onNextClickListener.OnEncodeStart();
        }
    }

    @Override
    public void onEncodeProgress(int progress) {
//        if (onNextClickListener != null) {
//            onNextClickListener.OnEncoding(progress);
//        }
    }

    @Override
    public void onEncodeComplete() {
//        Intent intent = new Intent(Intent.ACTION_VIEW);
        String path = mMediaObject.getOutputTempTranscodingVideoPath();//该路径可以自定义
        File file = new File(path);
        Uri uri = Uri.fromFile(file);
        encodeStatus = encodeStatusSuccess;
//        intent.setDataAndType(uri, "video/*");
//        getContext().startActivity(intent);
        encodeResult = uri;

        dismissProgressDialogIfNeeded();

        if (onNextClickListener != null && isResumed) {
            onNextClickListener.OnEncodeSuccess(uri);
        }
    }

    @Override
    public void onEncodeError() {
        encodeResult = null;
        encodeStatus = encodeStatusFail;
        dismissProgressDialogIfNeeded();
        if (onNextClickListener != null && isResumed) {
            onNextClickListener.OnEncodeFail();
        }
    }

    @Override
    public Context attachContext() {
        return getContext();
    }

    public void setOnNextClickListener(OnNextClickListener onNextClickListener) {
        this.onNextClickListener = onNextClickListener;
    }


    public void setOnCloseClickListener(OnCloseClickListener onCloseClickListener){
        this.onCloseClickListener = onCloseClickListener;
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    public void onBackPressed() {
        /*if (mRecordDelete != null && mRecordDelete.isChecked()) {
            cancelDelete();
            return;
        }*/

        if (mMediaObject != null && mMediaObject.getDuration() > 1) {
            // 未转码
            new AlertDialog.Builder(getContext())
                    .setMessage(R.string.record_camera_exit_dialog_message)
                    .setPositiveButton(
                            R.string.record_camera_cancel_dialog_yes,
                            (dialog, which) -> {
                                mMediaObject.delete();

                                if (onCloseClickListener != null) {
                                    onCloseClickListener.OnPositiveClicked();
                                }

                                mMediaObject = null;
                                setupMediaRecorder();
                                setupRecordProgress();

                            })
                    .setNegativeButton(R.string.record_camera_cancel_dialog_no,
                            (dialog, which)->{
                                if (onCloseClickListener != null) {
                                    onCloseClickListener.OnNegativeClicked();
                                }

                            })
                    .setCancelable(false)
                    .show();
            return;
        }

        if (mMediaObject != null) {
            mMediaObject.delete();
        }

        if (onCloseClickListener != null) {
            onCloseClickListener.OnPositiveClicked();
        }
    }



    private void dismissProgressDialogIfNeeded(){
        boolean isEncodeDone = ( encodeStatus == encodeStatusSuccess || encodeStatus == encodeStatusFail);
        if(isResumed && isEncodeDone && encodeProgressDialog != null && encodeProgressDialog.isShowing()){
            encodeProgressDialog.dismiss();
        }
    }

    private void handleNextCallback(){
        if (onNextClickListener == null) {
            return;
        }
        if(encodeProgressDialog == null || !encodeProgressDialog.isShowing()){
            return;
        }

        if(encodeStatus == encodeStatusSuccess){
            if(encodeResult != null){
                onNextClickListener.OnEncodeSuccess(encodeResult);
            }
        }else if( encodeStatus == encodeStatusFail){
            onNextClickListener.OnEncodeFail();


        }
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
