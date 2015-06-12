package org.hps.conditions.api;

import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.hps.conditions.database.ConditionsRecordConverter;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

/**
 * This class represents a single record from the primary conditions data table, which defines the validity range for a
 * specific collection of conditions objects.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@Table(names = {"conditions"})
@Converter(converter = ConditionsRecordConverter.class)
public final class ConditionsRecord extends BaseConditionsObject {

    /**
     * The concrete collection implementation, including sorting utilities.
     */
    @SuppressWarnings("serial")
    public static class ConditionsRecordCollection extends BaseConditionsObjectCollection<ConditionsRecord> {

        @Override
        public boolean add(final ConditionsRecord object) throws ConditionsObjectException {
            if (object == null) {
                throw new IllegalArgumentException("The object argument is null.");
            }
            final boolean added = getObjects().add(object);
            if (!added) {
                throw new RuntimeException("Failed to add object.");
            }
            return added;
        }

        /**
         * Compare conditions records by creation date.
         */
        private static class CreatedComparator implements Comparator<ConditionsRecord> {
            /**
             * Compare the creation dates of two conditions records.
             *
             * @param c1 The first conditions record.
             * @param c2 The second conditions record.
             * @return -1, 0, or 1 if first date is less than, equal to, or greater than the second date.
             */
            @Override
            public int compare(final ConditionsRecord c1, final ConditionsRecord c2) {
                final Date date1 = c1.getCreated();
                final Date date2 = c2.getCreated();
                if (date1.before(date2)) {
                    return -1;
                } else if (date1.after(date2)) {
                    return 1;
                }
                return 0;
            }
        }

        /**
         * Compare conditions records by their key (table name).
         */
        private static class KeyComparator implements Comparator<ConditionsRecord> {
            /**
             * Compare the keys (names) of two conditions records.
             *
             * @param c1 The first conditions record.
             * @param c2 The second conditions record.
             * @return -1, 0, or 1 if first name is less than, equal to, or greater than the second (using alphabetic
             *         comparison).
             */
            @Override
            public int compare(final ConditionsRecord c1, final ConditionsRecord c2) {
                return c1.getName().compareTo(c2.getName());
            }
        }

        /**
         * Compare conditions records by run start.
         */
        private static class RunStartComparator implements Comparator<ConditionsRecord> {
            /**
             * Compare the run start numbers of two conditions records.
             *
             * @param c1 The first conditions record.
             * @param c2 The second conditions record.
             * @return -1, 0, or 1 if first run number is less than, equal to, or greater than the second.
             */
            @Override
            public int compare(final ConditionsRecord c1, final ConditionsRecord c2) {
                if (c1.getRunStart() < c2.getRunStart()) {
                    return -1;
                } else if (c1.getRunStart() > c2.getRunStart()) {
                    return 1;
                }
                return 0;
            }
        }

        /**
         * Compare conditions records by updated date.
         */
        private static class UpdatedComparator implements Comparator<ConditionsRecord> {
            /**
             * Compare the updated dates of two conditions records.
             *
             * @param c1 The first conditions record.
             * @param c2 The second conditions record.
             * @return -1, 0, or 1 if first date is less than, equal to, or greater than the second date.
             */
            @Override
            public int compare(final ConditionsRecord c1, final ConditionsRecord c2) {
                final Date date1 = c1.getUpdated();
                final Date date2 = c2.getUpdated();
                if (date1.before(date2)) {
                    return -1;
                } else if (date1.after(date2)) {
                    return 1;
                }
                return 0;
            }
        }

        /**
         * Get the unique conditions keys from the records in this collection.
         *
         * @return the set of unique conditions keys
         */
        public final Set<String> getConditionsKeys() {
            final Set<String> conditionsKeys = new HashSet<String>();
            for (final ConditionsRecord record : this) {
                conditionsKeys.add(record.getName());
            }
            return conditionsKeys;
        }

        /**
         * Sort in place by creation date.
         */
        public final void sortByCreated() {
            sort(new CreatedComparator());
        }

        /**
         * Sort in place by key.
         */
        public final void sortByKey() {
            sort(new KeyComparator());
        }

        /**
         * Sort in place by run start.
         */
        public final void sortByRunStart() {
            sort(new RunStartComparator());
        }

        /**
         * Sort in place by updated date.
         */
        public final void sortByUpdated() {
            this.sort(new UpdatedComparator());
        }

        /**
         * Sort and return collection by creation date.
         *
         * @return the sorted collection
         */
        public final ConditionsRecordCollection sortedByCreated() {
            return (ConditionsRecordCollection) sorted(new CreatedComparator());
        }

        /**
         * Sort and return by key (table name).
         *
         * @return the sorted collection
         */
        public final ConditionsRecordCollection sortedByKey() {
            return (ConditionsRecordCollection) sorted(new KeyComparator());
        }

        /**
         * Sort and return by run start number.
         *
         * @return the sorted collection
         */
        public final ConditionsRecordCollection sortedByRunStart() {
            return (ConditionsRecordCollection) sorted(new RunStartComparator());
        }

        /**
         * Sort and return collection by updated date.
         *
         * @return the sorted collection
         */
        public final ConditionsRecordCollection sortedByUpdated() {
            return (ConditionsRecordCollection) sorted(new UpdatedComparator());
        }

        /**
         * Find a sub-set of the records with matching key name.
         */
        public ConditionsRecordCollection findByKey(String key) {
            ConditionsRecordCollection collection = new ConditionsRecordCollection();
            for (ConditionsRecord record : this) {
                if (record.getName().equals(key)) {
                    try {
                        collection.add(record);
                    } catch (ConditionsObjectException e) {
                        throw new RuntimeException("Error adding record to new collection.", e);
                    }
                }
            }
            return collection;
        }
    }

    /**
     * Create a "blank" conditions record.
     */
    public ConditionsRecord() {
    }

    /**
     * Copy constructor.
     *
     * @param record the <code>ConditionsRecord</code> to copy
     */
    public ConditionsRecord(final ConditionsRecord record) {
        this.setFieldValue("collection_id", record.getCollectionId());
        this.setFieldValue("run_start", record.getRunStart());
        this.setFieldValue("run_end", record.getRunEnd());
        this.setFieldValue("name", record.getName());
        this.setFieldValue("table_name", record.getTableName());
        this.setFieldValue("notes", record.getNotes());
        this.setFieldValue("tag", record.getTag());
        this.setFieldValue("created", record.getCreated());
        this.setFieldValue("created_by", record.getCreatedBy());
    }

    /**
     * Create a conditions record with fully qualified constructor.
     *
     * @param collectionId the ID of the associated conditions collection
     * @param runStart the starting run number
     * @param runEnd the ending run number
     * @param name the name of the conditions set (usually same as table name)
     * @param tableName the name of the conditions data table
     * @param notes text notes about this record
     * @param tag the conditions tag for grouping this record with others
     */
    public ConditionsRecord(final int collectionId, final int runStart, final int runEnd, final String name,
            final String tableName, final String notes, final String tag) {
        this.setFieldValue("collection_id", collectionId);
        this.setFieldValue("run_start", runStart);
        this.setFieldValue("run_end", runEnd);
        this.setFieldValue("name", name);
        this.setFieldValue("table_name", tableName);
        this.setFieldValue("notes", notes);
        this.setFieldValue("tag", tag);
        this.setFieldValue("created", new Date());
        this.setFieldValue("created_by", System.getProperty("user.name"));
    }

    /**
     * Get the collection ID, overriding this method from the parent class.
     *
     * @return the collection ID
     */
    @Field(names = {"collection_id"})
    public Integer getCollectionId() {
        return getFieldValue("collection_id");
    }

    /**
     * Get the date this record was created.
     *
     * @return the date this record was created
     */
    @Field(names = {"created"})
    public Date getCreated() {
        return getFieldValue("created");
    }

    /**
     * Get the name of the user who created this record.
     *
     * @return the name of the person who created the record
     */
    @Field(names = {"created_by"})
    public String getCreatedBy() {
        return getFieldValue("created_by");
    }

    /**
     * Get the name of these conditions. This is called the "key" in the table meta data to distinguish it from
     * "table name" but it is usually the same value.
     *
     * @return the name of the conditions
     */
    @Field(names = {"name"})
    public String getName() {
        return getFieldValue("name");
    }

    /**
     * Get the notes.
     *
     * @return the notes about this condition
     */
    @Field(names = {"notes"})
    public String getNotes() {
        return getFieldValue("notes");
    }

    /**
     * Get the ending run number.
     *
     * @return the ending run number
     */
    @Field(names = {"run_end"})
    public Integer getRunEnd() {
        return getFieldValue("run_end");
    }

    /**
     * Get the starting run number.
     *
     * @return the starting run number
     */
    @Field(names = {"run_start"})
    public Integer getRunStart() {
        return getFieldValue("run_start");
    }

    /**
     * Get the name of the table containing the actual raw conditions data.
     *
     * @return the name of the table with the conditions data
     */
    @Field(names = {"table_name"})
    public String getTableName() {
        return getFieldValue("table_name");
    }

    /**
     * Get the string tag associated with these conditions.
     *
     * @return The string tag.
     */
    @Field(names = {"tag"})
    public String getTag() {
        return getFieldValue("tag");
    }

    /**
     * Get the date this record was last updated.
     *
     * @return the date this record was updated
     */
    @Field(names = {"updated"})
    public Date getUpdated() {
        return getFieldValue("updated");
    }

    /**
     * Convert this record to a human readable string, one field per line.
     *
     * @return this object represented as a string
     */
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("id: " + getRowId() + '\n');
        sb.append("name: " + getName() + '\n');
        sb.append("runStart: " + getRunStart() + '\n');
        sb.append("runEnd: " + getRunEnd() + '\n');
        sb.append("tableName: " + getTableName() + '\n');
        sb.append("collectionId: " + getCollectionId() + '\n');
        sb.append("updated: " + getUpdated() + '\n');
        sb.append("created: " + getCreated() + '\n');
        sb.append("tag: " + getTag() + '\n');
        sb.append("createdBy: " + getCreatedBy() + '\n');
        sb.append("notes: " + getNotes() + '\n');
        return sb.toString();
    }
}
