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
	private String name, shortName, id, link, description;
	private List<String> tags;
	private Map<String, String> extraData;
	
	public ShibbyFile(String name, String link,
			String description)
	{
		init(name, null, null, link, description, null);
	}
	
	public ShibbyFile(String name, String id,
			List<String> tags, String link, String description,
			Map<String, String> extraData)
	{
		init(name, id, tags, link, description, extraData);
	}
	
	private void init(String name, String id,
			List<String> tags, String link, String description,
			Map<String, String> extraData)
	{
		this.name = name;
		this.shortName = getShortName(name);
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
        ShibbyFile file = new ShibbyFile(null, null, null);
        JSONObject json = new JSONObject(jsonStr);
        file.name = json.getString("name");
        if (json.has("shortName"))
        {
        	file.shortName = json.getString("shortName");
        }
        else
        {
        	file.shortName = file.getShortName(file.name);
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
        file.link = json.getString("link");
        file.description = json.getString("description");
        file.extraData = new HashMap<String, String>();
        JSONObject extras = json.getJSONObject("extras");
        for (String key : extras.keySet())
        {
        	file.extraData.put(key, extras.getString(key));
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
	
	private List<String> getTagsFromName()
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
	
	private String getShortName(String name)
	{
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
			else if (!in && c != ' ')
			{
				name = name.substring(i, name.length()-1);
				break;
			}
			tags += c;
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
		}
		return tags + name;
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
}
