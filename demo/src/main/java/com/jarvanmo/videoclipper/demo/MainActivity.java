package com.jarvanmo.videoclipper.demo;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.jarvanmo.ffmpeg.JianXiCamera;
import com.jarvanmo.ffmpeg.LocalMediaCompress;
import com.jarvanmo.ffmpeg.MediaRecorderActivity;
import com.jarvanmo.ffmpeg.model.AutoVBRMode;
import com.jarvanmo.ffmpeg.model.BaseMediaBitrateConfig;
import com.jarvanmo.ffmpeg.model.CBRMode;
import com.jarvanmo.ffmpeg.model.LocalMediaConfig;
import com.jarvanmo.ffmpeg.model.OnlyCompressOverBean;
import com.jarvanmo.videoclipper.widget.VideoClipperView;

import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_CODEE_VIDEO = 100;

    private VideoClipperView videoClipperView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JianXiCamera.initialize(true,"/log");
        JianXiCamera.setVideoCachePath(getExternalCacheDir().getAbsolutePath());
        setContentView(R.layout.activity_main);
        findViewById(R.id.root).setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        videoClipperView = findViewById(R.id.videoClipper);
        videoClipperView.setMaxDuration(31, TimeUnit.SECONDS);

        try {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            intent.setDataAndType(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/*");
            startActivityForResult(intent, REQUEST_CODEE_VIDEO);
        }catch (SecurityException e){
//            Intent intent = new Intent(Intent.ACTION_PICK, null);
//            intent.setType("video/*");
//            startActivityForResult(intent, REQUEST_CODEE_VIDEO);
        }


    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }

        if(requestCode == REQUEST_CODEE_VIDEO){
            Uri uri = data.getData();
            if (uri == null) {
                return;

            }

            if (data.getData() != null) {
                String[] proj = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.MIME_TYPE};

                Cursor cursor = getContentResolver().query(uri, proj, null,
                        null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int _data_num = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                    int mime_type_num = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE);

                    String _data = cursor.getString(_data_num);
                    String mime_type = cursor.getString(mime_type_num);
                    if (!TextUtils.isEmpty(mime_type) && mime_type.contains("video") && !TextUtils.isEmpty(_data)) {
                        BaseMediaBitrateConfig compressMode = null;

                            compressMode = new AutoVBRMode(37);


                        int iRate = 0;
                        float fScale=0;
                        LocalMediaConfig.Buidler buidler = new LocalMediaConfig.Buidler();
                        final LocalMediaConfig config = buidler
                                .setVideoPath(_data)
                                .captureThumbnailsTime(1)
                                .doH264Compress(compressMode)
                                .setFramerate(iRate)
                                .setScale(fScale)
                                .build();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this,"commmmm",Toast.LENGTH_SHORT).show();
//                                        showProgress("", "压缩中...", -1);
                                    }
                                });
                                OnlyCompressOverBean onlyCompressOverBean = new LocalMediaCompress(config).startCompress();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
//                                        hideProgress();

                                        Toast.makeText(MainActivity.this,"done",Toast.LENGTH_SHORT).show();
                                    }
                                });
                                runOnUiThread(()->{
                                    videoClipperView.setUri(Uri.parse(onlyCompressOverBean.getVideoPath()));
                                });
//                                Intent intent = new Intent(MainActivity.this, SendSmallVideoActivity.class);
//                                intent.putExtra(MediaRecorderActivity.VIDEO_URI, onlyCompressOverBean.getVideoPath());
//                                intent.putExtra(MediaRecorderActivity.VIDEO_SCREENSHOT, onlyCompressOverBean.getPicPath());
//                                startActivity(intent);
                            }
                        }).start();
                    } else {
                        Toast.makeText(this, "选择的不是视频或者地址错误,也可能是这种方式定制神机取不到！", Toast.LENGTH_SHORT).show();
                    }
                }
            }

//            ContentResolver cr = this.getContentResolver();
//            Cursor cursor = cr.query(uri, null, null, null, null);
//            if (cursor != null) {
//                if (cursor.moveToFirst()) {
//                    // 视频ID:MediaStore.Audio.Media._ID
//
//                    String videoPath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
//                    videoClipperView.setUri(Uri.parse(videoPath));
//                }
//
//                cursor.close();
//            }
        }

    }
}
