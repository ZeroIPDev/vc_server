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

public class profileicon extends BaseClientRequestHandler {
	@Override
	public void handleClientRequest(User u, ISFSObject p) {
		Connection connection;
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		//Build and execute query
		try {
			connection = dbManager.getConnection();
			String pIcons = getIcons(u, connection);
			if(pIcons.length() > 0) {
				sendResult(u, true, pIcons);
			} else {
				sendResult(u, false, "No icons owned!");
			}
			connection.close();
		} catch(SQLException e) {
			sendResult(u, false, e.getMessage());
		}
	}
	private String getIcons(User u, Connection connection) throws SQLException {
		PreparedStatement stmt = connection.prepareStatement("SELECT Icons FROM vc_accounts WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		stmt.setString(1, u.getName());
		ResultSet res = stmt.executeQuery();
		if(res.first() && res.getString("Icons") != null) {
			String pIcons = res.getString("Icons");
			return(pIcons);
		}
		return("");
	}
	public Boolean addIcon(User u, int n, Connection connection) throws SQLException {
		String pIcons = getIcons(u, connection);
		String iconValue = Integer.toString(n);
		if(n > -1 && !pIcons.contains(iconValue)) {
			if(pIcons.length() > 0) {
				pIcons = pIcons + "," + iconValue;
			} else {
				pIcons = iconValue;
			}
			PreparedStatement stmt = connection.prepareStatement("UPDATE vc_accounts SET Icons=? WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			stmt.setString(1, pIcons);
			stmt.setString(2, u.getName());
			int res = stmt.executeUpdate();
			if(res > 0) {
				return true;
			}
		}
		return(false);
	}
	private void sendResult(User u, Boolean s, String i) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", s);
		if(s) {
			resObj.putUtfString("Icons", i);
		} else {
			resObj.putUtfString("Error", i);
		}
		send("profileicon", resObj, u);
	}
}
