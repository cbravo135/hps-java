package org.hps.analysis.trigger.util;

import org.hps.record.triggerbank.SSPCluster;
import org.hps.record.triggerbank.VTPCluster;
import org.hps.record.triggerbank.TriggerModule;
import org.hps.record.triggerbank.TriggerModule2019;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;

/**
 * Class <code>TriggerDiagnosticUtil</code> contains a series of
 * utility methods that are used at various points throughout the
 * trigger diagnostic package.
 */
public class TriggerDiagnosticUtil {
    /**
     * Convenience method that writes the position of a cluster in the
     * form (ix, iy).
     * @param cluster - The cluster.
     * @return Returns the cluster position as a <code>String</code>.
     */
    public static final String clusterPositionString(Cluster cluster) {
        return String.format("(%3d, %3d)", TriggerModule.getClusterXIndex(cluster), TriggerModule.getClusterYIndex(cluster));
    }
    
    /**
     * Convenience method that writes the position of a cluster in the
     * form (ix, iy).
     * @param cluster - The cluster.
     * @return Returns the cluster position as a <code>String</code>.
     */
    public static final String clusterPositionString(SSPCluster cluster) {
        return String.format("(%3d, %3d)", TriggerModule.getClusterXIndex(cluster), TriggerModule.getClusterYIndex(cluster));
    }
    
    /**
     * Convenience method that writes the position of a cluster in the
     * form (ix, iy).
     * @param cluster - The cluster.
     * @return Returns the cluster position as a <code>String</code>.
     */
    public static final String clusterPositionString(VTPCluster cluster) {
        return String.format("(%3d, %3d)", TriggerModule2019.getClusterXIndex(cluster), TriggerModule2019.getClusterYIndex(cluster));
    }
    
    /**
     * Convenience method that writes the information in a cluster to
     * a <code>String</code>.
     * @param cluster - The cluster.
     * @return Returns the cluster information as a <code>String</code>.
     */
    public static final String clusterToString(Cluster cluster) {
        return String.format("Cluster at (%3d, %3d) with %.3f GeV and %.0f hits at %4.0f ns.",
                TriggerModule.getClusterXIndex(cluster), TriggerModule.getClusterYIndex(cluster),
                TriggerModule.getValueClusterTotalEnergy(cluster), TriggerModule.getClusterHitCount(cluster),
                TriggerModule.getClusterTime(cluster));
    }
    
    /**
     * Convenience method that writes the information in a cluster to
     * a <code>String</code>.
     * @param cluster - The cluster.
     * @return Returns the cluster information as a <code>String</code>.
     */
    public static final String clusterToString(SSPCluster cluster) {
        return String.format("Cluster at (%3d, %3d) with %.3f GeV and %.0f hits at %4.0f ns.",
                TriggerModule.getClusterXIndex(cluster), TriggerModule.getClusterYIndex(cluster),
                TriggerModule.getValueClusterTotalEnergy(cluster), TriggerModule.getClusterHitCount(cluster),
                TriggerModule.getClusterTime(cluster));
    }
    
    /**
     * Convenience method that writes the information in a cluster to
     * a <code>String</code>.
     * @param cluster - The cluster.
     * @return Returns the cluster information as a <code>String</code>.
     */
    public static final String clusterToString(VTPCluster cluster) {
        return String.format("Cluster at (%3d, %3d) with %.3f GeV and %.0f hits at %4.0f ns.",
                TriggerModule2019.getClusterXIndex(cluster), TriggerModule2019.getClusterYIndex(cluster),
                TriggerModule2019.getValueClusterTotalEnergy(cluster), TriggerModule2019.getClusterHitCount(cluster),
                TriggerModule2019.getClusterTime(cluster));
    }
    
    /**
     * Gets the number of digits in the base-10 String representation
     * of an integer primitive. Negative signs are not included in the
     * digit count.
     * @param value - The value of which to obtain the length.
     * @return Returns the number of digits in the String representation
     * of the argument value.
     */
    public static final int getDigits(int value) {
        if(value < 0) { return Integer.toString(value).length() - 1; }
        else { return Integer.toString(value).length(); }
    }
    
    /**
     * Checks whether a cluster is within the safe region of the FADC
     * output window.
     * @param sspCluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster is safe and
     * returns <code>false</code> otherwise.
     */
    public static final boolean isVerifiable(SSPCluster sspCluster, int nsa, int nsb, int windowWidth) {
        // Check that none of the hits are within the disallowed
        // region of the FADC readout window.
        if(TriggerModule.getClusterTime(sspCluster) < nsb || TriggerModule.getClusterTime(sspCluster) > (windowWidth - nsa)) {
            return false;
        }
        
        // If all of the cluster hits pass the time cut, the cluster
        // is valid.
        return true;
    }
    
    /**
     * Checks whether a cluster is within the safe region of the FADC
     * output window.
     * @param vtpCluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster is safe and
     * returns <code>false</code> otherwise.
     */
    public static final boolean isVerifiable(VTPCluster vtpCluster, int nsa, int nsb, int windowWidth) {
        // Check that none of the hits are within the disallowed
        // region of the FADC readout window.
        if(TriggerModule2019.getClusterTime(vtpCluster) < nsb || TriggerModule2019.getClusterTime(vtpCluster) > (windowWidth - nsa)) {
            return false;
        }
        
        // If all of the cluster hits pass the time cut, the cluster
        // is valid.
        return true;
    }
    
    /**
     * Checks whether all of the hits in a cluster are within the safe
     * region of the FADC output window.
     * @param reconCluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster is safe and
     * returns <code>false</code> otherwise.
     */
    public static final boolean isVerifiable(Cluster reconCluster, int nsa, int nsb, int windowWidth) {
        // Iterate over the hits in the cluster.
        for(CalorimeterHit hit : reconCluster.getCalorimeterHits()) {
            // Check that none of the hits are within the disallowed
            // region of the FADC readout window.
            if(hit.getTime() < nsb || hit.getTime() > (windowWidth - nsa)) {
                return false;
            }
            
            // Also check to make sure that the cluster does not have
            // any negative energy hits. These are, obviously, wrong.
            if(hit.getCorrectedEnergy() < 0.0) {
                return false;
            }
        }
        
        // If all of the cluster hits pass the time cut, the cluster
        // is valid.
        return true;
    }
        
    /**
     * Checks whether all of the hodoscope hits in a trigger are within the safe region of the FADC output window
     * @param trigger
     * @param nsa
     * @param nsb
     * @param windowWidth
     * @return
     */
    
    public static final <E> boolean isVerifiableHodoHits(SinglesTrigger2019<E> trigger, Class<E> clusterType, int nsa, int nsb, int windowWidth) {
        // Iterate over the hits in the hodoscope hit list of the trigger.
        // Verify that the cluster type is supported.
        if (!clusterType.equals(Cluster.class) && !clusterType.equals(VTPCluster.class)) {
            throw new IllegalArgumentException(
                    "Class \"" + clusterType.getSimpleName() + "\" is not a supported cluster type.");
        }
        
        for (CalorimeterHit hit : trigger.getHodoHitList()) {
            // Check that none of the hits are within the disallowed
            // region of the FADC readout window.
            if (hit.getTime() < nsb || hit.getTime() > (windowWidth - nsa)) {
                return false;
            }
        }

        // If all of the cluster hits pass the time cut, the cluster
        // is valid.
        return true;
    }
    
    /**
     * Checks whether all of the hodoscope hits in a trigger are within the safe region of the FADC output window
     * @param trigger
     * @param nsa
     * @param nsb
     * @param windowWidth
     * @return
     */
    
    public static final <E> boolean isVerifiableHodoHits(PairTrigger2019<E[]> trigger, Class<E> clusterType, int nsa, int nsb, int windowWidth) {
        // Iterate over the hits in the hodoscope hit list of the trigger.
        // Verify that the cluster type is supported.
        if (!clusterType.equals(Cluster.class) && !clusterType.equals(VTPCluster.class)) {
            throw new IllegalArgumentException(
                    "Class \"" + clusterType.getSimpleName() + "\" is not a supported cluster type.");
        }
        
        for (CalorimeterHit hit : trigger.getHodoHitList()) {
            // Check that none of the hits are within the disallowed
            // region of the FADC readout window.
            if (hit.getTime() < nsb || hit.getTime() > (windowWidth - nsa)) {
                return false;
            }
        }

        // If all of the cluster hits pass the time cut, the cluster
        // is valid.
        return true;
    }
}
