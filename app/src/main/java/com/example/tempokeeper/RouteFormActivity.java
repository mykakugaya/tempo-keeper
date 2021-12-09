package com.example.tempokeeper;

import static java.lang.Double.parseDouble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

public class RouteFormActivity extends AppCompatActivity {
    private EditText edtDestination;
    private EditText edtDistance;
    private RadioGroup radioGroup;
    private RadioButton rbtnElevation;
    private Button btnCalculate;

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 9002;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_route_form);

        //reference views
        edtDestination = (EditText) findViewById(R.id.edtDestination);
        edtDistance = (EditText) findViewById(R.id.edtDistance);
        btnCalculate = (Button) findViewById(R.id.btnCalculate);
        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);

        /**
         * Get destination string and extra parameters and request Google Directions JSONObject
         */
        btnCalculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // save the goal distance in sharedPreferences
                // when user reaches the goal distance in RunningActivity while running,
                // app will notify user that they have reached their distance goal
                double targetDistance;
                if(!edtDistance.getText().toString().equals("")) {
                    targetDistance = parseDouble(edtDistance.getText().toString());
                } else {
                    targetDistance = 0;
                }

                int selectedId = radioGroup.getCheckedRadioButtonId();
                // find the radiobutton by returned id
                rbtnElevation = (RadioButton) findViewById(selectedId);

                //get destination string and extra parameters
                String dest = edtDestination.getText().toString();

                //send to Route Preview Activity with destination and elevation
                Intent intent = new Intent(RouteFormActivity.this, RoutePreviewActivity.class);
                intent.putExtra("destination", dest);
                intent.putExtra("elevation", rbtnElevation.getText().toString());
                intent.putExtra("targetDist", targetDistance);
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
    @SuppressLint("ResourceAsColor")
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
            Toast.makeText(getBaseContext(), "Signed out.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(RouteFormActivity.this, LoginActivity.class));
            finish();

            return true;
        }

        // default
        return super.onOptionsItemSelected(item);

    }
}
