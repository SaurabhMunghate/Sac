package com.shatam.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.jar.JarException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.webapp.WebAppContext;
import org.json.JSONObject;

import com.shatam.util.Paths;
import com.shatam.util.U;

public class WebServerJetty {

	public static void main(String args[]) throws Exception {

		if (args.length > 0)
			Paths.READ_DATA = args[0];
		else {
			// Paths.READ_DATA=Paths.DATA_ROOT;
			//Paths.READ_DATA = "E:/Data";
		}

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
		U.log("SAC v 1.9 Running");
		server.start();
		server.join();

	}

}
