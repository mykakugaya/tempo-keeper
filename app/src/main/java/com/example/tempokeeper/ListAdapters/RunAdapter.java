package com.example.tempokeeper.ListAdapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tempokeeper.Model.Run;
import com.example.tempokeeper.PastRoutePreview;
import com.example.tempokeeper.ProfileActivity;
import com.example.tempokeeper.R;
import com.example.tempokeeper.RunningActivity;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.snackbar.Snackbar;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collections;

public class RunAdapter extends RecyclerView.Adapter<RunAdapter.ViewHolder> {
    private ArrayList<Run> pastRuns;
    private Context mContext;

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // Each run ViewHolder stores a Run object
        // index, date, distance, duration, max speed, avg speed
        // Also includes the ran route and a map fragment to display it
        public int runIndex;
        public TextView txtDate;
        public TextView txtDistance;
        public TextView txtDuration;
        public TextView txtMaxSpeed;
        public TextView txtAvgSpeed;

        public LinearLayout layoutAdapter;

        // Run object being stored in this row of the recycler view list
        public Run run;
        public ArrayList<LatLng> runningRoute;
        public ArrayList<Polyline> routesArray;
        public ArrayList<PolylineOptions> polylineOptArray;
        // the polyline that we build on the map -- we will send this to running activity during onClick
        public PolylineOptions lineOptions;

        // context for ProfileActivity
        private Context context;

        // each item in the recycler view is a ViewHolder
        public ViewHolder(View itemView, Context context) {
            super(itemView);

            // ViewHolder context is the same as RunAdapter mContext
            // both the context of ProfileActivity
            this.context = context;

            // set clickable
            itemView.setOnClickListener(this);

            // When a user clicks on a Run, isSelected is set to true so that
            // we can set the color of the Run information background

            // each Run ViewHolder has an image, name, and number of Runs
            txtDate = (TextView) itemView.findViewById(R.id.txtDate);
            txtDistance = (TextView) itemView.findViewById(R.id.txtRunDistance);
            txtDuration = (TextView) itemView.findViewById(R.id.txtRunTime);
            txtAvgSpeed = (TextView) itemView.findViewById(R.id.txtRunAvg);
            txtMaxSpeed = (TextView) itemView.findViewById(R.id.txtRunMax);
            layoutAdapter = (LinearLayout) itemView.findViewById(R.id.layoutRunAdapter);

        }

        // When user clicks on the Run adapter,
        // it takes them to a RunningActivity so that they can rerun that Run
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onClick(View view) {
            // highlight the clicked run
            layoutAdapter.setBackgroundColor(context.getResources().getColor(R.color.blue_100));
            // Send a snackbar asking user if they want to rerun this route
            // If they click "Yes!", it will take them to the RunningActivity with that route
            Snackbar sbAdapter = Snackbar.make(itemView, "Review/Rerun this route?", Snackbar.LENGTH_LONG).addCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar snackbar, int event) {
                    switch (event) {
                        case Snackbar.Callback.DISMISS_EVENT_ACTION:
                            // declare an intent, send the run information to PastRoutePreview activity to review stats
                            // from there, we can choose to rerun the route
                            Intent previewIntent = new Intent(context, PastRoutePreview.class);
                            previewIntent.putExtra("pastRoute", run.getRoute());
                            previewIntent.putExtra("pastDate", run.getDate());
                            previewIntent.putExtra("pastDur", run.getDuration());
                            previewIntent.putExtra("pastDist", run.getDistance());
                            previewIntent.putExtra("pastAvgSpd", run.getAvgSpeed());
//                            previewIntent.putExtra("pastMaxSpd",run.getMaxSpeed());

                            context.startActivity(previewIntent);
                            break;
                        case Snackbar.Callback.DISMISS_EVENT_TIMEOUT:
                            // timed out snackbar, unselect the run
                            layoutAdapter.setBackgroundColor(Color.TRANSPARENT);
                            break;
                    }
                }

                @Override
                public void onShown(Snackbar snackbar) {
                }
            }).setAction("YES!", new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
            sbAdapter.getView().setBackgroundColor(context.getColor(R.color.colorPrimaryDark));
            sbAdapter.show();
        }
    }

//        @RequiresApi(api = Build.VERSION_CODES.O)
//        @Override
//        public void onMapReady(@NonNull GoogleMap googleMap) {
//            mMap = googleMap;
//            // display the color-coded route on map
//            displayRoute(runningRoute);
//        }

//        @RequiresApi(api = Build.VERSION_CODES.O)
//        public void displayRoute(ArrayList<LatLng> runningRoute) {
//            lineOptions = null;
//            routesArray = new ArrayList<Polyline>();
//            polylineOptArray = new ArrayList<PolylineOptions>();
//
//            lineOptions = new PolylineOptions();
//
//            // color code the polyline based on running speed
//            ArrayList<int[]> colorArr = setRouteColors();
//
//            for (int i=0; i<runningRoute.size()-1; i++) {
//                // Get each consecutive pair of runningRoute, set color for line connecting them
//                int[] rgbColor = colorArr.get(i);
//                int r = rgbColor[0];
//                int g = rgbColor[1];
//                int b = rgbColor[2];
//                Color curColor = Color.valueOf(r,g,b);
//                lineOptions.add(runningRoute.get(i));
//                lineOptions.add(runningRoute.get(i+1));
//                lineOptions.color(curColor.toArgb());
//                lineOptions.width(12);
//                lineOptions.geodesic(true);
//                lineOptions.clickable(true);
//                mMap.addPolyline(lineOptions);
//            }
//            //marker for origin point
//            MarkerOptions originMarker = new MarkerOptions();
//            originMarker.position(runningRoute.get(0));
//            originMarker.anchor((float) 0.5, (float) 0.5);
//            originMarker.title("This is you");
//            originMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
//            mMap.addMarker(originMarker);
//
//            //marker for end point
//            MarkerOptions endMarker = new MarkerOptions();
//            endMarker.position(runningRoute.get(runningRoute.size()-1));
//            endMarker.anchor((float) 0.5, (float) 0.5);
//            endMarker.title("This is your end point");
//            mMap.addMarker(endMarker);
//
//            LatLngBounds.Builder builder = new LatLngBounds.Builder();
//            builder.include(originMarker.getPosition());
//            builder.include(endMarker.getPosition());
//            LatLngBounds bounds = builder.build();
//
//            int width = context.getResources().getDisplayMetrics().widthPixels;
//            int height = context.getResources().getDisplayMetrics().heightPixels;
//            int padding = (int) (width * 0.10); // offset from edges of the map 10% of screen
//
//            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, 300);
//
//            mMap.animateCamera(cu);
//        }
//
//        public ArrayList<int[]> setRouteColors(){
//            ArrayList<Double> distances = new ArrayList<>();
//            double maxSpeed;
//            double minSpeed;
//            for (int i = 1;i<runningRoute.size();i++){
//                distances.add(Math.abs(runningRoute.get(i).latitude) - Math.abs(runningRoute.get(i-1).latitude) +
//                        Math.abs(runningRoute.get(i).longitude) - Math.abs(runningRoute.get(i-1).longitude));
//            }
//
//            maxSpeed = Collections.max(distances);
//            // grade = 100
//            minSpeed = Collections.min(distances);
//            // grade = 0
//
//
//            // array list of speed proportions
//            // proportion = currentSpeed/maxSpeed
//            ArrayList<Double> speedArray = new ArrayList<>();
//
//            // Array list of rgb triplets for color coding
//            ArrayList<int[]> colorsArray = new ArrayList<>();
//
//            for (int j = 0;j<distances.size();j++){
//                // proportion = (currentSpeed-minSpeed)/(maxSpeed-minSpeed)
//                Double proportion = (distances.get(j)-minSpeed)/(maxSpeed-minSpeed);
//                // add to speed array
//                speedArray.add(proportion);
//
//                // set the color of this proportion of the run based on relative speed
//                // RED(255,0,0) = 0%
//                // YELLOW(255,255,0) = 50%
//                // GREEN(0,255,0) = 100%
//                int[] rgbArr = new int[3];
//
//                // red to yellow range
//                if(proportion < 0.5) {
//                    int[] slowArr = {255,0,0};
//                    int secondIndex = (int) (proportion*255);
//                    slowArr[1] = secondIndex;
//                    rgbArr = slowArr;
//                } else {    // yellow to green range
//                    int[] fastArr = {0,255,0};
//                    int firstIndex = (int) (255-(proportion*255));
//                    fastArr[0] = firstIndex;
//                    rgbArr = fastArr;
//                }
//                colorsArray.add(rgbArr);
//            }
//
//            // Colors array should now be full of rgb arr values (e.g. {0,255,76})
//            return colorsArray;
//        }
//    }

    // constructor for RunAdapter
    // pass in current Run array (one item) and the ProfileActivity context
    public RunAdapter(ArrayList<Run> runs, Context context) {
        mContext = context;
        pastRuns = runs;
    }

    // inflate the adapter_run layout to the ViewHolder
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                         int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_run, null, false);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutView.setLayoutParams(lp);

        // ViewHolder is one row of the list - holds one Run with its information
        ViewHolder vh = new ViewHolder(layoutView, mContext);
        return vh;
    }

    // set the name and image of the Run if available
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // set each run info in holder
        holder.run = pastRuns.get(position);
        holder.runningRoute = holder.run.getRoute();
        holder.txtDate.setText(holder.run.getDate());
        holder.txtDuration.setText("Duration: " + holder.run.getDuration());
        holder.txtDistance.setText("Distance: "+pastRuns.get(position).getDistance()+" miles");
        holder.txtAvgSpeed.setText("Average Speed: "+pastRuns.get(position).getAvgSpeed()+" MPH");
//        holder.txtMaxSpeed.setText("Max speed: "+pastRuns.get(position).getMaxSpeed()+" MPH");
        holder.runIndex = holder.getAdapterPosition();

    }

    // get the number of Runs displayed in the list
    @Override
    public int getItemCount() {
        return pastRuns.size();
    }


}
