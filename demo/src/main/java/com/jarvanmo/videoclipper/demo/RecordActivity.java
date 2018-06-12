package com.jarvanmo.videoclipper.demo;

import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.jarvanmo.ffmpeg.JianXiCamera;
import com.jarvanmo.videoclipper.dialog.EncodeProgressDialog;
import com.jarvanmo.videoclipper.widget.RecordLayout;

public class RecordActivity extends AppCompatActivity {

    private RecordLayout recordLayout;
    private EncodeProgressDialog encodeProgressDialog;

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


        recordLayout.setOnCloseClickListener(new RecordLayout.OnCloseClickListener() {
            @Override
            public void OnPositiveClicked() {
                    finish();
            }

            @Override
            public void OnNegativeClicked() {

            }
        });


        recordLayout.setOnNextClickListener(new RecordLayout.OnNextClickListener() {
            @Override
            public void OnEncodeStart() {
//                encodeProgressDialog = EncodeProgressDialog.newInstance();
//                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//                encodeProgressDialog.show(ft, "videoProgress");
            }

//            @Override
//            public void OnEncoding(int progress) {
//                encodeProgressDialog.updateProgress(progress);
//                Toast.makeText(RecordActivity.this,"--"+progress,Toast.LENGTH_SHORT).show();
//            }

            @Override
            public void OnEncodeSuccess(Uri uri) {
//                encodeProgressDialog.dismiss();
//
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "video/*");
                startActivity(intent);
            }

            @Override
            public void OnEncodeFail() {
//                encodeProgressDialog.dismiss();
            }
        });
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
    public void onBackPressed() {
        recordLayout.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        recordLayout.onPause();
    }
}
