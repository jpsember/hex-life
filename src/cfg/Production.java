package cfg;

import base.*;

public class Production {
  
  /**
   * Get string describing object
   * @return String
   */
  public String toString() {
    return toString(false, -1);
  }

  public String toString(boolean brief, int dotPos) {
    StringBuffer sb = new StringBuffer();
    sb.append(cfg.symbolFor(head()));
    if (!brief) {
      sb.append(" ->");
    }
    for (int k = 0; k <= bodyLength(); k++) {
      if (dotPos == k) {
        sb.append("*");
      }
      if (k < bodyLength()) {
        sb.append(" ");
        sb.append(cfg.symbolFor(body(k)));
      }
    }
    return sb.toString();
  }

  /**
   * Constructor
   * @param cfg
   * @param head
   */
  Production(CFG cfg, int head) {
    this.cfg = cfg;
    symbols.addInt(head);
  }

  /**
   * Get nonterminal at head of rule
   */
  public int head() {
    return symbols.getInt(0);
  }

  /**
   * Get # symbols in body of rule
   */
  public int bodyLength() {
    return symbols.size() - 1;
  }

  /**
   * Get body symbol 
   * @param elem position in body, 0..n-1
   */
  public int body(int elem) {
    return symbols.getInt(elem + 1);
  }
  /**
   * Add a symbol to the body
   * @param sym id of symbol
   */
  public void addSymbol(int sym) {
    symbols.addInt(sym);
  }

  private DArray symbols = new DArray();
  private CFG cfg;
}
