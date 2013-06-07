package life;

import java.util.*;
import base.*;

public interface ILife {
  
  /**
   * Set rules 
   * @param bits survival bits in lower 16 bits, birth bits in upper 16 bits
   */
  public void setRules(int bits);
  /**
   * Get rules 
   * @return survival bits in lower 16 bits, birth bits in upper 16 bits
   */
  public int getRules();
  
  /**
   * Get iterator for live cells
   * @return Iterator of IPoint2's, one for each live cell
   */
  public Iterator liveCells();
  
  /**
   * Determine if cell is alive
   * @param cell
   * @return true if alive
   */
  public boolean hasCell(IPoint2 cell);
  
  /**
   * Make cell alive or dead, adjust population accordingly
   * @param cell
   * @param alive new state of cell
   */
  public void setCell(IPoint2 cell, boolean alive);
  
  /**
   * Transition to next generation
   */
  public void update();
  
  /**
   * Get current population
   * @return number of live cells
   */
  public int population();
}
