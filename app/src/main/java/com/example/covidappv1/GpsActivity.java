package com.example.covidappv1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.android.PolyUtil;

import java.util.ArrayList;

public class GpsActivity extends AppCompatActivity   implements OnMapReadyCallback {
    private static final String OUT_OF_POLYGON = "out_of_polygon_id";
    Button btnTopLeft, btnTopRight, btnBotLeft, btnBotRight, btnReset;
    GoogleMap map;
    TextView txtviewInstruction;
    private double currLat, currLong;
    LocationCallback locationCallBack;
    LocationRequest locationRequest;
    private Marker currentMarker;
    private ArrayList<LatLng> polyCoord;
    private ArrayList<Marker> polyMarker;
    private boolean havePolygon = false;
    private NotificationManagerCompat notificationManagerCompat;
    SupportMapFragment supportMapFragment;
    FusedLocationProviderClient fusedLocationProviderClient;
    NotificationCompat.Builder builder;
    private Polygon polygon1;

    //constants
    private static final int PERMISSION_FINE_LOCATION = 66;
    //How fast the gps updates location (seconds)
    public static final int DEFAULT_UPDATE_INTERVAL = 5;
    public static final int FASTEST_UPDATE_INTERVAL = 3;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);


        btnTopLeft = findViewById(R.id.btnTopLeft);
        btnTopRight = findViewById(R.id.btnTopRight);
        btnBotLeft = findViewById(R.id.btnBotLeft);
        btnBotRight = findViewById(R.id.btnBotRight);
        btnReset = findViewById(R.id.btnReset);
        txtviewInstruction = findViewById(R.id.txtviewInstructions);
        polyCoord = new ArrayList<>();

        supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        notificationManagerCompat = NotificationManagerCompat.from(this);



        //init location request / callback
        locationRequest = new LocationRequest();

        //default interval that it does a location check
        locationRequest.setInterval(1000 * DEFAULT_UPDATE_INTERVAL);

        //Same as above but fastest
        locationRequest.setFastestInterval(1000 * FASTEST_UPDATE_INTERVAL);

        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        // triggered whenever interval is met
        locationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();
                updateCoordinates(location);
            }
        };


        //sets up the googlemap/map fragment and location updating
        if (supportMapFragment != null){
            supportMapFragment.onCreate(savedInstanceState);
            supportMapFragment.getMapAsync(this);
            startLocationUpdate();

        }


        //set up Notification
        createNotificationChannel();
        builder = new NotificationCompat.Builder(this, OUT_OF_POLYGON)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("Fencing Alert")
                .setContentText("This is a notification that you have exited the parameter you've set.")
                .setStyle(new NotificationCompat.BigTextStyle().bigText("This is a notification that you have exited the parameter you've set."))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);


        //setting the btns and their function

        addInstruction(" - Walk towards the top left corner of the property, once the cyan coloured marker reached the location press the button to create a marker");
        btnTopRight.setEnabled(false);
        btnBotRight.setEnabled(false);
        btnBotLeft.setEnabled(false);

        btnTopLeft.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                addInstruction(" - Walk towards the top right corner of the property, once the cyan coloured marker reached the location press the button to create a marker");
                //Gotta reset if theres anything in the arraylist already
                if (polyCoord.size() >= 4){
                    polyCoord= new ArrayList<>();
                }
                LatLng coord = new LatLng(currLat, currLong);
                Marker newMarker = map.addMarker(new MarkerOptions().
                        position(coord).
                        title("Top Left Corner"));
                polyCoord.add(coord);
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(coord, 20 ));
                btnTopLeft.setEnabled(false);
                btnTopRight.setEnabled(true);

            }
        });
        btnTopRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addInstruction(" - Walk towards the bottom right corner of the property, once the cyan coloured marker reached the location press the button to create a marker");

                LatLng coord = new LatLng(currLat, currLong);
                Marker newMarker = map.addMarker(new MarkerOptions().
                        position(coord).
                        title("Top Right Corner"));
                polyCoord.add(coord);
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(coord, 20 ));
                btnTopRight.setEnabled(false);
                btnBotRight.setEnabled(true);
            }
        });
        btnBotRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addInstruction(" - Walk towards the bottom left corner of the property, once the cyan coloured marker reached the location press the button to create a marker.");

                LatLng coord = new LatLng(currLat, currLong);
                Marker newMarker = map.addMarker(new MarkerOptions().
                        position(coord).
                        title("Bot Right Corner"));
                polyCoord.add(coord);
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(coord, 20 ));
                btnBotRight.setEnabled(false);
                btnBotLeft.setEnabled(true);
            }
        });
        btnBotLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addInstruction(" - The new created polygon is the \"fence\" that you should stay in during the quarantine period, if you forget, the notification will remind you that you have left the area.");
                LatLng coord = new LatLng(currLat, currLong);
                Marker newMarker = map.addMarker(new MarkerOptions().
                        position(coord).
                        title("Bot Right Corner"));
                polyCoord.add(coord);
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(coord, 20 ));
                btnBotLeft.setEnabled(false);
                btnReset.setEnabled(true);

                //make the polygon
                polygon1 = map.addPolygon(new PolygonOptions()
                        .clickable(true).addAll(polyCoord));
                havePolygon = true;
            }
        });
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnTopLeft.setEnabled(true);
                btnBotLeft.setEnabled(false);
                havePolygon = false;
                map.clear();
                addInstruction(" - Please restart from the top left corner of the location");
            }
        });
    }

    //checks permission
    @SuppressLint("MissingPermission")
    public void updateGPS() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(GpsActivity.this);
        //check if we have permission to get fine location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if(location != null){
                        updateCoordinates(location);
                    }
                }
            });
        } else { //if no permission was given
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);//permission fine location can be any arbitrary number that represents the permission from now on
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_FINE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateGPS();
                } else {
                    Toast.makeText(this, "This app requires permissions to work properly", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    //
    public void updateCoordinates(Location location) {
        currLat = location.getLatitude();
        currLong = location.getLongitude();
        LatLng coord = new LatLng(currLat, currLong);
        //updates the marker representing current location
        if (currentMarker != null){
            currentMarker.remove();
        }
        currentMarker= map.addMarker(new MarkerOptions().position(coord).title("Current Location").
                icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));

        //have not implemented notification yet
        if (havePolygon){
            boolean insidePolygon = insidePolygon(coord, polyCoord);
            if (!insidePolygon){
                notificationManagerCompat.notify(10,builder.build());
            }
        }



    }


    @SuppressLint("MissingPermission")
    public void startLocationUpdate() {
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
        updateGPS();
    }

    public void stopLocationUpdate() {

        fusedLocationProviderClient.removeLocationUpdates(locationCallBack);

    }


    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                //start tracking location
                LatLng coord = new LatLng(currLat, currLong);
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(coord, 20 ));
            }
        });
    }

    //checks if the location is inside the polygon
    private boolean insidePolygon(LatLng currCoord, ArrayList<LatLng> polyCoord){
        boolean insidePolygon  = PolyUtil.containsLocation(currCoord, polyCoord, true);
        if (insidePolygon){
            Toast.makeText(getApplicationContext(),"You are inside the parameter",Toast. LENGTH_SHORT).show();
        }else{
            Toast.makeText(getApplicationContext(),"You have stepped outside the parameter",Toast. LENGTH_SHORT).show();

        }
        return insidePolygon;
    }
    // For notifs
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(OUT_OF_POLYGON, "Location Check", importance);
            channel.setDescription("notification that is called when user leaves parameter");
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void addInstruction(String stringToAdd){
        String newString = " - The cyan marker indicates your current location. \n" + stringToAdd;
        txtviewInstruction.setText(newString);
    }

}