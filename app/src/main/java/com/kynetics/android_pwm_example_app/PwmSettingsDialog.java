/*
 * Copyright © 2020 – 2025  Kynetics, Inc.
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.snackbar.Snackbar;
import com.kynetics.android.sdk.pwm.Pwm;
import com.kynetics.android.sdk.pwm.PwmPolarity;

import java.io.IOException;

public class PwmSettingsDialog extends DialogFragment {
    private static Pwm mPwm;
    private String TAG = "KyneticsPwmApp::Settings";
    private OnPwmSettingsChangedListener callback;
    private EditText editTextFreq;
    private EditText editTextDutyCycle;
    private RadioGroup radioGroupPolarity;

    public static PwmSettingsDialog newInstance(Pwm pwm) {
        PwmSettingsDialog frag = new PwmSettingsDialog();
        /* Save PWM instance */
        mPwm = pwm;
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater i = getActivity().getLayoutInflater();
        final View v = i.inflate(R.layout.dialog_settings, null);

        editTextFreq = v.findViewById(R.id.editText_frequency);
        editTextDutyCycle = v.findViewById(R.id.editText_dutyCycle);
        radioGroupPolarity = v.findViewById(R.id.radioGroupPolarity);

        /* Setup initial values */
        try {
            editTextFreq.setText(String.valueOf(mPwm.getFrequencyHz()));
            editTextDutyCycle.setText(String.valueOf((int) mPwm.getDutyCycle()).trim());
            radioGroupPolarity.check(mPwm.getPolarity() == PwmPolarity.POLARITY_NORMAL ? R.id.radioButtonNormal : R.id.radioButtonInversed);

        } catch (IOException e) {
            e.printStackTrace();
        }

        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        b.setPositiveButton(android.R.string.ok, null);
        b.setView(v);

        final Dialog dialog = b.create();

        /* Update frequency, duty cycle and polarity */
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button posButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                posButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (editTextFreq != null) {
                            /* Update frequency value */
                            String freqStr = editTextFreq.getText().toString().trim();
                            if (freqStr.length() != 0) {
                                Log.i(TAG, "Setting frequency to: " + freqStr);

                                try {
                                    mPwm.setFrequencyHz(Double.valueOf(freqStr));
                                    callback.onPwmSettingsChanged();
                                } catch (IOException | IllegalArgumentException e) {
                                    Log.e(TAG, "Cannot set PWM frequency");
                                    Snackbar.make(v.getRootView(), "Error setting PWM frequency: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                                    e.printStackTrace();
                                    return;
                                }
                            } else
                                Log.i(TAG, "Frequency value not updated.");
                        }

                        if (editTextDutyCycle != null) {
                            /* Update duty cycle value */
                            String dutyCycleStr = editTextDutyCycle.getText().toString().trim();

                            if (dutyCycleStr.length() != 0) {
                                Log.i(TAG, "Setting duty cycle to: " + dutyCycleStr);

                                try {
                                    mPwm.setDutyCycle(Double.valueOf(dutyCycleStr));
                                    callback.onPwmSettingsChanged();
                                } catch (IOException | IllegalArgumentException e) {
                                    Log.e(TAG, "Cannot set PWM duty cycle");
                                    Snackbar.make(v.getRootView(), "Error setting PWM duty cycle: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                                    e.printStackTrace();
                                    return;
                                }
                            } else
                                Log.i(TAG, "Duty cycle value not updated.");
                        }

                        /* Update polarity value */
                        int checkedId = radioGroupPolarity.getCheckedRadioButtonId();
                        if (checkedId != -1) {
                            Log.i(TAG, "Setting polarity to: " + (checkedId == R.id.radioButtonNormal ? "normal" : "inversed"));
                            try {
                                mPwm.setPolarity(checkedId == R.id.radioButtonNormal ? PwmPolarity.POLARITY_NORMAL : PwmPolarity.POLARITY_INVERSED);
                                callback.onPwmSettingsChanged();
                            } catch (IOException e) {
                                Log.e(TAG, "Cannot set PWM polarity");
                                Snackbar.make(v.getRootView(), "Error setting PWM polarity: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                                e.printStackTrace();
                                return;
                            }
                        } else
                            Log.i(TAG, "Polarity not updated.");

                        /* Update info panel */
                        dialog.dismiss();
                    }
                });
            }
        });

        return dialog;
    }

    public void setOnPwmSettingsChangedListener(OnPwmSettingsChangedListener callback) {
        this.callback = callback;
    }

    public interface OnPwmSettingsChangedListener {
        void onPwmSettingsChanged();
    }

}
