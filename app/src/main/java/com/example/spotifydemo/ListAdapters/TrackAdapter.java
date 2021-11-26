package com.example.spotifydemo.ListAdapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.spotifydemo.Model.Track;
import com.example.spotifydemo.PlaybackActivity;
import com.example.spotifydemo.R;
import com.example.spotifydemo.SpotifyConnector.PlaybackService;
import com.example.spotifydemo.SpotifyConnector.TrackService;
import com.example.spotifydemo.TrackActivity;

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
        public LinearLayout layoutTrack;
        public boolean isSelected;

        // sharedPreferences and editor for saving the selected track
        private SharedPreferences sharedPreferences;
        private SharedPreferences.Editor editor;

        // track object being stored in this row of the recycler view list
        private Track track;
        // context for TrackActivity
        private Context context;

        // each item in the recycler view is a ViewHolder
        public ViewHolder(View itemView, Context context) {
            super(itemView);

            // ViewHolder context is the same as TrackAdapter mContext
            // both the context of TrackActivity
            this.context = context;

            // create new sharedPreferences to store the selected tempo
            sharedPreferences = context.getSharedPreferences("SPOTIFY", 0);

            // set clickable
            itemView.setOnClickListener(this);

            // When a user clicks on a track, isSelected is set to true so that
            // we can set the color of the track information background

            // each track ViewHolder has an image, name, and number of tracks
            imgTrack = (ImageView) itemView.findViewById(R.id.imgTrack);
            txtName = (TextView) itemView.findViewById(R.id.txtTrackName);
            txtArtist = (TextView) itemView.findViewById(R.id.txtArtist);
            txtAlbum = (TextView) itemView.findViewById(R.id.txtAlbum);
            txtTempo = (TextView) itemView.findViewById(R.id.txtTempo);
            layoutTrack = (LinearLayout) itemView.findViewById(R.id.layoutTrackInfo);
        }

        // Clicked on a track in the list of user's tracks
        // Go to TrackActivity to see the tracks of the selected track
        @Override
        public void onClick(View view) {
            // if isSelected is false, set to true, and vice versa
            isSelected = !isSelected;

            // if isSelected is true, set the background color of the track to green to indicate
            // that it has been selected
            if (isSelected) {
                // Make a toast indicating which track was selected at which bpm
                Toast.makeText(context, "Track " + track.getName() + " selected: "+track.getTempo()+" bpm", Toast.LENGTH_SHORT).show();
                layoutTrack.setBackgroundColor(Color.GREEN);
            } else {    // if the item is clicked twice, background is set to transparent (unselecting)
                layoutTrack.setBackgroundColor(Color.TRANSPARENT);
            }

            // save the selected tempo in sharedPreferences
            // we will use it in TrackActivity when the filter tempos button is clicked
            editor = sharedPreferences.edit();
            editor.putString("TEMPO", Double.toString(track.getTempo()));
            editor.putString("TRACK", track.getId());
            editor.commit();

            Log.d("TRACK", "SELECTED TEMPO: "+track.getTempo());
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

        // Was the track selected by the user?
        // Get currently selected track id from sharedPreferences if available
        String selectedTrack = holder.sharedPreferences.getString("TRACK","");
        if(!selectedTrack.equals("") && holder.track.getId().equals(selectedTrack)) {
            // if the saved track id = this holder's track id, the track has previously been clicked
            // set background color to green
            holder.layoutTrack.setBackgroundColor(Color.GREEN);
            holder.isSelected = true;
        } else {
            // else, it has not been clicked and background color is transparent
            holder.layoutTrack.setBackgroundColor(Color.TRANSPARENT);
            holder.isSelected = false;
        }
    }

    // get the number of tracks displayed in the list
    @Override
    public int getItemCount() {
        return playlistTracks.size();
    }
}
