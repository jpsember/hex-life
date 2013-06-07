package cfg;

import base.*;
import cfg.RewriteRule.*;

public class RuleNodeRight {

  public RuleNodeRight(int leftNodeIndex, boolean childrenOfFlag) {
    this.leftNodeIndex = leftNodeIndex;
    this.childrenOfFlag = childrenOfFlag;
  }

  /**
   * @param tokenId
   */
  public RuleNodeRight(int tokenId) {
    this.tokenId = tokenId;
  }


  public boolean childListDefined() {
    return childList != null;
  }
  public int nChildren() {
    if (!childListDefined())
      throw new IllegalStateException();
    return childList.size();
  }
  public RuleNodeRight child(int n) {
    return (RuleNodeRight) childList.get(n);
  }

  public void addChild(RuleNodeRight childNode) {
    openChildList();
    childList.add(childNode);
  }

  public void openChildList() {
    if (childList == null)
      childList = new DArray();
  }
  public boolean childrenOfFlag() {
    return this.childrenOfFlag;
  }
  public int leftNodeIndex() {
    if (isToken())
      throw new IllegalStateException();
    return this.leftNodeIndex;
  }
  public boolean isToken() {
    return tokenId != 0;
  }
  public int tokenId() {
    if (!isToken())
      throw new IllegalStateException();
    return tokenId;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    toString(sb);
    return sb.toString();
  }
  private void toString(StringBuilder sb) {
    sb.append(leftNodeIndex);
    if (childListDefined()) {
      sb.append('(');
      for (int i = 0; i < nChildren(); i++)
        child(i).toString(sb);
      sb.append(')');
    } else if (childrenOfFlag)
      sb.append('.');
    sb.append(' ');
  }

  private int leftNodeIndex;
  private int tokenId;
  private boolean childrenOfFlag;
  private DArray childList;
}
