/**
 * PetActivity displays dog petter's current location and dog walker's location and live updates
 *  Firebase of their locations
 * CPSC 312-02, Fall 2017
 * Programming Assignment Final Project
 *
 * @author Kurt Lamon, Andrew Yang
 * @version v1.0 12/8/17
 */

package com.kurtlemon.doggo3;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class PetActivity extends FragmentActivity implements OnMapReadyCallback {

    // Location request code for Google Maps.
    private static final int LOCATION_REQUEST_CODE = 1;

    // Google Maps fields.
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    // Firebase Database fields.
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private ChildEventListener childEventListener;

    // List of locations to appear on the map.
    private ArrayList<DogLocation> dogLocationArrayList;

    /** onCreate() runs whenever the activity is created.
     *
     * This sets up the location services, map, and database.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walk);
        // Setting up google maps and location services.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initialize the dog location list.
        dogLocationArrayList = new ArrayList<>();

        // Set up Firebase Database fields.
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
        childEventListener = new ChildEventListener() {

            /** onChildAdded() runs for each child already in the database when the user begins
             *      querying and for each new child added while the user is engaged with the
             *      database.
             *
             *  Creates a new DogLocation and displays it based on the information in the database.
             *
             * @param dataSnapshot
             * @param s
             */
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                // Create new DogLication for the child added, add it to the list and the screen.
                DogLocation dogLocation = dataSnapshot.getValue(DogLocation.class);
                dogLocationArrayList.add(dogLocation);
                fillMarkers();
            }

            /** onChildChanges() should run only when a dog walker moves location and their location
             *      updates in the database.
             *
             *  Replace the existing location that corresponds to that user with a new one in the
             *      right location.
             *
             * @param dataSnapshot
             * @param s
             */
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                // Get the new location.
                DogLocation newDogLocation = dataSnapshot.getValue(DogLocation.class);
                // Find the location that it corresponds to.
                ArrayList<DogLocation> toRemove = new ArrayList<>();
                for(DogLocation dogLocation: dogLocationArrayList){
                    if(dogLocation.compareTo(newDogLocation) == 0){
                        // Replace location and refresh map.
                        toRemove.add(dogLocation);
                    }
                }
                dogLocationArrayList.removeAll(toRemove);
                dogLocationArrayList.add(newDogLocation);
                fillMarkers();
            }

            /** When a dog walker leaves or marks that they are no longer available.
             *
             *  Delete the appropriate marker from the map.
             *
             * @param dataSnapshot
             */
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                // The location to remove.
                DogLocation newDogLocation = dataSnapshot.getValue(DogLocation.class);
                // Find and remove the appropriate location from the list and update the map.
                for(DogLocation dogLocation: dogLocationArrayList){
                    if(dogLocation.compareTo(newDogLocation) == 0){
                        dogLocationArrayList.remove(dogLocation);
                    }
                }
                fillMarkers();
            }

            /** Unused onChildMoved().
             *
             * @param dataSnapshot
             * @param s
             */
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            /** Unused onCancelled().
             *
             * @param databaseError
             */
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        databaseReference.addChildEventListener(childEventListener);

    }

    /** onMapReady() runs when the map has been built and is available.
     *
     *  Sets up the map information and checks permissions because setting up background location
     *      services.
     *
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Map initialization.
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Checking permissions.
        if(ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED){
            // Permission has been granted.
            mMap.setMyLocationEnabled(true);
        }else{
            // Permission has not been granted.
            // Request permission
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
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
                setUpLastKnownLocation();
            }
        }
    }

    /** Sets up information and recollection of last known location upon starting the activity
     *
     */
    private void setUpLastKnownLocation(){
        if(ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]
                    {android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }else{
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
                        }
                    }
                };
                // Check permissions.
                if (ActivityCompat.checkSelfPermission(PetActivity.this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED){
                    // Permission not granted.
                    ActivityCompat.requestPermissions(PetActivity.this,
                            new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_REQUEST_CODE);
                } else {
                    // Permission granted.
                    mFusedLocationProviderClient.requestLocationUpdates(locationRequest,
                            locationCallback, null);
                }
            }
        });
    }

    /** Called from database checking functions.
     *
     *  Updates the map to show markers from each dog in the dog locations list which is drawn from
     *      the database.
     *
     */
    private void fillMarkers(){
        // Delete all existing markers.
        mMap.clear();

        // Setting up the custom dog markers.
        int width = 75;
        int height = 75;
        for(DogLocation dogLocation : dogLocationArrayList){
            BitmapDrawable pawPrintBitmapDrawable = (BitmapDrawable) getResources().getDrawable(
                    R.mipmap.paw_print_marker_logo);
            Bitmap pawPrintBitmap = pawPrintBitmapDrawable.getBitmap();
            Bitmap smallPawPrintMarker = Bitmap.createScaledBitmap(pawPrintBitmap, width, height,
                    false);

            // Assigning the marker and placing it on the map.
            LatLng latLng = new LatLng(dogLocation.getLatitude(), dogLocation.getLongitude());
            MarkerOptions pawPrintMarker = new MarkerOptions();
            pawPrintMarker.position(latLng)
                    .title("(" + String.format("%.2f", dogLocation.getLatitude()) + ", " + String.format("%.2f", dogLocation.getLongitude()) + ")")
                    .icon(BitmapDescriptorFactory.fromBitmap(smallPawPrintMarker));

            mMap.addMarker(pawPrintMarker);

        }
    }

}
