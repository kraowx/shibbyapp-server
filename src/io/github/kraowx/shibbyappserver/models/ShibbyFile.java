package io.github.kraowx.shibbyappserver.models;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class ShibbyFile
{
	private String name, shortName, id,
		link, description, type;
	private long duration;
	private List<String> tags;
	private Map<String, String> extraData;
	
	public ShibbyFile(String name, String link,
			String description, String type)
	{
		init(name, null, null, link, description, type, null);
	}
	
	public ShibbyFile(String name, String id,
			List<String> tags, String link, String description,
			String type, Map<String, String> extraData)
	{
		init(name, id, tags, link, description, type, extraData);
	}
	
	private void init(String name, String id,
			List<String> tags, String link, String description,
			String type, Map<String, String> extraData)
	{
		this.name = name;
		this.shortName = getShortNameFromName(name);
		if (id == null && name != null)
		{
			createIdFromName();
		}
		if (tags != null)
		{
			this.tags = tags;
		}
		else
		{
			this.tags = getTagsFromName();
		}
		this.link = link;
		this.description = description;
		this.type = type;
		this.extraData = extraData != null ? extraData :
			new HashMap<String, String>();
	}
	
	public JSONObject toJSON()
	{
		JSONObject json = new JSONObject();
		json.put("name", name);
		json.put("shortName", shortName);
		JSONArray tagsJson = new JSONArray();
		for (String tag : tags)
		{
			tagsJson.put(tag);
		}
		json.put("tags", tagsJson);
		json.put("link", link);
		json.put("description", description);
		if (duration != 0)
		{
			json.put("duration", duration);
		}
		json.put("type", type);
		JSONObject extras = new JSONObject();
		for (String key : extraData.keySet())
		{
			extras.put(key, extraData.get(key));
		}
		json.put("extras", extras);
		return json;
	}
	
	public static ShibbyFile fromJSON(String jsonStr)
    {
        ShibbyFile file = new ShibbyFile(null, null, null, null);
        JSONObject json = new JSONObject(jsonStr);
        file.name = json.getString("name");
        if (json.has("shortName"))
        {
        	file.shortName = json.getString("shortName");
        }
        else
        {
        	file.shortName = file.getShortNameFromName(file.name);
        }
        if (!json.has("id") && file.name != null)
        {
            file.createIdFromName();
        }
        else
        {
        	file.id = json.getString("id");
        }
        if (!json.has("tags") && file.name != null)
        {
        	file.tags = file.getTagsFromName();
        }
        else
        {
	        file.tags = new ArrayList<String>();
	        for (Object tag : json.getJSONArray("tags"))
	        {
	        	file.tags.add((String)tag);
	        }
        }
        if (json.has("link"))
        {
            file.link = json.getString("link");
        }
        else if (json.has("links"))
        {
            file.link = json.getJSONArray("links").getString(0);
        }
        file.description = json.getString("description");
        if (json.has("duration"))
        {
        	file.duration = json.getLong("duration");
        }
        if (!json.has("type"))
        {
        	file.type = "";
        }
        else
        {
        	file.type = json.getString("type");
        }
        file.extraData = new HashMap<String, String>();
        if (json.has("extras"))
        {
	        JSONObject extras = json.getJSONObject("extras");
	        for (String key : extras.keySet())
	        {
	        	file.extraData.put(key, extras.getString(key));
	        }
        }
        return file;
    }
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public String getShortName()
	{
		return shortName;
	}
	
	public void setShortName(String shortName)
	{
		this.shortName = shortName;
	}
	
	public String getId()
	{
		return id;
	}
	
	public void setId(String id)
	{
		this.id = id;
	}
	
	public String getLink()
	{
		return link;
	}
	
	public void setLink(String link)
	{
		this.link = link;
	}
	
	public String getType()
	{
		return type;
	}
	
	public void setType(String type)
	{
		this.type = type;
	}
	
	public String getDescription()
	{
		return description;
	}
	
	public void setDescription(String description)
	{
		this.description = description;
	}
	
	public List<String> getTags()
	{
		return tags;
	}
	
	public void setTags(List<String> tags)
	{
		this.tags = tags;
	}
	
	public long getDuration()
	{
		return duration;
	}
	
	public void setDuration(long duration)
	{
		this.duration = duration;
	}
	
	public List<String> getTagsFromName()
	{
		List<String> tags = new ArrayList<String>();
		if (name != null)
		{
			StringBuilder tag = new StringBuilder();
			boolean in = false;
			for (int i = 0; i < name.length(); i++)
			{
				char c = name.charAt(i);
				if (c == ']')
				{
					tags.add(tag.toString());
					tag.setLength(0);
					in = false;
				}
				else if (c == '[')
				{
					in = true;
				}
				else if (c != '[' && in)
				{
					tag.append(c);
				}
			}
		}
		return tags;
	}
	
	public String getShortNameFromName(String name)
	{
		if (name == null)
		{
			return null;
		}
		String tags = "";
		char[] chars = name.toCharArray();
		boolean in = true;
		char c;
		for (int i = 0; i < chars.length; i++)
		{
			c = chars[i];
			if (c == '[')
			{
				if (in && i != 0)
				{
					tags = "";
					break;
				}
				in = true;
			}
			else if (c == ']' || c == ')')
			{
				in = false;
			}
			else if (!in && c != ' ' && !hasOr(chars, i))
			{
				name = name.substring(i, name.length()-1);
				break;
			}
			// Handle leading tags in the form "[xxx] or [xxx]"
			if (hasOr(chars, i) && i+1 == 'r')
			{
				chars[i] = 'o';
				chars[i+1] = 'r';
			}
			else
			{
				tags += c;
			}
		}
		if (!tags.endsWith(" "))
		{
			tags += " ";
		}
		if (!tags.contains("[") && !tags.contains("]"))
		{
			tags = "";
		}
		// Remove right tags
		int rightIndex = name.indexOf('[');
		if (rightIndex != -1)
		{
			name = name.substring(0, rightIndex);
			if (name.length() > 0 && name.charAt(name.length()-1) == ' ')
			{
				name = name.substring(0, name.length()-1);
			}
		}
		return removeScriptFillTag(tags) + name;
	}
	
	/*
	 * Checks if a string contains an "or" at the index i or the index i+1.
	 */
	private static boolean hasOr(char[] chars, int i)
	{
		return (i+1 < chars.length && chars[i] == 'o' && chars[i+1] == 'r') ||
				(i-1 > 0 && chars[i-1] == 'o' && chars[i] == 'r');
	}
	
	private static String removeScriptFillTag(String tags)
	{
		int index = tags.toLowerCase().indexOf("script fill");
		int left = index-1;
		if (left > 0 && tags.charAt(left-1) == ' ')
		{
			left--;
		}
		if (index != -1)
		{
			int right = index;
			while (right < tags.length() && tags.charAt(right) != ']')
			{
				right++;
			}
			String tag = tags.substring(left, ++right);
			return tags.replace(tag, "");
		}
		return tags;
	}
	
	private void createIdFromName()
	{
		try
		{
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
	        byte[] hash = digest.digest(name.getBytes(StandardCharsets.UTF_8));
	        StringBuffer hexString = new StringBuffer();

	        for (int i = 0; i < hash.length; i++)
	        {
	            String hex = Integer.toHexString(0xFF & hash[i]);
	            if (hex.length() == 1)
	            {
	            	hexString.append('0');
	            }
	            hexString.append(hex);
	        }
	        
	        id = hexString.toString();
		}
		catch (NoSuchAlgorithmException nsae)
		{
			nsae.printStackTrace();
			id = "";
		}
	}
	
	@Override
	public String toString()
	{
		return name;
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof ShibbyFile)
		{
			return this.toJSON().toString().equals(
					((ShibbyFile)other).toJSON().toString());
		}
		return false;
	}
}
