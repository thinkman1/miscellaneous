package com.jpmc.dart.filesync.servicesla;

import java.util.concurrent.TimeUnit;

import com.jpmc.cto.framework.concurrent.EmptyExpirationPolicy;
import com.jpmc.cto.framework.concurrent.SimpleLockManager;

public class NoCareLockManager extends SimpleLockManager {
	public NoCareLockManager() {
		setExpirationPolicy(new EmptyExpirationPolicy());
		setRetryCount(10);
		setRetryTimeUnit(TimeUnit.SECONDS);
		setWaitBetweenRetry(5);
	}
}
