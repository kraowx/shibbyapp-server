package io.github.kraowx.shibbydexserver.net;

import java.util.Timer;
import java.util.TimerTask;

import io.github.kraowx.shibbydexserver.DataUpdater;

public class Server
{
	private int port;
	private ClientListener clientListener;
	private DataUpdater dataUpdater;
	
	public Server(int port)
	{
		this.port = port;
		start();
	}
	
	public void start()
	{
		if (clientListener == null)
		{
			dataUpdater = new DataUpdater();
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
