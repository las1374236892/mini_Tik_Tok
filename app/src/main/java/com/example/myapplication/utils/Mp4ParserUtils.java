package com.example.myapplication.utils;

import android.content.Context;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Mp4ParserUtils {
    /**
     * 合并视频
     *
     * @param videoList
     * @param mergeVideoFile
     * @return
     */
    public static String mergeVideo(ArrayList<String> videoList, File mergeVideoFile) {
        FileOutputStream fos = null;
        FileChannel fc = null;
        try {
            List<Movie> sourceMovies = new ArrayList<>();
            for (String video : videoList) {
                System.out.println("adding "+video);
                sourceMovies.add(MovieCreator.build(video));
            }

            List<Track> videoTracks = new LinkedList<>();
            List<Track> audioTracks = new LinkedList<>();

            for (Movie movie : sourceMovies) {
                for (Track track : movie.getTracks()) {
                    if ("soun".equals(track.getHandler())) {
                        audioTracks.add(track);
                    }

                    if ("vide".equals(track.getHandler())) {
                        videoTracks.add(track);
                    }
                }
            }

            Movie mergeMovie = new Movie();
            if (audioTracks.size() > 0) {
                mergeMovie.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
            }

            if (videoTracks.size() > 0) {
                mergeMovie.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
            }

            Container out = new DefaultMp4Builder().build(mergeMovie);
            fos = new FileOutputStream(mergeVideoFile);
            fc = fos.getChannel();
            out.writeContainer(fc);
            fc.close();
            fos.close();
            return mergeVideoFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fc != null) {
                try {
                    fc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    private static double correctTimeToSyncSample(Track track, double cutHere, boolean next) {
        double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
        long currentSample = 0;
        double currentTime = 0;
        for (int i = 0; i < track.getSampleDurations().length; i++) {
            long delta = track.getSampleDurations()[i];

            if (Arrays.binarySearch(track.getSyncSamples(), currentSample + 1) >= 0) {
                // samples always start with 1 but we start with zero therefore +1
                timeOfSyncSamples[Arrays.binarySearch(track.getSyncSamples(), currentSample + 1)] = currentTime;
            }
            currentTime += (double) delta / (double) track.getTrackMetaData().getTimescale();
            currentSample++;

        }
        double previous = 0;
        for (double timeOfSyncSample : timeOfSyncSamples) {
            if (timeOfSyncSample > cutHere) {
                if (next) {
                    return timeOfSyncSample;
                } else {
                    return previous;
                }
            }
            previous = timeOfSyncSample;
        }
        return timeOfSyncSamples[timeOfSyncSamples.length - 1];
    }

}

