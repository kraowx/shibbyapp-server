package io.github.kraowx.shibbyappserver.tools;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import io.github.kraowx.shibbyappserver.models.Hotspot;
import io.github.kraowx.shibbyappserver.models.ShibbyFile;
import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.aac.SampleBuffer;
import net.sourceforge.jaad.mp4.MP4Container;
import net.sourceforge.jaad.mp4.api.AudioTrack;
import net.sourceforge.jaad.mp4.api.Frame;
import net.sourceforge.jaad.mp4.api.Movie;
import net.sourceforge.jaad.mp4.api.Track;

public class AudioAnalysis
{
	// Arbitrary constants that control how hotspots are detected
	final static int HOTSPOT_THRESHOLD_LEFT = 40;
	final static int HOTSPOT_THRESHOLD_RIGHT = 40;
	final static int GROUP_FILTER = 1;  // Number of consecutive threshold passes to trigger
	
	// Adapted from: https://stackoverflow.com/questions/45660234/java-play-aac-encoded-audio-jaad-decoder
	public static Hotspot[] getM4AHotspots(ShibbyFile file)
	{
		List<Hotspot> hotspots = new ArrayList<Hotspot>();
		try
		{
			MP4Container container = new MP4Container(
					new URL(file.getAudioURL()).openStream());
			Movie movie = container.getMovie();
			List<Track> content = movie.getTracks();
			if (content.isEmpty() || file.getDuration() == 0)
			{
				return null;
			}
			AudioTrack track = (AudioTrack)movie.getTracks().get(0);
			Decoder decoder = new Decoder(track.getDecoderSpecificInfo());
			SampleBuffer buf = new SampleBuffer();
			byte[] bytes;
			Frame frame;
			int leftAccum, leftAmp;  // channel 0 -> left ear
			int rightAccum, rightAmp;  // channel 1 -> right ear
			int group = 0;
			int n = 0;
			while (track.hasMoreFrames())
			{
				frame = track.readNextFrame();
				decoder.decodeFrame(frame.getData(), buf);
				bytes = buf.getData();
				leftAccum = rightAccum = 0;
				for (int i = 0; i < bytes.length; i+=4)
				{
					leftAccum += Math.abs(bytes[i]) + Math.abs(bytes[i+1]);
					rightAccum += Math.abs(bytes[i+2]) + Math.abs(bytes[i+3]);
				}
				leftAmp = leftAccum/(bytes.length/2);
				rightAmp = rightAccum/(bytes.length/2);
				if (hotspotExists(leftAmp, rightAmp))
				{
					group++;
				}
				if (group > GROUP_FILTER && !hotspotExists(leftAmp, rightAmp))
				{
					// Create hotspot in terms of frames
					hotspots.add(new Hotspot((long)(n-group), (long)n));
					group = 0;
				}
				n++;
			}
			
			for (int i = 0; i < hotspots.size(); i++)
			{
				Hotspot hs = hotspots.get(i);
				// Update start and end times in terms of time (instead of frames)
				hs.setStartTime((int)((hs.getStartTime()/(float)n)*file.getDuration()));
				hs.setEndTime((int)((hs.getEndTime()/(float)n)*file.getDuration()));
				hs.setDuration((int)((hs.getDuration()/(float)n)*file.getDuration()));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return hotspots.toArray(new Hotspot[0]);
	}
	
	static boolean hotspotExists(int leftAmp, int rightAmp)
	{
		return leftAmp > HOTSPOT_THRESHOLD_LEFT || rightAmp > HOTSPOT_THRESHOLD_RIGHT;
	}
}
