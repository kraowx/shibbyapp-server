package io.github.kraowx.shibbyappserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import io.github.kraowx.shibbyappserver.models.MasterList;
import io.github.kraowx.shibbyappserver.models.ShibbyFile;
import io.github.kraowx.shibbyappserver.models.ShibbyFileArray;
import io.github.kraowx.shibbyappserver.tools.FormattedOutput;
import io.github.kraowx.shibbyappserver.tools.SortByFileCount;

public class DataUpdater
{
	private int interval;
	private boolean initialized;
	private List<ShibbyFile> files;
	private List<ShibbyFileArray> tags;
	private MasterList masterList;
	private Timer timer;
	
	public DataUpdater(int interval)
	{
		this.interval = interval;
		files = new ArrayList<ShibbyFile>();
		tags = new ArrayList<ShibbyFileArray>();
		masterList = new MasterList();
		start();
	}
	
	public void start()
	{
		if (timer == null)
		{
			timer = new Timer();
			timer.scheduleAtFixedRate(new TimerTask()
			{
				@Override
				public void run()
				{
					update();
				}
			}, 0, interval);
		}
	}
	
	public JSONArray getAllJSON()
	{
		JSONArray files = getFilesJSON();
		JSONArray tags = getTagsJSON();
		JSONArray series = getSeriesJSON();
		JSONObject all = new JSONObject();
		all.put("files", files);
		all.put("tags", tags);
		all.put("series", series);
		JSONArray allArr = new JSONArray();
		allArr.put(all);
		return allArr;
	}
	
	public JSONArray getFilesJSON()
	{
		JSONArray arr = new JSONArray();
		for (ShibbyFile file : files)
		{
			arr.put(file.toJSON());
		}
		return arr;
	}
	
	public JSONArray getTagsJSON()
	{
		JSONArray arr = new JSONArray();
		for (ShibbyFileArray tag : tags)
		{
			JSONObject objTag = new JSONObject();
			objTag.put("name", tag.getName());
			JSONArray arrTag = new JSONArray();
			for (ShibbyFile file : tag.getFiles())
			{
				arrTag.put(file.getId());
			}
			objTag.put("files", arrTag);
			arr.put(objTag);
		}
		return arr;
	}
	
	public JSONArray getSeriesJSON()
	{
		StringBuilder sb = new StringBuilder();
		File file = new File("seriesList.json");
		BufferedReader reader;
		try
		{
			if (!file.exists())
			{
				file.createNewFile();
			}
			reader = new BufferedReader(new FileReader(file));
			String line;
			while ((line = reader.readLine()) != null)
			{
				sb.append(line);
			}
			reader.close();
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		JSONArray arr = new JSONArray();
		try
		{
			arr = new JSONArray(sb.toString());
		}
		catch (JSONException je)
		{
			je.printStackTrace();
		}
		return arr;
	}
	
	public boolean isInitialized()
	{
		return initialized;
	}
	
	private void update()
	{
		System.out.println(FormattedOutput.get("Starting data update..."));
		System.out.println(FormattedOutput.get("Updating master file list..."));
		updateFiles();
		System.out.println(FormattedOutput.get("Updating tags..."));
		updateTags();
		if (!initialized)
		{
			initialized = true;
		}
		System.out.println(FormattedOutput.get("Data update complete."));
	}
	
	private void updateFiles()
	{
		masterList.update();
		files = masterList.getFiles();
		for (int i = 0; i < files.size(); i++)
		{
			ShibbyFile file = files.get(i);
			try
			{
				System.out.println("Updating file " + (i+1) + "/" + files.size() + "...");
				Document doc = Jsoup.connect(file.getLink()).get();
				Element js = doc.select("script[type*=text/javascript]").get(1);
				String jss = js.toString();
				jss = jss.substring(jss.indexOf("m4a: \"")+6);
				jss = jss.substring(0, jss.indexOf("\""));
				file.setLink(jss);
			}
			catch (IOException ioe)
			{
				ioe.printStackTrace();
				System.out.println(FormattedOutput.get("Failed to update file " +
						(i+1) + "/" + files.size()));
			}
		}
	}
	
	private void updateTags()
	{
		tags.clear();
		for (ShibbyFile file : files)
		{
			for (String tag : file.getTags())
			{
				tag = toTitleCase(tag);
				int index = indexOfTag(tag);
				if (index != -1)
				{
					if (getBetterTag(tag, tags.get(index).getName()).equals(tag))
					{
						tags.get(index).setName(tag);
					}
					tags.get(index).addFile(file);
				}
				else
				{
					tags.add(new ShibbyFileArray(tag,
							new ShibbyFile[]{file}, null));
				}
			}
		}
		System.out.println(FormattedOutput.get("Filtering tags..."));
		filterTags();
		System.out.println(FormattedOutput.get("Sorting tags..."));
		Collections.sort(tags, new SortByFileCount());
	}
	
	private void filterTags()
	{
		List<ShibbyFileArray> temp =
				(List<ShibbyFileArray>)((ArrayList)tags).clone();
		for (ShibbyFileArray tag : temp)
		{
			if (tag.getFileCount() == 1)
			{
				tags.remove(tag);
			}
		}
	}
	
	/*
	 * Selects the tag that has more capital letters in it, and thus,
	 * the tag that has the higher chance of being more visually appealing
	 */
	private String getBetterTag(String tag1, String tag2)
	{
		return getCapitalLetterCount(tag1) > getCapitalLetterCount(tag2) ?
				tag1 : tag2;
	}
	
	private int getCapitalLetterCount(String str)
	{
		int capitals = 0;
		for (char c : str.toCharArray())
		{
			if (Character.isUpperCase(c))
			{
				capitals++;
			}
		}
		return capitals;
	}
	
	private String toTitleCase(String str)
	{
		char[] chars = str.toCharArray();
		for (int i = 0; i < chars.length; i++)
		{
			char c = chars[i];
			if (i == 0)
			{
				chars[i] = Character.toUpperCase(c);
			}
			else if (c == ' ' && i+1 < chars.length && Character.isLowerCase(chars[i+1]))
			{
				chars[i+1] = Character.toUpperCase(chars[i+1]);
			}
		}
		return new String(chars);
	}
	
	private int indexOfTag(String tag)
	{
		for (int i = 0; i < tags.size(); i++)
		{
			if (tags.get(i).getName().toLowerCase()
					.equals(tag.toLowerCase()))
			{
				return i;
			}
		}
		return -1;
	}
}
