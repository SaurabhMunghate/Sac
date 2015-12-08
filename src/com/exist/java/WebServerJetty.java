package com.exist.java;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.webapp.WebAppContext;
import org.json.JSONObject;

import com.shatam.io.ShatamIndexReader;
import com.shatam.util.Paths;
import com.shatam.util.U;

public class WebServerJetty {

	public static void main(String args[]) throws Exception {
		//U.log("read data="+args[0]);
		HttpResponse response;
		HttpClient client = new DefaultHttpClient();
		HttpConnectionParams.setConnectionTimeout(client.getParams(), 1000);
		JSONObject json = new JSONObject();
		
		if(args.length>0)
		Paths.READ_DATA=args[0];		
		else{
			Paths.READ_DATA=Paths.DATA_ROOT;
		}
		StringBuilder out = new StringBuilder();
	try{	
		HttpPost post = new HttpPost("http://phoneshoppingsite.com/Sacvalidation.aspx");
		json.put("user", "shatam");
		json.put("pass", "shatam");
		StringEntity se = new StringEntity(json.toString());
		se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE,
				"application/json"));
		post.setEntity(se);
		response = client.execute(post);
		InputStream in = response.getEntity().getContent(); // Get the
		// data in
		// the
		// entity
BufferedReader reader = new BufferedReader(
new InputStreamReader(in));

String line;
while ((line = reader.readLine()) != null) {
out.append(line);
}
//System.out.println("OUTPUT=" + out.toString());
		
	}
	catch(Exception e){
		U.log(e);
		U.log("Invalid User");
		System.exit(1);
	}
		
/*		InputStream in = response.getEntity().getContent(); // Get the
		// data in
		// the
		// entity
BufferedReader reader = new BufferedReader(
new InputStreamReader(in));
StringBuilder out = new StringBuilder();
String line;
while ((line = reader.readLine()) != null) {
out.append(line);
}
System.out.println("OUTPUT=" + out.toString());
		
		
		U.log("************************************"+response.toString());*/
	if(out.toString().contains("Done"))
	{	
		Server server = new Server();

		SelectChannelConnector connector = new SelectChannelConnector();

		connector.setPort(3309);
		/* connector.setConfidentialPort(49700); */
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
	else
	{
		U.log("Invalid User");
		System.exit(1);
	}
	}
	
}
