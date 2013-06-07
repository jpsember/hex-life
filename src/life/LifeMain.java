package life;

import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;
import cfg.*;
import testbed.*;
import base.*;

public class LifeMain extends TestBed {
  /*! .enum  .private   4000
      pixelsize clear go 
      autocent anim runstop file open save saveas filepath repos 
      plotgrid highquality rules hex animspeed colors
  */

  private static final int PIXELSIZE = 4000;//!
  private static final int CLEAR = 4001;//!
  private static final int GO = 4002;//!
  private static final int AUTOCENT = 4003;//!
  private static final int ANIM = 4004;//!
  private static final int RUNSTOP = 4005;//!
  private static final int FILE = 4006;//!
  private static final int OPEN = 4007;//!
  private static final int SAVE = 4008;//!
  private static final int SAVEAS = 4009;//!
  private static final int FILEPATH = 4010;//!
  private static final int REPOS = 4011;//!
  private static final int PLOTGRID = 4012;//!
  private static final int HIGHQUALITY = 4013;//!
  private static final int RULES = 4014;//!
  private static final int HEX = 4015;//!
  private static final int ANIMSPEED = 4016;//!
  private static final int COLORS = 4017;//!
  /* !*/

  private static final int MAXSCALE = 120;

  public static void main(String[] args) {

    if (false) {
      DFA dfa = DFA.readFromSet(LifeMain.class, "lif.dfa");
      TextScanner s = new TextScanner(dfa, -1);
      try {
        s.include(Streams.reader("test/0.lif"), "test");
      } catch (IOException e) {
        e.printStackTrace();
      }
      do {
        Token t = s.read();
        Streams.out.println("read> " + t);
        if (t.eof())
          break;
      } while (true);

      return;
    }

    new LifeMain().doMainGUI(args);
  }

  public void addControls() {

    C.sOpen("Logic");
    {
      C.sTextField(RULES, "Rules:",
          "set survival/birth rules, in format \"ddd/dd\"", 10, true, "23/3");
      C.sNewColumn();
      C.sCheckBox(HEX, "hex grid", null, false);
    }
    C.sClose();

    C.sOpen("Display");
    C.sIntSlider(PIXELSIZE, "pixel size", null, 0, MAXSCALE, 100, 1);
    {
      C.sOpen();
      C
          .sButton(REPOS, "recenter",
              "adjusts center and scale to fit live cells");
      C.sNewColumn();
      C.sCheckBox(AUTOCENT, "auto",
          "automatically recenters view if necessary", false);
      C.sClose();
    }
    {
      C.sOpen();
      C.sCheckBox(PLOTGRID, "plot grid", null, true);
      C.sNewColumn();
      C.sCheckBox(HIGHQUALITY, "high quality",
          "render in high quality at expense of speed", true);
      C.sClose();
      C.sCheckBox(COLORS, "colors", "highight dying/birthing cells", false);
    }
    C.sClose();

    C.sOpen("Edit");
    {
      C.sButton(CLEAR, "clear", null);
    }
    C.sClose();

    C.sOpen("Run");
    {
      C.sIntSlider(ANIMSPEED, "speed", "speed of animation", 0, 100, 25, 1);
      C.sOpen();
      C.sButton(GO, "step", null);
      C.sHide();
      C.sCheckBox(ANIM, "run", "run nonstop", false);
      C.sNewColumn();
      C.sButton(RUNSTOP, "run", null);
      C.sClose();
    }
    C.sClose();

    fileStats = new FileStats();
    fileStats.persistPath(FILEPATH);
    //   fileStats.setPathGadget(FILEPATH);
    TestBed.setFileStats(fileStats);

  }

  private void updateRunStop() {
    C.sets(RUNSTOP, C.vb(ANIM) ? "stop" : "run");
  }

  private FileStats fileStats;
  public void initTestbed() {

    C.sOpenMenu(FILE, "File");
    C.sMenuItem(OPEN, "Open", "^o");
    C.sMenuItem(SAVE, "Save", "^s");
    C.sMenuItem(SAVEAS, "Save as", null);
    C.sCloseMenu();

    prepareGrid();
    initBoard();

    recenter(false);

    updateRunStop();

    animThread = new AnimThread();
    updateAnim();
    animThread.start();
  }

  private void updateAnim() {
    updateRunStop();

    double fps = C.vi(ANIMSPEED);
    fps = Math.pow((fps / 15.0), 3.64);
    fps = Math.max(.2, fps);

    animThread.setFrameRate(fps);
    animThread.setPaused(!C.vb(ANIM), false);
  }

  private void setState(int s) {
    editState = s;
  }

  private static final int STATE_NONE = 0, STATE_DRAWING = 1,
      STATE_SCROLLING = 2, STATE_ZOOMING = 3;

  private void resetGeneration() {
    generation = 0;
    if (animThread != null)
      animThread.setPaused(true, true);
    prevCells = null;
    newCells = null;
  }

  private void initBoard() {
    resetGeneration();
    life = usingHexGrid ? (ILife) new HexLife() : (ILife) new SquareLife();
    parseRules();
  }

  public void processAction(TBAction a) {
    switch (a.code) {
    case TBAction.DOWN1:
      if (editState == STATE_NONE) {
        C.setb(ANIM, false);
        haltAnim();
        resetGeneration();
        setState(STATE_DRAWING);
        drawStatePenDownLoc = a.loc;
        IPoint2 bp = grid.toBoard(drawStatePenDownLoc, null);
        drawStatePenColor = !life.hasCell(bp);
        life.setCell(bp, drawStatePenColor);
      } else if (editState == STATE_SCROLLING) {
        zoomStartLoc = a.loc;
        zoomStartPixSize = C.vi(PIXELSIZE);
        setState(STATE_ZOOMING);
      }
      break;
    case TBAction.DOWN2:
      if (editState == STATE_NONE) {
        C.setb(AUTOCENT, false);
        scrollStatePenDownLoc = a.loc;
        scrollStateOriginalOrigin = new FPoint2(origin);
        setState(STATE_SCROLLING);
      }
      break;
    case TBAction.DRAG:
      {
        if (editState == STATE_DRAWING) {
          resetGeneration();
          FPoint2 loc = a.loc;
          IPoint2 bp = grid.toBoard(loc, null);
          life.setCell(bp, drawStatePenColor);
        } else if (editState == STATE_SCROLLING) {
          IPoint2 origOrig = grid.toBoard(scrollStatePenDownLoc, null);
          IPoint2 newOrig = grid.toBoard(a.loc, null);
          origin.setLocation(scrollStateOriginalOrigin.x
              - (newOrig.x - origOrig.x), scrollStateOriginalOrigin.y
              - (newOrig.y - origOrig.y));
        } else if (editState == STATE_ZOOMING) {
          final double ZOOM_ADJ_SCALE = 1.4;
          double dy = (a.loc.y - zoomStartLoc.y) * ZOOM_ADJ_SCALE;
          C.seti(PIXELSIZE, zoomStartPixSize + (int) dy);
        }
      }
      break;
    case TBAction.UP1:
      if (editState == STATE_DRAWING)
        setState(STATE_NONE);
      else if (editState == STATE_ZOOMING) {
        setState(STATE_NONE);
      }
      break;
    case TBAction.UP2:
      if (editState == STATE_SCROLLING || editState == STATE_ZOOMING)
        setState(STATE_NONE);
      break;
    case TBAction.CTRLVALUE:
      switch (a.ctrlId) {
      case CLEAR:
        haltAnim();
        initBoard();
        break;

      case GO:
        haltAnim();
        updateLife();
        updateView();
        break;

      case HEX:
        haltAnim();
        if (prepareGrid())
          initBoard();
        break;

      case PIXELSIZE:
        if (!autoFlag)
          C.setb(AUTOCENT, false);
        break;
      case RUNSTOP:
        C.toggle(ANIM);
        updateAnim();
        break;
      case ANIM:
      case ANIMSPEED:
        updateAnim();
        break;
      case OPEN:
        doOpen(null);
        break;
      case SAVE:
        doSave(false);
        break;
      case SAVEAS:
        doSave(true);
        break;
      case REPOS:
        recenter(false);
        break;
      case RULES:
        parseRules();
        break;
      }
      break;
    }

    if (displayedModified != fileStats.modified())
      updateTitle2();
  }
  private void updateTitle2() {
    StringBuilder sb = new StringBuilder("Life");
    //    TestBed app = TestBed.app;
    //    String title = app.title();
    //
    //    sb.append(title);

    String s = fileStats.getPath();
    if (s != null) {
      sb.append(" : ");
      if (!TestBed.isApplet())
        s = Path.relativeToUserHome(new File(s));

      sb.append(s);
      if (fileStats.modified())
        sb.append("   (changes not written)");
    }
    String t = sb.toString();
    app.setExtendedTitle(t);
    app.updateTitle();
    displayedModified = fileStats.modified();
  }

  private boolean prepareGrid() {
    boolean changed = false;
    if (grid == null || usingHexGrid != C.vb(HEX)) {
      usingHexGrid = C.vb(HEX);
      if (!usingHexGrid) {
        grid = new SqrGrid();
      } else {
        grid = new HexGrid();
      }
      pixelSize = readScaleFromSlider();
      grid.prepare(pixelSize, origin, V.viewRect.midPoint());

      changed = true;
    }
    return changed;
  }

  private void doSave(boolean saveAs) {
    C.setb(ANIM, false);
    haltAnim();

    recenter(false);

    String filePath = fileStats.getPath();

    if (!saveAs) {
      if (filePath == null)
        saveAs = true;
    }
    String f = filePath;
    if (saveAs)
      f = getSavePath();

    if (f != null) {
      try {
        ourSetFilePath(f);
        String ext = Path.getExtension(f);
        if (ext.equals("lif"))
          LifeUtil.writeLif(life, f);
        else if (ext.equals("rle"))
          LifeUtil.writeRLE(life, f);
        else
          throw new IOException("unsupported extension");
      } catch (IOException e) {
        throw new TBError(e.getMessage());
      }
      updateTitle();
    }
  }

  private void recenter(boolean auto) {
    FRect r = null;
    for (Iterator it = life.liveCells(); it.hasNext();) {
      IPoint2 p = (IPoint2) it.next();
      FPoint2 wp = grid.toWorld(p);
      r = FRect.add(r, wp);
    }

    if (r != null) {
      FRect wr = V.viewRect;
      double md = Math.min(wr.width / r.width, wr.height / r.height) * .8;

      double sv = ((pixelSize * md) - SCALEMIN) / SCALEFACTOR;
      int nPixSize = MyMath.clamp((int) Math.round(sv), 0, MAXSCALE);

      if (auto) {

        final double DRIFT_TOL = .1;
        final int SCALE_TOL = 10;

        // if scale isn't changing much, and origin isn't either, ignore.
        FPoint2 p0 = r.midPoint(), p1 = wr.midPoint();
        double drift = Math.max(Math.abs(p0.x - p1.x) / wr.width, Math.abs(p0.y
            - p1.y)
            / wr.height);
        if (drift < DRIFT_TOL
            && Math.abs(nPixSize - C.vi(PIXELSIZE)) < SCALE_TOL)
          return;
      }

      C.seti(PIXELSIZE, nPixSize);
      IPoint2 newOrig = grid.toBoard(r.midPoint(), null);
      origin = newOrig;
    } else {
      C.seti(PIXELSIZE, 100);
      origin = new IPoint2();
    }
  }
//  private static String filePath() {
//    String filePath = C.vs(FILEPATH);
//    if (filePath.length() == 0)
//      filePath = null;
//    return filePath;
//  }

  private static final String supportedExtensions = "lif LIF rle";

  private void doOpen(String f) {
    final boolean db = false;
    if (db)
      Tools.warn("db is on");
    haltAnim();

    try {
      if (f == null) {
        String s =   fileStats.getPath();
        IFileChooser ch = Streams.fileChooser();
        String ns = ch.doOpen("Open file:", s, new PathFilter(
            supportedExtensions));
        if (ns != null) {
          f = Path.addExtension(ns, TestBed.parms.fileExt);
        }
      }
      if (f != null) {
        resetGeneration();

        if (f.endsWith(".rle"))
          life = LifeUtil.readRLE(f);
        else
          life = LifeUtil.readLif(f);
        C.setb(HEX, life instanceof HexLife);
        C.sets(RULES, LifeUtil.compileRules(life.getRules()));
        prepareGrid();
        recenter(false);
        ourSetFilePath(f);
      }
    } catch (Throwable e) {

      if (TestBed.parms.debug)
        Streams.out.println(Tools.d(e));
      throw new TBError(e);
    }
  }
  private static int defaultFlags = LifeUtil.parseRules("23/3");

  private void parseRules() {
    String s = C.vs(RULES);
    int newFlags = LifeUtil.parseRules(s);

    if (newFlags == 0)
      newFlags = defaultFlags;

    String s2 = LifeUtil.compileRules(newFlags);
    if (!s2.equals(s))
      C.sets(RULES, s2);
    life.setRules(newFlags);
  }

  private void haltAnim() {
    animThread.setPaused(true, false);
    C.setb(ANIM, false);
    updateRunStop();
  }

  private static void ourSetFilePath(String f) {
    C.sets(FILEPATH, f);
  }

  /**
   * Request name of text file to write
   * @param fc FCData
   * @param autoMode boolean
   * @return File
   */
  private   String getSavePath() {
    String s = fileStats.getPath();

//    String s = filePath;
    String f = null;
    IFileChooser ch = Streams.fileChooser();

    f = ch.doWrite("Save file:", s, new PathFilter(supportedExtensions));
    if (f != null) {
      f = Path.addExtension(f, TestBed.parms.fileExt);
    }

    return f;
  }

  public void setParameters() {
    parms.appTitle = "Life";
    parms.menuTitle = "Life";
    parms.fileExt = "lif";
    parms.withEditor = false;
    parms.algTrace = false;
    parms.includeGrid = false;
    parms.debug = true;
  }

  private void updateLife() {
    life.update();
    generation++;
    prevCells = newCells;
    newCells = new HashMap();
    for (Iterator it = life.liveCells(); it.hasNext();)
      newCells.put(it.next(), Boolean.TRUE);
  }
  private static final int MAX_BUSY_MS = 300;
  private long prevAutoCent;

  private static final Color cSurvived = MyColor.cBLUE;
  private static final Color cDied = MyColor.get(MyColor.RED, .8);
  private static final Color cBorn = MyColor.get(MyColor.GREEN, .4);

  public void paintView() {
    super.paintView();

    boolean updated = false;

    if (animThread != null) {
      // run generations until we reach the desired one, or until 
      // a maximum number of time has elapsed
      if (C.vb(ANIM)) {
        int desGen = animThread.getDesiredGeneration();
        long currTime = System.currentTimeMillis();
        while (generation < desGen
            && System.currentTimeMillis() - currTime < MAX_BUSY_MS) {
          updateLife();
          animThread.displayedGeneration = generation;
          updated = true;
        }
      }
    }

    if (updated && C.vb(AUTOCENT)) {
      long time = System.currentTimeMillis();
      int delt = (int) (time - prevAutoCent);
      if (delt < 0 || delt > 250) {
        prevAutoCent = time;
        autoFlag = true;
        recenter(true);
        autoFlag = false;
      }
    }

    pixelSize = readScaleFromSlider();
    grid.prepare(pixelSize, origin, V.viewRect.midPoint());

    Graphics2D g = V.get2DGraphics();

    boolean hq = C.vb(HIGHQUALITY);
    if (!hq) {
      g.setRenderingHint(RenderingHints.KEY_RENDERING,
          RenderingHints.VALUE_RENDER_SPEED);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_OFF);
    }
    if (C.vb(PLOTGRID)) {

      V.pushStroke(STRK_THIN);
      V.pushColor(MyColor.cLIGHTGRAY);

      grid.render();

      V.pop(2);
    }

    if (newCells == null) {
      newCells = new HashMap();
      for (Iterator it = life.liveCells(); it.hasNext();)
        newCells.put(it.next(), Boolean.TRUE);
      prevCells = null;
    }

    if (C.vb(COLORS)) {
      DArray newish = new DArray();
      DArray died = new DArray();
      DArray survived = new DArray();

      if (prevCells != null) {
        // paint in red cells that have died
        for (Iterator it = prevCells.keySet().iterator(); it.hasNext();) {
          IPoint2 p = (IPoint2) it.next();
          if (!newCells.containsKey(p))
            died.add(p);
          else {
            survived.add(p);
          }
        }
      }
      if (newCells != null) {
        for (Iterator it = newCells.keySet().iterator(); it.hasNext();) {
          IPoint2 p = (IPoint2) it.next();
          if (prevCells == null || !prevCells.containsKey(p))
            newish.add(p);
        }
      }
      plotCells(died.iterator(), cDied);
      plotCells(survived.iterator(), cSurvived);
      plotCells(newish.iterator(), prevCells != null ? cBorn : cSurvived);
    } else {
      plotCells(life.liveCells(), cSurvived);
    }

    if (!hq) {
      g.setRenderingHint(RenderingHints.KEY_RENDERING,
          RenderingHints.VALUE_RENDER_DEFAULT);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_DEFAULT);
    }

    displayStats();
  }
  private void plotCells(Iterator it, Color c) {
    V.pushColor(c);
    while (it.hasNext())
      grid.plotCell((IPoint2) it.next());
    V.pop();
  }

  private void displayStats() {
    V.pushColor(MyColor.cDARKGREEN);
    FRect r = V.viewRect;
    for (int row = 0; row < 2; row++) {
      String s = null;
      switch (row) {
      case 1:
        s = "Gen:" + generation;
        break;
      case 0:
        s = "Pop:" + life.population();
        break;
      }
      if (s != null)
        V.draw(s, new FPoint2(r.x + 5, r.y + V.getScale() * (row * 3 + 2)),
            Globals.TX_CLAMP);
    }
    V.pop();
  }
  private static final double SCALEMIN = .05;
  private static final double SCALEFACTOR = .03;

  private double readScaleFromSlider() {
    double pixelSize = (SCALEFACTOR * C.vi(PIXELSIZE) + SCALEMIN);
    return pixelSize;
  }

  /**
   * Thread to trigger updates of population at a consistent rate
   */
  private static class AnimThread extends Thread {
    private static final boolean db = false;

    public AnimThread() {
      setDaemon(true);
    }

    private int currTime() {
      return (int) (System.currentTimeMillis() - baseTime);
    }

    public void setPaused(boolean paused, boolean resetGeneration) {
      boolean wasPaused = this.paused;
      this.paused = paused;
      if (resetGeneration) {
        displayedGeneration = 0;
      }

      if (paused || wasPaused) {
        desiredGeneration = lastPausedGeneration = displayedGeneration;
        baseTime = System.currentTimeMillis();
      }

      if (db)
        Streams.out.println(this + " setPaused");
    }

    public String toString() {
      if (db) {
        StringBuilder sb = new StringBuilder();
        sb.append("AnimThread");
        sb.append(paused ? " PAUSED " : " RUNNING");
        sb.append(" fps=" + fps);
        sb.append(" time=" + Tools.fh(currTime()));
        sb.append(" gen des=" + desiredGeneration + " disp="
            + displayedGeneration);
        return sb.toString();
      } else
        return super.toString();
    }

    public int getDesiredGeneration() {
      return desiredGeneration;
    }

    public void setFrameRate(double fps) {
      if (fps <= 0 || fps > 1000)
        throw new IllegalArgumentException();

      this.fps = fps;
      baseTime = System.currentTimeMillis();
      lastPausedGeneration = displayedGeneration;
      if (db)
        Streams.out.println(this + " setFrameRate");
    }

    public void run() {
      paused = true;
      desiredGeneration = 0;
      displayedGeneration = 0;
      setFrameRate(10);
      if (db)
        Streams.out.println(this + " reset");

      while (true) {

        int delay = 0;
        if (db) {
          delay = 1000;
        }

        if (!paused) {
          // estimate desired generation
          desiredGeneration = lastPausedGeneration + (int) (currTime() * fps)
              / 1000;

          if (db)
            Streams.out.println("desGen=" + desiredGeneration + " lastPaused="
                + lastPausedGeneration + " disp=" + displayedGeneration);

          if (desiredGeneration <= displayedGeneration)
            desiredGeneration = displayedGeneration;
          else {
            TestBed.updateView();
          }
          delay = (int) (((desiredGeneration + 1 - lastPausedGeneration) * 1000) / fps)
              - currTime();
        }
        delay = MyMath.clamp(delay, 10, 100);
        if (db)
          Streams.out.println(this + " (thread run)");

        Tools.sleep(delay);
        try {
          Thread.sleep(delay);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
    private boolean paused;
    private int desiredGeneration;
    private int lastPausedGeneration;
    private int displayedGeneration;
    private double fps;
    private long baseTime;
  };

  private LGrid grid;

  private ILife life;
  private int generation;
  private Map prevCells;
  private Map newCells;

  // size of each cell, in pixels
  private double pixelSize;
  private static boolean displayedModified; //, fileModified;
  private FPoint2 scrollStatePenDownLoc;
  private FPoint2 scrollStateOriginalOrigin;
  private FPoint2 zoomStartLoc;
  private int zoomStartPixSize;
  private int editState;
  private boolean drawStatePenColor;
  private FPoint2 drawStatePenDownLoc;
  private AnimThread animThread;
  private boolean usingHexGrid;

  // cell at center of screen
  private IPoint2 origin = new IPoint2();

  // true if scale changing because of autoscaling operation
  private boolean autoFlag;
}
