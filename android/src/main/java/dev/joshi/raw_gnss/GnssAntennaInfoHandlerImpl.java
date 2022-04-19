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
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.sql.Array;
import java.util.HashMap;
import java.util.List;
import android.app.Activity;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.BinaryMessenger;

public class GnssAntennaInfoHandlerImpl implements EventChannel.StreamHandler {
    private static final String TAG = "GNSS_ANTENNA_INFO";
    private  static  final  String GNSS_ANTENNA_INFO_CHANNEL_NAME = "dev.joshi.raw_gnss/gnss_antenna_info";

    private LocationManager locationManager;
    private Context _context;
    private GnssAntennaInfo.Listener listener;
    private final Handler uiThreadHandler = new Handler(Looper.getMainLooper());

    private Boolean _hasPermissions = false;
    @Nullable
    private EventChannel channel = null;

    GnssAntennaInfoHandlerImpl(LocationManager manager, Context context) {
        locationManager = manager;
        _context = context;
    }

    public void startListening(BinaryMessenger messenger) {
        if (channel != null) {
            Log.wtf(TAG, "Setting a method call handler before the last was disposed.");
            stopListening();
        }

        channel = new EventChannel(messenger, GNSS_ANTENNA_INFO_CHANNEL_NAME);
        channel.setStreamHandler(this);
    }

    public void stopListening() {
        if (channel == null) {
            Log.d(TAG, "Tried to stop listening when no MethodChannel had been initialized.");
            return;
        }

        channel.setStreamHandler(null);
        channel = null;
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        Log.d(TAG, "onListen");
        listener = createSensorEventListener(events);
        _hasPermissions = /*ActivityCompat*/_context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        if (_hasPermissions) {
            final String msg = "Do't has permission - ACCESS_FINE_LOCATION";
            Log.w(TAG, msg);
            listener = null;
            events.error("PERMISSION_ERROR", msg, null);
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

                final HashMap<String, Object> resultMap = new HashMap<>();

                for (int i=0; i < gnssAntennaInfos.size(); i++) {
                    final HashMap<String, Object> infoMap = new HashMap<>();

                    final GnssAntennaInfo info = gnssAntennaInfos.get(i);
                    infoMap.put("id", i);
                    infoMap.put("contents", info.describeContents());
                    infoMap.put("carrierFrequencyMHz", info.getCarrierFrequencyMHz());
                    infoMap.put("phaseCenterOffset", _phaseCenterOffsetToMap(info.getPhaseCenterOffset()));
                    infoMap.put("phaseCenterVariationCorrections", _correctionsToMap(info.getPhaseCenterVariationCorrections()));
                    infoMap.put("signalGainCorrections", _correctionsToMap(info.getSignalGainCorrections()));
                    infoMap.put("string", info.toString());
                    
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

    private  HashMap<String, Object> _correctionsToMap(GnssAntennaInfo.SphericalCorrections corrections) {
        HashMap<String, Object> resultMap = new HashMap<String, Object>();
        //resultMap.put("correctionsArray", corrections.getCorrectionsArray());
        //resultMap.put("correctionUncertaintiesArray", corrections.getCorrectionUncertaintiesArray());
        resultMap.put("deltaPhi", corrections.getDeltaPhi());
        resultMap.put("deltaTheta", corrections.getDeltaTheta());

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


