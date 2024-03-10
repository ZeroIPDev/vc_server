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

public class revive extends BaseClientRequestHandler {
	@Override
	public void handleClientRequest(User u, ISFSObject p) {
		//Retrieve variables from user
		Boolean isUpdate = p.getBool("Update");
		//Link to functions
		if(isUpdate) { //Use revive
			useRevive(u);
		} else { //Get total
			Connection connection;
			IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
			try {
				connection = dbManager.getConnection();
				String reviveString = getTotal(u, connection);
				if(reviveString != "ERROR") { //Send total to client
					int revives = Integer.parseInt(reviveString);
					sendResult(u, true, revives);
				} else {
					sendResult(u, false, 0);
				}
			}
			catch(SQLException e) {
				trace("SQL Error: ", e);
				sendResult(u, false, 0);
			}
			
		}
	}
	private void useRevive(User u) {
		Connection connection;
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		//Get current total
		try {
			connection = dbManager.getConnection();
			String reviveString = getTotal(u, connection);
			if(reviveString != "ERROR") {
				int revives = Integer.parseInt(reviveString);
				if(revives > 0) {
					revives -= 1;
					//Build and execute query
					PreparedStatement stmt = connection.prepareStatement("UPDATE vc_accounts SET Revives=? WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
					stmt.setInt(1, revives);
					stmt.setString(2, u.getName());
					int res = stmt.executeUpdate();
					if(res > 0) {
						sendResult(u, true, revives);
					} else {
						sendResult(u, false, 0);
					}
				} else {
					sendResult(u, false, 0);
				}
			} else {
				sendResult(u, false, 0);
			}
			connection.close();
		}
		catch(SQLException e) {
			trace("SQL Error: ", e);
			sendResult(u, false, 0);
		}
	}
	public Boolean addRevive(User u, String name, Connection connection) throws SQLException {
		String reviveString = getTotal(u, connection);
		if(reviveString != "ERROR") {
			int reviveCount = Integer.parseInt(reviveString);
			if(name.contains("1")) {
				reviveCount++;
			} else if(name.contains("3")) {
				reviveCount += 3;
			} else if(name.contains("5")) {
				reviveCount += 5;
			} else if(name.contains("10")) {
				reviveCount += 10;
			}
			PreparedStatement stmt = connection.prepareStatement("UPDATE vc_accounts SET Revives=? WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			stmt.setInt(1, reviveCount);
			stmt.setString(2, u.getName());
			int res = stmt.executeUpdate();
			if(res > 0) {
				return(true);
			}
		}
		return(false);
	}
	public String getTotal(User u, Connection connection) {
		//Build and execute query
		try {
			PreparedStatement stmt = connection.prepareStatement("SELECT Revives FROM vc_accounts WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			stmt.setString(1, u.getName());
			ResultSet res = stmt.executeQuery();
			if(res.first()) {
				String reviveString = res.getString("Revives");
				return(reviveString);
			} else {
				return("ERROR");
			}
		}
		catch (SQLException e) {
			trace("SQL Error: ", e);
			return("ERROR");
		}
	}
	private void sendResult(User u, Boolean s, int t) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", s);
		resObj.putInt("Revives", t);
		send("revive", resObj, u);
	}
}
