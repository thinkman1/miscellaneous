package com.jpmc.dart.util.staxutils;

public interface StringCallback extends MatchCallback<String> {
	public void callback(String xmlData) throws Exception;
}
