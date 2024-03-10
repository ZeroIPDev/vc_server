package vc.ext.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Random;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class playerlevel extends BaseClientRequestHandler {
	@Override
	public void handleClientRequest(User u, ISFSObject p) {
		Boolean isUpdate = p.getBool("Update");
		Connection connection;
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		try {
			connection = dbManager.getConnection();
			String levelString = getLevelData(u, connection);
			if(levelString.length() > 0) {
				if(isUpdate) {
					//Get XP amount
					int mType = p.getByte("Type");
					int mDiff = p.getByte("Diff");
					boolean isWin = p.getBool("Win");
					int increaseAmt = getLevelIncrease(mType, mDiff, isWin);
					if(increaseAmt > -1) {
						//Do XP
						int[] levelArray = Arrays.stream(levelString.split(",")).mapToInt(Integer::parseInt).toArray();
						doXP(levelArray, increaseAmt, u, connection);
					} else {
						sendError(u, "Failed to get XP amount.");
					}
				} else {
					sendResult(u, levelString);
				}
			} else {
				sendError(u, "Failed to get player level.");
			}
			connection.close();
		}
		catch(SQLException e) {
			trace("SQL Error: ", e);
			sendError(u, e.getMessage());
		}
	}
	public String getLevelData(User u, Connection connection) throws SQLException {
		PreparedStatement stmt = connection.prepareStatement("SELECT PlayerLevel FROM vc_accounts WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		stmt.setString(1, u.getName());
		ResultSet res = stmt.executeQuery();
		if(res.first()) {
			String levelString = res.getString("PlayerLevel");
			return levelString;
		}
		return "";
	}
	public void doXP(int[] pl, int xp, User u, Connection connection) throws SQLException {
		Random rand = new Random();
		String rewardString = "";
		for(int i=0;i<xp;i++) {
			pl[1]++;
			if(pl[1] >= pl[2] && pl[0] < 999) { //Level up
				pl[0]++;
				pl[1] = 0;
				pl[2] = (int) Math.round( pl[2] * ( 1.2 + ( rand.nextInt(3) / 10 ) ) );
				//Give reward
				Reward lvlReward = getLevelUpReward(pl[0]);
				boolean rSuccess = true;
				if(lvlReward.reward == 3) { //I had to copy this from event.giveRewardData, idk why, I don't care, I'm tired
					revive revive = new revive();
					rSuccess = revive.addRevive(u, "1", connection); //This should be dynamic but that breaks it :D
				} else if(lvlReward.reward == 4) {
					xpboosts xpboost = new xpboosts();
					rSuccess = xpboost.addBoost(u, "1Hr", connection); //This should be dynamic but that breaks it :D
				} else if(lvlReward.reward == 5) {
					profileicon profileicon = new profileicon();
					rSuccess = profileicon.addIcon(u, lvlReward.rewardValue, connection);
				}
				if(!rSuccess) {
					if(lvlReward.reward == 5 || lvlReward.reward == 6) { //Already owned most likely
						lvlReward.reward = 4;
						lvlReward.rewardValue = 0;
						xpboosts xpboost = new xpboosts();
						rSuccess = xpboost.addBoost(u, "1Hr", connection); //Yeah idk bro this hurts
					} //There was more error handling here but that breaks it :D
				}
				if(rewardString.length() > 0) { //Multi-Reward
					rewardString = rewardString + ";" + lvlReward.returnFormattedString();
				} else {
					rewardString = lvlReward.returnFormattedString();
				}
			} else {
				if(pl[0] >= 999) { //Level lock
					pl[1] = pl[2];
					break;
				}
			}
		}
		String levelString = pl[0] + "," + pl[1] + "," + pl[2];
		//Update account
		PreparedStatement stmt = connection.prepareStatement("UPDATE vc_accounts SET PlayerLevel=? WHERE Name=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		stmt.setString(1, levelString);
		stmt.setString(2, u.getName());
		int res = stmt.executeUpdate();
		if(res > 0) {
			sendLevelUpResult(u, levelString, rewardString);
		} else {
			sendError(u, "Failed to update account.");
		}
	}
	private int getLevelIncrease(int t, int d, boolean w) {
		Random rand = new Random();
		int randAmt = (25 + rand.nextInt(26));
		int xp = -1;
		if(t == 0) { //PVP
			if(w) {
				xp = 300 + randAmt;
			} else {
				xp = 200 + randAmt;
			}
		} else if(t == 2) { //COOP
			if(w) {
				xp = 350 + randAmt;
				if(d > 0) {
					double xpBoost = 1 + (d * 0.5);
					xp = (int) Math.round(xp * xpBoost);
				}
			} else {
				xp = 0;
			}
		}
		//Event boost
		if(xpevent.xpEventType == 4 && t == 0 || xpevent.xpEventType == 5 && t == 2) {
			xp = (int) Math.round(xp * xpevent.xpEventBoost);
		}
		return(xp);
	}
	private Reward getLevelUpReward(int pl) {
		Random rand = new Random();
		Reward rewardObj = new Reward();
		if(pl % 10 == 0 || pl == 999) { //10th level, high reward
			int roll = rand.nextInt(101);
			//80% Boost, 20% Icon
			if(roll <= 80) {
				rewardObj.reward = 4;
			} else {
				rewardObj.reward = 5;
			} //Add borders
		} else { //Regular reward
			rewardObj.reward = 0 + rand.nextInt(4);
		}
		if(rewardObj.reward == 0) { //Money
			rewardObj.rewardValue = 10000 * (1 + rand.nextInt(3));
		} else if(rewardObj.reward == 1) { //Gold Chest
			rewardObj.rewardValue = 2 + rand.nextInt(2);
		} else if(rewardObj.reward == 2) { //Synths
			rewardObj.rewardValue = 200 * (1 + rand.nextInt(2));
		} else if(rewardObj.reward == 3) { //Revive
			rewardObj.rewardValue = 1;
		} else if(rewardObj.reward == 4) { //XP Boost
			rewardObj.rewardValue = 0;
		} else if(rewardObj.reward == 5) { //Profile Icons
			rewardObj.rewardValue = rewardObj.iconRewards[rand.nextInt(8)];
		} else if(rewardObj.reward == 6) { //Backgrounds
			//TODO: Add backgrounds
		}
		return rewardObj;
	}
	private void sendResult(User u, String pl) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", true);
		resObj.putUtfString("Level", pl);
		send("playerlevel", resObj, u);
	}
	private void sendLevelUpResult(User u, String pl, String reward) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", true);
		resObj.putUtfString("Level", pl);
		resObj.putUtfString("Reward", reward);
		send("playerlevel", resObj, u);
	}
	private void sendError(User u, String e) {
		ISFSObject resObj = new SFSObject();
		resObj.putBool("Success", false);
		resObj.putUtfString("Error", e);
		send("playerlevel", resObj, u);
	}
}
