package alteredu.stanford.nlp.util;

public class IntQuadruple extends IntTuple {

  private static final long serialVersionUID = 7154973101012473479L;


  public IntQuadruple() {
    elements = new int[4];
  }

  public IntQuadruple(int src, int mid, int trgt, int trgt2) {
    elements = new int[4];
    elements[0] = src;
    elements[1] = mid;
    elements[2] = trgt;
    elements[3] = trgt2;
  }


  public IntTuple getCopy() {
    IntQuadruple nT = new IntQuadruple(elements[0], elements[1], elements[2], elements[3]);
    return nT;
  }


  public int getSource() {
    return get(0);
  }

  public int getTarget() {
    return get(2);
  }

  public int getTarget2() {
    return get(3);
  }

  public int getMiddle() {
    return get(1);
  }


  public int hashCode() {
    return (get(0) << 20) ^ (get(1) << 10) ^ (get(2) << 5) ^ (get(3));
  }

}
