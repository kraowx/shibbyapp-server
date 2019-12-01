package io.github.kraowx.shibbyappserver.models;

import java.util.ArrayList;
import java.util.List;

public class ShibbySeries
{
	private String name;
	private List<ShibbyFile> files;
	
	public ShibbySeries(String name)
	{
		init(name, null);
	}
	
	public ShibbySeries(String name, ShibbyFile[] files)
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
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
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
}
