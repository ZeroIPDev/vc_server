package vc.ext.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class event extends BaseClientRequestHandler {
	@Override
	public void handleClientRequest(User u, ISFSObject p) {
		Boolean isUpdate = p.getBool("Update");
		Connection connection;
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		try {
			connection = dbManager.getConnection();
			if(isUpdate) {
				String eName = p.getUtfString("Event");
				if(checkEvent(u, connection, eName)) {
					updateEvent(u, connection, eName);
				} else {
					sendError(u, "Event already completed.");
				}
			} else {
				getEvents(u, connection);
			}
			connection.close();
		}
		catch(SQLException e) {
			trace("Error:", e);
			sendError(u, e.getMessage());
		}
		
	}
	private String[] getPlayerEvents(User u, Connection connection) throws SQLException {
		PreparedStatement stmt = connection.prepareStatement("SELECT Event FROM vc_accounts WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		stmt.setString(1, u.getName());
		ResultSet res = stmt.executeQuery();
		if(res.first()) {
			if(res.getString("Event") != null) {
				String eventString = res.getString("Event");
				if(eventString.contains(",")) {
					return(eventString.split(","));
				} else {
					String[] e = {eventString};
					return(e);
				}
			}
		}
		return new String[0];
	}
	public Boolean checkEvent(User u, Connection connection, String eName) throws SQLException {
		String[] compEvents = getPlayerEvents(u, connection);
		for(int i=0;i<compEvents.length;i++) {
			if(compEvents[i].contentEquals(eName)) {
				return false;
			}
		}
		return true;
	}
	private void getEvents(User u, Connection connection) throws SQLException {
		ArrayList<String> eNames = new ArrayList<String>();
		ArrayList<String> eDesc = new ArrayList<String>();
		ArrayList<String> eDates = new ArrayList<String>();
		ArrayList<String> eData = new ArrayList<String>();
		PreparedStatement stmt = connection.prepareStatement("SELECT Name,Description,Data,End FROM vc_events WHERE Start <= DATE_ADD(NOW(), INTERVAL 1 HOUR)", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		ResultSet res = stmt.executeQuery();
		if(res.isBeforeFirst()) {
			while(res.next()) {
				eNames.add(0, res.getString("Name"));
				eDesc.add(0, res.getString("Description"));
				eData.add(0, res.getString("Data"));
				eDates.add(0, res.getDate("End").toString());
			}
			sendResultEvent(u, eData, eNames, eDesc, eDates);
		} else {
			sendError(u, "No events to display.");
		}
	}
	private void updateEvent(User u, Connection connection, String eName) throws SQLException {
		String[] compEvents = getPlayerEvents(u, connection);
		String eData = "";
		for(int i=0;i<compEvents.length;i++) {
			eData = eData + compEvents[i] + ",";
		}
		eData = eData + eName;
		PreparedStatement stmt = connection.prepareStatement("UPDATE vc_accounts SET Event=? WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		stmt.setString(1, eData);
		stmt.setString(2, u.getName());
		int res = stmt.executeUpdate();
		if(res > 0) {
			String rewardString = giveReward(u, connection, eName);
			if(rewardString.length() > 0) {
				sendResultReward(u, rewardString); //Send result to client
			} else {
				sendError(u, "Failed to add reward to account.");
			}
		} else {
			sendError(u, "Failed to update database.");
		}
	}
	public String giveReward(User u, Connection connection, String eName) throws SQLException {
		PreparedStatement stmt = connection.prepareStatement("SELECT Reward FROM vc_events WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		stmt.setString(1, eName);
		ResultSet res = stmt.executeQuery();
		if(res.first()) {
			Boolean success = true;
			//Get reward
			String rewardString = res.getString("Reward");
			Reward rewardObj = new Reward();
			rewardObj.parseFormattedString(rewardString);
			//Give server rewards
			success = giveRewardData(u, connection, rewardObj);
			if(success) {
				return(rewardString);
			}
		}
		return("");
	}
	public boolean giveRewardData(User u, Connection connection, Reward r) throws SQLException {
		boolean success = true;
		if(r.reward == 3) {
			revive revive = new revive();
			success = revive.addRevive(u, String.valueOf(r.rewardValue), connection);
		} else if(r.reward == 4) {
			xpboosts xpboost = new xpboosts();
			success = xpboost.addBoost(u, xpboost.getXPBoostName(r.rewardValue), connection);
		} else if(r.reward == 5) {
			profileicon profileicon = new profileicon();
			success = profileicon.addIcon(u, r.rewardValue, connection);
		}
		return success;
	}
	public String getShopData(String taskHandle, int func) {
		if(taskHandle.contentEquals("LaunchEvent")) {
			if(func == 0) { //Get
				return("SELECT ID FROM vc_store WHERE Internal='EVENT_Party' OR Internal='EVENT_Ukiyoe'");
			} else if(func == 1) { //Insert
				return("INSERT INTO vc_store (Internal,Type,SubType,Price) VALUES ('EVENT_Party',5,14,50),('EVENT_Ukiyoe',4,1,100)");
			} else { //Delete
				return("DELETE FROM vc_store WHERE Internal='EVENT_Party' OR Internal='EVENT_Ukiyoe'");
			}
		}
		return "";
	}
	private void sendResultEvent(User u, ArrayList<String> t, ArrayList<String> n, ArrayList<String> d, ArrayList<String> date) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", true);
		resObj.putUtfStringArray("Names", n);
		resObj.putUtfStringArray("Desc", d);
		resObj.putUtfStringArray("Data", t);
		resObj.putUtfStringArray("Dates", date);
		send("event", resObj, u);
	}
	private void sendResultReward(User u, String r) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", true);
		resObj.putUtfString("Reward", r);
		send("event", resObj, u);
	}
	private void sendError(User u, String e) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", false);
		resObj.putUtfString("Error", e);
		send("event", resObj, u);
	}
}
