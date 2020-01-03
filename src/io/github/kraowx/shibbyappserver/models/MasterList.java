package io.github.kraowx.shibbyappserver.models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MasterList
{
	private final String MASTER_LIST_URL = "https://soundgasm.net/u/kinkyshibby";
	
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
		if (this.doc == null)
		{
			return true;
		}
		List<ShibbyFile> filesNew = parseDocument(doc);
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
			files.add(new ShibbyFile(name, link, description));
		}
		return files;
	}
}
