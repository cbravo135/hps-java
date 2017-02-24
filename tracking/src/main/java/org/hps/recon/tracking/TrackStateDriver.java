package org.hps.recon.tracking;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lcsim.detector.converter.compact.subdetector.HpsTracker2;
import org.lcsim.detector.converter.compact.subdetector.SvtStereoLayer;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.LCIOParameters.ParameterName;
import org.lcsim.event.base.BaseTrack;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotter;
import hep.aida.IHistogram1D;
import hep.aida.ITree;
import hep.aida.ref.rootwriter.RootFileStore;

/**
 * Driver used to incrementally extrapolate the track to all SVT layers, 
 * using a fieldmap and the positions from the compact, and save the TrackState at each layer.
 * 
 * @author <a href="mailto:btu29@wildcats.unh.edu">Bradley Yale</a>
 * @author <a href="mailto:omoreno@slac.stanford.edu">Omar Moreno</a>
 */
public final class TrackStateDriver extends Driver {
    
//%%%%%% Track Parameter Plotting stuff (To be removed later) %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    ITree tree;
    IHistogramFactory histogramFactory; 
    IPlotterFactory plotterFactory = IAnalysisFactory.create().createPlotterFactory();
    protected Map<String, IPlotter> plotters = new HashMap<String, IPlotter>();
    private Map<String, IHistogram1D> trackPlots = new HashMap<String, IHistogram1D>();
    
    AIDA aida = AIDA.defaultInstance();
    String aidaFileName = "MCTrackStateDriverPlots";    
    String aidaFileType = "root";
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    
    /** 
     * Name of the constants denoting the position of the Ecal face 
     * in the compact description.
     */
    
    private static final String ECAL_POSITION_CONSTANT_NAME = "ecal_dface";

    /** Name of the SVT subdetector volume. */
    private static final String SUBDETECTOR_NAME = "Tracker";
    
    /** The B field map */
    FieldMap bFieldMap = null;
    
    /** The magnitude of the B field used.  */
    private double bField = 0.24; // Tesla

    private double ecalPosition = 0; // mm
   
    /** Z position to start extrapolation from */
    private double extStartPos = 700; // mm

    /** The extrapolation step size */ 
    private double stepSize = 5; // mm
    
    /** Top/Bottom SVT layer 1 sensor z positions */
    // L1 top (module 0)
    private double L1t0_axial_Z = 0;
    private double L1t0_stereo_Z = 0;
    // L1 bottom (module 1)
    private double L1b1_axial_Z = 0;
    private double L1b1_stereo_Z = 0;
    
    /** Top/Bottom SVT layer 2 sensor z positions */
    // L2 top (module 0)
    private double L2t0_axial_Z = 0;
    private double L2t0_stereo_Z = 0;
    // L2 bottom (module 1)
    private double L2b1_axial_Z = 0;
    private double L2b1_stereo_Z = 0;
    
    /** Top/Bottom SVT layer 3 sensor z positions */
    // L3 top (module 0)
    private double L3t0_axial_Z = 0;
    private double L3t0_stereo_Z = 0;
    // L3 bottom (module 1)
    private double L3b1_axial_Z = 0;
    private double L3b1_stereo_Z = 0;
    
    /** Top/Bottom SVT layer 4 sensor z positions */
    // L4 top (module 0)
    private double L4t0_axial_Z = 0;
    private double L4t0_stereo_Z = 0;
    // L4 bottom (module 1)
    private double L4b1_axial_Z = 0;
    private double L4b1_stereo_Z = 0;
    // L4 top (module 2)
    private double L4t2_axial_Z = 0;
    private double L4t2_stereo_Z = 0;
    // L4 bottom (module 3)
    private double L4b3_axial_Z = 0;
    private double L4b3_stereo_Z = 0;
    
    /** Top/Bottom SVT layer 5 sensor z positions */
    // L5 top (module 0)
    private double L5t0_axial_Z = 0;
    private double L5t0_stereo_Z = 0;
    // L5 bottom (module 1)
    private double L5b1_axial_Z = 0;
    private double L5b1_stereo_Z = 0;
    // L5 top (module 2)
    private double L5t2_axial_Z = 0;
    private double L5t2_stereo_Z = 0;
    // L5 bottom (module 3)
    private double L5b3_axial_Z = 0;
    private double L5b3_stereo_Z = 0;
    
    /** Top/Bottom SVT layer 6 sensor z positions */
    // L6 top (module 0)
    private double L6t0_axial_Z = 0;
    private double L6t0_stereo_Z = 0;
    // L6 bottom (module 1)
    private double L6b1_axial_Z = 0;
    private double L6b1_stereo_Z = 0;
    // L6 top (module 2)
    private double L6t2_axial_Z = 0;
    private double L6t2_stereo_Z = 0;
    // L6 bottom (module 3)
    private double L6b3_axial_Z = 0;
    private double L6b3_stereo_Z = 0;
    
    
    /** Name of the collection of tracks to apply corrections to. */
    private String gblTrackCollectionName = "GBLTracks";
   
    /** Name of the collection of seed tracks. */
    private String seedTrackCollectionName = "MatchedTracks";

    /** Default constructor */
    public TrackStateDriver() {}
      
    @Override
    protected void detectorChanged(Detector detector) {
    
// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
// These plots are just for quickly analyzing the output.
// Will be removed for the final product.
    
        tree = IAnalysisFactory.create().createTreeFactory().create();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);
        
     // Track Parameters at all Track States
        plotters.put("Track Parameters", plotterFactory.create("Track Parameters"));
        plotters.get("Track Parameters").createRegions(6, 6);
       
      //trackPlots.put("position_X_IP", histogramFactory.createHistogram1D("position_X_IP", 500, -60, 60));
        //plotters.get("Track Parameters").region(1).plot(trackPlots.get("position_X_IP"));
        
        //trackPlots.put("position_Y_IP", histogramFactory.createHistogram1D("position_Y_IP", 500, -80, 100));
        //plotters.get("Track Parameters").region(2).plot(trackPlots.get("position_Y_IP"));
        
        trackPlots.put("chi2", histogramFactory.createHistogram1D("chi2", 500, 0, 10));    
        plotters.get("Track Parameters").region(0).plot(trackPlots.get("chi2"));
        
        // IP Track Parameters
        trackPlots.put("D0_IP", histogramFactory.createHistogram1D("D0_IP", 500, -80, 80));
        plotters.get("Track Parameters").region(1).plot(trackPlots.get("D0_IP"));
     
        trackPlots.put("z0_IP", histogramFactory.createHistogram1D("z0_IP", 500, 0, 10));
        plotters.get("Track Parameters").region(2).plot(trackPlots.get("z0_IP"));
        
        trackPlots.put("phi_IP", histogramFactory.createHistogram1D("phi0_IP", 500, -0.15, 0.15));    
        plotters.get("Track Parameters").region(3).plot(trackPlots.get("phi0_IP"));
        
        trackPlots.put("curvature_IP", histogramFactory.createHistogram1D("curvature_IP", 500, -1.5, 1.5));    
        plotters.get("Track Parameters").region(4).plot(trackPlots.get("curvature_IP"));
        
        trackPlots.put("tanLambda_IP", histogramFactory.createHistogram1D("tanLambda_IP", 500, -0.08, 0.08));    
        plotters.get("Track Parameters").region(5).plot(trackPlots.get("tanLambda_IP"));
        
        // L1_axial
        trackPlots.put("D0_L1_axial", histogramFactory.createHistogram1D("D0_L1_axial", 500, -80, 80));
        plotters.get("Track Parameters").region(6).plot(trackPlots.get("D0_L1_axial"));
     
        trackPlots.put("z0_L1_axial", histogramFactory.createHistogram1D("z0_L1_axial", 500, 0, 10));
        plotters.get("Track Parameters").region(7).plot(trackPlots.get("z0_L1_axial"));
        
        trackPlots.put("phi_L1_axial", histogramFactory.createHistogram1D("phi0_L1_axial", 500, -0.15, 0.15));    
        plotters.get("Track Parameters").region(8).plot(trackPlots.get("phi0_L1_axial"));
        
        trackPlots.put("curvature_L1_axial", histogramFactory.createHistogram1D("curvature_L1_axial", 500, -1.5, 1.5));    
        plotters.get("Track Parameters").region(9).plot(trackPlots.get("curvature_L1_axial"));
        
        trackPlots.put("tanLambda_L1_axial", histogramFactory.createHistogram1D("tanLambda_L1_axial", 500, -0.08, 0.08));    
        plotters.get("Track Parameters").region(10).plot(trackPlots.get("tanLambda_L1_axial"));
        
        
        
// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        
        // Get the field map from the detector object
        bFieldMap = detector.getFieldMap(); 
        
        // Get the B-field from the geometry description 
        bField = TrackUtils.getBField(detector).magnitude();
       
        ecalPosition = detector.getConstants().get(ECAL_POSITION_CONSTANT_NAME).getValue();
    
        // Get the stereo layers from the geometry and build the stereo
        // layer maps
        List<SvtStereoLayer> stereoLayers 
            = ((HpsTracker2) detector.getSubdetector(SUBDETECTOR_NAME).getDetectorElement()).getStereoPairs();

        // Loop through all of the stereo layers and collect their sensor positions.
        // This will be used to set the track states at those layers.
        
        for (SvtStereoLayer stereoLayer : stereoLayers) { 

         // 1st stereo pair
         if (stereoLayer.getLayerNumber() == 1){
            HpsSiSensor axialSensor1 = stereoLayer.getAxialSensor();
            HpsSiSensor stereoSensor1 = stereoLayer.getStereoSensor();
            
            double axialZ1 = axialSensor1.getGeometry().getPosition().z();
            double stereoZ1 = stereoSensor1.getGeometry().getPosition().z(); 
           
                if (axialSensor1.isTopLayer()) { L1t0_axial_Z = axialZ1; 
                } else { L1b1_axial_Z = axialZ1; }
            
                if (stereoSensor1.isTopLayer()) { L1t0_stereo_Z = stereoZ1;
                } else { L1b1_stereo_Z = stereoZ1; }
         }
                
         // 2nd stereo pair
         else if (stereoLayer.getLayerNumber() == 2){
            HpsSiSensor axialSensor2 = stereoLayer.getAxialSensor();
            HpsSiSensor stereoSensor2 = stereoLayer.getStereoSensor();
                
            double axialZ2 = axialSensor2.getGeometry().getPosition().z();
            double stereoZ2 = stereoSensor2.getGeometry().getPosition().z(); 
               
                if (axialSensor2.isTopLayer()) L2t0_axial_Z = axialZ2; 
                else L2b1_axial_Z = axialZ2;
                
                if (stereoSensor2.isTopLayer()) L2t0_stereo_Z = stereoZ2;
                else L2b1_stereo_Z = stereoZ2;
            }

      // 3rd stereo pair
         else if (stereoLayer.getLayerNumber() == 3){
            HpsSiSensor axialSensor3 = stereoLayer.getAxialSensor();
            HpsSiSensor stereoSensor3 = stereoLayer.getStereoSensor();
            
            double axialZ3 = axialSensor3.getGeometry().getPosition().z();
            double stereoZ3 = stereoSensor3.getGeometry().getPosition().z(); 
           
                if (axialSensor3.isTopLayer()) L3t0_axial_Z = axialZ3; 
                else L3b1_axial_Z = axialZ3;
            
                if (stereoSensor3.isTopLayer()) L3t0_stereo_Z = stereoZ3;
                else L3b1_stereo_Z = stereoZ3;
         }
         
      // 4th stereo pair
         else if (stereoLayer.getLayerNumber() == 4){
        
            HpsSiSensor axialSensor4 = stereoLayer.getAxialSensor();
            double axialZ4 = axialSensor4.getGeometry().getPosition().z();
            
                if (axialSensor4.isTopLayer()) {
                   if(axialSensor4.getModuleNumber() == 0) {
                      L4t0_axial_Z = axialZ4;
                   } else if (axialSensor4.getModuleNumber() == 2) { 
                      L4t2_axial_Z = axialZ4; 
                   }
                }
                
                if (axialSensor4.isBottomLayer()) {
                    if(axialSensor4.getModuleNumber() == 1){
                        L4b1_axial_Z = axialZ4;
                    } else if(axialSensor4.getModuleNumber() == 3){ 
                        L4b3_axial_Z = axialZ4; }
                 }
               
            HpsSiSensor stereoSensor4 = stereoLayer.getStereoSensor();
            double stereoZ4 = stereoSensor4.getGeometry().getPosition().z();
            
            if (stereoSensor4.isTopLayer()) {
                if(stereoSensor4.getModuleNumber() == 0) {
                   L4t0_stereo_Z = stereoZ4;
                } else if (stereoSensor4.getModuleNumber() == 2) { 
                   L4t2_stereo_Z = stereoZ4; 
                   }
             }
             
             if (stereoSensor4.isBottomLayer()) {
                 if(stereoSensor4.getModuleNumber() == 1){
                    L4b1_stereo_Z = stereoZ4;
                 } else if(stereoSensor4.getModuleNumber() == 3){ 
                    L4b3_stereo_Z = stereoZ4; 
                   }
             }
         }
         
      // 5th stereo pair
         else if (stereoLayer.getLayerNumber() == 5){
        
            HpsSiSensor axialSensor5 = stereoLayer.getAxialSensor();
            double axialZ5 = axialSensor5.getGeometry().getPosition().z();
            
                if (axialSensor5.isTopLayer()) {
                   if(axialSensor5.getModuleNumber() == 0) {
                      L5t0_axial_Z = axialZ5;
                   } else if (axialSensor5.getModuleNumber() == 2) { 
                      L5t2_axial_Z = axialZ5; 
                   }
                }
                
                if (axialSensor5.isBottomLayer()) {
                    if(axialSensor5.getModuleNumber() == 1){
                      L5b1_axial_Z = axialZ5;
                    } else if(axialSensor5.getModuleNumber() == 3){ 
                      L5b3_axial_Z = axialZ5; }
                 }
               
            HpsSiSensor stereoSensor5 = stereoLayer.getStereoSensor();
            double stereoZ5 = stereoSensor5.getGeometry().getPosition().z();
            
            if (stereoSensor5.isTopLayer()) {
                if(stereoSensor5.getModuleNumber() == 0) {
                    L5t0_stereo_Z = stereoZ5;
                } else if (stereoSensor5.getModuleNumber() == 2) { 
                    L5t2_stereo_Z = stereoZ5; 
                   }
             }
             
             if (stereoSensor5.isBottomLayer()) {
                 if(stereoSensor5.getModuleNumber() == 1){
                    L5b1_stereo_Z = stereoZ5;
                 } else if(stereoSensor5.getModuleNumber() == 3){ 
                    L5b3_stereo_Z = stereoZ5; 
                   }
             }
         }
         
      // 6th stereo pair
         else if (stereoLayer.getLayerNumber() == 6){
        
            HpsSiSensor axialSensor6 = stereoLayer.getAxialSensor();
            double axialZ6 = axialSensor6.getGeometry().getPosition().z();
            
                if (axialSensor6.isTopLayer()) {
                   if(axialSensor6.getModuleNumber() == 0) {
                      L6t0_axial_Z = axialZ6;
                   } else if (axialSensor6.getModuleNumber() == 2) { 
                      L6t2_axial_Z = axialZ6; 
                   }
                }
                
                if (axialSensor6.isBottomLayer()) {
                    if(axialSensor6.getModuleNumber() == 1){
                       L6b1_axial_Z = axialZ6;
                    } else if(axialSensor6.getModuleNumber() == 3){ 
                       L6b3_axial_Z = axialZ6; }
                 }
               
            HpsSiSensor stereoSensor6 = stereoLayer.getStereoSensor();
            double stereoZ6 = stereoSensor6.getGeometry().getPosition().z();
            
            if (stereoSensor6.isTopLayer()) {
                if(stereoSensor6.getModuleNumber() == 0) {
                   L6t0_stereo_Z = stereoZ6;
                } else if (stereoSensor6.getModuleNumber() == 2) { 
                   L6t2_stereo_Z = stereoZ6; 
                   }
             }
             
             if (stereoSensor6.isBottomLayer()) {
                 if(stereoSensor6.getModuleNumber() == 1){
                    L6b1_stereo_Z = stereoZ6;
                 } else if(stereoSensor6.getModuleNumber() == 3){ 
                    L6b3_stereo_Z = stereoZ6; 
                   }
             }
         }
            
         System.out.println("axial L"+ stereoLayer.getLayerNumber() + stereoLayer.getAxialSensor().isTopLayer() + " Module"+ stereoLayer.getAxialSensor().getModuleNumber() +": "+stereoLayer.getAxialSensor().getGeometry().getPosition().z());
         System.out.println("stereo L"+ stereoLayer.getLayerNumber() + stereoLayer.getStereoSensor().isTopLayer() + " Module"+ stereoLayer.getStereoSensor().getModuleNumber() +": "+stereoLayer.getStereoSensor().getGeometry().getPosition().z());
         
        } // stereoLayers
    } // detectorChanged
    
    @Override
    public void process(EventHeader event) {
    
        // If the event doesn't have the specified collection of tracks, throw
        // an exception.
        if (!event.hasCollection(Track.class, seedTrackCollectionName)) {
            throw new RuntimeException("Track collection " + seedTrackCollectionName + " doesn't exist");
        }
        
        // Get the collection of tracks from the event
        List<Track> tracks = event.get(Track.class, seedTrackCollectionName);
        
        // Loop through all tracks in an event and find/save track states at all the layers
        for (Track track : tracks) { 
        
           // ********************************************************************************
           // Extrapolate to all sensors in layers 1-6, and the ECal, saving the track states 
           // TODO Correct for fringe field effects after layer 5.
           //
           // TODO Right now, it extrapolates to each sensor from the target, instead of the
           // position of the previous sensor.
           //
           // ********************************************************************************
          
           // get track state at target
           TrackState stateIP = TrackUtils.getTrackStateAtLocation(track, TrackState.AtIP);
           if (stateIP == null) { 
               throw new RuntimeException("IP track state for track was not found");
           }
           
          // get track state at ECal (extrapolated from target, to compare)
           TrackState stateECal = TrackUtils.getTrackStateAtLocation(track, TrackState.AtCalorimeter);
           if (stateECal == null) { 
               throw new RuntimeException("ECal track state for track was not found");
           }
           
           List<TrackState> trackStates = track.getTrackStates();
           for (int i=1; i<trackStates.size(); i++){
             track.getTrackStates().set(i, null);
           }
     
           // Force the recomputation of the momentum. This is a hack to force
           // the persistence of the momentum, otherwise, a bogus momentum
           // value is used.
           ((BaseTrack) track).setTrackParameters(stateIP.getParameters(), bField);
           
           // TODO Replace AtOther place holders with new trackState locations defined in TrackState class
           
           // Get the track state at L1 (axial), extrapolated from IP
           double layer1_axialZ = stateIP.getTanLambda() > 0 ? L1t0_axial_Z : L1b1_axial_Z; 
           TrackState stateLayer1_axial = TrackUtils.extrapolateTrackUsingFieldMap(stateIP, extStartPos, layer1_axialZ, stepSize, bFieldMap);
           ((BaseTrackState) stateLayer1_axial).setLocation(TrackState.AtOther);
           track.getTrackStates().add(stateLayer1_axial);

           // Get the track state at L1 (stereo), extrapolated from L1 (axial)
           double layer1_stereoZ = stateIP.getTanLambda() > 0 ? L1t0_stereo_Z : L1b1_stereo_Z;
           //TrackState stateLayer1_stereo = TrackUtils.extrapolateTrackUsingFieldMap(stateIP, layer1_axialZ, layer1_stereoZ, stepSize, bFieldMap);
           TrackState stateLayer1_stereo = TrackUtils.extrapolateTrackUsingFieldMap(stateIP, extStartPos, layer1_stereoZ, stepSize, bFieldMap);           
           ((BaseTrackState) stateLayer1_stereo).setLocation(TrackState.AtOther);
           track.getTrackStates().add(stateLayer1_stereo);
           
           // Get the track state at L2 (axial), extrapolated from L1 (stereo)
           double layer2_axialZ = stateIP.getTanLambda() > 0 ? L2t0_axial_Z : L2b1_axial_Z; 
           //TrackState stateLayer2_axial = TrackUtils.extrapolateTrackUsingFieldMap(stateIP, layer1_stereoZ, layer2_axialZ, stepSize, bFieldMap);
           TrackState stateLayer2_axial = TrackUtils.extrapolateTrackUsingFieldMap(stateIP, extStartPos, layer2_axialZ, stepSize, bFieldMap);
           ((BaseTrackState) stateLayer2_axial).setLocation(TrackState.AtOther);
           track.getTrackStates().add(stateLayer2_axial);
           
           // Get the track state at L2 (stereo), extrapolated from L2 (axial)
           double layer2_stereoZ = stateIP.getTanLambda() > 0 ? L2t0_stereo_Z : L2b1_stereo_Z; 
           //TrackState stateLayer2_stereo = TrackUtils.extrapolateTrackUsingFieldMap(stateIP, layer2_axialZ, layer2_stereoZ, stepSize, bFieldMap);
           TrackState stateLayer2_stereo = TrackUtils.extrapolateTrackUsingFieldMap(stateIP, extStartPos, layer2_stereoZ, stepSize, bFieldMap);
           ((BaseTrackState) stateLayer2_stereo).setLocation(TrackState.AtOther);
           track.getTrackStates().add(stateLayer2_stereo);           
           
           // Get the track state at L3 (axial), extrapolated from L2 (stereo)
           double layer3_axialZ = stateIP.getTanLambda() > 0 ? L3t0_axial_Z : L3b1_axial_Z; 
           //TrackState stateLayer3_axial = TrackUtils.extrapolateTrackUsingFieldMap(stateIP, layer2_stereoZ, layer3_axialZ, stepSize, bFieldMap);
           TrackState stateLayer3_axial = TrackUtils.extrapolateTrackUsingFieldMap(stateIP, extStartPos, layer3_axialZ, stepSize, bFieldMap);
           ((BaseTrackState) stateLayer3_axial).setLocation(TrackState.AtOther);
           track.getTrackStates().add(stateLayer3_axial);
           
           // Get the track state at L3 (stereo), extrapolated from L3 (axial)
           double layer3_stereoZ = stateIP.getTanLambda() > 0 ? L3t0_stereo_Z : L3b1_stereo_Z; 
           //TrackState stateLayer3_stereo = TrackUtils.extrapolateTrackUsingFieldMap(stateIP, layer3_axialZ, layer3_stereoZ, stepSize, bFieldMap);
           TrackState stateLayer3_stereo = TrackUtils.extrapolateTrackUsingFieldMap(stateIP, extStartPos, layer3_stereoZ, stepSize, bFieldMap);
           ((BaseTrackState) stateLayer3_stereo).setLocation(TrackState.AtOther);
           track.getTrackStates().add(stateLayer3_stereo);
           
           
           
           // Get the track state at L4 (axial), extrapolated from L3 (stereo)
           double layer4_axialZ;
           if (stateIP.getOmega() > 0){
           layer4_axialZ = stateIP.getTanLambda() > 0 ? L4t0_axial_Z : L4b1_axial_Z;
           }else{ 
           layer4_axialZ = stateIP.getTanLambda() > 0 ? L4t2_axial_Z : L4b3_axial_Z; 
           }
           
           //TrackState stateLayer4_axial = TrackUtils.extrapolateTrackUsingFieldMap(stateLayer3_stereo, layer3_stereoZ, layer4_axialZ, stepSize, bFieldMap);
           TrackState stateLayer4_axial = TrackUtils.extrapolateTrackUsingFieldMap(stateIP, extStartPos, layer4_axialZ, stepSize, bFieldMap);
           ((BaseTrackState) stateLayer4_axial).setLocation(TrackState.AtOther);
           track.getTrackStates().add(stateLayer4_axial);
           
           // Get the track state at L4 (stereo), extrapolated from L4 (axial)
           double layer4_stereoZ;
           if (stateIP.getOmega() > 0){
           layer4_stereoZ = stateIP.getTanLambda() > 0 ? L4t0_stereo_Z : L4b1_stereo_Z;
           }else{ 
           layer4_stereoZ = stateIP.getTanLambda() > 0 ? L4t2_stereo_Z : L4b3_stereo_Z; 
           }
           
           //TrackState stateLayer4_stereo = TrackUtils.extrapolateTrackUsingFieldMap(stateLayer4_axial, layer4_axialZ, layer4_stereoZ, stepSize, bFieldMap);
           TrackState stateLayer4_stereo = TrackUtils.extrapolateTrackUsingFieldMap(stateIP, extStartPos, layer4_stereoZ, stepSize, bFieldMap);
           ((BaseTrackState) stateLayer4_stereo).setLocation(TrackState.AtOther);
           track.getTrackStates().add(stateLayer4_stereo);
           
           // Get the track state at L5 (axial), extrapolated from L4 (stereo)
           double layer5_axialZ;
           if (stateIP.getOmega() > 0){
           layer5_axialZ = stateIP.getTanLambda() > 0 ? L5t0_axial_Z : L5b1_axial_Z;
           }else{ 
           layer5_axialZ = stateIP.getTanLambda() > 0 ? L5t2_axial_Z : L5b3_axial_Z; 
           }
               
           //TrackState stateLayer5_axial = TrackUtils.extrapolateTrackUsingFieldMap(stateLayer4_stereo, layer4_stereoZ, layer5_axialZ, stepSize, bFieldMap);
           TrackState stateLayer5_axial = TrackUtils.extrapolateTrackUsingFieldMap(stateIP, extStartPos, layer5_axialZ, stepSize, bFieldMap);
           ((BaseTrackState) stateLayer5_axial).setLocation(TrackState.AtOther);
           track.getTrackStates().add(stateLayer5_axial);
           
           // Get the track state at L5 (stereo), extrapolated from L5 (axial)
           double layer5_stereoZ;
           if (stateIP.getOmega() > 0){
           layer5_stereoZ = stateIP.getTanLambda() > 0 ? L5t0_stereo_Z : L5b1_stereo_Z;
           }else{ 
           layer5_stereoZ = stateIP.getTanLambda() > 0 ? L5t2_stereo_Z : L5b3_stereo_Z; 
           }
           
           //TrackState stateLayer5_stereo = TrackUtils.extrapolateTrackUsingFieldMap(stateLayer5_axial, layer5_axialZ, layer5_stereoZ, stepSize, bFieldMap);
           TrackState stateLayer5_stereo = TrackUtils.extrapolateTrackUsingFieldMap(stateIP, extStartPos, layer5_stereoZ, stepSize, bFieldMap);
           ((BaseTrackState) stateLayer5_stereo).setLocation(TrackState.AtOther);
           track.getTrackStates().add(stateLayer5_stereo);
           
           // Get the track state at L6 (axial), extrapolated from L5 (stereo)
           double layer6_axialZ;
           if (stateIP.getOmega() > 0){
           layer6_axialZ = stateIP.getTanLambda() > 0 ? L6t0_axial_Z : L6b1_axial_Z;
           }else{ 
           layer6_axialZ = stateIP.getTanLambda() > 0 ? L6t2_axial_Z : L6b3_axial_Z; 
           }
           
           //TrackState stateLayer6_axial = TrackUtils.extrapolateTrackUsingFieldMap(stateLayer5_stereo, layer5_stereoZ, layer6_axialZ, stepSize, bFieldMap);
           TrackState stateLayer6_axial = TrackUtils.extrapolateTrackUsingFieldMap(stateIP, extStartPos, layer6_axialZ, stepSize, bFieldMap);
           ((BaseTrackState) stateLayer6_axial).setLocation(TrackState.AtOther);
           track.getTrackStates().add(stateLayer6_axial);
           
           // Get the track state at L6 (stereo), extrapolated from L6 (axial)
           double layer6_stereoZ;
           if (stateIP.getOmega() > 0){
           layer6_stereoZ = stateIP.getTanLambda() > 0 ? L6t0_stereo_Z : L6b1_stereo_Z;
           }else{ 
           layer6_stereoZ = stateIP.getTanLambda() > 0 ? L6t2_stereo_Z : L6b3_stereo_Z; 
           }
           
           //TrackState stateLayer6_stereo = TrackUtils.extrapolateTrackUsingFieldMap(stateLayer6_axial, layer6_axialZ, layer6_stereoZ, stepSize, bFieldMap);
           TrackState stateLayer6_stereo = TrackUtils.extrapolateTrackUsingFieldMap(stateIP, extStartPos, layer6_stereoZ, stepSize, bFieldMap);
           ((BaseTrackState) stateLayer6_stereo).setLocation(TrackState.AtOther);
           track.getTrackStates().add(stateLayer6_stereo);
           
           // Extrapolate to ECal from SVT last SVT layer
           //TrackState stateEcalfromLayer6 = TrackUtils.extrapolateTrackUsingFieldMap(stateLayer6_stereo, layer6_stereoZ, ecalPosition, stepSize, bFieldMap);
           TrackState stateEcalfromLayer6 = TrackUtils.extrapolateTrackUsingFieldMap(stateIP, extStartPos, ecalPosition, stepSize, bFieldMap);
           ((BaseTrackState) stateEcalfromLayer6).setLocation(TrackState.AtCalorimeter);
           track.getTrackStates().add(stateEcalfromLayer6);
           // Replace the existing track state at the Ecal
           //int ecalTrackStateIndex = track.getTrackStates().indexOf(TrackUtils.getTrackStateAtLocation(track, TrackState.AtCalorimeter));
           //track.getTrackStates().set(ecalTrackStateIndex, stateEcalfromL6);  
           
        // Fill the track parameter plots
           
           // Track parameters at IP
           //trackPlots.get("doca_IP").fill(TrackUtils.getDoca(TrackUtils.getTrackStateAtLocation(track, 1)));
           trackPlots.get("chi2").fill(track.getChi2());
         //  trackPlots.get("position_X_IP").fill(stateLayer5_axial.getReferencePoint()[2] - Math.sin(stateLayer5_axial.getPhi())/stateLayer5_axial.getOmega());
         //  trackPlots.get("position_Y_IP").fill(stateLayer5_axial.getReferencePoint()[1] + Math.cos(stateLayer5_axial.getPhi())/stateLayer5_axial.getOmega());
           //trackPlots.get("position_X_IP").fill(TrackUtils.getX0(stateECal));
           //trackPlots.get("position_Y_IP").fill(TrackUtils.getY0(stateECal));
           trackPlots.get("D0_IP").fill(stateIP.getParameters()[ParameterName.d0.ordinal()]);
           trackPlots.get("z0_IP").fill(stateIP.getParameters()[ParameterName.z0.ordinal()]);
           trackPlots.get("phi_IP").fill(stateIP.getPhi());
           trackPlots.get("curvature_IP").fill(stateIP.getParameters()[ParameterName.omega.ordinal()]);
           trackPlots.get("tanLambda_IP").fill(stateIP.getParameters()[ParameterName.tanLambda.ordinal()]);
           
           trackPlots.get("D0_L1_axial").fill(stateLayer3_axial.getParameters()[ParameterName.d0.ordinal()]);
           trackPlots.get("z0_L1_axial").fill(stateLayer3_axial.getParameters()[ParameterName.z0.ordinal()]);
           trackPlots.get("phi_L1_axial").fill(stateLayer3_axial.getPhi());
           trackPlots.get("curvature_L1_axial").fill(stateLayer3_axial.getParameters()[ParameterName.omega.ordinal()]);
           trackPlots.get("tanLambda_L1_axial").fill(stateLayer3_axial.getParameters()[ParameterName.tanLambda.ordinal()]);
           
           
           
      //     System.out.println(L1t0_axial_Z);
      //     System.out.println(L1t0_stereo_Z);
      //     System.out.println(L1b1_axial_Z);
      //     System.out.println(L1b1_stereo_Z);
      //     System.out.println(L2t0_axial_Z);
      //     System.out.println(L2t0_stereo_Z);
      //     System.out.println(L2b1_axial_Z);
      //     System.out.println(L2b1_stereo_Z);
      //     System.out.println(L3t0_axial_Z);
      //     System.out.println(L3t0_stereo_Z);
      //     System.out.println(L3b1_axial_Z);
      //     System.out.println(L3b1_stereo_Z);
      //     System.out.println(L4t0_axial_Z);
      //     System.out.println(L4t0_stereo_Z);
      //     System.out.println(L4b1_axial_Z);
      //     System.out.println(L4b1_stereo_Z);
      //     System.out.println(L4t2_axial_Z);
      //     System.out.println(L4t2_stereo_Z);
      //     System.out.println(L4b3_axial_Z);
      //     System.out.println(L4b3_stereo_Z);
      //     System.out.println(L5t0_axial_Z);
      //     System.out.println(L5t0_stereo_Z);
      //     System.out.println(L5b1_axial_Z);
      //     System.out.println(L5b1_stereo_Z);
      //     System.out.println(L5t2_axial_Z);
      //     System.out.println(L5t2_stereo_Z);
      //     System.out.println(L5b3_axial_Z);
      //     System.out.println(L5b3_stereo_Z);
      //     System.out.println(L6t0_axial_Z);
      //     System.out.println(L6t0_stereo_Z);
      //     System.out.println(L6b1_axial_Z);
      //     System.out.println(L6b1_stereo_Z);
      //     System.out.println(L6t2_axial_Z);
      //     System.out.println(L6t2_stereo_Z);
      //     System.out.println(L6b3_axial_Z);
      //     System.out.println(L6b3_stereo_Z);
           
         
        //   System.out.println(stateIP.getParameters()[ParameterName.omega.ordinal()]);
        //   System.out.println(stateLayer1_axial.getParameters()[ParameterName.omega.ordinal()]);
        //   System.out.println(stateLayer1_stereo.getParameters()[ParameterName.omega.ordinal()]);
        //   System.out.println(stateLayer2_axial.getParameters()[ParameterName.omega.ordinal()]);
        //   System.out.println(stateLayer2_stereo.getParameters()[ParameterName.omega.ordinal()]);
        //   System.out.println(stateLayer3_axial.getParameters()[ParameterName.omega.ordinal()]);
        //   System.out.println(stateLayer3_stereo.getParameters()[ParameterName.omega.ordinal()]);
        //   System.out.println(stateLayer4_axial.getParameters()[ParameterName.omega.ordinal()]);
        //   System.out.println(stateLayer5_axial.getParameters()[ParameterName.omega.ordinal()]);
        //   System.out.println(stateLayer6_axial.getParameters()[ParameterName.omega.ordinal()]);
           //System.out.println(ecalPosition);
           
           
        } // loop over tracks
        
    } // Process event
    
    public void endOfData() { 
    
        String rootFile = aidaFileName + ".root";
        RootFileStore store = new RootFileStore(rootFile);
        try {
            store.open();
            store.add(tree);
            store.close(); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
