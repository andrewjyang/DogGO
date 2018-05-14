/**
 * WalkActivity displays dog walker's current location and live updates Firebase his/her location
 * CPSC 312-02, Fall 2017
 * Programming Assignment Final Project
 *
 * @author Kurt Lamon, Andrew Yang
 * @version v1.0 12/8/17
 */
package com.kurtlemon.doggo3;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class WalkActivity extends FragmentActivity implements OnMapReadyCallback {

    // Location request code for Google Maps.
    private static final int LOCATION_REQUEST_CODE = 1;

    // User ID is used to identify the individual user to the database.
    private String userID = "";

    // Google Maps fields.
    private GoogleMap mMap;

    // Location services fields.
    private FusedLocationProviderClient mFusedLocationProviderClient;

    // Firebase Database Fields.
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;

    /** onCreate() runs when the WalkActivity is started.
     *
     *  Sets up map and location service functionality, initializes user variables, sets up database
     *      fields for later use.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walk);

        // Gets the necessary information from the previous activity.
        Intent intent = getIntent();
        userID = intent.getStringExtra("userID");

        // Establishes the location provider client.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initializes the Firebase database fields for later use.
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference().child("dogLocation" + userID);
    }


    /** onMapReady() runs when the map is created and available
     *
     *  Sets up the map and asks for necessary permissions to run map & location services.
     *
     * @param googleMap
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Checking permissions.
        if(ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED){
            // The user has allowed location permission.
            mMap.setMyLocationEnabled(true);
        }else{
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                // The user has not allowed location permission.
                requestPermissions(new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_REQUEST_CODE);
            }
        }

        // Set up last known location services and location updates.
        setUpLastKnownLocation();
        setUpUserLocationUpdates();
    }

    /** Runs after the user has been asked for location permissions. If successful, set up the last
     *      known location and location updates.
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == LOCATION_REQUEST_CODE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                // Permission for location services has been granted
                // Set up last known location services and location updates
                setUpLastKnownLocation();
                setUpUserLocationUpdates();
            }
        }
    }

    /** Sets up information and recollection of last known location upon starting the activity
     *
     */
    private void setUpLastKnownLocation(){
        // Checks permissions.
        if(ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            // The user has not given permissions.
            ActivityCompat.requestPermissions(this, new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }else{
            // The user has given permission
            // Use the location services client to get last known location.
            Task<Location> locationTask = mFusedLocationProviderClient.getLastLocation();
            locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if(location != null){
                        LatLng userLatLng = new LatLng(location.getLatitude(),
                                location.getLongitude());
                    }
                }
            });
        }
    }

    /** Sets up live updates for the user location.
     *
     */
    private void setUpUserLocationUpdates(){
        final LocationRequest locationRequest = new LocationRequest();
        // Set location request intervals.
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);

        // Use the location services client to update the user's current location.
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                LocationCallback locationCallback = new LocationCallback(){
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        for (Location location : locationResult.getLocations()){
                            // Get the user's location and zoom to it.
                            LatLng latLng = new LatLng(location.getLatitude(),
                                    location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.0f));

                            // Create a DogLocation object with the user's location and push it to
                            //  the database.
                            DogLocation dogLocation = new DogLocation(location.getLatitude(),
                                    location.getLongitude(), userID);
                            databaseReference.setValue(dogLocation);
                        }
                    }
                };

                // Check permissions.
                if (ActivityCompat.checkSelfPermission(WalkActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED){
                    // Permission not granted.
                    ActivityCompat.requestPermissions(WalkActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_REQUEST_CODE);
                } else {
                    // Permission is granted.
                    mFusedLocationProviderClient.requestLocationUpdates(locationRequest,
                            locationCallback, null);
                }
            }
        });
    }

    /** onDestroy() runs when the activity is ending usually.
     *
     *  Remove the current marker from the database so the user isn't being tracked any more.
     *
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        databaseReference.removeValue();
    }

    /** onStop() always runs when the activity is closed or invisible to the user.
     *
     *  Remove the current marker from the database so the user isn't being tracked any more.
     *
     */
    @Override
    protected void onStop() {
        super.onStop();
        databaseReference.removeValue();

    }
}