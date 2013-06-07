package life;

import testbed.*;
import base.*;

public class HexGrid implements LGrid {

  private static final double RT3 = Math.sqrt(3);

  public void prepare(double pscale, IPoint2 origin, FPoint2 wOrigin) {

    this.pixelSize = pscale; // * 2.5;
    this.highDef = pixelSize >= 1.5;

    Matrix gridOffset = Matrix.getTranslate(origin, true);

    Matrix skew;
    {
      Matrix m = new Matrix(3);
      m.setIdentity();
      m.set(1, 0, 1);
      m.set(1, 1, 2);
      skew = m;
    }
    Matrix scaleToWorld;
    {
      Matrix m = Matrix.getScale(pixelSize, pixelSize / RT3);
      scaleToWorld = m;
    }

    Matrix offsetWorld;
    {
      Matrix m = Matrix.getTranslate(wOrigin, false);
      offsetWorld = m;
    }
    Matrix m = offsetWorld;
    Matrix.mult(m, scaleToWorld, m);
    Matrix.mult(m, skew, m);
    Matrix.mult(m, gridOffset, m);
    toView = m;
    //    toView = Matrix.mult(m, m3, null);
    toGrid = toView.invert(null);
    preu = toView.get(0, 0) * 2 / 3.0;
    prev = toView.get(1, 1) / 2;
  }
  public HexGrid() {
  }

  /**
   * Render grid
   * @param V : vp
   * @param withLabels : true to label cells
   */
  public void render() {
    final boolean db = false;

    if (highDef) {
      if (db)
        Streams.out.println("HexGrid.render");

      // calculate the extreme visible cells
      FRect r = V.viewRect;
      r = new FRect(r);
      r.inset(-pixelSize);

      IPoint2 pTopLeft = toBoard(r.topLeft(), null);
      IPoint2 pBotLeft = toBoard(r.bottomLeft(), null);
      IPoint2 pBotRight = toBoard(r.bottomRight(), null);

      //    pTopLeft.x -= 2;
      //    pTopLeft.y += 2;
      //    pBotRight.x += 2;
      //    pBotRight.x -= 2;
      //    

      if (db)
        Streams.out.println("extreme vis=" + pTopLeft + ", " + pBotRight);

      FPoint2 wa = toWorld(pTopLeft.x, pTopLeft.y);
      FPoint2 wb = toWorld(pTopLeft.x + 1, pTopLeft.y - 1);

      double sx = wb.x - wa.x;
      double sy = (wb.y - wa.y);

      int xc = pBotRight.x + 1 - pTopLeft.x;
      int yc = pTopLeft.y + 1 - pBotLeft.y;

      if (db)
        Streams.out.println("xc=" + xc + " yc=" + yc + " wa=" + wa + " wb="
            + wb + " sy=" + sy);

      if (db)
        V.pushColor(MyColor.cPURPLE);

      double wx = wa.x;
      for (int x = 0; x < xc; x += 2, wx += sx * 2) {
        double wy = wa.y;
        for (int y = 0; y < yc; y++, wy += sy * 2) {
          double x0 = wx - preu * .5, y0 = wy + prev;
          double x1 = wx - preu, y1 = wy;
          double x2 = x0, y2 = wy - prev;
          double x3 = x2 + preu, y3 = y2;
          V.drawLine(x0, y0, x1, y1);
          V.drawLine(x1, y1, x2, y2);
          V.drawLine(x2, y2, x3, y3);

          x0 += sx;
          x1 += sx;
          x2 += sx;
          x3 += sx;
          y0 += sy;
          y1 += sy;
          y2 += sy;
          y3 += sy;
          V.drawLine(x0, y0, x1, y1);
          V.drawLine(x1, y1, x2, y2);
          V.drawLine(x2, y2, x3, y3);

        }
      }
      if (db)
        V.pop();

      if (false) {
        V.pushColor(MyColor.cRED);
        plotCell(new IPoint2(), false);
        V.pop();
      }
    }
  }
  private static final double[] vertMultipliers = { -.5, 1, -1, 0, -.5, -1, .5,
      -1, 1, 0, .5, 1, };

  private void plotCell(IPoint2 cell, boolean detailed) {
    FPoint2 vc = toWorld(cell);
    if (detailed) {

      double us = preu * .8, vs = prev * .8;

      FPoint2 prev = null;
      for (int i = 0; i <= 6; i++) {
        int j = i % 6;
        FPoint2 c = new FPoint2(vc.x + vertMultipliers[j * 2 + 0] * us, vc.y
            + vertMultipliers[j * 2 + 1] * vs);
        if (prev != null)
          V.drawLine(prev, c);
        prev = c;
      }
    } else {
      double us = preu * .7, vs = us;
      V.fillRect(vc.x - us, vc.y - vs, us * 2, vs * 2);
    }
  }

  public void plotCell(IPoint2 cell) {
    plotCell(cell, highDef);
  }

  private static IPoint2 snapGridSpace(FPoint2 vp, IPoint2 dest) {
    if (dest == null)
      dest = new IPoint2();
    dest.setLocation(vp.x, vp.y);
    return dest;
  }
  /**
   * Round view space point to grid space
   * @param viewX
   * @param viewY
   * @param gridPt : where to store grid space point; if null, constructs it
   * @return gridPt
   */
  public IPoint2 toBoard(FPoint2 viewPt, IPoint2 gridPt) {

    double viewX = viewPt.x;
    double viewY = viewPt.y;

    if (gridPt == null)
      gridPt = new IPoint2();

    double[] c = toGrid.coeff();

    // casting double->int takes floor of value.  We want to round it.

    double tx = c[0] * viewX + c[1] * viewY + c[2];
    double ty = c[3] * viewX + c[4] * viewY + c[5];

    snapGridSpace(new FPoint2(tx, ty), gridPt);

    return gridPt;
  }

  /**
   * Transform grid point to view space
   * @param x,y : grid point
   * @param dest : where to store view space point; if null, constructs it
   * @return dest
   */
  public FPoint2 toWorld(IPoint2 boardCell) {
    return toWorld(boardCell.x, boardCell.y);
  }

  private FPoint2 toWorld(int x, int y) {
    FPoint2 dest = new FPoint2();
    double[] c = toView.coeff();
    dest.x = c[0] * x + c[1] * y + c[2];
    dest.y = c[3] * x + c[4] * y + c[5];
    return dest;
  }

  private boolean highDef;
  // transform matrix grid->view
  private Matrix toView;

  // width of cell in view space 
  private double pixelSize;

  // precalculated constants for rendering
  private double preu, prev;

  // transform matrix, view->grid
  private Matrix toGrid;
}
