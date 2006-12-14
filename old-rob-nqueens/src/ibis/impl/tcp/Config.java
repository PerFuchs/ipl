/* $Id$ */

package ibis.impl.tcp;

import ibis.util.TypedProperties;

interface Config {
    static final String PROPERTY_PREFIX = "ibis.tcp.";

    static final String s_debug = PROPERTY_PREFIX + "debug";

    static final String s_stats = PROPERTY_PREFIX + "stats";

    static final String s_cache = PROPERTY_PREFIX + "cache";

    static final boolean DEBUG = TypedProperties.booleanProperty(s_debug);

    static final boolean STATS = TypedProperties.booleanProperty(s_stats, true);

    // Not configurable at runtime: too error prone.
    // Note that the nameserver uses this as well.
    static final boolean ID_CACHE = false;

    static final String[] sysprops = { s_debug, s_stats, s_cache };
}
