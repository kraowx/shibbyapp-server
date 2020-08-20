package io.github.kraowx.shibbyappserver.models;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/*
 * ShibbyFile v3:
 * 	(to match ShibbyDex)
 * 
 * + Name
 * + Id (shibbydex uuid)
 * + Version (shibbyfile version number)
 * + ViewType (public/patreon/...)
 * + Duration (milliseconds)
 * + BasicInfo
 *   + Author
 *   + Artist
 *   + Release
 *   + Tone
 *   + Setting
 *   + Consent
 *   + DS
 *   + Orgasm
 *   + Instructions
 *   + IntendedEffect
 * + AudioInfo
 *   + FileType (full/clip/series/...)
 *   + AudioType (stereo/mono/...)
 *   + AudioUrl (direct to m4a/mp3)
 *   + Effects
 *   + Background (binaurals/naked/...)
 * + Tags (string array)
 * + HypnosisInfo
 *   + Style (pure/...)
 *   + Level (beginner/intermediate/advanced/...)
 *   + Induction
 *   + Deepener
 *   + Body
 *   + Wakener (true/false)
 *   + Aftercare (true/false)
 * + Triggers (string array)
 * + Description
 */
public class ShibbyFile
{
	public static final int CURRENT_VERSION = 3;
	public static final String DEFAULT_VIEW_TYPE = "public";
	
	private final String SHIBBYDEX_ROOT_URL = "https://shibbydex.com/";
	private final String SHIBBYDEX_FILE_URL = SHIBBYDEX_ROOT_URL + "file/";
	
	private String name;
	private String id;
	private int version;
	private String viewType;
	private long duration;
	private ShibbyBasicInfo basicInfo;
	private ShibbyAudioInfo audioInfo;
	private List<String> tags;
	private ShibbyHypnosisInfo hypnosisInfo;
	private List<String> triggers;
	private String description;
	
	public ShibbyFile(String name, String id,
			String viewType, long duration)
	{
		this.name = name;
		this.id = id;
		this.viewType = viewType;
		this.duration = duration;
	}
	
	public JSONObject toJSON()
	{
		JSONObject json = new JSONObject();
		json.put("name", name);
		json.put("id", id);
		json.put("version", version);
		json.put("view_type", viewType);
		json.put("duration", duration);
		json.put("basic_info", basicInfo.toJSON());
		json.put("audio_info", audioInfo.toJSON());
		JSONArray tagsJson = new JSONArray();
		if (tags != null)
		{
			for (String tag : tags)
			{
				tagsJson.put(tag);
			}
		}
		json.put("tags", tagsJson);
		if (hypnosisInfo != null)
		{
			json.put("hypnosis_info", hypnosisInfo.toJSON());
		}
		JSONArray triggersJson = new JSONArray();
		if (triggers != null)
		{
			for (String trigger : triggers)
			{
				triggersJson.put(trigger);
			}
		}
		json.put("triggers", triggersJson);
		json.put("description", description);
		return json;
	}
	
	public static ShibbyFile fromJSON(String jsonStr)
    {
        ShibbyFile file = new ShibbyFile(null, null, null, -1);
        JSONObject json = new JSONObject(jsonStr);
        file.name = json.has("name") ? json.getString("name") : null;
        file.id = json.has("id") ? json.getString("id") : null;
        file.version = json.has("version") ? json.getInt("version") : 0;
        file.viewType = json.has("view_type") ? json.getString("view_type") : DEFAULT_VIEW_TYPE;
        file.duration = json.has("duration") ? json.getLong("duration") : 0;
        file.basicInfo = json.has("basic_info") ?
        		ShibbyBasicInfo.fromJSON(json.getJSONObject("basic_info")) :
        			new ShibbyBasicInfo();
        file.audioInfo = json.has("audio_info") ?
        		ShibbyAudioInfo.fromJSON(json.getJSONObject("audio_info")) :
        			new ShibbyAudioInfo();
        file.tags = new ArrayList<String>();
        for (Object tag : json.getJSONArray("tags"))
        {
        	file.tags.add((String)tag);
        }
        file.hypnosisInfo = json.has("hypnosis_info") ?
        		ShibbyHypnosisInfo.fromJSON(json.getJSONObject("hypnosis_info")) :
        			new ShibbyHypnosisInfo();
        file.triggers = new ArrayList<String>();
        for (Object trigger : json.getJSONArray("triggers"))
        {
        	file.triggers.add((String)trigger);
        }
        file.description = json.getString("description");
        return file;
    }
	
	public void applyHTML(Document doc)
	{
		this.name = doc.select("h1[class*=display-4 text-center text-light shibbydex-font-accent]").text();
//		this.id = getIdFromURL(fileUrl);
		this.version = CURRENT_VERSION;
		this.basicInfo = ShibbyBasicInfo.fromHTML(doc);
		this.audioInfo = ShibbyAudioInfo.fromHTML(doc);
		this.tags = getTagsFromHTML(doc);
		this.hypnosisInfo = ShibbyHypnosisInfo.fromHTML(doc);
		this.triggers = getTriggersFromHTML(doc);
		this.description = doc.select("p[class*=lead text-light]").text();
	}
	
	private List<String> getTagsFromHTML(Document doc)
	{
		return getCardsFromHTML(doc, 0);
	}
	
	private List<String> getTriggersFromHTML(Document doc)
	{
		return getCardsFromHTML(doc, 1);
	}
	
	private List<String> getCardsFromHTML(Document doc, int index)
	{
		Element container = doc.select("p[class*=col-12 card-text h4 text-center text-light]").get(index);
		Elements cards = container.select("a");
		List<String> items = new ArrayList<String>();
		for (Element e : cards)
		{
			items.add(e.text());
		}
		return items;
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public String getId()
	{
		return id;
	}
	
	public void setId(String id)
	{
		this.id = id;
	}
	
	public int getVersion()
	{
		return version;
	}
	
	public void setVersion(int version)
	{
		this.version = version;
	}
	
	public String getViewType()
	{
		return viewType;
	}
	
	public void setViewType(String viewType)
	{
		this.viewType = viewType;
	}
	
	public long getDuration()
	{
		return duration;
	}
	
	public void setDuration(long duration)
	{
		this.duration = duration;
	}
	
	public String getFileUrl()
	{
		return SHIBBYDEX_FILE_URL + id + "?spoilers=1";
	}
	
	public ShibbyBasicInfo getBasicInfo()
	{
		return basicInfo;
	}
	
	public String getAuthor()
	{
		return basicInfo.getAuthor();
	}
	
	public String getArtist()
	{
		return basicInfo.getArtist();
	}
	
	public String getReleaseDate()
	{
		return basicInfo.getRelease();
	}
	
	public String getAudienceType()
	{
		return basicInfo.getAudience();
	}
	
	public String getTone()
	{
		return basicInfo.getTone();
	}
	
	public String getSetting()
	{
		return basicInfo.getSetting();
	}
	
	public String getConsentType()
	{
		return basicInfo.getConsent();
	}
	
	public String getDSType()
	{
		return basicInfo.getDS();
	}
	
	public String getOrgasm()
	{
		return basicInfo.getOrgasm();
	}
	
	public String getInstructions()
	{
		return basicInfo.getInstructions();
	}
	
	public String getIntendedEffect()
	{
		return basicInfo.getIntendedEffect();
	}
	
	public ShibbyAudioInfo getAudioInfo()
	{
		return audioInfo;
	}
	
	public String getAudioFileType()
	{
		return audioInfo.getFileType();
	}
	
	public String getAudioType()
	{
		return audioInfo.getAudioType();
	}
	
	public String getAudioURL()
	{
		return audioInfo.getAudioURL();
	}
	
	public String getAudioEffects()
	{
		return audioInfo.getEffects();
	}
		
	public String getAudioBackground()
	{
		return audioInfo.getBackground();
	}
	
	public List<String> getTags()
	{
		return tags;
	}
	
	public void setTags(List<String> tags)
	{
		this.tags = tags;
	}
	
	public ShibbyHypnosisInfo getHypnosisInfo()
	{
		return hypnosisInfo;
	}
	
	public String getHypnosisStyle()
	{
		return hypnosisInfo.getStyle();
	}
	
	public String getHypnosisLevel()
	{
		return hypnosisInfo.getLevel();
	}
	
	public String getHypnosisInduction()
	{
		return hypnosisInfo.getInduction();
	}
	
	public String getHypnosisDeepener()
	{
		return hypnosisInfo.getDeepener();
	}
	
	public String getHypnosisBody()
	{
		return hypnosisInfo.getBody();
	}
	
	public boolean hasWakener()
	{
		return hypnosisInfo.hasWakener();
	}
	
	public boolean hasAftercare()
	{
		return hypnosisInfo.hasAftercare();
	}
	
	public List<String> getTriggers()
	{
		return triggers;
	}
	
	public void setTriggers(List<String> triggers)
	{
		this.triggers = triggers;
	}
	
	public String getDescription()
	{
		return description;
	}
	
	public void setDescription(String description)
	{
		this.description = description;
	}
	
	@Override
	public String toString()
	{
		return name;
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof ShibbyFile)
		{
			return this.toJSON().toString().equals(
					((ShibbyFile)other).toJSON().toString());
		}
		return false;
	}
}
