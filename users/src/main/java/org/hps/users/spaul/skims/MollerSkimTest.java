package org.hps.users.spaul.skims;



import java.util.List;

import org.hps.recon.filtering.EventReconFilter;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;

public class MollerSkimTest extends EventReconFilter{
    private String mollerCollectionName = "TargetConstrainedMollerCandidates";

    public void setMollerCollectionName(String val){
        this.mollerCollectionName = val;
    }
    
    private String clusterCollectionName = "EcalClustersCorr";

    public void setClusterCollectionName(String val){
        this.clusterCollectionName = val;
    }
    
    public void setClusterXSumMax(double val){
        this.xSumMax = val;
    }
    public void setClusterXSumMin(double val){
        this.xSumMin = val;
    }
    public void setClusterXMax(double val){
        this.xClusterMax = val;
    }
    public void setClusterXMin(double val){
        this.xClusterMin = val;
    }
    public void setTrackPMax(double val){
        this.trackPMax = val;
    }
    public void setTrackPMin(double val){
        this.trackPMin = val;
    }
    public void setUseOneClusterCuts(boolean val){
        this.oneClusterCuts = val;
    }
    public void setUseTwoClusterCuts(boolean val){
        this.twoClusterCuts = val;
    }
    
    double trackPMin = 0;
    double trackPMax = 1.9;
    double xSumMax = 0;
    double xSumMin = -190; //only an xSum min cut is needed for the skim
    double xClusterMax = 0;
    double xClusterMin = -300; 

    boolean twoClusterCuts = true;
    boolean oneClusterCuts = false;

    public void process(EventHeader event){
        incrementEventProcessed();
        List<ReconstructedParticle> mollers = event.get(ReconstructedParticle.class, mollerCollectionName);
        if(mollers.size() == 0)
            skipEvent();


        List<Cluster> clusters = event.get(Cluster.class, clusterCollectionName);
        if(twoClusterCuts){
            boolean foundPair = false;
            outer : for(Cluster c1 : clusters){
                if(c1.getPosition()[1] < 0 || c1.getPosition()[0] < xClusterMin || c1.getPosition()[0] > xClusterMax)
                    continue;
                for(Cluster c2 : clusters){
                    if(c2.getPosition()[1] > 0 || c2.getPosition()[0] < xClusterMin || c2.getPosition()[0]  > xClusterMax)
                        continue;
                    double xsum = c1.getPosition()[0] + c2.getPosition()[0];
                    if(xsum> xSumMin && xsum < xSumMax){
                        foundPair = true;
                        break outer;
                    }

                }
            }
            if(!foundPair){
                skipEvent();
            }
        } else if(oneClusterCuts){
            boolean found = false;
            for(Cluster c1 : clusters){
                if(c1.getPosition()[0] > xClusterMin || c1.getPosition()[0]  < xClusterMax)
                    found = true;
            }
            if(!found)
                skipEvent();
                
            
        }
        
        boolean found = false;
        for(ReconstructedParticle mol : mollers){
            if(mol.getType()<32)
                continue;
            double p1 = mol.getParticles().get(0).getMomentum().magnitude();
            double p2 = mol.getParticles().get(1).getMomentum().magnitude();
            if(p1 >trackPMin && p1<trackPMax && p2 > trackPMin && p2 < trackPMax)
                found = true;
            
        }
        if(!found)
            skipEvent();
        
        incrementEventPassed();
    }
}
