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

public class DeleteAccount extends BaseClientRequestHandler {
	@Override
	public void handleClientRequest(User u, ISFSObject p) {
		Connection connection;
        IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
        try {
        	connection = dbManager.getConnection();
        	PreparedStatement stmt = connection.prepareStatement("DELETE FROM vc_accounts WHERE BINARY Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        	stmt.setString(1, u.getName());
        	int res = stmt.executeUpdate();
        	if(res > 0) {
        		sendResult(u);
        	} else {
        		sendError(u, "Failed to delete user account");
        	}
        	connection.close();
        }
        catch(SQLException e) {
        	sendError(u, e.getMessage());
        	trace("SQL Error:", e);
        }
	}
	private void sendResult(User u) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", true);
		send("deleteaccount", resObj, u);
	}
	private void sendError(User u, String e) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", false);
		resObj.putUtfString("Error", e);
		send("deleteaccount", resObj, u);
	}
}
