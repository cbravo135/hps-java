package org.hps.recon.tracking.kalman;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
//import org.hps.conditions.beam.BeamPosition;
//import org.hps.conditions.beam.BeamPosition.BeamPositionCollection;
//import org.hps.conditions.database.DatabaseConditionsManager;

/**
 * Parameters used by the Kalman-Filter pattern recognition and fitting
 * @author Robert Johnson
 *
 */ 
public class KalmanParams {
    static final int mxTrials = 2;  // Max number of iterations through the entire pattern recognition; not configurable
    int nTrials;                    // Number of iterations through the entire pattern recognition
    int nIterations;                // Number of Kalman-fit iterations in the final fit
    double[] kMax; 
    double kMin;
    double[] tanlMax; 
    double[] dRhoMax; 
    double[] dzMax;
    double[] chi2mx1; 
    int minHits0;
    int[] minHits1; 
    double mxChi2Inc; 
    double minChi2IncBad;
    double mxChi2Vtx;
    double[] mxResid; 
    double mxResidShare; 
    double mxChi2double;
    int firstLayer;
    int mxShared; 
    int [] minStereo;
    int minAxial;
    double mxTdif;
    double lowPhThresh;
    double seedCompThr;           // Compatibility threshold for seedTracks helix parameters;
    ArrayList<int[]> [] lyrList;
    double [] beamSpot;
    double [] vtxSize;
    double [] minSeedE;
    static final int numLayers = 14;
    
    private int[] Swap = {1,0, 3,2, 5,4, 7,6, 9,8, 11,10, 13,12};
    private String [] tb;
    private Logger logger;
    int maxListIter1;
    
    public void print() {
        System.out.format("\nKalmanParams: dump of the Kalman pattern recognition cuts and parameters\n");
        System.out.println("  (In the case of two values, they refer to the two iterations.)");
        System.out.format("  There are %d layers in the tracker.\n", numLayers);
        System.out.format("  Cluster energy cuts for seeds, by layer: ");
        for (int lyr=0; lyr<numLayers; ++lyr) {
            System.out.format(" %6.2f", minSeedE[lyr]);
        }
        System.out.format("\n");
        System.out.format("  Residual improvement ratio necessary to use a low-ph hit instead of high-ph = %8.2f\n", lowPhThresh);
        System.out.format("  First layer in the tracking system: %d\n", firstLayer);
        System.out.format("  Number of global iterations: %d\n", nTrials);
        System.out.format("  Number of Kalman filter iterations per track in the final fits: %d\n", nIterations);
        System.out.format("  Maximum seed curvature=%8.2f, %8.2f (1/GeV)\n", kMax[0], kMax[1]);
        System.out.format("  Minimum seed curvature=%8.2f (1/GeV)\n", kMin);
        System.out.format("  Maximum tan(lambda): %8.2f, %8.2f\n", tanlMax[0], tanlMax[1]);
        System.out.format("  Maximum dRho at target plane for seeds: %8.2f, %8.2f (mm)\n", dRhoMax[0], dRhoMax[1]);
        System.out.format("  Maximum dz at target plane for seeds: %8.2f, %8.2f (mm)\n", dzMax[0], dzMax[1]);
        System.out.format("  Maximum chi^2/hit for a good track: %8.2f, %8.2f\n", chi2mx1[0], chi2mx1[1]);
        System.out.format("  Minimum number of hits in the initial outward filtering: %d\n", minHits0);
        System.out.format("  Minimum number of hits for a good track: %d, %d\n", minHits1[0], minHits1[1]);
        System.out.format("  Minimum number of stereo hits: %d %d\n", minStereo[0], minStereo[1]);
        System.out.format("  Minimum number of axial hits: %d\n",  minAxial);
        System.out.format("  Maximum chi^2 increment to add a hit to a track: %8.2f\n", mxChi2Inc);
        System.out.format("  Chi^2 increment threshold for removing a bad hit from a track candidate: %8.2f\n", minChi2IncBad);
        System.out.format("  Maximum residual, in units of detector resolution, for picking up a hit: %8.2f, %8.2f\n", mxResid[0], mxResid[1]);
        System.out.format("  Maximum residual, in units of detector resolution, for a hit to be shared: %8.2f\n", mxResidShare);
        System.out.format("  Maximum chi^2 increment to keep a shared hit: %8.2f\n", mxChi2double);
        System.out.format("  Maximum number of shared hits on a track: %d\n",  mxShared);
        System.out.format("  Maximum time difference among the hits on a track: %8.2f ns\n", mxTdif);
        System.out.format("  Threshold to remove redundant seeds (-1 to disable): %8.2f\n", seedCompThr);
        System.out.format("  Maximum chi^2 for 5-hit tracks with a vertex constraint: %8.2f\n", mxChi2Vtx);
        System.out.format("  Default origin to use for vertex constraints:\n");
        for (int i=0; i<3; ++i) {
            System.out.format("      %d: %8.3f +- %8.3f\n", i, beamSpot[i], vtxSize[i]);
        }
        for (int i=0; i<2; ++i) {
            System.out.format("  Search strategies for %s:\n",tb[i]);
            for (int j=0; j<lyrList[i].size(); ++j) {
                int [] list = lyrList[i].get(j);
                System.out.format("     ");
                for (int k=0; k<5; ++k) {
                    System.out.format(" %d ", list[k]);
                }
                System.out.format("\n");
            }
        }
        System.out.format("  The number of seed strategies used in the first iteration is %d\n", maxListIter1+1);
        System.out.format("\n");
    }
    
    KalmanParams() {
        
        logger = Logger.getLogger(KalmanParams.class.getName());
        tb = new String[2];
        tb[0] = "bottom";
        tb[1] = "top";
        
        kMax = new double[mxTrials];
        tanlMax = new double[mxTrials];
        dRhoMax = new double[mxTrials];
        dzMax = new double[mxTrials];
        chi2mx1 = new double[mxTrials];
        minHits1 = new int[mxTrials];
        mxResid = new double[mxTrials];
        minStereo = new int[mxTrials];  
        
        minSeedE = new double[numLayers];
        for (int lyr=0; lyr<numLayers; ++lyr) {
            minSeedE[lyr] = 1.5;
        }
        
        // Set all the default values
        // Cut and parameter values (length units are mm, time is ns).
        // The index is the iteration number.
        // The second iteration generally will have looser cuts.
        nTrials = 2;        // Number of global iterations of the pattern recognition
        nIterations = 1;    // Number of Kalman filter iterations per track in the final fit
        kMax[0] = 4.0;      // Maximum curvature for seed
        kMax[1] = 8.0;      
        kMin = 0.;          // Minimum curvature for seed
        tanlMax[0] = 0.10;  // Maximum tan(lambda) for seed
        tanlMax[1] = 0.13;
        dRhoMax[0] = 15.;   // Maximum dRho at target plane for seed
        dRhoMax[1] = 25.;
        dzMax[0] = 4.;      // Maximum z at target plane for seed
        dzMax[1] = 10.;
        chi2mx1[0] = 8.0;   // Maximum chi**2/#hits for good track
        chi2mx1[1] = 16.0;  
        mxChi2Vtx = 1.0;    // Maximum chi**2 for 5-hit tracks with vertex constraint
        minHits0 = 5;       // Minimum number of hits in the initial outward filtering (including 5 from the seed)
        minHits1[0] = 7;    // Minimum number of hits for a good track
        minHits1[1] = 6;
        mxChi2Inc = 5.;     // Maximum increment to the chi^2 to add a hit to a completed track 
        minChi2IncBad = 10.; // Threshold for removing a bad hit from a track candidate
        mxResid[0] = 50.;   // Maximum residual, in units of detector resolution, for picking up a hit
        mxResid[1] = 100.;
        mxResidShare = 20.; // Maximum residual, in units of detector resolution, for a hit to be shared
        mxChi2double = 6.;  // Maximum chi^2 increment to keep a shared hit
        minStereo[0] = 4;
        minStereo[1] = 3;   // Minimum number of stereo hits
        minAxial = 2;       // Minimum number of axial hits
        mxShared = 2;       // Maximum number of shared hits
        mxTdif = 30.;       // Maximum time difference of hits in a track
        firstLayer = 0;     // First layer in the tracking system (2 for pre-2019 data)
        lowPhThresh = 0.25; // Residual improvement ratio necessary to use a low-ph hit instead of high-ph
        seedCompThr = -1.;  // Remove SeedTracks with all Helix params within relative seedCompThr . If -1 do not apply duplicate removal
        
        // Load the default search strategies
        // Index 0 is for the bottom tracker (+z), 1 for the top (-z)
        lyrList = new ArrayList[2];
        for (int i=0; i<2; ++i) {
            lyrList[i] = new ArrayList<int[]>();
        } 
        //                  0   1   2   3   4    5    6
        //                 A S A S A S A S A S A  S  A  S   top
        //                 0,1,2,3,4,5,6,7,8,9,10,11,12,13
        //                 S A S A S A S A S A S  A  S  A  bottom
        final int[] list0 = {6, 7, 8, 9, 10};
        final int[] list1 = {4, 5, 6, 7, 8};
        final int[] list2 = {5, 6, 8, 9, 10};
        final int[] list3 = {5, 6, 7, 8, 10};
        final int[] list4 = { 3, 6, 8, 9, 10 };
        final int[] list5 = { 4, 5, 8, 9, 10 };
        final int[] list6 = { 4, 6, 7, 8, 9 };
        final int[] list7 = { 4, 6, 7, 9, 10 };
        final int[] list8 = { 2, 5, 8, 9, 12};
        final int[] list9 = { 8, 10, 11, 12, 13};
        final int[] list10 = {6, 9, 10, 11, 12};
        final int[] list11 = {6, 7, 9, 10, 12};
        final int[] list12 = {2, 3, 4, 5, 6};
        final int[] list13 = {2, 4, 5, 6, 7};
        final int[] list14 = {6, 7, 8, 10, 11};
        final int[] list15 = {1, 2, 3, 4, 6};
        final int[] list16 = {0, 2, 3, 4, 5};
        final int[] list17 = {0, 3, 4, 5, 6};
        lyrList[0].add(list0);
        lyrList[0].add(list1);
        lyrList[0].add(list2);
        lyrList[0].add(list3);
        lyrList[0].add(list4);
        lyrList[0].add(list5);
        lyrList[0].add(list6);
        lyrList[0].add(list7);
        lyrList[0].add(list8);
        lyrList[0].add(list9);
        lyrList[0].add(list10);
        lyrList[0].add(list11);
        lyrList[0].add(list12);
        lyrList[0].add(list13);
        lyrList[0].add(list14);
        lyrList[0].add(list15);
        lyrList[0].add(list16);
        lyrList[0].add(list17);
        maxListIter1 = 14;           // The maximum index for lyrList for the first iteration
        
        beamSpot = new double[3];
        beamSpot[0] = 0.;
        beamSpot[1] = 0.;
        beamSpot[2] = 0.;
        vtxSize = new double[3];
        vtxSize[0] = 0.05;
        vtxSize[1] = 1.0;
        vtxSize[2] = 0.02;
//        final DatabaseConditionsManager mgr = DatabaseConditionsManager.getInstance();
//        if (mgr.hasConditionsRecord("beam_positions")) {
//            logger.config("Using default beam position from conditions database");
//            BeamPositionCollection beamPositions = 
//                    mgr.getCachedConditions(BeamPositionCollection.class, "beam_positions").getCachedData();
//            BeamPosition beamPositionCond = beamPositions.get(0);      
//            double [] beamPosHPS = {
//                    beamPositionCond.getPositionX(), 
//                    beamPositionCond.getPositionY(), 
//                    beamPositionCond.getPositionZ()
//                    };
//            Vec beamPosKal = KalmanInterface.vectorGlbToKalman(beamPosHPS);
//            beamSpot[0] = beamPosKal.v[0];
//            beamSpot[1] = beamPosKal.v[1];
//            beamSpot[2] = beamPosKal.v[2];
//        }            
        
        // Swap axial/stereo in list entries for the top tracker
        for (int[] list: lyrList[0]) {
            int [] listTop = new int[5];
            for (int i=0; i<5; ++i) {
                listTop[i] = Swap[list[i]];
            }
            for (int i=0; i<4; ++i) {
                if (listTop[i] > listTop[i+1]) { // Sorting entries. No more than one swap should be necessary.
                    int tmp = listTop[i];
                    listTop[i] = listTop[i+1];
                    listTop[i+1] = tmp;
                }
            }
            lyrList[1].add(listTop);
        }       
    }
    
    public void setLowPhThreshold(double cut) {
        if (cut <0. || cut > 1.) {
            logger.warning(String.format("low pulse-height threshold %10.4f is not valid and is ignored.", cut));
            return;
        }
        logger.config(String.format("Setting the low-pulse-height threshold to %10.4f", cut));
        lowPhThresh = cut;
    }
    
    public void setMinSeedEnergy(double minE) {
        logger.config("Setting the minimum seed energy to " + Double.toString(minE));
        for (int lyr=0; lyr<numLayers; ++lyr) {
            minSeedE[lyr] = minE;
        }
    }
    
    public void setGlbIterations(int nTrials) {
        if (nTrials < 1 || nTrials > mxTrials) {
            logger.log(Level.WARNING,String.format("Number of global iterations %d is not valid and is ignored.", nTrials));
            return;
        }
        logger.log(Level.CONFIG, String.format("Setting the number of global patrec iterations to %d", nTrials));
        this.nTrials = nTrials;
    }
    
    public void setFirstLayer(int firstLayer) {
        if (firstLayer != 0 && firstLayer != 2) {
            logger.log(Level.WARNING,String.format("First layer of %d is not valid and is ignored.", firstLayer));
            return;
        }
        logger.log(Level.CONFIG, String.format("Setting the first tracking layer to %d", firstLayer));
        this.firstLayer = firstLayer;
    }
    
    public void setIterations(int N) {
        if (N < 1) {
            logger.log(Level.WARNING,String.format("%d iterations not allowed.", N));
            return;
        }
        logger.log(Level.CONFIG,String.format("Setting the number of Kalman Filter iterations to %d.", N));
        nIterations = N;
    }
    
    public void setMxChi2Inc(double mxC) {
        if (mxC <= 1.) {
            logger.log(Level.WARNING,String.format("Maximum chi^2 increment must be at least unity. %8.2f not valid.", mxC));
            return;
        }
        logger.log(Level.CONFIG,String.format("Maximum chi^2 increment to add a hit to a track to %8.2f.", mxC));
        mxChi2Inc = mxC;
    }
    
    public void setMxChi2double(double mxDb) {
        if (mxDb <= 0.) {
            logger.log(Level.WARNING,String.format("Maximum chi^2 increment of shared hit of %8.2f not allowed.", mxDb));
            return;
        }
        logger.log(Level.CONFIG,String.format("Setting the maximum chi^2 increment of shared hit to %8.2f sigma.", mxDb));
        mxChi2double = mxDb;  
    }
    
    public void setMinChi2IncBad(double mnB) {
        if (mnB <= 3.0) {
            logger.log(Level.WARNING,String.format("Minimum chi^2 increment to remove a bad hit must be at least 3. %8.2f not valid.", mnB));
            return;
        }
        logger.log(Level.CONFIG,String.format("Setting the minimum chi^2 increment to remove a bad hit to %8.2f.", mnB));
        minChi2IncBad = mnB;        
    }
    
    public void setMxResidShare(double mxSh) {
        if (mxSh <= 0.) {
            logger.log(Level.WARNING,String.format("Maximum residual of shared hit of %8.2f not allowed.", mxSh));
            return;
        }
        logger.log(Level.CONFIG,String.format("Setting the maximum residual for a shared hit to %8.2f sigma.", mxSh));
        mxResidShare = mxSh;  
    }
    
    public void setMaxK(double kMx) {
        if (kMx <= 0.) {
            logger.log(Level.WARNING,String.format("Max 1/pt of %8.2f not allowed.", kMx));
            return;
        }
        logger.log(Level.CONFIG,String.format("Setting the maximum 1/pt to %8.2f.", kMx));
        kMax[1] = kMx;
        kMax[0] = Math.min(kMax[0], 0.6*kMx);
    }
    
    void setMinK(double kMn) {
        if (kMn < 0.) {
            logger.log(Level.WARNING,String.format("Min 1/pt of %8.2f not allowed.", kMn));
            return;
        }
        kMin = kMn;
    }
    
    public void setMxResid(double mxR) {
        if (mxR <= 1.) {
            logger.log(Level.WARNING,String.format("Max resid of %8.2f not allowed.", mxR));
            return;
        }
        logger.log(Level.CONFIG,String.format("Setting the maximum residual to pick up hits to %8.2f sigma.", mxR));
        mxResid[1] = mxR;
        mxResid[0] = Math.min(mxResid[0], 0.5*mxR);
    }
    
    public void setMaxTanL(double tlMx) {
        if (tlMx <= 0.) {
            logger.log(Level.WARNING,String.format("Max seed tan(lambda) of %8.2f not allowed.", tlMx));
            return;
        }
        logger.log(Level.CONFIG,String.format("Setting the maximum seed tan(lambda) to %8.2f.", tlMx));
        tanlMax[1] = tlMx;
        tanlMax[0] = Math.min(tanlMax[0], 0.8*tlMx);
    }
    
    public void setMaxdRho(double dMx) {
        if (dMx <= 0.0) {
            logger.log(Level.WARNING,String.format("Max dRho of %8.2f not allowed.", dMx));
            return;
        }
        logger.log(Level.CONFIG,String.format("Setting the maximum dRho to %8.2f mm.", dMx));
        dRhoMax[1] = dMx;
        dRhoMax[0] = Math.min(dRhoMax[0], 0.6*dMx);
    }
    
    public void setMaxdZ(double zMx) {
        if (zMx <= 0.0) {
            logger.log(Level.WARNING,String.format("Max dZ of %8.2f not allowed.", zMx));
            return;
        }
        logger.log(Level.CONFIG,String.format("Setting the maximum dz to %8.2f mm.", zMx));
        dzMax[1] = zMx;
        dzMax[0] = Math.min(dzMax[0], 0.6*zMx);
    }
    
    public void setMaxChi2(double xMx) {
        if (xMx <= 0.) {
            logger.log(Level.WARNING,String.format("Max chi2 of %8.2f not allowed.", xMx));
            return;
        }
        logger.log(Level.CONFIG,String.format("Setting the maximum chi^2/hit to %8.2f.", xMx));
        chi2mx1[1] = xMx;
        chi2mx1[0] = Math.min(chi2mx1[0], 0.6*xMx);
    }
    
    public void setMaxChi2Vtx(double xMx) {
        if (xMx <= 0.) {
            logger.log(Level.WARNING,String.format("Max chi2 of %8.2f not allowed.", xMx));
            return;
        }
        logger.log(Level.CONFIG,String.format("Setting the maximum chi^2 for 5-hit tracks with vertex constraint to %8.2f.", xMx));
        mxChi2Vtx = xMx;
    }
    
    public void setMinHits(int minH) {
        if (minH < 5) {
            logger.log(Level.WARNING,String.format("Minimum number of hits = %d not allowed.", minH));
            return;
        }
        logger.log(Level.CONFIG,String.format("Setting the minimum number of hits to %d.", minH));
        minHits1[1] = minH;
        minHits1[0] = Math.max(minHits1[0], minH+1);
    }
    
    public void setMinStereo(int minS) {
        if (minS < 3) {
            logger.log(Level.WARNING,String.format("Minimum number of stereo hits = %d not allowed.", minS));
            return;
        }
        logger.log(Level.CONFIG,String.format("Setting the minimum number of stereo hits to %d.", minS));
        minStereo[1] = minS;
        minStereo[0] = Math.max(minStereo[0], minS+1);
    }
    
    public void setMaxShared(int mxSh) {
        logger.log(Level.CONFIG,String.format("Setting the maximum number of shared hits to %d.", mxSh));
        mxShared = mxSh;
    }
    
    public void setMaxTimeRange(double mxT) {
        logger.log(Level.CONFIG,String.format("Setting the maximum time range for hits on a track to %8.2f ns.", mxT));
        mxTdif = mxT;
    }

    public void setSeedCompThr(double seedComp_Thr) {      
        if (seedComp_Thr < 0.) {
            logger.log(Level.CONFIG, "SeedTracks duplicate removal is turned off.");
            return;
        }
        logger.log(Level.CONFIG, String.format("Setting the SeedTracks duplicate removal threshold to %f percent.",seedComp_Thr*100.));
        seedCompThr = seedComp_Thr;
    }

    public void setBeamSpotY(double spot) {
        beamSpot[1] = spot;
        logger.log(Level.CONFIG, String.format("Setting the Y beam spot location to %f %f %f", beamSpot[0], beamSpot[1], beamSpot[2]));       
    }
    
    public void setBeamSizeY(double size) {
        vtxSize[1] = size;
        logger.log(Level.CONFIG, String.format("Setting the Y beam spot size to %f %f %f", vtxSize[0], vtxSize[1], vtxSize[2]));       
    }
    public void setBeamSpotX(double spot) {
        beamSpot[0] = spot;
        logger.log(Level.CONFIG, String.format("Setting the X beam spot location to %f %f %f", beamSpot[0], beamSpot[1], beamSpot[2]));       
    }
    
    public void setBeamSizeX(double size) {
        vtxSize[0] = size;
        logger.log(Level.CONFIG, String.format("Setting the X beam spot size to %f %f %f", vtxSize[0], vtxSize[1], vtxSize[2]));       
    }
    public void setBeamSpotZ(double spot) {
        beamSpot[2] = spot;
        logger.log(Level.CONFIG, String.format("Setting the Z beam spot location to %f %f %f", beamSpot[0], beamSpot[1], beamSpot[2]));       
    }
    
    public void setBeamSizeZ(double size) {
        vtxSize[2] = size;
        logger.log(Level.CONFIG, String.format("Setting the Z beam spot size to %f %f %f", vtxSize[0], vtxSize[1], vtxSize[2]));       
    }
    
    public void clrStrategies() {
        logger.log(Level.CONFIG,String.format("Clearing all lists of search strategies.."));
        lyrList[0].clear();
        lyrList[1].clear();
    }
    
    public void setNumSeedIter1(int num) {
        int n = num;
        for (int topBottom=0; topBottom<2; ++topBottom) {          
            if (n > lyrList[topBottom].size()) {
                n = lyrList[topBottom].size();
            }
        }
        logger.config(String.format("The number of seeds used in iteration 1 is set to %d", n));
        maxListIter1 = n-1;
    }
    
    // Add a seed search strategy for the bottom or top tracker
    public boolean addStrategy(String strategy, String topBottom) {
        if (!(topBottom == "top" || topBottom == "bottom")) {
            logger.config("The argument topBottom must be 'top' or 'bottom'. This seed strategy is ignored.");
            return false;
        }
        if (strategy.length() != 7) {
            logger.config("The seed strategy " + strategy + " does not have 7 characters and is ignored.");
            return false;
        }
        int iTB;
        if (topBottom=="top") {
            iTB = 1;
        } else {
            iTB = 0;
        }
        int nAxial = 0;
        int nStereo = 0;
        int n=0;
        int [] newList = new int[5];
        String goodChars = "0AaSsBb";
        for (int lyr=0; lyr<7; ++lyr) {
            if (goodChars.indexOf(strategy.charAt(lyr)) < 0) {
                logger.warning(String.format("Character %c for layer %d in strategy %s is not recognized. Should be 0, A, S, B, a, s, or b", 
                        strategy.charAt(lyr), lyr, strategy));
                continue;
            }
            if (strategy.charAt(lyr) == '0') continue;
            int nA = n;
            int nS = n;
            if (strategy.charAt(lyr)=='B' || strategy.charAt(lyr)=='b') {
                if (topBottom=="top") {
                    nS=n+1;
                } else {
                    nA=n+1;
                }
                n += 2;
            } else if (strategy.charAt(lyr)=='A' || strategy.charAt(lyr)=='a' || strategy.charAt(lyr)=='S' || strategy.charAt(lyr)=='s') {
                n++;
            }
            //System.out.format("addStrategy %s: lyr=%d, nA=%d, nS=%d, strategy=%s\n",topBottom,lyr,nA,nS,strategy);
            if (strategy.charAt(lyr)=='A' || strategy.charAt(lyr)=='B' || strategy.charAt(lyr)=='a' || strategy.charAt(lyr)=='b') {
                if (topBottom == "top") {
                    if (nA > 4) {
                        logger.warning("Strategy " + strategy + " has more than 5 layers! The extra ones are ignored");
                    } else {
                        newList[nA] = 2*lyr;      // The top tracker begins with an axial layer
                        //System.out.format("addStrategy %s: adding axial element %d, lyr=%d\n", topBottom, nA, 2*lyr);
                        nAxial++;
                    }
                } else {
                    if (nA > 4) {
                        logger.warning("Strategy " + strategy + " has more than 5 layers! The extra ones are ignored");
                    } else {
                        newList[nA] = 2*lyr + 1;  // The bottom tracker begins with a stereo layer
                        //System.out.format("addStrategy %s: adding axial element %d, lyr=%d\n", topBottom, nA, 2*lyr+1);
                        nAxial++;
                    }
                }
            }
            if (strategy.charAt(lyr)=='S' || strategy.charAt(lyr)=='B' || strategy.charAt(lyr)=='s' || strategy.charAt(lyr)=='b') {
                if (topBottom == "top") {
                    if (nS > 4) {
                        logger.warning("Strategy " + strategy + " has more than 5 layers! The extra ones are ignored");
                    } else {
                        newList[nS] = 2*lyr + 1;      // The top tracker begins with an axial layer
                        //System.out.format("addStrategy %s: adding stereo element %d, lyr=%d\n", topBottom, nS, 2*lyr+1);
                        nStereo++;
                    }
                } else {
                    if (nS > 4) {
                        logger.warning("Strategy " + strategy + " has more than 5 layers! The extra ones are ignored");
                    } else {
                        newList[nS] = 2*lyr;          // The bottom tracker begins with a stereo layer
                        //System.out.format("addStrategy %s: adding stereo element %d, lyr=%d\n", topBottom, nS, 2*lyr);
                        nStereo++;
                    }
                }
            }
        }
        if (nAxial != 2 || nStereo != 3) {
            logger.log(Level.WARNING,String.format("addStrategy: Invalid search strategy " + strategy + " for topBottom=%s: %d %d %d %d %d", 
                    topBottom, newList[0],newList[1],newList[2],newList[3],newList[4]));
            return false;
        }
        for (int [] oldList : lyrList[iTB]) {
            int nMatch = 0;
            for (int i=0; i<5; ++i) {
                if (oldList[i] == newList[i]) nMatch++;
            }
            if (nMatch == 5) {
                logger.log(Level.WARNING,String.format("addStrategy: strategy %s is already in the list", strategy));
                return false;
            }
        }
        logger.log(Level.CONFIG,String.format("addStrategy: adding search strategy %d %d %d %d %d for %s", 
                newList[0],newList[1],newList[2],newList[3],newList[4],topBottom));
        lyrList[iTB].add(newList);
        return true;
    }
}
