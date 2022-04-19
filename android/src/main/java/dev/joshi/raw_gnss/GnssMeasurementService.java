package dev.joshi.raw_gnss;

import android.Manifest;
import android.app.*;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.Nullable;

import android.os.IBinder;
import io.flutter.plugin.common.PluginRegistry;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.os.Binder;
import android.content.pm.PackageManager;
import io.flutter.plugin.common.MethodChannel;
import android.content.ActivityNotFoundException;
import android.content.Context;

class BackgroundNotification {
    public static final String kDefaultChannelName = "Raw GNSS background service";
    public static final String kDefaultNotificationTitle = "Raw GNSS background service running";
    public static final String kDefaultNotificationIconName = "navigation_empty_icon";

    private final Context context;
    private final String channelId;
    private final int notificationId;
    private final boolean onTapBringToFront = true;
    private NotificationCompat.Builder builder;


    public BackgroundNotification(Context context, String channelId, int notificationId) {
        this.context = context;
        this.channelId = channelId;
        this.notificationId = notificationId;
        builder = new NotificationCompat.Builder(context, channelId)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        updateNotification(/*options,*/ false);
    }

    private int _getDrawableId(String iconName) {
        return context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
    }

    private void updateNotification(
            /*options: NotificationOptions,*/
            Boolean notify
    ) {
        final int iconId =_getDrawableId(kDefaultNotificationIconName);
        builder = builder
                .setContentTitle(kDefaultNotificationTitle)
                .setSmallIcon(iconId)
                .setContentText("subtitle")
                .setSubText("description");

        /*if (options.color != null) {
             builder = builder.setColor(options.color).setColorized(true);
        } else {
             builder = builder.setColor(0).setColorized(false);
        }*/

        if (onTapBringToFront /*options.onTapBringToFront*/) {
            builder = builder.setContentIntent(buildBringToFrontIntent());
        } else {
            builder = builder.setContentIntent(null);
        }

        if (notify) {
            final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(notificationId, builder.build());
        }
    }
    @Nullable
    private PendingIntent buildBringToFrontIntent() {
        final PackageManager pm = context.getPackageManager();
        final Intent intent = pm.getLaunchIntentForPackage(context.getPackageName())
                .setPackage(null)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        if (intent != null) {
           return PendingIntent.getActivity(context, 0, intent, 0);
        } else {
           return null;
        }
    }
    Notification build() {
        //updateChannel(options.channelName)
        return builder.build();
    }
}

public class GnssMeasurementService extends Service implements PluginRegistry.RequestPermissionsResultListener {
    private static final String TAG = "GNSS_MEASUREMENT_SERVICE";
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 641;
    private static final int ONGOING_NOTIFICATION_ID = 75428;
    private static final String CHANNEL_ID = "gnss_measurement_channel" /*+ String.valueOf(getRandomNumber())*/;

    private final LocalBinder binder = new LocalBinder();
    private boolean isForeground = false;
    @Nullable
    private Activity activity = null;
    // Store result until a permission check is resolved
    @Nullable
    private MethodChannel.Result result;
    @Nullable
    private FlutterGnssMeasurement measurement;
    @Nullable
    private BackgroundNotification backgroundNotification;

    public GnssMeasurementService() {
        super();
    }

    @Nullable
    public FlutterGnssMeasurement getMeasurement() {
        return measurement;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind called..");
        return binder;
    }

    @Override
    public boolean onUnbind(@Nullable Intent intent) {
        Log.d(TAG, "Unbinding from location service.");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroying service.");

        disableBackgroundMode();
        backgroundNotification = null;
        measurement = null;

        super.onDestroy();
    }

    /*@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand called..");
        return START_STICKY;
    }*/

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Creating service.");
        /*stream = new GnssMeasurementHandlerImpl(locationManager, getApplicationContext());*/
        measurement = new FlutterGnssMeasurement(getApplicationContext(), null);
        backgroundNotification = new BackgroundNotification(
                getApplicationContext(),
                CHANNEL_ID,
                ONGOING_NOTIFICATION_ID
        );
    }


    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE && permissions.length == 2 &&
                permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION && permissions[1] == Manifest.permission.ACCESS_BACKGROUND_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted, background mode can be enabled
                enableBackgroundMode();
                if (result != null) {
                    result.success(1);
                }
                return true;
            } else {
                if (!shouldShowRequestBackgroundPermissionRationale()) {
                    if (result != null) {
                        result.error("PERMISSION_DENIED_NEVER_ASK",
                                "Background location permission denied forever - please open app settings", null);
                    }
                } else {
                    if (result != null) {
                        result.error("PERMISSION_DENIED", "Background location permission denied", null);
                    }
                }
            }
            result = null;
        }
        return false;
    }

   private boolean shouldShowRequestBackgroundPermissionRationale() {
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
           if (activity != null) {
               return activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
           } else {
               throw new ActivityNotFoundException();
           }
       } else {
           return false;
       }
   }

    public Boolean checkBackgroundPermissions() {
        if (activity == null) {
            Log.wtf(TAG, "Error in checkBackgroundPermissions - Activity is NULL");
            throw new ActivityNotFoundException();
        }
        int locationPermissionState = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        return locationPermissionState == PackageManager.PERMISSION_GRANTED;
    }

    public void requestBackgroundPermissions() {
        if (activity == null) {
            Log.wtf(TAG, "Error in requestBackgroundPermissions - Activity is NULL");
            throw new ActivityNotFoundException();
        }
        final String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION};
        ActivityCompat.requestPermissions(activity, permissions,
                REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    public boolean isInForegroundMode() {
        return isForeground;
    }

    public void enableBackgroundMode() {
        if (isForeground) {
            Log.d(TAG, "Service already in foreground mode.");
        } else {
            Log.d(TAG, "Start service in foreground mode.");

            Notification notification = backgroundNotification.build();
            startForeground(ONGOING_NOTIFICATION_ID, notification);

            isForeground = true;
        }
    }

    public void disableBackgroundMode() {
        Log.d(TAG, "Stop service in foreground.");

        stopForeground(true);
        isForeground = false;
    }

    public void setActivity(@Nullable Activity activity) {
        this.activity = activity;
        measurement.setActivity(activity);
    }
/*
    PluginRegistry.ActivityResultListener getLocationActivityResultListener() {
        return
    }

    val locationRequestPermissionsResultListener: PluginRegistry.RequestPermissionsResultListener?
    get() = location*/

    public PluginRegistry.RequestPermissionsResultListener getServiceRequestPermissionsResultListener() {
        return  this;
    }

    public class LocalBinder extends Binder {
        GnssMeasurementService getService() {
            return GnssMeasurementService.this;
        }
    }
}
