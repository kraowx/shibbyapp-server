package io.github.kraowx.shibbyappserver;

import io.github.kraowx.shibbyappserver.net.Server;

public class Main
{
	static int DEFAULT_PORT = 1967;
	
	public static void main(String[] args)
	{
		int port = DEFAULT_PORT;
		if (args.length > 0)
		{
			try
			{
				port = Integer.parseInt(args[0]);
			}
			catch (NumberFormatException nfe)
			{
				System.out.println("Invalid argument \"" +
						args[0] + "\". Terminating...");
				System.exit(1);
			}
		}
		Server server = new Server(port);
	}
}
