package com.example.tempokeeper.ListAdapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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
import java.util.Base64;
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
        public ImageView imgRoute;

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
            imgRoute = (ImageView) itemView.findViewById(R.id.imgRoute);
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
        holder.runIndex = holder.getAdapterPosition();
        holder.runningRoute = holder.run.getRoute();
        holder.txtDate.setText(holder.run.getDate());
        holder.txtDuration.setText("Duration: " + holder.run.getDuration());
        holder.txtDistance.setText("Distance: "+pastRuns.get(position).getDistance()+" miles");
        holder.txtAvgSpeed.setText("Average Speed: "+pastRuns.get(position).getAvgSpeed()+" MPH");
//        holder.txtMaxSpeed.setText("Max speed: "+pastRuns.get(position).getMaxSpeed()+" MPH");

        if (holder.run.getImage() != null) {
            /* Glide is an image loading framework for Android
             * primary focus is on making scrolling any kind of a list of images as smooth and fast as possible
             * also effective for any case where you need to fetch, resize, and display a remote image
             */
            String strBitmap = pastRuns.get(position).getImage();
            byte[] decode = Base64.getDecoder().decode(strBitmap);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decode,0,decode.length);
            holder.imgRoute.setImageBitmap(bitmap);
//            Glide.with(mContext).load(pastRuns.get(position).getImage()).into(holder.imgRoute);
        }
    }

    // get the number of Runs displayed in the list
    @Override
    public int getItemCount() {
        return pastRuns.size();
    }


}
