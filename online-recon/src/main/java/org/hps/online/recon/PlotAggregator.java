package org.hps.online.recon;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.online.recon.properties.Property;

import hep.aida.IAnalysisFactory;
import hep.aida.IBaseHistogram;
import hep.aida.ICloud;
import hep.aida.ICloud1D;
import hep.aida.ICloud2D;
import hep.aida.ICloud3D;
import hep.aida.IDataPointSet;
import hep.aida.IHistogram;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogram3D;
import hep.aida.IManagedObject;
import hep.aida.IProfile;
import hep.aida.IProfile1D;
import hep.aida.IProfile2D;
import hep.aida.ITree;
import hep.aida.ITreeFactory;
import hep.aida.dev.IDevTree;
import hep.aida.ref.remote.RemoteServer;
import hep.aida.ref.remote.rmi.client.RmiStoreFactory;
import hep.aida.ref.remote.rmi.interfaces.RmiServer;
import hep.aida.ref.remote.rmi.server.RmiServerImpl;
import hep.aida.ref.rootwriter.RootFileStore;
import hep.aida.ref.xml.AidaXMLStore;

/**
 * Creates an AIDA tree that is used to combine histograms from multiple
 * online recon {@link Station} instances by connecting to their remote trees
 * and adding histogram objects with the same paths together
 *
 * The resulting combined plots can then be viewed in a remote AIDA client,
 * such as JAS3 with the Remote AIDA plugin or in a browser by connecting to
 * a webapp running the aidatld library.
 *
 * The station trees are mounted into a server tree and added together
 * into a combined set of histograms in a separate directory structure.
 * The remote plots are read-only, so a station's plots can only be reset
 * by restarting it. The remotely mounted trees are automatically
 * unmounted when a station is deactivated (stopped).
 *
 * This class implements <code>Runnable</code> and is designed to be run
 * periodically using a scheduled thread executor by the {@code Server}.
 *
 * The <code>RemoteTreeBindThread</code> is used to connect asynchronously
 * to a station that has been activated for event processing.
 *
 * When saving plots to ROOT, all combined clouds are automatically converted
 * to histograms, as an error will occur otherwise.
 */
public class PlotAggregator implements Runnable {

    /** Package logger */
    private static Logger LOG = Logger.getLogger(PlotAggregator.class.getPackage().getName());

    /** Directory where remote AIDA trees are mounted */
    private static final String REMOTES_DIR = "/remotes";

    /** Directory where plots are aggregated */
    private static final String COMBINED_DIR = "/combined";

    /** Number of bins to use for converting clouds to histograms */
    private static final int CLOUD_BINS = 100;

    /** Network port for AIDA connection */
    private int port = 3001;

    /** Name of the remote server for RMI */
    private String serverName = "HPSRecon";

    /** Name of the host to bind */
    private String hostName = null;

    /** Interval of plot aggregation in milliseconds */
    private Long updateInterval = 5000L;

    /** URLs of the remote AIDA trees that are currently mounted
     * e.g. <pre>//localhost:4321/MyTree</pre> */
    private Set<String> remotes = new HashSet<String>();

    /**
     * AIDA objects for the primary server tree
     */
    private IAnalysisFactory af = IAnalysisFactory.create();
    private ITreeFactory tf = af.createTreeFactory();
    private IDevTree serverTree = (IDevTree) tf.create();

    /**
     * RMI server objects
     */
    private RemoteServer treeServer;
    private RmiServer rmiTreeServer;

    /**
     * Dummy object used for synchronization
     */
    private final String updating = "updating";

    /**
     * Create an instance of the plot aggregator
     */
    public PlotAggregator() {
    }

    /**
     * Set the network port
     * @param port The network port
     */
    void setPort(int port) {
        this.port = port;
    }

    /**
     * Set the server name
     * @param serverName The server name
     */
    void setServerName(String serverName) {
        this.serverName = serverName;
    }

    /**
     * Set the host name
     * @param hostName The host name
     */
    void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * Set the update interval in millis
     * @param updateInterval The update interval in millis
     */
    void setUpdateInterval(Long updateInterval) {
        if (updateInterval < 1000L) {
            throw new IllegalArgumentException("Update interval must be >= 1 second");
        }
        if (updateInterval > 30000L) {
            throw new IllegalArgumentException("Update interval must be <= 30 seconds");
        }
        this.updateInterval = updateInterval;
    }

    /**
     * Get the update interval
     * @return The update interval
     */
    Long getUpdateInterval() {
        return this.updateInterval;
    }

    /**
     * Open the main AIDA remote tree
     * @throws IOException If there is an error when opening the tree
     */
    synchronized void connect() throws IOException {
        if (this.hostName == null) {
            this.hostName = InetAddress.getLocalHost().getHostName();
        }
        String treeBindName = "//"+this.hostName+":"+port+"/"+serverName;
        LOG.config("Creating server tree: " + treeBindName);
        boolean serverDuplex = true;
        treeServer = new RemoteServer(serverTree, serverDuplex);
        rmiTreeServer = new RmiServerImpl(treeServer, treeBindName);

        serverTree.mkdir(COMBINED_DIR);
        serverTree.mkdir(REMOTES_DIR);
    }

    /**
     * Get the base path for aggregating plots for a remote tree
     * @param remoteName The tree bind name
     * @return The AIDA path for combining plots
     */
    private static String toAggregateName(String remoteName) {
        String[] sp = remoteName.split("/");
        if (sp.length < 3) {
            throw new IllegalArgumentException("Bad remote name: " + remoteName);
        }
        return remoteName.replace(REMOTES_DIR + "/" + sp[2], COMBINED_DIR);
    }

    /**
     * Convert the remote tree bind (URL) of a remote to a valid AIDA directory name
     * @param treeBindName The tree bind name
     * @return The AIDA directory
     */
    static String toMountName(String treeBindName) {
        return REMOTES_DIR + "/" + treeBindName.replace("//", "")
                .replace("/", "_")
                .replace(":", "_")
                .replace(".", "_");
    }

    /**
     * List object names at a path in the server AIDA tree
     * @param path The path in the tree
     * @param recursive Whether objects should be listed recursively
     * @param type Filter by a set of object types (use <code>null</code> for all)
     * @return A list of the full object paths in the tree
     */
    private String[] listObjectNames(String path, boolean recursive, Set<String> types) {
        String[] names = null;
        if (types != null) {
            if (types.size() == 0) {
                throw new IllegalArgumentException("Set of types for filtering is empty");
            }
            List<String> filtNames = new ArrayList<String>();
            String[] objectNames = serverTree.listObjectNames(path, recursive);
            String[] objectTypes = serverTree.listObjectTypes(path, recursive);
            for (int i = 0; i < objectNames.length; i++) {
                if (types.contains(objectTypes[i])) {
                    filtNames.add(objectNames[i]);
                }
            }
            names = filtNames.toArray(new String[] {});
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            serverTree.ls(path, recursive, baos);
            try {
                names = baos.toString("UTF-8").split("\\r?\\n");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                baos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return names;
    }

    /**
     * Clear the aggregated plots in the tree
     *
     * This does nothing to the remote trees that have been mounted,
     * since they are read-only.
     */
    private synchronized void clearTree() {
        LOG.fine("Clearing tree...");
        String[] objectNames = listObjectNames(COMBINED_DIR, true, null);
        for (String name : objectNames) {
            try {
                IManagedObject obj = serverTree.find(name);
                if (obj instanceof IBaseHistogram) {
                    IBaseHistogram hist = (IBaseHistogram) obj;
                    if (hist.entries() > 0) {
                        LOG.finer("Clearing " + hist.title() + " with entries: " + hist.entries());
                        ((IBaseHistogram) obj).reset();
                    }
                } else if (obj instanceof IDataPointSet) {
                    ((IDataPointSet) obj).clear();
                }
            } catch (IllegalArgumentException e) {
            }
        }
        LOG.fine("Done clearing tree");
    }

    /**
     * Update the aggregated plots by adding all the histograms from the remote
     * trees together
     */
    private synchronized void update() {
        LOG.fine("Plot aggregator is updating...");
        try {
            String[] dirs = this.listObjectNames(REMOTES_DIR, false, null);

            // Add src to target objects
            for (String dir : dirs) {
                String[] remoteObjects = serverTree.listObjectNames(dir, true);
                for (String remoteName : remoteObjects) {

                    LOG.finer("Updating: " + remoteName);

                    // Get the source object
                    if (!objectExists(remoteName)) {
                        // The path is a directory and should be skipped.
                        continue;
                    }
                    IManagedObject srcObject = serverTree.find(remoteName);

                    // Get the target object
                    String targetPath = toAggregateName(remoteName);
                    IManagedObject targetObject = null;
                    try {
                        // Get object from the tree
                        targetObject = serverTree.find(targetPath);

                        // Add source to target
                        if (srcObject instanceof IBaseHistogram) {
                            add((IBaseHistogram) srcObject, (IBaseHistogram) targetObject);
                        }
                    } catch (IllegalArgumentException e) {
                        if (srcObject instanceof IBaseHistogram) {
                            // Create a new target histogram by copying one of the remote objects
                            LOG.finer("Copying: " + remoteName + " -> " + targetPath);
                            serverTree.cp(remoteName, targetPath, false);
                            LOG.finer("Copied object " + srcObject.name() + " entries: "
                                    + ((IBaseHistogram) srcObject).entries());
                        }
                    }

                    LOG.finer("Done updating: " + remoteName);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error updating plots", e);
        }
        LOG.fine("Plot aggregator is done updating");
    }

    private boolean objectExists(String path) {
        try {
            serverTree.find(path);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Add an AIDA object to another
     * @param srcName The source AIDA object path (from the remote)
     * @param target The target AIDA object (the combined histogram)
     */
    private static void add(IBaseHistogram src, IBaseHistogram target) {
        LOG.finest("Adding plots: " + src.title() + " -> " + target.title());
        LOG.finest("Source entries: " + src.entries());
        LOG.finest("Target entries before: " + target.entries());
        if (src instanceof IHistogram) {
            // Add two histograms
            LOG.finest("Adding histograms");
            add((IHistogram) src, (IHistogram) target);
        } else if (src instanceof ICloud) {
            // Add two clouds
            LOG.finest("Adding clouds");
            add((ICloud) src, (ICloud) target);
        } else if (src instanceof IProfile) {
            // Add two profile histograms
            LOG.finest("Adding profile histograms");
            add((IProfile) src, (IProfile) target);
        }

        LOG.finest("Target entries after: " + target.entries());
    }

    /**
     * Add two histograms together
     *
     * Entries in <code>src</code> will be added to those in <code>target</code>
     *
     * @param src The source histogram
     * @param target The target histogram
     */
    private static void add(IHistogram src, IHistogram target) {
        if (src.entries() == 0) {
            return;
        }
        if (target instanceof IHistogram1D) {
            ((IHistogram1D) target).add((IHistogram1D) src);
        } else if (target instanceof IHistogram2D) {
            ((IHistogram2D) target).add((IHistogram2D) src);
        } else if (target instanceof IHistogram3D) {
            ((IHistogram3D) target).add((IHistogram3D) src);
        }
    }

    /**
     * Add two profile histograms together
     *
     * Entries in <code>src</code> will be added to those in <code>target</code>
     *
     * @param src The source profile histogram
     * @param target The target profile histogram
     */
    private static void add(IProfile src, IProfile target) {
        if (src.entries() == 0) {
            return;
        }
        if (target instanceof IProfile1D) {
            ((IProfile1D) target).add((IProfile1D) src);
        } else if (target instanceof IHistogram2D) {
            ((IProfile2D) target).add((IProfile2D) src);
        }
    }

    /**
     * Add two unconverted clouds together
     * @param src The source cloud
     * @param target The target cloud
     */
    private static void add(ICloud src, ICloud target) {
        if (src.isConverted()) {
            LOG.warning("Skipping add of converted src cloud: " + src.title());
            return;
        }
        LOG.finest("Cloud src entries: " + ((ICloud1D)src).entries());
        LOG.finest("Cloud target entries before: " + ((ICloud1D)target).entries());
        if (src instanceof ICloud1D) {
            for (int i=0; i<src.entries(); i++) {
                ((ICloud1D)target).fill(
                        ((ICloud1D) src).value(i),
                        ((ICloud1D) src).weight(i));
            }
        }
        LOG.finest("Cloud target entries after: " + ((ICloud1D)src).entries());
    }

    /**
     * Convert all AIDA clouds to histograms within a tree
     *
     * @param tree The AIDA tree
     * @throws IOException If there is an error converting the clouds
     */
    private static void convertClouds(ITree tree) throws IOException {
        if (tree.isReadOnly()) {
            throw new IOException("Tree is read only");
        }
        String[] objects = tree.listObjectNames("/", true);
        String[] types = tree.listObjectTypes("/", true);
        for (int i=0; i<objects.length; i++) {
            String path = objects[i];
            String type = types[i];
            if (type.contains("Cloud")) {
                IManagedObject obj = tree.find(path);
                ICloud cloud = (ICloud) obj;
                if (cloud.isConverted()) {
                    continue;
                }
                LOG.finest("Converting cloud to hist: " + cloud.title());
                try {
                    if (obj instanceof ICloud1D) {
                        ICloud1D c1d = (ICloud1D) obj;
                        c1d.setConversionParameters(CLOUD_BINS, c1d.lowerEdge(), c1d.upperEdge());
                    } else if (obj instanceof ICloud2D) {
                        ICloud2D c2d = (ICloud2D) obj;
                        c2d.setConversionParameters(CLOUD_BINS, c2d.lowerEdgeX(), c2d.upperEdgeX(),
                                CLOUD_BINS, c2d.lowerEdgeY(), c2d.upperEdgeY());
                    } else if (obj instanceof ICloud3D) {
                        ICloud3D c3d = (ICloud3D) obj;
                        c3d.setConversionParameters(CLOUD_BINS, c3d.lowerEdgeX(), c3d.upperEdgeX(),
                                CLOUD_BINS, c3d.lowerEdgeY(), c3d.upperEdgeY(),
                                CLOUD_BINS, c3d.lowerEdgeZ(), c3d.upperEdgeZ());
                    }
                    cloud.convertToHistogram();
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Error converting cloud to histogram: " + cloud.title());
                }
            }
        }
    }

    /**
     * Copy objects and directories from one tree into another
     * @param srcTree The source tree
     * @param targetTree The target tree
     * @param srcPath The path in the source tree from which to copy
     */
    private static void copy(ITree srcTree, IDevTree targetTree, String srcPath) {
        if (srcPath == null) {
            srcPath = "/";
        }
        String[] objects = srcTree.listObjectNames(srcPath, true);
        String[] types = srcTree.listObjectTypes(srcPath, true);
        for (int i=0; i<objects.length; i++) {
            String path = objects[i];
            String type = types[i];
            if (type.equals("dir")) {
                LOG.finest("Creating dir in output tree: " + path);
                targetTree.mkdirs(path);
            } else {
                LOG.finest("Copying object to target tree: " + path);
                IManagedObject object = srcTree.find(path);
                List<String> spl = new ArrayList<String>(Arrays.asList(path.split("/")));
                spl.remove(spl.size() - 1);
                String dir = String.join("/", spl);
                targetTree.add(dir, object);
            }
        }
    }

    /**
     * Disconnect the object's remote tree server
     *
     * The object is unusable after this unless {@link #connect()}
     * is called again.
     */
    synchronized void disconnect() {

        LOG.info("Disconnecting aggregator ...");
        if (rmiTreeServer != null) {
            try {
                ((RmiServerImpl) rmiTreeServer).disconnect();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error disconnecting RMI tree server", e);
            }
        }
        if (treeServer != null) {
            try {
                treeServer.close();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error disconnecting tree server", e);
            }
        }
        if (serverTree != null) {
            try {
                serverTree.close();
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Error disconnecting server tree", e);
            }
        }
        LOG.info("Done disconnecting aggregator");
    }

    /**
     * Add a new remote AIDA tree
     * @param remoteTreeBind The URL of the remote tree
     * @throws IOException If there is an exception opening the remote tree
     */
    synchronized private void addRemote(String remoteTreeBind) throws IOException {
        if (remoteTreeBind == null) {
            LOG.warning("remoteTreeBind is null");
            return;
        }
        if (remotes.contains(remoteTreeBind)) {
            LOG.warning("Remote already exists: " + remoteTreeBind);
            return;
        }

        boolean clientDuplex = true;
        boolean hurry = false;
        String options = "duplex=\""+clientDuplex+"\",RmiServerName=\"rmi:"+remoteTreeBind+"\",hurry=\""+hurry+"\"";
        ITree remoteTree = null;

        LOG.info("Creating remote tree: " + remoteTreeBind);
        remoteTree = tf.create(remoteTreeBind, RmiStoreFactory.storeType, true, false, options);
        String mountName = toMountName(remoteTreeBind);

        synchronized (updating) {
            LOG.fine("Mounting remote tree to: " + mountName);
            serverTree.mount(mountName, remoteTree, "/");

            String remoteDir = toMountName(remoteTreeBind);
            LOG.fine("Adding dirs for: " + remoteDir);
            Set<String> dirType = new HashSet<String>();
            dirType.add("dir");
            String[] dirNames = listObjectNames(remoteDir, true, dirType);
            for (String dirName : dirNames) {
                String aggName = toAggregateName(dirName);
                LOG.fine("Making aggregation dir: " + aggName);
                this.serverTree.mkdirs(aggName);
            }
        }

        remotes.add(remoteTreeBind);
        LOG.info("Done creating remote tree: " + remoteTreeBind);
        LOG.info("Number of remotes after add: " + remotes.size());
    }

    /**
     * Unmount a remote tree e.g. when a station goes inactive
     * @param remoteTreeBind The URL of the remote tree
     */
    synchronized void unmount(String remoteTreeBind) {
        LOG.info("Unmounting remote tree: " + remoteTreeBind);
        if (!remotes.contains(remoteTreeBind)) {
            LOG.warning("No remote with name: " + remoteTreeBind);
            return;
        }
        synchronized(updating) {
            try {
                String path = toMountName(remoteTreeBind);
                LOG.fine("Unmounting: " + path);
                this.serverTree.unmount(path);
                remotes.remove(remoteTreeBind);
                LOG.info("Number of remotes after remove: " + remotes.size());
                LOG.info("Done unmounting remote tree: " + remoteTreeBind);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error unmounting remote tree: " + remoteTreeBind, e);
            }
        }
    }

    /**
     * Clear the aggregated plots and then add all the remote plots together
     * into combined plots
     */
    @Override
    public void run() {
        if (remotes.size() == 0) {
            return;
        }
        try {
            double start = (double) System.currentTimeMillis();
            synchronized (updating) {
                clearTree();
                update();
            }
            double elapsed = ((double) System.currentTimeMillis()) - start;
            LOG.info("Plot update took: " + elapsed/1000. + " sec");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error updating plots", e);
        }
    }

    /**
     * Save an AIDA file with the current tree contents
     *
     * Only the combined plots are saved to the output file, by creating an
     * output tree.
     *
     * @param file The output AIDA or ROOT file (use '.aida' or '.root' to specify)
     * @throws IOException If there is a problem saving the tree
     */
    void save(File file) throws IOException {

        String path = file.getCanonicalPath();

        synchronized (updating) {
            /*
             * Copy server objects to a new AIDA tree containing
             * only the combined outputs.
             */
            IDevTree outputTree = (IDevTree) tf.create();
            synchronized (this.serverTree) {
                copy(serverTree, outputTree, PlotAggregator.COMBINED_DIR);
            }

            // Modified code from AIDA class in lcsim
            if (path.endsWith(".root")) {

                /*
                 * The combined clouds must be converted to histograms
                 * before writing to a ROOT file or an error will occur.
                 */
                convertClouds(outputTree);

                if (file.exists()) {
                    LOG.info("Deleting old ROOT file: " + path);
                    file.delete();
                }
                RootFileStore store = new RootFileStore(path);
                store.open();
                store.add(outputTree);
                store.close();
                LOG.info("Saved ROOT file: " + path);
            } else {
                if (!path.endsWith(".aida")) {
                    path = path + ".aida";
                    file = new File(path);
                }
                if (file.exists()) {
                    LOG.info("Deleting old AIDA file: " + path);
                    file.delete();
                }
                AidaXMLStore store = new AidaXMLStore();
                de.schlichtherle.io.File newFile = new de.schlichtherle.io.File(path);
                store.commit(outputTree, newFile, null, false, false, false);
                // store.commit(serverTree, newFile, null, true /*gzip*/, false, false);
                LOG.info("Saved AIDA file: " + file.getPath());
            }

            outputTree.close();
            outputTree = null;
        }
    }

    /**
     * Thread for mounting a remote AIDA tree asynchronously, e.g. for a running {@link Station}
     * after it has been started
     */
    class RemoteTreeBindThread extends Thread {

        StationProcess station;
        String remoteTreeBind;
        Integer maxAttempts = 5;

        RemoteTreeBindThread(StationProcess station, Integer maxAttempts) {
            super("Remote Tree Bind Thread");
            if (station == null) {
                throw new IllegalArgumentException("station is null");
            }
            this.station = station;
            Property<String> rtbProp = this.station.getProperties().get("lcsim.remoteTreeBind");
            if (!rtbProp.valid()) {
                throw new IllegalArgumentException("Remote tree bind for station is not valid: " + station.stationName);
            }
            this.remoteTreeBind = rtbProp.value();
            if (maxAttempts != null) {
                if (maxAttempts <= 0) {
                    throw new IllegalArgumentException("Bad value for max attempts: " + maxAttempts);
                }
                this.maxAttempts = maxAttempts;
            }
        }

        /**
         * Attempts to connect to a remote AIDA tree up to a maximum number of attempts
         */
        public void run() {
            try {
                for (long attempt = 1; attempt <= this.maxAttempts; attempt++) {
                    try {
                        try {
                            long waitSec = attempt*5000L;
                            LOG.config("Waiting: " + waitSec + " sec");
                            Thread.sleep(waitSec);
                        } catch (InterruptedException e) {
                            LOG.log(Level.WARNING, "Interrupted (probably the station died)", e);
                            break;
                        }
                        if (!station.isActive()) {
                            LOG.warning("Cannot connect to tree - station went inactive");
                            break;
                        }
                        LOG.info("Remote tree connection attempt: " + attempt + "/" + this.maxAttempts);
                        addRemote(remoteTreeBind);
                        LOG.info("Successfully added remote tree: " + remoteTreeBind);
                        break;
                    } catch (Exception e) {
                        LOG.warning("Connection attempt failed: " + remoteTreeBind);
                    }
                }
            } finally {
                station.rtbThread = null;
            }
        }
    }
}
