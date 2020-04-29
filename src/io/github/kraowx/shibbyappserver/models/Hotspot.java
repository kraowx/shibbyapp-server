package io.github.kraowx.shibbyappserver.models;

import org.json.JSONObject;

public class Hotspot
{
	private long startTime, endTime;
	private long duration;
	
	public Hotspot(long startTime, long endTime)
	{
		this.startTime = startTime;
		this.endTime = endTime;
		this.duration = (int)(endTime - startTime);
	}
	
	public long getStartTime()
	{
		return startTime;
	}
	
	public void setStartTime(long startTime)
	{
		this.startTime = startTime;
	}
	
	public long getEndTime()
	{
		return endTime;
	}
	
	public void setEndTime(long endTime)
	{
		this.endTime = endTime;
	}
	
	public long getDuration()
	{
		return duration;
	}
	
	public void setDuration(long duration)
	{
		this.duration = duration;
	}
	
	public String toJSON()
	{
		JSONObject obj = new JSONObject();
		obj.put("startTime", startTime);
		obj.put("endTime", endTime);
		obj.put("duration", duration);
		return obj.toString();
	}
	
	public static Hotspot fromJSON(JSONObject json)
	{
		Hotspot hotspot = new Hotspot(0, 0);
		if (json.has("startTime"))
		{
			hotspot.startTime = json.getLong("startTime");
		}
		if (json.has("endTime"))
		{
			hotspot.endTime = json.getLong("endTime");
		}
		if (json.has("duration"))
		{
			hotspot.duration = json.getLong("duration");
		}
		return hotspot;
	}
	
	@Override
	public String toString()
	{
		return toJSON();
	}
}
