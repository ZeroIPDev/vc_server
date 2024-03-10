package vc.ext.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.smartfoxserver.v2.api.ISFSApi;
import com.smartfoxserver.v2.buddylist.Buddy;
import com.smartfoxserver.v2.buddylist.BuddyList;
import com.smartfoxserver.v2.buddylist.BuddyListManager;
import com.smartfoxserver.v2.buddylist.SFSBuddyEventParam;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.entities.match.MatchExpression;
import com.smartfoxserver.v2.entities.match.StringMatch;
import com.smartfoxserver.v2.entities.match.UserProperties;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

public class buddyAdd extends BaseServerEventHandler {
	@Override
	public void handleServerEvent(ISFSEvent e) throws SFSException {
		//Get buddy data from request
		Buddy bData = (Buddy) e.getParameter(SFSBuddyEventParam.BUDDY);
		User u = (User) e.getParameter(SFSEventParam.USER);
		
		//Search users for matching name
		Connection connection;
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		try {
			connection = dbManager.getConnection();
			PreparedStatement stmt = connection.prepareStatement("SELECT ID FROM vc_accounts WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			stmt.setString(1, bData.getName());
			ResultSet res = stmt.executeQuery();
			if(res.first()) {
				//Attempt to parse user data
				MatchExpression exp = new MatchExpression(UserProperties.NAME, StringMatch.EQUALS, bData.getName());
				ISFSApi sfs = getParentExtension().getApi();
				List<User> users = sfs.findUsers(getParentExtension().getParentZone().getUserList(), exp, 1);
				if(users.size() > 0) { //User is online and can be sent invite
					User r = users.get(0);
					if(!checkIfAdded(u, r)) {
						sendBuddyInvite(u, r);
					}
				} else { //User is offline, add to backlog
					stmt = connection.prepareStatement("INSERT INTO vc_requests (Sender,User) VALUES (?,?)", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
					stmt.setString(1, u.getName());
					stmt.setString(2, bData.getName());
					int resAdd = stmt.executeUpdate();
					if(resAdd <= 0) {
						sendBuddyError(u, "Failed to add user.");
					}
				}
			} else {
				sendBuddyError(u, "User does not exist.");
			}
			connection.close();
		}
		catch(SQLException error) {
			trace("SQL Error:", error);
			sendBuddyError(u, error.getMessage());
		}
	}
	private boolean checkIfAdded(User u, User r) {
		BuddyListManager manager = getParentExtension().getParentZone().getBuddyListManager();
		BuddyList uList = manager.getBuddyList(r.getName());
		if(uList.getBuddy(u.getName()) != null) {
			return true;
		}
		return false;
	}
	private void sendBuddyInvite(User u, User r) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", true);
		resObj.putUtfString("Invite", u.getName() + " added you as a friend!");
		resObj.putUtfString("Name", u.getName());
		send("BuddyAdd", resObj, r);
	}
	private void sendBuddyError(User u, String e) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", false);
		resObj.putUtfString("Error", e);
		send("BuddyAdd", resObj, u);
	}
}
