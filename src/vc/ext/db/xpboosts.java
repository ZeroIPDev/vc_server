package vc.ext.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class xpboosts extends BaseClientRequestHandler {
	@Override
	public void handleClientRequest(User u, ISFSObject p) {
		//Retrieve variables
		Boolean isUpdate = p.getBool("Update");
		Connection connection;
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		try {
			connection = dbManager.getConnection();
			String boosts = getBoosts(u, connection);
			//Link to functions
			if(isUpdate) {
				int[] boostArray = Arrays.stream(boosts.split(",")).mapToInt(Integer::parseInt).toArray();
				int typeUse = p.getByte("BoostType");
				if(boostArray[typeUse] - 1 >= 0) {
					boostArray[typeUse]--;
					boosts = boostArray[0] + "," + boostArray[1] + "," + boostArray[2] + "," + boostArray[3] + "," + boostArray[4];
					useBoost(u, connection, boosts);
				} else {
					sendResult(u, false, boosts);
				}
			} else {
				if(boosts.length() > 0) {
					sendResult(u, true, boosts);
				} else {
					sendResult(u, false, boosts);
				}
			}
			connection.close();
		} catch(SQLException e) {
			trace("SQL Error: ", e);
		}
	}
	public String getBoosts(User u, Connection connection) throws SQLException {
		PreparedStatement stmt = connection.prepareStatement("SELECT Boosts FROM vc_accounts WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		stmt.setString(1, u.getName());
		ResultSet res = stmt.executeQuery();
		if(res.first()) {
			String boostArray = res.getString("Boosts");
			return(boostArray);
		}
		return("");
	}
	private void useBoost(User u, Connection connection, String boosts) throws SQLException {
		PreparedStatement stmt = connection.prepareStatement("UPDATE vc_accounts SET Boosts=? WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		stmt.setString(1, boosts);
		stmt.setString(2, u.getName());
		int res = stmt.executeUpdate();
		if(res > 0) {
			sendResult(u, true, boosts);
		} else {
			sendResult(u, false, boosts);
		}
	}
	public Boolean addBoost(User u, String name, Connection connection) throws SQLException {
		String currentBoosts = getBoosts(u, connection);
		if(currentBoosts.length() > 0) {
			int[] boostArray = Arrays.stream(currentBoosts.split(",")).mapToInt(Integer::parseInt).toArray();
			if(name.contains("1Hr")) {
				boostArray[0]++;
			} else if(name.contains("12Hr")) {
				boostArray[1]++;
			} else if(name.contains("24Hr")) {
				boostArray[2]++;
			} else if(name.contains("7dy")) {
				boostArray[3]++;
			} else if(name.contains("30dy")) {
				boostArray[4]++;
			}
			currentBoosts = boostArray[0] + "," + boostArray[1] + "," + boostArray[2] + "," + boostArray[3] + "," + boostArray[4];
			PreparedStatement stmt = connection.prepareStatement("UPDATE vc_accounts SET Boosts=? WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			stmt.setString(1, currentBoosts);
			stmt.setString(2, u.getName());
			int res = stmt.executeUpdate();
			if(res > 0) {
				return(true);
			}
		}
		return(false);
	}
	public String getXPBoostName(int t) {
		if(t == 0) {
			return("1HR");
		} else if(t == 1) {
			return("12HR");
		} else if(t == 2) {
			return("24HR");
		} else if(t == 3) {
			return("7DAY");
		} else if(t == 4) {
			return("30DAY");
		}
		return "";
	}
	private void sendResult(User u, Boolean s, String b) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", s);
		resObj.putUtfString("Boosts", b);
		send("xpboosts", resObj, u);
	}
}
