package tileworld;

/**
 * Parameters
 *
 * @author michaellees
 * Created: Apr 21, 2010
 *
 * Copyright michaellees 
 *
 * Description:
 *
 * Class used to store global simulation parameters.
 * Environment related parameters are still in the TWEnvironment class.
 *
 */
public class Parameters {

    //Simulation Parameters
    public final static int seed = 9042014; //no effect with gui
    public static final long endTime = 5000; //no effect with gui

    //Agent Parameters
    public static final int defaultFuelLevel = 1000;
    public static final int defaultSensorRange = 3;

//    //Environment Parameters for ENV 3
//    public static final int xDimension = 150; //size in cells
//    public static final int yDimension = 40;
//
//    //Object Parameters
//    public static final double tileMean = 0.02;
//    public static final double holeMean = 0.2;
//    public static final double obstacleMean = 0.5;
//    public static final double tileDev = 0.001f;
//    public static final double holeDev = 0.01f;
//    public static final double obstacleDev = 0.1f;
//    public static final int lifeTime = 120;
//  
   
    //Environment Parameters for ENV 2
    public static final int xDimension = 50; //size in cells
    public static final int yDimension = 50;

    //Object Parameters
    public static final double tileMean = 2;
    public static final double holeMean = 2;
    public static final double obstacleMean = 2;
    public static final double tileDev = 0.5f;
    public static final double holeDev = 0.5f;
    public static final double obstacleDev = 0.5f;
    public static final int lifeTime = 30;

}

