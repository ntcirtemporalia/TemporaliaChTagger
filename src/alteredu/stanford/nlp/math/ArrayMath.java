package alteredu.stanford.nlp.math;

import alteredu.stanford.nlp.util.StringUtils;

import java.text.NumberFormat;
import java.util.Random;
import java.util.Arrays;
import java.util.List;
import java.io.IOException;

/**
 * Class ArrayMath
 *
 * @author Teg Grenager
 */
public class ArrayMath {

  private static Random rand = new Random();


  // BASIC INFO -----------------------------------------------------------------

  public static int numRows(double[] v) {
    return v.length;
  }


  // CASTS ----------------------------------------------------------------------

  public static float[] doubleArrayToFloatArray(double a[]) {
    float[] result = new float[a.length];
    for (int i = 0; i < a.length; i++) {
      result[i] = (float) a[i];
    }
    return result;
  }

  public static double[] floatArrayToDoubleArray(float a[]) {
    double[] result = new double[a.length];
    for (int i = 0; i < a.length; i++) {
      result[i] = (double) a[i];
    }
    return result;
  }

  public static double[][] floatArrayToDoubleArray(float a[][]) {
    double[][] result = new double[a.length][];
    for (int i = 0; i < a.length; i++) {
      result[i] = new double[a[i].length];
      for (int j = 0; j < a[i].length; j++) {
        result[i][j] = (double) a[i][j];
      }
    }
    return result;
  }

  public static float[][] doubleArrayToFloatArray(double a[][]) {
    float[][] result = new float[a.length][];
    for (int i = 0; i < a.length; i++) {
      result[i] = new float[a[i].length];
      for (int j = 0; j < a[i].length; j++) {
        result[i][j] = (float) a[i][j];
      }
    }
    return result;
  }

  // OPERATIONS ON AN ARRAY - NONDESTRUCTIVE

  public static double[] exp(double[] a) {
    double[] result = new double[a.length];
    for (int i = 0; i < a.length; i++) {
      result[i] = Math.exp(a[i]);
    }
    return result;
  }

  public static double[] log(double[] a) {
    double[] result = new double[a.length];
    for (int i = 0; i < a.length; i++) {
      result[i] = Math.log(a[i]);
    }
    return result;
  }

  // OPERATIONS ON AN ARRAY - DESTRUCTIVE

  public static void expInPlace(double[] a) {
    for (int i = 0; i < a.length; i++) {
      a[i] = Math.exp(a[i]);
    }
  }

  public static void logInPlace(double[] a) {
    for (int i = 0; i < a.length; i++) {
      a[i] = Math.log(a[i]);
    }
  }

  // OPERATIONS WITH SCALAR - DESTRUCTIVE

  /**
   * Shifts the values in this array by b. Does it in place.
   */
  public static void addInPlace(double[] a, double b) {
    for (int i = 0; i < a.length; i++) {
      a[i] = a[i] + b;
    }
  }

  /**
   * Shifts the values in this array by b. Does it in place.
   */
  public static void addInPlace(float[] a, double b) {
    for (int i = 0; i < a.length; i++) {
      a[i] = (float) (a[i] + b);
    }
  }

  /**
   * Scales the values in this array by b. Does it in place.
   */
  public static void multiplyInPlace(double[] a, double b) {
    for (int i = 0; i < a.length; i++) {
      a[i] = a[i] * b;
    }
  }

  /**
   * Scales the values in this array by b. Does it in place.
   */
  public static void multiplyInPlace(float[] a, double b) {
    for (int i = 0; i < a.length; i++) {
      a[i] = (float) (a[i] * b);
    }
  }

  /**
   * Scales the values in this array by c.
   */
  public static void powInPlace(double[] a, double c) {
    for (int i = 0; i < a.length; i++) {
      a[i] = Math.pow(a[i], c);
    }
  }

  /**
   * Sets the values in this array by to their value taken to cth power.
   */
  public static void powInPlace(float[] a, float c) {
    for (int i = 0; i < a.length; i++) {
      a[i] = (float) Math.pow(a[i], c);
    }
  }

  // OPERATIONS WITH SCALAR - NONDESTRUCTIVE

  public static double[] add(double[] a, double c) {
    double[] result = new double[a.length];
    for (int i = 0; i < a.length; i++) {
      result[i] = a[i] + c;
    }
    return result;
  }

  public static float[] add(float[] a, double c) {
    float[] result = new float[a.length];
    for (int i = 0; i < a.length; i++) {
      result[i] = (float) (a[i] + c);
    }
    return result;
  }

  /**
   * Scales the values in this array by c.
   */
  public static double[] multiply(double[] a, double c) {
    double[] result = new double[a.length];
    for (int i = 0; i < a.length; i++) {
      result[i] = a[i] * c;
    }
    return result;
  }

  /**
   * Scales the values in this array by c.
   */
  public static float[] multiply(float[] a, float c) {
    float[] result = new float[a.length];
    for (int i = 0; i < a.length; i++) {
      result[i] = a[i] * c;
    }
    return result;
  }

  /**
   * Scales the values in this array by c.
   */
  public static double[] pow(double[] a, double c) {
    double[] result = new double[a.length];
    for (int i = 0; i < a.length; i++) {
      result[i] = Math.pow(a[i], c);
    }
    return result;
  }

  /**
   * Scales the values in this array by c.
   */
  public static float[] pow(float[] a, float c) {
    float[] result = new float[a.length];
    for (int i = 0; i < a.length; i++) {
      result[i] = (float) Math.pow(a[i], c);
    }
    return result;
  }

  // OPERATIONS WITH TWO ARRAYS - DESTRUCTIVE

  public static void pairwiseAddInPlace(double[] to, double[] from) {
    if (to.length != from.length) {
      throw new RuntimeException();
    }
    for (int i = 0; i < to.length; i++) {
      to[i] = to[i] + from[i];
    }
  }

  public static void pairwiseSubtractInPlace(double[] to, double[] from) {
    if (to.length != from.length) {
      throw new RuntimeException();
    }
    for (int i = 0; i < to.length; i++) {
      to[i] = to[i] - from[i];
    }
  }

  // OPERATIONS WITH TWO ARRAYS - NONDESTRUCTIVE

  public static int[] pairwiseAdd(int[] a, int[] b) {
    int[] result = new int[a.length];
    for (int i = 0; i < a.length; i++) {
      result[i] = a[i] + b[i];
    }
    return result;
  }

  public static double[] pairwiseAdd(double[] a, double[] b) {
    double[] result = new double[a.length];
    for (int i = 0; i < a.length; i++) {
      result[i] = a[i] + b[i];
    }
    return result;
  }

  public static float[] pairwiseAdd(float[] a, float[] b) {
    float[] result = new float[a.length];
    for (int i = 0; i < a.length; i++) {
      result[i] = a[i] + b[i];
    }
    return result;
  }

  public static double[] pairwiseSubtract(double[] a, double[] b) {
    double[] c = new double[a.length];

    for (int i = 0; i < a.length; i++) {
      c[i] = a[i] - b[i];
    }
    return c;
  }

  public static float[] pairwiseSubtract(float[] a, float[] b) {
    float[] c = new float[a.length];

    for (int i = 0; i < a.length; i++) {
      c[i] = a[i] - b[i];
    }
    return c;
  }

  /**
   * Assumes that both arrays have same length.
   */
  public static double[] pairwiseMultiply(double[] a, double[] b) {
    if (a.length != b.length) {
      throw new RuntimeException("Can't pairwise multiple different lengths: a.length=" + a.length + " b.length=" + b.length);
    }
    double[] result = new double[a.length];
    for (int i = 0; i < result.length; i++) {
      result[i] = a[i] * b[i];
    }
    return result;
  }

  /**
   * Assumes that both arrays have same length.
   */
  public static float[] pairwiseMultiply(float[] a, float[] b) {
    if (a.length != b.length) {
      throw new RuntimeException();
    }
    float[] result = new float[a.length];
    for (int i = 0; i < result.length; i++) {
      result[i] = a[i] * b[i];
    }
    return result;
  }

  /**
   * Puts the result in the result array.
   * Assumes that all arrays have same length.
   */
  public static void pairwiseMultiply(double[] a, double[] b, double[] result) {
    if (a.length != b.length) {
      throw new RuntimeException();
    }
    for (int i = 0; i < result.length; i++) {
      result[i] = a[i] * b[i];
    }
  }

  /**
   * Puts the result in the result array.
   * Assumes that all arrays have same length.
   */
  public static void pairwiseMultiply(float[] a, float[] b, float[] result) {
    if (a.length != b.length) {
      throw new RuntimeException();
    }
    for (int i = 0; i < result.length; i++) {
      result[i] = a[i] * b[i];
    }
  }

  // ERROR CHECKING

  public static boolean hasNaN(double[] a) {
    for (int i = 0; i < a.length; i++) {
      if (Double.isNaN(a[i])) return true;
    }
    return false;
  }

  public static boolean hasInfinite(double[] a) {
    for (int i = 0; i < a.length; i++) {
      if (Double.isInfinite(a[i])) return true;
    }
    return false;
  }

  // methods for filtering vectors ------------------------------------------
  
  public static int countNaN(double[] v) {
    int c = 0;
    for (int i = 0; i < v.length; i++)
      if (Double.isNaN(v[i])) 
        c++;
    return c;
  }

  public static double[] filterNaN(double[] v) {
    double[] u = new double[numRows(v) - countNaN(v)];
    int j = 0;
    for (int i = 0; i < v.length; i++) {
      if (!Double.isNaN(v[i])) {
        u[j++] = v[i];
      }
    }
    return u;
  }

  public static int countInfinite(double[] v) {
    int c = 0;
    for (int i = 0; i < v.length; i++)
      if (Double.isInfinite(v[i])) 
        c++;
    return c;
  }

  public static double[] filterInfinite(double[] v) {
    double[] u = new double[numRows(v) - countInfinite(v)];
    int j = 0;
    for (int i = 0; i < v.length; i++) {
      if (!Double.isInfinite(v[i])) {
        u[j++] = v[i];
      }
    }
    return u;
  }

  public static double[] filterNaNAndInfinite(double[] v) {
    return filterInfinite(filterNaN(v));
  }


  // VECTOR PROPERTIES

  /**
   * Returns the sum of an array of numbers.
   */
  public static double sum(double[] a) {
    return sum(a,0,a.length);
  }
  
  /**
   * Returns the sum of the portion of an array of numbers between 
   * <code>fromIndex</code>, inclusive, and <code>toIndex</code>, exclusive. 
   * Returns 0 if <code>fromIndex</code> &gt;= <code>toIndex</code>.
   */
  public static double sum(double[] a, int fromIndex, int toIndex) {
    double result = 0.0;
    for (int i = fromIndex; i < toIndex; i++) {
      result += a[i];
    }
    return result;    
  }
  


  public static int sum(int[] a) {
    int result = 0;
    for (int i = 0; i < a.length; i++) {
      result += a[i];
    }
    return result;
  }

  public static float sum(float[] a) {
    float result = 0.0F;
    for (int i = 0; i < a.length; i++) {
      result += a[i];
    }
    return result;
  }

  public static int sum(int[][] a) {
    int result = 0;
    for (int i = 0; i < a.length; i++) {
      for (int j=0; j<a[i].length; j++) {
        result += a[i][j];
      }
    }
    return result;
  }

  public static double average(double[] a) {
    double total = ArrayMath.sum(a);
    return total / (double)a.length;
  }

  /**
   * Computes inf-norm of vector
   *
   * @param a
   * @return inf-norm of a
   */
  public static double norm_inf(double[] a) {
    double max = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < a.length; i++) {
      if (Math.abs(a[i]) > max) {
        max = Math.abs(a[i]);
      }
    }
    return max;
  }


  /**
   * Computes inf-norm of vector
   *
   * @param a
   * @return inf-norm of a
   */
  public static double norm_inf(float[] a) {
    double max = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < a.length; i++) {
      if (Math.abs(a[i]) > max) {
        max = Math.abs(a[i]);
      }
    }
    return max;
  }

  /**
   * Computes 1-norm of vector
   *
   * @param a
   * @return 1-norm of a
   */
  public static double norm_1(double[] a) {
    double sum = 0;
    for (int i = 0; i < a.length; i++) {
      sum += (a[i] < 0 ? -a[i] : a[i]);
    }
    return sum;
  }

  /**
   * Computes 1-norm of vector
   *
   * @param a
   * @return 1-norm of a
   */
  public static double norm_1(float[] a) {
    double sum = 0;
    for (int i = 0; i < a.length; i++) {
      sum += (a[i] < 0 ? -a[i] : a[i]);
    }
    return sum;
  }


  /**
   * Computes 2-norm of vector
   *
   * @param a
   * @return Euclidean norm of a
   */
  public static double norm(double[] a) {
    double squaredSum = 0;
    for (int i = 0; i < a.length; i++) {
      squaredSum += a[i] * a[i];
    }
    return Math.sqrt(squaredSum);
  }

  /**
   * Computes 2-norm of vector
   *
   * @param a
   * @return Euclidean norm of a
   */
  public static double norm(float[] a) {
    double squaredSum = 0;
    for (int i = 0; i < a.length; i++) {
      squaredSum += a[i] * a[i];
    }
    return Math.sqrt(squaredSum);
  }

  /**
   * @return the index of the max value; if max is a tie, returns the first one.
   */
  public static int argmax(double[] a) {
    double max = Double.NEGATIVE_INFINITY;
    int argmax = 0;
    for (int i = 0; i < a.length; i++) {
      if (a[i] > max) {
        max = a[i];
        argmax = i;
      }
    }
    return argmax;
  }

  public static double max(double[] a) {
    return a[argmax(a)];
  }

  /**
   * @return the index of the max value; if max is a tie, returns the first one.
   */
  public static int argmax(float[] a) {
    float max = Float.NEGATIVE_INFINITY;
    int argmax = 0;
    for (int i = 0; i < a.length; i++) {
      if (a[i] > max) {
        max = a[i];
        argmax = i;
      }
    }
    return argmax;
  }

  public static float max(float[] a) {
    return a[argmax(a)];
  }

  /**
   * @return the index of the max value; if max is a tie, returns the first one.
   */
  public static int argmin(double[] a) {
    double min = Double.POSITIVE_INFINITY;
    int argmin = 0;
    for (int i = 0; i < a.length; i++) {
      if (a[i] < min) {
        min = a[i];
        argmin = i;
      }
    }
    return argmin;
  }

  public static double min(double[] a) {
    return a[argmin(a)];
  }

  /** 
   * Returns the largest value in a vector of doubles.  Any values which
   * are NaN or infinite are ignored.  If the vector is empty, 0.0 is
   * returned.
   */
  public static double safeMin(double[] v) {
    double[] u = filterNaNAndInfinite(v);
    if (numRows(u) == 0) return 0.0;
    return min(u);
  }

  /**
   * @return the index of the max value; if max is a tie, returns the first one.
   */
  public static int argmin(float[] a) {
    float min = Float.POSITIVE_INFINITY;
    int argmin = 0;
    for (int i = 0; i < a.length; i++) {
      if (a[i] < min) {
        min = a[i];
        argmin = i;
      }
    }
    return argmin;
  }

  public static float min(float[] a) {
    return a[argmin(a)];
  }

  /**
   * @return the index of the max value; if max is a tie, returns the first one.
   */
  public static int argmin(int[] a) {
    int min = Integer.MAX_VALUE;
    int argmin = 0;
    for (int i = 0; i < a.length; i++) {
      if (a[i] < min) {
        min = a[i];
        argmin = i;
      }
    }
    return argmin;
  }

  public static int min(int[] a) {
    return a[argmin(a)];
  }

  /**
   * @return the index of the max value; if max is a tie, returns the first one.
   */
  public static int argmax(int[] a) {
    int max = Integer.MIN_VALUE;
    int argmax = 0;
    for (int i = 0; i < a.length; i++) {
      if (a[i] > max) {
        max = a[i];
        argmax = i;
      }
    }
    return argmax;
  }

  public static int max(int[] a) {
    return a[argmax(a)];
  }

  /** 
   * Returns the largest value in a vector of doubles.  Any values which
   * are NaN or infinite are ignored.  If the vector is empty, 0.0 is
   * returned.
   */
  public static double safeMax(double[] v) {
    double[] u = filterNaNAndInfinite(v);
    if (numRows(u) == 0) return 0.0;
    return max(u);
  }

  /**
   * Returns the log of the sum of an array of numbers, which are
   * themselves input in log form.  This is all natural logarithms.
   * Reasonable care is taken to do this as efficiently as possible
   * (under the assumption that the numbers might differ greatly in
   * magnitude), with high accuracy, and without numerical overflow.
   *
   * @param logInputs An array of numbers [log(x1), ..., log(xn)]
   * @return log(x1 + ... + xn)
   */
  public static double logSum(double[] logInputs) {
    return logSum(logInputs,0,logInputs.length);
  }
  
  /**
   * Returns the log of the portion between <code>fromIndex</code>, inclusive, and 
   * <code>toIndex</code>, exclusive, of an array of numbers, which are
   * themselves input in log form.  This is all natural logarithms.
   * Reasonable care is taken to do this as efficiently as possible
   * (under the assumption that the numbers might differ greatly in
   * magnitude), with high accuracy, and without numerical overflow.  Throws an 
   * {@link IllegalArgumentException} if <code>logInputs</code> is of length zero.
   * Otherwise, returns Double.NEGATIVE_INFINITY if <code>fromIndex</code> &gt;= 
   * <code>toIndex</code>.
   *
   * @param logInputs An array of numbers [log(x1), ..., log(xn)]
   * @param fromIndex the 
   * @return log(x1 + ... + xn)
   */
  public static double logSum(double[] logInputs, int fromIndex, int toIndex) {  
    if (logInputs.length == 0)
      throw new IllegalArgumentException();
    if(fromIndex >= 0 && toIndex < logInputs.length && fromIndex >= toIndex)
      return Double.NEGATIVE_INFINITY;
    int maxIdx = fromIndex; 
    double max = logInputs[fromIndex];
    for (int i = fromIndex+1; i < toIndex; i++) {
      if (logInputs[i] > max) {
        maxIdx = i;
        max = logInputs[i];
      }
    }
    boolean haveTerms = false;
    double intermediate = 0.0;
    double cutoff = max - SloppyMath.LOGTOLERANCE;
    // we avoid rearranging the array and so test indices each time!
    for (int i = fromIndex; i < toIndex; i++) {
      if (i != maxIdx && logInputs[i] > cutoff) {
        haveTerms = true;
        intermediate += Math.exp(logInputs[i] - max);
      }
    }
    if (haveTerms) {
      return max + Math.log(1.0 + intermediate);
    } else {
      return max;
    }
  }

  /**
   * Returns the log of the sum of an array of numbers, which are
   * themselves input in log form.  This is all natural logarithms.
   * Reasonable care is taken to do this as efficiently as possible
   * (under the assumption that the numbers might differ greatly in
   * magnitude), with high accuracy, and without numerical overflow.
   *
   * @param logInputs An array of numbers [log(x1), ..., log(xn)]
   * @return log(x1 + ... + xn)
   */
  public static float logSum(float[] logInputs) {
    int leng = logInputs.length;
    if (leng == 0) {
      throw new IllegalArgumentException();
    }
    int maxIdx = 0;
    float max = logInputs[0];
    for (int i = 1; i < leng; i++) {
      if (logInputs[i] > max) {
        maxIdx = i;
        max = logInputs[i];
      }
    }
    boolean haveTerms = false;
    double intermediate = 0.0f;
    float cutoff = max - SloppyMath.LOGTOLERANCE_F;
    // we avoid rearranging the array and so test indices each time!
    for (int i = 0; i < leng; i++) {
      if (i != maxIdx && logInputs[i] > cutoff) {
        haveTerms = true;
        intermediate += Math.exp(logInputs[i] - max);
      }
    }
    if (haveTerms) {
      return max + (float) Math.log(1.0 + intermediate);
    } else {
      return max;
    }
  }

  // LINEAR ALGEBRAIC FUNCTIONS

  public static double innerProduct(double[] a, double[] b) {
    double result = 0.0;
    for (int i = 0; i < a.length; i++) {
      result += a[i] * b[i];
    }
    return result;
  }

  public static double innerProduct(float[] a, float[] b) {
    double result = 0.0;
    for (int i = 0; i < a.length; i++) {
      result += a[i] * b[i];
    }
    return result;
  }

  // UTILITIES

  public static int[] subArray(int[] a, int from, int to) {
    int[] result = new int[to-from];
    System.arraycopy(a, from, result, 0, to-from);
    return result;
  }

  public static double[][] load2DMatrixFromFile(String filename) throws IOException {
    String s = StringUtils.slurpFile(filename);
    String[] rows = s.split("[\r\n]+");
    double[][] result = new double[rows.length][];
    for (int i=0; i<result.length; i++) {
      String[] columns = rows[i].split("\\s+");
      result[i] = new double[columns.length];
      for (int j=0; j<result[i].length; j++) {
        result[i][j] = Double.parseDouble(columns[j]);
      }
    }
    return result;
  }

  public static Integer[] box(int[] assignment) {
    Integer[] result = new Integer[assignment.length];
    for (int i=0; i<assignment.length; i++) {
      result[i] = assignment[i];
    }
    return result;
  }

  public static int[] unbox_1(List<Integer> integerList) {
    int[] result = new int[integerList.size()];
    for (int i=0; i<integerList.size(); i++) {
      result[i] = integerList.get(i);
    }
    return result;
  }

  public static Double[] box(double[] assignment) {
    Double[] result = new Double[assignment.length];
    for (int i=0; i<assignment.length; i++) {
      result[i] = assignment[i];
    }
    return result;
  }

  public static double[] unbox_2(List<Double> integerList) {
    double[] result = new double[integerList.size()];
    for (int i=0; i<integerList.size(); i++) {
      result[i] = integerList.get(i);
    }
    return result;
  }

  public static int indexOf(int n, int[] a) {
    for (int i=0; i<a.length; i++) {
      if (a[i]==n) return i;
    }
    return -1;
  }

  public static int[][] castToInt(double[][] doubleCounts) {
    int[][] result = new int[doubleCounts.length][];
    for (int i=0; i<doubleCounts.length; i++) {
      result[i] = new int[doubleCounts[i].length];
      for (int j=0; j<doubleCounts[i].length; j++) {
        result[i][j] = (int) doubleCounts[i][j];
      }
    }
    return result;
  }

  // PROBABILITY FUNCTIONS

  /**
   * Makes the values in this array sum to 1.0. Does it in place.
   * If the total is 0.0, sets a to the uniform distribution.
   */
  public static void normalize(double[] a) {
    double total = sum(a);
    if (total == 0.0) {
      throw new RuntimeException("Can't normalize an array with sum 0.0: " + Arrays.toString(a));
    }
    multiplyInPlace(a, 1.0/total); // divide each value by total
  }

  /**
   * Makes the values in this array sum to 1.0. Does it in place.
   * If the total is 0.0, sets a to the uniform distribution.
   */
  public static void normalize(float[] a) {
    float total = sum(a);
    if (total == 0.0) {
      throw new RuntimeException("Can't normalize an array with sum 0.0");
    }
    multiplyInPlace(a, 1.0/total); // divide each value by total
  }

  /**
   * Makes the values in this array sum to 1.0. Does it in place.
   * If the total is 0.0, sets a to the uniform distribution.
   */
  public static void logNormalize(double[] a) {
    double logTotal = logSum(a);
    if (logTotal == Double.NEGATIVE_INFINITY) {
      // to avoid NaN values
      double v = -Math.log((double) a.length);
      for (int i = 0; i < a.length; i++) {
        a[i] = v;
      }
      return;
    }
    addInPlace(a, -logTotal); // subtract log total from each value
  }

  /**
   * Samples from the distribution over values 0 through d.length given by d.
   * Assumes that the distribution sums to 1.0.
   *
   * @param d the distribution to sample from
   * @return a value from 0 to d.length
   */
  public static int sampleFromDistribution(double[] d) {
    return sampleFromDistribution(d, rand);
  }

  /**
   * Samples from the distribution over values 0 through d.length given by d.
   * Assumes that the distribution sums to 1.0.
   *
   * @param d the distribution to sample from
   * @return a value from 0 to d.length
   */
  public static int sampleFromDistribution(double[] d, Random random) {
    // sample from the uniform [0,1]
    double r = random.nextDouble();
    // now compare its value to cumulative values to find what interval it falls in
    double total = 0;
    for (int i = 0; i < d.length - 1; i++) {
      if (Double.isNaN(d[i])) {
        throw new RuntimeException("Can't sample from NaN");
      }
      total += d[i];
      if (r < total) {
        return i;
      }
    }
    return d.length - 1; // in case the "double-math" didn't total to exactly 1.0
  }

  /**
   * Samples from the distribution over values 0 through d.length given by d.
   * Assumes that the distribution sums to 1.0.
   *
   * @param d the distribution to sample from
   * @return a value from 0 to d.length
   */
  public static int sampleFromDistribution(float[] d, Random random) {
    // sample from the uniform [0,1]
    double r = random.nextDouble();
    // now compare its value to cumulative values to find what interval it falls in
    double total = 0;
    for (int i = 0; i < d.length - 1; i++) {
      if (Float.isNaN(d[i])) {
        throw new RuntimeException("Can't sample from NaN");
      }
      total += d[i];
      if (r < total) {
        return i;
      }
    }
    return d.length - 1; // in case the "double-math" didn't total to exactly 1.0
  }

  public static double klDivergence(double[] from, double[] to) {
    double kl = 0.0;
    double tot = sum(from);
    double tot2 = sum(to);
    // System.out.println("tot is " + tot + " tot2 is " + tot2);
    for (int i = 0; i < from.length; i++) {
      if (from[i] == 0.0) {
        continue;
      }
      double num = from[i] / tot;
      double num2 = to[i] / tot2;
      // System.out.println("num is " + num + " num2 is " + num2);
      kl += num * (Math.log(num / num2) / Math.log(2.0));
    }
    return kl;

  }

  public static void setToLogDeterministic(float[] a, int i) {
    for (int j = 0; j < a.length; j++) {
      if (j == i) {
        a[j] = 0.0F;
      } else {
        a[j] = Float.NEGATIVE_INFINITY;
      }
    }
  }

  public static void setToLogDeterministic(double[] a, int i) {
    for (int j = 0; j < a.length; j++) {
      if (j == i) {
        a[j] = 0.0;
      } else {
        a[j] = Double.NEGATIVE_INFINITY;
      }
    }
  }

  // SAMPLE ANALYSIS

  public static double mean(double[] a) {
    return sum(a) / (double) a.length;
  }

  /** 
   * Returns the mean of a vector of doubles.  Any values which are NaN or
   * infinite are ignored.  If the vector is empty, 0.0 is returned.
   */
  public static double safeMean(double[] v) {
    double[] u = filterNaNAndInfinite(v);
    if (numRows(u) == 0) return 0.0;
    return mean(u);
  }

  public static double sumSquaredError(double[] a) {
    double mean = mean(a);
    double result = 0.0;
    for (int i = 0; i < a.length; i++) {
      double diff = a[i] - mean;
      result += (diff * diff);
    }
    return result;
  }

  public static double variance(double[] a) {
    return sumSquaredError(a) / (double) (a.length - 1);
  }

  public static double stdev(double[] a) {
    return Math.sqrt(variance(a));
  }

  /** 
   * Returns the standard deviation of a vector of doubles.  Any values which
   * are NaN or infinite are ignored.  If the vector contains fewer than two
   * values, 1.0 is returned.
   */
  public static double safeStdev(double[] v) {
    double[] u = filterNaNAndInfinite(v);
    if (numRows(u) < 2) return 1.0;
    return stdev(u);
  }

  public static double standardErrorOfMean(double[] a) {
    return stdev(a) / Math.sqrt((double) a.length);
  }


  /**
   * Fills the array with sample from 0 to numArgClasses-1 without replacement.
   * @param array
   * @param numArgClasses
   */
  public static void sampleWithoutReplacement(int[] array, int numArgClasses) {
    sampleWithoutReplacement(array, numArgClasses, rand);
  }
  /**
   * Fills the array with sample from 0 to numArgClasses-1 without replacement.
   * @param array
   * @param numArgClasses
   */
  public static void sampleWithoutReplacement(int[] array, int numArgClasses, Random rand) {
    int[] temp = new int[numArgClasses];
    for (int i = 0; i < temp.length; i++) {
      temp[i] = i;
    }
    shuffle(temp, rand);
    System.arraycopy(temp, 0, array, 0, array.length);
  }

  public static void shuffle(int[] a) {
    shuffle(a, rand);
  }

  public static void shuffle(int[] a, Random rand) {
    for (int i=a.length-1; i>=1; i--) {
      int j = rand.nextInt(i+1); // a random index from 0 to i inclusive, may shuffle with itself
      int tmp = a[i];
      a[i] = a[j];
      a[j] = tmp;
    }
  }

  public static void reverse(int[] a) {
    for (int i=0; i<a.length/2; i++) {
      int j = a.length - i - 1; 
      int tmp = a[i];
      a[i] = a[j];
      a[j] = tmp;
    }
  }

  public static boolean contains(int[] a, int i) {
    for (int j = 0; j < a.length; j++) {
      if (a[j]==i) return true;
    }
    return false;
  }

  public static boolean containsInSubarray(int[] a, int begin, int end, int i) {
    for (int j = begin; j < end; j++) {
      if (a[j]==i) return true;
    }
    return false;
  }

  /**
   * Computes the significance level by approximate randomization, using a
   * default value of 1000 iterations.  See documentation for other version
   * of method.
   */
  public static double sigLevelByApproxRand(double[] A, double[] B) {
    return sigLevelByApproxRand(A, B, 1000);
  }

  /**
   * Takes a pair of arrays, A and B, which represent corresponding
   * outcomes of a pair of random variables: say, results for two different
   * classifiers on a sequence of inputs.  Returns the estimated
   * probability that the difference between the means of A and B is not
   * significant, that is, the significance level.  This is computed by
   * "approximate randomization".  The test statistic is the absolute
   * difference between the means of the two arrays.  A randomized test
   * statistic is computed the same way after initially randomizing the
   * arrays by swapping each pair of elements with 50% probability.  For
   * the given number of iterations, we generate a randomized test
   * statistic and compare it to the actual test statistic.  The return
   * value is the proportion of iterations in which a randomized test
   * statistic was found to exceed the actual test statistic.
   */
  public static double sigLevelByApproxRand(double[] A, double[] B, int iterations) {
    if (A.length == 0)
      throw new IllegalArgumentException("Input arrays must not be empty!");
    if (A.length != B.length)
      throw new IllegalArgumentException("Input arrays must have equal length!");
    if (iterations <= 0)
      throw new IllegalArgumentException("Number of iterations must be positive!");
    double testStatistic = absDiffOfMeans(A, B, false); // not randomized
    int successes = 0;
    for (int i = 0; i < iterations; i++) {
      double t =  absDiffOfMeans(A, B, true); // randomized
      if (t >= testStatistic) successes++;
    }
    return (double) (successes + 1) / (double) (iterations + 1);
  }

  public static double sigLevelByApproxRand(int[] A, int[] B) {
    return sigLevelByApproxRand(A, B, 1000);
  }

  public static double sigLevelByApproxRand(int[] A, int[] B, int iterations) {
    if (A.length == 0)
      throw new IllegalArgumentException("Input arrays must not be empty!");
    if (A.length != B.length)
      throw new IllegalArgumentException("Input arrays must have equal length!");
    if (iterations <= 0)
      throw new IllegalArgumentException("Number of iterations must be positive!");
    double[] X = new double[A.length];
    double[] Y = new double[B.length];
    for (int i = 0; i < A.length; i++) {
      X[i] = (double) A[i];
      Y[i] = (double) B[i];
    }
    return sigLevelByApproxRand(X, Y, iterations);
  }

  public static double sigLevelByApproxRand(boolean[] A, boolean[] B) {
    return sigLevelByApproxRand(A, B, 1000);
  }

  public static double sigLevelByApproxRand(boolean[] A, boolean[] B, int iterations) {
    if (A.length == 0)
      throw new IllegalArgumentException("Input arrays must not be empty!");
    if (A.length != B.length)
      throw new IllegalArgumentException("Input arrays must have equal length!");
    if (iterations <= 0)
      throw new IllegalArgumentException("Number of iterations must be positive!");
    double[] X = new double[A.length];
    double[] Y = new double[B.length];
    for (int i = 0; i < A.length; i++) {
      X[i] = (A[i] ? 1.0 : 0.0);
      Y[i] = (B[i] ? 1.0 : 0.0);
    }
    return sigLevelByApproxRand(X, Y, iterations);
  }


  // Returns the absolute difference between the means of arrays A and B.
  // If 'randomize' is true, swaps matched A & B entries with 50% probability
  // Assumes input arrays have equal, non-zero length.
  private static double absDiffOfMeans(double[] A, double[] B, boolean randomize) {
    Random random = new Random();
    double aTotal = 0.0;
    double bTotal = 0.0;
    for (int i = 0; i < A.length; i++) {
      if (randomize && random.nextBoolean()) {
        aTotal += B[i];
        bTotal += A[i];
      } else {
        aTotal += A[i];
        bTotal += B[i];
      }
    }
    double aMean = aTotal / A.length;
    double bMean = bTotal / B.length;
    return Math.abs(aMean - bMean);
  }

  // PRINTING FUNCTIONS

  public static String toString(double[] a) {
    return toString(a, null);
  }

  public static String toString(double[] a, NumberFormat nf) {
    if (a == null) return null;
    if (a.length == 0) return "[]";
    StringBuffer b = new StringBuffer();
    b.append("[");
    for (int i = 0; i < a.length - 1; i++) {
      String s;
      if (nf == null) {
        s = String.valueOf(a[i]);
      } else {
        s = nf.format(a[i]);
      }
      b.append(s);
      b.append(", ");
    }
    String s;
    if (nf == null) {
      s = String.valueOf(a[a.length - 1]);
    } else {
      s = nf.format(a[a.length - 1]);
    }
    b.append(s);
    b.append(']');
    return b.toString();
  }

  public static String toString(float[] a) {
    return toString(a, null);
  }

  public static String toString(float[] a, NumberFormat nf) {
    if (a == null) return null;
    if (a.length == 0) return "[]";
    StringBuffer b = new StringBuffer();
    b.append("[");
    for (int i = 0; i < a.length - 1; i++) {
      String s;
      if (nf == null) {
        s = String.valueOf(a[i]);
      } else {
        s = nf.format(a[i]);
      }
      b.append(s);
      b.append(", ");
    }
    String s;
    if (nf == null) {
      s = String.valueOf(a[a.length - 1]);
    } else {
      s = nf.format(a[a.length - 1]);
    }
    b.append(s);
    b.append(']');
    return b.toString();
  }

  public static String toString(int[] a) {
    return toString(a, null);
  }

  public static String toString(int[] a, NumberFormat nf) {
    if (a == null) return null;
    if (a.length == 0) return "[]";
    StringBuffer b = new StringBuffer();
    b.append("[");
    for (int i = 0; i < a.length - 1; i++) {
      String s;
      if (nf == null) {
        s = String.valueOf(a[i]);
      } else {
        s = nf.format(a[i]);
      }
      b.append(s);
      b.append(", ");
    }
    String s;
    if (nf == null) {
      s = String.valueOf(a[a.length - 1]);
    } else {
      s = nf.format(a[a.length - 1]);
    }
    b.append(s);
    b.append(']');
    return b.toString();
  }

  public static String toString(byte[] a) {
    return toString(a, null);
  }

  public static String toString(byte[] a, NumberFormat nf) {
    if (a == null) return null;
    if (a.length == 0) return "[]";
     StringBuffer b = new StringBuffer();
    b.append("[");
    for (int i = 0; i < a.length - 1; i++) {
      String s;
      if (nf == null) {
        s = String.valueOf(a[i]);
      } else {
        s = nf.format(a[i]);
      }
      b.append(s);
      b.append(", ");
    }
    String s;
    if (nf == null) {
      s = String.valueOf(a[a.length - 1]);
    } else {
      s = nf.format(a[a.length - 1]);
    }
    b.append(s);
    b.append(']');
    return b.toString();
  }

  public static String toString(int[][] counts) {
    return toString(counts, null, null, 10, 10, NumberFormat.getInstance(), false);
  }

  public static String toString(int[][] counts, Object[] rowLabels, Object[] colLabels, int labelSize, int cellSize, NumberFormat nf, boolean printTotals) {
    // first compute row totals and column totals
    if (counts.length==0 || counts[0].length==0) return "";
    int[] rowTotals = new int[counts.length];
    int[] colTotals = new int[counts[0].length]; // assume it's square
    int total = 0;
    for (int i = 0; i < counts.length; i++) {
      for (int j = 0; j < counts[i].length; j++) {
        rowTotals[i] += counts[i][j];
        colTotals[j] += counts[i][j];
        total += counts[i][j];
      }
    }
    StringBuffer result = new StringBuffer();
    // column labels
    if (colLabels != null) {
      result.append(StringUtils.padLeft("", labelSize)); // spacing for the row labels!
      for (int j = 0; j < counts[0].length; j++) {
        String s = (colLabels[j]==null ? "null" : colLabels[j].toString());
        if (s.length() > cellSize - 1) {
          s = s.substring(0, cellSize - 1);
        }
        s = StringUtils.padLeft(s, cellSize);
        result.append(s);
      }
      if (printTotals) {
        result.append(StringUtils.padLeftOrTrim("Total", cellSize));
      }
      result.append("\n");
    }
    for (int i = 0; i < counts.length; i++) {
      // row label
      if (rowLabels != null) {
        String s = (rowLabels[i]==null ? "null" : rowLabels[i].toString());
        s = StringUtils.padOrTrim(s, labelSize); // left align this guy only
        result.append(s);
      }
      // value
      for (int j = 0; j < counts[i].length; j++) {
        result.append(StringUtils.padLeft(nf.format(counts[i][j]), cellSize));
      }
      // the row total
      if (printTotals) {
        result.append(StringUtils.padLeft(nf.format(rowTotals[i]), cellSize));
      }
      result.append("\n");
    }
    // the col totals
    if (printTotals) {
      result.append(StringUtils.pad("Total", labelSize));
      for (int j = 0; j < colTotals.length; j++) {
        result.append(StringUtils.padLeft(nf.format(colTotals[j]), cellSize));
      }
      result.append(StringUtils.padLeft(nf.format(total), cellSize));
    }
    return result.toString();
  }


  public static String toString(double[][] counts) {
    return toString(counts, 10, null, null, NumberFormat.getInstance(), false);
  }

  public static String toString(double[][] counts, int cellSize, Object[] rowLabels, Object[] colLabels, NumberFormat nf, boolean printTotals) {
    if (counts==null) return null;
    // first compute row totals and column totals
    double[] rowTotals = new double[counts.length];
    double[] colTotals = new double[counts[0].length]; // assume it's square
    double total = 0.0;
    for (int i = 0; i < counts.length; i++) {
      for (int j = 0; j < counts[i].length; j++) {
        rowTotals[i] += counts[i][j];
        colTotals[j] += counts[i][j];
        total += counts[i][j];
      }
    }
    StringBuffer result = new StringBuffer();
    // column labels
    if (colLabels != null) {
      result.append(StringUtils.padLeft("", cellSize));
      for (int j = 0; j < counts[0].length; j++) {
        String s = colLabels[j].toString();
        if (s.length() > cellSize - 1) {
          s = s.substring(0, cellSize - 1);
        }
        s = StringUtils.padLeft(s, cellSize);
        result.append(s);
      }
      if (printTotals) {
        result.append(StringUtils.padLeftOrTrim("Total", cellSize));
      }
      result.append("\n");
    }
    for (int i = 0; i < counts.length; i++) {
      // row label
      if (rowLabels != null) {
        String s = rowLabels[i].toString();
        s = StringUtils.padOrTrim(s, cellSize); // left align this guy only
        result.append(s);
      }
      // value
      for (int j = 0; j < counts[i].length; j++) {
        result.append(StringUtils.padLeft(nf.format(counts[i][j]), cellSize));
      }
      // the row total
      if (printTotals) {
        result.append(StringUtils.padLeft(nf.format(rowTotals[i]), cellSize));
      }
      result.append("\n");
    }
    // the col totals
    if (printTotals) {
      result.append(StringUtils.pad("Total", cellSize));
      for (int j = 0; j < colTotals.length; j++) {
        result.append(StringUtils.padLeft(nf.format(colTotals[j]), cellSize));
      }
      result.append(StringUtils.padLeft(nf.format(total), cellSize));
    }
    return result.toString();
  }

  public static String toString(float[][] counts) {
    return toString(counts, 10, null, null, NumberFormat.getIntegerInstance(), false);
  }

  public static String toString(float[][] counts, int cellSize, Object[] rowLabels, Object[] colLabels, NumberFormat nf, boolean printTotals) {
    // first compute row totals and column totals
    double[] rowTotals = new double[counts.length];
    double[] colTotals = new double[counts[0].length]; // assume it's square
    double total = 0.0;
    for (int i = 0; i < counts.length; i++) {
      for (int j = 0; j < counts[i].length; j++) {
        rowTotals[i] += counts[i][j];
        colTotals[j] += counts[i][j];
        total += counts[i][j];
      }
    }
    StringBuffer result = new StringBuffer();
    // column labels
    if (colLabels != null) {
      result.append(StringUtils.padLeft("", cellSize));
      for (int j = 0; j < counts[0].length; j++) {
        String s = colLabels[j].toString();
        s = StringUtils.padLeftOrTrim(s, cellSize);
        result.append(s);
      }
      if (printTotals) {
        result.append(StringUtils.padLeftOrTrim("Total", cellSize));
      }
      result.append("\n");
    }
    for (int i = 0; i < counts.length; i++) {
      // row label
      if (rowLabels != null) {
        String s = rowLabels[i].toString();
        s = StringUtils.pad(s, cellSize); // left align this guy only
        result.append(s);
      }
      // value
      for (int j = 0; j < counts[i].length; j++) {
        result.append(StringUtils.padLeft(nf.format(counts[i][j]), cellSize));
      }
      // the row total
      if (printTotals) {
        result.append(StringUtils.padLeft(nf.format(rowTotals[i]), cellSize));
      }
      result.append("\n");
    }
    // the col totals
    if (printTotals) {
      result.append(StringUtils.pad("Total", cellSize));
      for (int j = 0; j < colTotals.length; j++) {
        result.append(StringUtils.padLeft(nf.format(colTotals[j]), cellSize));
      }
      result.append(StringUtils.padLeft(nf.format(total), cellSize));
    }
    return result.toString();
  }

  /**
   * For testing only.
   * @param args
   */
  public static void main(String[] args) {
    Random random = new Random();
    int length = 100;
    double[] A = new double[length];
    double[] B = new double[length];
    double aAvg = 70.0;
    double bAvg = 70.5;
    for (int i = 0; i < length; i++) {
      A[i] = aAvg + random.nextGaussian();
      B[i] = bAvg + random.nextGaussian();
    }
    System.out.println("A has length " + A.length + " and mean " + mean(A));
    System.out.println("B has length " + B.length + " and mean " + mean(B));
    for (int t = 0; t < 10; t++) {
      System.out.println("p-value: " + sigLevelByApproxRand(A, B));
    }
  }

  public static int[][] deepCopy(int[][] counts) {
    int[][] result = new int[counts.length][];
    for (int i=0; i<counts.length; i++) {
      result[i] = new int[counts[i].length];
      for (int j=0; j<counts[i].length; j++) {
        result[i][j] = counts[i][j];
      }
    }
    return result;
  }

}
