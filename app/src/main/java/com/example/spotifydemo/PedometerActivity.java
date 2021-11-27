/*
 *  Pedometer - Android App
 *  Copyright (C) 2009 Levente Bagi
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.example.spotifydemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spotifydemo.PaceTracker.PedometerSettings;
import com.example.spotifydemo.PaceTracker.StepService;


public class PedometerActivity extends AppCompatActivity {
	private static final String TAG = "Pedometer";
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private PedometerSettings mPedometerSettings;

    private TextView mStepValueView;
    private TextView mPaceValueView;
    private int mStepValue;
    private int mPaceValue;
    private boolean mQuitting = false; // Set when user selected Quit from menu, can be used by onPause, onStop, onDestroy
    
    /**
     * True, when service is running.
     */
    private boolean mIsRunning;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "[ACTIVITY] onCreate");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        mStepValue = 0;
        mPaceValue = 0;
        
        setContentView(R.layout.activity_pedometer);
    }
    
    @Override
    protected void onStart() {
        Log.i(TAG, "[ACTIVITY] onStart");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "[ACTIVITY] onResume");
        super.onResume();
        
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mPedometerSettings = new PedometerSettings(sharedPref);

        
        // Read from preferences if the service was running on the last onPause
        mIsRunning = mPedometerSettings.isServiceRunning();
        
        // Start the service if this is considered to be an application start (last onPause was long ago)
        if (!mIsRunning && mPedometerSettings.isNewStart()) {
            startStepService();
            bindStepService();
        }
        else if (mIsRunning) {
            bindStepService();
        }
        
        mPedometerSettings.clearServiceRunning();

        mStepValueView = (TextView) findViewById(R.id.txtSteps);
        mPaceValueView = (TextView) findViewById(R.id.txtPace);

    }
    
    @Override
    protected void onPause() {
        Log.i(TAG, "[ACTIVITY] onPause");
        if (mIsRunning) {
            unbindStepService();
        }
        if (mQuitting) {
            mPedometerSettings.saveServiceRunningWithNullTimestamp(mIsRunning);
        }
        else {
            mPedometerSettings.saveServiceRunningWithTimestamp(mIsRunning);
        }

        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "[ACTIVITY] onStop");
        super.onStop();
    }

    protected void onDestroy() {
        Log.i(TAG, "[ACTIVITY] onDestroy");
        super.onDestroy();
    }
    
    protected void onRestart() {
        Log.i(TAG, "[ACTIVITY] onRestart");
        super.onRestart();
    }

    private StepService mService;
    
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = ((StepService.StepBinder)service).getService();

            mService.registerCallback(mCallback);
            mService.reloadSettings();
            
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };
    

    private void startStepService() {
        if (! mIsRunning) {
            Log.i(TAG, "[SERVICE] Start");
            mIsRunning = true;
            startService(new Intent(PedometerActivity.this,
                    StepService.class));
        }
    }
    
    private void bindStepService() {
        Log.i(TAG, "[SERVICE] Bind");
        bindService(new Intent(PedometerActivity.this,
                StepService.class), mConnection, Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
    }

    private void unbindStepService() {
        Log.i(TAG, "[SERVICE] Unbind");
        unbindService(mConnection);
    }
    
    private void stopStepService() {
        Log.i(TAG, "[SERVICE] Stop");
        if (mService != null) {
            Log.i(TAG, "[SERVICE] stopService");
            stopService(new Intent(PedometerActivity.this,
                  StepService.class));
        }
        mIsRunning = false;
    }
    
    private void resetValues(boolean updateDisplay) {
        if (mService != null && mIsRunning) {
            mService.resetValues();                    
        }
        else {
            mStepValueView.setText("0");
            mPaceValueView.setText("0");
            SharedPreferences state = getSharedPreferences("state", 0);
            editor = state.edit();
            if (updateDisplay) {
                editor.putInt("steps", 0);
                editor.putInt("pace", 0);
                editor.commit();
            }
        }
    }

    private static final int MENU_SETTINGS = 8;
    private static final int MENU_QUIT     = 9;

    private static final int MENU_PAUSE = 1;
    private static final int MENU_RESUME = 2;
    private static final int MENU_RESET = 3;
    
//    /* Creates the menu items */
//    public boolean onPrepareOptionsMenu(Menu menu) {
//        menu.clear();
//        if (mIsRunning) {
//            menu.add(0, MENU_PAUSE, 0, R.string.pause)
//            .setIcon(android.R.drawable.ic_media_pause)
//            .setShortcut('1', 'p');
//        }
//        else {
//            menu.add(0, MENU_RESUME, 0, R.string.resume)
//            .setIcon(android.R.drawable.ic_media_play)
//            .setShortcut('1', 'p');
//        }
//        menu.add(0, MENU_RESET, 0, R.string.reset)
//        .setIcon(android.R.drawable.ic_menu_close_clear_cancel)
//        .setShortcut('2', 'r');
//        menu.add(0, MENU_SETTINGS, 0, R.string.settings)
//        .setIcon(android.R.drawable.ic_menu_preferences)
//        .setShortcut('8', 's')
//        .setIntent(new Intent(this, Settings.class));
//        menu.add(0, MENU_QUIT, 0, R.string.quit)
//        .setIcon(android.R.drawable.ic_lock_power_off)
//        .setShortcut('9', 'q');
//        return true;
//    }
//
//    /* Handles item selections */
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case MENU_PAUSE:
//                unbindStepService();
//                stopStepService();
//                return true;
//            case MENU_RESUME:
//                startStepService();
//                bindStepService();
//                return true;
//            case MENU_RESET:
//                resetValues(true);
//                return true;
//            case MENU_QUIT:
//                resetValues(false);
//                unbindStepService();
//                stopStepService();
//                mQuitting = true;
//                finish();
//                return true;
//        }
//        return false;
//    }
//
    // TODO: unite all into 1 type of message
    private StepService.ICallback mCallback = new StepService.ICallback() {
        public void stepsChanged(int value) {
            mHandler.sendMessage(mHandler.obtainMessage(STEPS_MSG, value, 0));
        }
        public void paceChanged(int value) {
            mHandler.sendMessage(mHandler.obtainMessage(PACE_MSG, value, 0));
        }
    };
    
    private static final int STEPS_MSG = 1;
    private static final int PACE_MSG = 2;
    
    private final Handler mHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
            switch (msg.what) {
                case STEPS_MSG:
                    mStepValue = (int)msg.arg1;
                    mStepValueView.setText("" + mStepValue);
                    break;
                case PACE_MSG:
                    mPaceValue = msg.arg1;
                    if (mPaceValue <= 0) { 
                        mPaceValueView.setText("0");
                    }
                    else {
                        mPaceValueView.setText("" + (int)mPaceValue);
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
        
    };
    

}