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

public class coopdiff extends BaseClientRequestHandler {
	@Override
	public void handleClientRequest(User u, ISFSObject p) {
		Boolean isUpdate = p.getBool("Update");
		Connection connection;
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		try {
			connection = dbManager.getConnection();
			if(isUpdate) {
				int coopDiff = increaseCoopDiff(u, connection);
				if(coopDiff > -1) {
					sendResult(u, coopDiff);
				} else {
					sendError(u, "Failed to update max difficulty.");
				}
			} else {
				int coopDiff = getCoopDiff(u, connection);
				if(coopDiff > -1) {
					sendResult(u, coopDiff);
				} else {
					sendError(u, "Failed to get max difficulty.");
				}
			}
			connection.close();
		}
		catch(SQLException e) {
			trace("SQL Error: ", e);
			sendError(u, e.getMessage());
		}
	}
	public int getCoopDiff(User u, Connection connection) throws SQLException {
		PreparedStatement stmt = connection.prepareStatement("SELECT CoopDiff FROM vc_accounts WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		stmt.setString(1, u.getName());
		ResultSet res = stmt.executeQuery();
		if(res.first()) {
			return(res.getInt("CoopDiff"));
		}
		return -1;
	}
	private int increaseCoopDiff(User u, Connection connection) throws SQLException {
		int coopDiff = getCoopDiff(u, connection);
		if(coopDiff < 6) {
			coopDiff++;
		}
		PreparedStatement stmt = connection.prepareStatement("UPDATE vc_accounts SET CoopDiff=? WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		stmt.setInt(1, coopDiff);
		stmt.setString(2, u.getName());
		int res = stmt.executeUpdate();
		if(res > 0) {
			return coopDiff;
		}
		return -1;
	}
	private void sendResult(User u, int i) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", true);
		resObj.putInt("CoopDiff", i);
		send("coopdiff", resObj, u);
	}
	private void sendError(User u, String e) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", false);
		resObj.putUtfString("Error", e);
		send("coopdiff", resObj, u);
	}
}
