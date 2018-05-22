package com.jarvanmo.videoclipper.widget;

import android.content.Context;
import android.os.Build;
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
import android.widget.TextView;

import com.jarvanmo.ffmpeg.MediaRecorderBase;
import com.jarvanmo.ffmpeg.MediaRecorderNative;
import com.jarvanmo.videoclipper.R;

import java.io.File;

public class RecordLayout extends FrameLayout implements
        MediaRecorderBase.OnErrorListener, MediaRecorderBase.OnPreparedListener,
        MediaRecorderBase.OnEncodeListener {

    private CheckBox isFrontCamera;
    private TextView pressToRecord;
    private SurfaceView surfaceView;


    /**
     * SDK视频录制对象
     */
    private MediaRecorderBase mMediaRecorder;






    private View.OnTouchListener mOnVideoControllerTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (mMediaRecorder == null) {
                return false;
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 检测是否手动对焦
                    // 判断是否已经超时
                    if (mMediaObject.getDuration() >= RECORD_TIME_MAX) {
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
                        mProgressView.setData(mMediaObject);
                        setStartUI();
                        mMediaRecorder.setRecordState(true);
                    }

                    break;

                case MotionEvent.ACTION_UP:

                    mMediaRecorder.setRecordState(false);
                    if (mMediaObject.getDuration() >= RECORD_TIME_MAX) {
                        mTitleNext.performClick();
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
        this(context,null);
    }

    public RecordLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public RecordLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        findViews(context);
        setupPressToRecord();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public RecordLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        findViews(context);
        setupPressToRecord();
    }


    private void findViews(Context context){
        LayoutInflater.from(context).inflate(R.layout.layout_record, this, true);
        isFrontCamera = findViewById(R.id.switchCamera);
        pressToRecord = findViewById(R.id.pressToRecord);
        surfaceView = findViewById(R.id.recordPreview);
    }


    private void setupPressToRecord(){



    }

    private void setupRecordPreview(){

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

        File file = getContext().getExternalMediaDirs();
        String key = String.valueOf(System.currentTimeMillis());
//        mMediaObject = mMediaRecorder.setOutputDirectory(key,
//                JianXiCamera.getVideoCachePath() + key);
        mMediaRecorder.setSurfaceHolder(surfaceView.getHolder());
        mMediaRecorder.prepare();
    }

    @Override
    public void onPrepared() {

    }

    @Override
    public void onVideoError(int what, int extra) {

    }

    @Override
    public void onAudioError(int what, String message) {

    }

    @Override
    public void onEncodeStart() {

    }

    @Override
    public void onEncodeProgress(int progress) {

    }

    @Override
    public void onEncodeComplete() {

    }

    @Override
    public void onEncodeError() {

    }
}
