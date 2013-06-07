package life;

import testbed.*;
import base.*;

public class SqrGrid implements LGrid {

  /**
   * Determine what board cell contains a world space pixel
   * @param w world space location
   * @param dest where to store board cell location, or null 
   * @return board cell location
   */
  public IPoint2 toBoard(FPoint2 w, IPoint2 dest) {
    FPoint2 dest3 = squareGridToBoardMatrix.apply(w, null);
    dest3 = MyMath.snapToGrid(dest3, 1.0);
    if (dest == null)
      dest = new IPoint2();
    dest.set((int) dest3.x, (int) dest3.y);
    return dest;
  }

  /**
   * Convert board cell location to world space point
   * @param boardCell board cell location
   * @return point at center of cell, in world space
   */
  public FPoint2 toWorld(IPoint2 boardCell) {
    return squareGridFromBoardMatrix.apply(boardCell, null);
  }

  public void prepare(double pixelSize, IPoint2 origin, FPoint2 wOrigin) {
    this.pixelSize = pixelSize;
    this.highDef = pixelSize >= 1.5;
    if (highDef) {
      xo = pixelSize * .45;
      yo = pixelSize * .45;
      xs = 2 * xo;
      ys = 2 * yo;
    } else {
      xo = pixelSize * .5;
      yo = xo;
      xs = pixelSize;
      ys = xs;
    }

    if (true) {

      Matrix gridOffset = Matrix.getTranslate(origin, true);
      Matrix scaleToWorld;
      {
        Matrix m = Matrix.getScale(pixelSize, -pixelSize);
        scaleToWorld = m;
      }
      Matrix offsetWorld;
      {
        Matrix m = Matrix.getTranslate(wOrigin, false);
        offsetWorld = m;
      }
      Matrix m = offsetWorld;
      Matrix.mult(m, scaleToWorld, m);
      Matrix.mult(m, gridOffset, m);
      squareGridFromBoardMatrix = m;

    } else {

      Matrix m1 = Matrix.getScale(pixelSize, pixelSize);
      Matrix m2 = Matrix.getTranslate(new FPoint2(-origin.x, -origin.y), false);
      m2 = Matrix.mult(Matrix.getScale(1.0, -1.0), m2, null);

      FPoint2 trans = wOrigin;
      trans.scale(1.0 / pixelSize);
      Matrix m3 = Matrix.getTranslate(trans, false);

      Matrix m = Matrix.mult(m1, m2, null);
      squareGridFromBoardMatrix = Matrix.mult(m, m3, m);
    }
    squareGridToBoardMatrix = squareGridFromBoardMatrix.invert(null);
  }

  public void plotCell(IPoint2 pt) {
    FPoint2 b0 = toWorld(pt);
    V.fillRect(b0.x - xo, b0.y - yo, xs, ys);
  }

  public void render() {
    if (highDef) {
      // calculate the extreme visible cells
      FRect r = new FRect(V.viewRect);
      r.inset(-pixelSize);
      IPoint2 p0, p1;

      double wx0, wx1, wy0, wy1;
      FPoint2 w0, w1;
      p0 = toBoard(r.bottomLeft(), null);
      p1 = toBoard(r.topRight(), null);

      //      if (FLIPY) {
      //        p0.x--;
      //        p0.y++;
      //        p1.x++;
      //        p1.y--;
      //      } else {
      //        p0.x--;
      //        p0.y--;
      //        p1.x++;
      //        p1.y++;
      //      }
      w0 = toWorld(p0);
      w1 = toWorld(p1);
      wx0 = w0.x;
      wx1 = w1.x;
      wy0 = w0.y;
      wy1 = w1.y;

      double of = pixelSize * .5;

      wx0 -= of;
      wy0 -= of;
      wx1 += of;
      wy1 += of;

      int ys = Math.abs(p1.y - p0.y);
      int xs = Math.abs(p1.x - p0.x);
      {
        double yStep = (wy1 - wy0) / (ys + 1);
        double xStep = (wx1 - wx0) / (xs + 1);

        double wy = wy0;
        for (int y = 0; y <= ys; y++, wy += yStep) {
          V.drawLine(wx0, wy, wx1, wy);
        }
        double wx = wx0;
        for (int x = 0; x <= xs; x++, wx += xStep) {
          V.drawLine(wx, wy0, wx, wy1);
        }
      }
    }
  }

  private boolean highDef;
  private double xo, yo;
  private double xs, ys;
  private Matrix squareGridToBoardMatrix;
  private Matrix squareGridFromBoardMatrix;
  private double pixelSize;
}
