package com.example.tempokeeper;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.LatLng;

public class RouteFormActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 9002;
    private EditText edtDestination;
    private String TAG = "RunningFormActivity";
    private String origin = "&origin=";
    private LatLng originPoint;
    private double longitude;
    private double latitude;
    private Button btnCalculate;

    //temp tvs to see output
    private TextView tvSadge;
    private TextView tv2;



    //Distance is used after getting routes back
    private EditText edtDistance;
    private RadioButton rbtnHilly;
    private RadioButton rbtnFlat;
    private RadioButton rbtnNA;
    private Switch swcNewRoute;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_form);

//        tvSadge = (TextView) findViewById(R.id.tvSadge);
//        tv2 = (TextView) findViewById(R.id.tv2);

        //reference views
        edtDestination = (EditText) findViewById(R.id.edtDestination);
        edtDistance = (EditText) findViewById(R.id.edtDistance);
        rbtnHilly = (RadioButton) findViewById(R.id.rbtnHilly);
        rbtnFlat = (RadioButton) findViewById(R.id.rbtnFlat);
        rbtnNA = (RadioButton) findViewById(R.id.rbtnNA);
        swcNewRoute = (Switch) findViewById(R.id.swcNewRoute);
        btnCalculate = (Button) findViewById(R.id.btnCalculate);

        /**
         * Get destination string and extra parameters and request Google Directions JSONObject
         */
        btnCalculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get origin point
                LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                //required permission check from Android Studio
                if (ActivityCompat.checkSelfPermission(RouteFormActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(RouteFormActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                longitude = location.getLongitude();
                latitude = location.getLatitude();
                originPoint = new LatLng(latitude, longitude);

                //get destination string and extra parameters
                String dest = edtDestination.getText().toString();
                // Getting URL to the Google Directions API
                String url = getDirectionsUrl(originPoint, dest);

                //send to preview intent
                Intent intent = new Intent(RouteFormActivity.this, RoutePreviewActivity.class);
                intent.putExtra("url", url);
                intent.putExtra("originLat", latitude);
                intent.putExtra("originLong", longitude);
                startActivity(intent);
            }
        });
    }

    private String getDirectionsUrl(LatLng origin, String dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        dest.replaceAll("//s", "");
        String str_dest = "destination=" + dest;

        // Sensor enabled
        String mode = "mode=walking";
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest;

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/json?" + parameters + "&key=AIzaSyDOMa68BrLRKcMLnCVODFd3cwqOxfnB2qw&alternatives=true";

        Log.d("URL", url);
        return url;
    }
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            //start app
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getLocationPermission();
    }
}
