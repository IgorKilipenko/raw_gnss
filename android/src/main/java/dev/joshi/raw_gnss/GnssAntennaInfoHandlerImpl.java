package dev.joshi.raw_gnss;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GnssAntennaInfo;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.sql.Array;
import java.util.HashMap;
import java.util.List;

import io.flutter.plugin.common.EventChannel;

public class GnssAntennaInfoHandlerImpl implements EventChannel.StreamHandler {
    private static final String TAG = "GNSS_ANTENNA_INFO";

    LocationManager locationManager;
    private Context _context;
    GnssAntennaInfo.Listener listener;
    private final Handler uiThreadHandler = new Handler(Looper.getMainLooper());

    private Boolean _hasPermissions = false;

    GnssAntennaInfoHandlerImpl(LocationManager manager, Context context) {
        locationManager = manager;
        _context = context;
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        Log.d(TAG, "onListen");
        listener = createSensorEventListener(events);
        _hasPermissions = ActivityCompat.checkSelfPermission(_context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        if (_hasPermissions) {
            Log.w(TAG, "Do't has permission - ACCESS_FINE_LOCATION");
            listener = null;
            return;
        }
        try {
            locationManager.registerAntennaInfoListener(_context.getMainExecutor(), listener);
        } catch (Exception e){
            Log.e(TAG, e.getMessage());
            events.error("REGISTER_ERROR", e.getMessage(), e);
            listener = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void onCancel(Object arguments) {
        Log.d(TAG, "onCancel");
        if (listener != null) {
            Log.d(TAG, "Stopping antenna updates");
            locationManager.unregisterAntennaInfoListener(listener);
            listener = null;
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.R)
    GnssAntennaInfo.Listener createSensorEventListener(final EventChannel.EventSink events) {
        return new GnssAntennaInfo.Listener() {
            @Override
            public void onGnssAntennaInfoReceived(@NonNull List<GnssAntennaInfo> gnssAntennaInfos) {
                Log.d(TAG, "onGnssAntennaInfoReceived: " + gnssAntennaInfos.toString());

                HashMap<String, Object> resultMap = new HashMap<>();
                HashMap<String, Object> infoMap = new HashMap<>();
                for (int i=0; i < gnssAntennaInfos.size(); i++) {
                    final GnssAntennaInfo info = gnssAntennaInfos.get(i);
                    infoMap.put("id", i);
                    infoMap.put("carrierFrequencyMHz", info.getCarrierFrequencyMHz());
                    infoMap.put("PhaseCenterOffset", _phaseCenterOffsetToMap(info.getPhaseCenterOffset()));
                    resultMap.put("antenna#" + i, infoMap);
                }

                uiThreadHandler.post(() -> events.success(resultMap));
            }
        };
    }

    private HashMap<String, Double> _phaseCenterOffsetToMap(GnssAntennaInfo.PhaseCenterOffset phaseCenterOffset) {
        HashMap<String, Double> resultMap = new HashMap<String, Double>();
        resultMap.put("xOffsetMm", phaseCenterOffset.getXOffsetMm());
        resultMap.put("xOffsetUncertaintyMm", phaseCenterOffset.getXOffsetUncertaintyMm());
        resultMap.put("yOffsetMm", phaseCenterOffset.getYOffsetMm());
        resultMap.put("yOffsetUncertaintyMm", phaseCenterOffset.getYOffsetUncertaintyMm());
        resultMap.put("zOffsetMm", phaseCenterOffset.getZOffsetMm());
        resultMap.put("zOffsetUncertaintyMm", phaseCenterOffset.getZOffsetUncertaintyMm());

        return  resultMap;
    }

    public class PhaseCenterOffset {
        final double xOffsetMm;
        final double yOffsetMm;
        final double zOffsetMm;
        final double xOffsetUncertaintyMm;
        final double yOffsetUncertaintyMm;
        final double zOffsetUncertaintyMm;

        PhaseCenterOffset(GnssAntennaInfo.PhaseCenterOffset phaseCenterOffset) {
            xOffsetMm = phaseCenterOffset.getXOffsetMm();
            yOffsetMm = phaseCenterOffset.getYOffsetMm();
            zOffsetMm = phaseCenterOffset.getZOffsetMm();

            xOffsetUncertaintyMm = phaseCenterOffset.getXOffsetUncertaintyMm();
            yOffsetUncertaintyMm = phaseCenterOffset.getYOffsetUncertaintyMm();
            zOffsetUncertaintyMm = phaseCenterOffset.getZOffsetUncertaintyMm();
        }

        HashMap<String, Double> toMap() {
            HashMap<String, Double> resultMap = new HashMap<String, Double>();
            resultMap.put("xOffsetMm", xOffsetMm);
            resultMap.put("xOffsetUncertaintyMm", xOffsetUncertaintyMm);
            resultMap.put("yOffsetMm", yOffsetMm);
            resultMap.put("yOffsetUncertaintyMm", yOffsetUncertaintyMm);
            resultMap.put("zOffsetMm", zOffsetMm);
            resultMap.put("zOffsetUncertaintyMm", zOffsetUncertaintyMm);

            return  resultMap;
        }

        @Override
        public String toString() {
            return "PhaseCenterOffset{"
                    + "OffsetXMm=" + xOffsetMm + " +/-" + xOffsetUncertaintyMm
                    + ", OffsetYMm=" + yOffsetMm + " +/-" + yOffsetUncertaintyMm
                    + ", OffsetZMm=" + zOffsetMm + " +/-" + zOffsetUncertaintyMm
                    + '}';
        }
    }
}


