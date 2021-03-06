package org.hps.record.daqconfig2019;

/**
 * Class <code>PairTriggerConfig2019</code> holds the configuration data
 * for a pair trigger.
 */
public class PairTriggerConfig2019 extends AbstractConfig2019<AbstractConfig2019<Double>> {
    private static final int CUT_ENERGY_MIN   = 0;
    private static final int CUT_ENERGY_MAX   = 1;
    private static final int CUT_HIT_COUNT    = 2;      
    private static final int CUT_ENERGY_SUM   = 3;
    private static final int CUT_ENERGY_DIFF  = 4;
    private static final int CUT_ENERGY_SLOPE = 5;
    private static final int CUT_COPLANARITY  = 6;
    private static final int CUT_TIME_DIFF    = 7;
    private static final int L1_MATCHING = 8;
    private static final int L2_MATCHING = 9;
    private static final int L1L2_GEO_MATCHING = 10;
    private static final int HODOECAL_GEO_MATCHING = 11;
    
    /**
     * Creates a new <code>PairTriggerConfig</code> object.
     */
    public PairTriggerConfig2019() {
        // Instantiate the superclass.
        super(12);
        
        // Define the pair cuts.
        setValue(CUT_ENERGY_MIN, new LBOCutConfig2019());
        setValue(CUT_ENERGY_MAX, new UBOCutConfig2019());
        setValue(CUT_HIT_COUNT,  new LBOCutConfig2019());       
        setValue(CUT_ENERGY_SUM,   new ULBCutConfig2019());
        setValue(CUT_ENERGY_DIFF,  new UBOCutConfig2019());
        setValue(CUT_ENERGY_SLOPE, new ESBCutConfig2019());
        setValue(CUT_COPLANARITY,  new UBOCutConfig2019());
        setValue(CUT_TIME_DIFF,    new UBOCutConfig2019());
        // Only pair3 trigger requires geometry matching for hodoscope and Ecal
        setValue(L1_MATCHING,  new HodoEcalCoincidence2019());
        setValue(L2_MATCHING,  new HodoEcalCoincidence2019());
        setValue(L1L2_GEO_MATCHING,  new HodoEcalCoincidence2019());
        setValue(HODOECAL_GEO_MATCHING,  new HodoEcalCoincidence2019());
    }
    
    /**
     * Gets the configuration object for the cluster energy lower bound
     * cut. Note that cuts are in units of GeV.
     * @return Returns the configuration object for the cut.
     */
    public LBOCutConfig2019 getEnergyMinCutConfig() {
        return (LBOCutConfig2019) getValue(CUT_ENERGY_MIN);
    }
    
    /**
     * Gets the configuration object for the cluster energy upper bound
     * cut. Note that cuts are in units of GeV.
     * @return Returns the configuration object for the cut.
     */
    public UBOCutConfig2019 getEnergyMaxCutConfig() {
        return (UBOCutConfig2019) getValue(CUT_ENERGY_MAX);
    }
    
    /**
     * Gets the configuration object for the cluster hit count cut.
     * Note that cuts are in units of calorimeter hits.
     * @return Returns the configuration object for the cut.
     */
    public LBOCutConfig2019 getHitCountCutConfig() {
        return (LBOCutConfig2019) getValue(CUT_HIT_COUNT);
    }    
       
    /**
     * Gets the configuration object for the pair energy sum cut. Note
     * that cuts are in units of GeV.
     * @return Returns the configuration object for the cut.
     */
    public ULBCutConfig2019 getEnergySumCutConfig() {
        return (ULBCutConfig2019) getValue(CUT_ENERGY_SUM);
    }
    
    /**
     * Gets the configuration object for the pair energy difference
     * cut. Note that cuts are in units of GeV.
     * @return Returns the configuration object for the cut.
     */
    public UBOCutConfig2019 getEnergyDifferenceCutConfig() {
        return (UBOCutConfig2019) getValue(CUT_ENERGY_DIFF);
    }
    
    /**
     * Gets the configuration object for the pair energy slope cut.
     * Note that cuts are in units of GeV and mm.
     * @return Returns the configuration object for the cut.
     */
    public ESBCutConfig2019 getEnergySlopeCutConfig() {
        return (ESBCutConfig2019) getValue(CUT_ENERGY_SLOPE);
    }
    
    /**
     * Gets the configuration object for the pair coplanarity cut.
     * Note that cuts are in units of degrees.
     * @return Returns the configuration object for the cut.
     */
    public UBOCutConfig2019 getCoplanarityCutConfig() {
        return (UBOCutConfig2019) getValue(CUT_COPLANARITY);
    }
    
    /**
     * Gets the configuration object for the pair time coincidence cut.
     * Note that cuts are in units of nanoseconds.
     * @return Returns the configuration object for the cut.
     */
    public UBOCutConfig2019 getTimeDifferenceCutConfig() {
        return (UBOCutConfig2019) getValue(CUT_TIME_DIFF);
    }
    
    /**
     * Gets the configuration object for L1 matching.
     * @return Returns the configuration object.
     */
    public HodoEcalCoincidence2019 getL1MatchingConfig() {
        return (HodoEcalCoincidence2019) getValue(L1_MATCHING);
    }
    
    /**
     * Gets the configuration object for L2 matching.
     * @return Returns the configuration object.
     */
    public HodoEcalCoincidence2019 getL2MatchingConfig() {
        return (HodoEcalCoincidence2019) getValue(L2_MATCHING);
    }
    
    /**
     * Gets the configuration object for L1L2 geometry matching.
     * @return Returns the configuration object.
     */
    public HodoEcalCoincidence2019 getL1L2GeoMatchingConfig() {
        return (HodoEcalCoincidence2019) getValue(L1L2_GEO_MATCHING);
    }
    
    /**
     * Gets the configuration object for hodoscope and Ecal geometry matching.
     * @return Returns the configuration object.
     */
    public HodoEcalCoincidence2019 getHodoEcalGeoMatchingConfig() {
        return (HodoEcalCoincidence2019) getValue(HODOECAL_GEO_MATCHING);
    }
}
