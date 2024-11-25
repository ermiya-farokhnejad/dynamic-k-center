package main;

//implementation of Lp-norm
public class LpNorm extends Metric {

	private int p;

	private double noise;

	public LpNorm(int p, double noise) {
		this.p = p;
		this.noise = noise;
	}

	LpNorm(int p) {
		this.p = p;
		this.noise = 0;
	}

	public double d(double[] x, double[] y) {

		// ensure vectors have same dimension
		if (x.length != y.length) {
			return 0;
		}

		double sum = 0;
		for (int i = 0; i < x.length; i++) {
			sum += Math.pow(Math.abs(x[i] - y[i]), p);
		}

		double d = Math.pow(sum, 1.0 / (double) p);

		return d + noise;
	}
}
