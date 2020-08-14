# ShibbyApp Server
This is the companion backend program for [ShibbyApp](https://github.com/kraowx/shibbyapp) responsible for collecting, organizing, and distributing Shibby's audio files.

## Installation
This is a console program so it must be run from the terminal. Download the latest version of the server and all of its assets [here](https://github.com/kraowx/shibbyapp-server/releases/latest). You will need to download <code>shibbyapp-server.jar</code>, <code>seriesList.json</code>, and optionally <code>patreonScript.py</code> if you want to support Patreon files. You can also downloaded the pre-formatted soundgasm data file (sgData.json) which will **greatly** decrease the amount of time it takes to perform the first update, especially if you are using the <code>-d</code> option to include the file durations. The program also requires *at least* Java 8 to run. If you don't have Java installed already, you can download the latest version [here](https://www.java.com/en/download/). Run the program by opening a terminal and entering <code>java -jar <PATH_TO_YOUR_DOWNLOADED_FILE></code>, where you should replace <PATH_TO_YOUR_DOWNLOADED_FILE> with the absolute path to the server file you downloaded. You can optionally specify a port for the server to run on by appending <code>-p <PORT_NUMBER></code> to the previous command, where you should replace <PORT_NUMBER> with an actual number. You can also optionally specify an interval to wait between refreshing the data from soundgasm. To specify the interval to the server, append <code>-i <INTERVAL_IN_MINUTES></code> to the original command, where you should replace <INTERVAL_IN_MINUTES> with an actual number. Note that the two previous commands *can* be combined. *If no port is specified the server will default to port 1967. If no interval is specified, the server will default to update every 24 hours.* You can also append <code>--help</code> to the end of the command for a list of all available options and some information about the program.

### Example
The following command will start a new server located at the local file "server.jar" on the port 2020 and update every 25 hours: <code>java -jar shibbyapp-server.jar -p 2020 -i 1500</code>

## Patreon Integration
This is an *optional* feature that enables the server to gather the latest files from Patreon. This feature is not enabled by default. Patreon requests to a server with this feature not enabled will result in an error.

*Disclaimer: This feature is **not** a way to bypass supporting Shibby on Patreon. A Patreon account with an active pledge to Shibby ($5 minimum tier for access to patron-only content) is required when requesting data from the server. An account is also required by the server host to set up this feature. Due to limitations imposed by Patreon, all accounts requesting data from the server will be restricted to the pledge tier of the server host's account, regardless of the tier of the user. Also, if the pledge tier of the host is higher than that of the user, the ShibbyApp client will not play any files that the user would not have access to.*

### Patreon Setup
<s>To setup Patreon integration, you will need [Python3](https://www.python.org/downloads/) installed on your computer. You will also need to download the [Patreon script](https://github.com/kraowx/shibbyapp-server/releases/latest/download/patreonScript.py) which handles communication with Patreon. Move <code>patreonScript.py</code> to the same location as <code>shibbyapp-server.jar</code>. Next, you will need to install a few dependencies in order for the script to work. Open a terminal and enter the command <code>pip3 install requests cfscrape</code>. Press enter and wait for the installation to finish. Finally, you need to add your Patreon account to the server configuration. Simply run the server with the --config-patreon option: <code>java -jar shibbyapp-server.jar --config-patreon</code> and you will be prompted to enter the email and password for your Patreon account. This should create the file <code>shibbyapp-server.config</code> in the same location as the server executable containing your Patreon account info. The next time you run the server normally, the server will start gathering data from your Patreon feed.</s> Patreon integration is currently broken. Hopefully I will be able to get this working again if/when ShibbyDex adds Patreon files.

### Hotspots (Experimental)
Hotspots are locations defined in an audio file that attempt to produce a more intense reaction to certain parts of the file. Currently, hotspots are somwhat arbitrarily defined to react to the amplitude/loudness of the file. This is not a perfect solution by any means, so I will probably be tweaking it over time. A somewhat significant limitation to this is the amount of time that it takes to compute the hotspots for each file. Each file must be essentially downloaded and analyzed separately, which can take up to multiple days depending on hardware and internet speed.

## Series
Since it is difficult to detect which files are part of which series, they will have to be declared manually. I have already created a file that contains a few declarations. You can download that file [here](https://raw.githubusercontent.com/kraowx/shibbyapp-server/master/seriesList.json) and save it to a file named "seriesList.json". In order for the server to detect this file, it must be placed in the same directory/folder as the server executable. **Update: This feature will likely be automated (built-in) in the future thanks to the extensive amount of new data available on ShibbyDex**

### Series Structure
The series file uses the JSON format, where each series is a JSON object declared in the top level. A series JSON object uses the following structure: <code>{name: string, fileCount: int, files: [file1, file2, ...]}</code>, where "name" (required) is the name of the series, "fileCount" (optional) is the number of files in the series, and "files" (required) is a JSON array of the file data for each file. Each file in the "files" JSON array uses the following structure: <code>{name: string, id: string, shortName: string, link: string, tags: [tag1, tag2, ...], description: string, type: string}</code>, where "name" (required) is the name of the file, "id" (optional) is the unique base64 encoded ID created from the original file name (will be generated if omitted), "shortName" (optional) is the file name without the trailing tags (will be generated if omitted), "link" (required) is the direct link to the audio file, "tags" (optional) is a JSON array of strings that will be used *instead* of the tags detected in the file name, "description" (optional) is the text that describes the contents of the file, and "type" (optional) is the type that the file should be displayed as. ShibbyApp currently supports the types "soundgasm", "patreon", and "user", where soundgasm files are displayed with no special properties and the other tags are displayed with special colored tags. If no type is specified then the type "soundgasm" is assumed.

### Adding a Series
Adding a series is actually quite straightforward, and you can really just copy the format/pattern used in the <code>seriesList.json</code> file to create a new series. You will need to construct a JSON object for each file you want to add to the series by following the structure described above. Once you have the JSON objects for each file, you can construct a JSON object for the series and simply add it to the top-level JSON array.

## Interface Structure
The server interfaces with the client over HTTP. The base structure of a request is <code>http://&lt;IP&gt;:&lt;PORT&gt;?version=&lt;VERSION&gt;&type=&lt;TYPE&gt;</code>. Where &lt;VERSION&gt; is the version of the client, and &lt;TYPE&gt; is the type of request being made. The available types are described below. You can also express a request as <code>http://&lt;IP&gt;:&lt;PORT&gt;/&lt;TYPE&gt;?version=&lt;VERSION&gt;</code>.

### Version
Returns the current version of the server in the format <code>{"type": "VERSION", "data": &lt;VERSION&gt;}</code>. No <code>version</code> parameter required.

### Verify\_Patreon\_Account
Checks if a Patreon account exists and has an active pledge to Shibby. Returns <code>{"type": "VERIFY\_PATREON\_ACCOUNT", "data": true/false}</code>.

### All
Returns all the data that can be requested from the other types. This includes all soundgasm files (FILES), all tags (TAGS), series (SERIES), and Patreon files (PATREON\_FILES). The format is <code>{"type": "ALL", "data": {"files": [...], "tags": [...], "series": [...], "patreonFiles": [...]}}</code>. Note that Patreon files will only be included if the request includes the "email=&lt;EMAIL&gt;" and "password=&lt;PASSWORD&gt;" parameters. For example: <code>http://&lt;IP&gt;:&lt;PORT&gt;?version=&lt;VERSION&gt;&type=all&email=&lt;EMAIL&gt;&password=&lt;PASSWORD&gt;</code>. If any error occurs when validating the Patreon account, then the Patreon files will not be included and no error will be sent.

### Files
Returns all soundgasm files in the format <code>{"type": "FILES", "data": ["name", &lt;NAME&gt;, "link": &lt;LINK&gt;, "description": &lt;DESCRIPTION&gt;]}</code>.

### Tags
Returns all tags in the format <code>{"type": "TAGS", "data": ["name": &lt;TAG&gt;, "files": [&lt;HASH&gt;, &lt;HASH&gt;, ...]]}</code>. Where &lt;HASH&gt; is the SHA256 hash of each file's original name. Note that Patreon files will only be included if the request includes the "email=&lt;EMAIL&gt;" and "password=&lt;PASSWORD&gt;" parameters. For example: <code>http://&lt;IP&gt;:&lt;PORT&gt;?version=&lt;VERSION&gt;&type=all&email=&lt;EMAIL&gt;&password=&lt;PASSWORD&gt;</code>. If any error occurs when validating the Patreon account, then the Patreon files will not be included and no error will be sent.

### Series
Returns all series in the format <code>{"type": "SERIES", "data": [{"name": &lt;NAME&gt;, "files": [&lt;FILE&gt;, &lt;FILE&gt;, ...]}, ...]}</code>. Where &lt;FILE&gt; is a file (json object) in the format specified above.

### Patreon\_Files
On success, returns all Patreon files in the format <code>{"type": "PATREON\_FILES", "data": [&lt;FILE&gt;, &lt;FILE&gt;, ...]}</code>. Where &lt;FILE&gt; is a file (json object) in the format specified above. May also return a number of other error responses with no associated data. These errors include: VERIFY\_PATREON\_ACCOUNT (Patreon email confirmation is required on the device), TOO\_MANY\_REQUESTS (Patreon is throttling requests on the account for 10 mins), and BAD\_ACCOUNT (invalid Patreon account).

### Hotspots
Returns the "hotspots" of all supported files in the form <code>{"type": HOTSPOTS, "data": [["id": &lt;FILE\_ID&gt;, "startTime": &lt;START\_TIME&gt;, "endTime": &lt;END\_TIME&gt;], ...]}</code>. Where &lt;FILE\_ID&gt; is the ID (SHA256 hash) of the file associated with the hotspot, &lt;START\_TIME&gt; is the starting time of the hotspot in milliseconds, and &lt;END\_TIME&gt; is the ending time of the hotspot in milliseconds. Also note that hotspots are locations within a file that are somewhat arbitrarily defined to attempt to produce a more intense reaction to Shibby's voice.

## Used Libraries
- [JSON-java](https://github.com/stleary/JSON-java)
- [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd)
- [Http Request](https://github.com/kevinsawicki/http-request)
- [JSoup](https://github.com/jhy/jsoup)
- [JAAD](http://jaadec.sourceforge.net)
- [Ithaka Audio Info](https://github.com/beckchr/ithaka-audioinfo)
