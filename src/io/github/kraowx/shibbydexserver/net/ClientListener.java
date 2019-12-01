package io.github.kraowx.shibbydexserver.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import io.github.kraowx.shibbydexserver.DataUpdater;
import io.github.kraowx.shibbydexserver.tools.FormattedOutput;

public class ClientListener
{
	public ClientListener(int port, DataUpdater dataUpdater)
	{
		try
		{
			System.out.println(FormattedOutput.get("Starting server..."));
			ServerSocket serverSocket = new ServerSocket(port);
			System.out.println(FormattedOutput.get("Server started on port " + port + "."));
			while (true)
			{
				Socket clientSocket = serverSocket.accept();
			    PrintWriter writer =
			        new PrintWriter(clientSocket.getOutputStream(), true);
			    BufferedReader reader = new BufferedReader(
			        new InputStreamReader(clientSocket.getInputStream()));
			    new Thread(new ClientHandler(writer, reader, dataUpdater)).start();
			}
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
}
