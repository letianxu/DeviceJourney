package com.pelmorex.devicejourney;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.pelmorex.devicejourney.location.FusedLocationProvider;
import com.pelmorex.devicejourney.location.LocationProvider;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationProvider.LocationProviderListener {

    private final static String TAG = "MapsActivity";
    private final double ACCURACY = 0.0001;

    private GoogleMap map;
    private TextView locationUpdatesResultView;
    private LocationProvider locationProvider;
    private Location lastLocation;
    private List<Location> locationTrack = new ArrayList<>();
    private Polyline trackingPath;
    private Marker currentMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        locationUpdatesResultView = (TextView)findViewById(R.id.location_updates_result);

        // request the last device location
        locationProvider = FusedLocationProvider.getInstance();
        locationProvider.init(this);
        locationProvider.setListener(this);
        locationProvider.requestLastLocation();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        setupActivityTransition();
    }

    @Override
    public void onDestroy() {
        locationProvider.unregisterBackgroundLocationUpdate();
        super.onDestroy();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        if (lastLocation != null) {
            zoomMapTo(lastLocation);
        }
    }

    @Override
    public void onLastLocationAvailable(Location location) {
        lastLocation = location;
        locationTrack.add(lastLocation);
        if (map != null) {
            zoomMapTo(lastLocation);
        }
    }

    @Override
    public void onUpdateLocation(List<Location> locationResults) {
        locationUpdatesResultView.setText(getLocationResultText(locationResults));
        if (locationResults.size() == 1 && isLocationRoughlySame(locationResults.get(0), lastLocation)) {
            Log.d(TAG, "Location doesn't change so don't update");
            zoomMapTo(lastLocation);
            return;
        }

        locationTrack.addAll(locationResults);
        lastLocation = locationResults.get(locationResults.size() - 1);

        addPolyline();
    }


    private void addPolyline() {
        Log.d(TAG, "Location is updated and path is updating");

        if (locationTrack.size() == 2) {
            Location fromLocation = locationTrack.get(0);
            Location toLocation = locationTrack.get(1);

            LatLng from = new LatLng(fromLocation.getLatitude(), fromLocation.getLongitude());

            LatLng to = new LatLng(toLocation.getLatitude(), toLocation.getLongitude());

            trackingPath = map.addPolyline(new PolylineOptions().add(from, to)
                    .color(Color.parseColor("#801B60FE")).geodesic(true));

        } else if (locationTrack.size() > 2) {
            Location toLocation = locationTrack.get(locationTrack.size() - 1);
            LatLng to = new LatLng(toLocation.getLatitude(), toLocation.getLongitude());

            List<LatLng> points = trackingPath.getPoints();
            points.add(to);

            trackingPath.setPoints(points);
        }
    }

    private void zoomMapTo(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (currentMarker == null) {
            currentMarker = map.addMarker(new MarkerOptions().position(latLng).title("Current Location"));
        } else {
            currentMarker.setPosition(latLng);
        }

        try {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.5f));
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }

    }

    private boolean isLocationRoughlySame(Location loc1, Location loc2) {
        if (Math.abs(loc1.getLatitude() - loc2.getLatitude()) < ACCURACY &&
                Math.abs(loc1.getLongitude() - loc2.getLongitude()) < ACCURACY)
            return true;

        return false;
    }

    private String getLocationResultText(List<Location> locationResult) {
        StringBuilder sb = new StringBuilder();
        for (Location location : locationResult) {
            sb.append("(");
            sb.append(location.getLatitude());
            sb.append(", ");
            sb.append(location.getLongitude());
            sb.append(")");
            sb.append("\n");
        }
        return sb.toString();
    }

    private void setupActivityTransition() {
        List<ActivityTransition> transitions = new ArrayList<>();

        transitions.add(new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.IN_VEHICLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build());

        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());

        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());

        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());

        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());

        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());

        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());

        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());

        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());

        ActivityTransitionRequest request = new ActivityTransitionRequest(transitions);

        Task<Void> task =
                ActivityRecognition.getClient(this).requestActivityTransitionUpdates(request, getTransitionIntent());

        task.addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "Request transition update success");
                    }
                }
        );

        task.addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.d(TAG, "Request transition update failed");
                    }
                }
        );

    }

    private PendingIntent getTransitionIntent() {
        Intent intent = new Intent(this, TransitionBroadcastReceiver.class);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static class TransitionBroadcastReceiver extends BroadcastReceiver {
        private static final String TAG = "TBroadcastReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ActivityTransitionResult.hasResult(intent)) {
                ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
                for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                    if ((event.getActivityType() == DetectedActivity.IN_VEHICLE || event.getActivityType() == DetectedActivity.ON_BICYCLE ||
                            event.getActivityType() == DetectedActivity.RUNNING || event.getActivityType() == DetectedActivity.WALKING) &&
                            event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                        Log.d(TAG, "Moving activity detected so enable the location update");
                        FusedLocationProvider.getInstance().registerBackgroundLocationUpdate(new LocationRequest()
                                .setInterval(10000)
                                .setFastestInterval(5000)
                                .setMaxWaitTime(5000)
                                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY));
                    }
                    else if ((event.getActivityType() == DetectedActivity.IN_VEHICLE || event.getActivityType() == DetectedActivity.ON_BICYCLE ||
                            event.getActivityType() == DetectedActivity.RUNNING || event.getActivityType() == DetectedActivity.WALKING) &&
                            event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                        Log.d(TAG, "Still activity detected so disable the location update");
                        FusedLocationProvider.getInstance().unregisterBackgroundLocationUpdate();
                    }

                    else if (event.getActivityType() == DetectedActivity.STILL && event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                        Log.d(TAG, "Still activity detected so disable the location update");
                        FusedLocationProvider.getInstance().unregisterBackgroundLocationUpdate();
                    }
                }
            }
        }
    }
}
