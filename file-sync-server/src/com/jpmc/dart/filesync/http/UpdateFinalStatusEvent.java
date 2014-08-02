package com.jpmc.dart.filesync.http;

import org.springframework.context.ApplicationEvent;

import com.jpmc.cto.dart.model.filesync.FileSynchronization;


public class UpdateFinalStatusEvent extends ApplicationEvent {
	private static final long serialVersionUID = 1L;

	public UpdateFinalStatusEvent(FileSynchronization finalState) {
		super(finalState);
	}

	public FileSynchronization getFinal() {
		return (FileSynchronization) getSource();
	}
}
