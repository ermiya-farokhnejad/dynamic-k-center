package main;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class Test {
	// 'census' input path
	private static String census = "census";

	// 'song' input path
	private static String song = "song";

	// 'kddcup' input path
	private static String kddcup = "kddcup";

	// 'drift' input path
	private static String drift = "drift";

	// 'sift10M' input path
	private static String sift10M = "sift";

	public static void main(String[] args) throws IOException {

		String dataset = kddcup;
		int k = 13;
		double epsilon = 1;
		int n = 1000;
		int windowLength = 300;
		int queryCount = 100;
		Metric metric = new LpNorm(2);

		// create update stream
		SlidingWindow updateStream = new SlidingWindow(n, windowLength,
				System.getProperty("user.dir") + "\\main\\dataSet\\" + dataset, true);

//		double dMax = EstimateMaxDistance(metric, updateStream);
//		double dMin = findMinDistance(metric, updateStream);

		DynamicAlgorithm algBEFHJMW = new BEFHJMW(k, metric, epsilon);
		runTest(updateStream, algBEFHJMW, metric, dataset, queryCount,
				System.getProperty("user.dir") + "\\test_results\\");

		DynamicAlgorithm algBCLP = new BCLP(k, metric, epsilon);
		runTest(updateStream, algBCLP, metric, dataset, queryCount,
				System.getProperty("user.dir") + "\\test_results\\");

	}

	private static double findMinDistance(Metric metric, SlidingWindow updateStream) {
		double min = Double.MAX_VALUE;
		for (int i = 0; i < updateStream.streamLength(); i++)
			for (int j = i + 1; j < updateStream.streamLength(); j++) {
				double d = metric.d(updateStream.point(i), updateStream.point(j));
				if (d > 0 && min > d)
					min = d;
			}
		return min;
	}

	private static double EstimateMaxDistance(Metric metric, SlidingWindow updateStream) {
		double max = Double.MIN_VALUE;
		double[] x = updateStream.point(0);
		for (int i = 1; i < updateStream.streamLength(); i++) {
			double[] y = updateStream.point(i);
			double d = metric.d(x, y);
			if (d > max)
				max = d;
		}
		return max;
	}

	private static void runTest(SlidingWindow updateStream, DynamicAlgorithm alg, Metric metric, String dataset,
			int queryCount, String dir) throws IOException {
		int queryFrequency = (int) (updateStream.streamLength() / queryCount);
		long updateTime = 0;
		long queryTime = 0;
		double cost = 0;
		long recourse = 0;

		// maintain the current instance in this BBT
		TreeMap<Integer, double[]> activePoints = new TreeMap<Integer, double[]>();

		BufferedWriter updateTimeWriter = new BufferedWriter(
				new FileWriter(dir + dataset + "-" + alg.name() + "-updateTime.txt"));
		BufferedWriter queryTimeWriter = new BufferedWriter(
				new FileWriter(dir + dataset + "-" + alg.name() + "-queryTime.txt"));
		BufferedWriter costWriter = new BufferedWriter(new FileWriter(dir + dataset + "-" + alg.name() + "-cost.txt"));
		BufferedWriter recourseWriter = new BufferedWriter(
				new FileWriter(dir + dataset + "-" + alg.name() + "-recourse.txt"));

		for (int i = 0; i < updateStream.streamLength(); i++) {
			TreeSet<Integer> oldSolution = alg.getCenters();
			long s;
			if (updateStream.updateType(i)) {
				activePoints.put(updateStream.key(i), updateStream.point(i));
				s = System.nanoTime();
				alg.insert(updateStream.key(i), updateStream.point(i));
				updateTime += System.nanoTime() - s;
			}
			if (!updateStream.updateType(i)) {
				activePoints.remove(updateStream.key(i));
				s = System.nanoTime();
				alg.delete(updateStream.key(i));
				updateTime += System.nanoTime() - s;
			}
			updateTimeWriter.write(updateTime + "\n");

			TreeSet<Integer> newSolution = alg.getCenters();

			for (Integer C : oldSolution)
				if (!newSolution.contains(C))
					recourse++;
			for (Integer C : newSolution)
				if (!oldSolution.contains(C))
					recourse++;

			recourseWriter.write(recourse + "\n");

			if (i % queryFrequency == 0 || i == updateStream.streamLength() - 1) {
				s = System.nanoTime();
				TreeMap<Integer, Integer> clusters = alg.cluster();
				queryTime += System.nanoTime() - s;
				cost = cost(activePoints, clusters, metric);
				queryTimeWriter.write(queryTime + "\n");
				costWriter.write(cost + "\n");
			}

			if (i % (int) (updateStream.streamLength() / 100) == 0) {
				printProgress(updateStream, i);
			}

		}

		recourseWriter.close();
		updateTimeWriter.close();
		queryTimeWriter.close();
		costWriter.close();
	}

	public static double cost(TreeMap<Integer, double[]> points, TreeMap<Integer, Integer> cluster, Metric metric) {
		double cost = 0;
		Iterator<Map.Entry<Integer, Integer>> iterator = cluster.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, Integer> entry = iterator.next();
			cost = Math.max(cost, metric.d(points.get(entry.getKey()), points.get(entry.getValue())));
		}
		return cost;
	}

	private static void printProgress(SlidingWindow updateStream, int i) {
		System.out.print(100 * i / updateStream.streamLength());
		System.out.print("% complete (");
		System.out.print(i);
		System.out.println(" updates)");
	}

}
