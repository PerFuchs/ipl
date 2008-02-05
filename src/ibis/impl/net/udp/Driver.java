package ibis.impl.net.udp;

import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;

import java.io.IOException;

/**
 * The NetIbis UDP driver.
 */
public final class Driver extends NetDriver {

	final static boolean DEBUG = false; // true;
	final static boolean STATISTICS = false;

	/**
	 * The driver name.
	 */
	private final String name = "udp";

	/**
	 * Constructor.
	 *
	 * @param ibis the {@link ibis.impl.net.NetIbis} instance.
	 */
	public Driver(NetIbis ibis) {
		super(ibis);
	}	

	/**
	 * Returns the name of the driver.
	 *
	 * @return The driver name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Creates a new UDP input.
	 *
	 * @param pt the input's {@link ibis.impl.net.NetPortType NetPortType}.
	 * @param context the context.
	 * @return The new UDP input.
	 */
	public NetInput newInput(NetPortType pt, String context, NetInputUpcall inputUpcall) throws IOException {
		return new UdpInput(pt, this, context, inputUpcall);
	}

	/**
	 * Creates a new UDP output.
	 *
	 * @param pt the output's {@link ibis.impl.net.NetPortType NetPortType}.
	 * @param context the context.
	 * @return The new UDP output.
	 */
	public NetOutput newOutput(NetPortType pt, String context) throws IOException {
		return new UdpOutput(pt, this, context);
	}
}