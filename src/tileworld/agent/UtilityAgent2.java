

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tileworld.agent;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;

import practicalreasoning.Intention;
import practicalreasoning.IntentionType;
import practicalreasoning.TWPlan;
import practicalreasoning.Utility;
import practicalreasoning.UtilityParams;

import sim.util.Int2D;
import tileworld.Parameters;
import tileworld.environment.TWDirection;
import tileworld.environment.TWEntity;
import tileworld.environment.TWEnvironment;
import tileworld.environment.TWHole;
import tileworld.environment.TWObject;
import tileworld.environment.TWObstacle;
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

public class UtilityAgent2 extends TWAgent{
	private static final long serialVersionUID = 1L;
	private HashMap<String, Double> parameters;
	private PriorityQueue<TWHole> holes;
	private PriorityQueue<TWTile> tiles;
	private TWPlan currentPlan = null;
	private Intention currIntention = null;
	private AstarPathGenerator pathGenerator;
	//private FuelPathGenerator fuelPathGen;
	//private ReactivePathGenerator reactivePathGen;
	private String name;
	public static double exploreThreshold = 10;
	public UtilityAgent2(int xpos, int ypos, TWEnvironment env, double fuelLevel, HashMap<String, Double> parameters) {
		super(xpos,ypos,env,fuelLevel);
		pathGenerator = new AstarPathGenerator(env, this, Integer.MAX_VALUE);
		//fuelPathGen = new FuelPathGenerator();
		//reactivePathGen = new ReactivePathGenerator();
		this.parameters = parameters;
	}

	protected TWThought think() {
		//High prio reactive
		TWEntity current = (TWEntity) getMemory().getObjectAt(x, y);		
		if(carriedTiles.size() < 3 & current instanceof TWTile)
			return new TWThought(TWAction.PICKUP, null);
		if(hasTile() && current instanceof TWHole)
			return new TWThought(TWAction.PUTDOWN, null);

		//Start of rational part

		Utility(this,getEnvironment());
		if(impossible(currentPlan))
		{
			HashMap<IntentionType, Double> utilities = options();
			currIntention = filter(utilities);	
			currentPlan = plan(currIntention);
		}
		{
			Intention newIntention = null;
			if(reconsider(currentPlan))
			{
				HashMap<IntentionType, Double> utilities = options();
				newIntention = filter(utilities);	
			}
			if(!sound(currentPlan, newIntention))	
			{
				currentPlan = plan(newIntention);
				currIntention = newIntention;
			}
		}
		System.out.print(name + " ");
		System.out.println(currIntention);
		System.out.println(currentPlan.peek());
		return currentPlan.next();
	}

	private boolean reconsider(TWPlan currentPlan2) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean sound(TWPlan currentPlan2, Intention newIntention) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void act(TWThought thought) {

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

	public HashMap<String, Double> getParameters(){
		return this.parameters;
	}
	
	private boolean impossible(TWPlan currentPlan) {
		return (currentPlan == null || currentPlan.peek() == null || !currentPlan.hasNext());
		}

	public double fueling(TWAgent agent, TWEnvironment environment)
	{
		double fuelLevel = agent.getFuelLevel();
		if((Parameters.endTime - environment.schedule.getTime()) <= fuelLevel)
			return 0;
		double bufferFuel = (environment.getxDimension() + environment.getyDimension()) * parameters.get("bufferRatio");
		double distance = agent.getDistanceTo(environment.getFuelingStation());
		distance += bufferFuel; 

		//reactive
		if (fuelLevel < distance)
			return 99.99;
		return distance / fuelLevel * 100;
	}
	public void Utility(TWAgent agent, TWEnvironment environment)
	{	
		this.holes = new PriorityQueue<TWHole>();
		this.tiles = new PriorityQueue<TWTile>();
		int x = environment.getxDimension();
		int y = environment.getyDimension();
		Double[][] utilities = new Double[x][y];
		int maxDistance = x + y;
		for(int i = 0; i < x; i++)
		{
			for(int j = 0; j < y; j++)
			{
				TWAgentPercept percept = agent.getMemory().getPerceptAt(i, j);
				if(percept == null || percept.getO() instanceof TWObstacle)
					continue;
				double distance = agent.getDistanceTo(percept.getO());		
				double howOld = environment.schedule.getTime() - percept.getT();
				double decayMultiplier = normalDistribution(1, 0, parameters.get("deviationMemoryDecay"), howOld);
				utilities[i][j] = normalDistribution(100, 0, parameters.get("deviationTiles"), (distance/maxDistance)) * decayMultiplier;
			}
		}

		for(int i = 0; i < x; i++)
		{
			for(int j = 0; j < y; j++)
			{
				if(utilities[i][j] == null)
					continue;
				for(int k = i - parameters.get(UtilityParams.NEIGHBOUR_SEARCH_LIMIT_X).intValue(); k < i + parameters.get("XSearch"); k++)
				{
					for(int l = j - parameters.get("YSearch").intValue(); l < j + parameters.get("YSearch"); l++)
					{
						if(!environment.isValidLocation(x, y) || utilities[k][l] == null)
							continue;
						double distance = environment.getDistance(i, j, k, l);
						double distScore = normalDistribution(utilities[k][l], 0, parameters.get("deviationNeighbour"), (distance + utilities[k][l])/ (maxDistance+ utilities[k][l]));
						utilities[i][j] = combinationFunction(utilities[i][j], distScore);
					}
				}
				TWObject currObj = (TWObject) agent.getMemory().getObjectAt(i, j);
				currObj.utility = utilities[i][j];
				if(currObj instanceof TWTile)
					tiles.add((TWTile) currObj);
				else
					holes.add((TWHole) currObj);

			}
		}
	}

	public double pickUpTile(TWAgent agent, TWEnvironment environment)
	{
		if (this.tiles.peek() == null)
		{
			return 0;
		}
		switch(agent.numberOfCarriedTiles())
		{
		case 0:
			return tiles.peek().utility * parameters.get(UtilityParams.PICKUP_ZERO_TILES);
		case 1:
			return tiles.peek().utility * parameters.get(UtilityParams.PICKUP_ONE_TILES);
		case 2:
			return tiles.peek().utility * parameters.get(UtilityParams.PICKUP_TWO_TILES);
		default:
			return 0;
		}
	}

	public double putInHole(TWAgent agent, TWEnvironment environment)
	{
		if (this.holes.peek() == null)
		{
			return 0;
		}
		switch(agent.numberOfCarriedTiles())
		{
		case 3:
			return holes.peek().utility * parameters.get(UtilityParams.PICKUP_THREE_HOLES);
		case 2:
			return holes.peek().utility * parameters.get(UtilityParams.PICKUP_TWO_HOLES);
		case 1:
			return holes.peek().utility * parameters.get(UtilityParams.PICKUP_ONE_HOLES);
		default:
			return 0;
		}
	}

	public TWTile getSelectedTile()
	{
		return tiles.poll();
	}

	public TWHole getSelectedHole()
	{
		return holes.poll();
	}

	private TWPlan plan(Intention intention) {
		LinkedList<TWThought> thoughts = new LinkedList<TWThought>();
		TWPath path = pathGenerator.findPath(getX(), getY(), intention.getLocation().x, intention.getLocation().y);
		while(path == null)
			
			//
			//
			//This is where we should use different pathgenerators based on which intention we have.
			//And make sure that we don't return null values.
			//
		{
			switch(intention.getIntentionType())
			{
			case EXPLORE:
				intention.setLocation(randomLocation());
				break;
			case FILLHOLE:
				HashMap<IntentionType, Double> utilities = options();
				currIntention = filter(utilities);	
				break;
			case PICKUPTILE:
				break;
			case REFUEL:
				break;
			}
			path = pathGenerator.findPath(getX(), getY(), intention.getLocation().x, intention.getLocation().y);
		}
		for(TWPathStep pathStep: path.getpath())
		{
			thoughts.add(new TWThought(TWAction.MOVE, pathStep.getDirection()));
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
			Int2D location = randomLocation();
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
			TWTile selected = Utility.getSelectedTile();
			return new Intention(IntentionType.PICKUPTILE, selected.getX(),
					selected.getY());
		}
		TWHole selected  = Utility.getSelectedHole();
		return new Intention(IntentionType.FILLHOLE, selected.getX(),
				selected.getY());
	}

	private HashMap<IntentionType, Double> options() {
		HashMap<IntentionType, Double> utilities = new HashMap<IntentionType, Double>();
		utilities.put(IntentionType.REFUEL, Utility.fueling(this, getEnvironment()));
		utilities.put(IntentionType.PICKUPTILE, Utility.pickUpTile(this, getEnvironment()));
		utilities.put(IntentionType.FILLHOLE, Utility.putInHole(this, getEnvironment()));
		return utilities;
	}
	private Int2D randomLocation() {
		Int2D location = getEnvironment().generateFarRandomLocation(getX(), getY(), 
				(getEnvironment().getxDimension() + getEnvironment().getyDimension()) / 2);
		while(getMemory().isCellBlocked(location.x, location.y))
			location = getEnvironment().generateFarRandomLocation(getX(), getY(), 
					(getEnvironment().getxDimension() + getEnvironment().getyDimension()) / 2);
		return location;
	}

	private double normalDistribution(double a, double b, double c, double x)
	{
		return a*Math.exp(-1*(Math.pow(x - b, 2)) / (2 * Math.pow(c, 2)));
	}

	public  double combinationFunction (double x, double y) {
		double result = Math.pow(Math.tanh(atanh(Math.pow(x, 5)) + atanh(Math.pow(y, 5))),1/5);
		return result;
	}
	private  double atanh(double x) {
		double result = 0.5 *(Math.log(1+x)-Math.log(1-x));
		return result;
	}
}



