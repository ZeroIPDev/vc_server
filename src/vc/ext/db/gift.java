package vc.ext.db;

import java.sql.Connection;
import java.sql.SQLException;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class gift extends BaseClientRequestHandler {
	@Override
	public void handleClientRequest(User u, ISFSObject p) {
		String eName = p.getUtfString("Name");
		Connection connection;
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		try {
			connection = dbManager.getConnection();
			event event = new event();
			if(event.checkEvent(u, connection, eName)) {
				sendResult(u);
			} else {
				sendError(u, "Event already completed.");
			}
			connection.close();
		}
		catch(SQLException e) {
			trace("SQL Error: ", e);
			sendError(u, e.getMessage());
		}
	}
	private void sendResult(User u) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", true);
		send("gift", resObj, u);
	}
	private void sendError(User u, String e) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", false);
		resObj.putUtfString("Error", e);
		send("gift", resObj, u);
	}
}
