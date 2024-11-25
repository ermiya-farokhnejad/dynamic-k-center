package main;

// implementation of metrics defined on R^n
public abstract class Metric {

  // computes the distance between two points with respect to this metric
  public abstract double d(double[] x, double[] y);

}


