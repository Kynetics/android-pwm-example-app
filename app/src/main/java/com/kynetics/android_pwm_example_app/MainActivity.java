/*
 * Copyright © 2020 – 2024  Kynetics, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.kynetics.android_pwm_example_app;

import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.kynetics.android.sdk.pwm.Pwm;
import com.kynetics.android.sdk.pwm.PwmManager;
import com.kynetics.android.sdk.pwm.PwmManagerFactory;
import com.kynetics.android.sdk.pwm.PwmPolarity;

import java.io.IOException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "KyneticsPwmApp::Main";
    private PwmManager pwmManager;
    private Pwm pwm;
    private int pwmControllerId = 0;
    private int pwmChannelId = 0;
    private View cardView;
    private Switch switchEnable;
    private TextView textCtrlId;
    private TextView textChId;
    private TextView textFreq;
    private TextView textDutyCycle;
    private TextView textPolarity;
    private FloatingActionButton settingsFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View mainView = this.getLayoutInflater().inflate(R.layout.activity_main, null);
        setContentView(mainView);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /* Initialize PWM manager */
        pwmManager = PwmManagerFactory.getInstance();

        /* Setup main UI elements */
        cardView = findViewById(R.id.card_view_row1_col1);
        switchEnable = findViewById(R.id.switchEnabled);
        textCtrlId = findViewById(R.id.textView_controllerId);
        textChId = findViewById(R.id.textView_chipId);
        textFreq = findViewById(R.id.textView_frequency);
        textDutyCycle = findViewById(R.id.textView_dutyCycle);
        textPolarity = findViewById(R.id.textView_polarity);
        settingsFab = findViewById(R.id.fabSettings);

    }

    @Override
    protected void onStart() {
        super.onStart();

        /* Show PWM setup dialog */
        FragmentManager fm = getSupportFragmentManager();
        PwmSetupDialog frag = PwmSetupDialog.newInstance();

        frag.setOnPwmSetupDoneListener((controllerId, channelId) -> {
            pwmControllerId = controllerId;
            pwmChannelId = channelId;
            initPwmUI();
        });

        frag.show(fm, "dialog");
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_recreate) {
            recreate();
            return true;
        } else if (item.getItemId() == R.id.menu_about) {
            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initPwmUI() {
        /* Open PWM */
        try {
            pwm = pwmManager.open(pwmControllerId, pwmChannelId);
        } catch (IOException e) {
            Log.e(TAG, "Cannot open PWM");
            Snackbar.make(cardView, "Error opening PWM channel: " + e.getMessage(), Snackbar.LENGTH_INDEFINITE).show();
            e.printStackTrace();
            switchEnable.setEnabled(false);
            settingsFab.setEnabled(false);
            return;
        }

        updatePwmInfo();
        setupPwmEnableSwitch();
        setupPwmSettingsButton();
    }

    private void updatePwmInfo() {
        /* Display controller and channel ids */
        textCtrlId.setText("controller id: " + pwmControllerId);
        textChId.setText("channel id: " + pwmChannelId);

        /* Display current frequency */
        try {
            double freq = pwm.getFrequencyHz();
            textFreq.setText(String.format(Locale.ENGLISH, "frequency: %.1f Hz", freq));

        } catch (IOException e) {
            Log.e(TAG, "Cannot get PWM frequency");
            Snackbar.make(cardView, "Error getting PWM frequency: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
            e.printStackTrace();
        }

        /* Display current duty cycle */
        try {
            double dc = pwm.getDutyCycle();
            textDutyCycle.setText(String.format(Locale.ENGLISH, "duty cycle: %.1f %%", dc));

        } catch (IOException e) {
            Log.e(TAG, "Cannot get PWM duty cycle");
            Snackbar.make(cardView, "Error getting PWM duty cycle: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
            e.printStackTrace();
        }

        /* Display current polarity */
        try {
            PwmPolarity pol = pwm.getPolarity();
            textPolarity.setText("polarity: " + (pol == PwmPolarity.POLARITY_NORMAL ? "normal" : "inversed"));

        } catch (IOException e) {
            Log.e(TAG, "Cannot get PWM polarity");
            Snackbar.make(cardView, "Error getting PWM polarity: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void setupPwmEnableSwitch() {
        /* Setup initial switch state */
        try {
            Boolean isPwmEnabled = pwm.isEnabled();
            switchEnable.setChecked(isPwmEnabled);
        } catch (IOException e) {
            Log.e(TAG, "Cannot get PWM state");
            Snackbar.make(cardView, "Error getting PWM state: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
            e.printStackTrace();
        }

        /* Setup switch listener */
        switchEnable.setOnCheckedChangeListener((compoundButton, b) -> {
            try {
                pwm.setEnabled(b);
            } catch (IOException e) {
                Log.e(TAG, "Cannot enable PWM");
                Snackbar.make(cardView, "Error enabling PWM: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                switchEnable.setChecked(!b);
                e.printStackTrace();
            }
        });
    }

    private void setupPwmSettingsButton() {
        /* Setup change freq/duty cycle dialog */
        settingsFab.setOnClickListener(view -> {
            FragmentManager fm = getSupportFragmentManager();
            PwmSettingsDialog frag = PwmSettingsDialog.newInstance(pwm);
            frag.show(fm, "dialog_settings");
            frag.setOnPwmSettingsChangedListener(this::updatePwmInfo);
        });
    }

    @Override
    protected void onDestroy() {
        if (pwm != null) {
            try {
                pwm.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }
}


