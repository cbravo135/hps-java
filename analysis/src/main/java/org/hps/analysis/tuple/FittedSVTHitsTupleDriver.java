package org.hps.analysis.tuple;

import hep.physics.vec.BasicHep3Vector;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.ShapeFitParameters;
import org.lcsim.event.EventHeader;
import org.hps.recon.tracking.TrackerHitUtils;
import org.hps.recon.utils.LegacyTrackClusterMatcher;
import org.lcsim.detector.converter.compact.subdetector.SvtStereoLayer;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;

// driver for investigating the SVT raw and fitted hits
//  author = matt graham
//  date created = 2/5/2019
// 
public class FittedSVTHitsTupleDriver extends MCTupleMaker {

    private static Logger LOGGER = Logger.getLogger(FittedSVTHitsTupleDriver.class.getPackage().getName());

    String finalStateParticlesColName = "FinalStateParticles";
    String mcParticlesColName = "MCParticle";
    String readoutHitCollectionName = "EcalReadoutHits";//these are in ADC counts
    String calibratedHitCollectionName = "EcalCalHits";//these are in energy
    String clusterCollectionName = "EcalClustersCorr";
    private String helicalTrackHitCollectionName = "HelicalTrackHits";
    private String rotatedTrackHitCollectionName = "RotatedHelicalTrackHits";
    private String unconstrainedV0CandidatesColName = "UnconstrainedV0Candidates";
    private String beamConV0CandidatesColName = "BeamspotConstrainedV0Candidates";
    private String targetV0ConCandidatesColName = "TargetConstrainedV0Candidates";
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private String fittedTrackerHitCollectionName = "SVTFittedRawTrackerHits";
    private String trackerHitCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    String[] fpQuantNames = {"nEle_per_Event", "nPos_per_Event", "nPhoton_per_Event", "nUnAssociatedTracks_per_Event", "avg_delX_at_ECal", "avg_delY_at_ECal", "avg_E_Over_P", "avg_mom_beam_elec", "sig_mom_beam_elec"};
    private LegacyTrackClusterMatcher matcher = new LegacyTrackClusterMatcher();
    //private Map<SiSensor, Map<Integer, Hep3Vector>> stripPositions = new HashMap<SiSensor, Map<Integer, Hep3Vector>>(); 
    private List<HpsSiSensor> sensors = null;
    private Map<Integer, List<SvtStereoLayer>> topStereoLayers = new HashMap<Integer, List<SvtStereoLayer>>();
    private Map<Integer, List<SvtStereoLayer>> bottomStereoLayers = new HashMap<Integer, List<SvtStereoLayer>>();
    // Constants
    public static final double SENSOR_LENGTH = 98.33; // mm
    public static final double SENSOR_WIDTH = 38.3399; // mm
    private static final String SUBDETECTOR_NAME = "Tracker";
    boolean doSkim = false;
    TrackerHitUtils trackerHitUtils = new TrackerHitUtils();
    //some counters
    int nRecoEvents = 0;
    int nTotEle = 0;
    int nTotPos = 0;
    int nTotPhotons = 0;
    int nTotUnAss = 0;
    int nTotAss = 0;
    //some summers
    double sumdelX = 0.0;
    double sumdelY = 0.0;
    double sumEoverP = 0.0;
    // double beamEnergy = 1.05; //GeV
    double clTimeMin = 30;//ns
    double clTimeMax = 50;//ns
    double deltaTimeMax = 4;//ns
    double coplanMean = 180;//degrees
    double coplanWidth = 10;//degrees
    double esumMin = 0.4;
    double esumMax = 1.2;
    double phot_nom_x = 42.52;//nominal photon position (px=0)
    boolean requirePositron = false;
    double maxPairs = 5;
    boolean requireSuperFiducial = false;
    int nbins = 50;
    double B_FIELD = 0.23;//Tesla

    protected final Map<Integer, String> layerMap = new HashMap<Integer, String>();

    double[] hthHitTime = new double[6];
    double[] SiClHitTime = new double[12];
    double[] SiHitClHitTime = new double[12];

    protected double[] beamSize = {0.001, 0.130, 0.050}; //rough estimate from harp scans during engineering run production running
    // Beam position variables.
    // The beamPosition array is in the tracking frame
    /* TODO get the beam position from the conditions db */
    protected double[] beamPosition = {0.0, 0.0, 0.0}; //

    double minPhi = -0.25;
    double maxPhi = 0.25;
    boolean isFirst = true;

    @Override
    boolean passesCuts() {
        return true;
    }

    private enum Constraint {

        /**
         * Represents a fit with no constraints.
         */
        UNCONSTRAINED,
        /**
         * Represents a fit with beam spot constraints.
         */
        BS_CONSTRAINED,
        /**
         * Represents a fit with target constraints.
         */
        TARGET_CONSTRAINED
    }

    String detName;

    /**
     * The B field map
     */
    FieldMap bFieldMap = null;
    /**
     * Position of the Ecal face
     */
    private double ecalPosition = 0; // mm

    /**
     * Z position to start extrapolation from
     */
    double extStartPos = 700; // mm

    /**
     * The extrapolation step size
     */
    double stepSize = 5.0; // mm
    /**
     * Name of the constant denoting the position of the Ecal face in the
     * compact description.
     */
    private static final String ECAL_POSITION_CONSTANT_NAME = "ecal_dface";

    @Override
    protected void setupVariables() {
        tupleVariables.clear();
       

        // String[] ADCVals = new String[]{"L/D","AxSt/D","HoSl/D","S1/D","S2/D","S3/D","S4/D","S5/D","S6/D"};
        String[] FitVals = new String[]{"L/D", "AxSt/D", "TopBot/D","ChiProb/D", "T0/D", "Amp/D", "T0Err/D", "AmpErr/D"};

        tupleVariables.addAll(Arrays.asList(FitVals));
        /*
        tupleVariables.addAll(Arrays.asList(L1aADC));
        tupleVariables.addAll(Arrays.asList(L2aADC));
        tupleVariables.addAll(Arrays.asList(L3aADC));
        tupleVariables.addAll(Arrays.asList(L4aADC));
        tupleVariables.addAll(Arrays.asList(L5aADC));
        tupleVariables.addAll(Arrays.asList(L6aADC));

        tupleVariables.addAll(Arrays.asList(L1sADC));
        tupleVariables.addAll(Arrays.asList(L2sADC));
        tupleVariables.addAll(Arrays.asList(L3sADC));
        tupleVariables.addAll(Arrays.asList(L4sADC));
        tupleVariables.addAll(Arrays.asList(L5sADC));
        tupleVariables.addAll(Arrays.asList(L6sADC));
         */
    }

    protected void detectorChanged(Detector detector) {

        super.detectorChanged(detector);
        layerMap.put(1, "L1");
        layerMap.put(2, "L2");
        layerMap.put(3, "L3");
        layerMap.put(4, "L4");
        layerMap.put(5, "L5");
        layerMap.put(6, "L6");
        detName = detector.getName();
        double maxFactor = 1.5;
        double feeMomentumCut = 0.75; //this number, multiplied by the beam energy, is the actual cut
        B_FIELD = detector.getFieldMap().getField(new BasicHep3Vector(0, 0, 500)).y();
        // Get the field map from the detector object
        bFieldMap = detector.getFieldMap();
        // Get the position of the Ecal from the compact description
        ecalPosition = detector.getConstants().get(ECAL_POSITION_CONSTANT_NAME).getValue();       
        LOGGER.setLevel(Level.ALL);
        LOGGER.info("B_FIELD=" + B_FIELD);
        LOGGER.info("Setting up the plotter");

    }

    @Override
    public void process(EventHeader event) {

        if (!event.hasCollection(FittedRawTrackerHit.class, fittedTrackerHitCollectionName))
            return;
        List<LCRelation> fittedTrackerHits = event.get(LCRelation.class, fittedTrackerHitCollectionName);
        RelationalTable rthtofit = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        for (LCRelation hit : fittedTrackerHits)
            rthtofit.add(FittedRawTrackerHit.getRawTrackerHit(hit), FittedRawTrackerHit.getShapeFitParameters(hit));

        String axStLabel = "";
        String tString = "";
        String layerLabel = "";
        Double hosl = 0.0;
        Double axst = 0.0;
        Double topbot=0.0;
        for (LCRelation fth : fittedTrackerHits) {
            RawTrackerHit rthDummy = FittedRawTrackerHit.getRawTrackerHit(fth);
            ShapeFitParameters shape=(ShapeFitParameters)FittedRawTrackerHit.getShapeFitParameters(fth);
            HpsSiSensor sensor = ((HpsSiSensor) rthDummy.getDetectorElement());            
            int layer = rthDummy.getLayerNumber();
            layerLabel = layerMap.get(layer);
            double[] dummyPosition=rthDummy.getPosition();
//            System.out.println("hit time = "+shape.getT0());
//            System.out.println("hit position = ("+dummyPosition[0]+","+dummyPosition[1]+","+dummyPosition[2]+")");
//            System.out.println("sensor.isTop() = "+sensor.isTopLayer());
            hosl = 0.0;
            axst = 0.0;
            topbot=1.0;
            if (sensor.getSide() == "POSITRON")
                hosl = 1.0;
            if (sensor.isStereo())
                axst = 1.0;
            if(dummyPosition[1]<0)
                topbot=0.0;
            tupleMap.put("L/D", layer / 1.0);
            tupleMap.put("AxSt/D", axst);
            tupleMap.put("TopBot/D", topbot);
            tupleMap.put("T0/D", shape.getT0());
            tupleMap.put("T0Err/D", shape.getT0Err());
            tupleMap.put("Amp/D", shape.getAmp());
            tupleMap.put("AmpErr/D", shape.getAmpErr());
            tupleMap.put("ChiProb/D", shape.getChiProb());
            if (tupleWriter != null) {
//                System.out.println("writing tuple");
                writeTuple();
            }
        }

    }

    private int moduleNumber(int layer) {
        return (int) (layer + 1) / 2;
    }

}
