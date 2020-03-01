package io.github.kraowx.shibbyappserver.net;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import org.json.JSONObject;

import fi.iki.elonen.NanoHTTPD;
import io.github.kraowx.shibbyappserver.DataUpdater;
import io.github.kraowx.shibbyappserver.Main;
import io.github.kraowx.shibbyappserver.tools.FormattedOutput;

public class Server extends NanoHTTPD
{
	public static final String VERSION = "2.0.0";
	private final String HTML_PATH = "index.html";
	
	private String html;
	private DataUpdater dataUpdater;
	
	public Server(int port, int interval, boolean heavyUpdate,
			int initialUpdate) throws IOException
	{
		super(getPort());
		html = getHtml();
		dataUpdater = new DataUpdater(interval, heavyUpdate, initialUpdate);
		start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
		System.out.println(FormattedOutput.get("Server started on port " + getPort() + "."));
	}
	
	@Override
    public Response serve(IHTTPSession session)
	{
		Request req = sessionToRequest(session);
		if (req != null)
		{
			if (req == null || req.getType() == null)
			{
				return newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST,
						"application/json", io.github.kraowx.shibbyappserver
						.net.Response.INVALID_REQUEST.toString());
			}
			if (req.getType() == RequestType.VERSION)
			{
				return newFixedLengthResponse(NanoHTTPD.Response.Status.OK,
						"application/json", io.github.kraowx.shibbyappserver
						.net.Response.VERSION.toString());
			}
			if (!checkVersion(req))
			{
				return newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST,
						"application/json", io.github.kraowx.shibbyappserver
							.net.Response.OUTDATED_CLIENT.toString());
			}
			io.github.kraowx.shibbyappserver.net.Response resp =
					RequestHandler.getResponse(req, dataUpdater);
			ResponseType type = resp.getType();
			if (type == ResponseType.INVALID_REQUEST || type == ResponseType.BAD_ACCOUNT ||
					type == ResponseType.FEATURE_NOT_SUPPORTED || type == ResponseType.TOO_MANY_REQUESTS)
			{
				return newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST,
						"application/json", resp.toString());
			}
			else
			{
				return newFixedLengthResponse(NanoHTTPD.Response.Status.OK,
						"application/json", resp.toString());
			}
		}
		return newFixedLengthResponse(html);
    }
	
	private Request sessionToRequest(IHTTPSession session)
	{
		Map<String, String> parms = session.getParms();
		String requestTypeParm = parms.containsKey("type") ?
				parms.get("type") : null;
		if (requestTypeParm != null)
		{
			RequestType reqType = getRequestType(requestTypeParm);
			JSONObject data = new JSONObject(parms);
			data.remove("type");
			Request req = new Request(reqType, data.toString());
			req.setData(data);
			return req;
		}
		return null;
	}
	
	private static RequestType getRequestType(String requestStr)
	{
		for (RequestType type : RequestType.values())
		{
			if (type.toString().equals(requestStr.toUpperCase()))
			{
				return type;
			}
		}
		return null;
	}
	
	private boolean checkVersion(Request request)
	{
		String clientVersion = request.getData().has("version") ?
				request.getData().getString("version") : null;
		return clientVersion != null && clientVersion.equals(VERSION);
	}
	
	private String getHtml()
	{
		StringBuilder html = new StringBuilder();
		BufferedReader reader;
		try
		{
			reader = new BufferedReader(new FileReader(HTML_PATH));
			String line = "";
			while ((line = reader.readLine()) != null)
			{
				html.append(line);
			}
			reader.close();
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		return html.toString();
	}
	
	private static int getPort()
	{
		int port;
		try
		{
			port = Integer.parseInt(System.getenv("PORT"));
		}
		catch (NumberFormatException nfe)
		{
			port = Main.DEFAULT_PORT;
		}
		return port;
	}
}
