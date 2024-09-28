package com.anurupjaiswal.ajmediapicker.basic.customgallery

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.VideoView
import androidx.fragment.app.Fragment
import com.anurupjaiswal.ajmediapicker.R
import com.bumptech.glide.Glide
import com.anurupjaiswal.ajmediapicker.databinding.FragmentMediaBinding

/**
 * MediaFragment is responsible for displaying media items (videos or images) in the fragment.
 * It handles the play/pause functionality for videos, as well as mute/unmute controls.
 */
class MediaFragment(private val mediaItem: MediaItem) : Fragment() {

    private var _binding: FragmentMediaBinding? = null
    private val binding get() = _binding!!

    private lateinit var handler: Handler
    private var isMuted: Boolean = true
    private var isPlaying: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout using View Binding
        _binding = FragmentMediaBinding.inflate(inflater, container, false)
        handler = Handler(Looper.getMainLooper())

        if (mediaItem.type == MediaType.VIDEO) {
            setupVideoPlayer(mediaItem.uri)
        } else if (mediaItem.type == MediaType.IMAGE) {
            setupImageViewer(mediaItem.uri)
        }

        return binding.root
    }

    /**
     * Set up the video player for the provided video URI.
     * Handles the play/pause, mute/unmute, and looping behavior of the video.
     */
    private fun setupVideoPlayer(uri: Uri?) {
        binding.apply {
            videoView.visibility = View.VISIBLE
            imageView.visibility = View.GONE
            videoView.setVideoURI(uri)

            playPauseButton.visibility = View.GONE
            muteButton.visibility = View.VISIBLE

            videoView.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = true
                mediaPlayer.start()
                toggleMute(mediaPlayer)
            }

            videoView.setOnCompletionListener {
                videoView.start()
            }

            videoView.setOnClickListener {
                if (isPlaying) {
                    videoView.pause()
                    showPlayPauseIcon(R.drawable.ic_play)
                } else {
                    videoView.start()
                    showPlayPauseIcon(R.drawable.ic_pause)
                }
                isPlaying = !isPlaying
            }

            muteButton.setOnClickListener {
                isMuted = !isMuted
                toggleMute(videoView.mediaPlayer)
                muteButton.setImageResource(
                    if (isMuted) R.drawable.ic_mute else R.drawable.ic_volume
                )
            }
        }
    }

    /**
     * Set up the image viewer using Glide for loading the image from the provided URI.
     */
    private fun setupImageViewer(uri: Uri?) {
        binding.apply {
            videoView.visibility = View.GONE
            imageView.visibility = View.VISIBLE
            playPauseButton.visibility = View.GONE
            muteButton.visibility = View.GONE

            Glide.with(this@MediaFragment)
                .load(uri)
                .into(imageView)
        }
    }

    /**
     * Toggle the mute state of the video player.
     */
    private fun toggleMute(mediaPlayer: MediaPlayer?) {
        mediaPlayer?.setVolume(if (isMuted) 0f else 1f, if (isMuted) 0f else 1f)
    }

    /**
     * Extension property to access the internal MediaPlayer of the VideoView.
     */
    private val VideoView.mediaPlayer: MediaPlayer?
        get() {
            try {
                val field = VideoView::class.java.getDeclaredField("mMediaPlayer")
                field.isAccessible = true
                return field.get(this) as? MediaPlayer
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

    /**
     * Show the play/pause icon for a short duration and then hide it.
     */
    private fun showPlayPauseIcon(iconResId: Int) {
        binding.playPauseButton.setImageResource(iconResId)
        binding.playPauseButton.visibility = View.VISIBLE
        handler.postDelayed({
            binding.playPauseButton.visibility = View.GONE
        }, 2000) // Hide after 2 seconds
    }

    /**
     * Clear the View Binding reference when the view is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
