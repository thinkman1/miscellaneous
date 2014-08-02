package com.jpmc.dart.filesync.http;

import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WorkerThreadFactory {
	private int maxThreads;

	public Executor getThreadPool() {
		return new ThreadPoolExecutor(maxThreads, maxThreads,
				60L, TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>());
	}

	public void setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
	}
}
