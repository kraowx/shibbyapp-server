package io.github.kraowx.shibbyappserver.models;

import java.util.List;

public class ShibbyTagFile
{
	private String id;
	private List<String> tags;
	
	public ShibbyTagFile(ShibbyFile file)
	{
		this.id = file.getId();
		this.tags = file.getTags();
	}
	
	public String getId()
	{
		return id;
	}
	
	public void setId(String id)
	{
		this.id = id;
	}
	
	public List<String> getTags()
	{
		return tags;
	}
	
	public void setTags(List<String> tags)
	{
		this.tags = tags;
	}
}
