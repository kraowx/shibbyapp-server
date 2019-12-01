package io.github.kraowx.shibbydexserver.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.json.JSONArray;

import io.github.kraowx.shibbydexserver.DataUpdater;

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
			Request req;
			String data;
			while ((data = reader.readLine()) != null)
			{
				req = Request.fromJSON(data);
				Response resp;
				switch (req.getType())
				{
					case ALL:
						resp = new Response(ResponseType.ALL,
								dataUpdater.getAllJSON());
						break;
					case FILES:
						resp = new Response(ResponseType.FILES,
								dataUpdater.getFilesJSON());
						break;
					case TAGS:
						resp = new Response(ResponseType.TAGS,
								dataUpdater.getTagsJSON());
						break;
					case SERIES:
						resp = new Response(ResponseType.SERIES,
								dataUpdater.getSeriesJSON());
						break;
					default:
						resp = new Response(ResponseType.INVALID_REQUEST,
								new JSONArray());
						break;
				}
				writer.println(resp.toString());
			}
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
}
