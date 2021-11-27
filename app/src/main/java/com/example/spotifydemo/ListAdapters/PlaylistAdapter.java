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
import com.example.spotifydemo.Model.Playlist;
import com.example.spotifydemo.R;
import com.example.spotifydemo.TrackActivity;

import java.util.ArrayList;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {
    private ArrayList<Playlist> userPlaylists;
    private Context mContext;

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView txtName;
        public TextView txtNumTracks;
        public ImageView imgView;
        public LinearLayout layoutPlaylist;

        // has the user selected the playlist? clicking on a playlist -> isSelected = true
        public boolean isSelected;

        // Playlist object being stored in this row of the recycler view list
        private Playlist playlist;

        // context for PlaylistActivity, as well as sharedPreferences
        private Context context;
        private SharedPreferences sharedPref;
        private SharedPreferences.Editor editor;

        // each item in the recycler view is a ViewHolder
        public ViewHolder(View itemView, Context context) {
            super(itemView);

            // get the sharedPreferences from PlaylistActivity
            this.context = context;
            this.sharedPref = context.getSharedPreferences("SPOTIFY",0);

            // set clickable
            itemView.setOnClickListener(this);

            // each playlist ViewHolder has an image, name, and number of tracks
            imgView = (ImageView) itemView.findViewById(R.id.imgPlaylist);
            txtName = (TextView) itemView.findViewById(R.id.txtPlaylistName);
            txtNumTracks = (TextView) itemView.findViewById(R.id.txtNumTracks);
            layoutPlaylist = (LinearLayout) itemView.findViewById(R.id.layoutPlaylistInfo);

        }

        // User clicked on a playlist from the list of playlists
        @Override
        public void onClick(View view) {
            // Toast notifies which playlist was clicked
            Toast.makeText(context, "Playlist " + playlist.getName() + " selected", Toast.LENGTH_SHORT).show();

            // if isSelected is false, set to true, and vice versa
            isSelected = !isSelected;

            // if isSelected is true, set the background color of the track to green to indicate
            // that it has been selected
            if (isSelected) {
                // Make a toast indicating which track was selected at which bpm
                Toast.makeText(context, "Playlist " + playlist.getName() + " selected", Toast.LENGTH_SHORT).show();
                layoutPlaylist.setBackgroundColor(Color.GREEN);
                // save the selected tempo in sharedPreferences
                // we will use it in TrackActivity when the filter tempos button is clicked
                editor = sharedPref.edit();
                editor.putString("PLAYLISTNAME", playlist.getName());
                editor.putString("PLAYLISTID", playlist.getId());
                editor.commit();

            } else {    // if the item is clicked twice, background is set to transparent (unselecting)
                layoutPlaylist.setBackgroundColor(Color.TRANSPARENT);
                // clear previously selected track from sharedPreferences
                editor = sharedPref.edit();
                editor.putString("PLAYLISTNAME", "");
                editor.putString("PLAYLISTID", "");
                editor.commit();
            }

        }

    }

    // constructor for PlaylistAdapter
    // pass in playlists to show and the PlaylistActivity context
    public PlaylistAdapter(ArrayList<Playlist> playlists, Context context) {
        userPlaylists = playlists;
        mContext = context;
    }

    // inflate the adapter_playlist layout to the ViewHolder
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                           int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_playlist, null, false);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutView.setLayoutParams(lp);

        // ViewHolder is one row of the list - holds one playlist
        ViewHolder vh = new ViewHolder(layoutView, mContext);
        return vh;
    }

    // set the name and image of the playlist if available
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // set the playlist, its name, and number of tracks
        holder.playlist = userPlaylists.get(position);
        holder.txtName.setText(userPlaylists.get(position).getName());
        holder.txtNumTracks.setText(userPlaylists.get(position).getNumTracks()+" tracks");

        // if the playlist has an image url, load the image using Glide framework
        if (holder.playlist.getImageURL() != null) {
            /* Glide is an image loading framework for Android
            * primary focus is on making scrolling any kind of a list of images as smooth and fast as possible
            * also effective for any case where you need to fetch, resize, and display a remote image
            */
            Glide.with(mContext).load(userPlaylists.get(position).getImageURL()).into(holder.imgView);
        }
    }

    // get the number of playlists displayed in the list
    @Override
    public int getItemCount() {
        return userPlaylists.size();
    }
}