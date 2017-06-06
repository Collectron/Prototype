package ANAC2017;

import java.util.List;

import negotiator.AgentID;
import negotiator.Bid;
import negotiator.Deadline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.EndNegotiation;
import negotiator.actions.Inform;
import negotiator.actions.Offer;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.timeline.TimeLineInfo;
import negotiator.utility.AbstractUtilitySpace;
import negotiator.persistent.PersistentDataContainer;

/**
 * This is your negotiation party.
 */
public class Prototype extends AbstractNegotiationParty {

	private AbstractUtilitySpace utilitySpace; // utility space
	private NegotiationStatistics Information; //information about the negotiation procedure
	private BidStrategy BidStrat; //how an agent chooses a bid
	private Bid bidtoffer = null;
	public static boolean isPrinting = false;
	double resValue = 1.0;
	int supporter_num = 0;
	private Bid offeredBid = null;
	
	
	@Override
	public void init(AbstractUtilitySpace utilSpace, Deadline dl,
			TimeLineInfo tl, long randomSeed, AgentID agentId, PersistentDataContainer storage) {
			//TimeLineInfo tl, long randomSeed, AgentID agentId) {
		
		super.init(utilSpace, dl, tl, randomSeed, agentId, storage);
		//super.init(utilSpace, dl, tl, randomSeed, agentId);
		
		System.out.println("Discount Factor is "
				+ utilSpace.getDiscountFactor());
		System.out.println("Reservation Value is "
				+ utilSpace.getReservationValueUndiscounted());

		// if you need to initialize some variables, please initialize them
		// below
		utilitySpace = utilSpace;
		resValue = utilSpace.getReservationValue();
		
		Information = new NegotiationStatistics(utilitySpace);
		try{
			BidStrat = new BidStrategy(utilitySpace, Information, tl.getTime());
		}catch(Exception e){
			System.out.println(" Failed to initiate the bidding strategy ");
			e.printStackTrace();
		}
		
	}

	/**
	 * Each round this method gets called and ask you to accept or offer. The
	 * first party in the first round is a bit different, it can only propose an
	 * offer.
	 *
	 * @param validActions
	 *            Either a list containing both accept and offer or only offer.
	 * @return The chosen action.
	 */
	@Override
	public Action chooseAction(List<Class<? extends Action>> validActions){
		double time = getTimeLine().getTime();
		Information.updateTimeScale(time);
		
		
		Double targetTime = BidStrat.targetTime(time);
//		Double targetTime = 1.0;
//		try{
//			targetTime = BidStrat.getNewThreshold(time);
//		}catch(Exception e){
//			System.out.println("Failed in choosing the new Threshold");
//			e.printStackTrace();
//		}
		
		System.out.println(" Threshold = "+ targetTime);
		try{
			//bidtoffer = new Bid(BidStrat.getBid(utilitySpace.getDomain().getRandomBid(null), targetTime, time));
			bidtoffer = new Bid(BidStrat.SimulatedAnnealingSearchForUop(utilitySpace.getDomain().getRandomBid(null), targetTime, time));
		}catch(Exception e){
			System.out.println(" This test failed ");
			e.printStackTrace();
		}
		
		if(Information.getRound()<=1){
			Information.updateMyBidHistory(BidStrat.maxBid);
			return new Offer(getPartyId(), BidStrat.maxBid);
		}
		else{
			if(utilitySpace.getUtilityWithDiscount(offeredBid, time) >= targetTime){
				return new Accept(getPartyId(), offeredBid);
			}
			Information.updateMyBidHistory(bidtoffer);
			return new Offer(getPartyId(), bidtoffer);
		}
	}

	/**
	 * All offers proposed by the other parties will be received as a message.
	 * You can use this information to your advantage, for example to predict
	 * their utility.
	 *
	 * @param sender
	 *            The party that did the action. Can be null.
	 * @param action
	 *            The action that party did.
	 */
	@Override
	public void receiveMessage(AgentID sender, Action action) {
		super.receiveMessage(sender, action);
		double time = getTimeLine().getTime();
		if (action != null){
			if (action instanceof Inform && ((Inform) action).getName() == "NumberOfAgents" && ((Inform) action).getValue() instanceof Integer) {
				Integer opponentsNum = (Integer) ((Inform) action).getValue();
				Information.updateOpponentsNum(opponentsNum);
				
			}
			else if (action instanceof Accept){
				if (!Information.getOpponents().contains(sender)){
					Information.initOpponent(sender);
				} // First appearance of the negotiators initialization
				try {
					Information.updateAcceptanceHistory(sender, offeredBid);
				} catch (Exception e) {
					System.out.println("Received Action Accept Failed Miserably");
					e.printStackTrace();
				}
				supporter_num++;
			} 
			else if (action instanceof Offer){
				if (!Information.getOpponents().contains(sender)){
					Information.initOpponent(sender);
				} // First appearance of the negotiators initialization
				supporter_num =  1 ;// reset the // supporter
				offeredBid = ((Offer)action).getBid (); // the proposed draft agreement candidate
				try {
					Information.updateInfo(sender, offeredBid, time);
				} // Update the negotiation information
				catch (Exception e) {
					System . out . println ( " failed to update the negotiation information " );
					e.printStackTrace();
				}
			} 
			else if (action instanceof EndNegotiation) {
			}
			// Regarded as one of the negotiators of the draft agreement candidate record (other than their own agent other than itself is in favor of. The first place as long as it is a non-cooperation other than the agent are two or more people own, agreement, regardless of the choice of own is impossible)
			if (supporter_num == Information.getNegotiatorNum()-1) {
				if (offeredBid != null) {
					try {
						Information.updatePopularBidList(offeredBid);
					} catch (Exception e) {
						System . out . println ( " PBList update failed of " ); // update the PopularBidHistory
						e.printStackTrace();
					}
				}
			}
		}
	}

	@Override
	public String getDescription() {
		return "Prototype";
	}

	
}
