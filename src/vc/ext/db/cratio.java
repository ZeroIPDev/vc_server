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

public class cratio extends BaseClientRequestHandler {
	@Override
	public void handleClientRequest(User u, ISFSObject p) {
		//Retrieve variables from user
		Boolean isUpdate = p.getBool("Update");
		//Link to functions
		if(isUpdate) {
			Boolean isWin = p.getBool("Win");
			updateRatio(isWin, u);
		} else {
			String ratio = getRatio(u);
			sendResult(u, ratio);
		}
	}
	private void updateRatio(Boolean w, User u) {
		Connection connection;
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		//Get current ratio
		String ratio = getRatio(u);
		int[] values = Arrays.stream(ratio.split(",")).mapToInt(Integer::parseInt).toArray();
		//Update ratio
		if(w) {
			values[0] += 1;
		} else {
			values[1] += 1;
		}
		//Merge to string
		ratio = values[0] + "," + values[1];
		//Build and execute query
		try {
			connection = dbManager.getConnection();
			PreparedStatement stmt = connection.prepareStatement("UPDATE vc_accounts SET Casual=? WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			stmt.setString(1, ratio);
			stmt.setString(2, u.getName());
			int res = stmt.executeUpdate();
			if(res > 0) {
				sendResult(u, ratio);
			} else {
				sendResult(u, "");
			}
			connection.close();
		}
		catch(SQLException e) {
			trace("SQL Error: ", e);
		}
	}
	private String getRatio(User u) {
		Connection connection;
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		//Build and execute query
		try {
			connection = dbManager.getConnection();
			PreparedStatement stmt = connection.prepareStatement("SELECT Casual FROM vc_accounts WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			stmt.setString(1, u.getName());
			ResultSet res = stmt.executeQuery();
			if(res.first()) {
				String ratio = res.getString("Casual");
				connection.close();
				return(ratio);
			} else {
				connection.close();
				return("");
			}
		}
		catch(SQLException e) {
			trace("SQL Error: ", e);
			return("");
		}
	}
	private void sendResult(User u, String s) {
		ISFSObject resObj = new SFSObject();
		resObj.putUtfString("Ratio", s);
		send("cratio", resObj, u); //Send result to client
	}
}
