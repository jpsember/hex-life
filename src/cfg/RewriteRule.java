package cfg;

import java.io.*;
import java.util.*;
import base.*;

public class RewriteRule {
  public RewriteRule(CFG cfg) {
    this.cfg = cfg;
    labelMap = new TreeMap();
  }

  /**
   * Determine if a rewrite rule applies to a parse tree.
   * @param g graph containing tree
   * @param rootNode root node of tree
   * @return array of indices for nodes if rule matches, else null
   */
  public int[] matches(CFG cfg, Graph g, int rootNode) {
    int[] nodeList = new int[lNodeCount];
    boolean ret = matches(cfg, g, rootNode, leftRoot, nodeList);
    return ret ? nodeList : null;
  }

  private boolean matches(CFG cfg, Graph g, int rootNode, RuleNodeLeft ln,
      int[] nodeList) {

    final boolean db = false;
    if (db)
      Streams.out.println("testing if rule matches, rule=" + this
          + ", rootNode=" + rootNode);

    boolean match = false;
    Token rootData = (Token) g.nodeData(rootNode);
    outer: do {

      int matchingToken = -1; //null;

      // search through list of possible root tokens for match
      for (int i = 0; i < ln.nRootTokens(); i++) {
        int tr = ln.rootToken(i);

        //        if (db)
        //          Streams.out.println(" rootToken=" + tr.debug() + "\n  rootData="
        //              + Tools.tv(rootData));

        if (tr == CFG.WILDCARD || tr == rootData.id()) {
          matchingToken = tr;
        }
      }
      if (matchingToken < 0) //== null)
        break outer;

      nodeList[ln.nodeIndex()] = rootNode;

      if (db)
        Streams.out.println("testing recursively, childList="
            + ln.hasChildList());

      if (ln.hasChildList()) {
        if (g.nCount(rootNode) != ln.nChildren()) {
          if (db)
            Streams.out.println("tree count=" + g.nCount(rootNode)
                + " != nChildren=" + ln.nChildren());
          break;
        }

        for (int i = 0; i < ln.nChildren(); i++) {
          RuleNodeLeft child = ln.child(i);
          if (db)
            Streams.out.println(" testing neighbor " + g.neighbor(rootNode, i)
                + " against child=" + child);

          if (!matches(cfg, g, g.neighbor(rootNode, i), child, nodeList))
            break outer;
        }
      }
      match = true;
    } while (false);
    return match;
  }

  public int apply(Graph graph, int[] nodeList) {
    return apply(graph, this.rightRoot, nodeList);
  }

  private int apply(Graph graph, RuleNodeRight rn, int[] nodeList) {

    final boolean db = false;
    if (db)
      Streams.out.println("RewriteRule, apply " + this + ", nodeList="
          + DArray.toString(nodeList));

    if (rn.childrenOfFlag())
      throw new IllegalStateException();

    // if root was left node reference:
    //
    // If no child node list was specified, use existing node with its children;
    // otherwise, create a new node with its data and fill in the new children
    //
    // if root was token:
    // Create new node using token id, and add children if 
    // child node list was specified
    //
    int newNode;
    if (rn.isToken()) {
      // create a new token using this token id
      Token t = new Token(rn.tokenId());
      // for debug purposes, store the string name of this token id as well
      t.setText(cfg.symbolFor(t.id()));
      newNode = graph.newNode(t);
      t.setNode(newNode);
    } else {
      if (!rn.childListDefined()) {
        // just use existing node, with any of its children intact
        newNode = nodeList[rn.leftNodeIndex()];
      } else {
        // create a new node using the same token as the old one
        Token tk = (Token) graph.nodeData(nodeList[rn.leftNodeIndex()]);
        tk = new Token(tk);
        newNode = graph.newNode(tk); //graph.nodeData(nodeList[rn.leftNodeIndex()]));
        tk.setNode(newNode);
      }
    }

    if (rn.childListDefined()) {
      for (int i = 0; i < rn.nChildren(); i++) {
        RuleNodeRight rChild = rn.child(i);

        // if it's not a token, check if its children of flag is set; if
        // so, add original node's children
        if (!rChild.isToken() && rChild.childrenOfFlag()) {
          int sourceNode = nodeList[rChild.leftNodeIndex()];
          for (int j = 0; j < graph.nCount(sourceNode); j++)
            graph.addEdge(newNode, graph.neighbor(sourceNode, j));
        } else {
          if (db)
            Streams.out
                .println("   adding edge recursively, applying to child "
                    + rChild);
          int newChild = apply(graph, rChild, nodeList);
          if (db)
            Streams.out.println("   recursively storing new child " + newChild);

          graph.addEdge(newNode, newChild);
        }
      }
    }
    if (db)
      Streams.out.println(" returning newNode=" + newNode);

    return newNode;
  }
  public void setPatterns(RuleNodeLeft leftRoot, RuleNodeRight rightRoot) {
    this.leftRoot = leftRoot;
    this.rightRoot = rightRoot;
  }

  public void labelNode(String lbl, Token t) {
    if (labelMap.containsKey(lbl))
      t.exception("duplicate label");
    labelMap.put(lbl, new Integer(lNodeCount));
  }

  public int findLabel(String lbl) {
    int ret = -1;
    Integer ival = (Integer) labelMap.get(lbl);
    if (ival != null)
      ret = ival.intValue();
    return ret;
  }

  public String toString() {

    if (true) {
      Tools.warn("verbose rules");
      StringBuilder sb = new StringBuilder();
      sb.append("Rule");
      sb.append(" #" + number);
      sb.append(" [" + leftRoot);
      sb.append(" = ");
      sb.append(rightRoot);
      sb.append("]");
      return sb.toString();
    }

    if (number > 0)
      return Integer.toString(number);
    else
      return "?";
    //    
    //    StringBuilder sb = new StringBuilder();
    //    sb.append("RewriteRule[");
    //    {
    //      if (labelMap != null)
    //        for (Iterator it = labelMap.keySet().iterator(); it.hasNext();) {
    //          String key = (String) it.next();
    //          sb.append(key);
    //          sb.append(':');
    //          sb.append(labelMap.get(key));
    //          sb.append(' ');
    //        }
    //      sb.append(leftRoot);
    //    }
    //    sb.append(']');
    //    return sb.toString();
  }
  public int getNodeCount() {
    return lNodeCount;
  }

  public void incNodeCount() {
    lNodeCount++;
  }
  public RuleNodeLeft leftRoot() {
    return leftRoot;
  }

  private int lNodeCount;
  private RuleNodeLeft leftRoot;
  private RuleNodeRight rightRoot;
  private Map labelMap;
  private CFG cfg;

  public void write(DataOutputStream s) throws IOException {
    s.writeShort(lNodeCount);
    write(leftRoot, s);
    write(rightRoot, s);
  }

  private RuleNodeRight readRightNode(DataInputStream s) throws IOException {
    RuleNodeRight n;
    if (s.readShort() != 0) {
      n = new RuleNodeRight(s.readShort());
    } else
      n = new RuleNodeRight(s.readShort(), s.readShort() != 0);
    if (s.readShort() != 0) {
      n.openChildList();
      int nKids = s.readShort();
      for (int i = 0; i < nKids; i++)
        n.addChild(readRightNode(s));
    }
    return n;
  }
  private void write(RuleNodeRight n, DataOutputStream s) throws IOException {
    s.writeShort(n.isToken() ? 1 : 0);
    if (n.isToken())
      s.writeShort(n.tokenId());
    else {
      s.writeShort(n.leftNodeIndex());
      s.writeShort(n.childrenOfFlag() ? 1 : 0);
    }
    s.writeShort(n.childListDefined() ? 1 : 0);
    if (n.childListDefined()) {
      s.writeShort(n.nChildren());
      for (int i = 0; i < n.nChildren(); i++)
        write(n.child(i), s);
    }
  }

  private void write(RuleNodeLeft n, DataOutputStream s) throws IOException {
    s.writeShort(n.nodeIndex());
    int k = n.nRootTokens();
    s.writeShort((int) k);
    for (int i = 0; i < k; i++) {
      int t = n.rootToken(i);
      s.writeShort(t);
    }
    s.writeShort(n.hasChildList() ? 1 : 0);
    if (n.hasChildList()) {
      s.writeShort(n.nChildren());
      for (int i = 0; i < n.nChildren(); i++) {
        write(n.child(i), s);
      }
    }
  }
  private RuleNodeLeft readLeftNode(CFG cfg, DataInputStream s)
      throws IOException {
    RuleNodeLeft n = new RuleNodeLeft(s.readShort(), null);
    int k = s.readShort();
    for (int i = 0; i < k; i++) {
      Token t = new Token(s.readShort());
      t.setText(cfg.symbolFor(t.id()));
      n.addRootToken(t);
    }
    if (s.readShort() != 0) {
      n.openChildList();
      int nKids = s.readShort();
      for (int i = 0; i < nKids; i++)
        n.addChild(readLeftNode(cfg, s));
    }
    return n;
  }
  public void setNumber(int n) {
    this.number = n;
  }
  private int number;

  public RewriteRule(CFG cfg, DataInputStream s) throws IOException {
    this.cfg = cfg;
    this.lNodeCount = s.readShort();

    this.leftRoot = readLeftNode(cfg, s);
    this.rightRoot = readRightNode(s);
  }
}
