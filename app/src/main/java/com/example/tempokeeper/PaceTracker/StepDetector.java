package com.example.tempokeeper.PaceTracker;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.ArrayList;

/**
 * Source code repository: https://github.com/bagilevi/android-pedometer
 * @author Levente Bagi
 * Detects steps and notifies all listeners (that implement StepListener).
 */

public class StepDetector implements SensorEventListener
{
    private final static String TAG = "StepDetector";
    private float   mLimit = 10;
    private float   mLastValues[] = new float[3*2];
    private float   mScale[] = new float[2];
    private float   mYOffset;

    private float   mLastDirections[] = new float[3*2];
    private float   mLastExtremes[][] = { new float[3*2], new float[3*2] };
    private float   mLastDiff[] = new float[3*2];
    private int     mLastMatch = -1;

    private ArrayList<StepListener> mStepListeners = new ArrayList<StepListener>();

    public StepDetector() {
        int h = 480; // TODO: remove this constant
        mYOffset = h * 0.5f;
        mScale[0] = - (h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
        mScale[1] = - (h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));
    }

    public void setSensitivity(float sensitivity) {
        mLimit = sensitivity;
    }

    public void addStepListener(StepListener sl) {
        mStepListeners.add(sl);
    }

    // Detect steps using the accelerometer
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        synchronized (this) {
                int j = (sensor.getType() == Sensor.TYPE_ACCELEROMETER) ? 1 : 0;
                if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    float vSum = 0;
                    for (int i=0 ; i<3 ; i++) {
                        final float v = mYOffset + event.values[i] * mScale[j];
                        vSum += v;
                    }
                    int k = 0;
                    float v = vSum / 3;

                    // Is the direction up or down
                    float direction = (v > mLastValues[k] ? 1 : (v < mLastValues[k] ? -1 : 0));

                    // Direction has changed if it is -(lastDirection)
                    if (direction == - mLastDirections[k]) {
                        int extType = (direction > 0 ? 0 : 1); // minumum or maximum?
                        mLastExtremes[extType][k] = mLastValues[k];
                        float diff = Math.abs(mLastExtremes[extType][k] - mLastExtremes[1 - extType][k]);

                        if (diff > mLimit) {

                            boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff[k]*2/3);
                            boolean isPreviousLargeEnough = mLastDiff[k] > (diff/3);
                            boolean isNotContra = (mLastMatch != 1 - extType);

                            // meets the conditions to be one "step", call stepListener.onStep()
                            if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough && isNotContra) {
                                Log.i(TAG, "step");
                                for (StepListener stepListener : mStepListeners) {
                                    stepListener.onStep();
                                }
                                mLastMatch = extType;
                            }
                            else {
                                mLastMatch = -1;
                            }
                        }
                        mLastDiff[k] = diff;
                    }
                    mLastDirections[k] = direction;
                    mLastValues[k] = v;
                }
            }

    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

}
