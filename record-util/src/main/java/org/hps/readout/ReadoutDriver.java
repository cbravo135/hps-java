package org.hps.readout;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hps.readout.util.LcsimSingleEventCollectionData;
import org.lcsim.util.Driver;

/**
 * Class <code>ReadoutDriver</code> is an abstract framework for
 * defining a driver that functions with the HPS readout simulation.
 * A readout driver is responsible for interfacing with the {@link
 * org.hps.readout.ReadoutDataManager ReadoutDataManager} for both
 * the purpose of creating data output collections, and accessing
 * input data collections. It is also responsible for declaring its
 * own time offsets and collection dependencies. To this end, it must
 * perform several tasks:<br/>
 * <ul>
 * <li><b>Declare itself:</b> A readout driver must declare itself to
 * the <code>ReadoutDataManager</code>. This is done by default via
 * the default constructor, but if this is overridden, it then the
 * superclass constructor must be called, or the driver must be
 * manually declared via the method {@link
 * org.hps.readout.ReadoutDataManager#registerReadoutDriver(ReadoutDriver)
 * ReadoutDataManager.registerReadoutDriver(ReadoutDriver)}. Note
 * that if the driver is a trigger driver, it must instead use the
 * method {@link
 * org.hps.readout.ReadoutDataManager#registerTrigger(ReadoutDriver)
 * ReadoutDataManager.registerTrigger(ReadoutDriver)} instead.</li>
 * <li><b>Declare input collections:</b> All readout drivers must
 * specify which data collections they require as input. This is used
 * to correctly calculate the total time displacement of a driver by
 * the <code>ReadoutDataManager</code> and is accomplished by calling
 * {@link org.hps.readout.ReadoutDriver#addDependency(String)
 * addDependency(String)} for each input collection. This should be
 * done in the {@link org.lcsim.util.Driver#startOfData()
 * startOfData()} method.</li>
 * <li><b>Declare time displacement:</b> Readout drivers must specify
 * the amount of simulation time (events) that they need to process
 * their input data before they can produce output from it. For
 * instance, a driver that needs to buffer 16 ns into the "future" to
 * make its decisions would need to specify an offset of 16 ns.</li>
 * <li><b>Declare output collections:</b> All readout drivers must
 * also declare what collections they produce. This is performed by
 * the method {@link
 * org.hps.readout.ReadoutDataManager#registerCollection(LcsimCollection)
 * ReadoutDataManager.registerCollection(LcsimCollection)} and must
 * be done after all input collections have been declared and the
 * driver time offset calculated. This is also generally performed
 * in the <code>startOfData()</code> method.</li>
 * <li><b>Produce on-trigger data:</b> Some drivers may need to
 * produce data to write into triggered readout files that is not or
 * can not be stored in the <code>ReadoutDataManager</code> itself.
 * In this case, each driver is allowed to produce its own special
 * collections data via the method {@link
 * org.hps.readout.ReadoutDriver#getOnTriggerData(double)
 * getOnTriggerData(double)}, where the argument is the true trigger
 * time (i.e. the simulation time of the trigger event with a time
 * correction applied based on the time displacements of the driver
 * chain leading up to it). Drivers that require this functionality
 * may override the aforementioned method. This method is only called
 * on a trigger write-out event, and by default will return a value
 * of <code>null</code>. If a driver does not require this
 * functionality, the method may be ignored.</li>
 * <li><b>Declare special data time displacement:</b> Drivers must
 * also declare the total amount of time that they need to produce
 * their local on-trigger data. If no on-trigger data is produced,
 * this should be set to 0 ns.</li>
 * </ul>
 * The goal of a readout driver is to be able to obtain needed input
 * data from any collection, regardless of the relative time offsets
 * of those collections, and to output its own collections, without
 * needing to know any information about how the input collections
 * are generated. A driver should only need to know the name of the
 * input collection. This is accomplished by using the above declared
 * information in the <code>ReadoutDataManager</code> to handle time
 * offset alignment between data sets automatically.<br/><br/>
 * By knowing the what collections a driver needs, and how long it
 * takes to produce its output, the data manager can determine what
 * the "true" data time for each output collection from each driver
 * is. It then allows drivers to request data from within a given
 * "true" time range via the method {@link
 * org.hps.readout.ReadoutDataManager#getData(double, double, String, Class)
 * ReadoutDataManager.getData(double, double, String, Class)} without
 * that driver needing to manually sync itself with its dependencies.
 * It is this method that should then be used to obtain data, rather
 * than pulling it from the event stream directly.</br><br/>
 * Data is output similarly to the data manager directly. This is
 * done via the method {@link
 * org.hps.readout.ReadoutDataManager#addData(String, Collection, Class)
 * ReadoutDataManager.addData(String, Collection, Class)}. The needed
 * time correction is then automatically applied.<br/><br/>
 * A driver should make sure that its input collection is populated
 * to the needed time before requesting the data. This can be done
 * through the method {@link
 * org.hps.readout.ReadoutDataManager#checkCollectionStatus(String, double)
 * ReadoutDataManager.checkCollectionStatus(String, double)} for the
 * appropriate collection.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public abstract class ReadoutDriver extends Driver {
	/**
	 * Stores the names of the collections which this driver requires
	 * as input.
	 */
	private final Set<String> dependencies = new HashSet<String>();
	
	// DEBUG
	protected boolean debug = true;
	protected java.util.List<TempOutputWriter> writers = new java.util.ArrayList<TempOutputWriter>();
	
	/**
	 * Instantiates the readout driver.
	 */
	protected ReadoutDriver() {
		ReadoutDataManager.registerReadoutDriver(this);
	}
	
	@Override
	public void endOfData() {
		for(TempOutputWriter writer : writers) {
			if(debug && writer != null) { writer.close(); }
		}
	}
	
	@Override
	public void startOfData() {
		// Set the debug status for each writer. If the writer should
		// output anything, tell it to delete the file on exit.
		if(debug) {
			for(TempOutputWriter writer : writers) {
				System.out.println(writer.toString());
				writer.initialize();
				writer.setEnabled(debug);
			}
		}
	}
	
	/**
	 * Specifies that the output of this readout driver depends on
	 * the specified input collection.
	 * @param collectionName - The name of the input collection.
	 */
	protected void addDependency(String collectionName) {
		dependencies.add(collectionName);
	}
	
	/**
	 * Returns a {@link java.util.Collection Collection} of type
	 * {@link java.lang.String String} containing the names of the
	 * input collections used by this driver.
	 * @return Returns a collection of <code>String</code> objects
	 * representing the driver input collection names.
	 */
	protected Collection<String> getDependencies() {
		return dependencies;
	}
	
	/**
	 * Generates a {@link java.util.Collection Collection} containing
	 * any special output data produced by the driver that should be
	 * included in triggered output.<br/><br/>
	 * By default, this outputs <code>null</code>. Individual drivers
	 * must override the method as needed.
	 * @param triggerTime - The time at which the trigger occurred.
	 * @return Returns a collection containing all special output
	 * data.
	 */
	protected Collection<LcsimSingleEventCollectionData<?>> getOnTriggerData(double triggerTime) {
		return null;
	}
	
	/**
	 * Specifies the amount of simulation time that the driver needs
	 * to produce its output. This indicates that the driver's
	 * present output was generated based on input a time equal to
	 * ({@link org.hps.readout.ReadoutDataManager#getCurrentTime()
	 * getCurrentTime()} - <code>getTimeDisplacement()</code>).
	 * @return Returns the time displacement of output data as a
	 * <code>double</code>.
	 */
	protected abstract double getTimeDisplacement();
	
	/**
	 * Specifies the amount of simulation time that the driver needs
	 * to produce special on-trigger data.
	 * @return Returns the time displacement of output data as a
	 * <code>double</code>.
	 */
	protected abstract double getTimeNeededForLocalOutput();
	
	public void setDebug(boolean state) {
		debug = state;
	}
}