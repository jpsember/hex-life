package cfg;

import java.io.*;
import java.util.*;
import base.*;

public class RuleNodeLeft {

  public RuleNodeLeft(int nodeIndex, Collection rootTokenList) {
    this.rootTokenList = new DArray();
    if (rootTokenList != null)
      this.rootTokenList.addAll(rootTokenList);
    this.nodeIndex = nodeIndex;
  }

  public void addChild(RuleNodeLeft childNode) {
    if (childList == null)
      openChildList();

    childList.add(childNode);
  }

  public void openChildList() {
    childList = new DArray();
  }

  public boolean hasChildList() {
    return childList != null;
  }

  public int nChildren() {
    return childList.size();
  }
  public RuleNodeLeft child(int i) {
    return (RuleNodeLeft) childList.get(i);
  }
  public String toString() {
    StringBuilder sb = new StringBuilder();
    toString(sb);
    return sb.toString();
  }
  private void toString(StringBuilder sb) {
    sb.append("#" + nodeIndex);
    sb.append(" [");
    for (int i = 0; i < nRootTokens(); i++) {
      int t = rootToken(i);
      if (i > 0)
        sb.append(' ');
      sb.append(t);
    }
    sb.append("]");

    sb.append(' ');
    if (childList != null) {
      sb.append("(");
      for (int i = 0; i < nChildren(); i++)
        child(i).toString(sb);
      sb.append(")");
    }
  }

  public int nodeIndex() {
    return this.nodeIndex;
  }
  public int nRootTokens() {
    return rootTokenList.size();
  }
  public int rootToken(int i) {
    return rootTokenList.getInt(i);  
  }
  public void addRootToken(Token t) {
    rootTokenList.addInt(t.id()); 
  }

  private int nodeIndex;
  private DArray rootTokenList;
  private DArray childList;
}
