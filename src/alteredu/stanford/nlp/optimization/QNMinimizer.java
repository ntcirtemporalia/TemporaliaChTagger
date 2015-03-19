package alteredu.stanford.nlp.optimization;

import alteredu.stanford.nlp.math.ArrayMath;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Limited-Memory Quasi-Newton BFGS implementation based on the algorithms in
 * <p>
 * Nocedal, Jorge, and Stephen J. Wright.  2000.  Numerical Optimization.  Springer.  pp. 224--
 * <p>
 * As of now, it requires a
 * differentiable function (DiffFunction) as input.
 * <p/>
 * The basic way to use the minimizer is with a null constructor, then
 * the simple minimize method:
 * <p/>
 * <p><code>Minimizer qnm = new QNMinimizer();</code>
 * <br><code>DiffFunction df = new SomeDiffFunction();</code>
 * <br><code>double tol = 1e-4;</code>
 * <br><code>double[] initial = getInitialGuess();</code>
 * <br><code>double[] minimum = qnm.minimize(df,tol,initial);</code>
 * <p/>
 * <p/>
 * If you do not choose a value of M, it will use the max amount of memory
 * available, up to M of 20.  This will slow things down a bit at first due
 * to forced garbage collection, but is probably faster overall b/c you are
 * guaranteed the largest possible M.
 *
 * @author <a href="mailto:jrfinkel@stanford.edu">Jenny Finkel</a>
 * @author Galen Andrew
 * @version 1.0
 * @since 1.0
 */
public class QNMinimizer implements Minimizer {

  private int k;
  private int M = 0;

  private boolean quiet = false;

  public void shutUp() {
    this.quiet = true;
  }

  public void setM(int m) {
    M = m;
  }

  private Function monitor;
  private FloatFunction floatMonitor;

  private static NumberFormat nf = new DecimalFormat("0.000E0");

  public QNMinimizer(int m) {
    M = m;
  }

  public QNMinimizer() {
  }

  public QNMinimizer(Function monitor) {
    this.monitor = monitor;
  }

  public QNMinimizer(Function monitor, int m) {
    this.monitor = monitor;
    M = m;
  }

  public QNMinimizer(FloatFunction monitor) {
    this.floatMonitor = monitor;
  }

  // computes d = a + b * c
  private static double[] plusAndConstMult(double[] a, double[] b, double c, double[] d) {
    for (int i = 0; i < a.length; i++) {
      d[i] = a[i] + c * b[i];
    }
    return d;
  }

  private List<double[]> sList = new ArrayList<double[]>();
  private List<double[]> yList = new ArrayList<double[]>();
  private List<Double> roList = new ArrayList<Double>();

  private void computeDir(double[] dir, double[] fg) throws SurpriseConvergence {
    System.arraycopy(fg, 0, dir, 0, fg.length);

    int mmm = sList.size();
    double[] as = new double[mmm];

    for (int i = mmm - 1; i >= 0; i--) {
      as[i] = roList.get(i) * ArrayMath.innerProduct(sList.get(i), dir);
      plusAndConstMult(dir, yList.get(i), -as[i], dir);
    }

    // multiply by hessian approximation
    if (mmm != 0) {
      double[] y = yList.get(mmm - 1);
      double yDotY = ArrayMath.innerProduct(y, y);
      if (yDotY == 0) {
        throw new SurpriseConvergence("Y is 0!!");
      }
      double gamma = ArrayMath.innerProduct(sList.get(mmm - 1), y) / yDotY;
      ArrayMath.multiplyInPlace(dir, gamma);
    }

    for (int i = 0; i < mmm; i++) {
      double b = roList.get(i) * ArrayMath.innerProduct(yList.get(i), dir);
      plusAndConstMult(dir, sList.get(i), as[i] - b, dir);
    }

    ArrayMath.multiplyInPlace(dir, -1);
  }

  public double[] minimize(Function function, double functionTolerance, double[] initial) {
			return minimize(function, functionTolerance, initial, -1);
	}

	public double[] minimize(Function function, double functionTolerance, double[] initial, int maxIterations) {
    say("QNMinimizer called on double function of " + function.domainDimension() + " variables;");
    if (M > 0) {
      sayln(" Using m = " + M);
    } else {
      sayln(" Using dynamic setting of M.");
    }

    // check for derivatives
    if (!(function instanceof DiffFunction)) {
      throw new UnsupportedOperationException();
    }
    DiffFunction dfunction = (DiffFunction) function;

    // for convergence test
    Queue<Double> previousVals = new LinkedList<Double>();

    double[] x, newX, grad, newGrad, dir;
    double ro, value;

    // initialize matrix
    x = initial;

    // initialize function value
    value = dfunction.valueAt(x);

    if (monitor != null) {
      monitor.valueAt(x);
    }

    // initialize gradient
    grad = new double[x.length];
    System.arraycopy(dfunction.derivativeAt(x), 0, grad, 0, grad.length);
    newGrad = new double[x.length];

    newX = new double[x.length];
    dir = new double[x.length];

    sList = new ArrayList<double[]>();
    yList = new ArrayList<double[]>();
    roList = new ArrayList<Double>();
    double[] nextS;
    double[] nextY;

    //    double initGradNorm = ArrayMath.norm(grad);
    sayln("Iter: n <chooseDir> [(derivInDir) chooseNewPoint] newValue (relAvgImprovement)\n");

    boolean have_max = (maxIterations > 0);
    for (k = 0; ; k++) {

      double newValue = 0;
      try {
        say("Iter: " + k + " ");

        // compute search direction (dir)
        say("<");
        try {
          computeDir(dir, grad);
        } catch (SurpriseConvergence s) {
          clearStuff();
          return x;
        }
        say("> ");

        if (M > 0 && sList.size() == M || sList.size() == 20) {
          nextS = sList.remove(0);
          nextY = yList.remove(0);
          roList.remove(0);
        } else {
          nextS = new double[x.length];
          nextY = new double[x.length];
        }

        // perform line search
        say("[");
        newValue = lineSearch(dfunction, dir, x, newX, grad, value);
        say("] ");
      } catch (OutOfMemoryError e) {
        sayln(" --- Reached memory limit.  Setting m and redoing iteration...");
        M = sList.size();
        k--;
        continue;
      }

      System.arraycopy(dfunction.derivativeAt(newX), 0, newGrad, 0, newGrad.length);

      say(nf.format(newValue));

      // compute s_k, y_k
      plusAndConstMult(newX, x, -1, nextS);
      plusAndConstMult(newGrad, grad, -1, nextY);
      ro = 1.0 / ArrayMath.innerProduct(nextS, nextY);
      sList.add(nextS);
      yList.add(nextY);
      roList.add(ro);

      previousVals.add(value);
      int size = previousVals.size();
      double previousVal = size == 10 ? previousVals.remove() : previousVals.peek();
      double averageImprovement = (previousVal - newValue) / size;

      sayln(" (" + nf.format(averageImprovement / newValue) + ")");

      if ((size > 5 && averageImprovement / newValue < functionTolerance) ||
					(have_max && k >= maxIterations)) {
        clearStuff();
        return newX;
      }

      if (monitor != null) {
        monitor.valueAt(newX);
      }

      // shift
      value = newValue;
      double[] temp = x;
      x = newX;
      newX = temp;

      System.arraycopy(newGrad, 0, grad, 0, newGrad.length);
      if (quiet) {
        System.err.print(".");
      }
    }
  }

  private double lineSearch(Function func, double[] dir, double[] x, double[] newX, double[] grad, double lastValue) {

    double normGradInDir = ArrayMath.innerProduct(dir, grad);
    say("(" + nf.format(normGradInDir) + ")");
    if (normGradInDir > 0) {
      say("{WARNING--- direction of positive gradient chosen!}");
    }

    // c1 can be anything between 0 and 1, exclusive (usu. 1/10 - 1/2)
    double a, c1;

    // for first few steps, we have less confidence in our initial step-size a so scale back quicker
    if (k <= 2) {
      a = 0.1;
      c1 = 0.1;
    } else {
      a = 1.0;
      c1 = 0.1;
    }

    // should be small e.g. 10^-5 ... 10^-1
    double c = 0.01;

    //double v = func.valueAt(x);
    //c = c * mult(grad, dir);
    c = c * normGradInDir;

    double newValue;

    while ((newValue = func.valueAt((plusAndConstMult(x, dir, a, newX)))) > lastValue + c * a) {
      if (newValue < lastValue) {
        // an improvement, but not good enough... suspicious!
        say("!");
      } else {
        say(".");
      }
      a = c1 * a;
    }

    return newValue;
  }

  /**
   * FLOAT BEGINS HERE
   */

  // computes d = a + b * c
  private static float[] plusAndConstMult(float[] a, float[] b, float c, float[] d) {
    for (int i = 0; i < a.length; i++) {
      d[i] = a[i] + c * b[i];
    }
    return d;
  }

  private List<float[]> sList_float = new ArrayList<float[]>();
  private List<float[]> yList_float = new ArrayList<float[]>();
  private List<Float> roList_float = new ArrayList<Float>();

  private void computeDir(float[] dir, float[] fg) throws SurpriseConvergence {
    System.arraycopy(fg, 0, dir, 0, fg.length);

    int mmm = sList_float.size();
    float[] as = new float[mmm];

    for (int i = mmm - 1; i >= 0; i--) {
      as[i] = roList_float.get(i) * (float) ArrayMath.innerProduct(sList_float.get(i), dir);
      plusAndConstMult(dir, yList_float.get(i), -as[i], dir);
    }

    // multiply by hessian approximation
    if (mmm != 0) {
      float[] y = yList_float.get(mmm - 1);
      float yDotY = (float) ArrayMath.innerProduct(y, y);
      if (yDotY == 0) {
        throw new SurpriseConvergence("Y is 0!!");
      }
      float gamma = (float) ArrayMath.innerProduct(sList_float.get(mmm - 1), y) / yDotY;
      ArrayMath.multiplyInPlace(dir, gamma);
    }

    for (int i = 0; i < mmm; i++) {
      float b = roList_float.get(i) * (float) ArrayMath.innerProduct(yList_float.get(i), dir);
      plusAndConstMult(dir, sList_float.get(i), as[i] - b, dir);
    }

    ArrayMath.multiplyInPlace(dir, -1);
  }

  public float[] minimize(FloatFunction function, float functionTolerance, float[] initial) {
    say("QNMinimizer called on float function of " + function.domainDimension() + " variables;");
    if (M > 0) {
      sayln(" Using m = " + M);
    } else {
      sayln(" Using dynamic setting of M.");
    }

    // check for derivatives
    if (!(function instanceof DiffFloatFunction)) {
      throw new UnsupportedOperationException();
    }
    DiffFloatFunction dfunction = (DiffFloatFunction) function;

    // for convergence test
    Queue<Float> previousVals = new LinkedList<Float>();

    float[] x, newX, grad, newGrad, dir;
    float ro, value;

    // initialize matrix
    x = initial;

    // initialize function value
    value = dfunction.valueAt(x);

    if (monitor != null) {
      floatMonitor.valueAt(x);
    }

    // initialize gradient
    grad = new float[x.length];
    System.arraycopy(dfunction.derivativeAt(x), 0, grad, 0, grad.length);
    newGrad = new float[x.length];

    newX = new float[x.length];
    dir = new float[x.length];

    sList_float = new ArrayList<float[]>();
    yList_float = new ArrayList<float[]>();
    roList_float = new ArrayList<Float>();
    float[] nextS;
    float[] nextY;

    //    float initGradNorm = ArrayMath.norm(grad);
    sayln("Iter: n <chooseDir> [(derivInDir) chooseNewPoint] newValue (relAvgImprovement)\n");

    // should set a MAX_IT
    for (k = 0; ; k++) {

      float newValue = 0;
      try {
        say("Iter: " + k + " ");

        // compute search direction (dir)
        say("<");
        try {
          computeDir(dir, grad);
        } catch (SurpriseConvergence s) {
          clearStuff();
          return x;
        }
        say("> ");

        if (M > 0 && sList_float.size() == M || sList_float.size() == 20) {
          nextS = sList_float.remove(0);
          nextY = yList_float.remove(0);
          roList_float.remove(0);
        } else {
          nextS = new float[x.length];
          nextY = new float[x.length];
        }

        // perform line search
        say("[");
        newValue = lineSearch(dfunction, dir, x, newX, grad, value);
        say("] ");
      } catch (OutOfMemoryError e) {
        sayln(" --- Reached memory limit.  Setting m and redoing iteration...");
        M = sList_float.size();
        k--;
        continue;
      }

      System.arraycopy(dfunction.derivativeAt(newX), 0, newGrad, 0, newGrad.length);

      say(nf.format(newValue));

      // compute s_k, y_k
      plusAndConstMult(newX, x, -1, nextS);
      plusAndConstMult(newGrad, grad, -1, nextY);
      ro = (float) (1.0 / ArrayMath.innerProduct(nextS, nextY));
      sList_float.add(nextS);
      yList_float.add(nextY);
      roList_float.add(ro);

      previousVals.add(value);
      int size = previousVals.size();
      float previousVal = size == 10 ? previousVals.remove() : previousVals.peek();
      float averageImprovement = (previousVal - newValue) / size;

      sayln(" (" + nf.format(averageImprovement / newValue) + ")");

      if (size > 5 && averageImprovement / newValue < functionTolerance) {
        clearStuff();
        return newX;
      }

      if (monitor != null) {
        floatMonitor.valueAt(newX);
      }

      // shift
      value = newValue;
      float[] temp = x;
      x = newX;
      newX = temp;

      System.arraycopy(newGrad, 0, grad, 0, newGrad.length);
      if (quiet) {
        System.err.print(".");
      }
    }
  }

  private float lineSearch(FloatFunction func, float[] dir, float[] x, float[] newX, float[] grad, float lastValue) {

    float normGradInDir = (float) ArrayMath.innerProduct(dir, grad);
    say("(" + nf.format(normGradInDir) + ")");
    if (normGradInDir > 0) {
      say("{WARNING--- direction of positive gradient chosen!}");
    }

    // c1 can be anything between 0 and 1, exclusive (usu. 1/10 - 1/2)
    float a, c1;

    // for first few steps, we have less confidence in our initial step-size a so scale back quicker
    if (k <= 2) {
      a = 0.1f;
      c1 = 0.1f;
    } else {
      a = 1.0f;
      c1 = 0.5f;
    }

    // should be small e.g. 10^-5 ... 10^-1
    float c = 0.01f;

    //float v = func.valueAt(x);
    //c = c * mult(grad, dir);
    c = c * normGradInDir;

    float newValue;

    while ((newValue = func.valueAt((plusAndConstMult(x, dir, a, newX)))) > lastValue + c * a) {
      if (newValue < lastValue) {
        // an improvement, but not good enough... suspicious!
        say("!");
      } else {
        say(".");
      }
      a = c1 * a;
    }

    return newValue;
  }

  /**
   * STUFF FOR DOUBLE AND FLOAT
   */

  private void clearStuff() {
    sList = null;
    yList = null;
    roList = null;
    sList_float = null;
    yList_float = null;
    roList_float = null;
  }

  private static class SurpriseConvergence extends Throwable {
    public SurpriseConvergence(String s) {
      super(s);
    }
  }

  private void sayln(String s) {
    if (!quiet) {
      System.err.println(s);
    }
  }

  private void say(String s) {
    if (!quiet) {
      System.err.print(s);
    }
  }

  public static void main(String[] args) {
    // optimizes test function using doubles and floats
    // test function is (0.5 sum(x_i^2 * var_i)) ^ PI
    // where var is a vector of random nonnegative numbers
    // dimensionality is variable.
    final int dim = 500000;
    final double maxVar = 5;
    final double[] var = new double[dim];
    final float[] varF = new float[dim];
    double[] init = new double[dim];
    for (int i = 0; i < dim; i++) {
      init[i] = ((i + 1) / (double) dim - 0.5);//init[i] = (Math.random() - 0.5);
      var[i] = maxVar * (i + 1) / (double) dim;
      varF[i] = (float) var[i];
    }
    float[] initF = ArrayMath.doubleArrayToFloatArray(init);

    final double[] grads = new double[dim];
    final float[] gradsF = new float[dim];

    final DiffFunction f = new DiffFunction() {
      public double[] derivativeAt(double[] x) {
        double val = Math.PI * valuePow(x, Math.PI - 1);
        for (int i = 0; i < dim; i++) {
          grads[i] = x[i] * var[i] * val;
        }
        return grads;
      }

      public double valueAt(double[] x) {
        return 1.0 + valuePow(x, Math.PI);
      }

      private double valuePow(double[] x, double pow) {
        double val = 0.0;
        for (int i = 0; i < dim; i++) {
          val += x[i] * x[i] * var[i];
        }
        return Math.pow(val * 0.5, pow);
      }

      public int domainDimension() {
        return dim;
      }
    };

    final DiffFloatFunction fF = new DiffFloatFunction() {
      public float[] derivativeAt(float[] x) {
        float val = (float) Math.PI * valuePow(x, Math.PI - 1);
        for (int i = 0; i < dim; i++) {
          gradsF[i] = x[i] * varF[i] * val;
        }
        return gradsF;
      }

      public float valueAt(float[] x) {
        return 1.0f + valuePow(x, Math.PI);
      }

      private float valuePow(float[] x, double pow) {
        float val = 0.0f;
        for (int i = 0; i < dim; i++) {
          val += x[i] * x[i] * varF[i];
        }
        return (float) Math.pow(val * 0.5, pow);
      }

      public int domainDimension() {
        return dim;
      }
    };

    QNMinimizer min = new QNMinimizer();

    System.out.println("-------------------------");
    System.out.println("-----               -----");
    System.out.println("-----    DOUBLE     -----");
    System.out.println("-----               -----");
    System.out.println("-------------------------");
    System.out.println();

    min.minimize(f, 1.0E-4, init);

    System.out.println("-------------------------");
    System.out.println("-----               -----");
    System.out.println("-----     FLOAT     -----");
    System.out.println("-----               -----");
    System.out.println("-------------------------");
    System.out.println();

    min.setM(0);
    min.minimize(fF, 1.0E-4f, initF);
  }

}
