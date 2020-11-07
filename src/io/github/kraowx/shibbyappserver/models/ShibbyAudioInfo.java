package io.github.kraowx.shibbyappserver.models;

import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ShibbyAudioInfo {
	private String fileType;
	private String audioType;
	private String audioUrl;
	private String freeAudioUrl; // available if not logged in (null for patreon files)
	private String effects;
	private String background;
	
	public static ShibbyAudioInfo fromHTML(Document doc, String fileId) {
		Element row = doc.select("dl[class*=row row-cols-2 text-light]").get(1);
		Elements keyTable = row.select("dt[class*=col-sm-3]");
		Elements valueTable = row.select("dd[class*=col-sm-3]");
		ShibbyAudioInfo audioInfo = new ShibbyAudioInfo();
		audioInfo.fileType = getField("file type", keyTable, valueTable);
		audioInfo.audioType = getField("audio type", keyTable, valueTable);
		try {
			audioInfo.freeAudioUrl = doc.select("source").attr("src");
		}
		catch (Exception e) {
			audioInfo.freeAudioUrl = null; // always null for patreon files
		}
		audioInfo.audioUrl = "https://shibbydex.com/audio/" + fileId;
		audioInfo.effects = getField("effects", keyTable, valueTable);
		audioInfo.background = getField("background", keyTable, valueTable);
		return audioInfo;
	}
	
	private static String getField(String key, Elements keyTable, Elements valueTable) {
		int i;
		for (i = 0; i < keyTable.size() &&
				!keyTable.get(i).text().equalsIgnoreCase(key); i++);
		if (i < valueTable.size() && !keyTable.isEmpty() && !valueTable.isEmpty()) //key and value exists
			return valueTable.get(i).text();
		return null;
	}
	
	public String getFileType() {
		return fileType;
	}
	
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}
	
	public String getAudioType() {
		return audioType;
	}
	
	public void setAudioType(String audioType) {
		this.audioType = audioType;
	}
	
	public String getAudioURL() {
		return audioUrl;
	}
	
	public void setAudioURL(String audioUrl) {
		this.audioUrl = audioUrl;
	}
	
	public String getFreeAudioURL() {
		return freeAudioUrl;
	}
	
	public void setFreeAudioURL(String freeAudioUrl) {
		this.freeAudioUrl = freeAudioUrl;
	}
	
	public String getEffects() {
		return effects;
	}
	
	public void setEffects(String effects) {
		this.effects = effects;
	}
	
	public String getBackground() {
		return background;
	}
	
	public void setBackground(String background) {
		this.background = background;
	}
	
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("file_type", fileType);
		json.put("audio_type", audioType);
		json.put("audio_url", audioUrl);
		json.put("free_audio_url", freeAudioUrl);
		json.put("effects", effects);
		json.put("background", background);
		return json;
	}
	
	public static ShibbyAudioInfo fromJSON(JSONObject json) {
		ShibbyAudioInfo audioInfo = new ShibbyAudioInfo();
		audioInfo.fileType = json.has("file_type") ? json.getString("file_type") : null;
		audioInfo.audioType = json.has("audio_type") ? json.getString("audio_type") : null;
		audioInfo.audioUrl = json.has("audio_url") ? json.getString("audio_url") : null;
		audioInfo.freeAudioUrl = json.has("free_audio_url") ? json.getString("free_audio_url") : null;
		audioInfo.effects = json.has("effects") ? json.getString("effects") : null;
		audioInfo.background = json.has("background") ? json.getString("background") : null;
		return audioInfo;
	}
}
