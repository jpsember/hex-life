package life;

import java.util.*;
import base.*;

public class SquareLife implements ILife {

  public void setCell(IPoint2 cell, boolean value) {
    if (value) {
      if (!cells.containsKey(cell)) {
        cells.put(new IPoint2(cell), Boolean.TRUE);
        pop++;
      }
    } else {
      if (cells.remove(cell) != null)
        pop--;
    }
  }

  public int population() {
    return pop;
  }

  private int nNeighbors(IPoint2 c) {
    work.set(c.x - 1, c.y - 1);
    int nc = 0;
    if (hasCell(work))
      nc++;
    work.x++;
    if (hasCell(work))
      nc++;
    work.x++;
    if (hasCell(work))
      nc++;
    work.y++;
    if (hasCell(work))
      nc++;
    work.y++;
    if (hasCell(work))
      nc++;
    work.x--;
    if (hasCell(work))
      nc++;
    work.x--;
    if (hasCell(work))
      nc++;
    work.y--;
    if (hasCell(work))
      nc++;
    return nc;
  }

  public void update() {

    Map vn = new HashMap();
    DArray dying = new DArray();
    DArray born = new DArray();

    // check if current active cells should die; also, add
    // empty neighbors to list
    for (Iterator it = cells.keySet().iterator(); it.hasNext();) {
      IPoint2 cell = (IPoint2) it.next();

      IPoint2 n0 = new IPoint2(cell.x - 1, cell.y - 1);
      IPoint2 n1 = new IPoint2(cell.x - 1, cell.y);
      IPoint2 n2 = new IPoint2(cell.x - 1, cell.y + 1);
      IPoint2 n3 = new IPoint2(cell.x + 1, cell.y - 1);
      IPoint2 n4 = new IPoint2(cell.x + 1, cell.y);
      IPoint2 n5 = new IPoint2(cell.x + 1, cell.y + 1);
      IPoint2 n6 = new IPoint2(cell.x, cell.y - 1);
      IPoint2 n7 = new IPoint2(cell.x, cell.y + 1);

      int nc = 0;
      if (cells.containsKey(n0))
        nc++;
      else
        vn.put(n0, Boolean.TRUE);
      if (cells.containsKey(n1))
        nc++;
      else
        vn.put(n1, Boolean.TRUE);
      if (cells.containsKey(n2))
        nc++;
      else
        vn.put(n2, Boolean.TRUE);
      if (cells.containsKey(n3))
        nc++;
      else
        vn.put(n3, Boolean.TRUE);
      if (cells.containsKey(n4))
        nc++;
      else
        vn.put(n4, Boolean.TRUE);
      if (cells.containsKey(n5))
        nc++;
      else
        vn.put(n5, Boolean.TRUE);

      if (cells.containsKey(n6))
        nc++;
      else
        vn.put(n6, Boolean.TRUE);

      if (cells.containsKey(n7))
        nc++;
      else
        vn.put(n7, Boolean.TRUE);

      if ((survivalBits & (1 << nc)) == 0) //nc < 2 || nc > 3)
        dying.add(cell);
    }

    // check empty neighbors for new cells
    for (Iterator it = vn.keySet().iterator(); it.hasNext();) {
      IPoint2 cell = (IPoint2) it.next();
      if ((birthBits & (1 << nNeighbors(cell))) != 0)
        born.add(cell);
    }

    for (Iterator it = dying.iterator(); it.hasNext();) {
      IPoint2 cell = (IPoint2) it.next();
      setCell(cell, false);
    }
    for (Iterator it = born.iterator(); it.hasNext();) {
      IPoint2 cell = (IPoint2) it.next();
      setCell(cell, true);
    }
  }

  public Iterator liveCells() {
    return cells.keySet().iterator();
  }

  public boolean hasCell(IPoint2 cell) {
    return cells.containsKey(cell);
  }

//  public Iterator getLiveCells() {
//    return cells.keySet().iterator();
//  }
//  public void setRules(int survivalBits, int birthBits) {
//    this.survivalBits = survivalBits;
//    this.birthBits = birthBits;
//  }
//  private int survivalBits = (1 << 2) | (1 << 3);
//  private int birthBits = (1 << 3);
  private Map cells = new HashMap();
  private int pop;
  private IPoint2 work = new IPoint2();
//  public int getBirthBits() {
//    return birthBits;
//  }
//
//  public int getSurvivalBits() {
//    return survivalBits;
//  }
  
  public void setRules(int bits) {
    this.survivalBits = (bits & 0x7fff);
    this.birthBits = (bits >> 16) & 0x7fff;
  }
  public int getRules() {
    return survivalBits | (birthBits << 16);
  }

  private int survivalBits = (1 << 2) | (1 << 3);
  private int birthBits = (1 << 3);

}
