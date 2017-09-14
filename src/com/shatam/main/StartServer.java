/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.main;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.webapp.WebAppContext;

import com.shatam.util.Paths;
import com.shatam.util.U;

public class StartServer {

	public static void main(String args[]) throws Exception {

		if (args.length > 0)
			Paths.READ_DATA = args[0];

//		Paths.READ_DATA = "C:\\sac test\\DATA\\";
//		U.log("Root Path ::"+System.getProperty("user.dir"));
		Server server = new Server();
		SelectChannelConnector connector = new SelectChannelConnector();
		connector.setPort(3309);
		server.addConnector(connector);
		ContextHandler contextState = new ContextHandler("/checkStatus");
		StateHandler2 handler = new StateHandler2();
		contextState.setHandler(handler);
		ContextHandler contextPostData = new ContextHandler("/postData");
		JsonPostHandler handler2 = new JsonPostHandler();
		contextPostData.setHandler(handler2);
		ResourceHandler resource_handler = new ResourceHandler();
		resource_handler.setDirectoriesListed(false);
		GenerateCache.doCache();
		Seeding.seed();
		resource_handler.setResourceBase(".");
		WebAppContext webapp = new WebAppContext();
		webapp.setResourceBase(".");
		webapp.setContextPath("/");
		webapp.setWelcomeFiles(new String[] { "WebContent/Index.html" });
		ContextHandlerCollection contexts = new ContextHandlerCollection();
		contexts.setHandlers(new Handler[] { contextState, contextPostData,
				webapp });
		server.setHandler(contexts);
		U.log("SAC v 1.15 Running");
		server.start();
		server.join();

	}

}
