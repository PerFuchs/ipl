package ibis.ipl;

/**
   This class defines the Ibis API, which can be implemented by an Ibis implementation.
   Every JVM may run multiple Ibis implementations.
   The user can specify the Ibis implementation at startup.
   An Ibis implementation offers certain PortType properties.
   When creating an IBIS-IMPL, it can negotiate with the application to see if 
   it offers the required properties.
**/

public abstract class Ibis { 

	protected String name;
	protected ResizeHandler resizeHandler;

	/** 
	    Creates a new Ibis instance. Instances must be given a unique name, which identifies the
	    instance. Lookups are done using this name. If the user tries to create two instances
	    with the same name, an IbisException will be thrown.
	    The resizeHandler will be invoked when Ibises join and leave, and may be null to indicate that
	    resize notifications are not wanted. **/
	public static Ibis createIbis(String name, String implName, ResizeHandler resizeHandler) throws IbisException {
		Ibis impl;

		try { 
			if (implName == null) { 
				throw new NullPointerException("Implementation name is null");
			} 
			
			if (name == null) { 
				throw new NullPointerException("Ibis name is null");
			} 
			
			impl = (Ibis) Class.forName(implName).newInstance();
			impl.name = name;
			impl.resizeHandler = resizeHandler;
			impl.init(); //@@@ EXPORT THIS !! We want to check the properties before doint an init!! 
			
		} catch (Throwable t) { 			 
			throw new IbisException("Could not initialize Ibis", t);
		}
		
		return impl;
	}

	/** After openWorld, join and leave calls may be received. **/
	public abstract void openWorld();

	/** After closeWorld, no join or leave calls are received. **/
	public abstract void closeWorld();

	/** Returns all Ibis recources to the system **/
	public abstract void end();

	/** A PortType can be created using this method.
	    A name is given to the PortType (e.g. "satin porttype" or "RMI porttype"), and
	    Port properties are specified (for example ports are "totally-ordered" and 
	    "reliable" and support "NWS"). The name and properties <strong>together</strong>
	    define the PortType.
	    If two Ibis implementations want to communicate, they must both create a PortType
	    with the same name and properties.
	    If an multiple implementation try to create a PortType with the same name but
	    different properties, an IbisException will be thrown.
	    For each PortType there is an ReceivePort and an SendPort.
	    Only ReceivePorts and Sendports of the same PortType can communicate.
	    Any number of ReceivePorts and Sendports can be created on a JVM
	    (even of the same PortType). **/
	public abstract PortType createPortType(String name, StaticProperties p) throws IbisException;
	public abstract PortType getPortType(String name) throws IbisException;

	/** Returns the Ibis Registry. **/
	public abstract Registry registry();

	/** Returns the properties of the underlying Ibis implementation. **/
	public abstract StaticProperties properties();

	/** Returns the user-specified name of this Ibis instance. **/
	public String name() { 
		return name;
	} 

	public abstract IbisIdentifier identifier();
	protected abstract void init() throws IbisException;
} 
