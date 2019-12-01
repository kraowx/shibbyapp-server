package io.github.kraowx.shibbydexserver.models;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class ShibbyTag
{
	private String name;
	private List<ShibbyFile> files;
	
	public ShibbyTag(String name)
	{
		init(name, null);
	}
	
	public ShibbyTag(String name, ShibbyFile[] files)
	{
		init(name, files);
	}
	
	private void init(String name, ShibbyFile[] files)
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
	}
	
	public String toJSON()
	{
		JSONObject json = new JSONObject();
		json.put("name", name);
		JSONArray arr = new JSONArray();
		for (ShibbyFile file : files)
		{
			arr.put(file.toJSON());
		}
		json.put("files", arr.toString());
		return json.toString();
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
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
	
	@Override
	public String toString()
	{
		return name;
	}
}
