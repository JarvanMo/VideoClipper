package com.jarvanmo.videoclipper.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jarvanmo.videoclipper.R;

public class EncodeProgressDialog extends DialogFragment {

    public static EncodeProgressDialog newInstance() {
        
        Bundle args = new Bundle();
        
        EncodeProgressDialog fragment = new EncodeProgressDialog();
        fragment.setArguments(args);
        return fragment;
    }

    private TextView progressStr;
    private ProgressBar progressBar;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_encode_progress,null);
        progressBar = view.findViewById(R.id.encodeProgress);
        progressStr = view.findViewById(R.id.progressStr);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        return dialog;
    }

    public void updateProgress(int progress){
        progressBar.setProgress(progress);
        String str = progress+"/100";
        progressStr.setText(str);
    }
}
