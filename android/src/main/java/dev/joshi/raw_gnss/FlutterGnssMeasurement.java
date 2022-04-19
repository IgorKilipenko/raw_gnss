package dev.joshi.raw_gnss;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.*;
import android.content.Intent;
import android.location.GnssClock;
import android.location.GnssMeasurementsEvent;
import android.location.LocationManager;
import android.os.Build;

import androidx.annotation.Nullable;

import io.flutter.plugin.common.PluginRegistry;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import android.content.pm.PackageManager;
import android.content.ActivityNotFoundException;
import android.content.Context;

//import com.google.android.gms.location.LocationRequest;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import io.flutter.plugin.common.EventChannel.EventSink;
import io.flutter.plugin.common.MethodChannel.Result;


public class FlutterGnssMeasurement implements PluginRegistry.RequestPermissionsResultListener, PluginRegistry.ActivityResultListener {
    private static final String TAG = "GNSS_MEASUREMENT";

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private static final int GPS_ENABLE_REQUEST = 0x1001;


    @Nullable
    private Activity activity = null;
    @Nullable
    GnssMeasurementsEvent.Callback gnssMeasurementsCallback;

    private EventSink events;

    // Store result until a permission check is resolved
    public Result result;

    // Store the result for the requestService, used in ActivityResult
    private Result requestServiceResult;

    // Store result until a location is getting resolved
    public Result getMeasurementResult;

    private final LocationManager locationManager;

    private final Handler uiThreadHandler = new Handler(Looper.getMainLooper());

    public FlutterGnssMeasurement(@Nullable Context applicationContext, @Nullable Activity activity) {
        if (applicationContext == null) {
            Log.e(TAG, "applicationContext == null");
            throw new IllegalArgumentException();
        }
        this.activity = activity;
        this.locationManager = (LocationManager) applicationContext.getSystemService(Context.LOCATION_SERVICE);
    }

    public void setEvents(@NotNull EventSink events) {
        this.events = events;
    }

    public void clearEvents() {
        this.events = null;
    }

    @Nullable
    public EventSink getEvents() {
        return events;
    }

    @Nullable
    public Activity getActivity() {
        return activity;
    }

    public LocationManager getLocationManager() {
        return locationManager;
    }


    public void setActivity(@Nullable Activity activity) {
        this.activity = activity;
        if (this.activity != null) {
            createGnssMeasurementCallback();
            // createLocationRequest();
            // buildLocationSettingsRequest();
        } else {
            stopListenGnssMeasurement();
        }
    }

    private void _sendSuccessMessage(Object event) {
        if (events != null) {
            uiThreadHandler.post(() -> events.success(event));
            //events.success(event);
        }
    }

    private void _sendSuccessResult(@Nullable Object result) {
        if (this.result != null) {
            uiThreadHandler.post(() -> this.result.success(result));
            //events.success(event);
        }
    }

    private void _sendErrorResult(String errorCode, String errorMessage, Object errorDetails) {
        if (result != null) {
            uiThreadHandler.post(() -> result.error(errorCode, errorMessage, errorDetails));
            //events.success(event);
        }
    }

    private void _sendError(String errorCode, String errorMessage, Object errorDetails) {
        if (getMeasurementResult != null) {
            uiThreadHandler.post(() -> getMeasurementResult.error(errorCode, errorMessage, errorDetails));
            // getMeasurementResult.error(errorCode, errorMessage, errorDetails);
            getMeasurementResult = null;
        }
        if (events != null) {
            uiThreadHandler.post(() -> events.error(errorCode, errorMessage, errorDetails));
            // events.error(errorCode, errorMessage, errorDetails);
            events = null;
        }
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case GPS_ENABLE_REQUEST:
                if (this.requestServiceResult == null) {
                    return false;
                }
                if (resultCode == Activity.RESULT_OK) {
                    // this.requestServiceResult.success(1);

                } else {
                    // this.requestServiceResult.success(0);
                }
                this.requestServiceResult = null;
                return true;
            case REQUEST_CHECK_SETTINGS:
                if (this.result == null) {
                    return false;
                }
                if (resultCode == Activity.RESULT_OK) {
                    startRequestingGnssMeasurement();
                    return true;
                }

                _sendErrorResult("SERVICE_STATUS_DISABLED", "Failed to get location. Location services disabled", null);
                this.result = null;
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        return onRequestPermissionsResultHandler(requestCode, permissions, grantResults);
    }

    public boolean checkPermissions() {
        if (this.activity == null) {
            _sendErrorResult("MISSING_ACTIVITY", "You should not checkPermissions activation outside of an activity.", null);
            throw new ActivityNotFoundException();
        }
        int locationPermissionState = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return locationPermissionState == PackageManager.PERMISSION_GRANTED;
    }

    public boolean onRequestPermissionsResultHandler(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE && permissions.length == 1
                && permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Checks if this permission was automatically triggered by a location request
                if (getMeasurementResult != null || events != null) {
                    startRequestingGnssMeasurement();
                }

                _sendSuccessResult(1);

            } else {
                if (!shouldShowRequestPermissionRationale()) {
                    _sendError("PERMISSION_DENIED_NEVER_ASK",
                            "Location permission denied forever - please open app settings", null);
                    _sendSuccessResult(2);
                } else {
                    _sendError("PERMISSION_DENIED", "Location permission denied", null);
                    _sendSuccessResult(0);
                }
            }
            result = null;
            return true;
        }
        return false;
    }

    public boolean shouldShowRequestPermissionRationale() {
        if (activity == null) {
            return false;
        }
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public void requestPermissions() {
        if (this.activity == null) {
            _sendErrorResult("MISSING_ACTIVITY", "You should not requestPermissions activation outside of an activity.", null);
            throw new ActivityNotFoundException();
        }
        if (checkPermissions()) {
            _sendSuccessResult(1);
            return;
        }
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    public void stopListenGnssMeasurement() {
        if (gnssMeasurementsCallback != null) {
            Log.d(TAG, "stopListenGnssMeasurement...");
            locationManager.unregisterGnssMeasurementsCallback(gnssMeasurementsCallback);
        }
        gnssMeasurementsCallback = null;
    }

    @SuppressLint("MissingPermission")
    public void startRequestingGnssMeasurement() {
        if (this.activity == null) {
            _sendErrorResult("MISSING_ACTIVITY", "You should not requestGnssMeasurement activation outside of an activity.", null);
            throw new ActivityNotFoundException();
        }
        requestPermissions();
        if (!locationManager.registerGnssMeasurementsCallback(gnssMeasurementsCallback, uiThreadHandler)) {
            Log.e(TAG, "Registered GnssMeasurementsCallback with error.");
            _sendError("UNEXPECTED_ERROR", "GnssMeasurementsCallback not registered", null);
            stopListenGnssMeasurement();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void createGnssMeasurementCallback() {
        stopListenGnssMeasurement();
        gnssMeasurementsCallback = new GnssMeasurementsEvent.Callback() {
            @Override
            public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
                Log.d(TAG, "onGnssMeasurementsReceived: " + eventArgs.getMeasurements().size());

                super.onGnssMeasurementsReceived(eventArgs);

                HashMap<String, Object> resultMap = _parseData(eventArgs);

                if (events != null ) {
                    _sendSuccessMessage(resultMap);
                }
            }

            @Override
            public void onStatusChanged(int status) {
                Log.d(TAG, "onStatusChanged: " + status);
                super.onStatusChanged(status);
            }


            @NotNull
            private HashMap<String, Object> _parseData(@NotNull GnssMeasurementsEvent eventArgs) {
                HashMap<String, Object> resultMap = new HashMap<>();
                resultMap.put("contents", eventArgs.describeContents());
                resultMap.put("string", eventArgs.toString());

                GnssClock clock = eventArgs.getClock();
                HashMap<String, Object> clockMap = _parseClockData(clock);

                resultMap.put("clock", clockMap);

                Collection<android.location.GnssMeasurement> measurements = eventArgs.getMeasurements();
                ArrayList<HashMap<String, Object>> measurementsMapList = _parseMeasurementsData(measurements);

                resultMap.put("measurements", measurementsMapList);

                return  resultMap;
            }

            @NotNull
            HashMap<String, Object> _parseClockData(@NotNull GnssClock clock) {
                HashMap<String, Object> clockMap = new HashMap<>();

                clockMap.put("contents", clock.describeContents());
                clockMap.put("biasNanos", clock.getBiasNanos());
                clockMap.put("biasUncertaintyNanos", clock.getBiasUncertaintyNanos());
                clockMap.put("driftNanosPerSecond", clock.getDriftNanosPerSecond());
                clockMap.put("driftUncertaintyNanosPerSecond", clock.getDriftUncertaintyNanosPerSecond());
                clockMap.put("fullBiasNanos", clock.getFullBiasNanos());
                clockMap.put("hardwareClockDiscontinuityCount", clock.getHardwareClockDiscontinuityCount());
                clockMap.put("leapSecond", clock.getLeapSecond());
                clockMap.put("timeNanos", clock.getTimeNanos());
                clockMap.put("timeUncertaintyNanos", clock.getTimeUncertaintyNanos());

                return  clockMap;
            }

            @NotNull
            ArrayList<HashMap<String, Object>> _parseMeasurementsData(@NotNull Collection<android.location.GnssMeasurement> measurements) {

                ArrayList<HashMap<String, Object>> measurementsMapList  = new ArrayList<>();

                for (int i = 0; i < measurements.size(); ++i) {
                    HashMap<String, Object> map = new HashMap<>();
                    android.location.GnssMeasurement measurement = (android.location.GnssMeasurement) measurements.toArray()[i];

                    map.put("contents", measurement.describeContents());
                    map.put("accumulatedDeltaRangeMeters", measurement.getAccumulatedDeltaRangeMeters());
                    map.put("accumulatedDeltaRangeState", measurement.getAccumulatedDeltaRangeState());
                    map.put("accumulatedDeltaRangeUncertaintyMeters", measurement.getAccumulatedDeltaRangeUncertaintyMeters());
                    if (measurement.hasAutomaticGainControlLevelDb()) {
                        map.put("automaticGainControlLevelDb", measurement.getAutomaticGainControlLevelDb());
                    }
                    if (measurement.hasCarrierFrequencyHz()) {
                        map.put("carrierFrequencyHz", measurement.getCarrierFrequencyHz());
                    }
                    map.put("cn0DbHz", measurement.getCn0DbHz());
                    map.put("constellationType", measurement.getConstellationType());
                    map.put("multipathIndicator", measurement.getMultipathIndicator());
                    map.put("pseudorangeRateMetersPerSecond", measurement.getPseudorangeRateMetersPerSecond());
                    map.put("pseudorangeRateUncertaintyMetersPerSecond", measurement.getPseudorangeRateUncertaintyMetersPerSecond());
                    map.put("receivedSvTimeNanos", measurement.getReceivedSvTimeNanos());
                    map.put("receivedSvTimeUncertaintyNanos", measurement.getReceivedSvTimeUncertaintyNanos());
                    if (measurement.hasSnrInDb()) {
                        map.put("snrInDb", measurement.getSnrInDb());
                    }
                    map.put("state", measurement.getState());
                    map.put("svid", measurement.getSvid());
                    map.put("timeOffsetNanos", measurement.getTimeOffsetNanos());
                    map.put("string", measurement.toString());
                    // Add
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        map.put("accumulatedDeltaRangeMeters", measurement.getAccumulatedDeltaRangeMeters());
                        map.put("accumulatedDeltaRangeState", measurement.getAccumulatedDeltaRangeState());
                        map.put("accumulatedDeltaRangeUncertaintyMeters", measurement.getAccumulatedDeltaRangeUncertaintyMeters());
                        map.put("basebandCn0DbHz", measurement.getBasebandCn0DbHz());
                        if (measurement.hasFullInterSignalBiasNanos()) {
                            map.put("fullInterSignalBiasNanos", measurement.getFullInterSignalBiasNanos());
                        }
                        if (measurement.hasFullInterSignalBiasUncertaintyNanos()) {
                            map.put("fullInterSignalBiasUncertaintyNanos", measurement.getFullInterSignalBiasUncertaintyNanos());
                        }
                    }

                    measurementsMapList.add(map);
                }

                Log.d(TAG, "_parseMeasurementsData parsed measurements: " + measurementsMapList.size());

                return  measurementsMapList;
            }
        };
    }
}
