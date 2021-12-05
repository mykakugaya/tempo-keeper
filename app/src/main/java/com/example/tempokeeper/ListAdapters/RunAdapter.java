package com.example.tempokeeper.ListAdapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.tempokeeper.Model.Run;
import com.example.tempokeeper.R;
import com.google.android.gms.maps.model.LatLng;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class RunAdapter extends RecyclerView.Adapter<RunAdapter.ViewHolder> {
    private ArrayList<Run> pastRuns;
    private Context mContext;

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public int runIndex;
        public TextView txtDate;
        public TextView txtDistance;
        public TextView txtDuration;
        public TextView txtMaxSpeed;
        public TextView txtAvgSpeed;

        // Run object being stored in this row of the recycler view list
        private Run run;
        public ArrayList<LatLng> route;

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
        }

        // When user clicks on the Run adapter,
        // it will take them to a Run preview page so that they can rerun that Run
        @Override
        public void onClick(View view) {
            // declare an intent, send in Run information

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
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // set each run info in holder
        holder.run = pastRuns.get(position);
        holder.route = pastRuns.get(position).getRoute();
        holder.txtDate.setText(pastRuns.get(position).getDate());
//        holder.txtDistance.setText(pastRuns.get(position).getDistance()+" miles");
        holder.txtDuration.setText(pastRuns.get(position).getDuration());
//        holder.txtAvgSpeed.setText(pastRuns.get(position).getAvgSpeed()+" MPH");
//        holder.txtMaxSpeed.setText(pastRuns.get(position).getMaxSpeed()+" MPH");
        holder.runIndex = holder.getAdapterPosition();
    }

    // get the number of Runs displayed in the list
    @Override
    public int getItemCount() {
        return pastRuns.size();
    }
}
