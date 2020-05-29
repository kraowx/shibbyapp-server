package io.github.kraowx.shibbyappserver.models;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.github.kevinsawicki.http.HttpRequest;

public class MasterList
{
	public static final String MASTER_LIST_URL = "https://soundgasm.net/u/kinkyshibby";
	public static final String LOCAL_LIST_PATH = "sgData.json";
	
	private List<ShibbyFile> files;
	private Document doc;
	
	public MasterList()
	{
		files = new ArrayList<ShibbyFile>();
	}
	
	public List<ShibbyFile> getFiles()
	{
		return files;
	}
	
	/*
	 * Updates the master list only if an update is required.
	 * Can be overridden to force an update.
	 */
	public boolean update(boolean force, boolean remoteEnabled,
			String url, String key)
	{
		try
		{
			Document doc = Jsoup.connect(MASTER_LIST_URL).get();
			if (force || updateNeeded(doc, remoteEnabled, url, key))
			{
				this.doc = doc;
				files = parseDocument(doc);
				return true;
			}
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		return false;
	}
	
	/*
	 * Check if an update is actually required by comparing
	 * the local master list with the latest version.
	 */
	private boolean updateNeeded(Document doc, boolean remoteEnabled,
			String url, String key)
	{
		List<ShibbyFile> filesNew = new ArrayList<ShibbyFile>();
		List<ShibbyFile> docFiles = parseDocument(doc);
		// Load cached files
		if (this.doc == null && remoteEnabled)
		{
			filesNew = files = readRemoteList(url, key);
			if (files == null)
			{
				filesNew = files = readLocalList();
			}
		}
		else if (this.doc == null)
		{
			filesNew = files = readLocalList();
		}
		// If filesNew does not contain the latest files, update it
		if (this.doc != null || filesNew.isEmpty() ||
				!docFiles.toString().equals(filesNew.toString()))
		{
			filesNew = docFiles;
		}
		// Files have been added or removed from master
		if (this.files.size() != filesNew.size())
		{
			return true;
		}
		// Lists are the same size, so compare each file to check
		// for any difference between their contents
		for (int i = 0; i < files.size(); i++)
		{
			ShibbyFile fileOld = this.files.get(i);
			ShibbyFile fileNew = filesNew.get(i);
			if (!fileOld.getName().equals(fileNew.getName()) ||
					!fileOld.getDescription().equals(fileNew.getDescription()) ||
					!fileOld.getTags().equals(fileNew.getTags()))
			{
				return true;
			}
		}
		return false;
	}
	
	/*
	 * Converts an HTML formatted master list to a list of shibbyfiles.
	 */
	private List<ShibbyFile> parseDocument(Document doc)
	{
		Elements soundsDetails = doc.select("div[class*=sound-details]");
		List<ShibbyFile> files = new ArrayList<ShibbyFile>();
		for (Element details : soundsDetails)
		{
			String name = details.select("a").text();
			String link = details.select("a").first().attr("href");
			String description = details.select("span[class*=soundDescription]").text();
			files.add(new ShibbyFile(name, link, description, "soundgasm"));
		}
		return files;
	}
	
	/*
	 * Converts a list of shibbyfiles to JSON format and writes it
	 * to the local master list on the disk.
	 */
	public void writeLocalList(List<ShibbyFile> list)
	{
		JSONArray arr = new JSONArray();
		for (ShibbyFile file : list)
		{
			arr.put(file.toJSON());
		}
		File file = new File(LOCAL_LIST_PATH);
		BufferedWriter writer = null;
		try
		{
			if (!file.exists())
			{
				file.createNewFile();
			}
			writer = new BufferedWriter(new FileWriter(file));
			writer.write(arr.toString());
			writer.close();
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
	
	/*
	 * Reads the local master list from the disk and converts
	 * it to a list of shibbyfiles.
	 */
	public List<ShibbyFile> readLocalList()
	{
		List<ShibbyFile> list = new ArrayList<ShibbyFile>();
		StringBuilder sb = new StringBuilder();
		File file = new File(LOCAL_LIST_PATH);
		JSONArray arr = null;
		BufferedReader reader = null;
		if (file.exists())
		{
			try
			{
				reader = new BufferedReader(new FileReader(file));
				String line;
				while ((line = reader.readLine()) != null)
				{
					sb.append(line);
				}
				reader.close();
				arr = new JSONArray(sb.toString());
			}
			catch (IOException ioe)
			{
				ioe.printStackTrace();
			}
		}
		if (arr != null)
		{
			for (int i = 0; i < arr.length(); i++)
			{
				list.add(ShibbyFile.fromJSON(arr.getJSONObject(i).toString()));
			}
		}
		return list;
	}
	
	public void writeRemoteList(List<ShibbyFile> list, String url, String key)
	{
		JSONArray arr = new JSONArray();
		for (ShibbyFile file : list)
		{
			arr.put(file.toJSON());
		}
		try
		{
			HttpRequest req = HttpRequest.put(url);
			req.contentType("application/json");
			req.userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36");
			req.header("Api-Key", key);
			req.send(arr.toString());
			req.code();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public List<ShibbyFile> readRemoteList(String url, String key)
	{
		List<ShibbyFile> files = new ArrayList<ShibbyFile>();
		try
		{
			HttpRequest req = HttpRequest.get(url);
			req.contentType("application/json");
			req.userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36");
			req.header("Api-Key", key);
			if (req.ok())
			{
				JSONObject data = new JSONObject(req.body());
				JSONArray arr = data.getJSONArray("data");
				for (int i = 0; i < arr.length(); i++)
				{
					files.add(ShibbyFile.fromJSON(
							arr.getJSONObject(i).toString()));
				}
				return files;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
}
