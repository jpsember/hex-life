package cfg;

import base.*;
import java.io.*;
import java.util.*;

public class CFG {
  public static final int NONTERMINALS_START = 1000;
  public static final int WILDCARD = 2000;

  public static final String PARSEENDSYMBOL = "end$";
  public static final String PARSESTARTSYMBOL = "START$";
  public static final int TYPE_SLR = 0, TYPE_LALR = 1, TYPE_LR1 = 2,
      TYPE_TOTAL = 3;

  /**
  * File version, for serialization
  */
  public static final int VERSION = 0x1965;

  /**
   * Constructor.  Used by MakeCFG program; user should normally
   * call CFG.read() to read a CFG from a binary file.
   * 
   * @param type type of parser
   * @param dfa DFA to use
   */
  public CFG(int type, DFA dfa) {
    this.type = type;
    this.dfa = dfa;
    this.nTerminals = dfa.nSymbols2();
    eoiSymbol = this.nTerminals + Token.T_USER;
    nTerminals++;
  }

  /**
   * Get grammar type (TYPE_xxx)
   * @return
   */
  public int type() {
    return type;
  }

  private void readFromBinary(Class owner, String path) throws IOException {
    final boolean db = false;
    if (db)
      Streams.out.println("readFromBinary: path=" + path + ", owner=" + owner);

    DataInputStream in = new DataInputStream(Streams.openResource(owner, path));

    if (db)
      Streams.out.println("opened input stream");
    if (in.readShort() != VERSION)
      throw new IOException("bad version number");
    this.type = in.readShort();
    setStartSymbol(in.readShort());
    setStartProduction(in.readShort());
    int nSym = in.readShort();
    if (db)
      Streams.out.println("reading " + nSym + " symbols");

    for (int i = 0; i < nSym; i++)
      addNonTerminal(in.readUTF());
    this.eoiSymbol = in.readShort();
    int nProd = in.readShort();
    if (db)
      Streams.out.println(" nProd=" + nProd);

    for (int i = 0; i < nProd; i++) {
      int head = in.readShort();
      int pi = this.createProduction(head);
      int pl = in.readShort();
      for (int j = 0; j < pl; j++)
        this.addToProduction(pi, in.readShort());
    }
    int nRules = in.readShort();
    if (db)
      Streams.out.println("nRules=" + nRules);

    if (nRules != 0) {
      rewriteMap = new HashMap();
      for (int i = 0; i < nRules; i++) {
        RewriteRule r = new RewriteRule(this, in);
        r.setNumber(i + 1);
        add(r);
      }
    }

    if (in.readShort() != 0) {
      parser = new Parser(this, in);
    }

  }

  /**
   * Read CFG from file; read associated DFA of same root name as well
   * @param owner owner; if not null, used to locate file within jar file 
   * @param path path of file
   * @return CFG
   */
  public static CFG read(Object owner, String path) {
    DFA dfa = DFA.readFromSet(owner, Path.changeExtension(path, "dfa"));
    return read(owner, Path.changeExtension(path, "cfb"), dfa);
  }

  /**
   * Read CFG from file 
   * @param owner owner; if not null, used to locate file within jar file 
   * @param path path of file
   * @param dfa DFA associated with CFG
   * @return CFG
   */
  public static CFG read(Object owner, String path, DFA dfa) {
    final boolean db = false;
    if (db)
      Streams.out.println("readBinary: path=" + path);

    Class c = Streams.classParam(owner);

    String key = path;
    if (c != null)
      key = c.getName() + key;

    if (cfgMap == null)
      cfgMap = new HashMap();

    CFG cfg = (CFG) cfgMap.get(key);
    if (cfg == null) {
      try {
        cfg = new CFG(0, dfa);
        cfg.readFromBinary(c, path);
        cfgMap.put(key, cfg);
      } catch (IOException e) {
        Streams.out.println("e=" + e.toString());
        ScanException.toss(e);
      }
    }
    if (db)
      Streams.out.println("returning " + cfg);

    return cfg;
  }

  private void add(RewriteRule r) {
    RuleNodeLeft lr = r.leftRoot();
    for (int i = 0; i < lr.nRootTokens(); i++) {
      int tr = lr.rootToken(i);

      Object key = new Integer(tr); //tr.text();
      DArray a = (DArray) rewriteMap.get(key);
      if (a == null) {
        a = new DArray();
        rewriteMap.put(key, a);
      }
      a.add(r);
    }
  }

  Map getRewriteMap() {
    return rewriteMap;
  }

  private Map rewriteMap() {
    if (rewriteMap == null)
      rewriteMap = new HashMap();
    return rewriteMap;
  }

  /**
   * Determine if an id is a terminal
   * @param id
   * @return true if id is before the start of the nonterminal ids
   */
  public static boolean isTerminal(int id) {
    return id < NONTERMINALS_START;
  }

  /**
   * Determine if a string has the syntax of a terminal symbol
   * @param s string
   * @return true its first character is a..z
   */
  public static boolean isTerminal(String s) {
    char c = s.charAt(0);
    return (c >= 'a' && c <= 'z');
  }

  /**
   * Get the number of nonterminal symbols in this grammar
   * @return # nonterminals
   */
  public int nNonTerminals() {
    return symbols.size();
  }

  /**
   * Determine start symbol
   * @return id of start symbol, or -1 if none defined
   */
  public int startSymbol() {
    return startSymbol;
  }

  /**
   * Determine index of a symbol
   * @param symbol symbol
   * @return id of symbol, or -1 if it doesn't exist
   */
  private int idOf(String symbol) {
    int ind = -1;
    if (isTerminal(symbol)) {
      if (symbol.equals(PARSEENDSYMBOL))
        ind = endOfInputSymbol();
      else
        ind = dfa.tokenId(symbol);
    } else {
      Integer data = (Integer) mapSymbolToId.get(symbol);
      if (data != null)
        ind = data.intValue();
    }
    return ind;
  }

  /**
   * Make a new production, with an initially empty body
   * @param headId id of head nonterminal
   * @return index of production
   */
  public int createProduction(int headId) {
    if (isTerminal(headId) || headId - NONTERMINALS_START >= symbols.size())
      throw new IllegalArgumentException();
    Production p = new Production(this, headId);
    int pIndex = productions.size();
    productions.add(p);
    return pIndex;
  }

  /**
   * Add a symbol to the body of a production
   * @param prod index of production to modify
   * @param symbolId symbol to add
   */
  public void addToProduction(int prod, int symbolId) {
    Production p = getProduction(prod);
    p.addSymbol(symbolId);
  }

  /**
   * Get a production
   * @param index index of production
   * @return production
   */
  public Production getProduction(int index) {
    return (Production) productions.get(index);
  }

  /**
   * Add nonterminal to the grammar if it doesn't already exist
   * @param label terminal or nonterminal
   * @return id of symbol
   */
  public int addNonTerminal(String label) {
    return idOf(label, true);
  }

  /**
   * Get number of symbols, terminals plus nonterminals
   * @return number of symbols
   */
  public int symbolCount() {
    return nTerminals + symbols.size();
  }

  /**
   * Convert a symbol index to a symbol id
   * @param index index of terminal or nonterminal
   * @return id of symbol
   */
  public int indexToId(int index) {
    int nSym = symbolCount();
    if (index < 0 || index >= nSym)
      throw new IllegalArgumentException();
    int ret;
    if (index < nTerminals)
      ret = Token.T_USER + index;
    else
      ret = (index - nTerminals) + NONTERMINALS_START;
    return ret;
  }

  /**
   * 	Determine the number of productions
   */
  public int size() {
    return productions.size();
  }

  /**
   * Get symbol marking the end of the input
   * @return id of end of input terminal
   */
  public int endOfInputSymbol() {
    return eoiSymbol;
  }

  /**
   * Set start production
   * @param productionIndex index of production
   */
  public void setStartProduction(int productionIndex) {
    this.startProduction = productionIndex;
  }

  /**
   * 	Get index of production S' -> S, or -1 if no
   *  new start symbol has been defined
   */
  public int startProduction() {
    return startProduction;
  }

  /**
   * Determine symbol index from string
   * @param str String
   * @param addIfMissing : if true, and no symbol exists, adds one
   * @return index of symbol, or -1 if not found
   */
  private int idOf(String str, boolean addIfMissing) {
    int id = idOf(str);
    if (id < 0 && addIfMissing) {
      if (isTerminal(str))
        throw new IllegalArgumentException("terminal not found: " + str);
      id = nNonTerminals() + NONTERMINALS_START;
      symbols.add(str);
      mapSymbolToId.put(str, new Integer(id));
      if (startSymbol < 0)
        startSymbol = id;
    }
    return id;
  }

  /**
   * Get text of symbol
   * @param id id of symbol
   * @return text of symbol
   */
  public String symbolFor(int id) {
    String ret = null;
    if (id == WILDCARD)
      ret = "?";
    else if (id >= NONTERMINALS_START)
      ret = symbols.getString(id - NONTERMINALS_START);
    else {
      if (id == eoiSymbol)
        ret = PARSEENDSYMBOL;
      else
        ret = DFA.tokenName(dfa, id).toLowerCase();
    }
    return ret;
  }

  /**
   * Determine id from token's name
   * @param text name of token
   * @param t token generating token
   * @param mustExist if not found, throws exception using token
   * @return id, from DFA if it's a terminal, or CFG if it's a nonterminal,
   *   or -1 if not found
   */
  public int idFor(String text, Token t, boolean mustExist) {
    int id;
    if (isTerminal(text))
      id = dfa.tokenId(text);
    else
      id = idOf(text);

    if (mustExist && id < 0)
      t.exception("symbol not found");
    return id;
  }

  /**
   * Get dfa associated with grammar
   * @return dfa
   */
  public DFA dfa() {
    return dfa;
  }

  private void setStartSymbol(int head) {
    this.startSymbol = head;
  }

  /**
   * Get a Parser for a reader
   * @param reader Reader to parse 
   * @param readerDesc optional description of reader, for displaying errors
   * @return Parser for text
   */
  public Parser getParserFor(Reader reader, String readerDesc) {
    return getParserFor(reader,readerDesc,Token.T_USER);
//    TextScanner s = new TextScanner(reader, readerDesc, dfa(), Token.T_USER);
//    return getParserFor(s);
  }
  /**
   * Get a Parser for a reader
   * @param reader Reader to parse 
   * @param readerDesc optional description of reader, for displaying errors
   * @return Parser for text
   */
  public Parser getParserFor(Reader reader, String readerDesc, int skipToken) {
    TextScanner s = new TextScanner(reader, readerDesc, dfa(), skipToken);
    return getParserFor(s);
  }

  public Parser getParserFor(TextScanner s) {

    if (false) {
      Tools.warn("debugging");
      s.setTrace(true);
    }

    Parser p = parser.create(s);
    return p;
  }

  /**
   * Add a rewrite rule to the grammar
   * @param rule rewrite rule
   */
  public void addRewriteRule(RewriteRule rule) {
    Map map = rewriteMap();
    RuleNodeLeft leftRoot = rule.leftRoot();
    for (int i = 0; i < leftRoot.nRootTokens(); i++) {
      int tr = leftRoot.rootToken(i);
      Object key = new Integer(tr);
      DArray a = (DArray) map.get(key);
      if (a == null) {
        a = new DArray();
        map.put(key, a);
      }
      a.add(rule);
    }
  }

  /**
   * Get the number of terminal symbols in this grammar
   * @return
   */
  public int nTerminals() {
    return nTerminals;
  }

  // map of cached CFGs
  private static Map cfgMap;

  // index of start symbol, or <0 if none defined
  private int startSymbol = -1;
  // index of production S' -> S
  private int startProduction = -1;

  private DArray symbols = new DArray();
  // map symbol=>id
  private Map mapSymbolToId = new HashMap();

  // array of productions
  private DArray productions = new DArray();
  private int eoiSymbol = -1;
  private Map rewriteMap;
  private DFA dfa;
  private Parser parser;
  private int nTerminals;
  private int type;
}
