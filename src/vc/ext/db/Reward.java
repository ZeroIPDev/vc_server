package vc.ext.db;

public class Reward {
	
	public int reward;
	public int rewardValue;
	public String rewardKeyWord;
	
	public int[] iconRewards = {6, 7, 8, 9, 10, 11, 12, 13};
	
	public String returnFormattedString() {
		if(reward == 6) {
			return reward + "," + rewardKeyWord;
		} else {
			return reward + "," + rewardValue;
		}
	}
	public void parseFormattedString(String s) {
		String[] splitReward = s.split(",");
		reward = Integer.parseInt(splitReward[0]);
		setRewardValue(splitReward[1]);
	}
	public void setRewardValue(String s) {
		if(reward == 6) {
			rewardKeyWord = s;
		} else {
			rewardValue = Integer.parseInt(s);
		}
	}
	
}
