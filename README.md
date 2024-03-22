# vc_server
VirtuaCreature server files.

Please note this repository is presented **without additional support** at this time. You are expected to have some general programming and networking knowledge to get this working.

Since the majority of functions are tied to *SmartFoxServer 2X*, reading the plethora of [https://docs2x.smartfoxserver.com/](help docs) is a great place to look if you're unsure on how a particular aspect of this server works.

# Server setup

## Prerequisites
* JDK 11 installation, such as [https://adoptium.net/temurin/releases/?version=11](Adoptium)
* [https://smartfoxserver.com/download/sfs2x#p=installer](SmartFoxServer 2X) working installation
* MySQL server, such as [https://mariadb.org/download/](MariaDB), and software to connect to said server and execute queries such as HeidiSQL/MySQL Workbench etc
* [https://dev.mysql.com/downloads/connector/j/](Connector/J); download and extract the Platform Independent ZIP, and drop the jar file into the `/SFS2X/extensions/__lib__/` folder on your SmartFox installation

## Installation
Ensure prerequisites are already installed and functioning before beginning.
1. Download and extract the [https://github.com/ZeroIPDev/vc_server/releases/latest](latest release)
2. Copy the `extensions` and `zones` folders into the `/SFS2X/` folder on your SmartFox installation
3. Open both `VC-Accounts.xml` and `VirtuaCreature.xml` inside `/SFS2X/zones`; navigate to the `databaseManager` node near the bottom, and replace the username/password values with the real login for your MySQL server
4. Connect to your MySQL server via your chosen client and create a new database called `sfs2x`
5. Load `/sql/db_setup.sql` and execute the query on said database, which should create 5 new `vc_` tables

At this point you should be able to launch SmartFox successfully.

# Connecting to server in-game

## Configuration
1. Copy `sfs-config.xml` from the `/config/` folder to the root of your VirtuaCreature installation
2. Open and change the `ip` value to the IP address of the server, if it's not running on the local machine

You should now be able to launch VirtuaCreature and connect to the server. If you wish to allow others to play on your server, simply send them this file with the IP correctly set and they will be able to connect.

**NOTE:** If you receive a "user not found" error before you've registered, you will need to clear your previous login cache. Simply navigate to `%appdata%\zip.vc` and delete the `ELS` folder, then restart the game and try again. You will likely have to do this when switching between servers.
