package com.pelmorex.devicejourney.location;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class LocationUpdateService extends Service {

    private static final String TAG = "LocationUpdateService";

    private final LocationServiceBinder binder = new LocationServiceBinder();
    private FusedLocationProviderClient fusedLocationClient;
    private LocationProvider.LocationProviderListener listener;
    private LocationCallback locationCallback;


    public void setListener(LocationProvider.LocationProviderListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        startForeground(12345678, getNotification());
        super.onCreate();
    }

    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "Location update service started");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        LocationRequest locationRequest = intent.getParcelableExtra(FusedLocationProvider.EXTRA_LOCATION_REQUEST);
        locationCallback = new LocationCallbackImpl();
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        super.onDestroy();
    }


    public class LocationServiceBinder extends Binder {
        public LocationUpdateService getService() {
            return LocationUpdateService.this;
        }
    }

    private Notification getNotification() {

        NotificationChannel channel = new NotificationChannel("channel_01", "LocationChannel", NotificationManager.IMPORTANCE_DEFAULT);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        Notification.Builder builder = new Notification.Builder(getApplicationContext(), "channel_01");
        return builder.build();
    }

    private class LocationCallbackImpl extends LocationCallback {

        public LocationCallbackImpl() {
        }

        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null || listener == null)
                return;

            listener.onUpdateLocation(locationResult.getLocations());
        }
    }
}
