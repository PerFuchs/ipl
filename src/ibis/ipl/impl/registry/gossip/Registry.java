package ibis.ipl.impl.registry.gossip;

import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisProperties;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.RegistryEventHandler;

import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.Location;

import ibis.util.ThreadPool;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.log4j.Logger;

public class Registry extends ibis.ipl.impl.Registry implements Runnable {

    private final Logger logger = Logger.getLogger(Registry.class);

    private final IbisCapabilities capabilities;

    private final String poolName;

    private final IbisIdentifier identifier;
    
    private final MemberSet members;
    
    private final CommunicationHandler commHandler;
    
    private final Upcaller upcaller;

    // data structures the user can poll

    private final ArrayList<IbisIdentifier> joinedIbises;

    private final ArrayList<IbisIdentifier> leftIbises;

    private final ArrayList<IbisIdentifier> diedIbises;

    private final ArrayList<String> signals;

    /**
     * Creates a Gossip Registry.
     * 
     * @param eventHandler
     *            Registry handler to pass events to.
     * @param userProperties
     *            properties of this registry.
     * @param data
     *            Ibis implementation data to attach to the IbisIdentifier.
     * @param ibisImplementationIdentifier
     *            the identification of this ibis implementation, including
     *            version, class and such. Must be identical for all Ibisses in
     *            a single poolName.
     * @throws IOException
     *             in case of trouble.
     * @throws IbisConfigurationException
     *             In case invalid properties were given.
     */
    public Registry(IbisCapabilities capabilities,
            RegistryEventHandler eventHandler, Properties userProperties,
            byte[] ibisData, String ibisImplementationIdentifier)
            throws IbisConfigurationException, IOException,
            IbisConfigurationException {
        this.capabilities = capabilities;

        if (capabilities
                .hasCapability(IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED)) {
            throw new IbisConfigurationException(
                    "gossip registry does not support totally ordered membership");
        }

        if (capabilities.hasCapability(IbisCapabilities.CLOSEDWORLD)) {
            throw new IbisConfigurationException(
                    "gossip registry does not support closed world");
        }

        if (capabilities.hasCapability(IbisCapabilities.ELECTIONS_STRICT)) {
            throw new IbisConfigurationException(
                    "gossip registry does not support strict elections");
        }

        TypedProperties properties =
                RegistryProperties.getHardcodedProperties();
        properties.addProperties(userProperties);

        if ((capabilities.hasCapability(IbisCapabilities.MEMBERSHIP_UNRELIABLE))
                && eventHandler == null) {
            joinedIbises = new ArrayList<IbisIdentifier>();
            leftIbises = new ArrayList<IbisIdentifier>();
            diedIbises = new ArrayList<IbisIdentifier>();
        } else {
            joinedIbises = null;
            leftIbises = null;
            diedIbises = null;
        }

        if (capabilities.hasCapability(IbisCapabilities.SIGNALS)
                && eventHandler == null) {
            signals = new ArrayList<String>();
        } else {
            signals = null;
        }

        if (eventHandler != null) {
            upcaller = new Upcaller(eventHandler);
        } else {
            upcaller = null;
        }

        UUID id = UUID.randomUUID();

        poolName = properties.getProperty(IbisProperties.POOL_NAME);

        if (poolName == null) {
            throw new IbisConfigurationException(
                    "cannot initialize registry, property "
                            + IbisProperties.POOL_NAME + " is not specified");
        }
       
        members = new MemberSet(properties);
        
        commHandler = new CommunicationHandler(properties, this);

        Location location = Location.defaultLocation(properties);

        identifier =
                new IbisIdentifier(id.toString(), ibisData, commHandler
                        .getAddress().toBytes(), location, poolName);

        ThreadPool.createNew(this, "pool management thread");
        
        logger.debug("registry for " + identifier + " initiated");
    }

    @Override
    public IbisIdentifier getIbisIdentifier() {
        return identifier;
    }

    public IbisIdentifier elect(String electionName) throws IOException {
        if (!capabilities.hasCapability(IbisCapabilities.ELECTIONS_UNRELIABLE)) {
            throw new IbisConfigurationException(
                    "No election support requested");
        }

        //TODO: implement
        return null;
    }

    public IbisIdentifier getElectionResult(String election) throws IOException {
        return getElectionResult(election, 0);
    }

    public IbisIdentifier getElectionResult(String electionName,
            long timeoutMillis) throws IOException {
        if (!capabilities.hasCapability(IbisCapabilities.ELECTIONS_UNRELIABLE)) {
            throw new IbisConfigurationException(
                    "No election support requested");
        }

        //TODO: implement
        return null;
    }

    public void maybeDead(ibis.ipl.IbisIdentifier suspect)
            throws IOException {
        members.maybeDead(suspect);
    }

    public void assumeDead(ibis.ipl.IbisIdentifier deceased)
            throws IOException {
        members.assumeDead(deceased);
    }

    public void signal(String signal,
            ibis.ipl.IbisIdentifier... ibisIdentifiers) throws IOException {
        if (!capabilities.hasCapability(IbisCapabilities.SIGNALS)) {
            throw new IbisConfigurationException("No string support requested");
        }

        //TODO: implement
    }

    public synchronized ibis.ipl.IbisIdentifier[] joinedIbises() {
        if (joinedIbises == null) {
            throw new IbisConfigurationException(
                    "Resize downcalls not configured");
        }

        ibis.ipl.IbisIdentifier[] result =
                joinedIbises.toArray(new ibis.ipl.IbisIdentifier[0]);
        joinedIbises.clear();
        return result;
    }

    public synchronized ibis.ipl.IbisIdentifier[] leftIbises() {
        if (leftIbises == null) {
            throw new IbisConfigurationException(
                    "Resize downcalls not configured");
        }
        ibis.ipl.IbisIdentifier[] result =
                leftIbises.toArray(new ibis.ipl.IbisIdentifier[0]);
        leftIbises.clear();
        return result;
    }

    public synchronized ibis.ipl.IbisIdentifier[] diedIbises() {
        if (diedIbises == null) {
            throw new IbisConfigurationException(
                    "Resize downcalls not configured");
        }

        ibis.ipl.IbisIdentifier[] result =
                diedIbises.toArray(new ibis.ipl.IbisIdentifier[0]);
        diedIbises.clear();
        return result;
    }

    public synchronized String[] receivedSignals() {
        if (signals == null) {
            throw new IbisConfigurationException(
                    "Registry downcalls not configured");
        }

        String[] result = signals.toArray(new String[0]);
        signals.clear();
        return result;
    }

    public int getPoolSize() {
        throw new IbisConfigurationException(
                "getPoolSize not supported by gossip registry");
    }

    public synchronized void waitUntilPoolClosed() {
        throw new IbisConfigurationException(
                "waitForAll not supported by gossip registry");

    }

    public void enableEvents() {
        if (upcaller == null) {
            throw new IbisConfigurationException("Registry not configured to "
                    + "produce events");
        }

        upcaller.enableEvents();
    }

    public void disableEvents() {
        if (upcaller == null) {
            throw new IbisConfigurationException("Registry not configured to "
                    + "produce events");
        }

        upcaller.disableEvents();
    }

    @Override
    public long getSequenceNumber(String name) throws IOException {
        throw new IbisConfigurationException(
                "Sequence numbers not supported by" + "gossip registry");
    }

    @Override
    public void leave() throws IOException {
        //TODO: implement
        upcaller.stop();
    }

    public Map<String, String> managementProperties() {
        // no properties (as of yet)
        return new HashMap<String, String>();
    }

    public String getManagementProperty(String key)
            throws NoSuchPropertyException {
        String result = managementProperties().get(key);

        if (result == null) {
            throw new NoSuchPropertyException(key + " is not a valid property");
        }
        return result;
    }

    public void setManagementProperties(Map<String, String> properties)
            throws NoSuchPropertyException {
        throw new NoSuchPropertyException(
                "central registry does not have any properties that can be set");
    }

    public void setManagementProperty(String key, String value)
            throws NoSuchPropertyException {
        throw new NoSuchPropertyException(
                "central registry does not have any properties that can be set");
    }

    // functions called by pool to tell the registry an event has occured

    synchronized void ibisJoined(IbisIdentifier ibis) {
        if (joinedIbises != null) {
            joinedIbises.add(ibis);
        }

        if (upcaller != null) {
            upcaller.ibisJoined(ibis);
        }
    }

    synchronized void ibisLeft(IbisIdentifier ibis) {
        if (leftIbises != null) {
            leftIbises.add(ibis);
        }

        if (upcaller != null) {
            upcaller.ibisLeft(ibis);
        }
    }

    synchronized void ibisDied(IbisIdentifier ibis) {
        if (diedIbises != null) {
            diedIbises.add(ibis);
        }

        if (upcaller != null) {
            upcaller.ibisDied(ibis);
        }
    }

    synchronized void signal(String signal) {
        if (signals != null) {
            signals.add(signal);
        }

        if (upcaller != null) {
            upcaller.signal(signal);
        }
    }

    synchronized void electionResult(String name, IbisIdentifier winner) {
        if (upcaller != null) {
            upcaller.electionResult(name, winner);
        }
    }

    public boolean isStopped() {
        // TODO Auto-generated method stub
        return false;
    }

    public String getPoolName() {
        return poolName;
    }

    public void run() {
        // TODO Auto-generated method stub
        
    }

}
