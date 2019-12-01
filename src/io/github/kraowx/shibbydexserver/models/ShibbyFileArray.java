package io.github.kraowx.shibbydexserver.models;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class ShibbyFileArray
{
	private String name;
	private List<ShibbyFile> files;
	
	public ShibbyFileArray(String name)
	{
		init(name, null);
	}
	
	public ShibbyFileArray(String name, ShibbyFile[] files)
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
	
	public JSONObject toJSON()
	{
		JSONObject json = new JSONObject();
		json.put("name", name);
		json.put("fileCount", files.size());
		JSONArray arr = new JSONArray();
		for (ShibbyFile file : files)
		{
			arr.put(file.toJSON());
		}
		json.put("files", arr);
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
