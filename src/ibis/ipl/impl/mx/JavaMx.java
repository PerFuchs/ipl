package ibis.ipl.impl.mx;

import java.nio.ByteBuffer;

final class JavaMx {
	
	static final class HandleManager {
		int size, blockSize, maxBlocks, activeHandles;
		boolean[] inUse;
		
		private HandleManager(int blockSize, int maxBlocks) throws MxException {
			this.size = 0;
			this.blockSize = blockSize;
			this.maxBlocks = maxBlocks;
			this.activeHandles = 0;
			this.inUse = new boolean[size];
			if (init(blockSize, maxBlocks) == false) {
				throw new MxException("HandleManager: could not initialize Handles.");
			}
			// init calls to C library
		}
		
		private native boolean init(int blockSize, int maxBlocks);
		private native boolean addBlock();
		
		protected synchronized int getHandle() {
			if(activeHandles == size) {
				//Size up handle array, also in C code
				addBlock();
			}
			
			int handle = 0;
			while(inUse[handle] ) {
				handle++;
			}
			inUse[handle] = true;
			activeHandles++;
			return handle;
		}
		
		protected synchronized void releaseHandle(int handle) {
			inUse[handle] = false;
			activeHandles--;
		}
		
	}
	
	static final class LinkManager {
		int size, blockSize, maxBlocks, activeTargets;
		boolean[] inUse;
		
		private LinkManager(int blockSize, int maxBlocks) throws MxException {
			this.size = 0;
			this.blockSize = blockSize;
			this.maxBlocks = maxBlocks;
			this.activeTargets = 0;
			inUse = new boolean[size];
			if (init(blockSize, maxBlocks) == false) {
				throw new MxException("LinkManager: could not initialize Links.");
			}
			// init calls to C library
		}
		
		private native boolean init(int blockSize, int maxBlocks);
		private native boolean addBlock();
		
		protected synchronized int getLink() {
			if(activeTargets == size) {
				//Size up target array, also in C code
				addBlock();
			}
			
			int target = 0;
			while(inUse[target] ) {
				target++;
			}
			inUse[target] = true;
			activeTargets++;
			return target;
		}
		
		protected synchronized void releaseLink(int target) {
			inUse[target] = false;
			activeTargets--;
		}
		
		
	}
	
	static int endpointsInUse = 0;
	static boolean initialized = false;
	static HandleManager handles;
	static LinkManager links;
	
	static {
		System.loadLibrary("myriexpress");
		System.loadLibrary("javamx");
		initialized = init();
		if(!initialized) {
			System.err.println("Error initializing JavaMX library.");
			System.exit(1);
		}
		try {
			handles = new HandleManager(128, 128*1024);
		} catch (MxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} //TODO come up with better values?
		try {
			links = new LinkManager(128, 128*1024);
		} catch (MxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} //TODO come up with better values?
	}
	
	static native boolean init();
	static native boolean deInit();
	
	static native int newHandler(int filter); 
	static native boolean closeHandler(int handlerId);
	
	static native long getMyNicId(int handlerId);
	static native int getMyEndpointId(int handlerId);
	static native long getNicId(String name);
	
	static native boolean connect(int handlerId, int link, long nicId, int endpointId, int filter);
	static native boolean connect(int handlerId, int link, long nicId, int endpointId, int filter, long timeout);
	static native boolean disconnect(int link);

	static native void send(ByteBuffer buffer, int bufsize, int handlerId, int link, int handle, long matchData);
	static native void sendSynchronous(ByteBuffer buffer, int bufsize, int handlerId, int link, int handle, long matchData); // returns a handle
	
	static native void recv(ByteBuffer buffer, int bufsize, int handlerId, int handle, long matchData) throws MxException; // returns a handle
	static native void recv(ByteBuffer buffer, int bufsize, int handlerId, int handle, long matchData, long matchMask) throws MxException; // returns a handle
	
	static native int wait(int handlerId, int handle) throws MxException; // return message size by success, -1, when unsuccesful
	static native int wait(int handlerId, int handle, long timeout) throws MxException; // return message size by success, -1, when unsuccesful
	static native int test(int handlerId, int handle) throws MxException; // return message size by success, -1, when unsuccesful
	
	static native boolean cancel(int handlerId, int handle);
	static native void wakeup(int handlerId);
	
	/*
	static native boolean jmx_probe();
	*/		
		
}
