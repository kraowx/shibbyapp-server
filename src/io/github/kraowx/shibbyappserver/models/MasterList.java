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
				Elements soundsDetails = doc.select("div[class*=sound-details]");
				files = new ArrayList<ShibbyFile>();
				for (Element details : soundsDetails)
				{
					String name = details.select("a").text();
					String link = details.select("a").first().attr("href");
					String description = details.select("span[class*=soundDescription]").text();
					files.add(new ShibbyFile(name, link, description));
				}
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
		Elements details1 = this.doc.select("div[class*=sound-details]");
		Elements details2 = doc.select("div[class*=sound-details]");
		String link1 = details1.select("a").first().attr("href");
		String link2 = details2.select("a").first().attr("href");
		if (!link1.equals(link2))
		{
			return true;
		}
		return false;
	}
}
