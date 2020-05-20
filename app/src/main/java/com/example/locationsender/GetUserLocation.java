package com.example.locationsender;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/*
 1. Check if Google Service API is available.
 2. Check if Location Permission is available to the app.
 3. if not, use ActivityCompat.requestPermission.
 4. onRequestPermissionsResult: Check permission results, if allowed, call x();
 5. in x(): Create a LocationRequest.
 6. check if location settings (GPS and wifi are on) are satisfied.
 7. if yes, create fusedLocationClient and call getLocation();
 8. if no, use resolvableApiException to turn on GPS.
*/

public class GetUserLocation extends AppCompatActivity {

    final int ACCESS_FINE_LOCATION_REQUEST_CODE = 111;
    private String name;
    TextView location_output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_user_location);
        location_output = (TextView) findViewById(R.id.location_output);

        // Set carName and contractAddress.
        TextView carNameTV = (TextView) findViewById(R.id.carName);
        TextView contractAddressTV = (TextView) findViewById(R.id.contractAddress);

        String contractAddress = getIntent().getStringExtra("contractAddress");

        carNameTV.setText(getIntent().getStringExtra("carName"));
        assert contractAddress != null;
        int len = contractAddress.length();

        contractAddressTV.setText(contractAddress.substring(0, 6) + "*****" + contractAddress.substring(len-5, len));

        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int result = api.isGooglePlayServicesAvailable(this);
        if(result != ConnectionResult.SUCCESS) {
            if(api.isUserResolvableError(result)) {
                // GoogleAPIServices is not available.
                location_output.setText(R.string.google_api_unabailable);
            }
        }
        else{
            // GoogleAPIServices is available.
            // Toast.makeText(GetUserLocation.this, "Checking for Location Permission", Toast.LENGTH_SHORT).show();

            // Check for Location Permission
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_DENIED){
                // permission is granted.
                // Toast.makeText(GetUserLocation.this, "Permission is granted", Toast.LENGTH_SHORT).show();
                x();
            }

            // Permission Denied, Request Permission.
            else{
                // Toast.makeText(GetUserLocation.this, "Permission is denied", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this,
                        new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                        ACCESS_FINE_LOCATION_REQUEST_CODE);
            }
        }
    }

     @Override
     public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResult){
         if (requestCode == ACCESS_FINE_LOCATION_REQUEST_CODE) {
             if (grantResult.length > 0 && grantResult[0] == PackageManager.PERMISSION_GRANTED) {
                 // Permission granted
                 // Create a location request

                 // Toast.makeText(GetUserLocation.this, "User Granted Permission", Toast.LENGTH_SHORT).show();
                 x();
             } else {
                 location_output.setText(R.string.location_permission_denied);
                 // Permission not granted
             }
         }
     }


    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;

    protected void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void x(){
//        Toast.makeText(GetUserLocation.this, "Creating Location Request and fetching location", Toast.LENGTH_SHORT).show();

        createLocationRequest();
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                // ...
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(GetUserLocation.this);
                getLocation();
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(GetUserLocation.this,
                                11);
                        finish();
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }

    protected void getLocation(){
        fusedLocationClient.getLastLocation().
                addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if(location != null){
                            // Got the location.
                            Toast.makeText(GetUserLocation.this, "Got the location.", Toast.LENGTH_SHORT).show();

                            double lat = location.getLatitude();
                            double lon = location.getLongitude();

                            // After getting the location, try to get firebase database.
                            FirebaseDatabase database = FirebaseDatabase.getInstance();
                            DatabaseReference ref = database.getReference("locations");

                            // Toast.makeText(GetUserLocation.this, ref.toString(), Toast.LENGTH_SHORT).show();
                            Map<String, Object> carLocation = new HashMap<>();

                            carLocation.put("lat", lat);
                            carLocation.put("lon", lon);

                            Intent intent = getIntent();
                            String contractAddress = intent.getStringExtra("contractAddress");

                            // Date currentTime = Calendar.getInstance().getTime();
                            // name = currentTime.toString();

                            assert contractAddress != null;
                            ref.child(contractAddress).updateChildren(carLocation);
                            // Toast.makeText(GetUserLocation.this, contractAddress + ": " + carLocation.toString(), Toast.LENGTH_SHORT).show();

                            location_output.setText("New Location of the Car is \nlatitudes: " + lat + "\nlongitudes: " + lon);

                        }
                        else{
                            // Unable to get location.
                            Toast.makeText(GetUserLocation.this, "Unable to get location.", Toast.LENGTH_SHORT).show();
                            location_output.setText(R.string.unable_to_get_location);
                        }
                    }
                });
    }

}
