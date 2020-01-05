package io.github.kraowx.shibbyappserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
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
	public static final String PATREON_SCRIPT_PATH = "patreonScript.py";
	public static final String CONFIG_FILE_PATH = "shibbyapp-server.config";
	
	private int interval;
	private boolean initialized,
		heavyUpdate, patreonEnabled;
	private List<ShibbyFile> files;
	private List<ShibbyFileArray> tags;
	private JSONArray patreonFiles;
	private MasterList masterList;
	private Timer timer;
	private List<String> verifiedPatreonEmails;
	
	public DataUpdater(int interval, boolean heavyUpdate)
	{
		this.interval = interval;
		this.heavyUpdate = heavyUpdate;
		init();
		start();
	}
	
	private void init()
	{
		files = new ArrayList<ShibbyFile>();
		tags = new ArrayList<ShibbyFileArray>();
		patreonFiles = new JSONArray();
		masterList = new MasterList();
		verifiedPatreonEmails = new ArrayList<String>();
		System.out.println(FormattedOutput.get("Checking Patreon updates status..."));
		patreonEnabled = checkPatreonEnabled();
		if (patreonEnabled)
		{
			System.out.println(FormattedOutput.get("Patreon updates ENABLED"));
		}
		else
		{
			System.out.println(FormattedOutput.get("Patreon updates DISABLED"));
		}
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
	
	public boolean isPatreonEnabled()
	{
		return patreonEnabled;
	}
	
	public List<String> getVerifiedPatreonEmails()
	{
		return verifiedPatreonEmails;
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
		if (patreonEnabled)
		{
			all.put("patreonFiles", patreonFiles);
		}
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
	
	public JSONArray getPatreonJSON()
	{
		JSONArray arr = new JSONArray();
		JSONObject obj = new JSONObject();
		obj.put("patreonFiles", patreonFiles);
		arr.put(obj);
		return arr;
	}
	
	public boolean isInitialized()
	{
		return initialized;
	}
	
	private void update()
	{
		long startTime = Calendar.getInstance().getTime().getTime();
		System.out.println(FormattedOutput.get("Starting data update..."));
		if (patreonEnabled)
		{
			System.out.println(FormattedOutput.get("Updating Patreon data..."));
			updatePatreonData();
		}
		System.out.println(FormattedOutput.get("Updating soundgasm master file list..."));
		updateFiles();
		System.out.println(FormattedOutput.get("Updating tags..."));
		updateTags();
		if (!initialized)
		{
			initialized = true;
		}
		long endTime = Calendar.getInstance().getTime().getTime();
		float duration = (float)(endTime - startTime)/1000;
		System.out.println(FormattedOutput.get("Data update complete in " +
				duration + " seconds."));
	}
	
	private void updateFiles()
	{
		if (masterList.update())
		{
			List<ShibbyFile> newFiles = masterList.getFiles();
			List<ShibbyFile> filesTemp = (List<ShibbyFile>)((ArrayList<ShibbyFile>)files).clone();
			boolean firstUpdate = files == null || files.isEmpty();
			if (firstUpdate)
			{
				files = (List<ShibbyFile>)((ArrayList<ShibbyFile>)newFiles).clone();
			}
			int filesAdded = 0;
			final int FILES_SIZE = files.size();
			for (int i = newFiles.size()-1; i > -1; i--)
			{
				ShibbyFile oldFile = null;
				if (!firstUpdate && FILES_SIZE > i)
				{
					oldFile = files.get(FILES_SIZE-1-i);
				}
				ShibbyFile newFile = newFiles.get(newFiles.size()-1-i);
				if (oldFile == null || heavyUpdate ||
						!oldFile.equals(newFile))
				{
					try
					{
						System.out.println(FormattedOutput.get("Updating file " +
								(newFiles.size()-i) + "/" + newFiles.size() + "..."));
						Document doc = Jsoup.connect(newFile.getLink()).get();
						Element js = doc.select("script[type*=text/javascript]").get(1);
						String jss = js.toString();
						jss = jss.substring(jss.indexOf("m4a: \"")+6);
						jss = jss.substring(0, jss.indexOf("\""));
						newFile.setLink(jss);
						if (firstUpdate || i > FILES_SIZE-1)
						{
							filesTemp.add(filesAdded++, newFile);
						}
						else
						{
							filesTemp.set(FILES_SIZE-1-i, newFile);
						}
					}
					catch (IOException ioe)
					{
						ioe.printStackTrace();
						System.out.println(FormattedOutput.get("Failed to update file " +
								(newFiles.size()-i) + "/" + newFiles.size()));
					}
				}
			}
			files = filesTemp;
		}
		else
		{
			System.out.println(FormattedOutput.get("\nMaster list is up to date."));
		}
	}
	
	private void updatePatreonData()
	{
		if (patreonEnabled)
		{
			Process process;
			BufferedReader in;
			try
			{
				String[] creds = getPatreonCredentials();
				process = Runtime.getRuntime().exec("python3 " + PATREON_SCRIPT_PATH +
						" " + creds[0] + " " + creds[1]);
				in = new BufferedReader(new InputStreamReader(process.getInputStream()));
				final String EMAIL_CONFIRM_RESPONSE = "error - this device " +
						"must be verified by clicking link in email";
				final String TOO_MANY_REQUESTS_RESPONSE = "error - too many requests";
				final String UNKNOWN_ERROR_RESPONSE = "error - unknown";
				String line;
				while ((line = in.readLine()) != null)
				{
					switch (line)
					{
						case EMAIL_CONFIRM_RESPONSE:
							System.out.println(FormattedOutput.get("ERROR: This " +
									"device must be verified to access your " +
									"Patreon account by clicking the link in your " +
									"email USING ONLY THIS DEVICE"));
							return;
						case TOO_MANY_REQUESTS_RESPONSE:
							System.out.println(FormattedOutput.get("ERROR: Too many " +
									"requests sent to Patreon. Update not completed"));
							return;
						case UNKNOWN_ERROR_RESPONSE:
							System.out.println(FormattedOutput.get("ERROR: Unknown " +
									"Patreon connection error"));
							return;
					}
					if (line.startsWith("file - "))
					{
						String[] data = line.split(" - ");
						System.out.println(FormattedOutput.get(
								"Found Patreon file: \"" + data[1]) + "\"");
					}
				}
				process.waitFor();
				in.close();
				
				BufferedReader reader = new BufferedReader(new FileReader(new File("patreonData.json")));
				String json = reader.readLine();
				reader.close();
				if (json != null)
				{
					patreonFiles = new JSONArray(json);
				}
				else
				{
					patreonFiles = new JSONArray();
				}
				for (int i = 0; i < patreonFiles.length(); i++)
				{
					System.out.println(patreonFiles.getJSONObject(i).getString("name"));
				}
			}
			catch (FileNotFoundException fnfe)
			{
				System.out.println(FormattedOutput.get("ERROR: " +
						"Invalid path to Patreon script"));
			}
			catch (IOException ioe)
			{
				System.out.println(FormattedOutput.get("ERROR: Failed to " +
						"update Patreon data"));
			}
			catch (InterruptedException ie)
			{
				ie.printStackTrace();
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
			else if (c == ' ' && i+1 < chars.length &&
					Character.isLowerCase(chars[i+1]))
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
	
	private String[] getPatreonCredentials()
	{
		String[] creds = new String[2];
		File configFile = new File("shibbyapp-server.config");
		BufferedReader reader;
		try
		{
			reader = new BufferedReader(new FileReader(configFile));
			creds[0] = reader.readLine();
			creds[1] = reader.readLine();
			reader.close();
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		return creds;
	}
	
	public boolean isAccountVerified(String email, String password)
	{
		try
		{
			if (email != null && password != null)
			{
				File scriptFile = new File("patreonScript.py");
				Process process = Runtime.getRuntime().exec("python3 " +
						scriptFile.getAbsolutePath() + " -v " +
						email + " " + password);
				BufferedReader in = new BufferedReader(
						new InputStreamReader(process.getInputStream()));
				final String VALID_ACCOUNT_RESPONSE = "login valid";
				final String EMAIL_CONFIRM_RESPONSE = "error - this device " +
						"must be verified by clicking link in email";
				String line;
				while (process.isAlive())
				{
					line = in.readLine();
					if (line != null)
					{
						switch (line)
						{
							case VALID_ACCOUNT_RESPONSE:
								return true;
							case EMAIL_CONFIRM_RESPONSE:
								System.out.println(FormattedOutput.get("ERROR: This " +
										"device must be verified to access your " +
										"Patreon account by clicking the link in your " +
										"email USING ONLY THIS DEVICE"));
								return false;
						}
					}
				}
				in.close();
			}
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		return false;
	}
	
	private boolean checkPatreonEnabled()
	{
		File scriptFile = new File("patreonScript.py");
		File configFile = new File("shibbyapp-server.config");
		boolean exists = true;
		if (!scriptFile.exists())
		{
			System.out.println(FormattedOutput.get(
					"Missing file: \"patreonScript.py\""));
			exists = false;
		}
		if (!configFile.exists())
		{
			System.out.println(FormattedOutput.get(
					"Missing file: \"shibbyapp-server.config\""));
			exists = false;
		}
		if (!exists)
		{
			return false;
		}
		String email = null, password = null;
		BufferedReader reader;
		try
		{
			reader = new BufferedReader(new FileReader(configFile));
			email = reader.readLine();
			password = reader.readLine();
			reader.close();
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		return isAccountVerified(email, password);
	}
	
//	private boolean isPatreonEnabled()
//	{
//		File scriptFile = new File("patreonScript.py");
//		File configFile = new File("shibbyapp-server.config");
//		boolean exists = true;
//		if (!scriptFile.exists())
//		{
//			System.out.println(FormattedOutput.get(
//					"Missing file: \"patreonScript.py\""));
//			exists = false;
//		}
//		if (!configFile.exists())
//		{
//			System.out.println(FormattedOutput.get(
//					"Missing file: \"shibbyapp-server.config\""));
//			exists = false;
//		}
//		if (!exists)
//		{
//			return false;
//		}
//		String email = null, password = null;
//		BufferedReader reader;
//		try
//		{
//			reader = new BufferedReader(new FileReader(configFile));
//			email = reader.readLine();
//			password = reader.readLine();
//			reader.close();
//			if (email != null && password != null)
//			{
//				Process process = Runtime.getRuntime().exec("python3 " +
//						scriptFile.getAbsolutePath() + " -v " +
//						email + " " + password);
//				BufferedReader in = new BufferedReader(
//						new InputStreamReader(process.getInputStream()));
//				final String VALID_ACCOUNT_RESPONSE = "login valid";
//				final String EMAIL_CONFIRM_RESPONSE = "error - this device " +
//						"must be verified by clicking link in email";
//				String line;
//				while (process.isAlive())
//				{
//					line = in.readLine();
//					if (line != null)
//					{
//						switch (line)
//						{
//							case VALID_ACCOUNT_RESPONSE:
//								return true;
//							case EMAIL_CONFIRM_RESPONSE:
//								System.out.println(FormattedOutput.get("ERROR: This " +
//										"device must be verified to access your " +
//										"Patreon account by clicking the link in your " +
//										"email USING ONLY THIS DEVICE"));
//								return false;
//						}
//					}
//				}
//				in.close();
//			}
//		}
//		catch (IOException ioe)
//		{
//			ioe.printStackTrace();
//		}
//		return false;
//	}
}
