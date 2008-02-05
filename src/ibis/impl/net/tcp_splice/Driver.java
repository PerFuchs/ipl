package ibis.impl.net.tcp_splice;

import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;

import java.io.IOException;

/**
 * The NetIbis TCP driver with pipelined block transmission.
 */
public final class Driver extends NetDriver {

	/**
	 * The driver name.
	 */
	private final String name = "tcp_splice";


	/**
	 * Constructor.
	 *
	 * @param ibis the {@link ibis.impl.net.NetIbis} instance.
	 */
	public Driver(NetIbis ibis) {
		super(ibis);
	}

	public String getName() {
		return name;
	}

	public NetInput newInput(NetPortType pt,
				 String context,
				 NetInputUpcall inputUpcall)
			 throws IOException {
                //System.err.println("new tcp input");
		return new TcpInput(pt, this, context, inputUpcall);
	}

	public NetOutput newOutput(NetPortType pt, String context) throws IOException {
                //System.err.println("new tcp output");
		return new TcpOutput(pt, this, context);
	}
}