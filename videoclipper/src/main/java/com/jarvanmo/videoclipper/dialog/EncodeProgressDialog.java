package com.jarvanmo.videoclipper.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import com.jarvanmo.videoclipper.R;

public class EncodeProgressDialog extends DialogFragment {

    public static EncodeProgressDialog newInstance() {
        
        Bundle args = new Bundle();
        
        EncodeProgressDialog fragment = new EncodeProgressDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_encode_progress,null);
        builder.setView(view);
        setCancelable(false);


        return builder.create();
    }

    public void updateProgress(int progress){

//        ProgressBar progressBar = getDialog().findViewById(R.id.encodeProgress);
//        progressBar.setProgress(progress);
//        String str = progress+"/100";
//
//        TextView  progressStr = getDialog().findViewById(R.id.progressStr);
//        progressStr.setText(str);
    }
}
