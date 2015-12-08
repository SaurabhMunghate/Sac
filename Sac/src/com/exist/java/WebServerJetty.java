package com.exist.java;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.webapp.WebAppContext;

public class WebServerJetty {

	public static void main(String args[]) throws Exception {

		Server server = new Server();

		SelectChannelConnector connector = new SelectChannelConnector();

		connector.setPort(3309);
		/* connector.setConfidentialPort(3309); */
		server.addConnector(connector);
		ContextHandler contextState = new ContextHandler("/getData");
		StateHandler2 handler = new StateHandler2();
		contextState.setHandler(handler);
		ContextHandler contextPostData = new ContextHandler("/postData");
		JsonPostHandler handler2 = new JsonPostHandler();
		contextPostData.setHandler(handler2);
		ResourceHandler resource_handler = new ResourceHandler();
		resource_handler.setDirectoriesListed(false); 
		 // resource_handler.setWelcomeFiles(new String[] {
		 // "WebContent/Index.html" });
	     // building cache for data
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
		String home = System.getProperty("user:dir", ".");
		
		server.start();
		server.join();  
	}

}
