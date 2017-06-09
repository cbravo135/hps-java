package org.hps.recon.ecal;

import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.beam.BeamEnergy.BeamEnergyCollection;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.conditions.ecal.EcalTimeWalk.EcalTimeWalkCollection;
import org.hps.conditions.ecal.TimeDependentEnergyCorrection;
import org.hps.conditions.ecal.TimeDependentEnergyCorrection.TimeDependentEnergyCorrectionCollection;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseCalorimeterHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

public class TimeDependentEnergyCorrectionsDriver extends Driver{
    private String inputHitsCollectionName;
    private String outputHitsCollectionName;
    private boolean isTransient = true;

   private EcalConditions ecalConditions;

    double getCorrectionFactor(CalorimeterHit hit, long timestamp){

        long cellID = hit.getCellID();
        return findChannel(cellID).getTimeDependentEnergyCorrection().calculateCorrection(timestamp);
    }    

    public EcalChannelConstants findChannel(long cellID) {
        return ecalConditions.getChannelConstants(ecalConditions.getChannelCollection().findGeometric(cellID));
    }

    public void setIsTransient(boolean val){
        this.isTransient = val;
    }

    @Override
    public void process(EventHeader event){
        List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputHitsCollectionName);
        int flags = event.getMetaData(hits).getFlags();
        long timestamp = event.getTimeStamp()/1000000000; //convert to ns.  
        List<CalorimeterHit> outhits = new ArrayList();


        for(CalorimeterHit inhit : hits){

            double corrFactor = getCorrectionFactor(inhit, timestamp);
            BaseCalorimeterHit corrhit = new BaseCalorimeterHit(
                    inhit.getRawEnergy()*corrFactor, 
                    inhit.getCorrectedEnergy()*corrFactor,
                    inhit.getEnergyError()*corrFactor,
                    inhit.getTime(),
                    inhit.getCellID(), 
                    inhit.getPositionVec(), 
                    inhit.getType(),
                    inhit.getMetaData()
                    );
            outhits.add(corrhit);
        }
        event.put(outputHitsCollectionName, outhits, CalorimeterHit.class, flags);
        event.getMetaData(outhits).setTransient(isTransient);
    }

    public void setInputHitsCollectionName(String val){
        this.inputHitsCollectionName = val;
    }
    public void setOutputHitsCollectionName(String val){
        this.outputHitsCollectionName = val;
    }


    public void detectorChanged(Detector detector){
        DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        ecalConditions = manager.getEcalConditions();

    }
}
