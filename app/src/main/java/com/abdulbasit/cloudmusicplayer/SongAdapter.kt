package com.abdulbasit.cloudmusicplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.abdulbasit.cloudmusicplayer.R
import com.abdulbasit.cloudmusicplayer.Song

class SongAdapter(
    private val songs: List<Song>,
    private val onSongClick: (Song) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.bind(song, onSongClick)
    }

    override fun getItemCount(): Int = songs.size

    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // XML ki exact ID 'tvSongName' se connect kiya hai
        private val tvSongName: TextView = itemView.findViewById(R.id.tvSongName)

        fun bind(song: Song, onSongClick: (Song) -> Unit) {
            // Hamare model ka title ab is textview mein set hoga
            tvSongName.text = song.title

            itemView.setOnClickListener {
                onSongClick(song)
            }
        }
    }
}