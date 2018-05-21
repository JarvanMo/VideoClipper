package com.jarvanmo.videoclipper;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.jarvanmo.videoclipper.widget.VideoClipperView;
import com.videoclipper.demo.R;

import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_CODEE_VIDEO = 100;

    private VideoClipperView videoClipperView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.root).setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        videoClipperView = findViewById(R.id.videoClipper);
        videoClipperView.setMaxDuration(31, TimeUnit.SECONDS);
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODEE_VIDEO);
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
            ContentResolver cr = this.getContentResolver();
            Cursor cursor = cr.query(uri, null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    // 视频ID:MediaStore.Audio.Media._ID

                    String videoPath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                    videoClipperView.setUri(Uri.parse(videoPath));
                }

                cursor.close();
            }
        }

    }
}
