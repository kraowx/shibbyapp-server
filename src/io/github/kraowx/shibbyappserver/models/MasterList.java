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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
	
	public boolean update()
	{
		try
		{
			Document doc = Jsoup.connect(MASTER_LIST_URL).get();
			if (updateNeeded(doc))
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
	
	private boolean updateNeeded(Document doc)
	{
		List<ShibbyFile> filesNew = new ArrayList<ShibbyFile>();
		if (this.doc == null)
		{
			filesNew = files = readLocalList();
		}
		if (this.doc != null || filesNew.isEmpty())
		{
			filesNew = parseDocument(doc);
		}
		if (this.files.size() != filesNew.size())
		{
			return true;
		}
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
	
	private List<ShibbyFile> readLocalList()
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
}
