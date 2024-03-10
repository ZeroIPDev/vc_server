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

public class cbox extends BaseClientRequestHandler {
	public final int MAX_BOX = 4; //Max number of purchasable boxes
	@Override
	public void handleClientRequest(User u, ISFSObject p) {
		Connection connection;
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		try {
			connection = dbManager.getConnection();
			int boxNum = getBoxNumber(u, connection);
			if(boxNum > -1) {
				sendResult(u, boxNum);
			} else {
				sendError(u, "Failed to get Boxes from account.");
			}
			connection.close();
		}
		catch(SQLException e) {
			trace("SQL Error: ", e);
			sendError(u, e.getMessage());
		}
	}
	public int getBoxNumber(User u, Connection connection) throws SQLException {
		PreparedStatement stmt = connection.prepareStatement("SELECT Boxes FROM vc_accounts WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		stmt.setString(1, u.getName());
		ResultSet res = stmt.executeQuery();
		if(res.first()) {
			return(res.getInt("Boxes"));
		}
		return -1;
	}
	public boolean updateBoxNumber(User u, Connection connection) throws SQLException {
		int boxNum = getBoxNumber(u, connection);
		if(boxNum > -1 && boxNum < MAX_BOX) {
			boxNum++;
			PreparedStatement stmt = connection.prepareStatement("UPDATE vc_accounts SET Boxes=? WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			stmt.setInt(1, boxNum);
			stmt.setString(2, u.getName());
			int res = stmt.executeUpdate();
			if(res > 0) {
				return true;
			}
		}
		return false;
	}
	private void sendResult(User u, int boxNum) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", true);
		resObj.putInt("Box", boxNum);
		send("cbox", resObj, u);
	}
	private void sendError(User u, String e) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", false);
		resObj.putUtfString("Error", e);
		send("cbox", resObj, u);
	}
}