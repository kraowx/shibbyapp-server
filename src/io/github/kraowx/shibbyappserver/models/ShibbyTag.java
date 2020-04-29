package io.github.kraowx.shibbyappserver.models;

import java.util.ArrayList;
import java.util.List;

/*
 * This class is used as a lighter weight implementation of the
 * ShibbyFileArray class to internally store and organize tags.
 * This implementation uses about 65% less memory.
 */
public class ShibbyTag
{
	private String name;
	private List<ShibbyTagFile> files;
	
	public ShibbyTag(String name, ShibbyFile initialFile)
	{
		this.name = name;
		this.files = new ArrayList<ShibbyTagFile>();
		files.add(new ShibbyTagFile(initialFile));
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public List<ShibbyTagFile> getFiles()
	{
		return files;
	}
	
	public void setFiles(List<ShibbyTagFile> files)
	{
		this.files = files;
	}
	
	public void addFile(ShibbyFile file)
	{
		files.add(new ShibbyTagFile(file));
	}
	
	public int getFileCount()
	{
		return files.size();
	}
}
