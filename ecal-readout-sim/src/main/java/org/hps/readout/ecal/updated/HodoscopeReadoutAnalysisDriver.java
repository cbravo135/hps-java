package org.hps.readout.ecal.updated;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.hps.readout.ReadoutDataManager;
import org.hps.readout.ReadoutDriver;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.SimCalorimeterHit;

public class HodoscopeReadoutAnalysisDriver extends ReadoutDriver {
    private int localTime = 2;
    
    @Override
    public void process(EventHeader event) {
        // Check the data manager to see if clusters are available.
        // If they are, get them. Otherwise, wait.
        if(!ReadoutDataManager.checkCollectionStatus("EcalClustersGTP", localTime + 4.0)) {
            return;
        }
        Collection<Cluster> clusters = ReadoutDataManager.getData(localTime, localTime + 4.0, "EcalClustersGTP", Cluster.class);
        
        // Get the truth relations for a large time range to make
        // sure that all possible cluster hits are included.
        Collection<LCRelation> hitTruthRelations = ReadoutDataManager.getData(localTime - 50.0, localTime + 20.0, "EcalInternalTruthRelations", LCRelation.class);
        
        // Parse the truth relations into a searchable table.
        
        
        if(!clusters.isEmpty()) {
            System.out.println("Clusters:");
            for(Cluster cluster : clusters) {
                System.out.printf("\tCluster at cell %d and energy %5.3f and %d hits.%n", cluster.getCalorimeterHits().get(0).getCellID(),
                        cluster.getEnergy(), cluster.getCalorimeterHits().size());
            }
        }
        
        localTime += 4.0;
    }
    
    private static final Map<RawCalorimeterHit, Set<SimCalorimeterHit>> parseRelations(Collection<LCRelation> truthRelations) {
        return null;
    }
    
    @Override
    protected double getTimeDisplacement() {
        return 0;
    }
    
    @Override
    protected double getTimeNeededForLocalOutput() {
        return 0;
    }
}