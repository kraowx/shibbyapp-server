package io.github.kraowx.shibbyappserver.net;

import org.json.JSONObject;

public class Response
{
	private ResponseType respType;
	private JSONObject data;
	
	public Response(ResponseType respType, JSONObject data)
	{
		this.respType = respType;
		this.data = data;
		this.data.put("type", respType);
	}
	
	public static Response INVALID_REQUEST =
			new Response(ResponseType.INVALID_REQUEST, new JSONObject());
	public static Response OUTDATED_CLIENT =
			new Response(ResponseType.OUTDATED_CLIENT, new JSONObject());
	public static Response VERSION = getVersionResponse();
	
	public static Response fromJSON(String json)
	{
		Response resp = new Response(null, null);
		JSONObject obj = new JSONObject(json);
		if (obj.has("type"))
		{
			resp.respType = formatResponseType(obj.getString("type"));
		}
		if (obj.has("data"))
		{
			resp.data = obj.getJSONObject("data");
		}
		return resp;
	}
	
	public JSONObject toJSON()
	{
		return data;
	}
	
	public ResponseType getType()
	{
		return respType;
	}
	
	public JSONObject getData()
	{
		return data;
	}
	
	private static ResponseType formatResponseType(String responseStr)
	{
		for (ResponseType type : ResponseType.values())
		{
			if (type.toString().equals(responseStr))
			{
				return type;
			}
		}
		return null;
	}
	
	private static Response getVersionResponse()
	{
		JSONObject obj = new JSONObject();
		obj.put("data", Server.VERSION);
		return new Response(ResponseType.VERSION, obj);
	}
	
	@Override
	public String toString()
	{
		return toJSON().toString();
	}
}
