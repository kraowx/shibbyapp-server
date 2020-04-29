package io.github.kraowx.shibbyappserver.models;

import org.json.JSONArray;
import org.json.JSONObject;

public class HotspotArray
{
	private String fileId;
	private Hotspot[] hotspots;
	private int version;
	
	public HotspotArray(String fileId, Hotspot[] hotspots, int version)
	{
		this.fileId = fileId;
		this.hotspots = hotspots;
		this.version = version;
	}
	
	public String getFileId()
	{
		return fileId;
	}
	
	public void setFileId(String fileId)
	{
		this.fileId = fileId;
	}
	
	public Hotspot[] getHotspots()
	{
		return hotspots;
	}
	
	public void setHotspots(Hotspot[] hotspots)
	{
		this.hotspots = hotspots;
	}
	
	public int getVersion()
	{
		return version;
	}
	
	public void setVersion(int version)
	{
		this.version = version;
	}
	
	public String toJSON()
	{
		JSONObject obj = new JSONObject();
		obj.put("id", fileId);
		JSONArray arr = new JSONArray();
		for (int i = 0; i < hotspots.length; i++)
		{
			arr.put(new JSONObject(hotspots[i].toJSON()));
		}
		obj.put("hotspots", arr);
		obj.put("version", version);
		return obj.toString();
	}
	
	public HotspotArray fromJSON(JSONObject json)
	{
		HotspotArray hotspotArray = new HotspotArray(null, null, -1);
		if (json.has("id"))
		{
			hotspotArray.fileId = json.getString("id");
		}
		if (json.has("hotspots"))
		{
			JSONArray arr = json.getJSONArray("hotspots");
			hotspotArray.hotspots = new Hotspot[arr.length()];
			for (int i = 0; i < arr.length(); i++)
			{
				hotspotArray.hotspots[i] = Hotspot.fromJSON(arr.getJSONObject(i));
			}
		}
		if (json.has("version"))
		{
			hotspotArray.version = json.getInt("version");
		}
		return hotspotArray;
	}
}
