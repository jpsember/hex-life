package cfg;

import java.io.*;
import base.*;

public class ParseTable {
  // # bits state occupies in table entry
  private static final int ACT_STATEBITS = 10;

  private static boolean COMPRESS = true;

  public ParseTable(int idStart, int idMax, int nRows) {
    this.idStart = idStart;
    this.idMax = idMax;
    this.width_ = idMax - idStart;
    this.nRows = nRows;
    this.values_ = new short[nRows * width_];
  }

  public int read(int id, int row) {
    int x = id - idStart;
    if (x < 0 || x >= width_ || row < 0 || row >= nRows) {
      Streams.out.println("index out of bounds, x=" + x + ", row=" + row);
      throw new IndexOutOfBoundsException();
    }
    int val = values_[row * width_ + x] - 1;
    return val;
  }

  public boolean put(int id, int row, int val) {
    int x = id - idStart;
    if (x < 0 || x >= width_ || row < 0 || row >= nRows)
      throw new IndexOutOfBoundsException("attempt to put id=" + id
          + ", idStart=" + idStart + ", row=" + row + ", val=" + val);
    int val2 = val + 1;
    int i = row * width_ + x;
    boolean err = false;

    int cv = values_[i];
    if (cv != val2) {
      if (cv > 0) {
        incrErrorCount();
        err = true;
      } else
        values_[i] = (short) val2;
    }

    return err;
  }

  public int width() {
    return width_;
  }

  public int height() {
    return this.nRows;
  }

  // for inserting/extracting action & state pairs to table entries:
  public static int getAction(int a) {
    return ((a) >> ACT_STATEBITS);
  }

  public static int getState(int a) {
    int val = ((a) & ((1 << ACT_STATEBITS) - 1));
    return val;
  }

  public static int makeEntry(int act) {
    return makeEntry(act, -1);
  }

  public static int makeEntry(int act, int state) {
    int val = (((act) << ACT_STATEBITS) | ((state & ((1 << ACT_STATEBITS) - 1))));
    return val;
  }

  /*	Determine # conflicts that occurred during table's construction
   */
  public int errors() {
    return errors_;
  }

  private void incrErrorCount() {
    errors_++;
  }

  void write(DataOutputStream s) throws IOException {
    s.writeShort(idStart);
    s.writeShort(idMax);
    s.writeShort(nRows);

    int zeroCount = 0;
    for (int i = 0; i < values_.length; i++) {
      short v = values_[i];
      if (COMPRESS) {
        if (v == 0)
          zeroCount++;
        if (v != 0 || zeroCount == 255) {
          if (zeroCount > 0) {
            s.writeShort(0x7f00 + zeroCount);
            zeroCount = 0;
          }
        }
        if (v != 0) {
          s.writeShort(v);
        }
      } else {
        s.writeShort(v);
      }
    }
    if (zeroCount > 0)
      s.writeShort(0x7f00 + zeroCount);
  }

  ParseTable(DataInputStream s) throws IOException {
    this(s.readShort(), s.readShort(), s.readShort());
    int i = 0;

    while (i < values_.length) {
      int k = s.readShort();
      if (COMPRESS) {
        if (k >= 0x7f00) {
          int zeroCount = k - 0x7f00;
          if (zeroCount + i > values_.length)
            throw new IOException("bad encoding");
          i += zeroCount;
          continue;
        }
      }
      values_[i++] = (short) k;
    }
  }

  public int idStart() {
    return this.idStart;
  }
  public int idMax() {
    return this.idMax;
  }
  private int idStart, idMax;
  private int nRows;
  private int width_;
  private int errors_;
  private short[] values_;
}
