package io.github.kraowx.shibbyappserver.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.json.JSONArray;
import org.json.JSONObject;

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
				JSONArray arr = new JSONArray();
				arr.put(Server.VERSION);
				return new Response(ResponseType.VERSION, arr);
			case VERIFY_PATREON_ACCOUNT:
				return getVerifiedAccountResponse(request);
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
			case PATREON_FILES:
				return getPatreonFilesResponse(request);
		}
		return new Response(ResponseType.INVALID_REQUEST,
				new JSONArray());
	}
	
	private Response getVerifiedAccountResponse(Request request)
	{
		if (!dataUpdater.isPatreonEnabled())
		{
			return new Response(ResponseType.FEATURE_NOT_SUPPORTED, null);
		}
		JSONObject verified = new JSONObject();
		if (request.getData() != null)
		{
			JSONObject data = new JSONObject(request.getData());
			verified.put("verified", dataUpdater.isAccountVerified(
					data.getString("email"), data.getString("password")));
		}
		else
		{
			verified.put("verified", false);
		}
		JSONArray arr = new JSONArray();
		arr.put(verified);
		return new Response(ResponseType.VERIFY_PATREON_ACCOUNT, arr);
	}
	
	private Response getPatreonFilesResponse(Request request)
	{
		if (!dataUpdater.isPatreonEnabled())
		{
			return new Response(ResponseType.FEATURE_NOT_SUPPORTED, null);
		}
		boolean verified = false;
		if (request.getData() != null)
		{
			JSONObject data = new JSONObject(request.getData());
			if (dataUpdater.getVerifiedPatreonEmails().contains(data.getString("email")))
			{
				verified = true;
			}
			else
			{
				verified = dataUpdater.isAccountVerified(
						data.getString("email"), data.getString("password"));
			}
		}
		if (verified)
		{
			return new Response(ResponseType.PATREON_FILES,
					dataUpdater.getPatreonJSON());
		}
		else
		{
			JSONArray arr = new JSONArray();
			JSONObject verifiedJson = new JSONObject();
			verifiedJson.put("verified", verified);
			arr.put(verifiedJson);
			return new Response(ResponseType.VERIFY_PATREON_ACCOUNT, arr);
		}
	}
}
