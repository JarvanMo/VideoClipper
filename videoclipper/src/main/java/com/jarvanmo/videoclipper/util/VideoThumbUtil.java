package com.jarvanmo.videoclipper.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.AsyncTaskLoader;

import com.jarvanmo.videoclipper.R;

import java.util.ArrayList;
import java.util.List;

public class VideoThumbUtil {

    public static final int VIDEO_MAX_DURATION = 15;// 15秒
    public static final int MIN_TIME_FRAME = 5;
    private static  int thumbWidth;
    private static  int thumbHeight ;
    private static final long oneFrameTime = 1000000;
    public interface  LoadThumbsCallback{

        void onThumbsLoaded(List<Bitmap> thumbs);

        void onNoDataAvailable();
    }

    @SuppressWarnings("unchecked")
    public static  void getVideoThumb(Context context, Uri uri, LoadThumbsCallback callback ){
        thumbWidth = (DisplayMetricsUtil.getWidth(context)- DensityUtils.dp2px(context,60)) / VIDEO_MAX_DURATION;
        thumbHeight = DensityUtils.dp2px(context,context.getResources().getDimensionPixelSize(R.dimen.frames_video_height));


        final ArrayList<Bitmap> bitmaps = new ArrayList<>();

        Runnable task = () -> {
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(context, uri);
            // Retrieve media data use microsecond
            long videoLengthInMs = Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) * 1000;
            long numThumbs = videoLengthInMs < oneFrameTime ? 1 : (videoLengthInMs / oneFrameTime);
            final long interval = videoLengthInMs / numThumbs;


            //每次截取到3帧之后上报
            for (long i = 0; i < numThumbs; ++i) {
                Bitmap bitmap = mediaMetadataRetriever.getFrameAtTime(i * interval, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                try {
                    bitmap = Bitmap.createScaledBitmap(bitmap, thumbWidth, thumbHeight, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                bitmaps.add(bitmap);
                if (bitmaps.size() == 3) {
                    UiThreadUtil.runOnUiThread(()->callback.onThumbsLoaded((ArrayList<Bitmap>) bitmaps.clone()));
                    bitmaps.clear();
                }
            }
            if (bitmaps.isEmpty()) {
                UiThreadUtil.runOnUiThread(()->callback.onThumbsLoaded((ArrayList<Bitmap>) bitmaps.clone()));
                bitmaps.clear();
            }else {
                UiThreadUtil.runOnUiThread(callback::onNoDataAvailable);
            }
            mediaMetadataRetriever.release();
        };

        new Thread(task).start();

    }
}
