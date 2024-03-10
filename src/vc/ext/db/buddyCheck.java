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

public class buddyCheck extends BaseClientRequestHandler {

	@Override
	public void handleClientRequest(User u, ISFSObject p) {
		Connection connection;
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		ArrayList<String> invites = new ArrayList<String>();
		try {
			//Get user invites
			connection = dbManager.getConnection();
			PreparedStatement stmt = connection.prepareStatement("SELECT Sender FROM vc_requests WHERE User=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			stmt.setString(1, u.getName());
			ResultSet res = stmt.executeQuery();
			if(res.isBeforeFirst()) {
				while(res.next()) {
					invites.add(res.getString("Sender"));
				}
			}
			sendResult(u, invites);
			//Cleanup invites
			if(invites.size() > 0) {
				stmt = connection.prepareStatement("DELETE FROM vc_requests WHERE User=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
				stmt.setString(1, u.getName());
				int ures = stmt.executeUpdate();
				if(ures <= 0) {
					trace("Failed to clean DB invites for", u.getName());
				}
			}
			connection.close();
		}
		catch(SQLException e) {
			trace("SQL Error:", e);
			sendError(u, e.getMessage());
		}
	}
	private void sendResult(User u, ArrayList<String> i) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", true);
		resObj.putUtfStringArray("Invites", i);
		send("buddycheck", resObj, u);
	}
	private void sendError(User u, String e) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", false);
		resObj.putUtfString("Error", e);
		send("buddycheck", resObj, u);
	}

}
