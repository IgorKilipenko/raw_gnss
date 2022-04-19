package dev.joshi.raw_gnss;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GnssMeasurementsEvent;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.EventSink;

public class GnssMeasurementHandlerImpl implements EventChannel.StreamHandler {
    private static final String TAG = "GNSS_MEASURE_STREAM_HANDLER";
    private static final String GNSS_MEASUREMENT_CHANNEL_NAME =
            "dev.joshi.raw_gnss/gnss_measurement";

    @Nullable
    private EventChannel channel;
    @Nullable
    private FlutterGnssMeasurement measurement;

    public GnssMeasurementHandlerImpl() { }

    void setMeasurement(FlutterGnssMeasurement measurement) {
        this.measurement = measurement;
    }

    public void startListening(BinaryMessenger messenger) {
        Log.d(TAG, "startListening...");
        if (channel != null) {
            Log.wtf(TAG, "Setting a method call handler before the last was disposed.");
            stopListening();
        }

        channel = new EventChannel(messenger, GNSS_MEASUREMENT_CHANNEL_NAME);
        channel.setStreamHandler(this);
    }

    public void stopListening() {
        Log.d(TAG, "stopListening...");
        if (channel == null) {
            Log.d(TAG, "Tried to stop listening when no MethodChannel had been initialized.");
            return;
        }

        channel.setStreamHandler(null);
        channel = null;
    }


    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        Log.d(TAG, "onListen...");

        if (measurement == null) {
            final String msg = "FlutterGnssMeasurement instance is null. Use setMeasurement to set instance value before onListen.";
            Log.wtf(TAG, msg);
            events.error("NO_MEASUREMENT_INSTANCE", msg, null);
            return;
        }
        if (measurement.getActivity() == null) {
            final String msg = "Activity is NULL";
            Log.wtf(TAG, msg);
            events.error("NO_ACTIVITY", msg, null);
            return;
        }
        measurement.setEvents(events);
        measurement.startRequestingGnssMeasurement();
    }

    @Override
    public void onCancel(Object arguments) {
        Log.d(TAG, "onCancel...");
        if (measurement != null) {
            measurement.stopListenGnssMeasurement();
            measurement.clearEvents();
        }
    }


}
