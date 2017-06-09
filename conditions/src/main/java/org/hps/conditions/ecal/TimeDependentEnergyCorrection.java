package org.hps.conditions.ecal;

import java.util.Comparator;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

/**
 * A per channel ECAL gain value.
 *
 * @author Jeremy McCormick, SLAC
 */
@Table(names = {"ecal_time_dependent_energy_corrections"})
public final class TimeDependentEnergyCorrection extends BaseConditionsObject {

    /**
     * The collection implementation for this class.
     */
    @SuppressWarnings("serial")
    public static final class TimeDependentEnergyCorrectionCollection extends BaseConditionsObjectCollection<TimeDependentEnergyCorrection> {

        /**
         * Comparison implementation by channel ID.
         */
        class ChannelIdComparator implements Comparator<TimeDependentEnergyCorrection> {
            /**
             * Compare two objects by their channel ID.
             *
             * @param o1 The first object.
             * @param o2 The second object.
             * @return -1, 0 or 1 if first channel ID is less than, equal to, or greater than second.
             */
            @Override
            public int compare(final TimeDependentEnergyCorrection o1, final TimeDependentEnergyCorrection o2) {
                if (o1.getChannelId() < o2.getChannelId()) {
                    return -1;
                } else if (o1.getChannelId() > o2.getChannelId()) {
                    return 1;
                } else {
                    return 0;
                }
            }

        }

        /**
         * Sort and return a copy of the collection.
         *
         * @return A sorted copy of the collection.
         */
        public ConditionsObjectCollection<TimeDependentEnergyCorrection> sorted() {
            return this.sorted(new ChannelIdComparator());
        }
    }

    /**
     * Get the ECal channel ID.
     *
     * @return the ECal channel ID
     */
    @Field(names = {"ecal_channel_id"})
    public Integer getChannelId() {
        return this.getFieldValue("ecal_channel_id");
    }

    /**
     * Get the reference time for the formula (usually about an hour or so 
     * before the first good run that weekend) in ns.  
     *
     * @return the reference time in ns
     */
    @Field(names = {"t0"})
    public Long getT0() {
        return this.getFieldValue("t0");
    }
    
    /**
     * Get the parameter A for the correction, which is of the form
     * <p>
     * E<sub>new</sub> = E<sub>old</sub>*(E<sub>beam</sub>/(A-B*exp(-(timestamp-t0)/C))))
     *
     * @return the parameter A
     */
    @Field(names = {"a"})
    public Double getA() {
        return this.getFieldValue("a");
    }
    
    /**
     * Get the parameter B for the correction, which is of the form
     * <p>
     * E<sub>new</sub> = E<sub>old</sub>*(E<sub>beam</sub>/(A-B*exp(-(timestamp-t0)/C))))
     *
     *
     * @return the parameter B
     */
    @Field(names = {"b"})
    public Double getB() {
        return this.getFieldValue("b");
    }
    
    /**
     * Get the parameter C for the correction, which is of the form
     * <p>
     * E<sub>new</sub> = E<sub>old</sub>*(E<sub>beam</sub>/(A-B*exp(-(timestamp-t0)/C))))
     *
     * @return the parameter C  
     */
    @Field(names = {"c"})
    public Double getC() {
        return this.getFieldValue("c");
    }
    
    
    
}
