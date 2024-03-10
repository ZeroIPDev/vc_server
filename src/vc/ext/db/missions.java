package vc.ext.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class missions extends BaseClientRequestHandler {
	@Override
	public void handleClientRequest(User u, ISFSObject p) {
		Boolean isUpdate = p.getBool("Update");
		String eventName = p.getUtfString("EventName");
		Connection connection;
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		try {
			connection = dbManager.getConnection();
			if(isUpdate) {
				Boolean success = updateEvent(u, eventName, connection);
				if(success) {
					event event = new event();
					String reward = event.giveReward(u, connection, eventName);
					if(reward.length() > 0) {
						sendResultReward(u, reward);
					} else {
						sendError(u, "Failed to add reward to account.");
					}
				} else {
					sendError(u, "Failed to update account.");
				}
			} else {
				String taskHandle = p.getUtfString("TaskHandle");
				ArrayList<String> missions = getMissionData(taskHandle, connection);
				if(missions.size() > 0) {
					String reward = getMissionReward(eventName, connection);
					sendMissionResult(u, missions, reward);
				} else {
					sendError(u, "Failed to get mission data.");
				}
			}
			connection.close();
		}
		catch(SQLException e) {
			trace("Error: ", e);
			sendError(u, e.getMessage());
		}
	}
	public Boolean updateEvent(User u, String eventName, Connection connection) throws SQLException {
		PreparedStatement stmt = connection.prepareStatement("SELECT Event FROM vc_accounts WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		stmt.setString(1, u.getName());
		ResultSet res = stmt.executeQuery();
		if(res.first()) {
			String eventString = res.getString("Event");
			if(eventString != null && eventString.length() > 0) {
				String[] compEvents = eventString.split(",");
				for(int i=0;i<compEvents.length;i++) {
					if(compEvents[i].contentEquals(eventString)) {
						return false;
					}
				}
			} else {
				eventString = "";
			}
			String newEvents;
			if(eventString.length() > 0) {
				newEvents = eventString + "," + eventName;
			} else {
				newEvents = eventName;
			}
			stmt = connection.prepareStatement("UPDATE vc_accounts SET Event=? WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			stmt.setString(1, newEvents);
			stmt.setString(2, u.getName());
			int ures = stmt.executeUpdate();
			if(ures > 0) {
				return true;
			}
		}
		return false;
	}
	public ArrayList<String> getMissionData(String taskHandle, Connection connection) throws SQLException {
		ArrayList<String> missions = new ArrayList<String>();
		PreparedStatement stmt = connection.prepareStatement("SELECT Tasks FROM vc_missions WHERE TaskHandle=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		stmt.setString(1, taskHandle);
		ResultSet res = stmt.executeQuery();
		if(res.first()) {
			String[] taskString = res.getString("Tasks").split(";");
			for(int i=0;i<taskString.length;i++) {
				missions.add(taskString[i]);
			}
		}
		return missions;
	}
	public String getMissionReward(String eName, Connection connection) throws SQLException {
		PreparedStatement stmt = connection.prepareStatement("SELECT Reward FROM vc_events WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		stmt.setString(1, eName);
		ResultSet res = stmt.executeQuery();
		if(res.first()) {
			return(res.getString("Reward"));
		}
		return "";
	}
	public boolean generateCampaign(Connection connection) throws SQLException, ParseException {
		//Set variables
		Calendar cal = Calendar.getInstance();
		String missionMonth = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
		int missionYear = cal.get(Calendar.YEAR);
		Random rand = new Random();
		int maxMissions = 4 + rand.nextInt(2);
		int[] taskTypes = new int[maxMissions];
		int[] taskAmt = new int[maxMissions];
		int[] reward = new int[2];
		//Create list of possible missions
		ArrayList<Integer> missions = new ArrayList<Integer>();
		for(int i=0;i<maxMissions;i++) {
			missions.add(i);
		}
		//Generate tasks
		for(int i=0;i<maxMissions;i++) {
			int index = rand.nextInt(missions.size());
			int multiplier = 1;
			taskTypes[i] = missions.get(index);
			missions.remove(index);
			if(taskTypes[i] == 5) {
				multiplier = 10000;
			}
			if(i <= 1) {
				taskAmt[i] = 5 * multiplier;
			} else if(i <= 3) {
				taskAmt[i] = (5 + rand.nextInt(2)) * multiplier;
			} else {
				taskAmt[i] = 7 * multiplier;
			}
		}
		//Update mission to database
		String missionString = buildMissionString(taskTypes, taskAmt);
		String mTaskHandle = missionMonth + "_" + missionYear;
		PreparedStatement stmt = connection.prepareStatement("UPDATE vc_missions SET TaskHandle=?, Tasks=? WHERE ID=1", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		stmt.setString(1, mTaskHandle);
		stmt.setString(2, missionString);
		int res = stmt.executeUpdate();
		if(res < 1) {
			return false;
		}
		//Generate reward
		reward[0] = rand.nextInt(5);
		if(reward[0] == 0) { //Money
			reward[1] = 100000 * (1 + rand.nextInt(2));
		} else if(reward[0] == 1) { //Chest (Gold)
			reward[1] = 9 + rand.nextInt(2);
		} else if(reward[0] == 2) { //Synths
			reward[1] = 500 * (1 + rand.nextInt(2));
		} else if(reward[0] == 3) { //Revive
			if(rand.nextInt(2) > 0) {
				reward[1] = 10;
			} else {
				reward[1] = 5;
			}
		} else if(reward[0] == 4) { //XP Boost (12HR/24HR)
			reward[1] = 1 + rand.nextInt(2);
		}
		//Add event to database
		Calendar calEnd = Calendar.getInstance();
		calEnd.set(Calendar.DAY_OF_MONTH, calEnd.getActualMaximum(Calendar.DAY_OF_MONTH));
		stmt = connection.prepareStatement("INSERT INTO vc_events (Name, Description, Data, Reward, Start, End) VALUES (?, ?, ?, ?, ?, ?)", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		stmt.setString(1, missionMonth + " " + missionYear + " Campaign");
		stmt.setString(2, "New campaign missions are here for " + missionMonth + "! Complete them this month for a large reward!");
		stmt.setString(3, "3," + mTaskHandle);
		stmt.setString(4, reward[0] + "," + reward[1]);
		stmt.setString(5, clock.dateFormat.format(cal.getTime()));
		stmt.setString(6, clock.dateFormat.format(calEnd.getTime()));
		res = stmt.executeUpdate();
		if(res > 0) {
			return true;
		}
		return false;
	}
	private String buildMissionString(int[] type, int[] amt) {
		String buildString = "";
		for(int i=0;i<type.length;i++) {
			buildString = buildString + type[i] + "," + amt[i];
			if(i < type.length - 1) {
				buildString = buildString + ";";
			}
		}
		return buildString;
	}
	private void sendMissionResult(User u, ArrayList<String> m, String r) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", true);
		resObj.putUtfStringArray("Missions", m);
		resObj.putUtfString("Reward", r);
		send("missions", resObj, u);
	}
	private void sendResultReward(User u, String r) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", true);
		resObj.putUtfString("Reward", r);
		send("missions", resObj, u);
	}
	private void sendError(User u, String e) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", false);
		resObj.putUtfString("Error", e);
		send("missions", resObj, u);
	}
}
