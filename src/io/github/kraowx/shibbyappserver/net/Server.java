package io.github.kraowx.shibbyappserver.net;

import java.util.Timer;
import java.util.TimerTask;

import io.github.kraowx.shibbyappserver.DataUpdater;

public class Server
{
	public static String VERSION = "1.1.3";
	
	private ClientListener clientListener;
	private DataUpdater dataUpdater;
	
	public Server(int port, int interval, boolean heavyUpdate, int initialUpdate)
	{
		start(port, interval, heavyUpdate, initialUpdate);
	}
	
	public void start(int port, int interval, boolean heavyUpdate, int initialUpdate)
	{
		if (clientListener == null)
		{
			dataUpdater = new DataUpdater(interval, heavyUpdate, initialUpdate);
			new Timer().scheduleAtFixedRate(new TimerTask()
			{
				@Override
				public void run()
				{
					if (dataUpdater.isInitialized())
					{
						clientListener = new ClientListener(
								port, dataUpdater);
						this.cancel();
					}
				}
			}, 0, 1000);
		}
	}
}
