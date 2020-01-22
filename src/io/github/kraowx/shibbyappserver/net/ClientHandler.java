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
	
	/*
	 * Process a request from the client into a response action.
	 */
	private Response getResponse(Request request)
	{
		switch (request.getType())
		{
			case VERSION:
				JSONArray arr = new JSONArray();
				arr.put(Server.VERSION);
				// No client verification required
				return new Response(ResponseType.VERSION, arr);
			case VERIFY_PATREON_ACCOUNT:
				return getVerifiedAccountResponse(request);
			case ALL:
				return getAllResponse(request);
			case FILES:
				// No client verification required
				return new Response(ResponseType.FILES,
						dataUpdater.getFilesJSON());
			case TAGS:
				return getTagsResponse(request);
			case SERIES:
				// No client verification required
				return new Response(ResponseType.SERIES,
						dataUpdater.getSeriesJSON());
			case PATREON_FILES:
				return getPatreonFilesResponse(request);
		}
		return new Response(ResponseType.INVALID_REQUEST,
				new JSONArray());
	}
	
	/*
	 * Attempts to check if the client's credentials (email + password)
	 * belong to a Patreon account that exists and that has an
	 * active pledge to Shibby. Returns a VERIFY_PATREON_ACCOUNT with
	 * data {"verified":true} if verified, or a VERIFY_PATREON_ACCOUNT with
	 * data {"verified":false} if not verified. May also return a FEATURE_NOT_SUPPORTED
	 * response if the patreonEnabled is false for the server.
	 */
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
	
	/*
	 * Returns a PATREON_FILES response with data containing the Patreon files
	 * only if the client's credentials can be verified. Otherwise, may return
	 * a number of other responses: FEATURE_NOT_SUPPORTED, VERIFY_PATREON_ACCOUNT
	 * (with verified=false), TOO_MANY_REQUESTS, or BAD_ACCOUNT
	 */
	private Response getPatreonFilesResponse(Request request)
	{
		if (!dataUpdater.isPatreonEnabled())
		{
			return new Response(ResponseType.FEATURE_NOT_SUPPORTED, null);
		}
		int verified = 0;
		if (request.getData() != null)
		{
			JSONObject data = new JSONObject(request.getData());
			verified = dataUpdater.isAccountVerified(
					data.getString("email"), data.getString("password"));
		}
		if (verified == 1)
		{
			return new Response(ResponseType.PATREON_FILES,
					dataUpdater.getPatreonJSON());
		}
		else if (verified == 2)
		{
			JSONArray arr = new JSONArray();
			JSONObject verifiedJson = new JSONObject();
			verifiedJson.put("verified", verified);
			arr.put(verifiedJson);
			return new Response(ResponseType.VERIFY_PATREON_ACCOUNT, arr);
		}
		else if (verified == 3)
		{
			return new Response(ResponseType.TOO_MANY_REQUESTS, null);
		}
		else
		{
			return new Response(ResponseType.BAD_ACCOUNT, null);
		}
	}
	
	/*
	 * Returns a TAGS response containing all tag data including Patreon
	 * files only if the client's credentials can be verified. Otherwise,
	 * will return a TAGS response *without* including Patreon files.
	 */
	private Response getTagsResponse(Request request)
	{
		int verified = 0;
		if (request.getData() != null)
		{
			JSONObject data = new JSONObject(request.getData());
			verified = dataUpdater.isAccountVerified(
					data.getString("email"), data.getString("password"));
		}
		if (verified == 1)
		{
			return new Response(ResponseType.TAGS,
					dataUpdater.getTagsWithPatreonJSON());
		}
		else
		{
			return new Response(ResponseType.TAGS,
					dataUpdater.getTagsJSON());
		}
	}
	
	/*
	 * Returns an ALL response containing all available data including
	 * Patreon files only if the client's credentials can be verified.
	 * Otherwise, will return an ALL response *without* including Patreon files.
	 */
	private Response getAllResponse(Request request)
	{
		int verified = 0;
		if (request.getData() != null)
		{
			JSONObject data = new JSONObject(request.getData());
			verified = dataUpdater.isAccountVerified(
					data.getString("email"), data.getString("password"));
		}
		return new Response(ResponseType.ALL,
				dataUpdater.getAllJSON(verified == 1));
	}
}
