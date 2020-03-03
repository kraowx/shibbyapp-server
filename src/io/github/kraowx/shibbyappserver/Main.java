package io.github.kraowx.shibbyappserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import io.github.kraowx.shibbyappserver.net.Server;

public class Main
{
	static int DEFAULT_PORT = 1967;
	static int DEFAULT_INTERVAL = 24*60;
	static boolean DEFAULT_HEAVY_UPDATE = false;
	
	public static void main(String[] args)
	{
		int port = getIntArg("--port", "-p", DEFAULT_PORT, args);
		int interval = getIntArg("--interval", "-i", DEFAULT_INTERVAL, args)*60*1000;
		boolean heavyUpdate = hasArg("--heavy-update", "-h", false, args);
		boolean noUpdate = hasArg("--no-update", "-n", false, args);
		boolean noSoundgasm = hasArg("--no-soundgasm", null, false, args);
		boolean noPatreon = hasArg("--no-patreon", null, false, args);
		boolean help = hasArg("--help", null, false, args);
		boolean version = hasArg("--version", null, false, args);
		boolean configPatreon = hasArg("--config-patreon", null, false, args);
		if (help)
		{
			System.out.println("Usage: exec [-h] [-p port] [-i interval]");
			System.out.println("Companion server for ShibbyApp that collects, " +
					"organizes, and distributes Shibby's audio files.");
			System.out.println("\nOptions:");
			System.out.println("  -p, --port            specifies the port for the server to run on");
			System.out.println("  -i, --interval        specifies the interval to update on");
			System.out.println("  -h, --heavy-update    forces each file to be updated on each update");
			System.out.println("  -n  --no-update       initial data update is skipped");
			System.out.println("        --no-soundgasm  initial soundgasm update is skipped");
			System.out.println("        --no-patreon    initial patreon update is skipped");
			System.out.println("      --help            display this help and exit");
			System.out.println("      --version         output version information and exit");
			System.out.println("      --config-patreon  setup Patreon integration feature");
			System.out.println("\nExit status:");
			System.out.println("1  invalid update interval");
			System.out.println("2  failed to read input");
			System.out.println("3  failed to start the server");
			System.exit(0);
		}
		else if (version)
		{
			System.out.println("shibbyapp-server " + Server.VERSION);
			System.out.println("https://github.com/kraowx/shibbyapp-server");
			System.exit(0);
		}
		else if (configPatreon)
		{
			String email = null, password = null;
			BufferedReader reader = null;
			BufferedWriter writer = null;
			try
			{
				reader = new BufferedReader(new InputStreamReader(System.in));
				while (email == null)
				{
					System.out.print("Enter the email for your Patreon account: ");
					email = reader.readLine();
				}
				while (password == null)
				{
					System.out.print("Enter the password for your Patreon account: ");
					password = reader.readLine();
				}
				File file = new File(DataUpdater.CONFIG_FILE_PATH);
				file.createNewFile();
				writer = new BufferedWriter(new FileWriter(file));
				writer.write(email + "\n" + password + "\n");
				writer.close();
				System.out.println("Configuration saved to " + file.getAbsolutePath());
				System.exit(0);
			}
			catch (IOException ioe)
			{
				System.out.println("error - failed to read input");
				System.exit(2);
			}
		}
		else if ((float)interval/(60*60*1000) < 6)
		{
			System.out.println("error - update interval must be greater " +
					"than or equal to six hours (360 mins)");
			System.exit(1);
		}
		else if (heavyUpdate)
		{
			System.out.println("WARNING: You have enabled heavy update. " +
					"This will not only cause the server to take longer to update, " +
					"but it will also put more stress on the soundgasm servers");
		}
		int initialUpdate = 0;
		if (noSoundgasm)
		{
			initialUpdate = 1;
		}
		if (noPatreon)
		{
			initialUpdate = 2;
		}
		if (noUpdate || (noSoundgasm && noPatreon))
		{
			initialUpdate = 3;
		}
		try
		{
			Server server = new Server(port, interval,
					heavyUpdate, initialUpdate);
		}
		catch (IOException ioe)
		{
			System.out.println("ERROR: Failed to start the server");
			ioe.printStackTrace();
			System.exit(3);
		}
	}
	
	private static int getIntArg(String arg, String shortArg,
			int defaultArg, String[] args)
	{
		for (int i = 0; i < args.length; i++)
		{
			if ((arg != null && args[i].equals(arg)) || args[i].equals(shortArg))
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
	
	private static boolean hasArg(String arg, String shortArg,
			boolean defaultArg, String[] args)
	{
		for (String a : args)
		{
			if ((arg != null && a.equals(arg)) || a.equals(shortArg))
			{
				return true;
			}
		}
		return defaultArg;
	}
}
