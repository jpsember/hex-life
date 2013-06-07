package cfg;

import java.io.*;
import java.util.*;
import base.*;

public class Parser {

  private static final int PREPARSE = 0;
  private static final int PARSEERR = 1;
  private static final int PARSECOMPLETED = 2;
  private static final boolean MULTIPLE_REWRITE_PASSES = true;

  protected static final int ACT_SHIFT = 0, ACT_REDUCE = 1, ACT_ACCEPT = 2;

  /**
   * Set trace status.  If set, displays effect of rule rewriting
   * @param trace trace status
   */
  public void setTrace(boolean trace) {
    this.trace = trace;
  }

  public Parser(CFG cfg) {
    this.cfg = cfg;
  }

//  /**
//   * Get string representation of a subtree
//   * @param root root token of subtree
//   * @return string
//   */
//  public String tree(Token root) {
//    //    if (true) {
//    //      Tools.warn("disabling");
//    //      return "disabled";
//    //    }
//    return getGraph().printRootedTree(root.node());
//  }

//  /**
//   * Get string representation of entire parse tree
//   * @return string
//   */
//  public String tree() {
//    return tree(parseRoot);
//  }

  /**
   * Construct a copy of this parser, to parse some new text
   * @return Parser
   */
  Parser create(TextScanner sc) {
    Parser p = new Parser();
    p.cfg = this.cfg;
    p.actionTbl = this.actionTbl;
    p.gotoTbl = this.gotoTbl;
    p.scanner = sc;
    return p;
  }
  private TextScanner scanner;

  private Parser() {
  }

  /**
   * Get graph containing parse tree
   * @return Graph
   */
  public Graph getGraph() {
    return graph;
  }

  /**
   * Perform parse.
   * Based on algorithm on p. 219 of Dragon book.
   */
  public Token parse() {

    if (parserState != PREPARSE)
      throw new IllegalStateException();

    parserState = PARSEERR;
    graph = new Graph();
    stack = new Stack();

    int rootNode = -1;

    Token t = Token.eofToken();

    outer: while (true) {
      int symbol = cfg.endOfInputSymbol();

      t = scanner.peek();

      if (!t.eof()) {
        if (t.unknown())
          t.exception("unrecognized token");
        symbol = t.id();
      }
      if (trace)
        Streams.out.println(Tools.d(t.text(), 10, true)
            + Tools.f(cfg.symbolFor(symbol), 12) + " " + stack.toString(cfg));

      int code = actionTbl.read(symbol, stack.peek(0).state);
      if (trace) {
        Streams.out.println("actionTbl for symbol " + cfg.symbolFor(symbol)
            + ", state " + stack.peek(0).state + " is " + Tools.fh(code));
      }

      if (code < 0) {
        if (trace)
          t.exception("unexpected token: " + t.idStr() + " for state "
              + stack.peek(0).state + "\n symbol=" + symbol + " endOfInp="
              + cfg.endOfInputSymbol());
        else {
          t.exception("parse error");
        }
      }

      int action = ParseTable.getAction(code);
      int arg = ParseTable.getState(code);

      switch (action) {

      case Parser.ACT_SHIFT:
        stack.push(new StackItem(arg, symbol, t));
        scanner.read();
        break;

      case Parser.ACT_REDUCE:
        reduce(t, arg);
        break;

      case Parser.ACT_ACCEPT:
        rootNode = stack.peek(0).getNode();
        break outer;
      }
    }
    // free up the stack, since it's no longer needed;
    // keep the graph around, though, for use by user
    stack = null;

    rootNode = rewriteTree(graph, rootNode);
    parseRoot = (Token) graph.nodeData(rootNode);
    parserState = PARSECOMPLETED;
    return parseRoot;
  }

  private Token parseRoot;

  /**
   * Rewrite a parse tree based on rewrite rules
   * @param graph graph containing parse tree
   * @param rootNode root node of parse tree
   * @return root node of rewritten parse tree
   */
  private int OLDrewriteTree(Graph graph, int rootNode) {
    int newRootNode = rootNode;

    boolean rewritten = false;

    // find all rules whose start node matches the root node
    Token rootData = (Token) graph.nodeData(rootNode);
    do {
      Map rewriteMap = cfg.getRewriteMap();
      if (rewriteMap == null)
        break;

      if (CFG.isTerminal(rootData.id()))
        break;

      // apply rules to children of this node
      for (int i = graph.nCount(rootNode) - 1; i >= 0; i--) {
        int oldChild = graph.neighbor(rootNode, i);
        int newChild = rewriteTree(graph, oldChild);
        if (oldChild != newChild)
          graph.addEdge(rootNode, newChild, null, i, true);
      }

      DArray possRules = (DArray) rewriteMap.get(new Integer(rootData.id()));
      if (possRules == null)
        break;

      // find first rule that applies to this node
      for (int i = 0; i < possRules.size(); i++) {
        RewriteRule r = (RewriteRule) possRules.get(i);

        int[] nl = r.matches(cfg, graph, rootNode);
        if (nl != null) {
          if (trace) {
            Streams.out.println(Tools.dashTitle(80, "Applying rule " + r));
            System.out.print(graph.printRootedTree(rootNode));
          }

          newRootNode = r.apply(graph, nl);

          if (trace) {
            Streams.out.println(Tools.dashTitle(80, "Output:"));
            System.out.print(graph.printRootedTree(newRootNode));
            Streams.out.println(Tools.dashTitle(80, null));
            Streams.out.println();
          }

          rewritten = true;
          break;
        }
      }
    } while (false);
    if (MULTIPLE_REWRITE_PASSES && rewritten) {
      rootNode = newRootNode;
      newRootNode = rewriteTree(graph, newRootNode);
    }
    return newRootNode;
  }

  private void reduce(Token origToken, int prodNum) {
    final boolean verbose = false;

    Production p = cfg.getProduction(prodNum);

    if (verbose) {
      Streams.out.print("   REDUCE  P:" + prodNum + "[ ");
      Streams.out.print(cfg.getProduction(prodNum));
      Streams.out.println("]");
    }

    // determine goto value
    int sPrime = stack.peek(p.bodyLength()).state;
    int gotoValue = gotoTbl.read(p.head(), sPrime);
    if (gotoValue < 0)
      throw new IllegalStateException();

    // construct subtree of head with body as leaves
    int headId = p.head();
    Token tk = new Token(origToken);
    //    Streams.out.println("origToken node="+tk.node());
    tk.setId(headId);
    //
    // this is not strictly necessary, but useful for printing
    tk.setText(cfg.symbolFor(headId));

    int nodeId = graph.newNode(tk);
    tk.setNode(nodeId);
    for (int i = 0; i < p.bodyLength(); i++) {
      StackItem s = stack.pop();
      int childNode = -1;

      if (s.isTerminal()) {
        childNode = graph.newNode(s.getToken());
        s.getToken().setNode(childNode);
      } else
        childNode = s.getNode();
      graph.addEdge(nodeId, childNode, null, 0, false);
    }

    // push head onto stack
    stack.push(new StackItem(gotoValue, p.head(), nodeId));

    if (verbose) {
      Streams.out.println("    pushed " + stack.peek(0));
    }
  }

  /**
   * @param s
   * @throws IOException
   */
  public void write(DataOutputStream s) throws IOException {
    actionTbl.write(s);
    gotoTbl.write(s);
  }

  /**
   * @param cfg
   * @param s
   * @throws IOException
   */
  Parser(CFG cfg, DataInputStream s) throws IOException {
    this.cfg = cfg;
    actionTbl = new ParseTable(s);
    gotoTbl = new ParseTable(s);
  }

  /**
   * Get the leftmost child of a node
   * @param token node
   */
  public Token child(Token token) {
    return child(token, 0);
  }

  /**
   * Get leftmost grandchild of a node
   * @param token node
   * @return leftmost grandchild
   */
  public Token cchild(Token token) {
    for (int k = 0; k < 2; k++) {
      token = child(token);
    }
    return token;
  }

  /**
   * Get leaf nodes of subtree
   * @param token root of subtree
   * @return array of tokens at leaves
   */
  public Token[] leaves(Token token) {
    DArray a = new DArray();
    leafSearch(a, token);
    return (Token[]) a.toArray(Token.class);
  }

  private void leafSearch(DArray a, Token root) {
    int k = nChildren(root);
    if (k == 0)
      a.add(root);
    else {
      for (int i = 0; i < k; i++) {
        leafSearch(a, child(root, k));
      }
    }
  }

  /**
   * Get a child of a node
   * @param token root node
   * @param childIndex index of child, 0..k-1
   * @return id of child
   */
  public Token child(Token token, int childIndex) {
    int nbrId = graph.neighbor(token.node(), childIndex);
    return (Token) graph.nodeData(nbrId);
  }

  /**
   * Get number of children of node
   * @param token id of node
   * @return number of children
   */
  public int nChildren(Token token) {
    return graph.nCount(token.node());
  }

  // CFG we're building for
  private CFG cfg;

  private Stack stack;
  private Graph graph;

  protected ParseTable actionTbl;
  protected ParseTable gotoTbl;

  private int parserState;
  private boolean trace;

  private static class Stack {
    public StackItem pop() {
      return (StackItem) n.pop();
    }

    /**
     * Constructor.
     * Places state #0 on stack.
     */
    public Stack() {
      n = new DArray();
      push(new StackItem(0, -1, -1));
    }

    /**
     * Add another state+symbol pair to stack
     * @param state int
     * @param symbolId int
     */
    public void push(StackItem item) {
      n.push(item);
    }

    public StackItem peek(int distFromTop) {
      return (StackItem) n.get(n.size() - 1 - distFromTop);
    }

    /**
     * Get string describing object
     * @return String
     */
    public String toString(CFG cfg) {
      StringBuffer sb = new StringBuffer();
      sb.append("Stack:");
      for (int i = 0; i < n.size(); i++) {
        sb.append(" " + ((StackItem) n.get(i)).toString(cfg));
      }
      return sb.toString();
    }
    private DArray n;
  }

  private static class StackItem {
    /**
     * Get string describing object
     * @return String
     */
    public String toString(CFG cfg) {
      StringBuffer sb = new StringBuffer();
      sb.append("[");
      sb.append(state);
      String sid = "-";
      if (symbolId >= 0)
        sid = cfg.symbolFor(symbolId);

      sb.append("|");
      sb.append(sid);
      sb.append("|");
      if (isTerminal()) {
        sb.append(TextScanner.convert(token.text(), false, '\0'));
      } else {
        sb.append(nodeId);
      }
      sb.append("]");
      return sb.toString();
    }

    /**
     * Construct a stack item for a terminal symbol
     * @param state
     * @param symbolId : CFG symbol associated with terminal symbol
     * @param tk : scanned token
     */
    public StackItem(int state, int symbolId, Token tk) {
      this.state = state;
      this.symbolId = symbolId;
      this.token = tk;
    }

    /**
     * Construct a stack item for a production head
     * @param state
     * @param symbolId : CFG symbol associated with nonterminal symbol at head
     * @param nodeId : id of node within forest containing parse subtree
     */
    public StackItem(int state, int symbolId, int nodeId) {
      this.state = state;
      this.symbolId = symbolId;
      this.nodeId = nodeId;
    }

    /**
     * Determine if stack item represents a terminal symbol
     * @return boolean
     */
    public boolean isTerminal() {
      return token != null;
    }

    public Token getToken() {
      return token;
    }

    public int getNode() {
      return nodeId;
    }

    private int state;
    private int symbolId;
    private Token token;
    private int nodeId;
  }

  private static class RewriteEnt {
    /**
     * Construct rewrite entry
     * @param parent id of parent to this node, or -1 if it has none
     * @param pchildNum child number that this node is 
     * @param root id of this node
     * @param nChildren number of children this node has
     */
    public RewriteEnt(int parent, int pchildNum, int root, int nChildren) {
      this.parentNode = parent;
      this.pChildNumber = pchildNum;
      this.rootNode = root;
      this.childNumber = nChildren - 1;
    }
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Ent");
      sb.append(" par:" + parentNode);
      sb.append(" pch:" + pChildNumber);
      sb.append(" root:" + rootNode);
      sb.append(" ch#:" + childNumber);

      return sb.toString();
    }
    public int parentNode;
    public int pChildNumber;
//    public boolean rewritten;
    public int childNumber;
    public int rootNode;
  };

  /**
   * Rewrite a parse tree based on rewrite rules.
   * Uses a stack instead of recursive calls.
   * 
   * @param graph graph containing parse tree
   * @param rootNode root node of parse tree
   * @return root node of rewritten parse tree
   */
  private int rewriteTree(Graph graph, int rootNode) {
    if (false)
      return OLDrewriteTree(graph, rootNode);

    int newRootNode = rootNode;
    // create a stack to perform dfs
    DArray stack = new DArray();
    do {
      Map rewriteMap = cfg.getRewriteMap();
      if (rewriteMap == null)
        break;

      stack.push(new RewriteEnt(-1, -1, rootNode, graph.nCount(rootNode)));

      boolean changesMade = false;

      while (!stack.isEmpty()) {

        // get top of stack entry
        RewriteEnt ent = (RewriteEnt) stack.last();

        // if more children to rewrite, do so
        if (ent.childNumber >= 0) {
          int parent = ent.rootNode;
          int child = graph.neighbor(parent, ent.childNumber);
          stack.push(new RewriteEnt(parent, ent.childNumber, child, graph
              .nCount(child)));
          ent.childNumber--;
          continue;
        }

        // pop entry from stack 
        stack.pop();

        int newrwNode = ent.rootNode;
        Token rootData = (Token) graph.nodeData(newrwNode);

        do {
          if (CFG.isTerminal(rootData.id()))
            break;

          DArray possRules = (DArray) rewriteMap
              .get(new Integer(rootData.id()));
          if (possRules == null)
            break;

          // find first rule that applies to this node
          for (int i = 0; i < possRules.size(); i++) {
            RewriteRule r = (RewriteRule) possRules.get(i);

            int[] nl = r.matches(cfg, graph, newrwNode);
            if (nl != null) {
              if (trace) {
                Streams.out.println(Tools.dashTitle(80, "Applying rule " + r));
                System.out.print(graph.printRootedTree(newrwNode));
              }
              newrwNode = r.apply(graph, nl);
              if (trace) {
                Streams.out.println(Tools.dashTitle(80, "Output:"));
                System.out.print(graph.printRootedTree(newrwNode));
                Streams.out.println(Tools.dashTitle(80, null));
                Streams.out.println();
              }
              changesMade = true;
              break;
            }
          }
        } while (false);

        if (newrwNode != ent.rootNode) {
          // if parent exists, replace its child
          if (ent.parentNode >= 0)
            graph.addEdge(ent.parentNode, newrwNode, null, ent.pChildNumber,
                true);
          else
            newRootNode = newrwNode;
        }
      }
      if (MULTIPLE_REWRITE_PASSES && changesMade) {
        newRootNode = rewriteTree(graph, newRootNode);
      }
    } while (false);
    return newRootNode;
  }
}
