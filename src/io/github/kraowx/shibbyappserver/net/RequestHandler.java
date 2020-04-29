package io.github.kraowx.shibbyappserver.net;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.kraowx.shibbyappserver.DataUpdater;

public class RequestHandler
{	
	/*
	 * Process a request from the client into a response action.
	 */
	public static Response getResponse(Request request, DataUpdater dataUpdater)
	{
		switch (request.getType())
		{
			case VERSION:
				JSONObject obj = new JSONObject();
				obj.put("data", Server.VERSION);
				// No client verification required
				return new Response(ResponseType.VERSION, obj);
			case VERIFY_PATREON_ACCOUNT:
				return getVerifiedAccountResponse(request, dataUpdater);
			case ALL:
				return getAllResponse(request, dataUpdater);
			case FILES:
				// No client verification required
				return new Response(ResponseType.FILES,
						getDataObject(dataUpdater.getFilesJSON()));
			case TAGS:
				return getTagsResponse(request, dataUpdater);
			case SERIES:
				// No client verification required
				return new Response(ResponseType.SERIES,
						getDataObject(dataUpdater.getSeriesJSON()));
			case PATREON_FILES:
				return getPatreonFilesResponse(request, dataUpdater);
			case HOTSPOTS:
				return new Response(ResponseType.HOTSPOTS,
						getDataObject(dataUpdater.getHotspotsJSON()));
		}
		return new Response(ResponseType.INVALID_REQUEST, new JSONObject());
	}
	
	/*
	 * Attempts to check if the client's credentials (email + password)
	 * belong to a Patreon account that exists and that has an
	 * active pledge to Shibby. Returns a VERIFY_PATREON_ACCOUNT with
	 * {"data": true} if verified, or a VERIFY_PATREON_ACCOUNT with
	 * {"data": false} if not verified. May also return a FEATURE_NOT_SUPPORTED
	 * response if the patreonEnabled is false for the server.
	 */
	private static Response getVerifiedAccountResponse(Request request,
			DataUpdater dataUpdater)
	{
		if (!dataUpdater.isPatreonEnabled())
		{
			return new Response(ResponseType.FEATURE_NOT_SUPPORTED, new JSONObject());
		}
		boolean verified = false;
		JSONObject data = request.getData();
		if (data.has("email") && data.has("password"))
		{
			verified = dataUpdater.isAccountVerified(
					data.getString("email"), data.getString("password")) == 1;
		}
		else
		{
			verified = false;
		}
		return new Response(ResponseType.VERIFY_PATREON_ACCOUNT,
				getDataObject(verified));
	}
	
	/*
	 * Returns a PATREON_FILES response with data containing the Patreon files
	 * only if the client's credentials can be verified. Otherwise, may return
	 * a number of other responses: FEATURE_NOT_SUPPORTED (no data),
	 * VERIFY_PATREON_ACCOUNT (no data), TOO_MANY_REQUESTS (no data),
	 * or BAD_ACCOUNT (no data).
	 */
	private static Response getPatreonFilesResponse(Request request,
			DataUpdater dataUpdater)
	{
		if (!dataUpdater.isPatreonEnabled())
		{
			return new Response(ResponseType.FEATURE_NOT_SUPPORTED, new JSONObject());
		}
		int verified = 0;
		JSONObject data = request.getData();
		if (data.has("email") && data.has("password"))
		{
			verified = dataUpdater.isAccountVerified(
					data.getString("email"), data.getString("password"));
		}
		if (verified == 1)
		{
			/* Account is valid */
			return new Response(ResponseType.PATREON_FILES,
					getDataObject(dataUpdater.getPatreonJSON()));
		}
		else if (verified == 2)
		{
			/* Account requires email verification */
			return new Response(ResponseType.VERIFY_PATREON_ACCOUNT,
					new JSONObject());
		}
		else if (verified == 3)
		{
			/* API requests from the account are being throttled for 10 mins */
			return new Response(ResponseType.TOO_MANY_REQUESTS,
					new JSONObject());
		}
		else
		{
			/* Account is invalid */
			return new Response(ResponseType.BAD_ACCOUNT, new JSONObject());
		}
	}
	
	/*
	 * Returns a TAGS response containing all tag data including Patreon
	 * files only if the client's credentials can be verified. Otherwise,
	 * will return a TAGS response *without* including Patreon files.
	 */
	private static Response getTagsResponse(Request request, DataUpdater dataUpdater)
	{
		int verified = 0;
		JSONObject data = request.getData();
		if (data.has("email") && data.has("password"))
		{
			verified = dataUpdater.isAccountVerified(
					data.getString("email"), data.getString("password"));
		}
		if (verified == 1)
		{
			/* Soundgasm + Patreon tags */
			return new Response(ResponseType.TAGS,
					getDataObject(dataUpdater.getTagsWithPatreonJSON()));
		}
		else
		{
			/* Soundgasm tags only */
			return new Response(ResponseType.TAGS,
					getDataObject(dataUpdater.getTagsJSON()));
		}
	}
	
	/*
	 * Returns an ALL response containing all available data including
	 * Patreon files only if the client's credentials can be verified.
	 * Otherwise, will return an ALL response *without* including Patreon files.
	 */
	private static Response getAllResponse(Request request, DataUpdater dataUpdater)
	{
		int verified = 0;
		JSONObject data = request.getData();
		if (data.has("email") && data.has("password"))
		{
			verified = dataUpdater.isAccountVerified(
					data.getString("email"), data.getString("password"));
		}
		return new Response(ResponseType.ALL,
				getDataObject(dataUpdater.getAllJSON(verified == 1)));
	}
	
	/*
	 * Wraps a json array object in a json object under the name "data".
	 * Placed directly into the json of a response object.
	 */
	private static JSONObject getDataObject(JSONArray arr)
	{
		JSONObject obj = new JSONObject();
		obj.put("data", arr);
		return obj;
	}
	
	/*
	 * Wraps a json object in another json object under the name "data".
	 * Placed directly into the json of a response object.
	 */
	private static JSONObject getDataObject(JSONObject obj)
	{
		JSONObject objData = new JSONObject();
		objData.put("data", obj);
		return objData;
	}
	
	/*
	 * Wraps a integer value in a json object under the name "data".
	 * Placed directly into the json of a response object.
	 */
	private static JSONObject getDataObject(int val)
	{
		JSONObject obj = new JSONObject();
		obj.put("data", val);
		return obj;
	}
	
	/*
	 * Wraps a boolean value in a json object under the name "data".
	 * Placed directly into the json of a response object.
	 */
	private static JSONObject getDataObject(boolean val)
	{
		JSONObject obj = new JSONObject();
		obj.put("data", val);
		return obj;
	}
}

