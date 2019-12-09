package io.github.kraowx.shibbyappserver.net;

import java.util.Timer;
import java.util.TimerTask;

import io.github.kraowx.shibbyappserver.DataUpdater;

public class Server
{
	public static String VERSION = "1.0.2";
	
	private int port, interval;
	private ClientListener clientListener;
	private DataUpdater dataUpdater;
	
	public Server(int port, int interval)
	{
		this.port = port;
		this.interval = interval;
		start();
	}
	
	public void start()
	{
		if (clientListener == null)
		{
			dataUpdater = new DataUpdater(interval);
			new Timer().scheduleAtFixedRate(new TimerTask()
			{
				@Override
				public void run()
				{
					if (dataUpdater.isInitialized())
					{
						clientListener = new ClientListener(port, dataUpdater);
						this.cancel();
					}
				}
			}, 0, 1000);
		}
	}
}
