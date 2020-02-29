package net.d53dev.geocodecache.http;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class Stats {
	private final AtomicLong requestCount;
	private final AtomicLong cacheHits;
	private final AtomicLong dbHits;
	private final AtomicLong apiHits;
	private final AtomicLong dbErrors;
	private final AtomicLong apiErrors;
	private final int printRequestInterval;

	public Stats() {
		this(-1);
	}

	public Stats(int printRequestInterval) {
		requestCount = new AtomicLong();
		cacheHits = new AtomicLong();
		dbHits = new AtomicLong();
		apiHits = new AtomicLong();
		dbErrors = new AtomicLong();
		apiErrors = new AtomicLong();
		this.printRequestInterval = printRequestInterval;
	}

	public void registerRequest() {
		var reqCount = this.requestCount.incrementAndGet();

		if (reqCount % printRequestInterval == 0) {
			log.info("Geocache Statistics: Requests={}, Cached={}, Db={}, API={}. Errors: Db={}, API={}.", reqCount,
					cacheHits.get(), dbHits.get(), apiHits.get(), dbErrors.get(), apiErrors.get());
		}
	}

	public void registerCacheHit() {
		this.cacheHits.incrementAndGet();
	}

	public void registerDbHit() {
		this.dbHits.incrementAndGet();
	}

	public void registerApiHit() {
		this.apiHits.incrementAndGet();
	}

	public void registerDbError() {
		this.dbErrors.incrementAndGet();
	}

	public void registerApiError() {
		this.apiErrors.incrementAndGet();
	}
}
