package main;

import java.util.TreeMap;
import java.util.TreeSet;

// and abstract class for a dynamic clustering algorithm

public abstract class DynamicAlgorithm {

	// insert a point
	public abstract void insert(int key, double[] point);

	// delete a point
	public abstract void delete(int key);

	// cluster the points and return the solution as a treemap
	public abstract TreeMap<Integer, Integer> cluster();

	// returns the open centers at any time
	public abstract TreeSet<Integer> getCenters();
	
	// returns the name of the algorithm
	public abstract String name();

	// prints the status of the algorithm just for record if needed
	public abstract void printStatus();

	// any kind of test that you would like to have for your algorithm
	// you can just return true without doing anything if the code is correct
	public abstract boolean staticTest(TreeMap<Integer, double[]> activPoints);
	

}
