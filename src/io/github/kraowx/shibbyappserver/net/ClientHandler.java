package io.github.kraowx.shibbyappserver.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.json.JSONArray;

import io.github.kraowx.shibbyappserver.DataUpdater;
import io.github.kraowx.shibbyappserver.tools.FormattedOutput;

public class ClientHandler implements Runnable
{
	private PrintWriter writer;
	private BufferedReader reader;
	private DataUpdater dataUpdater;
	
	public ClientHandler(PrintWriter writer,
			BufferedReader reader, DataUpdater dataUpdater)
	{
		this.writer = writer;
		this.reader = reader;
		this.dataUpdater = dataUpdater;
	}
	
	@Override
	public void run()
	{
		try
		{
			Request req = null;
			String data = null;
			while ((data = reader.readLine()) != null)
			{
				req = Request.fromJSON(data);
				System.out.println(FormattedOutput.get("Client requested " +
						req.getType()));
				Response resp = getResponse(req);
				writer.println(resp.toString());
				System.out.println(FormattedOutput.get("Responded to client with " +
						resp.getType()));
			}
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
	
	private Response getResponse(Request request)
	{
		switch (request.getType())
		{
			case VERSION:
				return new Response(ResponseType.VERSION,
						new JSONArray(Server.VERSION));
			case ALL:
				return new Response(ResponseType.ALL,
						dataUpdater.getAllJSON());
			case FILES:
				return new Response(ResponseType.FILES,
						dataUpdater.getFilesJSON());
			case TAGS:
				return new Response(ResponseType.TAGS,
						dataUpdater.getTagsJSON());
			case SERIES:
				return new Response(ResponseType.SERIES,
						dataUpdater.getSeriesJSON());
		}
		return new Response(ResponseType.INVALID_REQUEST,
				new JSONArray());
	}
}
