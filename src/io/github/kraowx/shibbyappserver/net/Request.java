package io.github.kraowx.shibbyappserver.net;

import org.json.JSONObject;

public class Request
{
	private RequestType reqType;
	private String data;
	
	public Request(RequestType reqType, String data)
	{
		this.reqType = reqType;
		this.data = data;
	}
	
	public static Request fromJSON(String json)
	{
		Request req = new Request(null, null);
		JSONObject obj = new JSONObject(json);
		if (obj.has("type"))
		{
			req.reqType = formatRequestType(obj.getString("type"));
		}
		if (obj.has("data"))
		{
			req.data = obj.getString("data");
		}
		return req;
	}
	
	public static Request all()
	{
		return new Request(RequestType.ALL, null);
	}
	
	public static Request files()
	{
		return new Request(RequestType.FILES, null);
	}
	
	public static Request tags()
	{
		return new Request(RequestType.TAGS, null);
	}
	
	public static Request series()
	{
		return new Request(RequestType.SERIES, null);
	}
	
	public JSONObject toJSON()
	{
		JSONObject json = new JSONObject();
		json.put("type", reqType.toString());
		json.put("data", data);
		return json;
	}
	
	public RequestType getType()
	{
		return reqType;
	}
	
	public String getData()
	{
		return data;
	}
	
	private static RequestType formatRequestType(String requestStr)
	{
		for (RequestType type : RequestType.values())
		{
			if (type.toString().equals(requestStr))
			{
				return type;
			}
		}
		return null;
	}
	
	@Override
	public String toString()
	{
		return toJSON().toString();
	}
}
