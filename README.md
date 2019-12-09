# ShibbyApp Server
This is the companion backend program for [ShibbyApp](https://github.com/kraowx/shibbyapp) responsible for collecting, organizing, and distributing Shibby's audio files.

## Installation
This is a console program so it must be run from the terminal. Download the latest version of the server [here](https://github.com/kraowx/shibbyapp-server/releases/latest). Run the program by opening a terminal and entering ```java -jar <PATH_TO_YOUR_DOWNLOADED_FILE>```, where you should replace PATH_TO_YOUR_DOWNLOADED_FILE with the absolute path to the server file you downloaded. You can optionally specify a port for the server to run on by appending ```-p <PORT_NUMBER>``` to the previous command, where you should replace PORT_NUMBER with an actual number. You can also optionally specify an interval to wait between refreshing the data from soundgasm. To specify the interval to the server, append ```-i <INTERVAL_IN_MINUTES>``` to the original command, where you should INTERVAL_IN_MINUTES with an actual number. Note that the two previous commands *can* be combined. *If no port is specified the server will default to port 1967. If no interval is specified, the server will default to update every 24 hours.*

### Example
The following command will start a new server located at the local file "server.jar" on the port 2020: ```java -jar server.jar 2020```

## Series
Since it is difficult to detect which files are part of which series, they will have to be declared manually. I have already created a file that contains a few declarations. You can download that file [here](https://raw.githubusercontent.com/kraowx/shibbyapp-server/master/seriesList.json) and save it to a file named "seriesList.json". In order for the server to detect this file, it must be placed in the same directory/folder as the server executable.
