package dev.joshi.raw_gnss;

import android.app.*;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import android.os.IBinder;
import io.flutter.plugin.common.MethodChannel;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import java.util.*;
import android.os.Handler;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.os.Binder;
import android.content.ServiceConnection;

import org.jetbrains.annotations.NotNull;

/** RawGnssPlugin */
public class RawGnssPlugin implements FlutterPlugin, ActivityAware {
  private static final String TAG = "RAW_GNSS_PLUGIN";
  /*private static final String GNSS_MEASUREMENT_CHANNEL_NAME =
          "dev.joshi.raw_gnss/gnss_measurement";*/
  private static final String GNSS_NAVIGATION_MESSAGE_CHANNEL_NAME = "dev.joshi.raw_gnss/gnss_navigation_message";
  private  static  final  String GNSS_ANTENNA_INFO_CHANNEL_NAME = "dev.joshi.raw_gnss/gnss_antenna_info";

  //private EventChannel gnssMeasurementChannel;
  private EventChannel gnssNavigationMessageChannel;
  private EventChannel gnssAntennaInfoChannel;
  @Nullable
  private LocationManager locationManager;
  @Nullable
  private Context context;
  @Nullable
  private ActivityPluginBinding activityBinding;
  @Nullable
  private GnssMeasurementService measurementService;
  @Nullable
  private GnssMeasurementHandlerImpl gnssMeasurementStreamHandler;

  @RequiresApi(api = Build.VERSION_CODES.R)
  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    Log.i(TAG, "onAttachedToEngine called..");

    context = flutterPluginBinding.getApplicationContext();
    setupEventChannels(context, flutterPluginBinding.getBinaryMessenger());

    measurementService = new GnssMeasurementService(/*context, activity*/);
    gnssMeasurementStreamHandler = new GnssMeasurementHandlerImpl(/*locationManager, context*/);
    gnssMeasurementStreamHandler.startListening(flutterPluginBinding.getBinaryMessenger());
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    Log.i(TAG, "onDetachedFromEngine called..");
    teardownEventChannels();
  }

  @RequiresApi(api = Build.VERSION_CODES.R)
  private void setupEventChannels(@NotNull Context context, BinaryMessenger messenger) {
    Log.i(TAG, "setupEventChannels called..");
    
    locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    // gnssMeasurementChannel = new EventChannel(messenger, GNSS_MEASUREMENT_CHANNEL_NAME);
    gnssNavigationMessageChannel = new EventChannel(messenger, GNSS_NAVIGATION_MESSAGE_CHANNEL_NAME);
    gnssAntennaInfoChannel = new EventChannel(messenger, GNSS_ANTENNA_INFO_CHANNEL_NAME);

    /*final GnssMeasurementHandlerImpl gnssMeasurementStreamHandler =
            new GnssMeasurementHandlerImpl(locationManager, context);
    gnssMeasurementChannel.setStreamHandler(gnssMeasurementStreamHandler);*/

    final GnssNavigationMessageHandlerImpl gnssNavigationMessageHandler =
            new GnssNavigationMessageHandlerImpl(locationManager);
    gnssNavigationMessageChannel.setStreamHandler(gnssNavigationMessageHandler);

    final GnssAntennaInfoHandlerImpl gnssAntennaInfoStreamHandler =
            new GnssAntennaInfoHandlerImpl(locationManager, context);
    gnssAntennaInfoChannel.setStreamHandler(gnssAntennaInfoStreamHandler);

  }

  private void teardownEventChannels() {
    //gnssMeasurementChannel.setStreamHandler(null);
    gnssNavigationMessageChannel.setStreamHandler(null);
    gnssAntennaInfoChannel.setStreamHandler(null);

    if (gnssMeasurementStreamHandler != null) {
      gnssMeasurementStreamHandler.stopListening();
      gnssMeasurementStreamHandler = null;
    }
  }

  private void _attachToActivity(@NotNull ActivityPluginBinding binding) {
    activityBinding = binding;
    activityBinding.getActivity().bindService(new Intent(binding.getActivity(), GnssMeasurementService.class), serviceConnection, Context.BIND_AUTO_CREATE);
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    Log.i(TAG, "onAttachedToActivity called..");
    // ContextCompat.startForegroundService(
    //        binding.getActivity(),
    //        new Intent(binding.getActivity(), RawGnssPlugin.class));

    _attachToActivity(binding);
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    Log.i(TAG, "onReattachedToActivityForConfigChanges called..");
    _attachToActivity(binding);
  }

  private void _detachActivity() {
    dispose();

    activityBinding.getActivity().unbindService(serviceConnection);
    activityBinding = null;
  }

  @Override
  public void onDetachedFromActivity() {
    Log.i(TAG, "onDetachedFromActivity called..");
    _detachActivity();
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    Log.i(TAG, "onDetachedFromActivityForConfigChanges called..");
    _detachActivity();
  }

  private final ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      Log.d(TAG, "Service connected: " + name);
      initialize(((GnssMeasurementService.LocalBinder) service).getService());
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      Log.d(TAG, "Service disconnected:" + name);
    }
  };

  private void initialize(@NotNull GnssMeasurementService service) {
    measurementService = service;

    measurementService.setActivity(activityBinding.getActivity());
    activityBinding.addRequestPermissionsResultListener(measurementService.getServiceRequestPermissionsResultListener());
    if (gnssMeasurementStreamHandler != null) {
      gnssMeasurementStreamHandler.setMeasurement(measurementService.getMeasurement());
      enableBackgroundMode();
    }
  }

  protected void dispose() {
    teardownEventChannels();
    if (measurementService != null) {
      activityBinding.removeRequestPermissionsResultListener(measurementService.getServiceRequestPermissionsResultListener());
    }
    //activityBinding.removeRequestPermissionsResultListener(locationService.getLocationRequestPermissionsResultListener());
    //activityBinding.removeActivityResultListener(locationService.getLocationActivityResultListener());
    if (measurementService != null) {
      measurementService.setActivity(null);
      measurementService = null;
    }
  }


  protected void enableBackgroundMode() {
    if (measurementService != null) {
      if (!measurementService.checkBackgroundPermissions()) {
        measurementService.requestBackgroundPermissions();
      }
      try {
        measurementService.enableBackgroundMode();
      }
      catch (Exception e) {
        Log.e(TAG, "Error in enableBackgroundMode. Error message: " + e.getMessage());
      }
    }
  }
}

