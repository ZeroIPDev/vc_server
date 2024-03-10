package vc.ext.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.extensions.SFSExtension;

public class db extends SFSExtension {
	ScheduledFuture<?> taskHandleDaily;
	ScheduledFuture<?> taskHandleEvent;
	ScheduledFuture<?> taskHandleToken;
	SmartFoxServer sfs = SmartFoxServer.getInstance();
	
	@Override
	public void init() {
		//Event Handlers
		addEventHandler(SFSEventType.USER_LOGIN, loginevent.class);
		addEventHandler(SFSEventType.BUDDY_ADD, buddyAdd.class);
		//Requests
		addRequestHandler("cratio", cratio.class);
		addRequestHandler("revive", revive.class);
		addRequestHandler("xpboosts", xpboosts.class);
		addRequestHandler("profileicon", profileicon.class);
		addRequestHandler("daily", daily.class);
		addRequestHandler("event", event.class);
		addRequestHandler("missions", missions.class);
		addRequestHandler("cbox", cbox.class);
		addRequestHandler("gift", gift.class);
		addRequestHandler("playerlevel", playerlevel.class);
		addRequestHandler("coopdiff", coopdiff.class);
		addRequestHandler("hostkick", HostKick.class);
		addRequestHandler("boosts", boosts.class);
		addRequestHandler("buddycheck", buddyCheck.class);
		addRequestHandler("deleteaccount", DeleteAccount.class);
		//Start timed threads
		try {
			clock.dailyDate = clock.getDate();
			clock.campaignDate = clock.getDate();
		} catch (ParseException e) {
			trace("CLOCK INIT ERROR:", e);
		}
		taskHandleEvent = sfs.getTaskScheduler().scheduleAtFixedRate(new EventRunner(), 0, 1, TimeUnit.HOURS);
	}
	@Override
	public void destroy() {
		if(taskHandleDaily != null) {
			taskHandleDaily.cancel(true);
		}
		if(taskHandleEvent != null) {
			taskHandleEvent.cancel(true);
		}
		if(taskHandleToken != null) {
			taskHandleToken.cancel(true);
		}
		super.destroy();
	}
	//Scheduled tasks
	private class EventRunner implements Runnable {
		@Override
		public void run() {
			Connection connection;
			IDBManager dbManager = getParentZone().getDBManager();
			try {
				connection = dbManager.getConnection();
				//Daily Missions
				if(clock.getDayDifference(clock.dailyDate, clock.getDate())) {
					Boolean isSetDaily = updateDailyMissions(connection);
					if(isSetDaily) {
						clock.dailyDate = clock.getDate();
						trace("Daily missions set!");
					} else {
						trace("ERROR: Failed to update daily missions!");
					}
				}
				//Campaigns
				if(clock.getMonthDifference(clock.campaignDate, clock.getDate())) {
					missions mission = new missions();
					Boolean isSetCampaign = mission.generateCampaign(connection);
					if(isSetCampaign) {
						clock.campaignDate = clock.getDate();
						trace("Monthly campaign set!");
					} else {
						trace("ERROR: Failed to set monthly campaign!");
					}
				}
				//Events
				Boolean isSet = updateEvents(connection);
				if(isSet) {
					trace("Events set!");
				} else {
					trace("No events to set.");
				}
				Boolean isRemove = removeEvents(connection);
				if(isRemove) {
					trace("Expired events removed!");
				} else {
					trace("No events to remove.");
				}
				
				connection.close();
			}
			catch (SQLException | ParseException e) {
				trace("ERROR:", e);
				taskHandleEvent.cancel(true);
			}
		}
	}
	private Boolean updateEvents(Connection connection) throws SQLException, ParseException {
		//Set variables
		ArrayList<String[]> eData = new ArrayList<String[]>();
		ArrayList<String[]> eReward = new ArrayList<String[]>();
		xpevent.xpEventType = -1;
		xpevent.xpEventBoost = 1.0;
		//Build and execute query
		PreparedStatement stmt = connection.prepareStatement("SELECT Data,Reward FROM vc_events WHERE Start <= DATE_ADD(NOW(), INTERVAL 1 HOUR)", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		ResultSet res = stmt.executeQuery();
		//Get data from server
		if(res.isBeforeFirst()) {
			while(res.next()) {
				eData.add(res.getString("Data").split(","));
				if(res.getString("Reward") != null) {
					eReward.add(res.getString("Reward").split(","));
				} else {
					eReward.add(new String[] {"Null"});
				}
			}
			//Print in console
			String traceData = "";
			for(int i=0;i<eData.size();i++) {
				traceData = traceData + "\n" + (eData.get(i)[0] + ":" + eData.get(i)[1]);
				if(!eReward.get(i)[0].contentEquals("Null")) {
					traceData = traceData + " REWARD " + (eReward.get(i)[0] + ":" + eReward.get(i)[1]);
				}
			}
			if(traceData.length() > 0) {
				trace("\nEVENTS:", traceData);
			}
		}
		//Run current event tasks
		event event = new event();
		Boolean isUpdated = false;
		for(int i=0;i<eData.size();i++) {
			if(Integer.parseInt(eData.get(i)[0]) == 1) { //Shop Event
				stmt = connection.prepareStatement(event.getShopData(eData.get(i)[1], 0), ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
				res = stmt.executeQuery();
				if(!res.first()) {
					stmt = connection.prepareStatement(event.getShopData(eData.get(i)[1], 1), ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
					int eres = stmt.executeUpdate();
					if(eres > 0) {
						isUpdated = true;
					}
				}
			}
			else if(Integer.parseInt(eData.get(i)[0]) == 2 && Integer.parseInt(eReward.get(i)[0]) == 3) { //Boost Event
				xpevent.xpEventType = Integer.parseInt(eData.get(i)[1]);
				xpevent.xpEventBoost = Double.parseDouble(eReward.get(i)[1]);
				isUpdated = true;
			}
		}
		if(isUpdated) {
			return true;
		}
		return false;
	}
	private Boolean removeEvents(Connection connection) throws SQLException, ParseException {
		//Variables
		ArrayList<String[]> eData = new ArrayList<String[]>();
		ArrayList<Integer> ePos = new ArrayList<Integer>();
		//Build and execute query
		PreparedStatement stmt = connection.prepareStatement("SELECT ID,Data FROM vc_events WHERE End <= DATE_SUB(NOW(), INTERVAL 1 HOUR)", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		ResultSet res = stmt.executeQuery();
		//Get data from server
		if(res.isBeforeFirst()) {
			while(res.next()) {
				ePos.add(res.getInt("ID"));
				eData.add(res.getString("Data").split(","));
			}
		}
		//Remove extra event data
		event event = new event();
		for(int i=0;i<eData.size();i++) {
			if(Integer.parseInt(eData.get(i)[0]) == 1) { //Shop event
				stmt = connection.prepareStatement(event.getShopData(eData.get(i)[1], 2), ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
				int eres = stmt.executeUpdate();
				if(eres <= 0) {
					trace("ERROR! Failed to remove items from shop.");
				}
			}
		}
		//Remove event listings
		if(ePos.size() > 0) {
			String sqlData = "DELETE FROM vc_events WHERE ID=" + ePos.get(0);
			if(ePos.size() > 1) {
				sqlData = "DELETE FROM vc_events WHERE ID=";
				for(int i=0;i<ePos.size();i++) {
					sqlData = sqlData + ePos.get(i) + " OR ID=";
				}
			}
			stmt = connection.prepareStatement(sqlData, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			int ures = stmt.executeUpdate();
			if(ures > 0) {
				return true;
			}
		}
		return false;
	}
	private Boolean updateDailyMissions(Connection connection) throws SQLException {
		//Set variables
		Random rand = new Random();
		int[] taskTypes = new int[5];
		int[] taskAmt = new int[5];
		int[] reward = new int[2];
		//Create list of possible missions
		ArrayList<Integer> missions = new ArrayList<Integer>();
		for(int i=0;i<6;i++) {
			missions.add(i);
		}
		//Generate daily missions
		for(int i=0;i<5;i++) {
			int index = rand.nextInt(missions.size());
			int multiplier = 1;
			taskTypes[i] = missions.get(index);
			missions.remove(index);
			if(taskTypes[i] == 5) {
				multiplier = 1000;
			}
			if(i <= 1) {
				taskAmt[i] = 1 * multiplier;
			} else if(i <= 3) {
				taskAmt[i] = (1 + rand.nextInt(2)) * multiplier;
			} else {
				taskAmt[i] = 3 * multiplier;
			}
		}
		//Generate reward
		reward[0] = rand.nextInt(5);
		if(reward[0] == 0) { //Money
			reward[1] = 20000 * (1 + rand.nextInt(2));
		} else if(reward[0] == 1) { //Chest (Gold)
			reward[1] = 3 + rand.nextInt(2);
		} else if(reward[0] == 2) { //Synths
			reward[1] = 100 * (1 + rand.nextInt(2));
		} else if(reward[0] == 3) { //Revive
			reward[1] = 1;
		} else if(reward[0] == 4) { //XP Boost (1HR)
			reward[1] = 0;
		}
		//Print in console
		String traceData = "";
		for(int i=0;i<taskTypes.length;i++) {
			traceData = traceData + "\n" + (taskTypes[i] + ":" + taskAmt[i]);
		}
		trace("\nDAILY MISSIONS:", traceData, "\nREWARD:\n" + reward[0] + ":" + reward[1]);
		//Update tasks on database
		PreparedStatement stmt = connection.prepareStatement("UPDATE vc_daily SET Task1=?,Task2=?,Task3=?,Task4=?,Task5=?,Reward=? WHERE ID=1", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		stmt.setString(1, taskTypes[0] + "," + taskAmt[0]);
		stmt.setString(2, taskTypes[1] + "," + taskAmt[1]);
		stmt.setString(3, taskTypes[2] + "," + taskAmt[2]);
		stmt.setString(4, taskTypes[3] + "," + taskAmt[3]);
		stmt.setString(5, taskTypes[4] + "," + taskAmt[4]);
		stmt.setString(6, reward[0] + "," + reward[1]);
		int res = stmt.executeUpdate();
		if(res > 0) {
			return(true);
		}
		return(false);
	}
}