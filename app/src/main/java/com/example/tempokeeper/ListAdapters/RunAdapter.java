package com.example.tempokeeper.ListAdapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.tempokeeper.Model.Run;
import com.example.tempokeeper.R;

import java.util.ArrayList;

public class RunAdapter extends RecyclerView.Adapter<RunAdapter.ViewHolder> {
    private ArrayList<Run> pastRuns;
    private Context mContext;

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public int runIndex;
        public TextView txtDate;
        public TextView txtDuration;
        public TextView txtDistance;
        public TextView txtMaxSpeed;
        public TextView txtAvgSpeed;

        // Route object being stored in this row of the recycler view list
        private Run run;

        // context for RouteActivity
        private Context context;

        // each item in the recycler view is a ViewHolder
        public ViewHolder(View itemView, Context context) {
            super(itemView);

            // ViewHolder context is the same as PlayingRouteAdapter mContext
            // both the context of RouteActivity
            this.context = context;

            // set clickable
            itemView.setOnClickListener(this);

            // When a user clicks on a Route, isSelected is set to true so that
            // we can set the color of the Route information background

            // each Route ViewHolder has an image, name, and number of Routes
//            imgRoute = (ImageView) itemView.findViewById(R.id.imgPlayingRoute);
//            txtName = (TextView) itemView.findViewById(R.id.txtPlayingRoute);
//            txtArtist = (TextView) itemView.findViewById(R.id.txtPlayingArtist);
//            txtAlbum = (TextView) itemView.findViewById(R.id.txtPlayingAlbum);
//            txtTempo = (TextView) itemView.findViewById(R.id.txtPlayingTempo);
        }

        // When user clicks on the playing Route adapter, they will get a Toast notifying them
        // which Route is currently playing
        @Override
        public void onClick(View view) {
//            Toast.makeText(context, "Route " + route.getName() + " playing: " + route.getTempo() + " bpm", Toast.LENGTH_SHORT).show();
        }

    }

    // constructor for PlayingRouteAdapter
    // pass in currently playing Route array (one item) and the PedometerActivity context
    public RunAdapter(ArrayList<Run> runs, Context context) {
        mContext = context;
        pastRuns = runs;
    }

    // inflate the adapter_playingRoute layout to the ViewHolder
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                         int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_run, null, false);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutView.setLayoutParams(lp);

        // ViewHolder is one row of the list - holds one Route with its information
        ViewHolder vh = new ViewHolder(layoutView, mContext);
        return vh;
    }

    // set the name and image of the Route if available
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // set the Route, its name, and tempo (in bpm)
        holder.run = pastRuns.get(position);
//        holder.txtName.setText(playingRoutes.get(position).getName());
//        holder.txtArtist.setText(playingRoutes.get(position).getArtist());
//        holder.txtAlbum.setText(playingRoutes.get(position).getAlbumName());
//        holder.txtTempo.setText(playingRoutes.get(position).getTempo()+" BPM");

        // if the Route has an image url, load the image using Glide framework
//        if (holder.route.getImageURL() != null) {
//            /* Glide is an image loading framework for Android
//             * primary focus is on making scrolling any kind of a list of images as smooth and fast as possible
//             * also effective for any case where you need to fetch, resize, and display a remote image
//             */
//            Glide.with(mContext).load(playingRoutes.get(position).getImageURL()).into(holder.imgRoute);
//        }
    }

    // get the number of Routes displayed in the list
    @Override
    public int getItemCount() {
        return pastRuns.size();
    }
}
