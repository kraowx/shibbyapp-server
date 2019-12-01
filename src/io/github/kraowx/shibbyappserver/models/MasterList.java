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
	
	public MasterList()
	{
		files = new ArrayList<ShibbyFile>();
	}
	
	public List<ShibbyFile> getFiles()
	{
		return files;
	}
	
	public void update()
	{
		try
		{
			Document doc = Jsoup.connect(MASTER_LIST_URL).get();
			Elements soundsDetails = doc.select("div[class*=sound-details]");
			files = new ArrayList<ShibbyFile>();
			for (Element details : soundsDetails)
			{
				String name = details.select("a").text();
				String link = details.select("a").first().attr("href");
				String description = details.select("span[class*=soundDescription]").text();
				files.add(new ShibbyFile(name, null, link, description));
			}
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
}
