package vc.ext.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class boosts extends BaseClientRequestHandler {
	@Override
	public void handleClientRequest(User u, ISFSObject p) {
		Connection connection;
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		try {
			connection = dbManager.getConnection();
			String eName = p.getUtfString("Event");
			String boostData = getBoostReward(eName, connection);
			if(boostData.length() > 0) {
				sendResult(u, boostData);
			} else {
				sendError(u, "Failed to get boost data.");
			}
			connection.close();
		}
		catch(SQLException e) {
			trace("SQL Error:", e);
			sendError(u, e.getMessage());
		}
	}
	public String getBoostReward(String n, Connection connection) throws SQLException {
		PreparedStatement stmt = connection.prepareStatement("SELECT Reward FROM vc_events WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		stmt.setString(1, n);
		ResultSet res = stmt.executeQuery();
		if(res.first()) {
			return(res.getString("Reward"));
		}
		return "";
	}
	private void sendResult(User u, String d) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", true);
		resObj.putUtfString("Data", d);
		send("boosts", resObj, u);
	}
	private void sendError(User u, String e) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", false);
		resObj.putUtfString("Error", e);
		send("boosts", resObj, u);
	}
}
