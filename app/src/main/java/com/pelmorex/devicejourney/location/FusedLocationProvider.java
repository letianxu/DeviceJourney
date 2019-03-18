package com.pelmorex.devicejourney.location;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

@SuppressLint("MissingPermission")
public class FusedLocationProvider implements LocationProvider, ServiceConnection {

    private static final String TAG = "FusedLocationProvider";
    public static final String EXTRA_LOCATION_REQUEST = "extra_location_request";

    private Context context;
    private LocationProviderListener listener;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private PendingIntent locationUpdateIntent;
    private Service backgroundLocationUpdateService;


    private static final class SingletonHolder {
        private static final FusedLocationProvider INSTANCE = new FusedLocationProvider();
    }


    private FusedLocationProvider() {

    }

    public static FusedLocationProvider getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public void init(Context context) {
        this.context = context;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @Override
    public void setListener(LocationProviderListener listener) {
        this.listener = listener;
    }


    @Override
    public void requestLastLocation() {

        fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location == null || listener == null)
                    return;

                listener.onLastLocationAvailable(location);
            }
        });
    }

    @Override
    public void registerForegroundLocationUpdate(LocationRequest request) {
        // do not request again, if it already requested
        if (locationCallback == null) {
            locationCallback = new LocationCallbackImpl();
            fusedLocationClient.requestLocationUpdates(request, locationCallback, null);
        }

    }

    @Override
    public void unregisterForegroundLocationUpdate() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
    }

    @Override
    public void registerBackgroundLocationUpdate(LocationRequest request) {
        if (backgroundLocationUpdateService == null) {
            Log.d(TAG, "background location update is registered");
            startBackgroundLocationUpdateService(request);
        }
        /*if (locationUpdateIntent == null) {
            locationUpdateIntent = getLocationUpdateIntentFromBroadcast();
            fusedLocationClient.requestLocationUpdates(request, locationUpdateIntent);
        }*/
    }

    @Override
    public void unregisterBackgroundLocationUpdate() {
        if (backgroundLocationUpdateService != null) {
            Log.d(TAG, "background location update is unregistered");
            Intent intent = new Intent(context, LocationUpdateService.class);
            context.unbindService(this);
            context.stopService(intent);
            backgroundLocationUpdateService = null;
        }
        /*if (locationUpdateIntent != null) {
            fusedLocationClient.removeLocationUpdates(locationUpdateIntent);
            locationUpdateIntent = null;
        }*/
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        String className = name.getClassName();
        if (className.endsWith("LocationUpdateService")) {
            backgroundLocationUpdateService = ((LocationUpdateService.LocationServiceBinder) service).getService();
            ((LocationUpdateService) backgroundLocationUpdateService).setListener(listener);
            Log.d(TAG, "Background location update service is ready");
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        if (name.getClassName().endsWith("LocationUpdateService")) {
            backgroundLocationUpdateService = null;
            Log.d(TAG, "Background location update service is disconnected");
        }
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

    private void startBackgroundLocationUpdateService(LocationRequest request) {
        Intent intent = new Intent(context, LocationUpdateService.class);
        intent.putExtra(EXTRA_LOCATION_REQUEST, request);
        context.startService(intent);
        context.bindService(intent, this, Context.BIND_AUTO_CREATE);

    }

    private PendingIntent getLocationUpdateIntentFromBroadcast() {
        Intent intent = new Intent(context, LocationUpdateBroadcastReceiver.class);
        intent.setAction(LocationUpdateBroadcastReceiver.ACTION_PROCESS_UPDATES);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
