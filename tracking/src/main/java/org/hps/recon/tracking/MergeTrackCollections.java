package org.hps.recon.tracking;

import hep.aida.IHistogram1D;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.record.StandardCuts;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Read all track collections in the event, use ambiguity resolver, and put the
 * resulting list of unique tracks in a new collection. Remove the original
 * track collections (this behavior can be disabled). Produce some basic plots
 * (can be disabled) in mergingPlots.aida.
 */
public class MergeTrackCollections extends Driver {

    private String outputCollectionName = "MatchedTracks";
    // private String partialTrackCollectionName = "PartialTracks";
    //private String inputTrackCollectionName = "";
    private Set<String> inputTrackCollectionName = new HashSet<String>();
    private boolean removeCollections = true;
    private boolean doPlots = false;
    boolean isTransient = false;
    private AmbiguityResolver ambi;
    // private AcceptanceHelper acc;
    private StandardCuts cuts = new StandardCuts();
    private AIDA aida2 = AIDA.defaultInstance();

    private IHistogram1D trackScoresPreAmbi;
    private IHistogram1D trackScoresPostAmbi;
    private IHistogram1D numDuplicateTracks;
    private IHistogram1D numPartialTracks;
    private IHistogram1D numSharedTracks;
    private IHistogram1D sharedHitsPreAmbi;
    private IHistogram1D numTracksPreAmbi;
    private IHistogram1D numTracksPostAmbi;
    private IHistogram1D sharedHitsPostAmbi;
    private IHistogram1D numHitsPreAmbi;
    private IHistogram1D numHitsPostAmbi;

    /**
     * Default constructor
     */

    public void setMaxSharedHitsPerTrack(int input) {
        cuts.setMaxSharedHitsPerTrack(input);
    }

    public void setPlots(boolean value) {
        doPlots = value;
    }

    /**
     * determines if the output collections will be transient or not
     * 
     * @param val
     */
    public void setIsTransient(boolean val) {
        this.isTransient = val;
    }

    /**
     * Name of the LCIO collection containing all good tracks.
     *
     * @param outputCollectionName
     *            Defaults to MatchedTracks.
     */
    public void setOutputCollectionName(String outputCollectionName) {
        this.outputCollectionName = outputCollectionName;
    }

    public void setDoPlots(boolean value) {
        doPlots = value;
    }

    /*
     * public void setPartialTrackCollectionName(String
     * partialTrackCollectionName) { this.partialTrackCollectionName =
     * partialTrackCollectionName; }
     */

    /**
     * Name of the LCIO collection containing input tracks.
     *
     * @param inputTrackCollectionName
     *            Defaults to "" which means take all track collections in file
     */
    public void setInputTrackCollectionName(String[] name) {
        this.inputTrackCollectionName = new HashSet<String>(Arrays.asList(name));
    }
    /**
     * Remove existing track collections after merging them.
     *
     * @param removeCollections
     *            Default to true.
     */
    public void setRemoveCollections(boolean removeCollections) {
        this.removeCollections = removeCollections;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        // if Ambiguity Resolver uses acceptance helper, must initialize here
        // acc = new AcceptanceHelper();
        // acc.initializeMaps(detector, "Tracker");
        ambi = new SimpleAmbiguityResolver();

        if (doPlots) {
            trackScoresPreAmbi = aida2.histogram1D("trackScoresPreAmbi", 200, -100, 100);
            trackScoresPostAmbi = aida2.histogram1D("trackScoresPostAmbi", 200, -100, 100);
            numDuplicateTracks = aida2.histogram1D("numDuplicateTracks", 10, 0, 10);
            numPartialTracks = aida2.histogram1D("numPartialTracks", 10, 0, 10);
            numSharedTracks = aida2.histogram1D("numSharedTracks", 10, 0, 10);
            sharedHitsPreAmbi = aida2.histogram1D("sharedHitsPreAmbi", 10, 0, 10);
            numTracksPreAmbi = aida2.histogram1D("numTracksPreAmbi", 10, 0, 10);
            numTracksPostAmbi = aida2.histogram1D("numTracksPostAmbi", 10, 0, 10);
            sharedHitsPostAmbi = aida2.histogram1D("sharedHitsPostAmbi", 10, 0, 10);
            numHitsPreAmbi = aida2.histogram1D("numHitsPreAmbi", 10, 0, 10);
            numHitsPostAmbi = aida2.histogram1D("numHitsPostAmbi", 10, 0, 10);
        }

    }

    @Override
    public void process(EventHeader event) {

        List<List<Track>> trackCollections;
        List<List<Track>> trackCollectionsForAmbi;

        //if (inputTrackCollectionName == "") {
        if (inputTrackCollectionName.isEmpty()) {
            //System.out.println("PF::DEBUG:: Empty inputTrackCollectionName");
            
            trackCollections = event.get(Track.class);
            trackCollectionsForAmbi = new ArrayList<List<Track>>();
            
            for (List<Track> trkList : trackCollections) {
                
                List<Track> TMP_trackList = new ArrayList<Track>();
                
                for (Track trk : trkList) {
                    
                    if (trk instanceof SeedTrack) {
                        TMP_trackList.add(trk);
                    }
                }

                if (TMP_trackList.size()>0) { 
                    trackCollectionsForAmbi.add(TMP_trackList);
                }
            }
            
                
        } else {
            trackCollections        = new ArrayList<List<Track>>();
            trackCollectionsForAmbi = new ArrayList<List<Track>>();
            for(String inputTrack:inputTrackCollectionName){
                List<Track> temp = event.get(Track.class, inputTrack);
                trackCollections.add(temp);
                trackCollectionsForAmbi.add(temp);
            }
            //List<Track> temp = event.get(Track.class, inputTrackCollectionName);
            //trackCollections.add(temp);
        }
        
        //System.out.println("PF::DEBUG:: CHECK TRACK COLLECTIONS");
        //for (List<Track> trk : trackCollectionsForAmbi) {
        //    System.out.println(trk.toString());
        //}
        
        
        if (doPlots) {
            numTracksPreAmbi.fill(ambi.getTracks().size());
            for (Track trk : ambi.getTracks()) {
                trackScoresPreAmbi.fill(ambi.getScore(trk));
                sharedHitsPreAmbi.fill(TrackUtils.numberOfSharedHits(trk, ambi.getTracks()));
                numHitsPreAmbi.fill(trk.getTrackerHits().size());
            }
        }

        // Classic Ambiguity Resolving
        // ((ClassicAmbiguityResolver) (ambi)).setDoChargeCheck(true);

        ambi.resetResolver();
        ambi.initializeFromCollection(trackCollectionsForAmbi);
        // simple ambi-resolver
        ((SimpleAmbiguityResolver) (ambi)).setMode(SimpleAmbiguityResolver.AmbiMode.DUPS);
        ambi.resolve();
        ((SimpleAmbiguityResolver) (ambi)).setMode(SimpleAmbiguityResolver.AmbiMode.PARTIALS);
        ambi.resolve();
        //((SimpleAmbiguityResolver) (ambi)).setMode(SimpleAmbiguityResolver.AmbiMode.SHARED);
        //((SimpleAmbiguityResolver) (ambi)).setShareThreshold(cuts.getMaxSharedHitsPerTrack());
        //ambi.resolve();
        List<Track> deduplicatedTracks = ambi.getTracks();

        // List<Track> partialTracks = ambi.getPartialTracks();

        if (doPlots) {
            numTracksPostAmbi.fill(deduplicatedTracks.size());
            numPartialTracks.fill(ambi.getPartialTracks().size());
            numDuplicateTracks.fill(ambi.getDuplicateTracks().size());
            numSharedTracks.fill(ambi.getSharedTracks().size());

            for (Track trk : deduplicatedTracks) {
                trackScoresPostAmbi.fill(ambi.getScore(trk));
                sharedHitsPostAmbi.fill(TrackUtils.numberOfSharedHits(trk, deduplicatedTracks));
                numHitsPostAmbi.fill(trk.getTrackerHits().size());
            }
        }

        
        if (removeCollections) {
            for (List<Track> tracklist : trackCollections) {
                event.remove(event.getMetaData(tracklist).getName());
            }
        }

        //System.out.println("De-duplicated track list");
        //System.out.println(deduplicatedTracks.toString());

        int flag = 1 << LCIOConstants.TRBIT_HITS;
        event.put(outputCollectionName, deduplicatedTracks, Track.class, flag);
        // event.put(partialTrackCollectionName, partialTracks, Track.class,
        // flag);
        if (isTransient) {
            event.getMetaData(deduplicatedTracks).setTransient(isTransient);
            // event.getMetaData(partialTracks).setTransient(isTransient);
        }
    }

    @Override
    protected void endOfData() {
        super.endOfData();
        if (doPlots) {
            File outputFile2 = new File("mergingPlots.root");
            outputFile2.getParentFile().mkdirs();
            try {
                aida2.saveAs(outputFile2);
            } catch (IOException ex) {
                Logger.getLogger(MergeTrackCollections.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
