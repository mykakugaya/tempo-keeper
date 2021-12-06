package com.example.tempokeeper;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tempokeeper.Model.Run;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Collections;

public class PastRoutePreview extends AppCompatActivity implements OnMapReadyCallback,  GoogleMap.OnPolylineClickListener,
        GoogleMap.OnPolygonClickListener, GoogleMap.OnInfoWindowClickListener{

    private TextView txtDate;
    private TextView txtDist;
    private TextView txtDur;
    private TextView txtAvgSpd;
    private TextView txtMaxSpd;
    private Button btnBack;
    private Button btnRerun;

    private String date;
    private String duration;
    private double distance;
    private double avgSpd;
    private double maxSpd;

    private GoogleMap mMap;
    private PolylineOptions lineOptions;    // send this to RunningActivity
    private ArrayList<Polyline> routesArray;
    private ArrayList<PolylineOptions> polylineOptArray;
    private ArrayList<LatLng> runningRoute;
    private Run pastRun;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_past_preview);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        txtDate = (TextView) findViewById(R.id.txtPastDate);
        txtDur = (TextView) findViewById(R.id.txtPastDur);
        txtDist = (TextView) findViewById(R.id.txtPastDist);
        txtAvgSpd = (TextView) findViewById(R.id.txtPastAvgSpd);
        txtMaxSpd = (TextView) findViewById(R.id.txtPastMaxSpd);
        btnBack = (Button) findViewById(R.id.btnBackProfile);
        btnRerun = (Button) findViewById(R.id.btnRerun);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapPastRoute);
        mapFragment.getMapAsync(PastRoutePreview.this);

        Bundle bundle = getIntent().getExtras();

        runningRoute = bundle.getParcelableArrayList("pastRoute");

        date = bundle.getString("pastDate");
        txtDate.setText(date);

        duration = bundle.getString("pastDur");
        txtDur.setText("Duration: "+duration);

        btnRerun.setEnabled(false);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent profileIntent = new Intent(PastRoutePreview.this, ProfileActivity.class);
                startActivity(profileIntent);
            }
        });

        btnRerun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent rerunIntent = new Intent(PastRoutePreview.this, PlaylistActivity.class);
                // add extras for the back button in PlaylistAct (comes back to this page)
                rerunIntent.putExtra("pastRoute", runningRoute);
                rerunIntent.putExtra("pastDate",date);
                rerunIntent.putExtra("pastDur",duration);

                rerunIntent.putExtra("chosenRoute", lineOptions);   // map route for RunningAct
                startActivity(rerunIntent);
            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void displayRoute(ArrayList<LatLng> runningRoute) {
        lineOptions = null;
        routesArray = new ArrayList<Polyline>();
        polylineOptArray = new ArrayList<PolylineOptions>();

        lineOptions = new PolylineOptions();

        // color code the polyline based on running speed
        ArrayList<int[]> colorArr = setRouteColors();

        for (int i=0; i+1<runningRoute.size(); i++) {
            // Get each consecutive pair of runningRoute, set color for line connecting them
            int[] rgbColor = colorArr.get(i);
            int r = rgbColor[0];
            int g = rgbColor[1];
            int b = rgbColor[2];
            Color curColor = Color.valueOf(r,g,b);
            lineOptions.add(runningRoute.get(i));
            lineOptions.add(runningRoute.get(i+1));
            lineOptions.color(curColor.toArgb());
            lineOptions.width(12);
            lineOptions.geodesic(true);
            lineOptions.clickable(true);
            mMap.addPolyline(lineOptions);
        }
        //marker for origin point
        MarkerOptions originMarker = new MarkerOptions();
        originMarker.position(runningRoute.get(0));
        originMarker.anchor((float) 0.5, (float) 0.5);
        originMarker.title("This is you");
        originMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        mMap.addMarker(originMarker);

        //marker for end point
        MarkerOptions endMarker = new MarkerOptions();
        endMarker.position(runningRoute.get(runningRoute.size()-1));
        endMarker.anchor((float) 0.5, (float) 0.5);
        endMarker.title("This is your end point");
        mMap.addMarker(endMarker);

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(originMarker.getPosition());
        builder.include(endMarker.getPosition());
        LatLngBounds bounds = builder.build();

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        int padding = (int) (width * 0.10); // offset from edges of the map 10% of screen

        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, 300);

        mMap.animateCamera(cu);

        btnRerun.setEnabled(true);
    }

    public ArrayList<int[]> setRouteColors(){
        ArrayList<Double> distances = new ArrayList<>();
        double maxSpeed;
        double minSpeed;
        for (int i = 1;i<runningRoute.size();i++){
            distances.add(Math.abs(runningRoute.get(i).latitude) - Math.abs(runningRoute.get(i-1).latitude) +
                    Math.abs(runningRoute.get(i).longitude) - Math.abs(runningRoute.get(i-1).longitude));
        }
        maxSpeed = Collections.max(distances);
        // grade = 100
        minSpeed = Collections.min(distances);
        // grade = 0

        // array list of speed proportions
        // proportion = currentSpeed/maxSpeed
        ArrayList<Double> speedArray = new ArrayList<>();

        // Array list of rgb triplets for color coding
        ArrayList<int[]> colorsArray = new ArrayList<>();

        for (int j = 0;j<distances.size();j++){
            // proportion = (currentSpeed-minSpeed)/(maxSpeed-minSpeed)
            Double proportion = (distances.get(j)-minSpeed)/(maxSpeed-minSpeed);
            // add to speed array
            speedArray.add(proportion);

            // set the color of this proportion of the run based on relative speed
            // RED(255,0,0) = 0%
            // YELLOW(255,255,0) = 50%
            // GREEN(0,255,0) = 100%
            int[] rgbArr = new int[3];

            // red to yellow range
            if(proportion < 0.5) {
                int[] slowArr = {255,0,0};
                int secondIndex = (int) (proportion*255);
                slowArr[1] = secondIndex;
                rgbArr = slowArr;
            } else {    // yellow to green range
                int[] fastArr = {0,255,0};
                int firstIndex = (int) (255-(proportion*255));
                fastArr[0] = firstIndex;
                rgbArr = fastArr;
            }
            colorsArray.add(rgbArr);
        }

        // Colors array should now be full of rgb arr values (e.g. {0,255,76})
        return colorsArray;
    }

    @Override
    public void onInfoWindowClick(@NonNull Marker marker) {

    }

    @Override
    public void onPolygonClick(@NonNull Polygon polygon) {

    }

    @Override
    public void onPolylineClick(@NonNull Polyline polyline) {

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnInfoWindowClickListener(PastRoutePreview.this);
        displayRoute(runningRoute);

    }
}
