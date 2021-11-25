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
import com.example.spotifydemo.Model.Playlist;
import com.example.spotifydemo.R;

import java.util.ArrayList;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {
    private ArrayList<Playlist> userPlaylists;
    private Context mContext;

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView txtName;
        public ImageView imgView;

        private SharedPreferences.Editor editor;
        private SharedPreferences sharedPreferences;

        // Playlist object being stored in this row of the recycler view list
        private Playlist playlist;
        // context for MainActivity
        private Context context;

        // each item in the recycler view is a ViewHolder
        public ViewHolder(View itemView, Context context) {
            super(itemView);

            // get the sharedPreferences from PlaylistActivity
            this.context = context;
            sharedPreferences = context.getSharedPreferences("SPOTIFY", 0);

            itemView.setOnClickListener(this);

            txtName = (TextView) itemView.findViewById(R.id.txtPlaylistName);
            imgView = (ImageView) itemView.findViewById(R.id.imgPlaylist);
        }

        // Clicked on a playlist in the list of user's playlists
        // Go to TrackActivity to see the tracks of the selected playlist
        @Override
        public void onClick(View view) {
            Toast.makeText(context, "Playlist " + playlist.getName() + " selected", Toast.LENGTH_SHORT).show();


        }


    }

    // constructor for PlaylistAdapter
    // pass in playlists to show and the PlaylistActivity context
    public PlaylistAdapter(ArrayList<Playlist> playlists, Context context) {
        mContext = context;
        userPlaylists = playlists;
    }

    // inflate the adapter_playlist layout to the ViewHolder
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                           int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_playlist, null, false);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutView.setLayoutParams(lp);

        ViewHolder vh = new ViewHolder(layoutView, mContext);
        return vh;
    }

    // set the name and image of the playlist if available
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.txtName.setText(userPlaylists.get(position).getName());
        holder.playlist = userPlaylists.get(position);
        // if the playlist has an image url, load the image using Glide framework
        if (holder.playlist.getImageURL() != null) {
            /* Glide is an image loading framework for Android
            * primary focus is on making scrolling any kind of a list of images as smooth and fast as possible
            * also effective for any case where you need to fetch, resize, and display a remote image
            */
            Glide.with(mContext).load(userPlaylists.get(position).getImageURL()).into(holder.imgView);
        }
    }

    // get the number of playlists displayed
    @Override
    public int getItemCount() {
        return userPlaylists.size();
    }
}