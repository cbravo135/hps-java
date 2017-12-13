package org.hps.conditions.svt;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtHitEfficiency.SvtHitEfficiencyCollection;

public class SvtHitEfficiencyTest extends TestCase {

    public void testSvtHitEfficiency() throws Exception {
        final DatabaseConditionsManager manager = new DatabaseConditionsManager();
        manager.setDetector("HPS-dummy-detector", 0);
        final SvtHitEfficiencyCollection coll = manager.getCachedConditions(SvtHitEfficiencyCollection.class, "svt_hit_efficiencies").getCachedData();
        System.out.println("Read " + coll.size() + " SVT hit efficiency records."); 
    }
}
