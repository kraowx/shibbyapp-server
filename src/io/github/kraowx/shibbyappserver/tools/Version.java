package io.github.kraowx.shibbyappserver.tools;

public class Version
{
	private int semVer;
	private String name;
	
	public Version(String name)
	{
		this.semVer = parseVersion(name);
		this.name = name;
	}
	
	public String getName()
	{
		return name;
	}
	
	public boolean greater(Version other)
	{
		return this.semVer > other.semVer;
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof Version)
		{
			return this.semVer == ((Version)other).semVer;
		}
		return false;
	}
	
	@Override
	public String toString()
	{
		return name;
	}
	
	/*
	 * Converts a version string in semantic version format
	 * ("vx.x.x" or "x.x.x" or "xxx") into its equivalent
	 * integer value.
	 */
	private int parseVersion(String verStr)
	{
		int semVer = -1;
		if (verStr != null)
		{
			verStr = verStr.replace("v", "");
			String[] semVerArr = verStr.split("\\.");
			String semVerStr = "";
			for (int i = 0; i < semVerArr.length; i++)
			{
				semVerStr += semVerArr[i];
			}
			try
			{
				semVer = Integer.parseInt(semVerStr);
			}
			catch (NumberFormatException nfe)
			{
				semVer = -1;
			}
		}
		return semVer;
	}
}
