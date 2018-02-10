package org.hps.readout.ecal.updated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.readout.ReadoutDataManager;
import org.hps.readout.ReadoutDriver;
import org.hps.readout.TempOutputWriter;
import org.hps.readout.util.LcsimCollection;
import org.hps.recon.ecal.cluster.ClusterType;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.geometry.subdetector.HPSEcal3.NeighborMap;
import org.lcsim.lcio.LCIOConstants;

/**
 * Class <code>GTPClusterReadoutDriver</code> produces GTP cluster
 * objects for use in the readout trigger simulation. It takes in
 * {@link org.lcsim.event.CalorimeterHit CalorimeterHit} objects as
 * input and generates clusters from these using the GTP algorithm.
 * This algorithm works by selected all hits in the current
 * clock-cycle (4 ns period) and comparing them to adjacent hits. If
 * a given hit is an energy maximum compared to all adjacent hits in
 * both the current clock-cycle, and a number of clock-cycles before
 * and after the current cycle (defined through the variable {@link
 * org.hps.readout.ecal.updated.GTPClusterReadoutDriver#temporalWindow
 * temporalWindow} and set through the method {@link
 * org.hps.readout.ecal.updated.GTPClusterReadoutDriver#setClusterWindow(int)
 * setClusterWindow(int)}), then it is a seed hit so long as it also
 * exceeds a certain minimum energy (defined through the variable
 * {@link
 * org.hps.readout.ecal.updated.GTPClusterReadoutDriver#seedEnergyThreshold
 * seedEnergyThreshold} and set through the method {@link
 * org.hps.readout.ecal.updated.GTPClusterReadoutDriver#setSeedEnergyThreshold(double)
 * setSeedEnergyThreshold(double)}).<br/><br/>
 * Clusters are then output as objects of type {@link
 * org.lcsim.event.Cluster Cluster} to the specified output
 * collection. If the {@link
 * org.hps.readout.ecal.updated.GTPClusterReadoutDriver#setWriteClusterCollection(boolean)
 * setWriteClusterCollection(boolean)} is set to true, the clusters
 * will also be persisted into the output LCIO file.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class GTPClusterReadoutDriver extends ReadoutDriver {
	// ==============================================================
	// ==== LCIO Collections ========================================
	// ==============================================================
	
	/**
	 * The name of the collection that contains the calorimeter hits
	 * from which clusters should be generated.
	 */
	private String inputCollectionName = "EcalCorrectedHits";
	/**
	 * The name of the collection into which generated clusters should
	 * be output.
	 */
	private String outputCollectionName = "EcalClustersGTP";
	
	// ==============================================================
	// ==== Driver Options ==========================================
	// ==============================================================
	
	/**
	 * The time window used for cluster verification. A seed hit must
	 * be the highest energy hit within plus or minus this range in
	 * order to be considered a valid cluster.
	 */
	private int temporalWindow = 16;
	/**
	 * The minimum energy needed for a hit to be considered as a seed
	 * hit candidate.
	 */
	private double seedEnergyThreshold = 0.050;
	/**
	 * The local time for the driver. This starts at 2 ns due to a
	 * quirk in the timing of the {@link
	 * org.hps.readout.ecal.updated.EcalReadoutDriver
	 * EcalReadoutDriver}.
	 */
	private double localTime = 2.0;
	/**
	 * The length of time by which objects produced by this driver
	 * are shifted due to the need to buffer data from later events.
	 * This is calculated automatically.
	 */
	private double localTimeDisplacement = 0;
	/**
	 * Indicates whether or not the GTP clusters produced by the
	 * driver should be output into the LCIO file.
	 */
	private boolean outputClusters = true;
	
	// ==============================================================
	// ==== Driver Parameters =======================================
	// ==============================================================
	
	/**
	 * An object which can provide, given an argument cell ID, a map
	 * of cell IDs that are physically adjacent to the argument ID.
	 * This is used to determine adjacency for energy comparisons in
	 * the clustering algorithm.
	 */
	private NeighborMap neighborMap;
	
	// ==============================================================
	// ==== Debug Output Writers ====================================
	// ==============================================================
	
	/**
	 * Outputs debug comparison data for both input hits and output
	 * clusters to a text file.
	 */
	private final TempOutputWriter writer = new TempOutputWriter("clusters_new.log");
	/**
	 * Outputs debug comparison data for seed hits to a text file.
	 */
	private final TempOutputWriter seedWriter = new TempOutputWriter("cluster_seeds_new.log");
	
	@Override
	public void endOfData() {
		if(debug) {
			writer.close();
			seedWriter.close();
		}
	}
    
	@Override
	public void detectorChanged(Detector etector) {
		// Get the calorimeter data object.
		HPSEcal3 ecal = (HPSEcal3) DatabaseConditionsManager.getInstance().getDetectorObject().getSubdetector("Ecal");
        if(ecal == null) {
            throw new IllegalStateException("Error: Calorimeter geometry data object not defined.");
        }
        
        // Get the calorimeter hit neighbor map.
        neighborMap = ecal.getNeighborMap();
        if(neighborMap == null) {
            throw new IllegalStateException("Error: Calorimeter hit neighbor map is not defined.");
        }
	}
	
	@Override
	public void process(EventHeader event) {
		// DEBUG :: Declare event headers.
		writer.write("> Event " + event.getEventNumber() + " - " + ReadoutDataManager.getCurrentTime() + " (Current) - "
				+ (ReadoutDataManager.getCurrentTime() - ReadoutDataManager.getTotalTimeDisplacement(outputCollectionName)) + " (Local)");
		seedWriter.write("> Event " + event.getEventNumber() + " - " + ReadoutDataManager.getCurrentTime());
		
		// DEBUG :: Output all input hits.
	    if(Math.round(ReadoutDataManager.getCurrentTime()) % 4 != 0) {
	    	if(ReadoutDataManager.checkCollectionStatus(inputCollectionName, localTime + temporalWindow + 4.0)) {
				writer.write("Input");
				for(int offset = temporalWindow; offset >= -temporalWindow; offset -= 4.0) {
					Collection<CalorimeterHit> windowHits = ReadoutDataManager.getData(localTime + offset, localTime + offset + 4.0,
							inputCollectionName, CalorimeterHit.class);
					for(CalorimeterHit hit : windowHits) {
						writer.write(String.format("%f;%f;%d", hit.getRawEnergy(), hit.getTime(), hit.getCellID()));
					}
				}
	    	}
	    }
		
		// Check the data management driver to determine whether the
		// input collection is available or not.
		if(!ReadoutDataManager.checkCollectionStatus(inputCollectionName, localTime + temporalWindow + 4.0)) {
			return;
		}
		
		// Get the hits that occur during the present clock-cycle, as
		// well as the hits that occur in the verification window
		// both before and after the current clock-cycle.
		// TODO: Simplify this?
		Collection<CalorimeterHit> seedCandidates = ReadoutDataManager.getData(localTime, localTime + 4.0, inputCollectionName, CalorimeterHit.class);
		Collection<CalorimeterHit> foreHits = ReadoutDataManager.getData(localTime - temporalWindow, localTime, inputCollectionName, CalorimeterHit.class);
		Collection<CalorimeterHit> postHits = ReadoutDataManager.getData(localTime + 4.0, localTime + temporalWindow + 4.0, inputCollectionName, CalorimeterHit.class);
		
		// DEBUG :: Output the seed hits.
		seedWriter.write("Input");
		for(CalorimeterHit hit : seedCandidates) {
			seedWriter.write(String.format("%f;%f;%d", hit.getRawEnergy(), hit.getTime(), hit.getCellID()));
		}
		
		// Increment the local time.
		localTime += 4.0;
		
		// DEBUG :: Print out the input hits.
		// TODO: Simplify this?
		List<CalorimeterHit> allHits = new ArrayList<CalorimeterHit>(seedCandidates.size() + foreHits.size() + postHits.size());
		allHits.addAll(foreHits);
		allHits.addAll(seedCandidates);
		allHits.addAll(postHits);
		
		// Store newly created clusters.
		List<Cluster> gtpClusters = new ArrayList<Cluster>();
		
		// Iterate over all seed hit candidates.
		seedLoop:
		for(CalorimeterHit seedCandidate : seedCandidates) {
			// A seed candidate must meet a minimum energy cut to be
			// considered for clustering.
			if(seedCandidate.getRawEnergy() < seedEnergyThreshold) {
				continue seedLoop;
			}
			
			// Collect other hits that are adjacent to the seed hit
			// and may be a part of the cluster.
			List<CalorimeterHit> clusterHits = new ArrayList<CalorimeterHit>();
			
			// Iterate over all other hits in the clustering window
			// and check that the seed conditions are met for the
			// seed candidate. Note that all hits are properly within
			// the clustering time window by definition, so the time
			// condition is not checked explicitly.
			hitLoop:
			for(CalorimeterHit hit : allHits) {
				// If the hit is not adjacent to the seed hit, it can
				// be ignored.
				if(!neighborMap.get(seedCandidate.getCellID()).contains(hit.getCellID())) {
					continue hitLoop;
				}
				
				// A seed hit must have the highest energy in its
				// spatiotemporal window. If it is not, this is not a
				// valid seed hit.
				if(seedCandidate.getRawEnergy() < hit.getRawEnergy()) {
					continue seedLoop;
				}
				
				// Add the hit to the list of cluster hits.
				clusterHits.add(hit);
			}
			
			// If no adjacent hit was found that invalidates the seed
			// condition, then the seed candidate is valid and a
			// cluster should be formed.
			gtpClusters.add(createBasicCluster(seedCandidate, clusterHits));
		}
		
		// Pass the clusters to the data management driver.
		ReadoutDataManager.addData(outputCollectionName, gtpClusters, Cluster.class);
		
		// DEBUG :: Output the produced clusters.
		writer.write("Output");
		for(Cluster cluster : gtpClusters) {
			writer.write(String.format("%f;%f;%d;%d", cluster.getEnergy(), cluster.getCalorimeterHits().get(0).getTime(),
					cluster.getCalorimeterHits().size(), cluster.getCalorimeterHits().get(0).getCellID()));
			for(CalorimeterHit hit : cluster.getCalorimeterHits()) {
				writer.write(String.format("\t%f;%f;%d", hit.getRawEnergy(), hit.getTime(), hit.getCellID()));
			}
		}
	}
	
	@Override
	public void startOfData() {
		// Define the output LCSim collection parameters.
		LcsimCollection<Cluster> clusterCollectionParams = new LcsimCollection<Cluster>(outputCollectionName, this, Cluster.class, getTimeDisplacement());
		clusterCollectionParams.setFlags(1 << LCIOConstants.CLBIT_HITS);
		clusterCollectionParams.setPersistent(outputClusters);
		
		// Instantiate the GTP cluster collection with the readout
		// data manager.
		localTimeDisplacement = temporalWindow + 4.0;
		addDependency(inputCollectionName);
		ReadoutDataManager.registerCollection(clusterCollectionParams);
		
		// DEBUG :: Pass the writers to the superclass writer list.
		writers.add(writer);
		writers.add(seedWriter);
		
		// Run the superclass method.
		super.startOfData();
	}
	
	@Override
	protected double getTimeDisplacement() {
		return localTimeDisplacement;
	}

	@Override
	protected double getTimeNeededForLocalOutput() {
		return 0;
	}
	
	/**
	 * Creates a new cluster object from a seed hit and list of hits.
	 * @param seedHit - The seed hit of the new cluster.
	 * @param hits - The hits for the new cluster.
	 * @return Returns a {@link org.lcsim.event.Cluster Cluster}
	 * object with the specified properties.
	 */
	private static final Cluster createBasicCluster(CalorimeterHit seedHit, List<CalorimeterHit> hits) {
        BaseCluster cluster = new BaseCluster();
        cluster.setType(ClusterType.GTP.getType());
        cluster.addHit(seedHit);
        cluster.setPosition(seedHit.getDetectorElement().getGeometry().getPosition().v());
        cluster.setNeedsPropertyCalculation(false);
    	cluster.addHits(hits);
        return cluster;
	}
    
    /**
     * Sets the size of the hit verification temporal window. Note
     * that this defines the size of the window in one direction, so
     * the full time window will be <code>(2 * clusterWindow)+
     * 1</code> clock-cycles in length. (i.e., it will be a length of
     * <code>clusterWindow</code> before the seed hit, a length of
     * <code>clusterWindow</code> after the seed hit, plus the cycle
     * that includes the seed hit.) Time length is in clock-cycles.
     * @param value - The number of clock-cycles around the hit in
     * one direction.
     */
    public void setClusterWindow(int value) {
        temporalWindow = value * 4;
    }
    
    /**
     * Sets the minimum seed energy needed for a hit to be considered
     * for forming a cluster. This is the seed energy lower bound
     * trigger cut and is in units of GeV.
     * @param value - The minimum cluster seed energy in GeV.
     */
    public void setSeedEnergyThreshold(double value) {
    	seedEnergyThreshold = value;
    }
    
    /**
     * Defines whether the output of this clusterer should be
     * persisted to LCIO or not. By default, this is false.
     * @param state - <code>true</code> indicates that clusters will
     * be persisted, and <code>false</code> that they will not.
     */
    public void setWriteClusterCollection(boolean state) {
    	outputClusters = state;
    }
}