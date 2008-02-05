package ibis.util;

import java.io.IOException;
import java.net.InetAddress;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * The <code>PoolInfo</code> class provides a utility for finding out
 * information about the nodes involved in a closed-world run.
 * This class can be used when the names of the nodes involved in the
 * run are known in advance, and can be given in the
 * <code>ibis.pool.host_names</code> property.
 * If these names are not known in advance, a {@link ibis.util.PoolInfoClient
 * PoolInfoClient} can be used instead. The
 * {@link ibis.util.PoolInfo#createPoolInfo() createPoolInfo} static method
 * can be used to take care of this automatically.
 * <br>
 * The <code>PoolInfo</code> class depends on the following system properties:
 * <br>
 * <pre>ibis.pool.total_hosts</pre>
 * must contain the total number of hosts involved in the run.
 * <br>
 * <pre>ibis.pool.host_names</pre>
 * must contain a space-separated list of hostnames.
 * The number of hostnames in the list must at least be equal to
 * the number of hosts involved in the run as given by the
 * <code>ibis.pool.total_hosts</code> property. Any additional host names
 * are ignored.
 * <br>
 * <pre>ibis.pool.host_number</pre>
 * optional, gives the index of the current host in the list of host names.
 * Should be between 0 and <code>ibis.pool.total_hosts</code> (inclusive).
 * If not supplied, it is determined by looking up the current host in
 * the list of host names.
 */
public class PoolInfo {

    static final String PROPERTY_PREFIX = "ibis.pool.";
    static final String s_cluster = PROPERTY_PREFIX + "cluster";
    static final String s_names = PROPERTY_PREFIX + "host_names";
    static final String s_total = PROPERTY_PREFIX + "total_hosts";
    static final String s_hnum = PROPERTY_PREFIX + "host_number";
    static final String s_single = PROPERTY_PREFIX + "single";
    static final String s_port = PROPERTY_PREFIX + "server.port";
    static final String s_host = PROPERTY_PREFIX + "server.host";
    static final String s_key = PROPERTY_PREFIX + "key";

    static final String [] sysprops = {
	s_cluster,
	s_names,
	s_total,
	s_hnum,
	s_single,
	s_port,
	s_host,
	s_key
    };

    int total_hosts;
    int host_number;
    String [] host_names;
    InetAddress [] hosts;
    static String clusterName;

    static {
	TypedProperties.checkProperties(PROPERTY_PREFIX, sysprops, null);
	clusterName = TypedProperties.stringProperty(s_cluster);
	if (clusterName == null) {
	    clusterName = "unknown";
	}
    }

    /**
     * Constructs a <code>PoolInfo</code> object.
     */
    private PoolInfo() {
	this(false);
    }

    /**
     * Constructs a <code>PoolInfo</code> object.
     * @param forceSequential when set to <code>true</code>,
     * a sequential pool is created, with only the current node as
     * member. The system properties are ignored.
     */
    private PoolInfo(boolean forceSequential) {
	if (forceSequential) {
	    sequentialPool();
	} else {
	    propertiesPool();
	}
    }

    /**
     * Constructor for subclasses.
     */
    protected PoolInfo(int dummy) {
	/* do nothing */
    }

    private void sequentialPool() {
	total_hosts = 1;
	host_number = 0;

	host_names = new String[total_hosts];
	hosts      = new InetAddress[total_hosts];

	try {
	    InetAddress adres = InetAddress.getLocalHost();
	    adres             = InetAddress.getByName(adres.getHostAddress());
	    host_names[host_number] = adres.getHostName();
	    hosts[host_number]      = adres;

	} catch (Exception e) {
	    throw new Error("Could not find my host name");
	}		       			
    }


    private void propertiesPool() {
	String ibisHostNames;

	Properties p = System.getProperties();

	total_hosts = getIntProperty(p, s_total);
	try {
	    host_number = getIntProperty(p, s_hnum);
	} catch (NumberFormatException e) {
	    host_number = -1;
	}

	ibisHostNames = p.getProperty(s_names);
	if(ibisHostNames == null) {
	    throw new RuntimeException("Property " + s_names + " not set!");
	}

	host_names = new String[total_hosts];
	hosts      = new InetAddress[total_hosts];

	StringTokenizer tok = new StringTokenizer(ibisHostNames, " ", false);

	String my_hostname;
	try {
	    my_hostname = InetAddress.getLocalHost().getHostName();
	} catch (java.net.UnknownHostException e) {
	    my_hostname = null;
	}
	// System.err.println(my_hostname + ": I see host_names \"" + ibisHostNames+ "\"");
	int match = 0;
	int my_host = -1;
	for (int i=0;i<total_hosts;i++) {

	    String t;
	    try {
		t = tok.nextToken();       
	    } catch (NoSuchElementException e) {
		throw new RuntimeException("Not enough hostnames in ibis.pool.host_names!");
	    }

	    try {
		/*
		   This looks weird, but is required to get the entire hostname
		   ie. 'java.sun.com' instead of just 'java'.
		   */

		InetAddress adres = InetAddress.getByName(t);
		adres             = InetAddress.getByName(adres.getHostAddress());
		host_names[i]     = adres.getHostName();
		if (! host_names[i].equals(t) &&
			host_names[i].toUpperCase().equals(t.toUpperCase())) {
		    System.err.println("This is probably M$ Windows. Restored lower case in host name " + t);
		    host_names[i] = t;
			}
		hosts[i]          = adres;

		if (host_number == -1) {
		    if (host_names[i].equals(my_hostname)) {
			match++;
			my_host = i;
		    }
		}

	    } catch (IOException e) {
		throw new RuntimeException("Could not find host name " + t);
	    }		       			
	}

	if (host_number == -1 && match == 1) {
	    host_number = my_host;
	    System.err.println("Phew... found a host number " + my_host + " for " + my_hostname);
	}

	if (host_number >= total_hosts || host_number < 0 || total_hosts < 1) {
	    throw new RuntimeException("Sanity check on host numbers failed!");
	}
    }

    /**
     * Returns the number of nodes in the pool.
     * @return the total number of nodes.
     */
    public int size() {
	return total_hosts;
    }

    /**
     * Returns the rank number in the pool of the current host.
     * @return the rank number.
     */
    public int rank() {
	return host_number;
    }

    /**
     * Returns the name of the current host.
     * @return the name of the current host.
     */
    public String hostName() {
	return host_names[host_number];
    }

    /**
     * Returns the cluster name for the current host.
     * @return the cluster name.
     */
    public String clusterName() {
	return clusterName;
    }

    /**
     * Returns the cluster name for the host specified by the rank number.
     * @param rank the rank number.
     * @return the cluster name.
     */
    public String clusterName(int rank) {
	return clusterName;
    }

    /**
     * Return a Grid Cluster rank based on the lower IP byte of the
     * host names
     */
    public int[] clusterIPRank() {
	int[] clusterRank = new int[hosts.length];
	byte[][] rawAddr = new byte[hosts.length][];

	for (int i = 0; i < hosts.length; i++) {
	    rawAddr[i] = hosts[i].getAddress();
	    clusterRank[i] = -1;
	}

	int nextFreeRank = 0;
	for (int i = 0; i < hosts.length; i++) {
	    if (clusterRank[i] == -1) {
		clusterRank[i] = nextFreeRank;
		for (int j = i + 1; j < hosts.length; j++) {
		    int b;
		    for (b = 0; b < 3; b++) {
			if (rawAddr[i][b] != rawAddr[j][b]) {
			    break;
			}
		    }
		    if (b == 3) {
			clusterRank[j] = nextFreeRank;
		    }
		}
		nextFreeRank++;
	    }
	}

	return clusterRank;
    }

    /**
     * Return the number of Grid Clusters based on the lower IP byte of the
     * host names
     */
    public int clusterIPSize() {
	int[] clusterRank = clusterIPRank();

	int clusterSize = -1;
	for (int i = 0; i < clusterRank.length; i++) {
	    clusterSize = Math.max(clusterSize, clusterRank[i]);
	}
	clusterSize++;

	return clusterSize;
    }

    /**
     * Returns an array of cluster names, one for each host involved in
     * the run.
     * @return the cluster names
     */
    public String[] clusterNames() {
	String[] r = new String[total_hosts];
	for (int i = 0; i < total_hosts; i++) {
	    r[i] = clusterName;
	}
	return r;
    }

    /**
     * Returns the name of the host with the given rank.
     * @param rank the rank number.
     * @return the name of the host with the given rank.
     */
    public String hostName(int rank) {
	return host_names[rank];
    }

    /**
     * Returns an array of hostnames of the hosts.
     * @return an array of hostnames of the hosts.
     */
    public String[] hostNames() {
	return (String[]) host_names.clone();
    }

    private static int getIntProperty(Properties p, String name) {

	String temp = p.getProperty(name);

	if (temp == null) { 
	    throw new NumberFormatException("Property " + name + " not found !");
	}

	return Integer.parseInt(temp);
    }	

    /**
     * Utility method to print the time used in a uniform format.
     * @param id name of the application
     * @param time the time used, in milliseconds.
     */
    public void printTime(String id, long time) {
	System.out.println("Application: " + id + "; Ncpus: " + total_hosts +
		"; time: " + time/1000.0 + " seconds\n");
    }

    /**
     * Creates and returns a <code>PoolInfo</code>.
     * The parameter indicates wether a pool for a sequential run
     * must be created. If not, if the system property
     * <code>ibis.pool.host_names</code> is set, a <code>PoolInfo</code>
     * is created. If not, a {@link ibis.util.PoolInfoClient PoolInfoClient}
     * is created.
     * @param forceSeq indicates wether a pool for a sequential run must
     * be created.
     * @return the resulting <code>PoolInfo</code> object.
     */
    public static PoolInfo createPoolInfo(boolean forceSeq) {
	if (forceSeq) {
	    return new PoolInfo(true);
	}
	if (TypedProperties.stringProperty(s_names) != null) {
	    return new PoolInfo();
	}
	try {
	    return PoolInfoClient.create();
	} catch(Throwable e) {
	    throw new RuntimeException("Got exception", e);
	}
    }

    /**
     * Creates and returns a <code>PoolInfo</code>.
     * If the system property <code>ibis.pool.host_names</code> is set,
     * a <code>PoolInfo</code> is created.
     * If not, a {@link ibis.util.PoolInfoClient PoolInfoClient}
     * is created.
     * @return the resulting <code>PoolInfo</code> object.
     */
    public static PoolInfo createPoolInfo() {
	return createPoolInfo(false);
    }

    /**
     * Returns a string representation of the information in this
     * <code>PoolInfo</code>.
     * @return a string representation.
     */
    public String toString() {
	String result = "pool info: size = " + total_hosts +
	    "; my rank is " + host_number + "; host list:\n";
	for (int i = 0; i < total_hosts; i++) {
	    result += i + ": address= " + hosts[i] + 
		" cluster=" + clusterName + "\n";
	}
	return result;
    }
}