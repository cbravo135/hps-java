package org.hps.analysis.trigger;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.util.Pair;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.lcio.LCIOReader;

public class LCIOOutputCompare {
    public static void main(String[] args) throws IOException, NumberFormatException, ConditionsNotFoundException {
        // Load the conditions database.
        DatabaseConditionsManager conditions = new DatabaseConditionsManager();
        conditions.setRun(Integer.parseInt(args[2]));
        conditions.setDetector(args[3], Integer.parseInt(args[2]));
        
        // Open the data files.
        File oldFile = new File(args[0]);
        File newFile = new File(args[1]);
        LCIOReader oldReader = new LCIOReader(oldFile);
        LCIOReader newReader = new LCIOReader(newFile);
        
        // Loop over event continuously. Eventually, and EOFException
        // will occur, indicating that one (or both) files have ended
        // and the process can terminate. Unfortunately, there is no
        // means of obtaining the actual file length a priori without
        // looping through the entire thing twice.
        int curEvent = 0;
        while(true) {
            System.out.println("\n\nComparing event " + curEvent + "...");
            
            // Get the next event from each file.
            EventHeader oldEvent = null;
            EventHeader newEvent = null;
            
            // Attempt to read an event from each file.
            try {
                oldEvent = oldReader.read();
                newEvent = newReader.read();
            }
            catch(EOFException e) {
                System.out.println("\n\nEnd of file at event " + curEvent + ".");
                break;
            }
            
            compareCollections(oldEvent, newEvent);
            
            // Increment the event count.
            curEvent++;
        }
        
        
        oldReader.close();
        newReader.close();
    }
    
    /*
     * ==================================================================================
     * ==== Main object collection comparison methods ===================================
     * ==================================================================================
     */
    
    @SuppressWarnings("unchecked")
    private static final void compareCollections(EventHeader oldEvent, EventHeader newEvent) {
        // Get the list of collections from each event. For some
        // reason, LCSim does not type the lists, so no type can be
        // applied here.
        @SuppressWarnings("rawtypes")
        Set<List> oldCollections = oldEvent.getLists();
        @SuppressWarnings("rawtypes")
        Set<List> newCollections = newEvent.getLists();
        
        // Get the corresponding collections from each event. To be
        // compared, both events must have the collection.
        Map<String, Pair<List<?>, LCMetaData>> oldCollectionMap = new HashMap<String, Pair<List<?>, LCMetaData>>(oldCollections.size());
        Map<String, Pair<List<?>, LCMetaData>> newCollectionMap = new HashMap<String, Pair<List<?>, LCMetaData>>(newCollections.size());
        for(List<?> collection : oldCollections) {
            LCMetaData metaData = oldEvent.getMetaData(collection);
            Pair<List<?>, LCMetaData> collectionPair = new Pair<List<?>, LCMetaData>(collection, metaData);
            oldCollectionMap.put(metaData.getName(), collectionPair);
        }
        for(List<?> collection : newCollections) {
            LCMetaData metaData = newEvent.getMetaData(collection);
            Pair<List<?>, LCMetaData> collectionPair = new Pair<List<?>, LCMetaData>(collection, metaData);
            newCollectionMap.put(metaData.getName(), collectionPair);
        }
        
        // Make a set of all collections across both events.
        Set<String> collectionNames = new HashSet<String>();
        collectionNames.addAll(oldCollectionMap.keySet());
        collectionNames.addAll(newCollectionMap.keySet());
        
        // Iterate over the extant collections, and compare them if
        // possible. Otherwise, explain why the collection was not
        // compared.
        collectionLoop:
        for(String collectionName : collectionNames) {
            // Get the collections from both events.
            System.out.println("\tAccessing collection \"" + collectionName + "\"...");
            Pair<List<?>, LCMetaData> oldCollection = oldCollectionMap.get(collectionName);
            Pair<List<?>, LCMetaData> newCollection = newCollectionMap.get(collectionName);
            
            // If either event is missing the collection, it is not
            // possible to perform a comparison.
            if(oldCollection == null || newCollection == null) {
                System.out.println(BashParameter.format("\t\t[ SKIP ] :: Collection is missing from one event.\n", BashParameter.PROPERTY_DIM));
                continue collectionLoop;
            }
            
            // Check that the necessary comparison methods exist for
            // the object type stored in this collection.
            try {
                LCIOOutputCompare.class.getDeclaredMethod("compare", oldCollection.getSecondElement().getType(), oldCollection.getSecondElement().getType());
            } catch(NoSuchMethodException | SecurityException e) {
                System.out.println(BashParameter.format("\t\t[ SKIP ] :: Error accessing object comparison method for object type \""
                        + oldCollection.getSecondElement().getType().getSimpleName() + "\".\n", BashParameter.PROPERTY_DIM));
                continue collectionLoop;
            }
            try {
                LCIOOutputCompare.class.getDeclaredMethod("toString", oldCollection.getSecondElement().getType());
            } catch(NoSuchMethodException | SecurityException e) {
                System.out.println(BashParameter.format("\t\t[ SKIP ] :: Error accessing to-string method for object type \""
                        + oldCollection.getSecondElement().getType().getSimpleName() + "\".\n", BashParameter.PROPERTY_DIM));
                continue collectionLoop;
            }
            
            // If both methods exist, then the object collections may
            // be compared. Run the comparison methods.
            boolean passed = compareCollections(oldCollection.getFirstElement(), newCollection.getFirstElement(), oldCollection.getSecondElement().getType());
            System.out.println("\tCollection Status :: [ "
                    + (passed ? BashParameter.format("PASS", BashParameter.TEXT_GREEN) : BashParameter.format("FAIL", BashParameter.TEXT_RED)) + " ]");
            System.out.println();
        }
    }
    
    private static final <T> boolean compareCollections(Collection<?> oldData, Collection<?> newData, Class<T> dataType) {
        // Get the comparison method.
        Method compareMethod = null;
        Method toStringMethod = null;
        try {
            compareMethod = LCIOOutputCompare.class.getDeclaredMethod("compare", dataType, dataType);
            toStringMethod = LCIOOutputCompare.class.getDeclaredMethod("toString", dataType);
        } catch(Exception e) {
            throw new RuntimeException("No valid comparison method found for data type \"" + dataType.getSimpleName() + "\".");
        }
        
        // Compare the argument collections.
        boolean passed = true;
        Set<String> failedParameters = new HashSet<String>();
        try {
            System.out.println("\t\tComparing old objects...");
            Pair<Boolean, Set<String>> oldResults = compare(oldData, newData, compareMethod, toStringMethod);
            passed = passed && oldResults.getFirstElement().booleanValue();
            
            System.out.println("\t\tComparing new hits...");
            Pair<Boolean, Set<String>> newResults = compare(newData, oldData, compareMethod, toStringMethod);
            passed = passed && newResults.getFirstElement().booleanValue();
            
            failedParameters.addAll(oldResults.getSecondElement());
            failedParameters.addAll(newResults.getSecondElement());
        } catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e.getClass().getSimpleName() + " :: Error accessing methods for object type \"" + dataType.getSimpleName() + "\".");
        }
        
        // Output any parameters that failed to compare.
        if(!failedParameters.isEmpty()) {
            System.out.println("\t\tFailed Parameters:");
            for(String parameter : failedParameters) {
                System.out.println("\t\t\t" + parameter);
            }
        }
        
        // Return whether both list comparisons passed.
        return passed;
    }
    
    private static final <T> Pair<Boolean, Set<String>> compare(Collection<?> objs0, Collection<?> objs1, Method compareMethod, Method toStringMethod)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        boolean passed = true;
        Set<String> failedParameters = new HashSet<String>();
        for(Object obj0 : objs0) {
            System.out.print("\t\t\t" + ((String) toStringMethod.invoke(null, obj0)) + " ... ");
            boolean matched = false;
            newHitLoop:
            for(Object obj1 : objs1) {
                @SuppressWarnings("unchecked")
                Pair<Boolean, Set<String>> compareResults = (Pair<Boolean, Set<String>>) compareMethod.invoke(null, obj0, obj1);
                failedParameters.addAll(compareResults.getSecondElement());
                if(compareResults.getFirstElement().booleanValue()) {
                    matched = true;
                    System.out.println("[ " + BashParameter.format("PASS", BashParameter.TEXT_GREEN) + " ]");
                    break newHitLoop;
                }
            }
            
            if(!matched) {
                passed = false;
                System.out.println("[ " + BashParameter.format("FAIL", BashParameter.TEXT_RED) + " ]");
            }
        }
        
        return new Pair<Boolean, Set<String>>(Boolean.valueOf(passed), failedParameters);
    }
    
    /*
     * ==================================================================================
     * ==== Object Comparison Methods ===================================================
     * ==================================================================================
     */
    
    @SuppressWarnings("unused")
    private static final Pair<Boolean, Set<String>> compare(CalorimeterHit hit0, CalorimeterHit hit1) {
        boolean pass = true;
        Set<String> fails = new HashSet<String>();
        
        if(hit0.getCellID() != hit1.getCellID()) {
            return new Pair<Boolean, Set<String>>(Boolean.valueOf(false), fails);
        }
        
        if(hit0.getCorrectedEnergy() != hit1.getCorrectedEnergy()) {
            pass = false;
            System.out.print("Fail Corr. Energy");
        }
        
        if(hit0.getRawEnergy() != hit1.getRawEnergy()) {
            pass = false;
            System.out.print("Fail Raw Energy");
        }
        
        if((hit0.getTime() > (hit1.getTime() + 1.5)) || (hit0.getTime() < (hit1.getTime() - 1.5))) {
            pass = false;
            System.out.print("Fail Time");
        }
        
        for(int i = 0; i < 3; i++) {
            if(hit0.getPosition()[i] != hit1.getPosition()[i]) {
                pass = false;
                System.out.print("Fail Position");
                break;
            }
        }
        
        if(hit0.getSubdetector().getName().compareTo(hit1.getSubdetector().getName()) != 0) { pass = false; System.out.print("Fail Detector"); }
        
        return new Pair<Boolean, Set<String>>(Boolean.valueOf(pass), fails);
    }
    
    @SuppressWarnings("unused")
    private static final Pair<Boolean, Set<String>> compare(Cluster cluster0, Cluster cluster1) {
        boolean pass = true;
        if(cluster0.getCalorimeterHits().size() != cluster1.getCalorimeterHits().size()) {
            pass = false;
        }
        
        CalorimeterHit seed0 = cluster0.getCalorimeterHits().get(0);
        CalorimeterHit seed1 = cluster1.getCalorimeterHits().get(0);
        if(seed0.getCellID() != seed1.getCellID()) { pass = false; }
        
        if((seed0.getTime() > (seed1.getTime() + 1.5)) || (seed0.getTime() < (seed1.getTime() - 1.5))) {
            pass = false;
            System.out.print("Fail Time");
        }
        
        if(cluster0.getEnergy() != cluster1.getEnergy()) { pass = false; }
        
        for(int i = 0; i < 3; i++) {
            if(cluster0.getPosition()[i] != cluster1.getPosition()[i]) {
                pass = false;
                break;
            }
        }
        
        return new Pair<Boolean, Set<String>>(Boolean.valueOf(pass), new HashSet<String>(0));
    }
    
    @SuppressWarnings("unused")
    private static final Pair<Boolean, Set<String>> compare(RawTrackerHit hit0, RawTrackerHit hit1) {
        boolean pass = true;
        Set<String> fails = new HashSet<String>();
        
        if(hit0.getTime() != hit1.getTime()) { pass = false; }
        
        if(hit0.getCellID() != hit1.getCellID()) { pass = false; }
        
        if(hit0.getADCValues().length != hit1.getADCValues().length) { pass = false; }
        
        for(int i = 0; i < hit0.getADCValues().length; i++) {
            if(hit0.getADCValues()[i] != hit1.getADCValues()[i]) {
                pass = false;
                break;
            }
        }
        
        for(int i = 0; i < 3; i++) {
            if(hit0.getPosition()[i] != hit1.getPosition()[i]) {
                pass = false;
                break;
            }
        }
        
        try {
            if(hit0.getBarrelEndcapFlag() != hit1.getBarrelEndcapFlag()) { pass = false; }
        }
        catch(Exception e) { fails.add("Barrel Endcap Flag"); }
        
        try {
            if(hit0.getLayerNumber() != hit1.getLayerNumber()) { pass = false; }
        }
        catch(Exception e) { fails.add("Layer Number"); }
        
        try {
            if(hit0.getSystemId() != hit1.getSystemId()) { pass = false; }
        }
        catch(Exception e) { fails.add("System ID"); }
        
        try {
            if(hit0.getSubdetector().getName().compareTo(hit1.getSubdetector().getName()) != 0) { pass = false; }
        }
        catch(Exception e) { fails.add("Subdetector"); }
        
        return new Pair<Boolean, Set<String>>(Boolean.valueOf(pass), fails);
    }
    
    /*
     * ==================================================================================
     * ==== Object To-String Methods ====================================================
     * ==================================================================================
     */
    
    @SuppressWarnings("unused")
    private static final String toString(CalorimeterHit hit) {
        return String.format("CalorimeterHit(id=%-8d | E=%7.5f | t=%7.2f | p=<%6.1f, %6.1f, %6.1f>)", hit.getCellID(), hit.getCorrectedEnergy(),
                hit.getTime(), hit.getPosition()[0], hit.getPosition()[1], hit.getPosition()[2]);
    }
    
    @SuppressWarnings("unused")
    private static final String toString(Cluster cluster) {
        return String.format("Cluster(id=%-8d | E=%7.5f | t=%7.2f | p=<%6.1f, %6.1f, %6.1f>)", cluster.getCalorimeterHits().get(0).getCellID(), cluster.getEnergy(),
                cluster.getCalorimeterHits().get(0).getTime(), cluster.getPosition()[0], cluster.getPosition()[1], cluster.getPosition()[2]);
    }
    
    @SuppressWarnings("unused")
    private static final String toString(RawTrackerHit hit) {
        return String.format("RawTrackerHit(id=%-14d | t=%-2d | l=%d)", hit.getCellID(), hit.getTime(), hit.getADCValues().length);
    }
    
    /*
     * ==================================================================================
     * ==== Utility Methods =============================================================
     * ==================================================================================
     */
    
    public enum BashParameter {
        DEFAULT(0, true, true, true), PROPERTY_BOLD(1, true, false, false), PROPERTY_DIM(2, true, false, false), PROPERTY_UNDERLINE(4, true, false, false),
        PROPERTY_BLINK(5, true, false, false), PROPERTY_REVERSE(7, true, false, false), PROPERTY_HIDDEN(8, true, false, false),
        
        TEXT_BLACK(30, false, true, false), TEXT_RED(31, false, true, false), TEXT_GREEN(32, false, true, false), TEXT_YELLOW(33, false, true, false),
        TEXT_BLUE(34, false, true, false), TEXT_MAGENTA(35, false, true, false), TEXT_CYAN(36, false, true, false), TEXT_LIGHT_GREY(37, false, true, false),
        TEXT_DARK_GREY(90, false, true, false), TEXT_LIGHT_RED(91, false, true, false), TEXT_LIGHT_GREEN(92, false, true, false),
        TEXT_LIGHT_YELLOW(93, false, true, false), TEXT_LIGHT_BLUE(94, false, true, false), TEXT_LIGHT_MAGENTA(95, false, true, false),
        TEXT_LIGHT_CYAN(96, false, true, false), TEXT_LIGHT_WHITE(97, false, true, false),
        
        BACKGROUND_BLACK(40, false, false, true), BACKGROUND_RED(41, false, false, true), BACKGROUND_GREEN(42, false, false, true),
        BACKGROUND_YELLOW(43, false, false, true), BACKGROUND_BLUE(44, false, false, true), BACKGROUND_MAGENTA(45, false, false, true),
        BACKGROUND_CYAN(46, false, false, true), BACKGROUND_LIGHT_GREY(47, false, false, true), BACKGROUND_DARK_GREY(100, false, false, true),
        BACKGROUND_LIGHT_RED(101, false, false, true), BACKGROUND_LIGHT_GREEN(102, false, false, true), BACKGROUND_LIGHT_YELLOW(103, false, false, true),
        BACKGROUND_LIGHT_BLUE(104, false, false, true), BACKGROUND_LIGHT_MAGENTA(105, false, false, true), BACKGROUND_LIGHT_CYAN(106, false, false, true),
        BACKGROUND_WHITE(107, false, false, true);
        
        private final int code;
        private final boolean isProperty;
        private final boolean isTextColor;
        private final boolean isBackgroundColor;
        
        private BashParameter(int code, boolean isProperty, boolean isTextColor, boolean isBackgroundColor) {
            this.code = code;
            this.isProperty = isProperty;
            this.isTextColor = isTextColor;
            this.isBackgroundColor = isBackgroundColor;
        }
        
        public int getCode() {
            return code;
        }
        
        public String getTextCode() {
            return ((char) 27) + "[" + Integer.toString(getCode()) + "m";
        }
        
        public boolean isProperty() {
            return isProperty;
        }
        
        public boolean isTextColor() {
            return isTextColor;
        }
        
        public boolean isBackgroundColor() {
            return isBackgroundColor;
        }
        
        public static final String format(String text, BashParameter... params) {
            // Only one text and background color, at most, are
            // allowed. Track whether more than one is seen.
            boolean sawTextColor = false;
            boolean sawBackgroundColor = false;
            
            // Buffer the formatted text.
            StringBuffer buffer = new StringBuffer();
            
            for(BashParameter param : params) {
                // If the default parameter is declared, it must be
                // the only parameters defined, since it overrides
                // all other parameters.
                if(param == BashParameter.DEFAULT && params.length != 1) {
                    throw new IllegalArgumentException("Error: The default style parameter can not be employed in conjunction with other style parameters.");
                }
                
                // Only one text color may be defined at a time, as
                // subsequent colors will override the earlier ones.
                if(param.isTextColor()) {
                    if(sawTextColor) {
                        throw new IllegalArgumentException("Error: Only one text color may be defined.");
                    } else {
                        sawTextColor = true;
                    }
                }
                
                // Only one background color may be defined at a
                // time, as subsequent colors will override the
                // earlier ones.
                if(param.isBackgroundColor()) {
                    if(sawBackgroundColor) {
                        throw new IllegalArgumentException("Error: Only one background color may be defined.");
                    } else {
                        sawBackgroundColor = true;
                    }
                }
                
                // Append the full bash text style code to the
                // buffer.
                buffer.append(param.getTextCode());
            }
            
            // Add the text to the buffer, and then reset the style
            // to the default.
            buffer.append(text);
            buffer.append(BashParameter.DEFAULT.getTextCode());
            
            // Return the text.
            return buffer.toString();
        }
    }
}