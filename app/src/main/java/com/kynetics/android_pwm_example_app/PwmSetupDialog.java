/*
 * Copyright (C)  2020 Kynetics, LLC
 * SPDX-License-Identifier: Apache-2.0
 */

package com.kynetics.android_pwm_example_app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.kynetics.libpwm.PwmManager;
import com.kynetics.libpwm.PwmManagerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PwmSetupDialog extends DialogFragment {
    OnPwmSetupDoneListener callback;
    private PwmManager pwmManager;
    private int pwmControllerId = 0;
    private int pwmChannelId = 0;
    private String TAG="KyneticsPwmApp::Setup";

    public static PwmSetupDialog newInstance() {
        return new PwmSetupDialog();
    }

    private List<String> mapPwmControllerNames() {
        /* Find all PWM controllers */
        List<String> pwmCtrlNames = new ArrayList<>();

        int[] pwmCtrls = pwmManager.getPwmControllers();
        for (int i = 0; i < pwmCtrls.length; i++) {
            pwmCtrlNames.add(String.format(Locale.ENGLISH, "PWM CHIP %d", pwmCtrls[i]));
        }

        return pwmCtrlNames;
    }

    private List<String> mapPwmChannelNames(int pwmController) {
        /* Find all PWM channels */
        List<String> pwmChNames = new ArrayList<>();

        int[] pwmChannels = pwmManager.getPwmChannels(pwmController);
        for (int i = 0; i < pwmChannels.length; i++) {
            pwmChNames.add(String.format(Locale.ENGLISH, "PWM%d", pwmChannels[i]));
        }

        return pwmChNames;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater i = getActivity().getLayoutInflater();
        final View v = i.inflate(R.layout.dialog, null);

        /* Initialize dialog */
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        b.setPositiveButton(android.R.string.ok, null);
        b.setView(v);

        setCancelable(false);

        /* Initialize PWM manager */
        pwmManager = PwmManagerFactory.getInstance();

       /* Setup list of pwm controllers and channels */
        final Spinner dropdownCtrls = v.findViewById(R.id.spinner_pwmCtrlSel);
        final Spinner dropdownChnls = v.findViewById(R.id.spinner_pwmSel);

        List<String> pwmCtrlNames = mapPwmControllerNames();
        /* No pwm controllers found */
        if (pwmCtrlNames.isEmpty()) {
            b.setView(i.inflate(R.layout.dialog_error, null));
            return b.create();
        }

        final Dialog dialog = b.create();

        /* Setup dropdown menus */
        ArrayAdapter<String> ctrlsAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, pwmCtrlNames);
        dropdownCtrls.setAdapter(ctrlsAdapter);
        dropdownCtrls.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                pwmControllerId = i;

                // Get list of pwms for controller i
                List<String> pwmChnlNames = mapPwmChannelNames(i);

                ArrayAdapter<String> chnlsAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, pwmChnlNames);
                dropdownChnls.setAdapter(chnlsAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        dropdownChnls.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG, "Selected PWM" + i + " of PWMCHIP" + pwmControllerId);
                pwmChannelId = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        /* Setup positive button */
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button posButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                posButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        callback.onPwmSetupDone(pwmControllerId, pwmChannelId);
                        dialog.dismiss();
                    }
                });
            }
        });

        return dialog;
    }

    public void setOnPwmSetupDoneListener(PwmSetupDialog.OnPwmSetupDoneListener callback) {
        this.callback = callback;
    }

    public interface OnPwmSetupDoneListener {
        void onPwmSetupDone(int controllerId, int channelId);
    }
}
