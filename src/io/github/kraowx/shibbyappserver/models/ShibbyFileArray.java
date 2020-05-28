package io.github.kraowx.shibbyappserver.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class ShibbyFileArray
{
	private String name, description;
	private List<ShibbyFile> files;
	private Map<String, String> extraData;
	
	public ShibbyFileArray(String name)
	{
		init(name, null, null);
	}
	
	public ShibbyFileArray(String name, ShibbyFile[] files,
			Map<String, String> extraData)
	{
		init(name, files, extraData);
	}
	
	private void init(String name, ShibbyFile[] files,
			Map<String, String> extraData)
	{
		this.name = name;
		this.files = new ArrayList<ShibbyFile>();
		if (files != null)
		{
			for (ShibbyFile file : files)
			{
				this.files.add(file);
			}
		}
		this.extraData = extraData != null ? extraData :
			new HashMap<String, String>();
	}
	
	public JSONObject toJSON()
	{
		JSONObject json = new JSONObject();
		json.put("name", name);
		if (description != null)
		{
			json.put("description", description);
		}
		json.put("fileCount", files.size());
		JSONArray arr = new JSONArray();
		for (ShibbyFile file : files)
		{
			arr.put(file.toJSON());
		}
		json.put("files", arr);
		JSONObject extras = new JSONObject();
		for (String key : extraData.keySet())
		{
			extras.put(key, extraData.get(key));
		}
		json.put("extras", extras);
		return json;
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public String getDescription()
	{
		return description;
	}
	
	public void setDescription(String description)
	{
		this.description = description;
	}
	
	public int getFileCount()
	{
		return files.size();
	}
	
	public List<ShibbyFile> getFiles()
	{
		return files;
	}
	
	public void setFiles(List<ShibbyFile> files)
	{
		this.files = files;
	}
	
	public boolean addFile(ShibbyFile file)
	{
		if (!files.contains(file))
		{
			files.add(file);
			return true;
		}
		return false;
	}
	
	public boolean removeFile(ShibbyFile file)
	{
		if (files.contains(file))
		{
			files.remove(file);
			return true;
		}
		return false;
	}
	
	public int compareTo(ShibbyFileArray other)
	{
		int fc = getFileCount();
		int ofc = other.getFileCount();
		if (fc > ofc)
		{
			return 1;
		}
		else if (fc < ofc)
		{
			return -1;
		}
		return 0;
	}
	
	@Override
	public String toString()
	{
		return name;
	}
}
