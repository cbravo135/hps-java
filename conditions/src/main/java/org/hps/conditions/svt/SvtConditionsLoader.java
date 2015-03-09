package org.hps.conditions.svt;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;

import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.TableMetaData;
import org.hps.conditions.svt.SvtConditionsReader;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;

/**
 *  Command line tool used to load SVT conditions into the conditions database.
 * 
 *  @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class SvtConditionsLoader {

    // Initialize the logger
    private static Logger logger = LogUtil.create(SvtConditionsLoader.class.getSimpleName(), 
            new DefaultLogFormatter(), Level.INFO);
    
    //-----------------//
    //--- Constants ---//
    //-----------------//
    
    // Default detector
    public static final String DETECTOR = "HPS-Proposal2014-v9-2pt2";
    
    // Table names
    public static final String DAQ_MAP_TABLE_NAME = "svt_daq_map";

    //-----------------//
    //-----------------//
   
	public static void main(String[] args) {
	
	   // Set up the command line options
	   Options options = setupCommandLineOptions(); 
	   
	   // Parse the command line arguments
	   CommandLineParser parser = new PosixParser();
	   CommandLine commandLine = null;
	   try { 
	       commandLine = parser.parse(options, args);
	   } catch (ParseException e){ 
	       throw new RuntimeException("Unable to parse command line arguments.", e);
	   }
	   
	   // Get the run number.  If a run number hasn't been set, warn the user 
	   // and exit.
	   if (!commandLine.hasOption("r")) { 
	       System.out.println("\nPlease specify a run number to associate with the conditions set.\n");
	       return;
	   }
	   int runNumber = Integer.valueOf(commandLine.getOptionValue("r"));
	   logger.info("Run number set to " + runNumber);
	 
	   //  Initialize the conditions system and load the conditions onto the
	   // detector object
	   try {
	       
	       // If a user has specified the connection properties, set them, 
	       // otherwise use the default values
	       if (commandLine.hasOption("p")) { 
	           DatabaseConditionsManager.getInstance()
	                                    .setConnectionProperties(new File(commandLine.getOptionValue("p")));
	       }
	       DatabaseConditionsManager.getInstance()
	                                .setDetector(SvtConditionsLoader.DETECTOR, runNumber);
	   } catch(ConditionsNotFoundException e) {
	       throw new RuntimeException("Could not initialize the conditions system.", e);
	   }
	   
	   // Instantiate the SVT conditions reader
	   SvtConditionsReader reader; 
	   try { 
	       reader = new SvtConditionsReader(); 
	   } catch (Exception e) {
	      throw new RuntimeException("Couldn't open SvtConditionsReader.", e); 
	   }
	   
	   // If a calibrations file has been specified, parse it and load them 
	   // to the conditions database.
	   if (commandLine.hasOption("c")) { 
	       File inputFile = new File(commandLine.getOptionValue("c"));
	       try { 
	           reader.parseCalibrations(inputFile);
	       } catch (Exception e) { 
	           throw new RuntimeException("Couldn't parse calibration file.", e);
	       }
	   }
	  
	   // If a DAQ map file has been specified, parse it and load them to the
	   // conditions database.
	   if (commandLine.hasOption("d")) { 
	       File daqMapFile = new File(commandLine.getOptionValue("d"));
	       logger.info("Loading DAQ map from file " + daqMapFile.getAbsolutePath());
	       try { 
	          
	           // Parse the DAQ map file
	           SvtDaqMappingCollection daqMapping = reader.parseDaqMap(daqMapFile);
	           
	           // Set the table meta data
	           TableMetaData tableMetaData = DatabaseConditionsManager.getInstance().findTableMetaData(SvtConditionsLoader.DAQ_MAP_TABLE_NAME);
	           daqMapping.setTableMetaData(tableMetaData);
	          
	           // Set the collection ID
	           int collectionID = DatabaseConditionsManager.getInstance().getNextCollectionID(SvtConditionsLoader.DAQ_MAP_TABLE_NAME);
	           daqMapping.setCollectionId(collectionID);
	           logger.info("Using collection ID " + collectionID);
	           
	           // Load the DAQ map
	           daqMapping.insert();
	           logger.info("DAQ map has been loaded successfully");
	           logger.fine(daqMapping.toString());
	           
	           // Create a conditions record associated with the set of 
	           // conditions that were just loaded 
	           ConditionsRecord conditionsRecord = new ConditionsRecord( 
	                   daqMapping.getCollectionId(), 
	                   runNumber, 
	                   99999, 
	                   SvtConditionsLoader.DAQ_MAP_TABLE_NAME,
	                   SvtConditionsLoader.DAQ_MAP_TABLE_NAME,
	                   "Engineering run DAQ map. Loaded using SvtConditionsLoader.",
	                   "eng_run");
	           conditionsRecord.insert();
	           
	       } catch (Exception e) { 
	           throw new RuntimeException("Couldn't parse DAQ map file.", e);
	       }
	   }
	}

	/**
	 * Method used to setup all command line options.
	 * 
	 * @return a set of options
	 */
	private static Options setupCommandLineOptions() { 
	    Options options = new Options(); 
	    options.addOption(new Option("r", true, "Run number"));
	    options.addOption(new Option("p", true, "Path to properties file"));
	    options.addOption(new Option("c", true, "Calibration file"));
	    options.addOption(new Option("d", true, "DAQ map file"));
	    return options;
	}
}
