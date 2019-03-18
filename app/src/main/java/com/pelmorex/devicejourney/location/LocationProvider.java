package com.pelmorex.devicejourney.location;

import android.content.Context;
import android.location.Location;

import com.google.android.gms.location.LocationRequest;

import java.util.List;

public interface LocationProvider {

    interface LocationProviderListener {

        void onLastLocationAvailable(Location location);
        void onUpdateLocation(List<Location> locationResults);
    }


    void init(Context context);
    void setListener(LocationProviderListener listener);
    void requestLastLocation();
    void registerForegroundLocationUpdate(LocationRequest request);
    void unregisterForegroundLocationUpdate();
    void registerBackgroundLocationUpdate(LocationRequest request);
    void unregisterBackgroundLocationUpdate();

}
