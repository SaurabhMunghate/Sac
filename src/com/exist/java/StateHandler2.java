package com.exist.java;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class StateHandler2 extends AbstractHandler {
	String state;

	/*
	 * public JsonPostHandler(String state) { this.state = state; }
	 */

	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException {
	}
}
