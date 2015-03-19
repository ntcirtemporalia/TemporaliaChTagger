package alteredu.stanford.nlp.sequences;

/**
 * @author grenager
 *         Date: Dec 14, 2004
 */
public abstract class CoolingSchedule {

  public abstract int numIterations();
  public abstract double getTemperature(int iteration);

  public static CoolingSchedule getExponentialSchedule(final double start, final double rate, final int numIterations) {
    return new CoolingSchedule() {
      public int numIterations() {
        return numIterations;
      }
      public double getTemperature(int iteration) {
        return start * Math.pow(rate, (double) iteration);
      }
    };
  }

  public static CoolingSchedule getLinearSchedule(final double start, final int numIterations) {
    return new CoolingSchedule() {
      final double rate = start / (double)numIterations;
      public int numIterations() {
        return numIterations+1; // will hit zero on the last one
      }
      public double getTemperature(int iteration) {
        return start - rate*(double)iteration;
      }
    };
  }

}
