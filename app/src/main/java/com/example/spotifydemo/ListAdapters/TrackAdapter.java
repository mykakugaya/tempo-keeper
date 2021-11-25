package com.example.spotifydemo.ListAdapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.spotifydemo.Model.Track;
import com.example.spotifydemo.R;
import com.example.spotifydemo.SpotifyConnector.TrackService;

import java.util.ArrayList;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.ViewHolder> {
    private ArrayList<Track> playlistTracks;
    private Context mContext;

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView txtName;
        public TextView txtArtist;
        public TextView txtAlbum;
        public TextView txtTempo;
        public ImageView imgTrack;

        private SharedPreferences sharedPreferences;

        // track object being stored in this row of the recycler view list
        private Track track;
        // context for TrackActivity
        private Context context;

        // each item in the recycler view is a ViewHolder
        public ViewHolder(View itemView, Context context) {
            super(itemView);

            // get the sharedPreferences from trackActivity
            this.context = context;
            sharedPreferences = context.getSharedPreferences("SPOTIFY", 0);

            itemView.setOnClickListener(this);

            // each track ViewHolder has an image, name, and number of tracks
            imgTrack = (ImageView) itemView.findViewById(R.id.imgTrack);
            txtName = (TextView) itemView.findViewById(R.id.txtTrackName);
            txtArtist = (TextView) itemView.findViewById(R.id.txtArtist);
            txtAlbum = (TextView) itemView.findViewById(R.id.txtAlbum);
            txtTempo = (TextView) itemView.findViewById(R.id.txtTempo);
        }

        // Clicked on a track in the list of user's tracks
        // Go to TrackActivity to see the tracks of the selected track
        @Override
        public void onClick(View view) {
            Toast.makeText(context, "Track " + track.getName() + " selected", Toast.LENGTH_SHORT).show();

        }

    }

    // constructor for TrackAdapter
    // pass in tracks to show and the trackActivity context
    public TrackAdapter(ArrayList<Track> tracks, Context context) {
        mContext = context;
        playlistTracks = tracks;
    }

    // inflate the adapter_track layout to the ViewHolder
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                         int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_track, null, false);
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
        holder.track = playlistTracks.get(position);
        holder.txtName.setText(playlistTracks.get(position).getName());
        holder.txtArtist.setText(playlistTracks.get(position).getArtist());
        holder.txtAlbum.setText(playlistTracks.get(position).getAlbumName());
        holder.txtTempo.setText(playlistTracks.get(position).getTempo()+" BPM");

        // if the track has an image url, load the image using Glide framework
        if (holder.track.getImageURL() != null) {
            /* Glide is an image loading framework for Android
             * primary focus is on making scrolling any kind of a list of images as smooth and fast as possible
             * also effective for any case where you need to fetch, resize, and display a remote image
             */
            Glide.with(mContext).load(playlistTracks.get(position).getImageURL()).into(holder.imgTrack);
        }
    }

    // get the number of tracks displayed in the list
    @Override
    public int getItemCount() {
        return playlistTracks.size();
    }
}
