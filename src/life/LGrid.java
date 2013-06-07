package life;

import base.*;

public interface LGrid {
  
 // public static final boolean FLIPY = true;


  /**
   * Determine what board cell contains a world space pixel
   * @param w world space location
   * @param dest where to store board cell location, or null 
   * @return board cell location
   */
  public IPoint2 toBoard(FPoint2 w, IPoint2 dest);

  /**
   * Convert board cell location to world space point
   * @param boardCell board cell location
   * @return point at center of cell, in world space
   */
  public FPoint2 toWorld(IPoint2 boardCell);

  public void render();
  
  public void prepare(double pixelSize, IPoint2 origin, FPoint2 wOrigin);
  
  public void plotCell(IPoint2 cellLoc);
}
