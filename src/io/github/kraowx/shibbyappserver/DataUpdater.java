package io.github.kraowx.shibbyappserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
	public static final String PATREON_DATA_PATH = "patreonData.json";
	public static final String CONFIG_FILE_PATH = "shibbyapp-server.config";
	
	private int interval;
	private boolean initialized,
		heavyUpdate, patreonEnabled;
	private List<ShibbyFile> files;
	private List<ShibbyFileArray> tags, tagsWithPatreon;
	private JSONArray patreonFiles;
	private MasterList masterList;
	private Timer timer;
	
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
		tagsWithPatreon = new ArrayList<ShibbyFileArray>();
		patreonFiles = new JSONArray();
		masterList = new MasterList();
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
	
	/*
	 * Starts the update timer to update every interval.
	 */
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
	
	/*
	 * Returns all available data in JSON format (except for Patreon tags).
	 * If patreon=true then Patreon tags will also be included.
	 * Patreon data is *not* included to improve app loading time. This
	 * may be changed in the future however.
	 */
	public JSONArray getAllJSON(boolean patreon)
	{
		JSONArray files = getFilesJSON();
		JSONArray tags = null;
		if (patreon)
		{
			tags = getTagsWithPatreonJSON();
		}
		else
		{
			tags = getTagsJSON();
		}
		JSONArray series = getSeriesJSON();
		JSONObject all = new JSONObject();
		all.put("files", files);
		all.put("tags", tags);
		all.put("series", series);
		JSONArray allArr = new JSONArray();
		allArr.put(all);
		return allArr;
	}
	
	/*
	 * Returns soundgasm file data in JSON format.
	 */
	public JSONArray getFilesJSON()
	{
		JSONArray arr = new JSONArray();
		for (ShibbyFile file : files)
		{
			arr.put(file.toJSON());
		}
		return arr;
	}
	
	/*
	 * Returns tags data in JSON format *without* Patreon files.
	 */
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
	
	/*
	 * Returns tags data in JSON format *with* Patreon files.
	 */
	public JSONArray getTagsWithPatreonJSON()
	{
		JSONArray arr = new JSONArray();
		for (ShibbyFileArray tag : tagsWithPatreon)
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
	
	/*
	 * Returns series data from the disk (already in JSON format).
	 */
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
	
	/*
	 * Returns Patreon file data in JSON format.
	 */
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
	
	/*
	 * Performs a full update. This is done in the following order: update Patreon files,
	 * update soundgasm files, update tags, filter tags, sort tags.
	 */
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
		System.out.println(FormattedOutput.get("Writing soundgasm master file list to '" +
				MasterList.LOCAL_LIST_PATH + "'..."));
		masterList.writeLocalList(files);
		if (!initialized)
		{
			initialized = true;
		}
		long endTime = Calendar.getInstance().getTime().getTime();
		float duration = (float)(endTime - startTime)/1000;
		System.out.println(FormattedOutput.get("Data update complete in " +
				duration + " seconds."));
	}
	
	/*
	 * Attempts to update soundgasm files by first checking if
	 * an update is required. If so, each local file will be
	 * compared with the latest file to check for differences.
	 */
	private void updateFiles()
	{
		// Only update if the local master list and the
		// latest master list are not identical
		if (masterList.update(heavyUpdate))
		{
			List<ShibbyFile> newFiles = masterList.getFiles();
			// A buffer is used so that the indices are not changed
			// when the list is changed
			List<ShibbyFile> filesTemp =
					(List<ShibbyFile>)((ArrayList<ShibbyFile>)files).clone();
			boolean firstUpdate = files == null || files.isEmpty();
			if (firstUpdate)
			{
				files = masterList.readLocalList();
				filesTemp = (List<ShibbyFile>)((ArrayList<ShibbyFile>)files).clone();
			}
			int j = 0, filesAdded = 0;
			for (int i = 0; i < newFiles.size(); i++)
			{
				ShibbyFile oldFile = files.get(j);
				ShibbyFile newFile = newFiles.get(i);
				if (oldFile.getName().equals(newFile.getName()))
				{
					// Iterate through the local list separately
					// from the updated list
					j++;
				}
				if (oldFile == null || heavyUpdate ||
						!filesMostlyEqual(oldFile, newFile))
				{
					try
					{
						System.out.println(FormattedOutput.get("Updating file " +
								(i+1) + "/" + newFiles.size() + "..."));
						/*
						 * Retrieve the actual audio file link from soundgasm.
						 * This becomes a somewhat intensive action when repeated
						 * hundreds of times, which is why files must only be updated
						 * if an update is actually needed
						 */
						Document doc = Jsoup.connect(newFile.getLink()).get();
						Element js = doc.select("script[type*=text/javascript]").get(1);
						String jss = js.toString();
						jss = jss.substring(jss.indexOf("m4a: \"")+6);
						jss = jss.substring(0, jss.indexOf("\""));
						newFile.setLink(jss);
						if (oldFile.getName().equals(newFile.getName()))
						{
							// If a local file with the same name already exists,
							// update the existing file with the contents of the new file
							filesTemp.set(j + filesAdded, newFile);
						}
						else
						{
							// If the file does not exist, add it at the
							// current position in the list
							filesTemp.add(i, newFile);
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
			System.out.println(FormattedOutput.
					get("Master list is up to date."));
		}
		if (files == null || files.isEmpty())
		{
			files = masterList.getFiles();
		}
	}
	
	/*
	 * Updates Patreon files (if enabled) regardless of whether
	 * an update is available or not. It is simply not possible
	 * to tell using the current method.
	 */
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
					/*
					 * The script reports a new file has been found.
					 * This is purely so the user can see that the script is
					 * doing something while it is running in the background.
					 */
					if (line.startsWith("file - "))
					{
						String[] data = line.split(" - ");
						System.out.println(FormattedOutput.get(
								"Found Patreon file: \"" + data[1]) + "\"");
					}
				}
				process.waitFor();
				in.close();
				
				BufferedReader reader = new BufferedReader(
						new FileReader(new File(PATREON_DATA_PATH)));
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
	
	/*
	 * Performs a full update of tags including file updating,
	 * filtering, and sorting for both soundgasm and Patreon files.
	 */
	private void updateTags()
	{
		tags.clear();
		tagsWithPatreon.clear();
		updateListTags(files, tags);
		if (patreonEnabled)
		{
			updateListTags(files, tagsWithPatreon);
			updateListTags(parsePatreonFiles(patreonFiles), tagsWithPatreon);
			filterTags(tagsWithPatreon);
			Collections.sort(tagsWithPatreon, new SortByFileCount());
		}
		System.out.println(FormattedOutput.get("Filtering tags..."));
		filterTags(tags);
		System.out.println(FormattedOutput.get("Sorting tags..."));
		Collections.sort(tags, new SortByFileCount());
	}
	
	/*
	 * Updates a local list of tags to include any new files.
	 */
	private void updateListTags(List<ShibbyFile> list,
			List<ShibbyFileArray> tagsList)
	{
		for (ShibbyFile file : list)
		{
			for (String tag : file.getTags())
			{
				tag = toTitleCase(tag);
				int index = indexOfTag(tag, tagsList);
				if (index != -1)
				{
					if (getBetterTag(tag, tagsList.get(index)
							.getName()).equals(tag))
					{
						/*
						 * Associated file has an equivalent but
						 * more visually appealing tag, so replace the
						 * tag with the associated file tag
						 * (this process continues until the most visually
						 * appealing tag is found)
						 */
						tagsList.get(index).setName(tag);
					}
					// Tag exists in the record, so add the
					// associated file to the tag
					tagsList.get(index).addFile(file);
				}
				else
				{
					// Tag does not yet exist in the record, so add it
					tagsList.add(new ShibbyFileArray(tag,
							new ShibbyFile[]{file}, null));
				}
			}
		}
	}
	
	/*
	 * Removes unneeded/unwanted tags by:
	 * - Removing tags that have less than two
	 *   files associated with them
	 * - (more filtering techniques will be added)
	 */
	private void filterTags(List<ShibbyFileArray> tagsList)
	{
		List<ShibbyFileArray> temp =
				(List<ShibbyFileArray>)((ArrayList)tagsList).clone();
		for (ShibbyFileArray tag : temp)
		{
			if (tag.getFileCount() == 1)
			{
				tagsList.remove(tag);
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
	
	/*
	 * Returns the number of upper case letters in a string.
	 */
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
	
	/*
	 * Converts the first letter of each word
	 * in a string to upper case.
	 */
	private String toTitleCase(String str)
	{
		char[] chars = str.toCharArray();
		for (int i = 0; i < chars.length; i++)
		{
			char c = chars[i];
			if (i == 0)
			{
				// The first letter is always upper case
				// (unless it is not a letter?)
				chars[i] = Character.toUpperCase(c);
			}
			else if (c == ' ' && i+1 < chars.length &&
					Character.isLowerCase(chars[i+1]))
			{
				// The character after a space is upper case
				// (if it exists)
				chars[i+1] = Character.toUpperCase(chars[i+1]);
			}
		}
		return new String(chars);
	}
	
	/*
	 * Returns the index of a tag in a list, or -1 if the
	 * tag is not in the list.
	 */
	private int indexOfTag(String tag, List<ShibbyFileArray> tagsList)
	{
		for (int i = 0; i < tagsList.size(); i++)
		{
			if (tagsList.get(i).getName().toLowerCase()
					.equals(tag.toLowerCase()))
			{
				return i;
			}
		}
		return -1;
	}
	
	/*
	 * Checks if the names, tags, and descriptions of two files match.
	 * On the surface, files are nearly identical at this point.
	 */
	private boolean filesMostlyEqual(ShibbyFile file1, ShibbyFile file2)
	{
		return file1.getName().equals(file2.getName()) &&
				file1.getDescription().equals(file2.getDescription()) &&
				file1.getTags().equals(file2.getTags());
	}
	
	/*
	 * Converts a JSON array of Patreon files into a list of shibbyfiles.
	 */
	private List<ShibbyFile> parsePatreonFiles(JSONArray patreonFiles)
	{
		List<ShibbyFile> files = new ArrayList<ShibbyFile>();
		for (int i = 0; i < patreonFiles.length(); i++)
		{
			files.add(ShibbyFile.fromJSON(patreonFiles.getJSONObject(i).toString()));
		}
		return files;
	}
	
	/*
	 * Returns the user's Patreon credentials from the
	 * file if they exist.
	 */
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
	
	/*
	 * Attempts to check if the user's credentials (email + password)
	 * belong to a Patreon account that exists and that has an
	 * active pledge to Shibby. This method communicates with the
	 * python script "patreonScript.py" in order to retrieve the data.
	 */
	public int isAccountVerified(String email, String password)
	{
		try
		{
			if (email != null && password != null)
			{
				File scriptFile = new File("patreonScript.py");
				// Start the script on a new process in the background
				Process process = Runtime.getRuntime().exec("python3 " +
						scriptFile.getAbsolutePath() + " -v " +
						email + " " + password);
				BufferedReader in = new BufferedReader(
						new InputStreamReader(process.getInputStream()));
				final String VALID_ACCOUNT_RESPONSE = "login valid";
				final String TOO_MANY_REQUESTS_RESPONSE = "error - too many requests";
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
							// Account has been verified
							case VALID_ACCOUNT_RESPONSE:
								return 1;
							// Patreon api has too many requests for the linked account
							case TOO_MANY_REQUESTS_RESPONSE:
								System.out.println(FormattedOutput.get("ERROR: Too many " +
										"requests have been sent to Patreon. " +
										"Try again in 10 minutes"));
								return 3;
							// Account requires email confirmation to be used with this device
							case EMAIL_CONFIRM_RESPONSE:
								System.out.println(FormattedOutput.get("ERROR: This " +
										"device must be verified to access your " +
										"Patreon account by clicking the link in your " +
										"email USING ONLY THIS DEVICE"));
								return 2;
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
		return 0;
	}
	
	/*
	 * Checks if the server can connect to Patreon by #1-checking if the
	 * user has set up the required files #2-checking if the user's
	 * Patreon account is valid and usable.
	 */
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
		return isAccountVerified(email, password) == 1;
	}
}
