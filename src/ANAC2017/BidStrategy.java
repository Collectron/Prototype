package ANAC2017;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import negotiator.Bid;
import negotiator.issue.Issue;
import negotiator.issue.Value;
import negotiator.utility.AbstractUtilitySpace;
import negotiator.utility.AdditiveUtilitySpace;

public class BidStrategy {
	
	private AbstractUtilitySpace utilitySpace; // utility space
	private NegotiationStatistics Information; // negotiation information
	protected Bid maxBid = null; // maximum utility value Bid
	// Of the search parameters
	private static int NEAR_ITERATION = 1;
	private static int SA_ITERATION = 1;
	static double START_TEMPERATURE = 1.0; // start temperature
	static double END_TEMPERATURE = 0.0001; // end temperature
	static double COOL = 0.999; // cooling degree
	static int STEP = 1; // width to change
	static int STEP_NUM = 1; // number of times to change
	//private Bid maxBidUop = null;
	private Double Threshold = 1.0;
	
	public BidStrategy(AbstractUtilitySpace utilitySpace, NegotiationStatistics negotiatingInfo, double time) throws Exception {
		this.utilitySpace = utilitySpace;
		this.Information = negotiatingInfo;
		initMaxBid(); // initial search of maximum utility value Bid
		Information.initValueRelativeUtility(); //initialize the relative utility list 
		//Information.setValueRelativeUtility(maxBid, time); //to derive a relative utility value
	}
	

	// Initial search of maximum utility value Bid (for initially is unknown type of utility space, to explore using the SA)
	private void initMaxBid() throws Exception{
		
		if(utilitySpace instanceof AdditiveUtilitySpace ){
			maxBid = utilitySpace.getMaxUtilityBid();
		}
		else{
			int tryNum = utilitySpace.getDomain().getIssues().size(); // Number of trials
			maxBid = utilitySpace.getDomain().getRandomBid(null);
			for (int i = 0; i < tryNum; i++) {
				try {
					do{ 
						SimulatedAnnealingSearch(maxBid, 1.0, 0.0);
					} while (utilitySpace.getUtilityWithDiscount(maxBid, 0.0) < utilitySpace.getReservationValue());
					if(utilitySpace.getUtilityWithDiscount(maxBid, 0.0) >= 0.99){ 
						break; 
					}
				} catch (Exception e) {
					System.out.println(" failed to initial search of maximum utility value Bid ");
					e.printStackTrace();
				}
			}
		}
		//System.out.println("The max bid utility: " + utilitySpace.getUtilityWithDiscount(maxBid,0.0));
	}
	
	// Return the Bid
	public Bid getBid(Bid baseBid, double threshold, double time) {
		
		try {
			Bid bid = new Bid(getBidbyNeighborhoodSearch(baseBid, threshold, time)); 	// vicinity Exploration
			if (utilitySpace.getUtilityWithDiscount(bid, time) < threshold){ //search the draft agreement candidate with an equal to or greater than the threshold value of the utility value
				bid = getBidbyAppropriateSearch(baseBid, threshold, time);
			} 
			if (utilitySpace.getUtility(bid) < threshold) { // if Bid obtained by the search is less than the threshold, with respect to the maximum utility value Bid
				bid = new Bid(maxBid); 
//				System.out.println("Nothing worked out. We give our maxBid! ");
			} 
			//bid = getConvertBidbyFrequencyList(bid, time); // to replace the Value of the Bid in accordance with the FrequencyList
			return bid;
		} catch (Exception e) {
			System.out.println(" in search of the Bid failed ");
			e.printStackTrace();
			return baseBid;
		}
	}
	
	// Search of the Bid by the vicinity search
	private Bid getBidbyNeighborhoodSearch(Bid baseBid, double threshold, double time) {
		Bid bid = new Bid(baseBid);
		try { 
			for (int i = 0; i < NEAR_ITERATION; i++) {
				bid = NeighborhoodSearch(bid, threshold, time); 
			} 
		} 
		catch (Exception e) {
				System.out.println("failed in the vicinity search ");
				System.out.println("Problem with received bid(Near:last):" + e.getMessage() + ". cancelling bidding");
		}
		return bid;
	}
		
	// Vicinity Exploration
	private Bid NeighborhoodSearch(Bid baseBid, double threshold, double time) throws Exception {
		Bid currentBid = new Bid(baseBid); // Current Bid
		double currenBidUtil = utilitySpace.getUtilityWithDiscount(baseBid, time);
		ArrayList<Bid> targetBids = new ArrayList<Bid>(); // ArrayList of optimal utility value Bid
		double targetBidUtil = 0.0;
//		Random randomnr = new Random(); // random number
		ArrayList<Value> values = null;
		List<Issue> issues = Information.getIssues();
		Bid NewPossibleBid = null;
		
		for (Issue issue:issues) {
			values = Information.getValues(issue);
			for (Value value:values) {
				NewPossibleBid = new Bid(currentBid.putValue(issue.getNumber(), value)); // seek the Bid in the vicinity
				currenBidUtil = utilitySpace.getUtilityWithDiscount(NewPossibleBid, time);
				// Update maximum utility value Bid
				if (maxBid == null || currenBidUtil >= utilitySpace.getUtilityWithDiscount(maxBid, time)) {
					maxBid = new Bid(NewPossibleBid); 
				}	
				// Update
				if (currenBidUtil >= threshold){
					if(targetBids.size() == 0){ 
						targetBids.add(new Bid(NewPossibleBid)); 
						targetBidUtil = currenBidUtil;
					} else{
						if(currenBidUtil < targetBidUtil){
							targetBids.clear(); // Initialization
							targetBids.add(new Bid(NewPossibleBid)); // Add the element
							targetBidUtil = currenBidUtil;
						} else if (currenBidUtil == targetBidUtil){
							targetBids.add(new Bid(NewPossibleBid)); // Add the element
						}
					}
				}
			}
		}

		if (targetBids.size() == 0) { // when it can not find the Bid with a large utility value than the boundary value, return the baseBid
			return new Bid(baseBid); 
		}
		else { // utility value returns a Bid which is near the boundary value
			double maxUopUtil = 0.0;
			double uoptmp = 0.0;
			Bid max_bid = null;
			for(Bid bids : targetBids){
				uoptmp = Uop(bids, time);
				if(uoptmp > maxUopUtil){
					maxUopUtil = uoptmp;
					max_bid = bids;
				}
			}
			return new Bid(max_bid);
		}
	}
	
	// Search of Bid
	private Bid getBidbyAppropriateSearch(Bid baseBid, double threshold, double time) {
		Bid bid = null;
		double currenBidUtil = utilitySpace.getUtilityWithDiscount(baseBid, time);
		ArrayList<Bid> targetBids = new ArrayList<Bid>(); // ArrayList of optimal utility value Bid
		double targetBidUtil = 0.0;
		try {
			// Search for the linear utility space
			if(Information.isLinearUtilitySpace()){
				//
				for(int i =0; i<utilitySpace.getDomain().getIssues().size();i++){
					bid = new Bid(relativeUtilitySearch(threshold, time));
					currenBidUtil= utilitySpace.getUtilityWithDiscount(bid, time);
					
					if (currenBidUtil >= threshold){
						if(targetBids.size() == 0){
							targetBids.add(new Bid(bid)); 
							targetBidUtil = currenBidUtil;
						} else{
							if(currenBidUtil < targetBidUtil){
								targetBids.clear(); // Initialization
								targetBids.add(new Bid(bid)); // Add the element
								targetBidUtil = currenBidUtil;
							} else if (currenBidUtil == targetBidUtil){
								targetBids.add(new Bid(bid)); // Add the element
							}
						}
					}
					
				}
				int size = targetBids.size();
				if (size != 0) { // utility value returns a Bid which is near the boundary value
//					System.out.println("We got ourselves "+size+"goodies!!!");
					double maxUopUtil = 0.0;
					double uoptmp = 0.0;
					bid = null;
					for(Bid bids : targetBids){
						uoptmp = Uop(bids, time);
						if(uoptmp > maxUopUtil){
							maxUopUtil = uoptmp;
							bid = bids;
						}
					}
				}
				
//				System.out.println("We used the relativeUtilitySearch!!!");
				if(utilitySpace.getUtilityWithDiscount(bid, time) < threshold){ // If search fails, switch to search for nonlinear utility space
					Information.utilitySpaceTypeisNonLinear(); 
					if(bid.equals(maxBid)){
		//				System.out.println("Einai idio me to MaxBid");
					}
	//				System.out.println("From here on out, we never use the relativeUtilitySearch!!!");
				} 
				
				//
				/*
				bid = new Bid(relativeUtilitySearch(threshold, time));
				System.out.println("We used the relativeUtilitySearch!!!");
				if(utilitySpace.getUtilityWithDiscount(bid, time) < threshold){ // If search fails, switch to search for nonlinear utility space
					Information.utilitySpaceTypeisNonLinear(); 
					if(bid.equals(maxBid)){
						System.out.println("Einai idio me to MaxBid");
					}
					System.out.println("From here on out, we never use the relativeUtilitySearch!!!");
				} 
				*/
			} 
			
			// Search for the non-linear utility space
			if(!Information.isLinearUtilitySpace()){
				Bid currentBid = null;
				double currentBidUtil = 0;
				double min = 1.0;
				for (int i = 0; i < SA_ITERATION; i++) {
					if(bid == null){
		//				System.out.print("Yep we never use the relativeUtilitySearch ");
						if(utilitySpace instanceof AdditiveUtilitySpace ){
		//					System.out.println("in a AdditiveUtilitySpace -.-");
						}
						bid = new Bid(baseBid);
					}
					currentBid = SimulatedAnnealingSearchForUop(bid, threshold, time);
					currentBidUtil = utilitySpace.getUtilityWithDiscount(currentBid, time);
					if (currentBidUtil <= min && currentBidUtil >= threshold) {
						bid = new Bid(currentBid);
						min = currentBidUtil;
					} 
				} 
			}
		} catch (Exception e) {
			System.out.println(" failed to SA search ");
			System.out.println("Problem with received bid(SA:last):" + e.getMessage() + ". cancelling bidding");
		}
		return bid;
	}
	
	// Search based on the relative utility value
	private Bid relativeUtilitySearch(double threshold, double time) throws Exception{
		Bid BidReturnBid = null;
		//ArrayList<Bid> targetBids = new ArrayList<Bid>(); // ArrayList of optimal utility value Bid
		//double targetBidUtil = 0.0;
		Bid temp = new Bid(maxBid);
		double d = threshold - utilitySpace.getUtilityWithDiscount(maxBid, time); // the difference between the maximum utility value
		double concessionSum = 0.0; // the sum of the reduced utility value
		double relativeUtility = 0.0;
		//double currenBidUtil = utilitySpace.getUtilityWithDiscount(temp, time);
		Information.setValueRelativeUtility(maxBid, time); //to derive a relative utility value
		HashMap<Issue, HashMap<Value, Double>> valueRelativeUtility = Information.getValueRelativeUtility();
		List<Issue> randomIssues = Information.getIssues();
		Collections.shuffle(randomIssues);
		ArrayList<Value> randomValues = null;
		for(Issue issue:randomIssues){
			randomValues = Information.getValues(issue);
			Collections.shuffle(randomValues);
			for(Value value:randomValues){
				relativeUtility = valueRelativeUtility.get(issue).get(value); // maximum utility value as a reference relative utility value
				if(d <= concessionSum + relativeUtility){
					BidReturnBid = new Bid(temp.putValue(issue.getNumber(), value));
					temp = new Bid(BidReturnBid);
					concessionSum += relativeUtility;
					/*
					currenBidUtil = utilitySpace.getUtilityWithDiscount(temp, time);
					
					// Update
					if (currenBidUtil >= threshold){
						if(targetBids.size() == 0){
							targetBids.add(new Bid(BidReturnBid)); 
							targetBidUtil = currenBidUtil;
						} else{
							if(currenBidUtil < targetBidUtil){
								targetBids.clear(); // Initialization
								targetBids.add(new Bid(BidReturnBid)); // Add the element
								targetBidUtil = currenBidUtil;
							} else if (currenBidUtil == targetBidUtil){
								targetBids.add(new Bid(BidReturnBid)); // Add the element
							}
						}
					}*/
					break;
				}
			}
		}	
		
		return new Bid(BidReturnBid); 
		
	}

	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private double targetEnd(double time){
		
		double weightedAverage = Information.getMyH()*utilitySpace.getUtilityWithDiscount(maxBid, time);
		double normalizer = Information.getMyH();
		for(Object ids : Information.getOpponents()){
			HashMap<Issue, Value> bestVals = Information.getTheMaxValuesForOpponent(ids);
			Bid opponentBid = new Bid(maxBid);
			Bid temp = new Bid(maxBid);
			for(Issue issue : Information.getIssues()){
				opponentBid = new Bid(temp.putValue(issue.getNumber(), bestVals.get(issue)));
				temp = new Bid(opponentBid);
			}
			weightedAverage += Information.getopponentsH().get(ids)*utilitySpace.getUtilityWithDiscount(opponentBid, time);
			normalizer += Information.getopponentsH().get(ids);
		}
		return (weightedAverage/normalizer);
	}
	
	protected double targetTime(double time){
		
		double targetEnd = targetEnd(time);
	//	System.out.println("TargetEnd(t) = "+targetEnd);
		double df = utilitySpace.getDiscountFactor();
		if(df < 1.0){
			
			double returnVal = ((1 - Math.pow(time,df))*(1 - targetEnd) + targetEnd);
			return returnVal;
		}
		else{
			double returnVal = ((1 - Math.pow(time,3))*(1 - targetEnd) + targetEnd);
			return returnVal;
		}
	}
	
	protected double Uop(Bid bid, double time){
		double temp = Information.getMyH()*(utilitySpace.getUtilityWithDiscount(bid, time))/(utilitySpace.getUtilityWithDiscount(maxBid, time));
		double xXxnormalizer420blazeit = Information.getMyH();
		for(Object ids : Information.getOpponents()){
			HashMap<Issue, Value> bestVals = Information.getTheMaxValuesForOpponent(ids);
			Bid tempv = new Bid(maxBid);
			Bid opponentBid = null;
			
			for(Issue issue : Information.getIssues()){
				opponentBid = new Bid(tempv.putValue(issue.getNumber(), bestVals.get(issue)));
				tempv = new Bid(opponentBid);
			}
			
			double ua = 0.0;	
			double max_ua=0.0;
			HashMap<Issue, HashMap<Value, Integer>> frequencyMap = Information.getOpponentValueFrequency(ids);
			for(Issue issue:Information.getIssues()){		
				Value value1 = bid.getValue(issue.getNumber());
				ua += frequencyMap.get(issue).get(value1);
				Value value2 = opponentBid.getValue(issue.getNumber());
				max_ua += frequencyMap.get(issue).get(value2);
			}		
			
			xXxnormalizer420blazeit += Information.getopponentsH().get(ids);
			temp += Information.getopponentsH().get(ids)*ua/max_ua;
		}		
		
		return temp/xXxnormalizer420blazeit;
	}
	
	// SA
	protected Bid SimulatedAnnealingSearch(Bid baseBid, double threshold, double time) throws Exception {
		Bid temp = new Bid(baseBid); // generation of initial solution
		double currenBidUtil = utilitySpace.getUtilityWithDiscount(baseBid, time);
		Bid nextBid = null; // Bid Evaluation 
		double nextBidUtil = 0.0;
		ArrayList<Bid> targetBids = new ArrayList<Bid>(); // ArrayList of optimal utility value Bid
		double targetBidUtil = 0.0;
		double p; // mobility Probability
		Random randomnr = new Random(); // random number
		double currentTemperature = START_TEMPERATURE; // Current temperature
		double newCost = 1.0;
		double currentCost = 1.0;
		List<Issue> issues = Information.getIssues();
		
		while (currentTemperature > END_TEMPERATURE) { // loop until the temperature drops enough
			for (int i = 0; i < STEP_NUM; i++) { // get the Bid in the vicinity
				int issueIndex = randomnr.nextInt(issues.size()); // specified in the random issue
				Issue issue = issues.get(issueIndex); // Issue of the specified index
				ArrayList<Value> values = Information.getValues(issue);
				int valueIndex = randomnr.nextInt(values.size()); // specified in the random in the range of up get value
				nextBid = new Bid(temp.putValue(issue.getNumber(), values.get(valueIndex))); 
				nextBidUtil = utilitySpace.getUtilityWithDiscount(nextBid, time);
				if (maxBid == null || nextBidUtil >= utilitySpace.getUtilityWithDiscount(maxBid, time)) { // update of maximum utility value Bid
					maxBid = new Bid(nextBid); 
				} 
			}

			newCost = Math.abs(threshold - nextBidUtil);
			currentCost = Math.abs(threshold - currenBidUtil);
			p = Math.exp(-Math.abs(newCost - currentCost) / currentTemperature);
			if (newCost < currentCost || p > randomnr.nextDouble()) { 
				temp = new Bid(nextBid); // Update Bid
				currenBidUtil = nextBidUtil;
			} 

			// Update
			if (currenBidUtil >= threshold){
				if(targetBids.size() == 0){ 
					targetBids.add(new Bid(temp)); 
					targetBidUtil = currenBidUtil;
				} else{
					if(currenBidUtil < targetBidUtil){
						targetBids.clear(); // Initialization
						targetBids.add(new Bid(temp)); // Add the element
						targetBidUtil = currenBidUtil;
					} else if (currenBidUtil == targetBidUtil){
						targetBids.add(new Bid(temp)); // Add the element
					}
				}
			}
			currentTemperature = currentTemperature * COOL; // lowering the temperature
		}			

		if (targetBids.size() == 0) { // when it can not find the Bid with a large utility value than the boundary value, return the baseBid
			return new Bid(baseBid); 
		} 
		else { // utility value returns a Bid which is near the boundary value
			return new Bid(targetBids.get(randomnr.nextInt(targetBids.size()))); 
		} 
	}
	
	// SA for Uop
	protected Bid SimulatedAnnealingSearchForUop(Bid baseBid, double threshold, double time) throws Exception {
		Bid temp = new Bid(baseBid); // generation of initial solution
		double currenBidUtil = utilitySpace.getUtilityWithDiscount(baseBid, time);
		Bid nextBid = null; // Bid Evaluation 
		double nextBidUtil = 0.0;
		ArrayList<Bid> targetBids = new ArrayList<Bid>(); // ArrayList of optimal utility value Bid
		double targetBidUtil = 0.0;
		double p; // mobility Probability
		Random randomnr = new Random(); // random number
		double currentTemperature = START_TEMPERATURE; // Current temperature
		double newCost = 1.0;
		double currentCost = 1.0;
		List<Issue> issues = Information.getIssues();
		
		while (currentTemperature > END_TEMPERATURE) { // loop until the temperature drops enough
			for (int i = 0; i < STEP_NUM; i++) { // get the Bid in the vicinity
				int issueIndex = randomnr.nextInt(issues.size()); // specified in the random issue
				Issue issue = issues.get(issueIndex); // Issue of the specified index
				ArrayList<Value> values = Information.getValues(issue);
				int valueIndex = randomnr.nextInt(values.size()); // specified in the random in the range of up get value
				nextBid = new Bid(temp.putValue(issue.getNumber(), values.get(valueIndex))); 
				nextBidUtil = utilitySpace.getUtilityWithDiscount(nextBid, time);
				if (maxBid == null || nextBidUtil >= utilitySpace.getUtilityWithDiscount(maxBid, time)) { // update of maximum utility value Bid
					maxBid = new Bid(nextBid); 
				} 
			}

			newCost = Math.abs(threshold - nextBidUtil);
			currentCost = Math.abs(threshold - currenBidUtil);
			p = Math.exp(-Math.abs(newCost - currentCost) / currentTemperature);
			if (newCost < currentCost || p > randomnr.nextDouble()) { 
				temp = new Bid(nextBid); // Update Bid
				currenBidUtil = nextBidUtil;
			} 

			// Update
			if (currenBidUtil >= threshold){
				if(targetBids.size() == 0){ 
					targetBids.add(new Bid(temp)); 
					targetBidUtil = currenBidUtil;
				} else{
					if(currenBidUtil < targetBidUtil){
						targetBids.clear(); // Initialization
						targetBids.add(new Bid(temp)); // Add the element
						targetBidUtil = currenBidUtil;
					} else if (currenBidUtil == targetBidUtil){
						targetBids.add(new Bid(temp)); // Add the element
					}
				}
			}
			currentTemperature = currentTemperature * COOL; // lowering the temperature
		}			

		if (targetBids.size() == 0) { // when it can not find the Bid with a large utility value than the boundary value, return the baseBid
			return new Bid(baseBid); 
		} 
		else { // utility value returns a Bid which is near the boundary value
			//return new Bid(targetBids.get(randomnr.nextInt(targetBids.size()))); 
			//double maxUopUtil = 0.0;
			double maxUopUtil = -1000.0;
			Bid max_bid = null;
			for(Bid bids : targetBids){
				double uoptmp = Uop(bids, time);
//				System.out.println("Uoptmp: "+uoptmp);
				if(uoptmp > maxUopUtil){
					maxUopUtil = uoptmp;
					max_bid = bids;
				}
			}
			
			if(max_bid == null) {
				Random rand = new Random();
				int location = rand.nextInt(targetBids.size()-1);
				return new Bid(targetBids.get(location));
			}
			else return new Bid(max_bid);

		} 
	}
		
//	public Double getCurrentThreshold(){
//		return Threshold;
//	}
	
	public double choose_Concession(double time) throws Exception{
		ArrayList<Double> threshold = new ArrayList<Double>();
		for(Object ids : Information.getOpponents()){
			threshold.add(Information.getReactiveConcession(ids));
		}
		double minConcession = 10000.0;
		for(Double Opponent_Concession:threshold){
	//		System.out.println("Opponent_Concession = "+Opponent_Concession);
			if(Opponent_Concession<minConcession){
				minConcession = Opponent_Concession;
			}
		}
		return minConcession;
	}
	
	public Double getNewThreshold(Double time)throws Exception{
		double targetTime = targetTime(time);
//		System.out.println("TargetTime(t) = "+targetTime);
		double concession=0;
		
		try{
			concession = choose_Concession(time);
		}catch(Exception e){
			System.out.println("Failed in choosing the concession of new round");
			e.printStackTrace();
		}
		concession = Threshold-concession;
//		System.out.println("Concession(t) = "+concession);
		Threshold = Math.max(targetTime, concession);
		if(Threshold>1.0){
			Threshold=1.0;
		}
		return Threshold;
	}
	
}
