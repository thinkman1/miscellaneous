package com.jpmc.dart.util.staxutils;

import java.util.Collections;
import java.util.List;

public class SimpleDataSelector extends SelectDataVisitor {

	public SimpleDataSelector(String... paths) throws Exception {
		super(paths);
	}

	public List<String> getData() {
		if (getSelectedData().get("DATA") != null) {
			return getSelectedData().get("DATA");
		}

		return Collections.emptyList();
	}

}
