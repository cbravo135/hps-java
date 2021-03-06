package org.hps.recon.utils;

import org.lcsim.event.Cluster;
import org.lcsim.event.Track;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.geometry.FieldMap;
import org.hps.record.StandardCuts;
import java.util.List;
import java.util.HashMap;

/**
 * This is an interface for creating TrackClusterMatcher algorithms used in
 * reconstruction.
 */
public interface TrackClusterMatcher {

    /**
     * Return a map of Tracks with matched Clusters.
     * Tracks may be returned with null Cluster match.
     */
    HashMap<Track,Cluster> matchTracksToClusters(EventHeader event, List<List<Track>> trackCollections, List<Cluster> clusters, StandardCuts cuts, int flipSign, boolean useCorrectedClusterPositionsForMatching, boolean isMC, HPSEcal3 ecal, double beamEnergy);

    /**
     * If set to true in steering-file, apply cluster corrections before
     * matching Tracs to Clusters.
     */
    void applyClusterCorrections(boolean useTrackPositionClusterCorrection, List<Cluster> clusters, double beamEnergy, HPSEcal3 ecal, boolean isMC);

    /**
     * Return match quality of Track+Cluster pair
     */
    double getMatchQC(Cluster cluster, ReconstructedParticle particle);

    /**
     * Set BField Map
     */
    void setBFieldMap(FieldMap bFieldMap);

    /**
     * Set beam energy
     */
    void setBeamEnergy(double beamEnergy);
    
    void setSnapToEdge(boolean val);

    /**
     * Pass generic parameterization file to matcher
     */
    void initializeParameterization(String fname);

    /**
     * Set Track collection name to GBLTracks or KalmanFullTracks
     */
    void setTrackCollectionName(String trackCollectionName);

    void enablePlots(boolean enablePlots);

    void saveHistograms();


}

