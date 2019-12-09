package io.github.kraowx.shibbyappserver;

import io.github.kraowx.shibbyappserver.net.Server;

public class Main
{
	static int DEFAULT_PORT = 1967;
	static int DEFAULT_INTERVAL = 24*60;
	
	public static void main(String[] args)
	{
		int port = getIntArg("-port", "-p", DEFAULT_PORT, args);
		int interval = getIntArg("-interval", "-i", DEFAULT_INTERVAL, args)*60*1000;
		Server server = new Server(port, interval);
	}
	
	private static int getIntArg(String arg, String shortArg,
			int defaultArg, String[] args)
	{
		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equals(arg) || args[i].equals(shortArg))
			{
				if (i < args.length-1)
				{
					try
					{
						int argInt = Integer.parseInt(args[i+1]);
						return argInt;
					}
					catch (NumberFormatException nfe)
					{
						return defaultArg;
					}
				}
			}
		}
		return defaultArg;
	}
}
