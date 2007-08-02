package ibis.ipl.impl.registry.newCentral;

import ibis.ipl.impl.IbisIdentifier;
import ibis.util.ThreadPool;

import org.apache.log4j.Logger;

final class RandomEventPusher implements Runnable {

	private static final Logger logger = Logger
			.getLogger(RandomEventPusher.class);

	private final Pool pool;

	private final long interval;

	/**
	 * If true, the interval of the event pushing is adapted to the pool size
	 */
	private final boolean adaptInterval;

	private final int maxThreads;

	private int currentThreads;

	// inner scheduling class
	private class Scheduler implements Runnable {

		Scheduler() {
			ThreadPool.createNew(this, "scheduler thread");
		}

		public void run() {
			while (!pool.ended()) {
				logger.debug("scheduling node contactor");
				long timeout = interval;

				createNewThread();

				logger.debug("scheduled node contactor");

				if (adaptInterval) {
					int poolSize = pool.getSize();
					if (poolSize > 0) {
						// divide by log2(poolSize)
						timeout = (long) (timeout / (Math.log(poolSize) / Math
								.log(2)));
					}

				}

				logger.debug("waiting " + timeout);
				synchronized (this) {
					try {

						wait(timeout);
					} catch (InterruptedException e) {
						// IGNORE
					}
				}

			}
		}
	}

	/**
	 * Randomly push events to registries in the pool
	 */
	RandomEventPusher(Pool pool, long interval, boolean adaptInterval,
			int maxThreads) {
		this.pool = pool;
		this.interval = interval;
		this.adaptInterval = adaptInterval;
		this.maxThreads = maxThreads;

		currentThreads = 0;

		// schedules calls to our run function
		new Scheduler();
	}

	private synchronized void createNewThread() {
		if (currentThreads >= maxThreads) {
			logger.debug("not creating thread, maximum reached");
			return;
		}
		ThreadPool.createNew(this, "node contactor");
		currentThreads++;
	}

	private synchronized void threadDone() {
		currentThreads--;
	}

	public void run() {
		IbisIdentifier member = pool.getRandomMember();

		if (member == null) {
			logger.debug("no member to contact");
		} else {
			pool.push(member);
		}
		threadDone();
	}
}