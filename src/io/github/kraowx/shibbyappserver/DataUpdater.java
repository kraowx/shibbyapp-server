package io.github.kraowx.shibbyappserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import de.odysseus.ithaka.audioinfo.AudioInfo;
import de.odysseus.ithaka.audioinfo.m4a.M4AInfo;
import de.odysseus.ithaka.audioinfo.mp3.MP3Info;
import io.github.kraowx.shibbyappserver.models.HotspotArray;
import io.github.kraowx.shibbyappserver.models.MasterList;
import io.github.kraowx.shibbyappserver.models.ShibbyFile;
import io.github.kraowx.shibbyappserver.models.ShibbyTag;
import io.github.kraowx.shibbyappserver.models.ShibbyTagFile;
import io.github.kraowx.shibbyappserver.net.ShibbyDexClient;
import io.github.kraowx.shibbyappserver.tools.AudioAnalysis;
import io.github.kraowx.shibbyappserver.tools.FormattedOutput;
import io.github.kraowx.shibbyappserver.tools.SortByFileCount;

public class DataUpdater
{
	public static final String PATREON_SCRIPT_PATH = "patreonScript.py";
	public static final String PATREON_DATA_PATH = "patreonData.json";
	public static final String CONFIG_FILE_PATH = "shibbyapp-server.config";
	public static final String ONLINE_STORAGE_PATH = "remoteStorage";
	public static final String HOTSPOTS_LOCAL_PATH = "hotspots.json";
	
	public static final int EXCLUDE_NONE = 0;
	public static final int EXCLUDE_SOUNDGASM = 1;
	public static final int EXCLUDE_PATREON = 2;
	public static final int EXCLUDE_ALL = 3;
	
	private final int HOTSPOTS_VERSION = 0;
	
	private int interval, initialUpdate;
	private boolean initialized, forceUpdate,
		includeFileDuration, includeHotspots,
		patreonEnabled, remoteStorageEnabled;
	private String remoteStorageUrl, remoteStorageKey;
	private List<ShibbyFile> files;
	private List<ShibbyTag> tags, tagsWithPatreon;
	private List<HotspotArray> hotspots;
	private JSONArray patreonFiles;
	private MasterList masterList;
	private ShibbyDexClient shibbydexClient;
	private Timer timer;
	
	public DataUpdater(int interval, boolean forceUpdate,
			int initialUpdate, boolean includeFileDuration,
			boolean includeHotspots)
	{
		this.interval = interval;
		this.forceUpdate = forceUpdate;
		this.initialUpdate = initialUpdate;
		this.includeFileDuration = includeFileDuration;
		this.includeHotspots = includeHotspots;
		init();
		start();
	}
	
	private void init()
	{
		files = new ArrayList<ShibbyFile>();
		tags = new ArrayList<ShibbyTag>();
		tagsWithPatreon = new ArrayList<ShibbyTag>();
		hotspots = new ArrayList<HotspotArray>();
		patreonFiles = new JSONArray();
		masterList = new MasterList();
		shibbydexClient = new ShibbyDexClient();
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
		String[] remoteStorage = readRemoteStorageFile();
		remoteStorageEnabled = remoteStorage != null;
		if (remoteStorageEnabled)
		{
			remoteStorageUrl = remoteStorage[0];
			remoteStorageKey = remoteStorage[1];
			System.out.println(FormattedOutput.get(
					"Remote storage enabled at URL '" + remoteStorageUrl + "'"));
		}
		else
		{
			System.out.println(FormattedOutput.get("Remote storage DISABLED"));
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
	public JSONObject getAllJSON(boolean patreon)
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
		return all;
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
		for (ShibbyTag tag : tags)
		{
			JSONObject objTag = new JSONObject();
			objTag.put("name", tag.getName());
			JSONArray arrTag = new JSONArray();
			for (ShibbyTagFile file : tag.getFiles())
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
		for (ShibbyTag tag : tagsWithPatreon)
		{
			JSONObject objTag = new JSONObject();
			objTag.put("name", tag.getName());
			JSONArray arrTag = new JSONArray();
			for (ShibbyTagFile file : tag.getFiles())
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
	 * Returns all file hotspot data from memory.
	 */
	public JSONArray getHotspotsJSON()
	{
		JSONArray arrs = new JSONArray();
		for (int i = 0; i < hotspots.size(); i++)
		{
			arrs.put(new JSONObject(hotspots.get(i).toJSON()));
		}
		return arrs;
	}
	
	/*
	 * Returns Patreon file data in JSON format.
	 */
	public JSONArray getPatreonJSON()
	{
		return patreonFiles;
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
		if (initialUpdate != EXCLUDE_ALL)
		{
			System.out.println(FormattedOutput.get("Starting data update..."));
		}
		else
		{
			System.out.println(FormattedOutput.get("Starting local data update..."));
		}
		if (patreonEnabled && initialUpdate != EXCLUDE_PATREON && initialUpdate != EXCLUDE_ALL)
		{
			System.out.println(FormattedOutput.get("Updating Patreon data..."));
		}
		updatePatreonData();
		if (shibbydexClient.updateNeeded() && initialUpdate != EXCLUDE_SOUNDGASM && initialUpdate != EXCLUDE_ALL)
		{
			System.out.println(FormattedOutput.get("Authenticating with ShibbyDex..."));
			try
			{
				shibbydexClient.updateAuthenticatedCookie();
			}
			catch (IOException e)
			{
				e.printStackTrace();
				System.out.println(FormattedOutput.get("ERROR: ShibbyDex login failed."));
			}
		}
		if (initialUpdate == EXCLUDE_NONE || initialUpdate == EXCLUDE_PATREON)
		{
			System.out.println(FormattedOutput.get("Updating soundgasm master file list..."));
		}
		else
		{
			System.out.println(FormattedOutput.get("Retrieving local soundgasm master file list..."));
		}
		updateFiles();
		System.out.println(FormattedOutput.get("Applying latest changes to local soundgasm master file list..."));
		applyLocalFileChanges();
		System.out.println(FormattedOutput.get("Updating tags..."));
		updateTags();
		if (initialUpdate == EXCLUDE_NONE || initialUpdate == EXCLUDE_PATREON)
		{
			System.out.println(FormattedOutput.get("Writing soundgasm master file list to '" +
					MasterList.LOCAL_LIST_PATH + "'..."));
			masterList.writeLocalList(files);
			if (remoteStorageEnabled)
			{
				System.out.println(FormattedOutput.get(
						"Writing soundgasm master file list to remote storage..."));
				masterList.writeRemoteList(files, remoteStorageUrl, remoteStorageKey);
			}
			System.out.println(FormattedOutput.get("Writing hotspots to local list..."));
			writeHotspotsToDisk();
		}
		if (!initialized)
		{
			initialized = true;
		}
		if (initialUpdate > 0)
		{
			initialUpdate = 0;
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
		if ((initialUpdate == EXCLUDE_NONE || initialUpdate == EXCLUDE_PATREON) &&
				masterList.update(forceUpdate, remoteStorageEnabled,
						remoteStorageUrl, remoteStorageKey, shibbydexClient))
		{
			int offset = 0;
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
			
			Map<String, Integer> fileIndex = new HashMap<String, Integer>();
			for (int i = 0; i < files.size(); i++)
			{
				fileIndex.put(files.get(i).getId(), i);
			}
			
//			int j = 0, filesAdded = 0;
			for (int i = 0; i < newFiles.size(); i++)
			{
				ShibbyFile newFile = newFiles.get(i);
				int idx = fileIndex.containsKey(newFile.getId()) ?
						fileIndex.get(newFile.getId()) : -1;
				ShibbyFile oldFile = idx != -1 ? files.get(idx) : null;
//				if (j < files.size())
//				{
//					oldFile = files.get(j);
//				}
				if (oldFile == null || forceUpdate ||
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
						Document doc = shibbydexClient.getHTMLResource(newFile.getFileUrl());
						if (!newFile.applyHTML(doc)) {
							offset++;
							System.out.println(FormattedOutput.get("FAILED to update file " +
									(i+1) + "/" + newFiles.size() + ". Skipping."));
							continue;
						}
//						if (includeFileDuration)
//						{
//							newFile.setDuration(getFileDuration(newFile));
//						}
						if (oldFile != null && oldFile.getId().equals(newFile.getId()))
						{
							// If a local file with the same name already exists,
							// update the existing file with the contents of the new file
//							filesTemp.set(j + filesAdded, newFile);
							filesTemp.remove(oldFile);
							filesTemp.add(i-offset, newFile);
						}
						else
						{
							// If the file does not exist, add it at the
							// current position in the list
							filesTemp.add(i-offset, newFile);
						}
					}
					catch (IOException ioe)
					{
						ioe.printStackTrace();
						System.out.println(FormattedOutput.get("Failed to update file " +
								(newFiles.size()-i) + "/" + newFiles.size()));
					}
				}
				if (oldFile != null && oldFile.getName().equals(newFile.getName()))
				{
					// Iterate through the local list separately
					// from the updated list
//					j++;
				}
			}
			files = filesTemp;
		}
		else if (initialUpdate != EXCLUDE_SOUNDGASM && initialUpdate != EXCLUDE_ALL)
		{
			System.out.println(FormattedOutput.
					get("Master list is up to date."));
		}
		else if (initialUpdate == EXCLUDE_SOUNDGASM || initialUpdate == EXCLUDE_ALL)
		{
			files = masterList.readLocalList();
		}
		if (files == null || files.isEmpty())
		{
			files = masterList.getFiles();
		}
	}
	
	/*
	 * Detects and applies changes made to the local soundgasm master list.
	 * These changes might be made after the data collection algorithm is modified,
	 * which results in small changes to file names or other file data.
	 */
	private void applyLocalFileChanges()
	{
//		for (int i = 0; i < files.size(); i++)
//		{
//			ShibbyFile file = files.get(i);
//			String latestShortName = file.getShortNameFromName(file.getName());
//			List<String> latestTags = file.getTagsFromName();
//			HotspotArray fileHotspots = getFileHotspots(file);
//			if (!file.getShortName().equals(latestShortName) ||
//					!file.getTags().toString().equals(latestTags.toString()) ||
//					(file.getDuration() == 0 && includeFileDuration) ||
//					(file.getType() == null || (file.getType() != null &&
//					file.getType().isEmpty())) || fileNeedsHotspotsUpdate(file, fileHotspots))
//			{
//				System.out.println(FormattedOutput.get("Applying local changes to file " +
//						(i+1) + "/" + files.size() + "..."));
//			}
//			// Compare local shortName to shortName based on original file name
//			if (!file.getShortName().equals(latestShortName))
//			{
//				file.setShortName(latestShortName);
//			}
//			if (!file.getTags().toString().equals(latestTags.toString()))
//			{
//				file.setTags(latestTags);
//			}
//			if (file.getDuration() == 0 && includeFileDuration)
//			{
//				System.out.println(FormattedOutput.get("Calculating file duration..."));
//				file.setDuration(getFileDuration(file));
//			}
//			if (file.getType() == null || (file.getType() != null && file.getType().isEmpty()))
//			{
//				file.setType("soundgasm");
//			}
//			if (fileNeedsHotspotsUpdate(file, fileHotspots))
//			{
//				System.out.println(FormattedOutput.get("Calculating file hotspots..."));
//				updateFileHotspots(file, fileHotspots);
//			}
//		}
	}
	
	/*
	 * Compute the duration of a shibbyfile in either M4A or MP3 format.
	 * Note: The 218 files from the bottom appears to take SIGNIFICANTLY
	 * longer to compute the file duration. I guess this is because
	 * the files were created in a different (less efficient) way.
	 */
	private long getFileDuration(ShibbyFile file)
	{
//		long duration = 0;
//		URL url = null;
//		try
//		{
//			url = new URL(file.getLink());
//		}
//		catch (MalformedURLException mue)
//		{
//			System.out.println(FormattedOutput.get(
//					"ERROR: Failed to get file duration."));
//		}
//		try (InputStream input = url.openStream())
//		{
//			if (file.getLink().endsWith(".m4a"))
//			{
//				AudioInfo audioInfo = new M4AInfo(input);
//				duration = audioInfo.getDuration();
//				input.close();
//			}
//			else if (file.getLink().endsWith(".mp3"))
//			{
//				HttpURLConnection conn;
//				conn = (HttpURLConnection)url.openConnection();
//				conn.setRequestMethod("HEAD");
//	            conn.getInputStream(); 
//	            long size = BigInteger.valueOf(conn.getContentLength()).longValue();
//	            conn.getInputStream().close();
//	            AudioInfo audioInfo = new MP3Info(input, size);
//				duration = audioInfo.getDuration();
//				input.close();
//			}
//		}
//		catch (Exception e)
//		{
//			System.out.println(FormattedOutput.get(
//					"ERROR: Failed to get file duration."));
//		}
//		return duration;
		return -1;
	}
	
	/*
	 * Updates Patreon files (if enabled) regardless of whether
	 * an update is available or not. It is simply not possible
	 * to tell using the current method.
	 */
	private void updatePatreonData()
	{
		if (patreonEnabled && initialUpdate != EXCLUDE_PATREON && initialUpdate != EXCLUDE_ALL)
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
		File file = new File(PATREON_DATA_PATH);
		if (file.exists())
		{
			try
			{
				BufferedReader reader = new BufferedReader(
						new FileReader(file));
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
			catch (IOException ioe)
			{
				ioe.printStackTrace();
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
//		tagsWithPatreon.clear();
		updateListTags(files, tags);
//		if (patreonEnabled)
//		{
//			updateListTags(files, tagsWithPatreon);
//			updateListTags(parsePatreonFiles(patreonFiles), tagsWithPatreon);
//			filterTags(tagsWithPatreon);
//			Collections.sort(tagsWithPatreon, new SortByFileCount());
//		}
		System.out.println(FormattedOutput.get("Filtering tags..."));
		filterTags(tags);
		System.out.println(FormattedOutput.get("Sorting tags..."));
		Collections.sort(tags, new SortByFileCount());
	}
	
	private void updateFileHotspots(ShibbyFile file,
			HotspotArray previousHotspots)
	{
		/*
		 * Verified files under threshold=40 and group=2 so far:
		 * - weeeeeeeeee
		 * - What Whispers Can Do
		 * - oh is it now
		 * - We Have To Keep Quiet
		 * - Deeper into pleasure
		 * - Ear Licking Experiment with My New 3dio
		 * - fuck this i want porn
		 * - Drunk Dorm Rescue (note: 2x playback speed?)
		 * 
		 * Files tested that seem to require random access so far
		 * (can't open stream through URL):
		 * - Yes Domina Training loop
		 * - I submit Only to Shibby
		 */
		if (previousHotspots != null)
		{
			for (HotspotArray hotspotArray : hotspots)
			{
				if (hotspotArray.equals(previousHotspots))
				{
					hotspots.remove(previousHotspots);
				}
			}
		}
		hotspots.add(new HotspotArray(file.getId(),
				AudioAnalysis.getM4AHotspots(file),
				HOTSPOTS_VERSION));
	}
	
	private boolean fileNeedsHotspotsUpdate(ShibbyFile file,
			HotspotArray fileHotspots)
	{
		return includeHotspots && (fileHotspots == null || (fileHotspots != null &&
				fileHotspots.getVersion() != HOTSPOTS_VERSION));
	}
	
	private HotspotArray getFileHotspots(ShibbyFile file)
	{
		for (HotspotArray hotspotArray : hotspots)
		{
			if (hotspotArray.getFileId().equals(file.getId()))
			{
				return hotspotArray;
			}
		}
		return null;
	}
	
	/*
	 * Updates a local list of tags to include any new files.
	 */
	private void updateListTags(List<ShibbyFile> list,
			List<ShibbyTag> tagsList)
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
					tagsList.add(new ShibbyTag(tag, file));
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
	private void filterTags(List<ShibbyTag> tagsList)
	{
		List<ShibbyTag> temp =
				(List<ShibbyTag>)((ArrayList)tagsList).clone();
		for (ShibbyTag tag : temp)
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
	private int indexOfTag(String tag, List<ShibbyTag> tagsList)
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
	 * Checks if the names of two files match.
	 * <s>On the surface, files are nearly identical at this point.</s>
	 */
	private boolean filesMostlyEqual(ShibbyFile file1, ShibbyFile file2)
	{
		return file1.getName().equals(file2.getName()) &&
				file1.getTier().equals(file2.getTier())/* &&
				file1.getDuration() == file2.getDuration()*/;
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
	
	private String[] readRemoteStorageFile()
	{
		String[] data = new String[2];
		File configFile = new File(ONLINE_STORAGE_PATH);
		BufferedReader reader;
		try
		{
			reader = new BufferedReader(new FileReader(configFile));
			data[0] = reader.readLine();
			data[1] = reader.readLine();
			reader.close();
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		if (data[0] == null || data[1] == null)
		{
			return null;
		}
		return data;
	}
	
	private void writeHotspotsToDisk()
	{
		JSONArray arr = getHotspotsJSON();
		File file = new File(HOTSPOTS_LOCAL_PATH);
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
}
