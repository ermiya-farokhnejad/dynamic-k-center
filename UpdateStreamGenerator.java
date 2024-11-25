package main;

// generates a stream of m updates given data path

abstract class UpdateStreamGenerator {

  // returns the data point in update i
  public abstract double[] point(int i);

  // returns the (unique) key corresponding to the data point in update i
  public abstract int key(int i);

  // returns whether update i is an insertion (true) / deletion (false)
  public abstract boolean updateType(int i);

}