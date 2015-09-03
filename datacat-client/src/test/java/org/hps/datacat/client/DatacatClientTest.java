package org.hps.datacat.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

/**
 * 
 * @author Jeremy McCormick, SLAC
 *
 */
public class DatacatClientTest extends TestCase {
    
    private static final String DATASET_NAME = "dummyDataset";
    private static final String FOLDER = "dummyFolder";
    private static final String RESOURCE = "/path/to/dummyDataset.ds";
    
    public void testDatacat() throws Exception {

        // Datacat client with default parameters.
        DatacatClient client = new DatacatClientImpl();
        
        // Stores response from HTTP operations
        int response = -1;
        
        // Create dummy folder.
        response = client.makeFolder("dummyFolder");
                        
        // Add dummy dataset.
        Map<String, Object> dsMetadata = new HashMap<String, Object>();
        dsMetadata.put("testInt", 1);
        dsMetadata.put("testFloat", 1.1f);
        dsMetadata.put("testDouble", 1.2d);
        dsMetadata.put("testString", "herpderp");
        response = client.addDataset(FOLDER, DatasetDataType.TEST, RESOURCE, DatasetSite.SLAC, DatasetFileFormat.TEST, DATASET_NAME, dsMetadata);
        
        // Patch the dataset with some meta data.
        Map<String, Object> metaData = new HashMap<String, Object>();
        metaData.put("testInt2", 1234);
        response = client.addMetadata(FOLDER, DATASET_NAME, metaData);
        
        // TODO: check that folder exists
        
        // TODO: check that dataset exists
        
        // TODO: get the full folder info
                
        // TODO: get the full dataset info
                       
        // Find the dataset with a simple query.
        Set<String> metadataFields = new HashSet<String>();
        metadataFields.add("testInt");
        metadataFields.add("testFloat");        
        metadataFields.add("testDouble");
        metadataFields.add("testString");
        List<Dataset> datasets = client.findDatasets(FOLDER, "testInt == 1", metadataFields);
        for (Dataset dataset : datasets) {
            System.out.println("found dataset: " + dataset.getName());
            System.out.println("metadata: " + dataset.getMetadata());
        }
        
        // Delete the dataset.
        response = client.deleteDataset("/" + FOLDER + "/" + DATASET_NAME);
        System.out.println("deleteDataset: " + response);
        System.out.println();
        
        // Remove the folder.
        client.removeFolder("/" + FOLDER);
        System.out.println("removeFolder: " + response);
        System.out.println();
    }         
}