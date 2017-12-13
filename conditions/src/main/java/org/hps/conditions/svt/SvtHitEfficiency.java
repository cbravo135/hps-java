package org.hps.conditions.svt;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.MultipleCollectionsAction;

/**
 * Condition mapping SVT channels to their calculated hit efficiency.
 */
@Table(names = {"svt_hit_efficiencies"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_UPDATED)
public class SvtHitEfficiency extends BaseConditionsObject {

    @SuppressWarnings("serial")
    public static class SvtHitEfficiencyCollection extends BaseConditionsObjectCollection<SvtGain> {
    }

    @Field(names = {"svt_channel_id"})
    public Integer getChannelID() {
        return this.getFieldValue(Integer.class, "svt_channel_id");
    }

    @Field(names = {"efficiency"})
    public Double getEfficiency() {
        return this.getFieldValue(Double.class, "efficiency");
    }
}
