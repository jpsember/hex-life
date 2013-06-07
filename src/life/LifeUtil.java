package life;

import java.io.*;
import java.util.*;
import testbed.*;
import base.*;
import cfg.*;

public class LifeUtil {

  public static ILife readRLE(String f) throws IOException {
    if (true)
      return readRLESimple(f);

    final boolean db = false;

    if (db && T.update())
      T.msg("readRLE: " + f);
    ILife life = null;

    DFA dfa = DFA.readFromSet(LifeMain.class, "rle.dfa");
    CFG cfg2 = CFG.read(LifeMain.class, "rle.cfb", dfa);
    {
      Parser parser = cfg2.getParserFor(Streams.reader(f), f);
      Token t = parser.parse();
//      if (db)
//        Streams.out.println("parse tree=\n" + parser.tree(t));

      ILife lf = null;

      int childNum = 0;
      if (parser.child(t, childNum).id(IRLE.COMMENTS)) {
        Token t2 = parser.child(t, childNum);
        childNum++;

        for (int j = 0; j < parser.nChildren(t2); j++) {
          String ct = parser.child(t2, j).text();
          if (db)
            Streams.out.println("read comment #" + j + ": " + ct);

          ct = ct.substring(2).trim();
          if (ct.equals("<H>")) {
            lf = new HexLife();
          }
        }
      }

      if (lf == null)
        lf = new SquareLife();

      Token t2 = parser.child(t, childNum++);
      // parse header?  
      //      {
      //        String hdr = t2.text();
      //        int c = 0;
      //        while (c < hdr.length()) {
      //          int next = hdr.indexOf(',', c);
      //          if (next < 0)
      //            next = hdr.length();
      //          String line = hdr.substring(c, next).trim().toLowerCase();
      //          c = next + 1;
      // int equ = line.indexOf('=');
      //          if (equ < 0) continue;
      //          
      //          if (line.startsWith("rule")) {
      //            int
      //          }
      //        }
      //
      //        Tools.unimp("parse header: " + hdr);
      //        if (db)
      //          Streams.out.println("header= " + t2);
      //      }

      IPoint2 loc = new IPoint2();

      t2 = parser.child(t, childNum++);
      if (db)
        Streams.out.println("tags=" + t2);

      int len = 1;
      for (int i = 0; i < parser.nChildren(t2); i++) {
        Token t3 = parser.child(t2, i);
        if (db)
          Streams.out.println("tag #" + i + "= " + t3);

        String s = parser.child(t3).text().toLowerCase();
        if (db)
          Streams.out.println("  tag=" + s);

        if (s.equals("b")) {
          for (int j = 0; j < len; j++) {
            lf.setCell(loc, false);
            loc.x++;
          }
          len = 1;
        } else if (s.equals("o")) {
          for (int j = 0; j < len; j++) {
            lf.setCell(loc, true);
            loc.x++;
          }
          len = 1;
        } else if (Character.isDigit(s.charAt(0))) {
          len = Integer.parseInt(s);
        } else if (s.equals("$")) {
          loc.x = 0;
          loc.y -= len;
          len = 1;
        } else {
          parser.child(t3).exception("unknown tag");
        }
      }
      life = lf;
    }
    return life;
  }

  public static ILife readRLESimple(String f) throws IOException {

    final boolean db = false;

    if (db && T.update())
      T.msg("readRLE: " + f);
    ILife life = new SquareLife();

    DFA dfa = DFA.readFromSet(LifeMain.class, "rle.dfa");

    TextScanner sc = new TextScanner(Streams.reader(f), f, dfa, IRLE.ws);
    sc.setTrace(db);
    sc.setEcho(db);

    int len = 1;
    IPoint2 loc = new IPoint2();

    outer: while (true) {
      Token t = sc.read();
      if (db)
        Streams.out.println("read token: " + t);

      if (t.eof())
        break;
      String tx = t.text();
      switch (t.id()) {
      case IRLE.comment:
        if (tx.substring(2).trim().equals("<H>")) {
          life = new HexLife();
        }
        break;
      case IRLE.runcount:
        len = Integer.parseInt(tx);
        break;
      case IRLE.eoln:
        loc.y += len;
        loc.x = 0;
        len = 1;
        break;
      case IRLE.eofile:
        break outer;
      case IRLE.tag:
        {
          char c = Character.toLowerCase(tx.charAt(0));
          boolean color = (c == 'o');
          for (int i = 0; i < len; i++) {
            life.setCell(loc, color);
            loc.x++;
          }
          len = 1;
        }
        break;
      }
    }
    sc.close();
    return life;
  }

  public static ILife readLif(String f) throws IOException {

    final boolean db = false;

    ILife life = null;

    if (db)
      Streams.out.println("reading .lif file: " + f);

    DFA dfa = DFA.readFromSet(LifeMain.class, "lif.dfa");
    CFG cfg2 = CFG.read(LifeMain.class, "lif.cfb", dfa);
    {
      Parser parser = cfg2.getParserFor(Streams.reader(f), f);
      //  parser.setTrace(db);

      Token t = parser.parse();
//      if (db)
//        Streams.out.println("tree=\n" + parser.tree(t));

      ILife lf = null;

      int childNum = 0;
      if (parser.child(t, childNum).id(IScript.COMMENTS)) {
        Token t2 = parser.child(t, childNum);
        childNum++;

        for (int j = 0; j < parser.nChildren(t2); j++) {
          String ct = parser.child(t2, j).text();
          ct = ct.substring(2).trim();
          if (ct.equals("<H>")) {
            lf = new HexLife();
          }
        }
      }

      //      Streams.out.println("tree:\n" + parser.tree());
      if (lf == null)
        lf = new SquareLife();

      Token t2 = parser.child(t, childNum++);
      t2 = parser.child(t2, 0);
      if (t2.id(IScript.DEFRULES)) {
      } else {
        t2 = parser.child(t2, 1);
        String rspec = t2.text();
        int bits = parseRules(rspec);

        if (bits == 0)
          t2.exception("bad rules format");

        lf.setRules(bits);
      }

      t2 = parser.child(t, childNum++);
      for (int i = 0; i < parser.nChildren(t2); i++) {
        Token t3 = parser.child(t2, i);

        int x = Integer.parseInt(parser.child(t3, 0).text());
        int y = Integer.parseInt(parser.child(t3, 1).text());
        if (db)
          Streams.out.println("next block is at x=" + x + " y=" + y);
        String data = parser.child(t3, 2).text();

        int cx = x;
        int cy = y;
        boolean prevcr = false;
        for (int j = 0; j < data.length(); j++) {
          char c = data.charAt(j);
          switch (c) {
          case 0x0a:
          case 0x0d:
            if (!prevcr) {
              prevcr = true;
              cy++;
              cx = x;
            }
            break;
          case '*':
          case '.':
            prevcr = false;
            lf.setCell(new IPoint2(cx, cy), c == '*');
            cx++;
            break;
          }
        }
      }
      life = lf;
    }
    return life;
  }

  private static class Block {
    // must be a power of 2 
    public static final int SIZE = 8;

    public Block(IPoint2 origin) {
      this.origin = origin;
    }
    public void add(IPoint2 cell) {
      int bitIndex = (cell.x - origin.x) + (cell.y - origin.y) * SIZE;
      if (bitIndex < 0 || bitIndex >= SIZE * SIZE)
        throw new IllegalArgumentException();
      bits.set(bitIndex);
    }

    public void write(PrintWriter pw) {
      // determine smallest rectangle in block containing all live cells
      int x0 = SIZE, x1 = -1, y0 = SIZE, y1 = -1;
      int ind = 0;
      for (int y = 0; y < SIZE; y++) {
        for (int x = 0; x < SIZE; x++, ind++) {
          if (bits.get(ind)) {
            x0 = Math.min(x0, x);
            x1 = Math.max(x1, x);
            y0 = Math.min(y0, y);
            y1 = Math.max(y1, y);
          }
        }
      }

      pw.write("#P ");
      pw.print(origin.x + x0);
      pw.write(' ');
      pw.print(origin.y + y0);
      pw.write('\n');

      ind = (y0 * SIZE);
      for (int y = y0; y <= y1; y++, ind += SIZE) {
        int len = 1;
        for (int x = x0; x <= x1; x++) {
          if (bits.get(ind + x))
            len = (x + 1) - x0;
        }

        for (int x = x0; x < x0 + len; x++) {
          if (bits.get(ind + x))
            pw.write('*');
          else
            pw.write('.');
        }
        pw.println();
      }
    }
    private IPoint2 origin;
    private BitSet bits = new BitSet(SIZE * SIZE);
  }

  public static void writeLif(ILife life, String f) throws IOException {

    PrintWriter pw = new PrintWriter(Streams.writer(f));

    try {

      Iterator it = life.liveCells();
      Map blockMap = new HashMap();

      pw.println("#Life");

      boolean hex = (life instanceof HexLife);
      if (hex)
        pw.println("#D <H>");

      {
        String s = compileRules(life.getRules());
        if (s.equals("23/3"))
          pw.println("#N");
        else
          pw.println("#R " + s);
      }

      while (it.hasNext()) {
        IPoint2 cell = (IPoint2) it.next();
        //  cell.y = cell.y;

        int blockx = cell.x & ~(Block.SIZE - 1);
        int blocky = cell.y & ~(Block.SIZE - 1);
        IPoint2 blkKey = new IPoint2(blockx, blocky);
        Block block = (Block) blockMap.get(blkKey);
        if (block == null) {
          block = new Block(blkKey);
          blockMap.put(blkKey, block);
        }
        block.add(cell);
      }
      for (it = blockMap.keySet().iterator(); it.hasNext();) {
        Block block = (Block) blockMap.get(it.next());
        block.write(pw);
      }
    } finally {
      pw.close();
    }
  }

  /**
   * Get minimum bounding rectangle of live cells
   * @param life
   * @return array with two IPoints: the bottom left, and the width and height;
   *   or null if no live cells
   */
  public static IPoint2[] getLiveBounds(ILife life) {
    IPoint2[] ret = null;
    int x0 = 0, x1 = 0, y0 = 0, y1 = 0;
    for (Iterator it = life.liveCells(); it.hasNext();) {
      IPoint2 cell = (IPoint2) it.next();
      if (ret == null) {
        ret = new IPoint2[2];
        x0 = x1 = cell.x;
        y0 = y1 = cell.y;
      }
      x0 = Math.min(x0, cell.x);
      x1 = Math.max(x1, cell.x);
      y0 = Math.min(y0, cell.y);
      y1 = Math.max(y1, cell.y);
    }
    if (ret != null) {
      ret[0] = new IPoint2(x0, y0);
      ret[1] = new IPoint2(x1 + 1 - x0, y1 + 1 - y0);
    }
    return ret;
  }

  public static String compileRules(int bits) {

    //int surviveBits, int birthBits) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 2; i++) {
      int f = ((i == 0) ? bits : (bits >> 16)) & 0xffff;
      if (i > 0)
        sb.append('/');
      int j = 0;
      while (f != 0) {
        if ((f & 1) != 0)
          sb.append((char) ('0' + j));
        j++;
        f >>= 1;
      }
    }
    return sb.toString();
  }

  /**
   * Parse a string as survive/birth counts
   * @param s string of format "ddd/ddd", where each d is neighbor count
   * @return rules, or 0 if parse error
   */
  public static int parseRules(String s) {
    final boolean db = false;
    if (db)
      Streams.out.println("parseRules: " + s);

    int ret = 0;

    int survivalBits = 0;
    int birthBits = 0;
    int state = 0;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '/') {
        if (state != 1) {
          state = -1;
          break;
        }
        state = 2;
      } else {
        int val = c - '0';
        if (val < 0 || val > 9) {
          state = -1;
          break;
        }
        if (state == 0 || state == 1) {
          state = 1;
          survivalBits |= 1 << val;
        } else {
          state = 3;
          birthBits |= 1 << val;
        }
      }
    }
    if (state == 3) {
      ret = (survivalBits | (birthBits << 16));
      if (db)
        Streams.out.println(compileRules(ret));
    }
    return ret;
  }

  private static IPoint2[] defaultBounds = { new IPoint2(0, 0),
      new IPoint2(1, 1), };

  public static void writeRLE(ILife life, String f) throws IOException {
    PrintWriter pw = new PrintWriter(Streams.writer(f));
    try {
      IPoint2[] b = getLiveBounds(life);
      if (b == null)
        b = defaultBounds;
      int w = b[1].x;
      int h = b[1].y;
      int x0 = b[0].x;
      int y0 = b[0].y;

      boolean hex = life instanceof HexLife;

      if (hex)
        pw.println("#C <H>");
      pw.println("x = " + w + ", y = " + h);

      IPoint2 prevCell = new IPoint2(x0, y0);
      boolean prevState = false;

      // # chars printed on current line; we need to insert linefeeds
      // every so often
      int charsPrinted = 0;

      IPoint2 cell = new IPoint2();

      for (int y = y0; y < y0 + h; y++) {
        cell.y = y;

        // read one past live cells, so all live cells are flushed from
        // current row
        for (int x = x0; x < x0 + w + 1; x++) {
          cell.x = x;

          boolean state = life.hasCell(cell);

          // if state has changed, flush old
          if (state != prevState) {
            // add blank rows?
            if (prevCell.y != cell.y) {
              int cnt = prevCell.y - cell.y;
              if (cnt > 1) {
                pw.print(cnt);
                charsPrinted++;
              }
              pw.print('$');
              charsPrinted++;
              prevCell.y = cell.y;
              prevCell.x = x0;
            }
            // add n cells for current row
            int cnt = cell.x - prevCell.x;
            if (cnt > 0) {
              if (cnt > 1) {
                pw.print(cnt);
                charsPrinted++;
              }
              pw.print(prevState ? 'o' : 'b');
              charsPrinted++;
            }
            prevCell.x = cell.x;
            prevState = state;

            if (charsPrinted >= 65) {
              pw.println();
              charsPrinted = 0;
            }
          }
        }
      }
      pw.println('!');
    } finally {
      pw.close();
    }
  }
}
