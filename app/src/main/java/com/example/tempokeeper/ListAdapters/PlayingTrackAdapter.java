package com.example.tempokeeper.ListAdapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tempokeeper.Model.Track;
import com.example.tempokeeper.R;

import java.util.ArrayList;

public class PlayingTrackAdapter extends RecyclerView.Adapter<PlayingTrackAdapter.ViewHolder> {
    private ArrayList<Track> playingTracks;
    private Context mContext;

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView txtName;
        public TextView txtArtist;
        public TextView txtAlbum;
        public TextView txtTempo;
        public ImageView imgTrack;

        // track object being stored in this row of the recycler view list
        private Track track;
        // context for TrackActivity
        private Context context;

        // each item in the recycler view is a ViewHolder
        public ViewHolder(View itemView, Context context) {
            super(itemView);

            // ViewHolder context is the same as PlayingTrackAdapter mContext
            // both the context of TrackActivity
            this.context = context;

            // set clickable
            itemView.setOnClickListener(this);

            // When a user clicks on a track, isSelected is set to true so that
            // we can set the color of the track information background

            // each track ViewHolder has an image, name, and number of tracks
            imgTrack = (ImageView) itemView.findViewById(R.id.imgPlayingTrack);
            txtName = (TextView) itemView.findViewById(R.id.txtPlayingTrack);
            txtArtist = (TextView) itemView.findViewById(R.id.txtPlayingArtist);
            txtAlbum = (TextView) itemView.findViewById(R.id.txtPlayingAlbum);
            txtTempo = (TextView) itemView.findViewById(R.id.txtPlayingTempo);
        }

        // When user clicks on the playing track adapter, they will get a Toast notifying them
        // which track is currently playing
        @Override
        public void onClick(View view) {
            Toast.makeText(context, "Track " + track.getName() + " playing: " + track.getTempo() + " bpm", Toast.LENGTH_SHORT).show();
        }

    }

    // constructor for PlayingTrackAdapter
    // pass in currently playing track array (one item) and the PedometerActivity context
    public PlayingTrackAdapter(ArrayList<Track> tracks, Context context) {
        mContext = context;
        playingTracks = tracks;
    }

    // inflate the adapter_playingtrack layout to the ViewHolder
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                         int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_playingtrack, null, false);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutView.setLayoutParams(lp);

        // ViewHolder is one row of the list - holds one track with its information
        ViewHolder vh = new ViewHolder(layoutView, mContext);
        return vh;
    }

    // set the name and image of the track if available
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // set the track, its name, and tempo (in bpm)
        holder.track = playingTracks.get(position);
        holder.txtName.setText(playingTracks.get(position).getName());
        holder.txtArtist.setText(playingTracks.get(position).getArtist());
        holder.txtAlbum.setText(playingTracks.get(position).getAlbumName());
        holder.txtTempo.setText(playingTracks.get(position).getTempo()+" BPM");

        // if the track has an image url, load the image using Glide framework
        if (holder.track.getImageURL() != null) {
            /* Glide is an image loading framework for Android
             * primary focus is on making scrolling any kind of a list of images as smooth and fast as possible
             * also effective for any case where you need to fetch, resize, and display a remote image
             */
            Glide.with(mContext).load(playingTracks.get(position).getImageURL()).into(holder.imgTrack);
        }
    }

    // get the number of tracks displayed in the list
    @Override
    public int getItemCount() {
        return playingTracks.size();
    }
}

