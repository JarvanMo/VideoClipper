package com.jarvanmo.videoclipper.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.google.android.exoplayer2.Player;
import com.jarvanmo.exoplayerview.media.SimpleMediaSource;
import com.jarvanmo.exoplayerview.ui.ExoVideoView;
import com.jarvanmo.videoclipper.R;
import com.jarvanmo.videoclipper.util.DensityUtils;
import com.jarvanmo.videoclipper.util.DisplayMetricsUtil;
import com.jarvanmo.videoclipper.util.VideoThumbUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VideoClipperView extends FrameLayout {

    /**
     * 计算公式:
     * PixRangeMax = (视频总长 * SCREEN_WIDTH) / 视频最长的裁剪时间(15s)
     * 视频总长/PixRangeMax = 当前视频的时间/游标当前所在位置
     */
    private static boolean isDebugMode = false;
    //
//    private static final String TAG = VideoTrimmerView.class.getSimpleName();
    private int margin;
    private int screenWidth;
    private int screenWidthFull;
    private static final int SHOW_PROGRESS = 2;
    //
    private Context mContext;
    private SeekBar progressSeekBar;
    private ExoVideoView exoVideoView;
    private RangedSeekBar rangedSeekBar;
    private RecyclerView thumbsContainer;
    private ThumbsAdapter thumbsAdapter;

    private List<Bitmap> thumbs = new ArrayList<>();
    //    private RangeSeekBarView mRangeSeekBarView;
//    private RelativeLayout mLinearVideo;
//    private ImageView mPlayView;
//    private VideoThumbHorizontalListView videoThumbListView;
//
    private Uri videoUri;
    private String mFinalPath;
    //
    private long mMaxDuration;
    //    private ProgressVideoListener mListeners;
//
//    private TrimVideoListener mOnTrimVideoListener;
    private long mDuration = 0;
    private long cursorPosition;
    private long mStartPosition = 0;
    private long mEndPosition = 0;


    private long pixelRangeMax;
    private int currentPixMax;  //用于处理红色进度条
    private int mScrolledOffset;
    private float leftThumbValue;
    private float rightThumbValue;
    private boolean isFromRestore = false;
    //
    private final MessageHandler mMessageHandler = new MessageHandler(this);


    private Player.DefaultEventListener defaultEventListener = new Player.DefaultEventListener() {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            super.onPlayerStateChanged(playWhenReady, playbackState);
            if (playbackState == Player.STATE_READY) {
                exoVideoView.getPlayer().setPlayWhenReady(true);
            } else if (playbackState == Player.STATE_ENDED) {
                onPlayerEnd();
            }

        }
    };


    private RecyclerView.OnScrollListener onThumbsContainerScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);


            if (dx < 0) {
                mScrolledOffset = mScrolledOffset - Math.abs(dx);
                if (mScrolledOffset <= 0)
                    mScrolledOffset = 0;
            } else {
                if (pixToTime(mScrolledOffset + screenWidth) <= mDuration)//根据时间来判断还是否可以向左滚动
                    mScrolledOffset = mScrolledOffset + dx;
            }
            onVideoReset();
            onSeekThumbs(0, mScrolledOffset + leftThumbValue);
            onSeekThumbs(1, mScrolledOffset + rightThumbValue);
            rangedSeekBar.invalidate();
        }
    };

    public VideoClipperView(@NonNull Context context) {
        this(context, null);
    }

    public VideoClipperView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoClipperView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupVariables();
        setupViews(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public VideoClipperView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setupVariables();
        setupViews(context);
    }


    private void setupVariables() {
        margin = DensityUtils.dp2px(getContext(), 6);
        screenWidth = (DisplayMetricsUtil.getWidth(getContext()) - margin * 2);
        screenWidthFull = DisplayMetricsUtil.getWidth(getContext());
    }

    private void setupViews(Context context) {
        this.mContext = context;
        LayoutInflater.from(context).inflate(R.layout.video_clipper_view, this, true);

        progressSeekBar = findViewById(R.id.progressSeekBar);
        rangedSeekBar = findViewById(R.id.timeline);
        thumbsContainer = findViewById(R.id.thumbsContainer);
        exoVideoView = findViewById(R.id.videoView);
        setupThumbsContainer();

        setupListeners();

        setupSeekBar();
    }


    private void setupThumbsContainer() {
        thumbsAdapter = new ThumbsAdapter(thumbs);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        thumbsContainer.setLayoutManager(linearLayoutManager);
        thumbsContainer.setAdapter(thumbsAdapter);
        thumbsContainer.addOnScrollListener(onThumbsContainerScrollListener);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupSeekBar() {
        progressSeekBar.setEnabled(false);
        progressSeekBar.setOnTouchListener(new OnTouchListener() {
            private float startX;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        return false;
                }

                return true;
            }
        });

    }

    public void setUri(Uri videoUri) {
        this.videoUri = videoUri;

        SimpleMediaSource simpleMediaSource = new SimpleMediaSource(videoUri);
        exoVideoView.play(simpleMediaSource, false);
        exoVideoView.getPlayer().addListener(defaultEventListener);
        VideoThumbUtil.getVideoThumb(getContext(), videoUri, new VideoThumbUtil.LoadThumbsCallback() {
            @Override
            public void onThumbsLoaded(List<Bitmap> thumbs) {
                VideoClipperView.this.thumbs.addAll(thumbs);
                thumbsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNoDataAvailable() {

            }
        });
    }

    //
//    public void setVideoURI(final Uri videoURI) {
//        mSrc = videoURI;
//
//        mVideoView.setVideoURI(mSrc);
//        mVideoView.requestFocus();
//
//        TrimVideoUtil.backgroundShootVideoThumb(mContext, mSrc, new SingleCallback<ArrayList<Bitmap>, Integer>() {
//            @Override
//            public void onSingleCallback(final ArrayList<Bitmap> bitmap, final Integer interval) {
//                UiThreadExecutor.runTask("", new Runnable() {
//                    @Override
//                    public void run() {
//                        videoThumbAdapter.addAll(bitmap);
//                        videoThumbAdapter.notifyDataSetChanged();
//                    }
//                }, 0L);
//
//            }
//        });
//    }
//
    private void initSeekBarPosition() {
        seekTo(mStartPosition);
        //时间与屏幕的刻度永远保持一致
        pixelRangeMax = (mDuration * screenWidth) / mMaxDuration;
        rangedSeekBar.initThumbForRangeSeekBar(mDuration, pixelRangeMax);

        //大于15秒的时候,游标处于0-15秒
        if (mDuration >= mMaxDuration) {
            mEndPosition = mMaxDuration;
            cursorPosition = mMaxDuration;
        } else {//小于15秒,游标处于0-mDuration
            mEndPosition = mDuration;
            cursorPosition = mDuration;
        }

        setUpProgressBarMarginsAndWidth(margin, screenWidthFull - (int) timeToPix(mEndPosition) - margin);//Fucking seekBar,Waste a lot of my time

        rangedSeekBar.setThumbValue(0, 0);
        rangedSeekBar.setThumbValue(1, timeToPix(mEndPosition));
//        mVideoView.pause();
        setProgressBarMax();
        setProgressBarPosition(mStartPosition);
        rangedSeekBar.initMaxWidth();
        rangedSeekBar.setStartEndTime(mStartPosition, mEndPosition);

        /**记录两个游标对应屏幕的初始位置,这个两个值只会在视频长度可以滚动的时候有效*/
        leftThumbValue = 0;
        rightThumbValue = mDuration <= mMaxDuration ? timeToPix(mDuration) : timeToPix(mMaxDuration);
    }

    private void initSeekBarFromRestore() {

        seekTo(mStartPosition);
        setUpProgressBarMarginsAndWidth((int) leftThumbValue, (int) (screenWidthFull - rightThumbValue - margin));//设置seekar的左偏移量

        setProgressBarMax();
        setProgressBarPosition(mStartPosition);
        rangedSeekBar.setStartEndTime(mStartPosition, mEndPosition);

        leftThumbValue = 0;
        rightThumbValue = mDuration <= mMaxDuration ? timeToPix(mDuration) : timeToPix(mMaxDuration);
    }

    //
//    private void onCancelClicked() {
//        mOnTrimVideoListener.onCancel();
//    }
//
    private void onPlayerIndicatorSeekStart() {
//        mMessageHandler.removeMessages(SHOW_PROGRESS);
        exoVideoView.pause();
        notifyProgressUpdate();
    }

    private void onPlayerIndicatorSeekStop(SeekBar seekBar) {
        exoVideoView.pause();
    }

    //
//
    private void onPlayerReady() {

        mDuration = exoVideoView.getPlayer().getDuration();

        if (!getRestoreState()) {
            initSeekBarPosition();
        } else {
            setRestoreState(false);
            initSeekBarFromRestore();
        }
    }

    //
//
    private void onSeekThumbs(int index, float value) {
        switch (index) {
            case Thumb.LEFT: {
                mStartPosition = pixToTime(value);
                setProgressBarPosition(mStartPosition);
                break;
            }
            case Thumb.RIGHT: {
                mEndPosition = pixToTime(value);
                if (mEndPosition > mDuration)//实现归位
                    mEndPosition = mDuration;
                break;
            }
        }
        setProgressBarMax();

        rangedSeekBar.setStartEndTime(mStartPosition, mEndPosition);
        seekTo(mStartPosition);
        cursorPosition = mEndPosition - mStartPosition;

        setUpProgressBarMarginsAndWidth((int) leftThumbValue, (int) (screenWidthFull - rightThumbValue - margin));//设置seekar的左偏移量
    }

    private void onStopSeekThumbs() {
//        mMessageHandler.removeMessages(SHOW_PROGRESS);
        setProgressBarPosition(mStartPosition);
        onVideoReset();
    }

    private void onPlayerEnd() {
        seekTo(mStartPosition);
        setPlayPauseViewIcon(false);
    }

    private void onVideoReset() {
        exoVideoView.pause();
        setPlayPauseViewIcon(false);
    }

    public void onPause() {

        Player player = exoVideoView.getPlayer();


        if (player != null && player.getPlayWhenReady()) {
//            mMessageHandler.removeMessages(SHOW_PROGRESS);
            exoVideoView.pause();
            seekTo(mStartPosition);//复位
            setPlayPauseViewIcon(false);
        }
    }

    //
    private void setProgressBarPosition(long time) {
        progressSeekBar.setProgress((int) (time - mStartPosition));
    }

    private void setProgressBarMax() {
        progressSeekBar.setMax((int) (mEndPosition - mStartPosition));
    }
//
//    public void setOnTrimVideoListener(TrimVideoListener onTrimVideoListener) {
//        mOnTrimVideoListener = onTrimVideoListener;
//    }

    /**
     * Cancel trim thread execute action when finish
     */
    public void destroy() {
//        BackgroundExecutor.cancelAll("", true);
//        UiThreadExecutor.cancelAll("");
    }

    public void setMaxDuration(long maxDuration, TimeUnit timeUnit) {
        mMaxDuration = TimeUnit.MILLISECONDS.convert(maxDuration, timeUnit);
    }


    public void setMaxDuration(long maxDuration) {
        this.mMaxDuration = maxDuration;
    }

    private void setupListeners() {
//        mListeners = new ProgressVideoListener() {
//            @Override
//            public void updateProgress(int time, int max, float scale) {
//                updateVideoProgress(time);
//            }
//        };


        rangedSeekBar.addOnRangeSeekBarListener(new RangedSeekBar.RangedSeekBarListener() {
            @Override
            public void onCreate(RangedSeekBar rangedSeekBar, int index, float value) {

            }

            @Override
            public void onSeek(RangedSeekBar rangedSeekBar, int index, float value) {
                if (index == 0) {
                    leftThumbValue = value;
                } else {
                    rightThumbValue = value;
                }

                onSeekThumbs(index, value + Math.abs(mScrolledOffset));
            }

            @Override
            public void onSeekStart(RangedSeekBar rangedSeekBar, int index, float value) {
                if (progressSeekBar.getVisibility() == View.VISIBLE) {
                    progressSeekBar.setVisibility(GONE);
                }
            }

            @Override
            public void onSeekStop(RangedSeekBar rangedSeekBar, int index, float value) {
                onStopSeekThumbs();
            }
        });


//
        progressSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                onPlayerIndicatorSeekStart();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                onPlayerIndicatorSeekStop(seekBar);
            }
        });


    }

    private void setUpProgressBarMarginsAndWidth(int left, int right) {
        if (left == 0) {
            left = margin;
        }

        ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) progressSeekBar.getLayoutParams();
        lp.setMargins(left, 0, right, 0);
        progressSeekBar.setLayoutParams(lp);
        currentPixMax = screenWidthFull - left - right;
        progressSeekBar.getLayoutParams().width = currentPixMax;
    }

    private void onSaveClicked() {

    }

    private String getTrimmedVideoPath() {
        if (mFinalPath == null) {
//            File file = mContext.getExternalCacheDir();
//            if (file != null)
//                mFinalPath = file.getAbsolutePath();
        }
        return mFinalPath;
    }

    private void onClickVideoPlayPause() {
//        if (mVideoView.isPlaying()) {
//            mVideoView.pause();
//            mMessageHandler.removeMessages(SHOW_PROGRESS);
//        } else {
//            mVideoView.start();
//            progressSeekBar.setVisibility(View.VISIBLE);
//            mMessageHandler.sendEmptyMessage(SHOW_PROGRESS);
//        }

//        setPlayPauseViewIcon(mVideoView.isPlaying());
    }

    /**
     * 屏幕长度转化成视频的长度
     */
    private long pixToTime(float value) {
        if (pixelRangeMax == 0)
            return 0;
        return (long) ((mDuration * value) / pixelRangeMax);
    }

    /**
     * 视频长度转化成屏幕的长度
     */
    private long timeToPix(long value) {
        return (pixelRangeMax * value) / mDuration;
    }

    private void seekTo(long position) {
        Player player = exoVideoView.getPlayer();
        if (player != null) {
            player.seekTo(position);
        }
    }


    private boolean getRestoreState() {
        return isFromRestore;
    }

    public void setRestoreState(boolean fromRestore) {
        isFromRestore = fromRestore;
    }
//

    private static class MessageHandler extends Handler {


        private final WeakReference<VideoClipperView> mView;

        MessageHandler(VideoClipperView view) {
            mView = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            VideoClipperView view = mView.get();
            if (view == null || view.exoVideoView == null) {
                return;
            }
//
//            view.notifyProgressUpdate();
//            if (view.mVideoView.isPlaying()) {
//                sendEmptyMessageDelayed(0, 10);
//            }
        }
    }

    //
//    private void updateVideoProgress(int time) {
//        if (mVideoView == null) {
//            return;
//        }
//        if (isDebugMode) Log.i("Jason", "updateVideoProgress time = " + time);
//        if (time >= mEndPosition) {
//            mMessageHandler.removeMessages(SHOW_PROGRESS);
//            mVideoView.pause();
//            seekTo(mStartPosition);
//            setPlayPauseViewIcon(false);
//            return;
//        }
//
//        if (progressSeekBar != null) {
//            setProgressBarPosition(time);
//        }
//    }
//
    private void notifyProgressUpdate() {
        if (mDuration == 0) {
            return;
        }
//        int position = mVideoView.getCurrentPosition();
//        if (isDebugMode) Log.i("Jason", "updateVideoProgress position = " + position);
//        mListeners.updateProgress(position, 0, 0);
    }

    private void setPlayPauseViewIcon(boolean isPlaying) {
//        mPlayView.setImageResource(isPlaying ? R.drawable.icon_video_pause_black : R.drawable.icon_video_play_black);
    }
//

//

    private class ThumbsAdapter extends RecyclerView.Adapter<ThumbsViewHolder> {

        private List<Bitmap> bitmaps;


        ThumbsAdapter(List<Bitmap> bitmaps) {
            this.bitmaps = bitmaps;
        }

        @NonNull
        @Override
        public ThumbsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View root = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_thumb, parent, false);
            return new ThumbsViewHolder(root);
        }

        @Override
        public void onBindViewHolder(@NonNull ThumbsViewHolder holder, int position) {
            holder.thumb.setImageBitmap(bitmaps.get(position));
        }

        @Override
        public int getItemCount() {
            return bitmaps == null ? 0 : bitmaps.size();
        }
    }

    private class ThumbsViewHolder extends RecyclerView.ViewHolder {

        ImageView thumb;

        ThumbsViewHolder(View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.thumb);
        }
    }


}
