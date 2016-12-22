package io.github.memfis19.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import io.github.memfis19.annca.Annca;
import io.github.memfis19.annca.internal.configuration.AnncaConfiguration;

/**
 * Annca
 * Created by memfis on 12/22/16.
 */

public class DemoDialogFragment extends DialogFragment {

    private Fragment fragment;

    private static final int CAPTURE_MEDIA = 131;

    public static DemoDialogFragment getInstance() {
        return new DemoDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        fragment = this;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View parent = inflater.inflate(R.layout.fragment_demo_layout, container, false);

        final AnncaConfiguration.Builder dialogDemo = new AnncaConfiguration.Builder(fragment, CAPTURE_MEDIA);

        parent.findViewById(R.id.dialogDemo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Annca(dialogDemo.build()).launchCamera();
            }
        });

        return parent;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == CAPTURE_MEDIA) {
            Toast.makeText(getContext(), "Media captured.", Toast.LENGTH_SHORT).show();
        }
    }
}