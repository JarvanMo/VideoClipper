package com.jarvanmo.videoclipper.demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.jarvanmo.ffmpeg.JianXiCamera;
import com.jarvanmo.videoclipper.widget.RecordLayout;

public class RecordActivity extends AppCompatActivity {

    private RecordLayout recordLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_record);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        JianXiCamera.initialize(true,"/log");
        JianXiCamera.setVideoCachePath(getExternalCacheDir().getAbsolutePath());

        recordLayout = findViewById(R.id.record);


    }

    @Override
    protected void onResume() {
        super.onResume();
        recordLayout.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        recordLayout.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        recordLayout.onStop();
    }
}
