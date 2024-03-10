package vc.ext.db;

import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class HostKick extends BaseClientRequestHandler {
	@Override
	public void handleClientRequest(User u, ISFSObject p) {
		Room userRoom = u.getLastJoinedRoom();
		String hostName = userRoom.getVariable("gameHost").getStringValue();
		if(u.getName().contentEquals(hostName)) {
			User kickUser = userRoom.getUserByName(p.getUtfString("KickRequest"));
			if(kickUser != null) {
				getParentExtension().getApi().leaveRoom(kickUser, userRoom);
				sendKickedMessage(kickUser);
			} else {
				sendError(u, "User not found in room.");
			}
		} else {
			sendError(u, "You are not the room host.");
		}
	}
	private void sendKickedMessage(User k) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", true);
		send("hostkick", resObj, k);
	}
	private void sendError(User u, String e) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", false);
		resObj.putUtfString("Error", e);
		send("hostkick", resObj, u);
	}
}
