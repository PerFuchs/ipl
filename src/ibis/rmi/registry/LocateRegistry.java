package ibis.rmi.registry;

import ibis.rmi.*;

public final class LocateRegistry
{
    private static String registryPkgPrefix =
        System.getProperty("java.rmi.registry.packagePrefix", "ibis.rmi.registry.impl");
			  
    private static RegistryHandler handler = null;
    
    private LocateRegistry() {}

    public static Registry getRegistry() throws RemoteException
    {
	try {
	    return getRegistry(null, Registry.REGISTRY_PORT);
	} catch (UnknownHostException e) {
	}
	return null;
    }

    public static Registry getRegistry(int port) throws RemoteException
    {
	try {
	    return getRegistry(null, port);
	} catch (UnknownHostException e) {
	}
	return null;
    }
    
    public static Registry getRegistry(String host) throws RemoteException, UnknownHostException
    {
	return getRegistry(host, Registry.REGISTRY_PORT);
    }
    
    public static Registry getRegistry(String host, int port) throws RemoteException, UnknownHostException
    {
	if (handler != null) {
	    return handler.registryStub(host, port);
	}

	throw new RemoteException("Registry handler not present");

    }

    public static Registry createRegistry(int port) throws RemoteException
    {
	if (handler != null) {
	    return handler.registryImpl(port);
	}

	throw new RemoteException("Registry handler not present");
    }

    static {
        String classname = registryPkgPrefix + ".RegistryHandler";
	try {
	    Class cl = Class.forName(classname);
	    handler = (RegistryHandler)(cl.newInstance());
	} catch (Exception e) {
	}
    }
}
