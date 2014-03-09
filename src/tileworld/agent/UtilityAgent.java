/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tileworld.agent;

import java.util.HashMap;
import java.util.LinkedList;

import practicalreasoning.Intention;
import practicalreasoning.IntentionType;
import practicalreasoning.TWPlan;
import practicalreasoning.Utility;
import sim.util.Int2D;
import tileworld.environment.TWDirection;
import tileworld.environment.TWEnvironment;
import tileworld.environment.TWHole;
import tileworld.environment.TWTile;
import tileworld.exceptions.CellBlockedException;
import tileworld.planners.AstarPathGenerator;
import tileworld.planners.TWPath;
import tileworld.planners.TWPathStep;

/**
 * TWContextBuilder
 *
 * @author michaellees
 * Created: Feb 6, 2011
 *
 * Copyright michaellees Expression year is undefined on line 16, column 24 in Templates/Classes/Class.java.
 *
 *
 * Description:
 *
 */
public class UtilityAgent extends TWAgent{
	public static double exploreThreshold = 10;
	private TWPlan currentPlan = null;
	private Intention currIntention = null;
	private AstarPathGenerator pathGenerator;

	public UtilityAgent(int xpos, int ypos, TWEnvironment env, double fuelLevel) {
		super(xpos,ypos,env,fuelLevel);
		pathGenerator = new AstarPathGenerator(getEnvironment(), this, Integer.MAX_VALUE);
	}
	protected TWThought think() {
		//        getMemory().getClosestObjectInSensorRange(Tile.class);
		//		System.out.println("Simple Score: " + this.score);
		if(currentPlan == null || !currentPlan.hasNext() || impossible(currentPlan))
		{
			HashMap<IntentionType, Double> utilities = options();
			currIntention = filter(utilities);	
			currentPlan = plan(currIntention);
		}
		else
		{
			Intention newIntention = null;
			if(reconsider(currentPlan))
			{
				HashMap<IntentionType, Double> utilities = options();
				newIntention = filter(utilities);	
			}
			if(!sound(currentPlan, newIntention))				
				currentPlan = plan(newIntention);
		}
		return currentPlan.next();

		////		TWEntity current = (TWEntity) getMemory().getObjects().get(x,  y);		
		//		if(carriedTiles.size() < 3 & current instanceof TWTile)
		//			return new TWThought(TWAction.PICKUP, null);
		//		else if(hasTile() && current instanceof TWHole)
		//			return new TWThought(TWAction.PUTDOWN, null);
		//		else
		//			return new TWThought(TWAction.MOVE,getRandomDirection());
	}

	private boolean sound(TWPlan currentPlan2, Intention newIntention) {
		if(newIntention == null)
			return false;		
		return !newIntention.equals(currIntention);	
	}
	private boolean reconsider(TWPlan currentPlan2) {
		// TODO Auto-generated method stub
		return true;
	}
	private boolean impossible(TWPlan currentPlan2) {
		// TODO Auto-generated method stub
		return false;
	}
	private TWPlan plan(Intention intention) {
		TWPath path = pathGenerator.findPath(getX(), getY(), intention.getLocation().x, intention.getLocation().y);
		LinkedList<TWThought> thoughts = new LinkedList<TWThought>();
		if(path != null)
		{
			for(TWPathStep pathStep: path.getpath())
			{
				thoughts.add(new TWThought(TWAction.MOVE, pathStep.getDirection()));
			}
		}
		switch(intention.getIntentionType())
		{
		case EXPLORE:
			break;
		case FILLHOLE:
			thoughts.add(new TWThought(TWAction.PUTDOWN, null));
			break;
		case PICKUPTILE:
			thoughts.add(new TWThought(TWAction.PICKUP, null));
			break;
		case REFUEL:
			thoughts.add(new TWThought(TWAction.REFUEL, null));
			break;
		}
		return new TWPlan(thoughts);
	}
	private Intention filter(HashMap<IntentionType, Double> utilities) {
		boolean explore = true;
		for(Double value: utilities.values())
		{
			if(value > exploreThreshold)
				explore = false;
		}
		if (explore)
		{
			Int2D location = getEnvironment().generateFarRandomLocation(getX(), getY(), 
							(getEnvironment().getxDimension() + getEnvironment().getyDimension()) / 2);
			return new Intention(IntentionType.EXPLORE, location);
		}

		if(utilities.get(IntentionType.REFUEL) >= utilities.get(IntentionType.PICKUPTILE) && 
				utilities.get(IntentionType.REFUEL) >= utilities.get(IntentionType.FILLHOLE))
		{
			return new Intention(IntentionType.REFUEL, getEnvironment().getFuelingStation().getX(), 
					getEnvironment().getFuelingStation().getY());
		}
		if(utilities.get(IntentionType.PICKUPTILE) > utilities.get(IntentionType.FILLHOLE))
		{
			return new Intention(IntentionType.PICKUPTILE, Utility.selectedTile.getX(),
					Utility.selectedTile.getY());
		}
		return new Intention(IntentionType.FILLHOLE, Utility.selectedHole.getX(),
				Utility.selectedHole.getY());
	}

	private HashMap<IntentionType, Double> options() {
		HashMap<IntentionType, Double> utilities = new HashMap<IntentionType, Double>();
		Utility.updateNeighbourBasedUtility(this, getEnvironment());
		utilities.put(IntentionType.REFUEL, Utility.fueling(this, getEnvironment()));
		utilities.put(IntentionType.PICKUPTILE, Utility.pickUpTile(this, getEnvironment()));
		utilities.put(IntentionType.FILLHOLE, Utility.putInHole(this, getEnvironment()));
		return utilities;
	}

	@Override
	protected void act(TWThought thought) {

		//You can do:
		//move(thought.getDirection())
		//pickUpTile(Tile)
		//putTileInHole(Hole)
		//refuel()

		try {
			switch(thought.getAction())
			{
			case MOVE:
				this.move(thought.getDirection());
				break;
			case PICKUP:
				TWTile tile = (TWTile)getEnvironment().getObjectGrid().get(x, y);
				this.pickUpTile(tile);
				this.getMemory().removeObject(tile);
				break;
			case PUTDOWN:
				TWHole hole = (TWHole)getEnvironment().getObjectGrid().get(x, y);
				this.putTileInHole(hole);
				this.getMemory().removeObject(hole);
			case REFUEL:
				break;
			default:
				break;			
			}
		}  catch (CellBlockedException ex) {

			// Cell is blocked, replan?
		}
	}


	private TWDirection getRandomDirection(){

		TWDirection randomDir = TWDirection.values()[this.getEnvironment().random.nextInt(5)];

		if(this.getX()>=this.getEnvironment().getxDimension() ){
			randomDir = TWDirection.W;
		}else if(this.getX()<=1 ){
			randomDir = TWDirection.E;
		}else if(this.getY()<=1 ){
			randomDir = TWDirection.S;
		}else if(this.getY()>=this.getEnvironment().getxDimension() ){
			randomDir = TWDirection.N;
		}

		return randomDir;

	}

	@Override
	public String getName() {
		return "Dumb Agent";
	}
}
