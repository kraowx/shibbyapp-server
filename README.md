# ShibbyApp Server
This is a companion backend program for ShibbyApp responsible for collecting, organizing, and distributing Shibby's audio files.

## Installation
This is a console program so it must be run from the terminal. Download the latest version of the server [here](https://github.com/kraowx/shibbyapp-server/releases/latest). Run the program by opening a terminal and entering ```java -jar <PATH_TO_YOUR_DOWNLOADED_FILE>```. You can also specify a port for the server to run on by appending an integer to the previous command. *If no port is specified the server will default to port 1967.*

### Example
The following command will start a new server located at the local file "server.jar" on the port 2020: ```java -jar server.jar 2020```

## Series
Since it is difficult to detect which files are part of which series, they will have to be declared manually. I have already created a file that contains seven declarations. You can download that file [here](https://raw.githubusercontent.com/kraowx/shibbyapp-server/master/seriesList.json) and save it to a file named "seriesList.json". In order for the server to detect this file, it must be placed in the same directory/folder as the server executable.
