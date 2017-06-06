package ANAC2017;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import negotiator.Bid;
import negotiator.issue.Issue;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.IssueInteger;
import negotiator.issue.Value;
import negotiator.issue.ValueInteger;
import negotiator.issue.ValueDiscrete;
import negotiator.utility.AbstractUtilitySpace;

public class NegotiationStatistics {
	
	private AbstractUtilitySpace utilitySpace; //My utility space
	private List<Issue> issues; // Issues of the domain
	private ArrayList<Object> opponents; // our opponents in the negotiation table
	private ArrayList<Bid> MyBidHistory = null; // Our Agents Bid history
	private Double myH = null;
	private ArrayList<Bid> BestOfferedBidHistory = null; // List of the Best Offered Bids in the negotiation
	private ArrayList<Bid> BestPopularBidsList = null; // List of Best Popular Bids in the negotiation
	private HashMap<Object, ArrayList<Bid>> opponentsBidHistory = null; // Every opponents bid history
	private HashMap<Object, ArrayList<Bid>> opponentsAcceptHistory = null; // Every opponents accept history
	private HashMap<Object, ArrayList<Double>> opponentsTimeBidHistory = null; // A list containing the time intervals at witch each Bid of the opponent was offered
	private HashMap<Object, Double> opponentsH; //the amount we are conceding towards each opponent with h e[0 1]
	private HashMap<Object, Double> opponentsAverage; // average
	private HashMap<Object, Double> opponentsVariance; // Dispersion
	private HashMap<Object, Double> opponentsSum; // sum
	private HashMap<Object, Double> opponentsPowSum; // Sum of squares
	private HashMap<Object, Double> opponentsStandardDeviation; // standard deviation
	private HashMap<Object, Double> opponentsWorstOfferUtil; //the worst utility the opponent offered our agent
	private HashMap<Object, Double> opponentsBestOfferUtil; //the best utility the opponent offered our agent
	private HashMap<Object, Double> opponentsBestOfferUtilWithoutLastOffer; //the best utility the opponent offered our agent counting out his/hers last bid
	private HashMap<Issue, HashMap<Value, Double>> valueRelativeUtility = null; // own relative utility value matrix of each issue value in the utility space (for linear utility space)
	private HashMap<Issue, HashMap<Value, Integer>> allValueFrequency = null; // the value frequency matrix of each issue
	private HashMap<Object, HashMap<Issue, HashMap<Value, Integer>>> opponentsValueFrequency = null; // the value frequency matrix of each issue of each negotiator
	private HashMap<Object, HashMap<Issue, HashMap<Value, Double>>> opponentsValueFrequencyWeighted = null; // the value frequency matrix of each issue of each negotiator weighted by the WeightFunction
	private HashMap<Object, HashMap<Issue, HashMap<Value, Integer>>> opponentsAcceptedValueFrequency = null; // the value frequency matrix of each issue of each negotiator
	private double BestOfferedUtility = 0.0; // BestOfferedUtility
	private int negotiatorNum = 3; // negotiator number initialized in three people by default
	private double time_scale = 0.0; // time interval
	private int round = 0;
	private boolean isLinearUtilitySpace = true; // whether it is a linear utility space
	private double MaxPopularBidUtility = 0.0; // MaxPopularBidUtility
	private double MyWorstOfferedUtility = 1.0; //The worst utility our agent has offered so far
	private Random randomnr; // random number
	private Double WeightFunction = 1.0; //default weight function is a constant function
	private Boolean ConstantWeightFunction=true; // by default our weight function is constant
	
	public NegotiationStatistics(AbstractUtilitySpace utilitySpace) {
		// Initialization
		this.utilitySpace = utilitySpace;
		issues = utilitySpace.getDomain().getIssues();
		opponents = new ArrayList<Object>();
		MyBidHistory = new ArrayList<Bid>();
		randomnr = new Random(); 
//		myH = randomnr.nextDouble();
		myH = 1.0;
		BestOfferedBidHistory = new ArrayList<Bid>();
		BestPopularBidsList = new ArrayList<Bid>();
		opponentsBidHistory = new HashMap<Object, ArrayList<Bid>>();
		opponentsAcceptHistory = new HashMap<Object, ArrayList<Bid>>();
		opponentsTimeBidHistory = new HashMap<Object, ArrayList<Double>>(); 
		opponentsAverage = new HashMap<Object, Double>();
		opponentsVariance = new HashMap<Object, Double>();
		opponentsSum = new HashMap<Object, Double>();
		opponentsPowSum = new HashMap<Object, Double>();
		opponentsStandardDeviation = new HashMap<Object, Double>();
		opponentsWorstOfferUtil = new HashMap<Object, Double>();
		opponentsBestOfferUtil = new HashMap<Object, Double>();
		opponentsBestOfferUtilWithoutLastOffer = new HashMap<Object, Double>();
		valueRelativeUtility = new HashMap<Issue, HashMap<Value, Double>>();
		allValueFrequency = new HashMap<Issue, HashMap<Value, Integer>>();
		opponentsValueFrequency = new HashMap<Object, HashMap<Issue, HashMap<Value, Integer>>>();
		opponentsValueFrequencyWeighted = new HashMap<Object, HashMap<Issue, HashMap<Value, Double>>>();
		opponentsAcceptedValueFrequency = new HashMap<Object, HashMap<Issue, HashMap<Value, Integer>>>();
		opponentsH = new HashMap<Object,Double>();

		try {
			initAllValueFrequency();
		} catch (Exception e1) {
			System.out.println(" Failed to initialize all the content of the frequency matrix ");
			e1.printStackTrace();
		}
		try {
			initValueRelativeUtility();
		} catch (Exception e) {
			System.out.println(" to initialize the relative utility matrix failed ");
			e.printStackTrace();
		}
		System.out.println(" NegotiationStatistics initialized ");
	}
	
	// Initialization of all content of the frequency matrix
		private void initAllValueFrequency() throws Exception {
			ArrayList<Value> values = null;
			for (Issue issue : issues) {
				allValueFrequency . put (issue, new  HashMap < Value , Integer > ()); // initialization of the issue row
				values = getValues(issue);
				for (Value value : values) { // Initialization of the elements of the issue row
					allValueFrequency.get(issue).put(value, 0);
				}
			}
		}
		
		// initialization of relative utility matrix
		public void initValueRelativeUtility() throws Exception {
			ArrayList<Value> values = null;
			for (Issue issue : issues) {
				valueRelativeUtility . put (issue, new  HashMap < Value , Double > ()); // initialization of the issue row
				values = getValues(issue);
				for (Value value : values) { // Initialization of the elements of the issue row
					valueRelativeUtility.get(issue).put(value, 0.0);
				}
			}
		}
		
		// It returns a list of possible values ​​in issue
		public ArrayList<Value> getValues(Issue issue) {
			ArrayList<Value> values = new ArrayList<Value>();
			switch (issue.getType()) {
				case DISCRETE:
					List<ValueDiscrete> valuesDis = ((IssueDiscrete) issue).getValues();
					for (Value value : valuesDis) {
						values.add(value);
					}
					break;
				case INTEGER:
					int min_value = ((IssueInteger) issue).getLowerBound();
					int max_value = ((IssueInteger) issue).getUpperBound();
					for (int j = min_value; j <= max_value; j++) {
						values.add(new ValueInteger(new Integer(j)));
					}
					break;
				default:
					try {
						throw new Exception("issue type " + issue.getType()
								+ " not supported by Prototype");
					} catch (Exception e) {
						System.out.println(" Failed to get the possible values of the issue ");
						e.printStackTrace();
					}
			}
			return values;
		}
		
		public void initOpponent(Object sender) {
			initNegotiatingInfo(sender); // initialize the negotiation information
			try {
				initOpponentsValueFrequency(sender);
			} // Initialize the frequency matrix of the sender
			catch (Exception e) {
				System.out.println(" negotiations failed to initialize the participants in the frequency matrix ");
				e.printStackTrace();
			}
			opponents.add(sender); // add the sender to the negotiation participants
		}
		
		//initialize the information of the opponent negotiator
		private void initNegotiatingInfo(Object sender) {
			//opponentsH.put(sender, randomnr.nextDouble());
			opponentsH.put(sender, 0.5);
			opponentsBidHistory.put(sender, new ArrayList<Bid>());
			opponentsTimeBidHistory.put(sender, new ArrayList<Double>());
			opponentsAcceptHistory.put(sender, new ArrayList<Bid>());
			opponentsAverage.put(sender, 0.0);
			opponentsVariance.put(sender, 0.0);
			opponentsSum.put(sender, 0.0);
			opponentsPowSum.put(sender, 0.0);
			opponentsStandardDeviation.put(sender, 0.0);
			opponentsWorstOfferUtil.put(sender, null);
			opponentsBestOfferUtil.put(sender, null);
			opponentsBestOfferUtilWithoutLastOffer.put(sender, null);
		}
		
		//initialize the value frequency of this opponent negotiator
		private void initOpponentsValueFrequency(Object sender) throws Exception {
			opponentsValueFrequency.put(sender, new  HashMap < Issue , HashMap < Value , Integer > > ()); // initialization of the sender of the frequency matrix
			opponentsValueFrequencyWeighted.put(sender, new HashMap< Issue, HashMap< Value, Double > >()); // initialization of the sender of the frequency matrix with weights
			opponentsAcceptedValueFrequency.put(sender, new  HashMap < Issue , HashMap < Value , Integer > > ()); // initialization of the sender of the accept frequency matrix
			for (Issue issue : issues) {
				opponentsValueFrequency.get(sender).put(issue, new  HashMap < Value , Integer > ()); // initialization of the issue row in the frequency matrix
				opponentsValueFrequencyWeighted.get(sender).put(issue, new  HashMap < Value , Double > ()); // initialization of the sender of the frequency matrix with weights
				opponentsAcceptedValueFrequency.get(sender).put(issue, new  HashMap < Value , Integer > ()); // initialization of the sender of the accept frequency matrix
				ArrayList<Value> values = getValues(issue);
				for (Value value : values) { // Initialization element number of occurrences of the issue row
					opponentsValueFrequency.get(sender).get(issue).put(value, 0);
					opponentsValueFrequencyWeighted.get(sender).get(issue).put(value, 0.0);
					opponentsAcceptedValueFrequency.get(sender).get(issue).put(value, 0);
				}
			}
		}
		
		// Derivation of relative utility matrix
		public void setValueRelativeUtility(Bid Bid, double time) throws Exception {
			ArrayList<Value> values = null;
			Bid currentBid = null;
			Bid newBid = null;
			for (Issue issue : issues) {
				currentBid = new Bid(Bid);
				values = getValues(issue);
				for (Value value : values) {
					newBid = new Bid(currentBid.putValue(issue.getNumber(), value));
					valueRelativeUtility.get(issue).put(value, utilitySpace.getUtilityWithDiscount(newBid,time) - utilitySpace.getUtilityWithDiscount(Bid,time));
				}
			}
		}
		
		// Returns proposed time interval
		public void updateTimeScale(double time) {
			round = round + 1;
			time_scale = time / round;
		}
		
		// Return the negotiator number
		public void updateOpponentsNum(int num) {
			negotiatorNum = num;
		}
		
		// Update of own proposal information
		public void updateMyBidHistory(Bid offerBid) {
			MyBidHistory.add(offerBid);
			if(utilitySpace.getUtilityWithDiscount(offerBid,0.0) < MyWorstOfferedUtility){ //update our agents worst offered Bid
				MyWorstOfferedUtility=utilitySpace.getUtilityWithDiscount(offerBid,0.0);
			}
		}
		
		public void updateInfo(Object sender, Bid offeredBid, double timeOfTheOffer) {
			try {
				updateNegotiatingInfo(sender, offeredBid, timeOfTheOffer);
			} // Update of negotiations information
			catch (Exception e1) {
				System.out.println(" failed to update the negotiation information ");
				e1.printStackTrace();
			}
			try {
				updateFrequencyList(sender, offeredBid);
			} // Update of the sender of the frequency matrix
			catch (Exception e) {
				System.out.println(" failed to update the frequency matrix ");
				e.printStackTrace();
			}
		}
		
		public void updateNegotiatingInfo(Object sender, Bid offeredBid, double timeOfTheOffer) throws Exception {
			opponentsBidHistory.get(sender).add(offeredBid); // Proposal history
			opponentsTimeBidHistory.get(sender).add(timeOfTheOffer); // Proposal history

			double util = utilitySpace.getUtilityWithDiscount(offeredBid,0.0);
			opponentsSum.put(sender, opponentsSum.get(sender) + util); // sum
			opponentsPowSum.put(sender, opponentsPowSum.get(sender) + Math.pow(util, 2)); // Sum of squares

			int round_num = opponentsBidHistory.get(sender).size();
			opponentsAverage.put(sender, opponentsSum.get(sender) / round_num); // average
			opponentsVariance.put(sender, (opponentsPowSum.get(sender) / round_num) - Math.pow(opponentsAverage.get(sender), 2)); // Dispersion

			if (opponentsVariance.get(sender) < 0) {
				opponentsVariance.put(sender, 0.0);
			}
			opponentsStandardDeviation.put(sender, Math.sqrt(opponentsVariance.get(sender))); // standard deviation
			
			if(opponentsWorstOfferUtil.get(sender)==null){ //initialize worst offer
				opponentsWorstOfferUtil.put(sender, util);
			}else{
				if(util < opponentsWorstOfferUtil.get(sender)){ //update worst offer
					opponentsWorstOfferUtil.put(sender, util);
				}
			}

			if((opponentsBestOfferUtil.get(sender)==null)&&(opponentsBestOfferUtilWithoutLastOffer.get(sender)==null)){ //initialize best offers
				opponentsBestOfferUtil.put(sender, util);
				opponentsBestOfferUtilWithoutLastOffer.put(sender, util);
			}else{ //update best offers
				opponentsBestOfferUtilWithoutLastOffer.put(sender, opponentsBestOfferUtil.get(sender));
				if(util > opponentsBestOfferUtil.get(sender)){
					opponentsBestOfferUtil.put(sender, util);
				}
			}
			
			if (util > BestOfferedUtility) {
				BestOfferedBidHistory.add(offeredBid); // Add to BestOfferedUtility update history
				BestOfferedUtility = util; // Update BestOfferedUtility
			}
			
			Double opponentsMean = opponentsAverage.get(sender);
			Double oppStandardDeviation = opponentsStandardDeviation.get(sender);
			// Sampling from opponent's Gaussian Propability Distribution of bids
			Double gaussianProb = opponentsMean + randomnr.nextGaussian() * oppStandardDeviation ;
			opponentsH.put(sender, gaussianProb);
			
		}
		
		// The frequency matrix update
		private void updateFrequencyList(Object sender, Bid offeredBid) throws Exception {
			for (Issue issue : issues) {
				Value value = offeredBid.getValue(issue.getNumber());
				opponentsValueFrequency.get(sender).get(issue).put(value, opponentsValueFrequency.get(sender).get(issue).get(value) + 1);// update the list
				if(!ConstantWeightFunction){ //if we have a constant WeightFunction, there is no point in updating this list at all, cause we don't need it
					opponentsValueFrequencyWeighted.get(sender).get(issue).put(value, opponentsValueFrequency.get(sender).get(issue).get(value) + WeightFunction);// update the list
				}
				
				allValueFrequency.get(issue).put(value, allValueFrequency.get(issue).get(value) + 1); // update the list
			}
		}
		
		public void updateAcceptanceHistory(Object sender, Bid AcceptedBid) throws Exception{
			opponentsAcceptHistory.get(sender).add(AcceptedBid);
			updateAcceptedValuesFrequency(sender,AcceptedBid);
		}
		
		public void updateAcceptedValuesFrequency(Object sender, Bid AcceptedBid) throws Exception{
			for (Issue issue : issues) {
				Value value = AcceptedBid.getValue(issue.getNumber());
				opponentsAcceptedValueFrequency.get(sender).get(issue).put(value, opponentsAcceptedValueFrequency.get(sender).get(issue).get(value) + 1); // update the list
			}
		}
		
		// Return the PopularBidList
		public void updatePopularBidList(Bid popularBid) throws Exception {
			if (!BestPopularBidsList.contains(popularBid)) { // unique record
				BestPopularBidsList.add(popularBid);
				MaxPopularBidUtility = Math.max(MaxPopularBidUtility, utilitySpace.getUtilityWithDiscount(popularBid,0.0));
				Collections.sort(BestPopularBidsList, new UtilityComparator()); // sort

				if (Prototype.isPrinting) {
					System.out.println("ranking");
					for (int i = 0; i < BestPopularBidsList.size(); i++) {
						System.out.println(utilitySpace.getUtilityWithDiscount(BestPopularBidsList.get(i),0.0));
					}
					System.out.println("Size:"
									+ BestPopularBidsList.size()
									+ ", Min:"
									+ utilitySpace.getUtilityWithDiscount(BestPopularBidsList.get(0),0.0)
									+ ", Max:"
									+ utilitySpace.getUtilityWithDiscount(BestPopularBidsList.get(BestPopularBidsList
											.size() - 1),0.0) + ", Opponents:"
									+ opponents);
				}
			}
		}
		
		public class UtilityComparator implements Comparator<Bid> {
			public int compare(Bid a, Bid b) {
				try {
					double u1 = utilitySpace.getUtilityWithDiscount(a,0.0);
					double u2 = utilitySpace.getUtilityWithDiscount(b,0.0);
					if (u1 < u2) {
						return 1;
					}
					if (u1 == u2) {
						return 0;
					}
					if (u1 > u2) {
						return -1;
					}
				} catch (Exception e) {
					System.out.println(" failed to sort based on the utility value ");
					e.printStackTrace();
				}
				return 0; // exception handling
			}
		}
		
		// If not a linear utility space
		public void utilitySpaceTypeisNonLinear() {
			isLinearUtilitySpace = false;
		}
		
		// Average
		public double getAverage(Object sender) {
			return opponentsAverage.get(sender);
		}

		// Dispersion
		public double getVariancer(Object sender) {
			return opponentsVariance.get(sender);
		}

		// standard deviation
		public double getStandardDeviation(Object sender) {
			return opponentsStandardDeviation.get(sender);
		}
		
		// Return the negotiator number
		public int getNegotiatorNum() {
			return negotiatorNum;
		}
		
		//It returns the number of round own
		public int getRound() {
			return round;
		}

		// Returns proposed time interval
		public double getTimeScale() {
			return time_scale;
		}
		
		// Returns the number of elements in the proposal history of opponent
		public int getOpponentBidNum(Object sender) {
			return opponentsBidHistory.get(sender).size();
		}
		
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		
		// Returns the relative utility matrix
		public HashMap<Issue, HashMap<Value, Double>> getValueRelativeUtility() {
			return valueRelativeUtility;
		}
		
		// Return whether it is a linear utility space
		public boolean isLinearUtilitySpace() {
			return isLinearUtilitySpace;
		}
		
		// Return the MaxPopularBidUtility
		public double getMPBU() {
			return MaxPopularBidUtility;
		}
		
		// Return the BestOfferedUtility
		public double getBOU() {
			return BestOfferedUtility;
		}
		
		// Return the negotiators entire BOBHistory
		public ArrayList<Bid> getBestOfferedBidHistory() {
			return BestOfferedBidHistory;
		}
		
		// Return the BestPopularBidsList
		public ArrayList<Bid> getBestPopularBidsList() {
			return BestPopularBidsList;
		}
		
		// Return the issue list
		public List<Issue> getIssues() {
			return issues;
		}
		
		// Return a list of negotiating partner
		public ArrayList<Object> getOpponents() {
			return opponents;
		}
		
		public double getMyWorstOfferedUtility(){
			return MyWorstOfferedUtility;
		}
		
		public HashMap<Object, Double> getopponentsWorstOfferUtil(){
			return opponentsWorstOfferUtil;
		}
		
		public HashMap<Object, Double> getopponentsBestOfferUtil(){
			return opponentsBestOfferUtil;
		}
		
		public HashMap<Object, Double> getopponentsBestOfferUtilWithoutLastOffer(){
			return opponentsBestOfferUtilWithoutLastOffer;
		}
		
		public HashMap<Object, Double> getopponentsH(){
			return opponentsH;
		}
		
		public double getMyH(){
			return myH;
		}
		
		public double getWeightFunction(){
			return WeightFunction;
		}
		
		public void WeightFunctionIsNotConstant(){
			ConstantWeightFunction = false;
		}
		
		//update the weight function
		public void updateWeightFunction(int ch, double normalizedTime){
			if(!ConstantWeightFunction){
				if(ch==0){ //for choice == 0 the weight function is a monotonically increasing function
					WeightFunction = normalizedTime;
				}else{ //for any other integer the weight function is a monotonically decreasing function
					WeightFunction = 1 - normalizedTime;
				}
			}
		}
		
		public  HashMap<Issue, HashMap<Value, Integer>> getOpponentValueFrequency(Object Sender){
			return opponentsValueFrequency.get(Sender);
		}
		
		//update the H of the opponent
		public void updateOpponentsH(Object opponent, double num){
			opponentsH.put(opponent,opponentsH.get(opponent)+num);
		}
		
		// Based on the frequency of opponentsValueFrequency and opponentsAcceptedValueFrequency, returns the most frequent value of this issue of this opponent
		public Value getValuebyFrequencyList(Object sender, Issue issue, int choice) {
			int current_f = 0;
			int max_f = 0; // number of occurrences of  most frequent element
			Value max_value = null; // The most frequent elements
			ArrayList<Value> randomOrderValues = getValues(issue);
			Collections.shuffle(randomOrderValues); // sort in random (when evaluated every time in the same order, because the number of occurrences is the return value is biased in the case of equivalence)

			for (Value value : randomOrderValues) {
				if(choice==0){
					current_f = opponentsValueFrequency.get(sender).get(issue).get(value);
				}else{
					current_f = opponentsAcceptedValueFrequency.get(sender).get(issue).get(value);
				}
				// Record the most frequent element
				if (max_value == null || current_f > max_f) {
					max_f = current_f;
					max_value = value;
				}
			}
			return max_value;
		}
		
		// Based on the frequency of opponentsValueFrequencyWeighted, returns the most frequent weighted value of this issue of this opponent
		public Value getValuebyFrequencyListWeighted(Object sender, Issue issue) {
			double current_f = 0.0;
			double max_f = 0.0; // number of occurrences of  most frequent element
			Value max_value = null; // The most frequent elements
			ArrayList<Value> randomOrderValues = getValues(issue);
			Collections.shuffle(randomOrderValues); // sort in random (when evaluated every time in the same order, because the number of occurrences is the return value is biased in the case of equivalence)

			for (Value value : randomOrderValues) {
				current_f = opponentsValueFrequencyWeighted.get(sender).get(issue).get(value);
				// Record the most frequent element
				if (max_value == null || current_f > max_f) {
					max_f = current_f;
					max_value = value;
				}
			}
			return max_value;
		}
		
		
		// Based on the frequency of FrequencyList of all minutes, returns elements
		public Value getValuebyAllFrequencyList(Issue issue) {
			int current_f = 0;
			int max_f = 0; // number of occurrences of most frequent element
			Value max_value = null; // The most frequent elements
			ArrayList<Value> randomOrderValues = getValues(issue);
			Collections.shuffle(randomOrderValues); // sort in random (when evaluated every time in the same order, because the number of occurrences is the return value is biased in the case of equivalence)

			for (Value value : randomOrderValues) {
				current_f = allValueFrequency.get(issue).get(value);
				// Record the most frequent element
				if (max_value == null || current_f > max_f) {
					max_f = current_f;
					max_value = value;
				}
			}
			return max_value;
		}
		
		
		public double getMarginalPerceivedChange(Object sender){
			//Double Opponents_best_offer_util = opponentsBestOfferUtil.get(sender);
			//Double Opponents_best_offer_util_without_last_offer = opponentsBestOfferUtilWithoutLastOffer.get(sender);
			//System.out.println("Opponents best offer util = "+Opponents_best_offer_util);
			//System.out.println("Opponents best offer util without last offer = "+Opponents_best_offer_util_without_last_offer);
			return (opponentsBestOfferUtil.get(sender)-opponentsBestOfferUtilWithoutLastOffer.get(sender));
		}
		
		
		//get the relative to our agent concession degree of opponent 
		public double getRelativeConcessionDegree(Object sender){
			//Double max_util_opponent_has_offered = opponentsBestOfferUtil.get(sender);
			//Double min_util_opponent_has_offered = opponentsWorstOfferUtil.get(sender);
			//System.out.println("max util opponent has offered = "+max_util_opponent_has_offered);
			//System.out.println("min util opponent has offered = "+min_util_opponent_has_offered);
			return ((opponentsBestOfferUtil.get(sender) - opponentsWorstOfferUtil.get(sender))-(1-MyWorstOfferedUtility));
		}
		
		
		public double getReactiveConcession(Object sender){
			//Double MarginalPerceivedChange = getMarginalPerceivedChange(sender);
			//Double RelativeConcessionDegree  = getRelativeConcessionDegree(sender);
			//System.out.println("Marginal Perceived Change = "+ MarginalPerceivedChange);
			//System.out.println("Relative Concession Degree = " +RelativeConcessionDegree);
			return Math.max(Math.max(getMarginalPerceivedChange(sender), getRelativeConcessionDegree(sender)), 0.0);
		}
		
		public double getEvaluationOfValue(Object sender,Issue issue, Value value){
			if(!ConstantWeightFunction){
				return opponentsValueFrequencyWeighted.get(sender).get(issue).get(value);
			}else{
				return opponentsValueFrequency.get(sender).get(issue).get(value);
			}
			
		}
		
		public HashMap<Issue, Value> getTheMaxValuesForOpponent(Object opponent){
			HashMap<Issue, Value> bestValuesForOpponent = new HashMap<Issue, Value>();
			for(Issue issue:issues){
				if(!ConstantWeightFunction){
					bestValuesForOpponent.put(issue,getValuebyFrequencyListWeighted(opponent,issue));
				}else{
					bestValuesForOpponent.put(issue,getValuebyFrequencyList(opponent,issue,0));
				}
			}
			return bestValuesForOpponent;
		}
		
}
