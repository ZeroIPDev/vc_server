package vc.ext.account;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.util.IWordFilter;

public class creation extends BaseClientRequestHandler {
	
	String[] alphabetChar = new String[] {"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
	String[] specialChar = new String[] {"!", ":", "+", "-"};
	
	@Override
	public void handleClientRequest(User u, ISFSObject p) {
		//Retrieve variables from user
		String uName = p.getUtfString("Name");
        String uPlatform = p.getUtfString("Platform");
        String uID = p.getUtfString("ID");
        
        //Variables
        Connection connection;
        IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
        IWordFilter filter = getParentExtension().getParentZone().getWordFilter();
        
        //Apply filter to username
        uName = filter.apply(uName).getMessage();
        
        //Build and execute queries
        try {
        	connection = dbManager.getConnection();
        	PreparedStatement stmt;
        	//Check if name is used
        	stmt = connection.prepareStatement("SELECT ID FROM vc_accounts WHERE BINARY Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        	stmt.setString(1, uName);
        	ResultSet res = stmt.executeQuery();
        	if(!res.first()) { //Name is free
        		String uPass = generateUserPassword(); //Create password
        		PreparedStatement ustmt;
        		if(uPlatform.contentEquals("Steam") && account.enable_steam_ids) {
        			ustmt = connection.prepareStatement("INSERT INTO vc_accounts (Name, Pass, Steam) VALUES (?, ?, ?)", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        			ustmt.setString(3, uID);
        		} else {
        			ustmt = connection.prepareStatement("INSERT INTO vc_accounts (Name, Pass) VALUES (?, ?)", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        		}
        		ustmt.setString(1, uName);
    			ustmt.setString(2, uPass);
        		int ures = ustmt.executeUpdate();
        		if(ures > 0) {
        			search Search = new search();
        			int ID = Search.getIDFromLogin(uName, uPass, connection);
        			if(ID > -1) {
        				sendResult(u, ID, uName, uPass);
        			} else {
        				sendError(u, "Failed to pull account info after creation");
        			}
        		} else {
        			sendError(u, "Failed to create account");
        		}
        	} else { //Name is taken
        		sendError(u, "Name is already taken");
        	}
        	connection.close();
        }
        catch(SQLException e) {
        	sendError(u, e.getMessage());
        	trace("SQL Error: ", e);
        }
	}
	private String generateUserPassword() {
		Random rand = new Random();
		int cKey;
		int pos;
		ArrayList<String> cArray = new ArrayList<String>();
		//Generate 4-5 letters
		cKey = 4;
		cKey += rand.nextInt(2);
		for(int i=0;i<cKey;i++) {
			cArray.add(alphabetChar[rand.nextInt(alphabetChar.length)]);
			if(rand.nextBoolean()) { //50% chance for lowercase
				cArray.set(i, cArray.get(i).toLowerCase());
			}
		}
		//Generate 2-3 numbers
		cKey = 2;
		cKey += rand.nextInt(2);
		for(int i=0;i<cKey;i++) {
			pos = rand.nextInt(cArray.size());
			cArray.add(pos, String.valueOf(rand.nextInt(10)));
		}
		//Generate 1 special character
		pos = rand.nextInt(cArray.size());
		cArray.add(pos, specialChar[rand.nextInt(specialChar.length)]);
		//Merge into random password
		String cPass = "";
		for(int i=0;i<cArray.size();i++) {
			cPass += cArray.get(i);
		}
		return cPass;
	}
	private void sendResult(User u, int i, String n, String p) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", true);
		resObj.putInt("ID", i);
		resObj.putUtfString("Name", n);
		resObj.putUtfString("Pass", p);
		send("creation", resObj, u); //Send result to client
	}
	private void sendError(User u, String e) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", false);
		resObj.putUtfString("Error", e);
		send("creation", resObj, u);
	}
}