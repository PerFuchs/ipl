package ibis.impl.net.bytes;

import ibis.util.TypedProperties;

public interface Settings {

        /** 
         * Upper bound on the length of byte buffers generated by the <code>'byte'</code> driver.
         */
        final int maxMtu = TypedProperties.intProperty(ibis.impl.net.NetIbis.bytes_mtu, Integer.MAX_VALUE);
}
