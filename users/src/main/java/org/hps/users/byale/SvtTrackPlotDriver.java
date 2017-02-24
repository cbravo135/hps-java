package org.hps.users.byale;

import static java.lang.Math.sqrt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotter;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.ITree;
import hep.aida.ref.rootwriter.RootFileStore;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.IRotation3D;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.ITranslation3D;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHit;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;
import org.lcsim.recon.tracking.digitization.sisim.TransformableTrackerHit;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.gbl.GBLTrackData;
import org.hps.recon.tracking.gbl.TruthResiduals;
import org.hps.users.omoreno.SvtTrackAnalysis;

/**
 * 
 * @author Bradley Yale <btu29@wildcats.unh.edu>
 *
 */
public class SvtTrackPlotDriver extends Driver {

    // Use JFreeChart as the default plotting backend
    static { 
        hep.aida.jfree.AnalysisFactory.register();
    }

    // Plotting
    ITree tree; 
    IHistogramFactory histogramFactory; 
    IPlotterFactory plotterFactory = IAnalysisFactory.create().createPlotterFactory();
    protected Map<String, IPlotter> plotters = new HashMap<String, IPlotter>();
    private Map<String, IHistogram1D> trackPlots = new HashMap<String, IHistogram1D>();
    private Map<String, IHistogram1D> clusterChargePlots = new HashMap<String, IHistogram1D>();
    private Map<String, IHistogram1D> clusterSizePlots = new HashMap<String, IHistogram1D>();    
    private Map<String, IHistogram1D> residualPlots = new HashMap<String, IHistogram1D>();
    private Map<String, IHistogram2D> Plots2D = new HashMap<String, IHistogram2D>();
    
    
    private List<HpsSiSensor> sensors;
    private Map<RawTrackerHit, LCRelation> fittedRawTrackerHitMap 
        = new HashMap<RawTrackerHit, LCRelation>();
    
    // Detector name
    private static final String SUBDETECTOR_NAME = "Tracker";
    
    // Collections
    private String trackCollectionName = "MatchedTracks";
    private String GBLtrackCollectionName = "GBLTracks";    
    private String HelicalTrackHitRelationsColName = "HelicalTrackHitRelations";
    private String fittedHitsCollectionName = "SVTFittedRawTrackerHits";
    private String rotatedHthRelationsColName = "RotatedHelicalTrackHitRelations";
    private String siClusterCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private String SVTTrueHitRelationName = "SVTTrueHitRelations";

    // private String StripsCollectionName = "SiTrackerHitStrip";
    //private String MCParticleCollectionName = "MCParticles";
    

    boolean _debug = false;
    AIDA aida = AIDA.defaultInstance();
    String aidaFileName = "MCTrackerHitResidualAnalysisDriverPlots";
    String aidaFileType = "root";

    
    private int runNumber = -1; 
    
    int npositive = 0;
    int nnegative = 0;
    double ntracks = 0;
    double ntracksTop = 0;
    double ntracksBottom = 0;
    double nTwoTracks = 0;
    double nevents = 0;

    double d0Cut = -9999;
    
    // Flags 
    boolean electronCut = false;
    boolean positronCut = false;
    
    /**
     *  Default Constructor
     */    
    public SvtTrackPlotDriver(){
    }
    
    public void setEnableElectronCut(boolean electronCut) {
        this.electronCut = electronCut;
    }

    public void setEnablePositronCut(boolean positronCut) {
        this.positronCut = positronCut;
    }

    public void setD0Cut(double d0Cut) {
       this.d0Cut = d0Cut; 
    }
    
    private int computePlotterRegion(HpsSiSensor sensor) {

        if (sensor.getLayerNumber() < 7) {
            if (sensor.isTopLayer()) {
                return 6*(sensor.getLayerNumber() - 1); 
            } else { 
                return 6*(sensor.getLayerNumber() - 1) + 1;
            } 
        } else { 
        
            if (sensor.isTopLayer()) {
                if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE) {
                    return 6*(sensor.getLayerNumber() - 7) + 2;
                } else { 
                    return 6*(sensor.getLayerNumber() - 7) + 3;
                }
            } else if (sensor.isBottomLayer()) {
                if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE) {
                    return 6*(sensor.getLayerNumber() - 7) + 4;
                } else {
                    return 6*(sensor.getLayerNumber() - 7) + 5;
                }
            }
        }
        return -1; 
    }
    
    protected void detectorChanged(Detector detector){
    
        tree = IAnalysisFactory.create().createTreeFactory().create();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);
        
        // Get the HpsSiSensor objects from the tracker detector element
        sensors = detector.getSubdetector(SUBDETECTOR_NAME)
                          .getDetectorElement().findDescendants(HpsSiSensor.class);
   
        // If the detector element had no sensors associated with it, throw
        // an exception
        if (sensors.size() == 0) {
            throw new RuntimeException("No sensors were found in this detector.");
        }

        plotters.put("Event Information", plotterFactory.create("Event information"));
        plotters.get("Event Information").createRegions(2, 3);

        trackPlots.put("Number of tracks", histogramFactory.createHistogram1D("Number of tracks", 10, 0, 10));
        plotters.get("Event Information").region(0).plot(trackPlots.get("Number of tracks"));

        trackPlots.put("Track charge", histogramFactory.createHistogram1D("Track charge", 3, -1, 2));
        plotters.get("Event Information").region(1).plot(trackPlots.get("Track charge"));

        trackPlots.put("chi2", histogramFactory.createHistogram1D("chi2", 40, 0, 40));    
        plotters.get("Event Information").region(2).plot(trackPlots.get("chi2"));

  // Track Parameters at all Track States
        plotters.put("Track Parameters", plotterFactory.create("Track Parameters"));
        plotters.get("Track Parameters").createRegions(6, 6);

        // IP Track Parameters (0)
        trackPlots.put("doca_IP", histogramFactory.createHistogram1D("doca_IP", 200, -10, 10));         
        plotters.get("Track Parameters").region(0).plot(trackPlots.get("doca_IP")); 
      
        trackPlots.put("z0_IP", histogramFactory.createHistogram1D("z0_IP", 200, -2, 2));    
        plotters.get("Track Parameters").region(1).plot(trackPlots.get("z0_IP"));

        trackPlots.put("phi0_IP", histogramFactory.createHistogram1D("phi0_IP", 200, -90, 90));    
        plotters.get("Track Parameters").region(2).plot(trackPlots.get("phi0_IP"));
    
        trackPlots.put("curvature_IP", histogramFactory.createHistogram1D("curvature_IP", 200, -0.001, 0.001));    
        plotters.get("Track Parameters").region(3).plot(trackPlots.get("curvature_IP"));

        trackPlots.put("tan_lambda_IP", histogramFactory.createHistogram1D("tan_lambda_IP", 200, -0.1, 0.1));    
        plotters.get("Track Parameters").region(4).plot(trackPlots.get("tan_lambda_IP"));

        // Calorimeter Track Parameters (4)
        trackPlots.put("doca_calorimeter", histogramFactory.createHistogram1D("doca_calorimeter", 200, -10, 10));         
        plotters.get("Track Parameters").region(5).plot(trackPlots.get("doca_calorimeter")); 
      
        trackPlots.put("z0_calorimeter", histogramFactory.createHistogram1D("z0_calorimeter", 200, -2, 2));    
        plotters.get("Track Parameters").region(6).plot(trackPlots.get("z0_calorimeter"));

        trackPlots.put("phi0_calorimeter", histogramFactory.createHistogram1D("phi0_calorimeter", 200, -90, 90));    
        plotters.get("Track Parameters").region(7).plot(trackPlots.get("phi0_calorimeter"));
    
        trackPlots.put("curvature_calorimeter", histogramFactory.createHistogram1D("curvature_calorimeter", 200, -0.001, 0.001));    
        plotters.get("Track Parameters").region(8).plot(trackPlots.get("curvature_calorimeter"));

        trackPlots.put("tan_lambda_calorimeter", histogramFactory.createHistogram1D("tan_lambda_calorimeter", 200, -0.1, 0.1));    
        plotters.get("Track Parameters").region(9).plot(trackPlots.get("tan_lambda_calorimeter"));
        
        
//        trackPlots.put("cos(theta)", histogramFactory.createHistogram1D("cos(theta)", 40, -0.1, 0.1));
//        plotters.get("Track Parameters").region(5).plot(trackPlots.get("cos(theta)"));
        
//        trackPlots.put("cluster time dt", histogramFactory.createHistogram1D("cluster time dt", 100, -20, 20));
//        plotters.get("Track Parameters").region(6).plot(trackPlots.get("cluster time dt"));
       
  //      plotters.put("Cluster Amplitude", plotterFactory.create("Cluster Amplitude"));
  //      plotters.get("Cluster Amplitude").createRegions(6, 6);
        
  //      plotters.put("Cluster Size", plotterFactory.create("Cluster Size"));
  //      plotters.get("Cluster Size").createRegions(6, 6);
        
        
     // Track-MC residuals at Track States
       
        plotters.put("Residuals", plotterFactory.create("Residuals"));
        plotters.get("Residuals").createRegions(6, 6);
        
        // IP Track-MC Residuals (0)
        
        residualPlots.put("doca_IP_residual", histogramFactory.createHistogram1D("doca_IP_residual", 200, -5, 5));         
        plotters.get("Residuals").region(0).plot(residualPlots.get("doca_IP_residual")); 
      
        residualPlots.put("z0_IP_residual", histogramFactory.createHistogram1D("z0_IP_residual", 200, -5, 5));    
        plotters.get("Residuals").region(1).plot(residualPlots.get("z0_IP_residual"));

        residualPlots.put("phi0_IP_residual", histogramFactory.createHistogram1D("phi0_IP_residual", 200, -5, 5));    
        plotters.get("Residuals").region(2).plot(residualPlots.get("phi0_IP_residual"));
    
        residualPlots.put("curvature_IP_residual", histogramFactory.createHistogram1D("curvature_IP_residual", 200, -5, 5));    
        plotters.get("Residuals").region(3).plot(residualPlots.get("curvature_IP_residual"));

        residualPlots.put("tan_lambda_IP_residual", histogramFactory.createHistogram1D("tan_lambda_IP_residual", 200, -5, 5));    
        plotters.get("Residuals").region(4).plot(residualPlots.get("tan_lambda_IP_residual"));
        
        residualPlots.put("track-MC_X_IP_residual", histogramFactory.createHistogram1D("track-MC_X_IP_residual", 200, -5, 5));    
        plotters.get("Residuals").region(5).plot(residualPlots.get("track-MC_X_IP_residual"));
        
        residualPlots.put("track-MC_Y_IP_residual", histogramFactory.createHistogram1D("track-MC_Y_IP_residual", 200, -5, 5));    
        plotters.get("Residuals").region(6).plot(residualPlots.get("track-MC_Y_IP_residual"));
        
        residualPlots.put("track-MC_Z_IP_residual", histogramFactory.createHistogram1D("track-MC_Z_IP_residual", 200, -5, 5));    
        plotters.get("Residuals").region(7).plot(residualPlots.get("track-MC_Z_IP_residual"));
        
        residualPlots.put("track-MC_PX_IP_residual", histogramFactory.createHistogram1D("track-MC_PX_IP_residual", 200, -5, 5));    
        plotters.get("Residuals").region(8).plot(residualPlots.get("track-MC_PX_IP_residual"));
        
        residualPlots.put("track-MC_PY_IP_residual", histogramFactory.createHistogram1D("track-MC_PY_IP_residual", 200, -5, 5));    
        plotters.get("Residuals").region(9).plot(residualPlots.get("track-MC_PY_IP_residual"));
        
        residualPlots.put("track-MC_PZ_IP_residual", histogramFactory.createHistogram1D("track-MC_PZ_IP_residual", 200, -5, 5));    
        plotters.get("Residuals").region(10).plot(residualPlots.get("track-MC_PZ_IP_residual"));

        // Calorimeter Track-MC Residuals (4)
        
        residualPlots.put("doca_calorimeter_residual_track-MC", histogramFactory.createHistogram1D("doca_calorimeter_residual_track-MC", 200, -5, 5));         
        plotters.get("Residuals").region(11).plot(residualPlots.get("doca_calorimeter_residual_track-MC")); 
      
        residualPlots.put("z0_calorimeter_residual_track-MC", histogramFactory.createHistogram1D("z0_calorimeter_residual_track-MC", 200, -5, 5));    
        plotters.get("Residuals").region(12).plot(residualPlots.get("z0_calorimeter_residual_track-MC"));

        residualPlots.put("phi0_calorimeter_residual_track-MC", histogramFactory.createHistogram1D("phi0_calorimeter_residual_track-MC", 200, -5, 5));    
        plotters.get("Residuals").region(13).plot(residualPlots.get("phi0_calorimeter_residual_track-MC"));
    
        residualPlots.put("curvature_calorimeter_residual_track-MC", histogramFactory.createHistogram1D("curvature_calorimeter_residual_track-MC", 200, -5, 5));    
        plotters.get("Residuals").region(14).plot(residualPlots.get("curvature_calorimeter_residual_track-MC"));

        residualPlots.put("tan_lambda_calorimeter_residual_track-MC", histogramFactory.createHistogram1D("tan_lambda_calorimeter_residual_track-MC", 200, -5, 5));    
        plotters.get("Residuals").region(15).plot(residualPlots.get("tan_lambda_calorimeter_residual_track-MC"));
        
   // top
        
//        residualPlots.put("simHit_u_top", histogramFactory.createHistogram1D("simHit_u_top", 200, -20, 20));
//        plotters.get("Track Parameters").region(5).plot(residualPlots.get("simHit_u_top"));
        
//        residualPlots.put("cluster_u_top", histogramFactory.createHistogram1D("cluster_u_top", 200, -20, 20));
//        plotters.get("Track Parameters").region(6).plot(residualPlots.get("cluster_u_top"));
        
//        residualPlots.put("strip_u-MC_u_top", histogramFactory.createHistogram1D("strip_u-MC_u_top", 200, -20, 20));
//        plotters.get("Track Parameters").region(7).plot(residualPlots.get("strip_u-MC_u_top"));
        
//        residualPlots.put("strip_u-MC_u_Pull_top", histogramFactory.createHistogram1D("strip_u-MC_u_Pull_top", 200, -20, 20));
//        plotters.get("Track Parameters").region(8).plot(residualPlots.get("strip_u-MC_u_Pull_top"));
        
//        residualPlots.put("strip_u-MC_u_Chi2_top", histogramFactory.createHistogram1D("strip_u-MC_u_Chi2_top", 200, -20, 20));
//        plotters.get("Track Parameters").region(9).plot(residualPlots.get("strip_u-MC_u_Chi2_top"));
        
   // bottom     

//        residualPlots.put("simHit_u_bottom", histogramFactory.createHistogram1D("simHit_u_bottom", 200, -20, 20));
//        plotters.get("Track Parameters").region(10).plot(residualPlots.get("simHit_u_bottom"));
        
//        residualPlots.put("cluster_u_bottom", histogramFactory.createHistogram1D("cluster_u_bottom", 200, -20, 20));
//        plotters.get("Track Parameters").region(11).plot(residualPlots.get("cluster_u_bottom"));
        
//        residualPlots.put("strip_u-MC_u_bottom", histogramFactory.createHistogram1D("strip_u-MC_u_bottom", 200, -20, 20));
//        plotters.get("Track Parameters").region(12).plot(residualPlots.get("strip_u-MC_u_bottom"));
        
//        residualPlots.put("strip_u-MC_u_Pull_bottom", histogramFactory.createHistogram1D("strip_u-MC_u_Pull_bottom", 200, -20, 20));
//        plotters.get("Track Parameters").region(13).plot(residualPlots.get("strip_u-MC_u_Pull_bottom"));
        
//        residualPlots.put("strip_u-MC_u_Chi2_bottom", histogramFactory.createHistogram1D("strip_u-MC_u_Chi2_bottom", 200, -20, 20));
//        plotters.get("Track Parameters").region(14).plot(residualPlots.get("strip_u-MC_u_Chi2_bottom"));
        
      // 2D Plots
//        Plots2D.put("N_strips vs. u", histogramFactory.createHistogram2D("N_strips vs. u", 200, -20, 20, 4, 0, 3));    
//        plotters.get("Event Information").region(3).plot(Plots2D.get("N_strips vs. u"));

        
        
       // for (HpsSiSensor sensor : sensors) { 
       
       //     clusterChargePlots.put(sensor.getName(), 
       //             histogramFactory.createHistogram1D(sensor.getName() + " - Cluster Charge", 100, 0, 5000));
       //     plotters.get("Cluster Amplitude").region(this.computePlotterRegion(sensor))
       //                                      .plot(clusterChargePlots.get(sensor.getName()));
            
       //     clusterSizePlots.put(sensor.getName(),
       //             histogramFactory.createHistogram1D(sensor.getName() + " - Cluster Size", 10, 0, 10));
       //     plotters.get("Cluster Size").region(this.computePlotterRegion(sensor))
       //                                         .plot(clusterSizePlots.get(sensor.getName()));
            
       // }

        
        
        //--- Track Extrapolation ---//
        //---------------------------// 
        /*plotters.add(aida.analysisFactory().createPlotterFactory().create("Track Position at Ecal"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("Track Position at Ecal", 200, -350, 350, 200, -100, 100));
        plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
        plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style();
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Track Position at Harp"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("Track Position at Harp", 200, -200, 200, 100, -50, 50));
        plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
        plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        nPlotters++;

        plotters.add(aida.analysisFactory().createPlotterFactory().create("Track Position at Ecal: curvature < 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("Track Position at Ecal: curvature < 0",200, -350, 350, 200, -100, 100));
        plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
        plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Track Position at Harp: curvature < 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("Track Position at Harp: curvature < 0", 200, -200, 200, 100, -50, 50));
        plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
        plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Track Position at Ecal: curvature > 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("Track Position at Ecal: curvature > 0", 200, -350, 350, 200, -100, 100));
        plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
        plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Track Position at Harp: curvature > 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("Track Position at Harp: curvature > 0", 200, -200, 200, 100, -50, 50));
        plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
        plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Track Position at Ecal: Two Tracks"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("Track Position at Ecal: Two Tracks", 200, -350, 350, 200, -100, 100));
        plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
        plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style();
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Track Position at Harp: Two Tracks"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("Track Position at Harp: Two Tracks", 200, -200, 200, 100, -50, 50));
        plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
        plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        nPlotters++;
        
        
        //--- Momentum ---//
        //----------------//
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Px"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Px", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Py"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Py", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Pz"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Pz", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Px: C > 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Px: C > 0", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Py: C > 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Py: C > 0", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Pz: C > 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Pz: C > 0", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Px: C < 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Px: C < 0", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Py: C < 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Py: C < 0", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Pz: C < 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Pz: C < 0", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Px: Two Tracks"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Px: Two Tracks", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("E over P"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("E over P", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
        nPlotters++;
           
        plotters.add(aida.analysisFactory().createPlotterFactory().create("E versus P"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("E versus P", 100, 0, 1500, 100, 0, 4000));
        plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
        plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        nPlotters++;
        
        //--- Cluster Matching ---//
        //------------------------//        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("XY Difference between Ecal Cluster and Track Position"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("XY Difference between Ecal Cluster and Track Position", 200, -200, 200, 100, -50, 50));
        plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
        plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        nPlotters++;
        */
        for (IPlotter plotter : plotters.values()) { 
            plotter.show();
        }
    }
    
// LATEST STUFF ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void process(EventHeader event){
        nevents++;

        // Get the run number from the event
        if (runNumber == -1) runNumber = event.getRunNumber();
        
        // If the event doesn't have any tracks, skip it    
       // if(!event.hasCollection(Track.class, trackCollectionName)) return;
        
     // If the event doesn't have any SimTrackerHits, skip it    
      //  if(!event.hasCollection(SimTrackerHit.class, TrackerHitsCollectionName)) return;
     // If the event doesn't have any SimTrackerHits, skip it    
       // if(!event.hasCollection(SiTrackerHitStrip1D.class)) return;
        
        RelationalTable mcHittomcP = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);

        
        
        
        RelationalTable rawtomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        if (event.hasCollection(LCRelation.class, "SVTTrueHitRelations")) {
            List<LCRelation> trueHitRelations = event.get(LCRelation.class, "SVTTrueHitRelations");
            for (LCRelation relation : trueHitRelations) {
                if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                    rawtomc.add(relation.getFrom(), relation.getTo());
                }
            }
        }
        List<TrackerHit> siClusters = event.get(TrackerHit.class, siClusterCollectionName);
        RelationalTable clustertosimhit = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        for (TrackerHit cluster : siClusters) {
            List<RawTrackerHit> rawHits = cluster.getRawHits();
            for (RawTrackerHit rth : rawHits) {
                Set<SimTrackerHit> simTrackerHits = rawtomc.allFrom(rth);
                if (simTrackerHits != null) {
                    for (SimTrackerHit simhit : simTrackerHits) {
                        clustertosimhit.add(cluster, simhit);
                    }
                }
            }
        }
      
        
     // a map of MC hit positions keyed on sensor name
        Map<String, List<Double>> mcSensorHitPositionMap = new HashMap<String, List<Double>>();
        // a map of Tracker hit positions keyed on sensor name
        Map<String, List<Double>> trackSensorHitPositionMap = new HashMap<String, List<Double>>();

        // First step is to get the SimTrackerHits (from the event) and determine their location
        // in local coordinates.
        List<SimTrackerHit> simHits = event.get(SimTrackerHit.class, "TrackerHits");
        if(_debug) System.out.println("found " + simHits.size() + " SimTrackerHits");
        // loop over each hit
        for (SimTrackerHit hit : simHits) {
            Hep3Vector stripPos = null;
            SymmetricMatrix covG = null;
            // did we correctly map clusters to this simhit?
            Set<TrackerHit> clusters = clustertosimhit.allTo(hit);
            if(_debug) System.out.println("found " + clusters.size() + " clusters associated to this SimTrackerHit");
            int clusterSize = 0;
            if (clusters != null) {
                for (TrackerHit clust : clusters) {
                    clusterSize = clust.getRawHits().size();
                    double[] clusPos = clust.getPosition();
                    stripPos = new BasicHep3Vector(clusPos);
                    // now for the uncertainty in u
                    covG = new SymmetricMatrix(3, clust.getCovMatrix(), true);
                }
            }

            // get the hit's position in global coordinates..
            Hep3Vector globalPos = hit.getPositionVec();
            // get the transformation from global to local
            ITransform3D g2lXform = hit.getDetectorElement().getGeometry().getGlobalToLocal();
            //System.out.println("transform matrix: " + g2lXform);
            IRotation3D rotMat = g2lXform.getRotation();
            //System.out.println("rotation matrix: " + rotMat);
            ITranslation3D transMat = g2lXform.getTranslation();
            //System.out.println("translation vector: " + transMat);
            // check that we can reproduce the local origin
            ITransform3D l2gXform = hit.getDetectorElement().getGeometry().getLocalToGlobal();
            Hep3Vector o = new BasicHep3Vector();
            //System.out.println("origin: " + o);
            // tranform the local origin into global position
            Hep3Vector localOriginInglobal = l2gXform.transformed(o);
            //System.out.println("transformed local to global: " + localOriginInglobal);
            // and now back...
            //System.out.println("and back: " + g2lXform.transformed(localOriginInglobal));
            // hmmm, so why is this not the same as the translation vector of the transform?
            //Note:
            // u is the measurement direction perpendicular to the strip
            // v is along the strip
            // w is normal to the wafer plane

            Hep3Vector localPos = g2lXform.transformed(globalPos);
//            System.out.println("Layer: " + hit.getLayer() + " Layer Number: " + hit.getLayerNumber() + " ID: " + hit.getCellID() + " " + hit.getDetectorElement().getName());
//            System.out.println("global position " + globalPos);
//            System.out.println("local  position " + localPos);
            String sensorName = hit.getDetectorElement().getName();
            
            // get sensor from hit
            HpsSiSensor sensor = null;
            sensor = (HpsSiSensor) hit.getDetectorElement();
            
            double u = localPos.x();
            if (stripPos != null) {
                Hep3Vector clusLocalPos = g2lXform.transformed(stripPos);
                double clusU = clusLocalPos.x();
                aida.cloud1D(sensorName + " " + clusterSize + " strip cluster u-MC_u").fill(clusU - u);
                SymmetricMatrix covL = g2lXform.transformed(covG);
                double sigmaU = sqrt(covL.e(0, 0));
                aida.cloud1D(sensorName + " " + clusterSize + " strip cluster u-MC_u pull").fill((clusU - u)/sigmaU);     
                
 //           if(sensor.isTopLayer()){
                
 //               residualPlots.get("simHit_u_top").fill(u);
 //               residualPlots.get("cluster_u_top").fill(clusU);
 //               residualPlots.get("strip_u-MC_u_top").fill(clusU - u);
 //               residualPlots.get("strip_u-MC_u_Pull_top").fill((clusU - u)/sigmaU);
 //               residualPlots.get("strip_u-MC_u_Chi2_top").fill(((clusU - u)/sigmaU)*((clusU - u)/sigmaU));
                
 //           } else if(sensor.isBottomLayer()){
                
 //               residualPlots.get("simHit_u_bottom").fill(u);
 //               residualPlots.get("cluster_u_bottom").fill(clusU);
 //               residualPlots.get("strip_u-MC_u_bottom").fill(clusU - u);
 //               residualPlots.get("strip_u-MC_u_Pull_bottom").fill((clusU - u)/sigmaU);
 //               residualPlots.get("strip_u-MC_u_Chi2_bottom").fill(((clusU - u)/sigmaU)*((clusU - u)/sigmaU));
 //           } 
            
                
            }
            if(_debug) System.out.println("MC " + hit.getDetectorElement().getName() + " u= " + localPos.x());
            if (mcSensorHitPositionMap.containsKey(sensorName)) {
                List<Double> vals = mcSensorHitPositionMap.get(sensorName);
                vals.add(u);
            } else {
                List<Double> vals = new ArrayList<Double>();
                vals.add(u);
                mcSensorHitPositionMap.put(sensorName, vals);
            }
        } // end of loop over SimTrackerHits
        
        
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%        
       // Get the local u coordinate from SimTrackerHits (1st version)

        
        // a map of MC hit positions keyed on sensor name
//        Map<String, List<Double>> mcSensorHitPositionMap = new HashMap<String, List<Double>>();
        // a map of Tracker hit positions keyed on sensor name
//        Map<String, List<Double>> trackSensorHitPositionMap = new HashMap<String, List<Double>>();

        // First step is to get the SimTrackerHits and determine their location
        // in local coordinates.
//        List<SimTrackerHit> simHits = event.get(SimTrackerHit.class, "TrackerHits");
        // System.out.println("found " + simHits.size() + " SimTrackerHits");
        // loop over each hit
        for (SimTrackerHit hit2 : simHits) {
            // get the hit's position in global coordinates..
            Hep3Vector globalPos2 = hit2.getPositionVec();
            // get the transformation from global to local
            ITransform3D g2lXform2 = hit2.getDetectorElement().getGeometry().getGlobalToLocal();
            //System.out.println("transform matrix: " + g2lXform);
            IRotation3D rotMat2 = g2lXform2.getRotation();
            //System.out.println("rotation matrix: " + rotMat);
            ITranslation3D transMat2 = g2lXform2.getTranslation();
            //System.out.println("translation vector: " + transMat);
            // check that we can reproduce the local origin
            ITransform3D l2gXfor2 = hit2.getDetectorElement().getGeometry().getLocalToGlobal();
            Hep3Vector o2 = new BasicHep3Vector();
            //System.out.println("origin: " + o);
            // tranform the local origin into global position
            Hep3Vector localOriginInglobal2 = l2gXfor2.transformed(o2);
            //System.out.println("transformed local to global: " + localOriginInglobal);
            // and now back...
            //System.out.println("and back: " + g2lXform.transformed(localOriginInglobal));
            // hmmm, so why is this not the same as the translation vector of the transform?
            //Note:
            // u is the measurement direction perpendicular to the strip
            // v is along the strip
            // w is normal to the wafer plane

            Hep3Vector localPos2 = g2lXform2.transformed(globalPos2);
//            System.out.println("Layer: " + hit.getLayer() + " Layer Number: " + hit.getLayerNumber() + " ID: " + hit.getCellID() + " " + hit.getDetectorElement().getName());
//            System.out.println("global position " + globalPos);
//            System.out.println("local  position " + localPos);
            String sensorName2 = hit2.getDetectorElement().getName();
            double u2 = localPos2.x();
   //         System.out.println("MC " + hit.getDetectorElement().getName() + " u= " + localPos.x());
            if (mcSensorHitPositionMap.containsKey(sensorName2)) {
                List<Double> vals = mcSensorHitPositionMap.get(sensorName2);
                vals.add(u2);
            } else {
                List<Double> vals = new ArrayList<Double>();
                vals.add(u2);
                mcSensorHitPositionMap.put(sensorName2, vals);
            }
            
         //   residualPlots.get("simHit_U").fill(u);
          
        } // end of loop over SimTrackerHits
        
//TRACKER HITS         
        setupSensors(event);
        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);
        List<Track> tracks = event.get(Track.class, "MatchedTracks");
   //     System.out.println("found " + tracks.size() + " tracks");
        for (Track t : tracks) {
            List<TrackerHit> hits = t.getTrackerHits();
 //           System.out.println("track has " + hits.size() + " hits");
            if (hits.size() > 0) {
               for (TrackerHit h : hits) {
                    Set<TrackerHit> stripList = hitToStrips.allFrom(hitToRotated.from(h));
                    for (TrackerHit strip : stripList) {
                        List rawHits = strip.getRawHits();
                        HpsSiSensor sensor2 = null;
                        for (Object o2 : rawHits) {
                            RawTrackerHit rth = (RawTrackerHit) o2;
                            // TODO figure out why the following collection is always null
                            //List<SimTrackerHit> stipMCHits = rth.getSimTrackerHits();
                            sensor2 = (HpsSiSensor) rth.getDetectorElement();
                            
                        }
                        int nHitsInCluster = rawHits.size();
                        String sensorName2 = sensor2.getName();
                        Hep3Vector posG = new BasicHep3Vector(strip.getPosition());
                        Hep3Vector posL = sensor2.getGeometry().getGlobalToLocal().transformed(posG);
                        double u2 = posL.x();
                        double v = posL.y();
                        double mcU = mcSensorHitPositionMap.get(sensorName2).get(0);
                                                
                        // now for the uncertainty in u
                        SymmetricMatrix covG2 = new SymmetricMatrix(3, strip.getCovMatrix(), true);
                        SymmetricMatrix covL = sensor2.getGeometry().getGlobalToLocal().transformed(covG2);
                        double sigmaU = sqrt(covL.e(0, 0));
                        
                     //   residualPlots.get("trackerHit_U").fill(u2);
                     //   residualPlots.get("MC_U").fill(mcU);
                     //   residualPlots.get("trackerHit-MC_U").fill(u2-mcU);
                     //   residualPlots.get("trackerHit-MC_U_Pull").fill((u2-mcU)/sigmaU);
                     //   residualPlots.get("trackerHit-MC_U_Chi2").fill(((u2-mcU)/sigmaU)*((u2-mcU)/sigmaU));
                  
                        
                        
                        
                        
                        //System.out.println(" Track Hit: " + nHitsInCluster + " " + sensorName + " u " + u + " mcU " + mcU + " sigmaU " + sigmaU);
                    }
                }
            } // end of loop over six-hit tracks
        } // end of loop over tracks

   // (1st version)     
// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
//*********************THIS IS JUNK *****************************************************        
//        List<SimTrackerHit> OneDHits = event.get(SimTrackerHit.class, "TrackerHits");
//        for(SimTrackerHit OneDHit : OneDHits){
//      
//        	double umeas = ((TransformableTrackerHit) OneDHit).getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR).getPosition()[0];
//           // Hep3Vector v = OneDStrip.;
//           // HelicalTrackStrip strip = makeDigiStrip(OneDStrips);

//        	residualPlots.get("umeas").fill(umeas);

//        }
        
        	
     // Get the collection of SimTrackerHits from the event
     //   List<SimTrackerHit> hits = event.get(SimTrackerHit.class, TrackerHitsCollectionName);
   
     //   for(SimTrackerHit hit : hits){
        	
       //     double[] hitPosition = hit.getPosition();
           // double MCPosition_x = hit.getMCParticle();
            
      //      residualPlots.get("umeas").fill(MCPosition_x);
            //hitPosition[0]
         
            
      //  }
        
 // ************************** THIS IS JUNK ************************************************************************
        
        
        
        // Get the collection of LCRelations between a stereo hit and the strips making it up
//        List<LCRelation> stereoHitRelations = event.get(LCRelation.class, stereoHitRelationsColName);
//        BaseRelationalTable stereoHitToClusters = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
//        for (LCRelation relation : stereoHitRelations) { 
//            if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
//                stereoHitToClusters.add(relation.getFrom(), relation.getTo());
 //           }
 //       }
        
        // Get the collection of LCRelations relating RotatedHelicalTrackHits to
        // HelicalTrackHits
 //       List<LCRelation> rotatedHthToHthRelations = event.get(LCRelation.class, rotatedHthRelationsColName);
 //       BaseRelationalTable hthToRotatedHth = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
 //       for (LCRelation relation : rotatedHthToHthRelations) {
 //           if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
  //              hthToRotatedHth.add(relation.getFrom(), relation.getTo());
  //          }
  //      }
        
        // Get the list of fitted hits from the event
//        List<LCRelation> fittedHits = event.get(LCRelation.class, fittedHitsCollectionName);
        
        // Map the fitted hits to their corresponding raw hits
//        this.mapFittedRawHits(fittedHits);
       
        
        // Loop over all of the tracks in the event
   //     for(Track track : tracks){
                
        
        
        // Relate GBL to Matched Tracks
            List<LCRelation> MatchedToGBL = event.get(LCRelation.class, "MatchedToGBLTrackRelations");
            BaseRelationalTable TracktoGBLTrack = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
            for (LCRelation relation : MatchedToGBL) {
                if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                	TracktoGBLTrack.add(relation.getFrom(), relation.getTo());
                }
            }
        
        // get detector from simHits (for the fieldmap)
     //   Detector detector = (Detector) event.getMetaData(simHits).getIDDecoder().getSubdetector();
        
        // loop over GBL tracks in event
        List<Track> GBLtracks = event.get(Track.class, GBLtrackCollectionName);
        
        // relate raw hits (to get sim->particles) to tracker hits
        List<LCRelation> TrueHitRelation = event.get(LCRelation.class, SVTTrueHitRelationName);
        BaseRelationalTable rawtotracker = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        for (LCRelation relation : TrueHitRelation) { 
            if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
            	rawtotracker.add(relation.getFrom(), relation.getTo());
            }
        }
        
        for(Track GBLtrack : GBLtracks){
        	
        	// get MatchedTrack from GBLTrack
            Track matchedTrack = (Track) TracktoGBLTrack.from(GBLtrack);
            // get hits from track
            List<TrackerHit> hits = matchedTrack.getTrackerHits();
            // get raw hits
            RawTrackerHit rh = (RawTrackerHit) rawtotracker.to(hits.get(0));
            // get sim->MCP
 	        MCParticle mcp = rh.getSimTrackerHits().get(0).getMCParticle();
            
            //Set<SimTrackerHit> simTrackerHits = rawtomc.allTo(matchedTrack.getTrackerHits());
            //SimTrackerHit sth = trueHitRelations.to(matchedTrack.getTrackerHits().get(0));
      //      for (SimTrackerHit hit : simTrackerHits) {           
      //         MCParticle mcp = hit.getMCParticle();
            
            
            double track_X0_IP = TrackUtils.getX0(TrackUtils.getTrackStateAtLocation(matchedTrack, 1));
            double track_Y0_IP = TrackUtils.getY0(TrackUtils.getTrackStateAtLocation(matchedTrack, 1));
            double track_Z0_IP = TrackUtils.getZ0(TrackUtils.getTrackStateAtLocation(matchedTrack, 1));
            
        //    double MC_X0_IP = TrackUtils.getMatchedTruthParticle(matchedTrack).getOriginX();
        //    double MC_Y0_IP = TrackUtils.getMatchedTruthParticle(matchedTrack).getOriginY();
        //    double MC_Z0_IP = TrackUtils.getMatchedTruthParticle(matchedTrack).getOriginZ();
            
            double MC_X0_IP = mcp.getOriginX();
            double MC_Y0_IP = mcp.getOriginY();
            double MC_Z0_IP = mcp.getOriginZ();
            
            double track_PX0_IP = matchedTrack.getPX();
            double track_PY0_IP = matchedTrack.getPY();
            double track_PZ0_IP = matchedTrack.getPZ();
            
            double MC_PX0_IP = mcp.getPX();
            double MC_PY0_IP = mcp.getPY();
            double MC_PZ0_IP = mcp.getPZ();
            
            
            //TrackUtils.getMatchedTruthParticle(GBLtrack);
            
    //        TruthResiduals TruthResiduals = new TruthResiduals(TrackUtils.getBField(detector));
     //       TruthResiduals.processSim(TrackUtils.getMatchedTruthParticle(GBLtrack), simHits);
            
            //Hep3Vector simHitPosTracking = CoordinateTransformations.transformVectorToTracking(simHit.getPositionVec());
            
            //if (TrackUtils.getR(track) < 0 && electronCut) continue; 
            //if (TrackUtils.getR(track) > 0 && positronCut) continue;
            //if (d0Cut != -9999 && Math.abs(TrackUtils.getDoca(track)) < d0Cut) continue;
            
        	// trackPlots.get("Track charge").fill(TrackUtils.getR(GBLtrack), 1);
            // trackPlots.get("cos(theta)").fill(TrackUtils.getCosTheta(TrackUtils.getTrackStateAtLocation(GBLtrack, 1)));
            
            
   // Fill the track parameter plots
            
            // Track parameters at IP
            trackPlots.get("doca_IP").fill(TrackUtils.getDoca(TrackUtils.getTrackStateAtLocation(GBLtrack, 1)));
            trackPlots.get("z0_IP").fill(TrackUtils.getZ0(TrackUtils.getTrackStateAtLocation(GBLtrack, 1)));
            trackPlots.get("phi0_IP").fill(TrackUtils.getPhi0(TrackUtils.getTrackStateAtLocation(GBLtrack, 1)));
            trackPlots.get("curvature_IP").fill(TrackUtils.getR(TrackUtils.getTrackStateAtLocation(GBLtrack, 1)));
            trackPlots.get("tan_lambda_IP").fill(TrackUtils.getTanLambda(TrackUtils.getTrackStateAtLocation(GBLtrack, 1)));            
            
            trackPlots.get("track-MC_X_IP_residual").fill(track_X0_IP-MC_X0_IP);
            trackPlots.get("track-MC_Y_IP_residual").fill(track_Y0_IP-MC_Y0_IP);
            trackPlots.get("track-MC_Z_IP_residual").fill(track_Z0_IP-MC_Z0_IP);
            
            trackPlots.get("track-MC_PX_IP_residual").fill(track_PX0_IP-MC_PX0_IP);
            trackPlots.get("track-MC_PY_IP_residual").fill(track_PY0_IP-MC_PY0_IP);
            trackPlots.get("track-MC_PZ_IP_residual").fill(track_PZ0_IP-MC_PZ0_IP);
            
            
         // Track parameters at Calorimeter
          if (TrackUtils.getTrackStateAtLocation(GBLtrack, TrackState.AtCalorimeter) != null) {
            trackPlots.get("doca_calorimeter").fill(TrackUtils.getDoca(TrackUtils.getTrackStateAtLocation(GBLtrack, 4)));
            trackPlots.get("z0_calorimeter").fill(TrackUtils.getZ0(TrackUtils.getTrackStateAtLocation(GBLtrack, 4)));
            trackPlots.get("phi0_calorimeter").fill(TrackUtils.getPhi0(TrackUtils.getTrackStateAtLocation(GBLtrack, 4)));
            trackPlots.get("curvature_calorimeter").fill(TrackUtils.getR(TrackUtils.getTrackStateAtLocation(GBLtrack, 4)));
            trackPlots.get("tan_lambda_calorimeter").fill(TrackUtils.getTanLambda(TrackUtils.getTrackStateAtLocation(GBLtrack, 4)));
          }
            
            trackPlots.get("chi2").fill(matchedTrack.getChi2());
              
            
              
            
        } // Loop over GBL tracks  
        
     } // Process Event
     
 //           for (TrackerHit rotatedStereoHit : track.getTrackerHits()) { 
             
                // Get the HelicalTrackHit corresponding to the RotatedHelicalTrackHit
                // associated with a track
//                Set<TrackerHit> clusters = stereoHitToClusters.allFrom(hthToRotatedHth.from(rotatedStereoHit));
                
//                int clusterIndex = 0;
//                double clusterTimeDt = 0;
//                for (TrackerHit cluster : clusters) { 
                    
//                    if (clusterIndex == 0) { 
//                        clusterTimeDt += cluster.getTime();
//                        clusterIndex++;
 //                   } else { 
 //                       clusterTimeDt -= cluster.getTime();
 //                   }
                    
  //                  double amplitude = 0;
  //                  HpsSiSensor sensor = null;
  //                  for (Object rawHitObject : cluster.getRawHits()) {
  //                      RawTrackerHit rawHit = (RawTrackerHit) rawHitObject; 
                        
  //                      sensor = (HpsSiSensor) rawHit.getDetectorElement();
                        
                        // Get the channel of the raw hit
                        //int channel = rawHit.getIdentifierFieldValue("strip");
                
                        // Add the amplitude of that channel to the total amplitude
   //                     amplitude += FittedRawTrackerHit.getAmp(this.getFittedHit(rawHit));
   //                 }
                    
   //                 clusterChargePlots.get(sensor.getName()).fill(amplitude);
   //                 clusterSizePlots.get(sensor.getName()).fill(cluster.getRawHits().size());
   //             }
                
   //             trackPlots.get("cluster time dt").fill(clusterTimeDt);
   //         }
   //     }
  //  }
    
    public void endOfData() { 
        
        String rootFile = "run" + runNumber + "_track_analysis.root";
        RootFileStore store = new RootFileStore(rootFile);
        try {
            store.open();
            store.add(tree);
            store.close(); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  Method that creates a map between a fitted raw hit and it's corresponding raw fit
     *  
     * @param fittedHits : List of fitted hits to map
     */
//    private void mapFittedRawHits(List<LCRelation> fittedHits) { 
        
        // Clear the fitted raw hit map of old values
//        fittedRawTrackerHitMap.clear();
       
        // Loop through all fitted hits and map them to their corresponding raw hits
 //       for (LCRelation fittedHit : fittedHits) { 
 //           fittedRawTrackerHitMap.put(FittedRawTrackerHit.getRawTrackerHit(fittedHit), fittedHit);
 //       }
 //   }
    
    /**
     * 
     * @param rawHit
     * @return
     */
//    private LCRelation getFittedHit(RawTrackerHit rawHit) { 
//        return fittedRawTrackerHitMap.get(rawHit);
//    }
    
    
    

   // Setup Sensors to use
            private void setupSensors(EventHeader event)
            {
                List<RawTrackerHit> rawTrackerHits = null;
                if (event.hasCollection(RawTrackerHit.class, "SVTRawTrackerHits")) {
                    rawTrackerHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
                }
                if (event.hasCollection(RawTrackerHit.class, "RawTrackerHitMaker_RawTrackerHits")) {
                    rawTrackerHits = event.get(RawTrackerHit.class, "RawTrackerHitMaker_RawTrackerHits");
                }
                EventHeader.LCMetaData meta = event.getMetaData(rawTrackerHits);
                // Get the ID dictionary and field information.
                IIdentifierDictionary dict = meta.getIDDecoder().getSubdetector().getDetectorElement().getIdentifierHelper().getIdentifierDictionary();
                int fieldIdx = dict.getFieldIndex("side");
                int sideIdx = dict.getFieldIndex("strip");
                for (RawTrackerHit hit : rawTrackerHits) {
                    // The "side" and "strip" fields needs to be stripped from the ID for sensor lookup.
                    IExpandedIdentifier expId = dict.unpack(hit.getIdentifier());
                    expId.setValue(fieldIdx, 0);
                    expId.setValue(sideIdx, 0);
                    IIdentifier strippedId = dict.pack(expId);
                    // Find the sensor DetectorElement.
                    List<IDetectorElement> des = DetectorElementStore.getInstance().find(strippedId);
                    if (des == null || des.size() == 0) {
                        throw new RuntimeException("Failed to find any DetectorElements with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
                    } else if (des.size() == 1) {
                        hit.setDetectorElement((SiSensor) des.get(0));
                    } else {
                        // Use first sensor found, which should work unless there are sensors with duplicate IDs.
                        for (IDetectorElement de : des) {
                            if (de instanceof SiSensor) {
                                hit.setDetectorElement((SiSensor) de);
                                break;
                            }
                        }
                    }
                    // No sensor was found.
                    if (hit.getDetectorElement() == null) {
                        throw new RuntimeException("No sensor was found for hit with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
                    }
                }
            }
}


            /*
            ntracks++;
            Hep3Vector positionEcal = TrackUtils.getTrackPositionAtEcal(track);
            System.out.println("Position at Ecal: " + positionEcal);
            Hep3Vector positionConverter = TrackUtils.extrapolateTrack(track,-700);
        
            aida.histogram2D("Track Position at Ecal").fill(positionEcal.y(), positionEcal.z());
            aida.histogram2D("Track Position at Harp").fill(positionConverter.y(), positionConverter.z());

            if(positionEcal.z() > 0 ) ntracksTop++;
            else if(positionEcal.z() < 0) ntracksBottom++;
            */
            
        
            /*    
            aida.histogram1D("Px").fill(track.getTrackStates().get(0).getMomentum()[0]);
            aida.histogram1D("Py").fill(track.getTrackStates().get(0).getMomentum()[1]);
            aida.histogram1D("Pz").fill(track.getTrackStates().get(0).getMomentum()[2]);
            aida.histogram1D("ChiSquared").fill(track.getChi2());
            
            if(Math.signum(TrackUtils.getR(track)) < 0){
                aida.histogram2D("Track Position at Ecal: curvature < 0").fill(positionEcal.y(), positionEcal.z());
                aida.histogram2D("Track Position at Harp: curvature < 0").fill(positionConverter.y(), positionConverter.z());
                aida.histogram1D("Px: C < 0").fill(track.getTrackStates().get(0).getMomentum()[0]);
                aida.histogram1D("Py: C < 0").fill(track.getTrackStates().get(0).getMomentum()[1]);
                aida.histogram1D("Pz: C < 0").fill(track.getTrackStates().get(0).getMomentum()[2]);
                nnegative++;
            } else if(Math.signum(TrackUtils.getR(track)) > 0){
                aida.histogram2D("Track Position at Ecal: curvature > 0").fill(positionEcal.y(), positionEcal.z());
                aida.histogram2D("Track Position at Harp: curvature > 0").fill(positionConverter.y(), positionConverter.z());
                aida.histogram1D("Px: C > 0").fill(track.getTrackStates().get(0).getMomentum()[0]);
                aida.histogram1D("Px: C > 0").fill(track.getTrackStates().get(0).getMomentum()[1]);
                aida.histogram1D("Px: C > 0").fill(track.getTrackStates().get(0).getMomentum()[2]);
                npositive++;
            }
            
            if(tracks.size() > 1){
                aida.histogram2D("Track Position at Ecal: Two Tracks").fill(positionEcal.y(), positionEcal.z());
                aida.histogram2D("Track Position at Harp: Two Tracks").fill(positionConverter.y(), positionConverter.z()); 
                aida.histogram1D("Px: Two Tracks").fill(track.getTrackStates().get(0).getMomentum()[0]);
                if(tracks.size() == 2) nTwoTracks++;
            }
            
            trackToEcalPosition.put(positionEcal, track);
            ecalPos.add(positionEcal);          
        }
        
        if(!event.hasCollection(Cluster.class, "EcalClusters")) return;
        List<Cluster> clusters = event.get(Cluster.class, "EcalClusters");
        

        for(Hep3Vector ecalP : ecalPos){
            double xdiff = 1000; 
            double ydiff = 1000;
            for(Cluster cluster : clusters){
                double xd = ecalP.y() - cluster.getPosition()[0];
                double yd = ecalP.z() - cluster.getPosition()[1];  
                if(yd < ydiff){
                    xdiff = xd;
                    ydiff = yd;
                    trackToCluster.put(trackToEcalPosition.get(ecalP),cluster);
                }
            }
            clusters.remove(trackToCluster.get(trackToEcalPosition.get(ecalP)));
            aida.histogram2D("XY Difference between Ecal Cluster and Track Position").fill(xdiff, ydiff);
        }
        
        for(Map.Entry<Track, Cluster> entry : trackToCluster.entrySet()){
            double Energy = entry.getValue().getEnergy();
            Track track = entry.getKey();
            double pTotal = Math.sqrt(track.getTrackStates().get(0).getMomentum()[0]*track.getTrackStates().get(0).getMomentum()[0] + track.getTrackStates().get(0).getMomentum()[1]*track.getTrackStates().get(0).getMomentum()[1] + track.getTrackStates().get(0).getMomentum()[2]*track.getTrackStates().get(0).getMomentum()[2]);
            
            double ep = Energy/(pTotal*1000);
            
            System.out.println("Energy: " + Energy + "P: " + pTotal + " E over P: " + ep);
            
            aida.histogram1D("E over P").fill(ep);
            aida.histogram2D("E versus P").fill(Energy, pTotal*1000);
        }
        
        for(Cluster cluster : clusters){
            double[] clusterPosition = cluster.getPosition();
            
            System.out.println("Cluster Position: [" + clusterPosition[0] + ", " + clusterPosition[1] + ", " + clusterPosition[2]+ "]");
        }
        
        double ratio = nnegative/npositive;
        System.out.println("Ratio of Negative to Position Tracks: " + ratio);
    
        double tracksRatio = ntracks/nevents;
        double tracksTopRatio = ntracksTop/nevents;
        double tracksBottomRatio = ntracksBottom/nevents;
        double twoTrackRatio = nTwoTracks/nevents;
        System.out.println("Number of tracks per event: " + tracksRatio);
        System.out.println("Number of top tracks per event: " + tracksTopRatio);
        System.out.println("Number of bottom tracks per event: " + tracksBottomRatio);
        System.out.println("Number of two track events: " + twoTrackRatio);
    }*/


