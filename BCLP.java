package main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

public class BCLP extends DynamicAlgorithm {

	private ArrayList<TreeMap<Integer, double[]>> MISs;
	private double[] thresholds;
	private Metric metric;
	private TreeMap<Integer, double[]> space;
	private TreeSet<Integer> oldSolution;
	private TreeSet<Integer> newSolution;
	private TreeMap<Integer, TreeSet<Integer>> orderedDisToCenters;

	private int k;
	private int depth;
	private double dMin;
	private double dMax;
	private double epsilon;
	private TreeMap<Integer, Double> permutation;
	private Comparator<Integer> order;

	public BCLP(int k, Metric metric) {
		this(k, metric, 1);
	}

	public BCLP(int k, Metric metric, double epsilon, double dMin, double dMax) {
		this.k = k;
		this.metric = metric;
		this.dMin = dMin;
		this.dMax = dMax;
		this.depth = (int) (Math.log(dMax / dMin) / Math.log(1 + epsilon)) + 3;
		this.epsilon = epsilon;
		initialize();
	}

	public BCLP(int k, Metric metric, double epsilon) {
		this.k = k;
		this.metric = metric;
		this.dMin = Double.MAX_VALUE;
		this.dMax = 0;
		this.depth = 2;
		this.epsilon = epsilon;
		initialize();
	}

	private void initialize() {
		space = new TreeMap<Integer, double[]>();
		permutation = new TreeMap<Integer, Double>();
		makeOrdering();
		oldSolution = new TreeSet<Integer>(order);
		orderedDisToCenters = new TreeMap<Integer, TreeSet<Integer>>();
		thresholds = new double[depth];
		thresholds[0] = dMin / (1 + epsilon);
		for (int i = 1; i < depth; i++)
			thresholds[i] = thresholds[i - 1] * (1 + epsilon);
		MISs = new ArrayList<TreeMap<Integer, double[]>>();
		for (int i = 0; i < depth; i++)
			MISs.add(new TreeMap<Integer, double[]>(order));
	}

	private void makeOrdering() {
		order = new Comparator<Integer>() {
			@Override
			public int compare(Integer c1, Integer c2) {
				if (c1.equals(c2))
					return 0;
				if (permutation.get(c1) < permutation.get(c2))
					return -1;
				return 1;
			}
		};
	}

	@Override
	public void insert(int key, double[] point) {
		if (space.containsKey(key)) {
			System.out.println("Warning: A point with this key already exists.");
			return;
		}
		checkForChangeInAspectRatio(point);
		permutation.put(key, Math.random());
		space.put(key, point);
		orderedDisToCenters.put(key, makeNewTree(point));
		add(key, point, 1);
		updateTrees();
	}

	private TreeSet<Integer> makeNewTree(double[] point) {
		Comparator<Integer> disToCenters = new Comparator<Integer>() {
			@Override
			public int compare(Integer c1, Integer c2) {
				if (c1.equals(c2))
					return 0;
				double d1 = metric.d(space.get(c1), point);
				double d2 = metric.d(space.get(c2), point);
				if (d1 < d2)
					return -1;
				else if (d1 > d2)
					return 1;
				return c1.compareTo(c2);
			}
		};
		TreeSet<Integer> sortedDis = new TreeSet<Integer>(disToCenters);
		for (Integer c : oldSolution)
			sortedDis.add(c);
		return sortedDis;
	}

	private void updateTrees() {
		newSolution = getCenters();
		removeOldCentersFromTrees();
		addNewCentersToTrees();
		oldSolution = newSolution;
	}

	private void removeOldCentersFromTrees() {
		Iterator<Integer> oldCenter = oldSolution.iterator();
		while (oldCenter.hasNext()) {
			int c = oldCenter.next();
			if (!newSolution.contains(c)) {
				Iterator<Integer> iter = space.keySet().iterator();
				while (iter.hasNext())
					orderedDisToCenters.get(iter.next()).remove(c);
			}
		}
	}

	private void addNewCentersToTrees() {
		Iterator<Integer> newCenter = newSolution.iterator();
		while (newCenter.hasNext()) {
			int c = newCenter.next();
			if (!oldSolution.contains(c)) {
				Iterator<Integer> iter = space.keySet().iterator();
				while (iter.hasNext())
					orderedDisToCenters.get(iter.next()).add(c);
			}
		}
	}

	private void add(int key, double[] point, int layer) {
		MISs.get(layer - 1).put(key, point);
		if (layer == depth || !shouldAdd(key, point, layer))
			return;
		add(key, point, layer + 1);
		checkForwardEdges(key, point, layer);
	}

	private void checkForwardEdges(int key, double[] point, int layer) {
		SortedMap<Integer, double[]> tail = MISs.get(layer).tailMap(key, false);
		for (Map.Entry<Integer, double[]> oldP : tail.entrySet()) {
			int oldKey = oldP.getKey();
			if (metric.d(point, oldP.getValue()) <= thresholds[layer]) {
				remove(oldKey, layer + 1);
				rebuild(layer, oldKey);
				return;
			}
		}
	}

	private boolean shouldAdd(int key, double[] point, int layer) {
		if (MISs.get(layer).containsKey(key))
			return false;
		return !hasBackwardEdge(key, point, layer);
	}

	private boolean hasBackwardEdge(int key, double[] point, int layer) {
		Iterator<Map.Entry<Integer, double[]>> iterator = MISs.get(layer).entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, double[]> oldP = iterator.next();
			if (oldP.getKey() == key || permutation.get(oldP.getKey()) > permutation.get(key))
				return false;
			else if (metric.d(point, oldP.getValue()) <= thresholds[layer])
				return true;
		}
		return false;
	}

	@Override
	public void delete(int key) {
		if (!space.containsKey(key)) {
			System.out.println("Warning: space does not contain a point with this key.");
			return;
		}
		remove(key, 1);
		updateTrees();
		space.remove(key);
		orderedDisToCenters.remove(key);
		permutation.remove(key);
	}

	private void remove(int key, int layer) {
		MISs.get(layer - 1).remove(key);
		if (layer == depth || !MISs.get(layer).containsKey(key))
			return;
		remove(key, layer + 1);
		rebuild(layer, key);
	}

	private void rebuild(int layer, int key) {
		SortedMap<Integer, double[]> tail = MISs.get(layer - 1).tailMap(key, false);
		ff: for (Map.Entry<Integer, double[]> newP : tail.entrySet()) {
			if (hasBackwardEdge(newP.getKey(), newP.getValue(), layer)) {
				if (MISs.get(layer).containsKey(newP.getKey()))
					remove(newP.getKey(), layer + 1);
				continue ff;
			}
			add(newP.getKey(), newP.getValue(), layer + 1);
		}
	}

	private void checkForChangeInAspectRatio(double[] newPoint) {
		double min = Double.MAX_VALUE, max = 0;
		Iterator<Map.Entry<Integer, double[]>> iterator = space.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, double[]> entry = iterator.next();
			double d = metric.d(entry.getValue(), newPoint);
			if (d > max)
				max = d;
			if (d > 0 && d < min)
				min = d;
		}
		if (min < dMin || max > dMax)
			reset(Math.min(min, dMin), Math.max(max, dMax));
	}

	private void reset(double min, double max) {
		dMin = min / Math.pow(1 + epsilon, 2);
		dMax = max * Math.pow(1 + epsilon, 2);
		depth = (int) (Math.log(dMax / dMin) / Math.log(1 + epsilon)) + 3;
		TreeMap<Integer, double[]> oldSpace = new TreeMap<Integer, double[]>(space);
		initialize();
		reInsertAllPoints(oldSpace);
	}

	private void reInsertAllPoints(TreeMap<Integer, double[]> oldSpace) {
		Iterator<Map.Entry<Integer, double[]>> iterator = oldSpace.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, double[]> entry = iterator.next();
			this.insert(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public TreeMap<Integer, Integer> cluster() {
		TreeMap<Integer, Integer> clusters = new TreeMap<Integer, Integer>();
		Iterator<Map.Entry<Integer, TreeSet<Integer>>> iter = orderedDisToCenters.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<Integer, TreeSet<Integer>> tree = iter.next();
			clusters.put(tree.getKey(), tree.getValue().first());
		}
		return clusters;
	}

	public int depth() {
		return depth;
	}

	@Override
	public String name() {
		return "BCLP(k=" + k + ")";
	}

	@Override
	public void printStatus() {
		System.out.println("--------------------Status--------------------");
		System.out.println("n: " + MISs.get(0).size() + ", k: " + k + ", depth: " + depth);
		System.out.println("min dis: " + dMin + ", max dis: " + dMax);
		System.out.println("Number of centers in each layer: ");
		int[] num = new int[depth];
		for (int i = 0; i < num.length; i++) {
			System.out.println("This is layer " + i + ":");
			System.out.print("[");
			for (Map.Entry<Integer, double[]> entry : MISs.get(i).entrySet()) {
				System.out.print(entry.getKey() + ", ");
			}
			System.out.println("]");
			num[i] = MISs.get(i).size();
		}
		System.out.println(Arrays.toString(num));
		System.out.println("current solution:");
		System.out.print("[");
		Iterator<Integer> iter = getCenters().iterator();
		while (iter.hasNext()) {
			int center = iter.next();
			if (iter.hasNext())
				System.out.print(center + ", ");
			else
				System.out.print(center + "]");
		}
		System.out.println("----------------------------------------------");
	}

	@Override
	public TreeSet<Integer> getCenters() {
		int iStar;
		for (iStar = 0; iStar < depth - 1; iStar++)
			if (MISs.get(iStar).size() <= k)
				break;
		if (iStar == 0)
			return new TreeSet<Integer>(MISs.get(0).keySet());
		TreeSet<Integer> solution = new TreeSet<Integer>(MISs.get(iStar).keySet());
		TreeSet<Integer> diff = new TreeSet<Integer>(order);
		for (Integer e : MISs.get(iStar - 1).keySet())
			if (!solution.contains(e))
				diff.add(e);
		Iterator<Integer> iter = diff.iterator();
		while (solution.size() < k)
			solution.add(iter.next());
		return solution;
	}

	@Override
	public boolean staticTest(TreeMap<Integer, double[]> activPoints) {
		if (!MISs.get(0).equals(activPoints))
			return false;
		ArrayList<TreeMap<Integer, double[]>> all = new ArrayList<TreeMap<Integer, double[]>>();
		double threshold = dMin;
		all.add(activPoints);
		int i = 1;
		while (threshold <= dMax) {
			TreeMap<Integer, double[]> newSet = new TreeMap<Integer, double[]>();
			TreeMap<Integer, double[]> oldSet = all.get(all.size() - 1);
			Iterator<Map.Entry<Integer, double[]>> iterOld = oldSet.entrySet().iterator();
			f: while (iterOld.hasNext()) {
				Map.Entry<Integer, double[]> newP = iterOld.next();
				Iterator<Map.Entry<Integer, double[]>> iterNew = newSet.entrySet().iterator();
				while (iterNew.hasNext()) {
					Map.Entry<Integer, double[]> oldP = iterNew.next();
					if (metric.d(oldP.getValue(), newP.getValue()) <= threshold)
						continue f;
				}
				newSet.put(newP.getKey(), newP.getValue());
			}
			if (!MISs.get(i).equals(newSet)) {
				return false;
			}
			all.add(newSet);
			i++;
			threshold *= (1 + epsilon);
		}
		return true;
	}

}
