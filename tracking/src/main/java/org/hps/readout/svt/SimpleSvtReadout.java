package org.hps.readout.svt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtTimingConstants;

import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.event.base.BaseRawTrackerHit;
import org.lcsim.recon.tracking.digitization.sisim.CDFSiSensorSim;
import org.lcsim.recon.tracking.digitization.sisim.SiElectrodeData;
import org.lcsim.recon.tracking.digitization.sisim.SiElectrodeDataCollection;
import org.lcsim.recon.tracking.digitization.sisim.SiSensorSim;
import org.lcsim.recon.tracking.digitization.sisim.config.SimTrackerHitReadoutDriver;
import org.hps.readout.TempOutputWriter;
import org.hps.readout.ecal.ClockSingleton;
import org.hps.readout.ecal.ReadoutTimestamp;
import org.hps.readout.ecal.TriggerableDriver;
import org.hps.recon.tracking.PulseShape;
import org.hps.util.RandomGaussian;

/**
 * SVT readout simulation.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 */
public class SimpleSvtReadout extends TriggerableDriver {

    //-----------------//
    //--- Constants ---//
    //-----------------//
    private static final String SVT_SUBDETECTOR_NAME = "Tracker";

    private PulseShape shape = new PulseShape.FourPole();

    private SimTrackerHitReadoutDriver readoutDriver = new SimTrackerHitReadoutDriver();
    private SiSensorSim siSimulation = new CDFSiSensorSim();
    private Map<SiSensor, PriorityQueue<StripHit>[]> hitMap = new HashMap<SiSensor, PriorityQueue<StripHit>[]>();
    private List<HpsSiSensor> sensors = null;

    // readout period time offset in ns
    private double readoutOffset = 0.0;
    private double readoutLatency = 280.0;
    private double pileupCutoff = 300.0;
    private String readout = "TrackerHits";
    private double timeOffset = 30.0;
    private boolean noPileup = false;
    private boolean addNoise = true;

    private boolean useTimingConditions = false;

    // cut settings
    private boolean enableThresholdCut = true;
    private int samplesAboveThreshold = 3;
    private double noiseThreshold = 2.0;
    private boolean enablePileupCut = true;
    private boolean dropBadChannels = true;

    // Collection Names
    private String outputCollection = "SVTRawTrackerHits";
    private String relationCollection = "SVTTrueHitRelations";
    
    private final TempOutputWriter inputWriter = new TempOutputWriter("svt_input_hits_old.log");
    private final TempOutputWriter outputWriter = new TempOutputWriter("svt_output_hits_old.log");
    private final TempOutputWriter verboseWriter = new TempOutputWriter("svt_verbose_old.log");
    private final TempOutputWriter triggerWriter = new TempOutputWriter("svt_trigger_hits_old.log");

    private int verbosity = 0;
    
    private boolean debug = false;
    
    @Override
    public void endOfData() {
        inputWriter.close();
        outputWriter.close();
        verboseWriter.close();
        triggerWriter.close();
        super.endOfData();
    }
    
    @Override
    public void startOfData() {
        if(debug) {
            inputWriter.initialize();
            outputWriter.initialize();
            verboseWriter.initialize();
            triggerWriter.initialize();
        }
        verboseWriter.write("Test");
        super.startOfData();
    }

    public SimpleSvtReadout() {
        add(readoutDriver);
        triggerDelay = 100.0;
    }

    public void setAddNoise(boolean addNoise) {
        this.addNoise = addNoise;
    }
    
    public void setDebug(boolean state) {
        debug = state;
    }

    public void setEnablePileupCut(boolean enablePileupCut) {
        this.enablePileupCut = enablePileupCut;
    }

    public void setEnableThresholdCut(boolean enableThresholdCut) {
        this.enableThresholdCut = enableThresholdCut;
    }

    public void setNoiseThreshold(double noiseThreshold) {
        this.noiseThreshold = noiseThreshold;
    }

    public void setSamplesAboveThreshold(int samplesAboveThreshold) {
        this.samplesAboveThreshold = samplesAboveThreshold;
    }

    public void setNoPileup(boolean noPileup) {
        this.noPileup = noPileup;
    }

    public void setDropBadChannels(boolean dropBadChannels) {
        this.dropBadChannels = dropBadChannels;
    }

    public void setReadoutLatency(double readoutLatency) {
        this.readoutLatency = readoutLatency;
    }

    public void setUseTimingConditions(boolean useTimingConditions) {
        this.useTimingConditions = useTimingConditions;
    }

    /**
     * Set the amount of printouts generated by this driver. 0 = silent, 1 =
     * normal, 2+ = debug
     *
     * @param verbosity
     */
    public void setVerbosity(int verbosity) {
        this.verbosity = verbosity;
    }

    public void setPulseShape(String pulseShape) {
        switch (pulseShape) {
            case "CR-RC":
                shape = new PulseShape.CRRC();
                break;
            case "FourPole":
                shape = new PulseShape.FourPole();
                break;
            default:
                throw new RuntimeException("Unrecognized pulseShape: " + pulseShape);
        }
    }

    /**
     *
     */
    @Override
    public void detectorChanged(Detector detector) {
        super.detectorChanged(detector);

        // Get the collection of all SiSensors from the SVT 
        sensors = detector.getSubdetector(SVT_SUBDETECTOR_NAME).getDetectorElement().findDescendants(HpsSiSensor.class);

        String[] readouts = {readout};
        readoutDriver.setCollections(readouts);

        if (!noPileup) {
            for (HpsSiSensor sensor : sensors) {
                @SuppressWarnings("unchecked")
                PriorityQueue<StripHit>[] hitQueues = new PriorityQueue[sensor.getNumberOfChannels()];
                hitMap.put(sensor, hitQueues);
            }
        }

        if (useTimingConditions) {
            SvtTimingConstants timingConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtTimingConstants.SvtTimingConstantsCollection.class, "svt_timing_constants").getCachedData().get(0);
            readoutOffset = 4 * (timingConstants.getOffsetPhase() + 3);
            readoutLatency = 248.0 + timingConstants.getOffsetTime();
        }
    }

    /**
     *
     */
    @Override
    public void process(EventHeader event) {
        super.process(event);
        
        List<StripHit> stripHits = doSiSimulation();

        if (!noPileup) {
            for (StripHit stripHit : stripHits) {
                SiSensor sensor = stripHit.sensor;
                int channel = stripHit.channel;

                PriorityQueue<StripHit>[] hitQueues = hitMap.get(sensor);
                if (hitQueues[channel] == null) {
                    hitQueues[channel] = new PriorityQueue<StripHit>();
                }
                hitQueues[channel].add(stripHit);
            }

            // dump stale hits
            for (SiSensor sensor : sensors) {
                PriorityQueue<StripHit>[] hitQueues = hitMap.get(sensor);
                for (int i = 0; i < hitQueues.length; i++) {
                    if (hitQueues[i] != null) {
                        while (!hitQueues[i].isEmpty() && hitQueues[i].peek().time < ClockSingleton.getTime() - (readoutLatency + pileupCutoff)) {
                            //System.out.format("Time %f: Dump stale hit with time %f\n",ClockSingleton.getTime(),hitQueues[i].peek().time);
                            hitQueues[i].poll();
                        }
                        if (hitQueues[i].isEmpty()) {
                            hitQueues[i] = null;
                        }
                    }
                }
            }

            // If an ECal trigger is received, make hits from pipelines
            checkTrigger(event);
        } else {

            // Create a list to hold the analog data
            List<RawTrackerHit> hits = new ArrayList<RawTrackerHit>();

            for (StripHit stripHit : stripHits) {
                HpsSiSensor sensor = (HpsSiSensor) stripHit.sensor;
                int channel = stripHit.channel;
                double amplitude = stripHit.amplitude;
                short[] samples = new short[6];

                double[] signal = new double[6];
                for (int sampleN = 0; sampleN < 6; sampleN++) {
                    signal[sampleN] = sensor.getPedestal(channel, sampleN);
                }
                if (addNoise) {
                    addNoise(sensor, channel, signal);
                }

                for (int sampleN = 0; sampleN < 6; sampleN++) {
                    double time = sampleN * HPSSVTConstants.SAMPLING_INTERVAL - timeOffset;
                    shape.setParameters(channel, (HpsSiSensor) sensor);
                    signal[sampleN] += amplitude * shape.getAmplitudePeakNorm(time);//add the pulse to the pedestal
                    samples[sampleN] = (short) Math.round(signal[sampleN]);
                    if (verbosity >= 1) {
                        System.out.println("\t\tMaking samples: sample#" + sampleN + " has " + samples[sampleN] + " ADC counts");
                    }
                }

                long channel_id = sensor.makeChannelID(channel);
                RawTrackerHit hit = new BaseRawTrackerHit(0, channel_id, samples, new ArrayList<SimTrackerHit>(stripHit.simHits), sensor);
                if (readoutCuts(hit)) {
                    hits.add(hit);
                }
            }

            int flags = 1 << LCIOConstants.TRAWBIT_ID1;
            // flags += 1 << LCIOConstants.RTHBIT_HITS;
            event.put(outputCollection, hits, RawTrackerHit.class, flags, readout);
            if (verbosity >= 1) {
                System.out.println("Made " + hits.size() + " RawTrackerHits");
            }
        }
    }

    /**
     *
     * @return Collection of StripHits
     */
    private List<StripHit> doSiSimulation() {

        List<StripHit> stripHits = new ArrayList<StripHit>();

        for (SiSensor sensor : sensors) {

            // Set the sensor to be used in the charge deposition simulation
            siSimulation.setSensor(sensor);

            // Perform the charge deposition simulation
            Map<ChargeCarrier, SiElectrodeDataCollection> electrodeDataMap = siSimulation.computeElectrodeData();

            for (ChargeCarrier carrier : ChargeCarrier.values()) {

                // If the sensor is capable of collecting the given charge carrier
                // then obtain the electrode data for the sensor
                if (sensor.hasElectrodesOnSide(carrier)) {

                    SiElectrodeDataCollection electrodeDataCol = electrodeDataMap.get(carrier);

                    // If there is no electrode data available create a new instance of electrode
                    // data
                    if (electrodeDataCol == null) {
                        electrodeDataCol = new SiElectrodeDataCollection();
                    }

                    // Loop over all sensor channels
                    for (Integer channel : electrodeDataCol.keySet()) {

                        // Get the electrode data for this channel
                        SiElectrodeData electrodeData = electrodeDataCol.get(channel);
                        Set<SimTrackerHit> simHits = electrodeData.getSimulatedHits();

                        // compute hit time as the unweighted average of SimTrackerHit times; this
                        // is dumb but okay since there's generally only one SimTrackerHit
                        double time = 0.0;
                        for (SimTrackerHit hit : simHits) {
                            time += hit.getTime();
                        }
                        time /= simHits.size();
                        time += ClockSingleton.getTime();

                        // Get the charge in units of electrons
                        double charge = electrodeData.getCharge();

                        double resistorValue = 100; // Ohms
                        double inputStageGain = 1.5;
                        // FIXME: This should use the gains instead
                        double amplitude = (charge / HPSSVTConstants.MIP) * resistorValue * inputStageGain * Math.pow(2, 14) / 2000;

                        stripHits.add(new StripHit(sensor, channel, amplitude, time, simHits));
                    }
                }
            }
            // Clear the sensors of all deposited charge
            siSimulation.clearReadout();
        }
        return stripHits;
    }

    private void addNoise(SiSensor sensor, int channel, double[] signal) {
        for (int sampleN = 0; sampleN < 6; sampleN++) {
            signal[sampleN] += RandomGaussian.getGaussian(0, ((HpsSiSensor) sensor).getNoise(channel, sampleN));
        }
    }

    private boolean readoutCuts(RawTrackerHit hit) {
        if (enableThresholdCut && !samplesAboveThreshold(hit)) {
            if (verbosity > 1) {
                System.out.println("Failed threshold cut");
            }
            return false;
        }
        if (enablePileupCut && !pileupCut(hit)) {
            if (verbosity > 1) {
                System.out.println("Failed pileup cut");
            }
            return false;
        }
        if (dropBadChannels && !badChannelCut(hit)) {
            if (verbosity > 1) {
                System.out.println("Failed bad channel cut");
            }
            return false;
        }
        return true;
    }

    private boolean badChannelCut(RawTrackerHit hit) {
        HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
        int channel = hit.getIdentifierFieldValue("strip");
        return !sensor.isBadChannel(channel);
    }

    private boolean pileupCut(RawTrackerHit hit) {
        short[] samples = hit.getADCValues();
        return (samples[2] > samples[1] || samples[3] > samples[2]);
    }

    private boolean samplesAboveThreshold(RawTrackerHit hit) {
        HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
        int channel = hit.getIdentifierFieldValue("strip");
        double pedestal;
        double noise;
        int count = 0;
        short[] samples = hit.getADCValues();
        for (int sampleN = 0; sampleN < samples.length; sampleN++) {
            pedestal = sensor.getPedestal(channel, sampleN);
            noise = sensor.getNoise(channel, sampleN);
            if (verbosity > 1) {
                System.out.format("%f, %f\n", samples[sampleN] - pedestal, noise * noiseThreshold);
            }
            if (samples[sampleN] - pedestal > noise * noiseThreshold) {
                count++;
            }
        }
        return count >= samplesAboveThreshold;
    }

    @Override
    protected void processTrigger(EventHeader event) {
        verboseWriter.write("Running method ProcessTrigger(EventHeader).");
        verboseWriter.write("noPileup = " + noPileup);
        
        if (noPileup) {
            return;
        }
        //System.out.println("Got trigger");

        // Create a list to hold the analog data
        List<RawTrackerHit> hits = new ArrayList<RawTrackerHit>();
        List<LCRelation> trueHitRelations = new ArrayList<LCRelation>();
        // Calculate time of first sample
        double firstSample = Math.floor((ClockSingleton.getTime() - readoutLatency - readoutOffset) / HPSSVTConstants.SAMPLING_INTERVAL)
                * HPSSVTConstants.SAMPLING_INTERVAL + readoutOffset;
        
        
        outputWriter.write("> Event " + event.getEventNumber() + "; t = " + ClockSingleton.getTime());
        List<StripHit> processedHits = new ArrayList<StripHit>();
        
        verboseWriter.write("Processing trigger request for Event " + event.getEventNumber() + " at time " + ClockSingleton.getTime());
        verboseWriter.write("\tNoise is " + (addNoise ? "" : "not ") + "enabled.");
        verboseWriter.write("\tClockSingleton.getTime()          :: " + ClockSingleton.getTime());
        verboseWriter.write("\treadoutLatency                    :: " + readoutLatency);
        verboseWriter.write("\treadoutOffset                     :: " + readoutOffset);
        verboseWriter.write("\tHPSSVTConstants.SAMPLING_INTERVAL :: " + HPSSVTConstants.SAMPLING_INTERVAL);
        verboseWriter.write("\treadoutOffset                     :: " + readoutOffset);
        verboseWriter.write("\tfirstSample                       :: " + firstSample);
        verboseWriter.write("\tdouble firstSample = Math.floor((ClockSingleton.getTime() - readoutLatency - readoutOffset) / HPSSVTConstants.SAMPLING_INTERVAL) "
                + "* HPSSVTConstants.SAMPLING_INTERVAL + readoutOffset;");
        verboseWriter.write(String.format("\tdouble %f = Math.floor((%f - %f - %f) / %f) * %f + %f;", firstSample, ClockSingleton.getTime(), readoutLatency,
                readoutOffset, HPSSVTConstants.SAMPLING_INTERVAL, HPSSVTConstants.SAMPLING_INTERVAL, readoutOffset));
        
        triggerWriter.write(">" + ClockSingleton.getTime() + ";" + (firstSample * HPSSVTConstants.SAMPLING_INTERVAL) + ";" + (readoutLatency - readoutOffset));
        
        for (SiSensor sensor : sensors) {
            
            verboseWriter.write("\tGot hit queue for sensor " + sensor.getName() + ".");
            
            PriorityQueue<StripHit>[] hitQueues = hitMap.get(sensor);
            for (int channel = 0; channel < hitQueues.length; channel++) {
                
                verboseWriter.write("\t\tExamining channel " + channel + ".");
                
                if (!addNoise && (hitQueues[channel] == null || hitQueues[channel].isEmpty())) {
                    verboseWriter.write("\t\t\tChannel is empty. Nothing to process...");
                    continue;
                }
                double[] signal = new double[6];
                for (int sampleN = 0; sampleN < 6; sampleN++) {
                    signal[sampleN] = ((HpsSiSensor) sensor).getPedestal(channel, sampleN);
                }
                if (addNoise) {
                    addNoise(sensor, channel, signal);
                }

                List<SimTrackerHit> simHits = new ArrayList<SimTrackerHit>();

                if (hitQueues[channel] != null) {
                    for (StripHit hit : hitQueues[channel]) {
                        
                        processedHits.add(hit);
                        
                        verboseWriter.write(String.format("\t\t\tEvaluating hit with amplitude %f at time %f on channel %d.", hit.amplitude, hit.time,
                                ((HpsSiSensor) sensor).makeChannelID(channel)));
                        
                        double totalContrib = 0;
                        double meanNoise = 0;
                        StringBuffer signalBuffer = new StringBuffer("\t\t\t\tSample Pulse       :: [");
                        for (int sampleN = 0; sampleN < 6; sampleN++) {
                            verboseWriter.write("\t\t\t\tSample " + (sampleN + 1));
                            verboseWriter.write("\t\t\t\t\tSensor             :: " + ((HpsSiSensor) sensor).getName());
                            double sampleTime = firstSample + sampleN * HPSSVTConstants.SAMPLING_INTERVAL;
                            verboseWriter.write("\t\t\t\t\tSample Time        :: " + sampleTime);
                            shape.setParameters(channel, (HpsSiSensor) sensor);
                            double signalAtTime = hit.amplitude * shape.getAmplitudePeakNorm(sampleTime - hit.time);
                            verboseWriter.write("\t\t\t\t\tSignal At Time     :: " + signalAtTime);
                            totalContrib += signalAtTime;
                            verboseWriter.write("\t\t\t\t\tTotal Contribution :: " + totalContrib);
                            signal[sampleN] += signalAtTime;
                            meanNoise += ((HpsSiSensor) sensor).getNoise(channel, sampleN);
                            verboseWriter.write("\t\t\t\t\tNoise on Sensor    :: " + ((HpsSiSensor) sensor).getNoise(channel, sampleN));
                            
                            signalBuffer.append(signalAtTime + " (" + sampleTime + ")");
                            if(sampleN != 5) {
                                signalBuffer.append("   ");
                            }
                        }
                        meanNoise /= 6;
                        
                        signalBuffer.append("]");
                        
                        verboseWriter.write(signalBuffer.toString());
                        verboseWriter.write("\t\t\t\tTotal Contribution :: " + totalContrib);
                        verboseWriter.write("\t\t\t\tMean Noise         :: " + meanNoise);
                        // Compare to the mean noise of the six samples instead
                        if (totalContrib > 4.0 * meanNoise) {
                            //System.out.format("adding %d simHits\n", hit.simHits.size());
                            simHits.addAll(hit.simHits);
                            verboseWriter.write("\t\t\t\tAdded hit.");
                        } else {
                            verboseWriter.write("\t\t\t\tIgnored hit.");
                        }
                    }
                }

                short[] samples = new short[6];
                for (int sampleN = 0; sampleN < 6; sampleN++) {
                    samples[sampleN] = (short) Math.round(signal[sampleN]);
                }
                long channel_id = ((HpsSiSensor) sensor).makeChannelID(channel);
                RawTrackerHit hit = new BaseRawTrackerHit(0, channel_id, samples, simHits, sensor);
                if (readoutCuts(hit)) {
                    hits.add(hit);
                    //System.out.format("simHits: %d\n", simHits.size());
                    for (SimTrackerHit simHit : hit.getSimTrackerHits()) {
                        LCRelation hitRelation = new BaseLCRelation(hit, simHit);
                        trueHitRelations.add(hitRelation);
                    }
                }
            }
        }
        
        
        outputWriter.write("Input");
        for(StripHit hit : processedHits) {
            outputWriter.write(String.format("%f;%f;%d;%s", hit.amplitude, hit.time, hit.channel, hit.sensor.getName()));
        }
        
        outputWriter.write("Output");
        for(RawTrackerHit hit : hits) {
            outputWriter.write(String.format("%d;%d;%d;%d;%d;%d;%d;%d", hit.getCellID(), hit.getLayerNumber(), hit.getADCValues()[0],
                    hit.getADCValues()[1], hit.getADCValues()[2], hit.getADCValues()[3], hit.getADCValues()[4], hit.getADCValues()[5]));
            triggerWriter.write(String.format("%d;%d;%d;%d;%d;%d;%d;%d", hit.getCellID(), hit.getLayerNumber(), hit.getADCValues()[0],
                    hit.getADCValues()[1], hit.getADCValues()[2], hit.getADCValues()[3], hit.getADCValues()[4], hit.getADCValues()[5]));
        }
        

        int flags = 1 << LCIOConstants.TRAWBIT_ID1;
        event.put(outputCollection, hits, RawTrackerHit.class, flags, readout);
        event.put(relationCollection, trueHitRelations, LCRelation.class, 0);
        if (verbosity >= 1) {
            System.out.println("Made " + hits.size() + " RawTrackerHits");
            System.out.println("Made " + trueHitRelations.size() + " LCRelations");
        }
    }

    @Override
    public double readoutDeltaT() {
        double triggerTime = ClockSingleton.getTime() + triggerDelay;
        // Calculate time of first sample
        double firstSample = Math.floor((triggerTime - readoutLatency - readoutOffset) / HPSSVTConstants.SAMPLING_INTERVAL) * HPSSVTConstants.SAMPLING_INTERVAL + readoutOffset;

        return firstSample;
    }

    private class StripHit implements Comparable<Object> {

        SiSensor sensor;
        int channel;
        double amplitude;
        double time;
        Set<SimTrackerHit> simHits;

        public StripHit(SiSensor sensor, int channel, double amplitude, double time, Set<SimTrackerHit> simHits) {
            this.sensor = sensor;
            this.channel = channel;
            this.amplitude = amplitude;
            this.time = time;
            this.simHits = simHits;
        }

        @Override
        public int compareTo(Object o) {
            double deltaT = time - ((StripHit) o).time;
            if (deltaT > 0) {
                return 1;
            } else if (deltaT < 0) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    @Override
    public int getTimestampType() {
        return ReadoutTimestamp.SYSTEM_TRACKER;
    }
}