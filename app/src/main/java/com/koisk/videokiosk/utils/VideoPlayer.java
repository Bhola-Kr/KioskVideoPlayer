package com.koisk.videokiosk.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.koisk.videokiosk.storage.LocalData;

import java.io.File;
import java.util.List;

public class VideoPlayer {

    private Context context;
    private VideoView videoView;
    private ImageView imageView;
    private List<File> mediaFiles;
    private int currentMediaIndex;
    private boolean isPlaying;
    private Handler handler;
    private Runnable nextMediaRunnable;

    public VideoPlayer(Context context, VideoView videoView, ImageView imageView) {
        this.context = context;
        this.videoView = videoView;
        this.imageView = imageView;
        this.mediaFiles = LocalData.allMediaList;
        this.currentMediaIndex = 0;
        this.isPlaying = false;
        this.handler = new Handler();
    }

    public void videoSetup() {
        if (!mediaFiles.isEmpty()) {
            isPlaying = true;
            playNextMedia();
        }
    }

    public void stopPlayback() {
        isPlaying = false;
        videoView.stopPlayback();
        videoView.setOnCompletionListener(null);
        videoView.setOnErrorListener(null);
        handler.removeCallbacks(nextMediaRunnable);
    }

    private void playNextMedia() {
        if (!isPlaying) {
            return;
        }

        if (currentMediaIndex >= mediaFiles.size()) {
            // Restart from the first media file if the index is out of bounds
            currentMediaIndex = 0;
        }

        File mediaFile = mediaFiles.get(currentMediaIndex);
        String filePath = mediaFile.getAbsolutePath();

        if (filePath.endsWith(".mp4")) {
            imageView.setVisibility(ImageView.GONE);
            videoView.setVisibility(VideoView.VISIBLE);
            videoView.setVideoURI(Uri.fromFile(mediaFile));
            videoView.setOnCompletionListener(mp -> {
                currentMediaIndex++;
                playNextMedia();
            });
            videoView.setOnErrorListener((mp, what, extra) -> {
                // Error occurred while playing video, skip to the next media
                currentMediaIndex++;
                playNextMedia();
                return true; // Returning true indicates that the error has been handled
            });
            videoView.start();
        } else if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg") || filePath.endsWith(".png")) {
            videoView.setVisibility(VideoView.GONE);
            imageView.setVisibility(ImageView.VISIBLE);
            Glide.with(context).load(mediaFile).into(imageView);
            nextMediaRunnable = () -> {
                currentMediaIndex++;
                playNextMedia();
            };
            try {
                handler.postDelayed(nextMediaRunnable, 1000 * LocalData.imageDisplayInterval);
            } catch (Exception e) {
                Log.d("error", e.getLocalizedMessage());
            }
        } else {
            // Invalid file format, skip to the next media
            currentMediaIndex++;
            playNextMedia();
        }
    }
}
