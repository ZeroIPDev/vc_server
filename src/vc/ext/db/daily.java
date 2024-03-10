package vc.ext.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class daily extends BaseClientRequestHandler {
	@Override
	public void handleClientRequest(User u, ISFSObject p) {
		Boolean isUpdate = p.getBool("Update");
		Connection connection;
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		try {
			connection = dbManager.getConnection();
			//Check dates
			if(getDailyDate(u, connection)) {
				//Link to functions
				if(isUpdate) {
					Boolean hasUpdated = updateDailyComplete(u, connection);
					if(hasUpdated) {
						getReward(u, connection);
					} else {
						sendError(u, "Failed to update database.");
					}
				} else {
					getDailyMissions(u, connection);
				}
			} else {
				sendError(u, "Too early for new missions!");
			}
			connection.close();
		}
		catch(SQLException | ParseException e) {
			trace("Error:", e);
			sendError(u, e.getMessage());
		}
	}
	private String getDateString(User u, Connection connection) throws SQLException {
		PreparedStatement stmt = connection.prepareStatement("SELECT Daily FROM vc_accounts WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		stmt.setString(1, u.getName());
		ResultSet res = stmt.executeQuery();
		if(res.first()) {
			return(res.getDate("Daily").toString());
		}
		return "";
		
	}
	private Boolean getDailyDate(User u, Connection connection) throws SQLException, ParseException {
		PreparedStatement stmt = connection.prepareStatement("SELECT Daily FROM vc_accounts WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		stmt.setString(1, u.getName());
		ResultSet res = stmt.executeQuery();
		if(res.first()) {
			//Compare against 24hr period
			if(clock.getHourDifference(res.getDate("Daily"), clock.dailyDate) >= 24) {
				return(true);
			}
		}
		return(false);
	}
	private void getDailyMissions(User u, Connection connection) throws SQLException {
		PreparedStatement stmt = connection.prepareStatement("SELECT Task1,Task2,Task3,Task4,Task5,Reward FROM vc_daily WHERE ID=1", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		ResultSet res = stmt.executeQuery();
		if(res.first()) {
			ArrayList<Integer> tasks = new ArrayList<Integer>();
			ArrayList<Integer> taskAmt = new ArrayList<Integer>();
			for(int i=1;i<6;i++) {
				String value = res.getString("Task" + i);
				int[] valueArray = Arrays.stream(value.split(",")).mapToInt(Integer::parseInt).toArray();
				tasks.add(valueArray[0]);
				taskAmt.add(valueArray[1]);
			}
			String reward = res.getString("Reward");
			String currentDate = getDateString(u, connection);
			sendResult(u, tasks, taskAmt, reward, currentDate);
		} else {
			sendError(u, "Failed to get daily missions.");
		}
	}
	private Boolean updateDailyComplete(User u, Connection connection) throws SQLException, ParseException {
		PreparedStatement stmt = connection.prepareStatement("UPDATE vc_accounts SET Daily=? WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		stmt.setString(1, clock.dateFormat.format(new Date()));
		stmt.setString(2, u.getName());
		int res = stmt.executeUpdate();
		if(res > 0) {
			return true;
		}
		return false;
	}
	private void getReward(User u, Connection connection) throws SQLException {
		PreparedStatement stmt = connection.prepareStatement("SELECT Reward FROM vc_daily WHERE ID=1", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		ResultSet res = stmt.executeQuery();
		if(res.first()) {
			String reward = res.getString("Reward");
			//Give account rewards
			Boolean success = true;
			int[] rewardArray = Arrays.stream(reward.split(",")).mapToInt(Integer::parseInt).toArray();
			if(rewardArray[0] == 3) {
				revive revive = new revive();
				success = revive.addRevive(u, Integer.toString(rewardArray[1]), connection);
			} else if(rewardArray[0] == 4) {
				xpboosts xpboost = new xpboosts();
				success = xpboost.addBoost(u, "1Hr", connection);
			}
			//Send message to player
			if(success) {
				sendResultReward(u, reward);
			} else {
				sendError(u, "Failed to add reward to account.");
			}
		} else {
			sendError(u, "Failed to get reward.");
		}
	}
	private void sendResult(User u, ArrayList<Integer> t, ArrayList<Integer> a, String r, String d) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", true);
		resObj.putIntArray("Tasks", t);
		resObj.putIntArray("TaskAmt", a);
		resObj.putUtfString("Reward", r);
		resObj.putUtfString("Date", d);
		send("daily", resObj, u);
	}
	private void sendResultReward(User u, String r) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", true);
		resObj.putUtfString("Reward", r);
		send("daily", resObj, u);
	}
	private void sendError(User u, String e) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", false);
		resObj.putUtfString("Error", e);
		send("daily", resObj, u);
	}
}
