package com.example.tempokeeper;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class RouteFormActivity extends AppCompatActivity {
    private EditText edtDestination;
    private String TAG = "RunningFormActivity";
    private String origin = "&origin=";
    private RadioGroup radioGroup;
    private Button btnCalculate;

    //Distance is used after getting routes back
    /**
     * Distance Strategy:
     * 1. get an array of route arrays of distances between each leg from the JSONParser
     * 2. sum up the distance of each route and compare it with the distance parameter
     * 3. if there exists a route that fulfills the distance parameter, show that route
     *          set some boolean as true so we know there is a route
     * 4. if the boolean is true: do nothing
     *      if the boolean is false (if the JSONParser routes arent within the distance parameter)
     *          compare the difference between the latitude of the origin and destination to the
     *          difference in longitude
     *      if difference in latitude is less than the difference in longitude
     *          find a midpoint at the same latitude and change the longitude to make a triangle
     *          between the origin, midpoint, and destination westwise(anything is fine). Do math so the midpoint causes
     *          the route to be within the distance parameter
     *      request an API call with the route with the midpoint we created and check if such a route is possible
     *      if not possible, try eastwise. if that doesnt work, return error
     *      if a route is found, return that route.
     */
    private EditText edtDistance;

    /**
     * Elevation Strategy:
     * 1. get an array of route arrays of latlngs from the JSONParser
     * 2. traverse each route, find the elevation of each latlng, and add the elevation to an array
     * 3. get the difference in elevation for the whole route and compare it with the other routes
     * 4. return the one with the greatest or smallest difference
     */
    private RadioButton rbtnHilly;
    private RadioButton rbtnFlat;
    private RadioButton rbtnNA;
    private RadioButton rbtnElevation;

    /**
     * New Route Strategy:
     * 1. get an array of route arrays of latlngs from the JSONParser
     * 2. check if any of the routes are in the database
     * 3. get a random int 1-whatever and add that many waypoints
     *    at random points between the latitude or longitude
     *    depending on which one has a smallest difference between
     *    the origin and destination
     */
    private Switch swcNewRoute;

    private static final int PERMISSIONS_REQUEST_ENABLE_GPS = 9001;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 9002;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_route_form);

        //reference views
        edtDestination = (EditText) findViewById(R.id.edtDestination);
//        rbtnHilly = (RadioButton) findViewById(R.id.rbtnHilly);
//        rbtnFlat = (RadioButton) findViewById(R.id.rbtnFlat);
//        rbtnNA = (RadioButton) findViewById(R.id.rbtnNA);
        swcNewRoute = (Switch) findViewById(R.id.swcNewRoute);
        btnCalculate = (Button) findViewById(R.id.btnCalculate);
        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);

        /**
         * Get destination string and extra parameters and request Google Directions JSONObject
         */
        btnCalculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int selectedId = radioGroup.getCheckedRadioButtonId();
                // find the radiobutton by returned id
                rbtnElevation = (RadioButton) findViewById(selectedId);

//                //get destination string and extra parameters
                String dest = edtDestination.getText().toString();
//                //send to Route Preview Activity
                Intent intent = new Intent(RouteFormActivity.this, RoutePreviewActivity.class);
                intent.putExtra("destination", dest);
                intent.putExtra("elevation", rbtnElevation.getText().toString());
                startActivity(intent);
            }
        });
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            //start app
        } else {
            //reask for permission if denied. If denied and not allowed to ask again, app will not work.
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getLocationPermission();
        Log.d("onResume", " Called");
    }


    // MENU
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    // Handle menu clicks by the user
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // if route option selected, don't do anything, already on page
        if (id == R.id.menuRoute) {
            return true;
        }

        // go to user profile
        if (id == R.id.menuProfile) {
            Intent profileIntent = new Intent(RouteFormActivity.this, ProfileActivity.class);
            startActivity(profileIntent);
            return true;
        }

        // sign out of app
        if (id == R.id.menuSignOut) {
            FirebaseAuth.getInstance().signOut();

            // user is now signed out
            Toast.makeText(getBaseContext(), "Signed out.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(RouteFormActivity.this, LoginActivity.class));
            finish();

            return true;
        }

        // default
        return super.onOptionsItemSelected(item);

    }
}
