package main;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class BEFHJMW extends DynamicAlgorithm {

	private ArrayList<ThresholdGraph> LFMISs;
	private double[] thresholds;
	private Metric metric;
	private TreeMap<Integer, double[]> space;

	private int k;
	private int depth;
	private double dMin;
	private double dMax;
	private double epsilon;
	TreeMap<Integer, Double> permutation;

	public BEFHJMW(int k, Metric metric) {
		this(k, metric, 1);
	}

	public BEFHJMW(int k, Metric metric, double epsilon, double dMin, double dMax) {
		this.k = k;
		this.metric = metric;
		this.dMin = dMin;
		this.dMax = dMax;
		this.depth = (int) (Math.log(dMax / dMin) / Math.log(1 + epsilon)) + 3;
		this.epsilon = epsilon;
		initialize();
	}

	public BEFHJMW(int k, Metric metric, double epsilon) {
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
		thresholds = new double[depth];
		thresholds[0] = dMin / (1 + epsilon);
		for (int i = 1; i < depth; i++)
			thresholds[i] = thresholds[i - 1] * (1 + epsilon);
		LFMISs = new ArrayList<ThresholdGraph>();
		for (int i = 0; i < depth; i++)
			LFMISs.add(new ThresholdGraph(space, metric, thresholds[i], k, permutation));
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
		for (ThresholdGraph layer : LFMISs)
			layer.insert(key, point);
	}

	@Override
	public void delete(int key) {
		if (!space.containsKey(key)) {
			System.out.println("Warning: space does not contain a point with this key.");
			return;
		}

		for (ThresholdGraph layer : LFMISs)
			layer.delete(key);
		space.remove(key);
		permutation.remove(key);
	}

	@Override
	public TreeMap<Integer, Integer> cluster() {
		int iStar;
		for (iStar = 0; iStar < depth - 1; iStar++)
			if (LFMISs.get(iStar).size() <= k)
				break;
		return new TreeMap<Integer, Integer>(LFMISs.get(iStar).followerMap);
	}

	@Override
	public String name() {
		return "BEFHJMW(k=" + k + ")";
	}

	@Override
	public void printStatus() {
		System.out.println("-------------------------------------Status-------------------------------------");
		System.out.println("Space: ");
		System.out.println(space.toString());
		System.out.println("Depth: " + depth);
		System.out.println("Permutation for ordering: " + permutation);
		System.out.println("Threhsold graphs:");
		for (ThresholdGraph g: LFMISs) {
			System.out.println("Graph with threshold " + g.threshold + ":");
			g.printStatus();
		}
		System.out.println("--------------------------------------------------------------------------------");
	}

	@Override
	public boolean staticTest(TreeMap<Integer, double[]> activPoints) {
		return true;
	}

	@Override
	public TreeSet<Integer> getCenters() {
		int iStar;
		for (iStar = 0; iStar < depth - 1; iStar++)
			if (LFMISs.get(iStar).size() <= k)
				break;
		TreeSet<Integer> solution = new TreeSet<Integer>();
		for (Integer e : LFMISs.get(iStar).I)
			solution.add(e);
		return solution;
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

}

class ThresholdGraph {

	int k;
	TreeMap<Integer, Integer> followerMap;
	Metric metric;
	double threshold;
	TreeSet<Integer> I;
	TreeMap<Integer, Double> permutation;
	TreeMap<Integer, double[]> space;
	TreeSet<Integer> unclustered;
	Comparator<Integer> order;
	TreeMap<Integer, TreeSet<Integer>> leaders;

	public ThresholdGraph(TreeMap<Integer, double[]> space, Metric metric, double threshold, int k,
			TreeMap<Integer, Double> permutation) {
		this.space = space;
		this.threshold = threshold;
		this.permutation = permutation;
		this.metric = metric;
		this.k = k;
		followerMap = new TreeMap<Integer, Integer>();
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
		unclustered = new TreeSet<Integer>(order);
		I = new TreeSet<Integer>(order);
		leaders = new TreeMap<Integer, TreeSet<Integer>>();
	}

	public int size() {
		return I.size();
	}

	public void delete(int key) {
		remove(key);
		updateQ();
	}

	public void printStatus() {
		System.out.println("------------------------------------------------------------------------");
		System.out.print("I: ");
		System.out.println(I.toString());
		System.out.print("Follower Map: ");
		System.out.println(followerMap.toString());
		System.out.print("Unclustered: ");
		System.out.println(unclustered.toString());
		System.out.print("Leader: ");
		System.out.println(leaders.toString());
		System.out.print("Permutation: ");
		System.out.println(permutation.toString());
		System.out.println("------------------------------------------------------------------------");
	}

	public void insert(int key, double[] point) {
		add(key);
		updateQ();
	}

	private void updateQ() {
		while (!unclustered.isEmpty()) {
			int u = unclustered.first();
			if (I.size() <= k || permutation.get(u) < permutation.get(I.last())) {
				unclustered.remove(u);
				add(u);
			} else {
				return;
			}
		}
	}

	private void add(int v) {
		if (I.size() == k + 1 && permutation.get(v) > permutation.get(I.last())) {
			unclustered.add(v);
			return;
		}
		TreeSet<Integer> S = findNeighborsInI(v);
		if (S.isEmpty()) {
			addToI(v);
			return;
		}
		int uStar = S.first();
		if (permutation.get(uStar) < permutation.get(v)) {
			if (leaders.containsKey(v)) {
				for (int e : leaders.get(v)) {
					unclustered.add(e);
					followerMap.remove(e);
				}
				leaders.remove(v);
			}
			leaders.get(uStar).add(v);
			followerMap.put(v, uStar);
			return;
		}

		I.add(v);
		if (leaders.get(v) == null)
			leaders.put(v, new TreeSet<Integer>());

		for (int s : S) {
			for (int w : leaders.get(s)) {
				unclustered.add(w);
				followerMap.remove(w);
			}
			followerMap.put(s, v);
			leaders.get(v).add(s);
			leaders.remove(s);
			I.remove(s);
		}
	}

	private void addToI(int v) {
		I.add(v);
		if (leaders.get(v) == null)
			leaders.put(v, new TreeSet<Integer>());
		if (I.size() == k + 2) {
			int u = I.last();
			I.remove(u);
			unclustered.add(u);
		}
	}

	private TreeSet<Integer> findNeighborsInI(int v) {
		TreeSet<Integer> S = new TreeSet<Integer>(order);
		Iterator<Integer> iter = I.iterator();
		while (iter.hasNext()) {
			int u = iter.next();
			if (metric.d(space.get(u), space.get(v)) <= threshold)
				S.add(u);
		}
		return S;
	}

	private void remove(int v) {
		if (followerMap.containsKey(v)) {
			leaders.get(followerMap.get(v)).remove(v);
			followerMap.remove(v);
			return;
		} else if (unclustered.contains(v)) {
			removeUnclustered(v);
			return;
		} else {
			removeLeader(v);
		}
	}

	private void removeLeader(int v) {
		for (int w : leaders.get(v)) {
			unclustered.add(w);
			followerMap.remove(w);
		}
		leaders.remove(v);
		I.remove(v);
	}

	private void removeUnclustered(int v) {
		if (leaders.containsKey(v)) {
			for (int w : leaders.get(v)) {
				unclustered.add(w);
				followerMap.remove(w);
			}
			leaders.remove(v);
		}
		unclustered.remove(v);
	}

}
