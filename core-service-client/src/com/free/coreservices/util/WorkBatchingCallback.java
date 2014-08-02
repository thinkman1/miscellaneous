package com.free.coreservices.util;

import java.util.List;

public interface WorkBatchingCallback<T> {
	public void act(List<T> gatheredItems);
}
