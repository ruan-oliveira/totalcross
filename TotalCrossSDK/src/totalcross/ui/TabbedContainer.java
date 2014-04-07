/*********************************************************************************
 *  TotalCross Software Development Kit                                          *
 *  Copyright (C) 1998, 1999 Wabasoft <www.wabasoft.com>                         *
 *  Copyright (C) 2000-2012 SuperWaba Ltda.                                      *
 *  All Rights Reserved                                                          *
 *                                                                               *
 *  This library and virtual machine is distributed in the hope that it will     *
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of    *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                         *
 *                                                                               *
 *  This file is covered by the GNU LESSER GENERAL PUBLIC LICENSE VERSION 3.0    *
 *  A copy of this license is located in file license.txt at the root of this    *
 *  SDK or can be downloaded here:                                               *
 *  http://www.gnu.org/licenses/lgpl-3.0.txt                                     *
 *                                                                               *
 *********************************************************************************/

package totalcross.ui;

import totalcross.sys.*;
import totalcross.ui.event.*;
import totalcross.ui.font.Font;
import totalcross.ui.font.FontMetrics;
import totalcross.ui.gfx.*;
import totalcross.ui.image.Image;
import totalcross.ui.image.ImageException;
import totalcross.ui.media.Sound;
import totalcross.util.Vector;

/**
 * TabbedContainer is a bar of text or image tabs.
 * It is assumed that all images will have the same height, but they may have different widths.
 * <br>
 * A scroll is automatically added when the total width of the titles is bigger than the control's width.
 * <br>
 * The containers are created automatically and switched when the user press the corresponding tab.
 * <p>
 * Here is an example showing a tab bar being used:
 *
 * <pre>
 * public class MyProgram extends MainWindow
 * {
 *    TabbedContainer tab;
 *
 *    public void initUI()
 *    {
 *       String names[] = {"Edition","Report"};
 *       tab = new TabbedContainer(names);
 *       add(tab);
 *       tab.setGaps(2,2,2,2); // set it before setting the rect
 *       tab.setRect(LEFT,TOP,FILL,FILL);
 *       tab.setContainer(0,new Edition()); // replace container 1 by a class that extends Container.
 *       tab.getContainer(1).add(new Label("Not implemented"),CENTER,CENTER);
 *    }
 *
 *    public void onEvent(Event event)
 *    {
 *       if (event.type == ControlEvent.PRESSED && event.target == tp)
 *       {
 *          int activeIndex = tp.getActiveTab();
 *          ... handle tab being pressed
 *       }
 *    }
 * }
 * </pre>
 * Here's another sample that will show two TabbedContainers, one with images and another one with scrolling tabs.
 * Note that you must create img1.png and img2.png.
 * <pre>
 * TabbedContainer tp1 = new TabbedContainer(new Image[]{new Image("img1.png"), new Image("img2.png")}, null);
 * add(tp1);
 * tp1.setRect(LEFT,TOP,Settings.screenWidth/2,Settings.screenHeight/2);
 * tp1.activeTabBackColor = Color.getRGB(222,222,222);
 *
 * TabbedContainer tp2 = new TabbedContainer(new String[]{"verinha","marcelo","denise","guilherme","renato","michelle","rafael","barbara","lucas","ronaldo","nenem",});
 * add(tp2);
 * tp2.setRect(LEFT,AFTER+2,FILL,FILL);
 * </pre>
 * 
 * When the user interface is Android, the tabs do not look good if the background is the same of the parent's.
 * In this case, we force the background to be slighly darker. There are a few fields that you can use to change
 * the color, like activeTabBackColor, useOnTabTheContainerColor and pressedColor.
 * 
 * Important: starting in TotalCross 1.3, with Settings.fingerTouch=true, you CANNOT call setRect in your container.
 * Otherwise, the flick and drag will not work and your container will be positioned incorrectly.
 */

public class TabbedContainer extends ClippedContainer implements Scrollable
{
   private int activeIndex=-1;
   private String []strCaptions;
   private Image []imgCaptions,imgDis, imgCaptions0;
   private boolean isTextCaption=true;
   private Container containers[];
   private int count;
   private int tabH;
   private int captionColor = Color.BLACK;
   private boolean atTop=true;
   private Rect [] rects,rSel,rNotSel;
   private int fColor,cColor;
   private Rect clientRect;
   private ArrowButton btnLeft, btnRight;
   private static final byte FOCUSMODE_OUTSIDE = 0;
   private static final byte FOCUSMODE_CHANGING_TABS = 1;
   private static final byte FOCUSMODE_INSIDE_CONTAINERS = 2;
   private byte focusMode;
   private boolean brightBack;
   /** Set to true to enable the beep when a tab is clicked */
   public  boolean beepOn; // guich@230_37
   /** Set the arrows color right after the constructor and after calling setCaptionsColor, which also change this property. */
   public int arrowsColor = Color.BLACK;
   private Font bold;
   private int btnX;
   private int style = Window.RECT_BORDER;
   private boolean []disabled; // guich@tc110_58
   // flick support
   private boolean isScrolling;
   private boolean flickTimerStarted=true;
   private int tempSelected=-1;
   private int []wplains,wbolds;
   private boolean scScrolled;

   /** Enables or not the arrows if scroll is needed. */
   public boolean showArrows = true;
   /** This color is the one used to paint the background of the active tab.
    * This is specially useful for image tabs.
    * @see #setBackColor
    * @see #useOnTabTheContainerColor
    * @see #tabsBackColor
    * @since SuperWaba 5.64
    */
   public int activeTabBackColor=-1; // guich@564_14

   /** Sets the colors used on each tab. You must create and set the array with the colors. Pass -1 to keep
    * the original color. This array has precedence over the other ways that changes colors, except activeTabBackColor.
    * @see #setBackColor
    * @see #useOnTabTheContainerColor
    * @see #activeTabBackColor
    * @since TotalCross 1.52
    */
   public int[] tabsBackColor;
   
   /** Sets the tabs with the same colors of the container.
    * @see #setBackColor
    * @see #tabsBackColor
    * @see #activeTabBackColor
    * @since SuperWaba 5.72
    */
   public boolean useOnTabTheContainerColor; // guich@572_12

   /** Stores the last active tab index, or -1 if none was previously selected.
     * @since SuperWaba 4.21
     */
   public int lastActiveTab = -1; // guich@421_30: changed name to conform with getActiveIndex

   /** In finger touch devices, the user still can flick into a disabled tab. To disable this behaviour,
    * set this flag to false; so when a disabled tab is reached, the user will not be able to flick into it, and will
    * have to click on an enabled tab to continue flicking.
    * @since TotalCross 1.3
    */
   public boolean flickIntoDisabledTabs = true;
   
   /** To be used on the setType method: specifies that the tabs will be placed on the top. */
   public static final byte TABS_TOP = 0;
   /** To be used on the setType method: specifies that the tabs will be placed on the bottom. */
   public static final byte TABS_BOTTOM = 1;
   
   /** Set the color when the user clicks on the tab. 
    * @since TotalCross 1.3.4
    */
   public int pressedColor = -1;
   
   /** Set to true to make all tabs have the same width.
    * @since TotalCross 1.3.4
    */
   public boolean allSameWidth;
   
   /** Define an extra height for the tabs. Use something line fmH/2.
    * Required when setIcons is called.
    * @see #setIcons(Image[]) 
    * @since TotalCross 1.3.4
    */
   public int extraTabHeight;

   /** The Flick object listens and performs flick animations on PenUp events when appropriate. */
   protected Flick flick;

   private TabbedContainer(int count)
   {
      ignoreOnAddAgain = ignoreOnRemove = true;
      this.count = count;
      this.focusTraversable = true; // kmeehl@tc100
      started = true;
      focusHandler = true;
      containers = new Container[count];
      if (Settings.fingerTouch)
      {
         flick = new Flick(this);
         flick.forcedFlickDirection = Flick.HORIZONTAL_DIRECTION_ONLY;
         flick.maximumAccelerationMultiplier = 1;
      }
      // create the rects since we want to reuse them
      rects = new Rect[count];
      for (int i = count-1; i >= 0; i--)
      {
         rects[i] = new Rect();
         Container c = containers[i] = new Container();
         if (flick != null)
            flick.addEventSource(c);
         c.ignoreOnAddAgain = c.ignoreOnRemove = true;
      }
      disabled = new boolean[count];
   }
   
   public void initUI()
   {
      onBoundsChanged(false);
   }
   
   /** Returns the number of tabs.
    * @since TotalCross 1.15
    */
   public int getTabCount()
   {
      return count;
   }

   /** Sets the given tab index as enabled or not. When a tab is disabled, it is displayed faded,
    * and if the user clicks on it, nothing happens. However, you still can activate it by calling
    * setActiveTab. If there are no tabs enabled, the current tab will be made active and the controls will
    * also be enabled. So, if you plan to disable all tabs, better disable the TabbedContainer control instead.
    * @param on If true, the tab is enabled, if false it is disabled.
    * @param tabIndex The tab's index (0 to count-1)
    * @since TotalCross 1.01
    * @see #setActiveTab
    */
   public void setEnabled(int tabIndex, boolean on) // guich@tc110_58
   {
      disabled[tabIndex] = !on;
      if (!on && (!isTextCaption || imgCaptions != null) && (imgDis[tabIndex] == null))
         try
         {
            imgDis[tabIndex] = imgCaptions[tabIndex].getFadedInstance();
         }
         catch (ImageException e)
         {
            imgDis[tabIndex] = imgCaptions[tabIndex];
         }
      if (!on && activeIndex == tabIndex) // move to next tab
         setActiveTab(nextEnabled(activeIndex,true));
      if (Settings.fingerTouch)
      {
         containers[tabIndex].setEnabled(on);
         if (!on) // tell Control.postEvent that the flick still needs to be called
            containers[tabIndex].eventsEnabled = true;
      }
      Window.needsPaint = true;
   }

   /** Returns if the given tab index is enabled.
    * @since TotalCross 1.01
    */
   public boolean isEnabled(int tabIndex)
   {
      return !disabled[tabIndex];
   }

   /** Constructs a tab bar control with Strings as captions. */
   public TabbedContainer(String []strCaptions)
   {
      this(strCaptions.length);
      this.strCaptions = strCaptions;
      onFontChanged();
   }

   /** Constructor to keep compilation compatibility with TC 1; transparentColor is ignored. */
   public TabbedContainer(Image []imgCaptions, int transparentColor) // guich@564_13
   {
      this(imgCaptions);
   }

   /** Constructs a tab bar control with images as captions, using the given color as transparent color.
    * If you don't want to use transparent colors, just pass -1 to the color. */
   public TabbedContainer(Image []imgCaptions) // guich@564_13
   {
      this(imgCaptions.length);
      this.imgCaptions = imgCaptions;
      setupImageProps();
      isTextCaption = false;
      onFontChanged();
   }
   
   /** Set the given icons to appear at the top (or bottom, if TABS_BOTTOM) of a text TabbedContainer.
    * The icon images must be squared. You must also set the extraTabHeight, because the icons
    * will be resized to (extraTabHeight-fmH) in both directions.
    * @since TotalCross 1.3.4
    */
   public void setIcons(Image[] icons)
   {
      if (icons.length != count)
         throw new RuntimeException("Image array passed in setIcons must have the same length of the captions.");
      imgCaptions0 = icons;
      imgCaptions = new Image[icons.length];
      setupImageProps();
   }
   
   private void setupImageProps()
   {
      imgDis = new Image[count];
   }

   /** Sets the position of the tabs. use constants TABS_TOP or TABS_BOTTOM.
     * Since the tabs are not changed dinamicaly, this method must be called
     * after the constructor. */
   public void setType(byte type)
   {
      atTop = type == TABS_TOP;
      onFontChanged();
   }

   /** Returns the Container for tab i */
   public Container getContainer(int i)
   {
      return containers[i];
   }

   /** Sets the type of border. Currently, only the Window.NO_BORDER and Window.RECT_BORDER are supported. NO_BORDER only draws the line under the tabs. */
   public void setBorderStyle(byte style)
   {
      this.style = style;
   }

   /** Replaces the default created Container with the given one. This way you can
    * avoid adding a container to a container and, as such, waste memory.
    * Note that you must do this before the first setRect for this TabbedContainer; otherwise,
    * you must explicitly call setRect again to update the added container bounds
    */
   public void setContainer(int i, Container container)
   {
      if (containers != null && i >= 0 && i < containers.length)
      {
         Container old = containers[i];
         containers[i] = container;
         if (i == activeIndex) // guich@300_34: fixed problem when the current tab was changed
         {
            remove(old);
            if (flick != null)
               flick.removeEventSource(old);
            add(container);
            tabOrder.removeAllElements(); // don't let the cursor keys get into our container
            container.requestFocus();
         }
         if (!container.started) // guich@340_58: set the container's rect
         {
            if (flick != null)
            {
               add(container);
               container.setRect(old.getRect());
               flick.addEventSource(container);
            }
            else
            {
               Container cp = container.parent;
               container.parent = this;
               container.setRect(clientRect);
               container.parent = cp;
            }
            container.setBackColor(container.getBackColor());
         }
      }
      if (Settings.keyboardFocusTraversable) // otherwise, in an app where there's only a TabbedContainer, the last added container would remain highlighted
         requestFocus();
   }

   /**
    * Sets the currently active tab. A PRESSED event will be posted to
    * the given tab if it is not the currently active tab; then, the containers will be switched.
    */
   public void setActiveTab(int tab)
   {
      if (tab != activeIndex && tab >= 0)
      {
         boolean firstTabChange = activeIndex == -1;
         if (!firstTabChange && flick == null) 
            remove(containers[activeIndex]);
         lastActiveTab = activeIndex; // guich@402_4
         activeIndex = tab;
         if (flick != null)
            for (int xx = -activeIndex * width + clientRect.x, i = 0; i < containers.length; i++, xx += width)
               containers[i].x = xx;
         else
            add(containers[activeIndex]);
         tabOrder.removeAllElements(); // don't let the cursor keys get into our container
         computeTabsRect();
         scrollTab(activeIndex);
         Window.needsPaint = true;
         if (!firstTabChange) // guich@200b4_87
            postPressedEvent();
      }
   }

   /** Returns the index of the selected tab */
   public int getActiveTab()
   {
      return activeIndex;
   }
   
   /** Returns the container of the active tab. 
    * @since TotalCross 1.2
    */
   public Container getActiveContainer() // guich@tc120_16
   {
      return containers[activeIndex];
   }

   /** Returns the caption height for this TabbedContainer. Note that it is not possible to compute the correct height of 
    * each container, since they will be added AFTER this TabbedContainer has their bounds set. So, you should actually use some
    * other way to specify the bounds, like FILL or FIT; using PREFERRED in the height of setRect will make your application abort. */
   public int getPreferredHeight()
   {
      return tabH /* guich@564_12: + 20 */ + insets.top+insets.bottom;
   }
   /** Returns the minimum width (based on the sizes of the captions) for this TabbedContainer */
   public int getPreferredWidth()
   {
      int sum = 0;
      if (count > 0)
      {
         // the max size is the size of the biggest bolded title plus the size of the plain titles
         int maxw = 0, maxi = 0;
         for (int i = count; --i >= 0;)
         {
            int w = rSel[i].width;
            if (w > maxw)
            {
               maxi = i;
               maxw = w;
            }
            sum += rNotSel[i].width-1;
         }
         sum += maxw - rNotSel[maxi].width; // add the diff between the bold and the plain fonts of the biggest title
      }
      return sum+getExtraSize(); // guich@573_11: changed from 3 to 2
   }
   
   private int getExtraSize()
   {
      return 2 + insets.left+insets.right;
   }

   /** Returns the index of the next/prev enabled tab, or the current tab if there's none. */
   private int nextEnabled(int from, boolean forward)
   {
      for (int i =0; i < count; i++)
      {
         boolean limitsReached = (forward && from == containers.length-1) || (!forward && from == 0);
         if (limitsReached)
            from = forward? 0 : count-1;
         else
            from = forward?from+1:from-1;
         if (!disabled[from])
            break;
      }
      return from < 0 ? 0 : from;
   }

   /** Used internally. resizes all the containers and add the arrows if scroll is needed. */
   protected void onBoundsChanged(boolean screenChanged)
   {
      int i;
      onFontChanged();
      computeTabsRect();
      int borderGap = style==Window.NO_BORDER || uiAndroid ? 0 : 1; // guich@400_89
      int xx = insets.left+borderGap;
      int yy = (atTop?tabH:borderGap)+insets.top;
      int ww = width-insets.left-insets.right-(borderGap<<1);
      int hh = height-insets.top-insets.bottom-(borderGap<<1)-(atTop?yy:tabH);
      clientRect = new Rect(xx,yy,ww,hh);
      for (i = 0; i < count; i++)
      {
         Container c = containers[i];
         if (flick != null && c.parent == null)
            add(c);
         c.setRect(xx,yy,ww,hh,null,screenChanged);
         c.reposition();
         if (flick != null)
            xx += width;
      }
      if (flick != null)
         flick.setScrollDistance(width);
      if (activeIndex == -1) setActiveTab(nextEnabled(-1,true)); // fvincent@340_40
      addArrows();
   }

   private boolean mustScroll()
   {
      return count > 1 && getPreferredWidth() > this.width; // guich@564_10: support scroll - guich@573_2: only add arrows if there's more than one tab
   }

   private void addArrows()
   {
      boolean scroll = mustScroll();
      if (scroll && showArrows)
      {
         int c = parent != null ? parent.backColor : UIColors.controlsBack; // guich@573_4
         if (btnLeft == null)
         {
            int hh = Settings.fingerTouch ? fmH*3/4 : Math.max(fmH/2,tabH/4); // guich@tc110_90: use tab height if its larger than font's height
            btnRight = new ArrowButton(Graphics.ARROW_RIGHT, hh, arrowsColor);
            btnRight.setBackColor(c);
            btnRight.setBorder(Button.BORDER_NONE);
            btnLeft  = new ArrowButton(Graphics.ARROW_LEFT,  hh, arrowsColor);
            btnLeft.setBackColor(c);
            btnLeft.setBorder(Button.BORDER_NONE);
            int yy = (tabH+btnRight.getPreferredHeight()) >> 1;
            super.add(btnRight,RIGHT,atTop ? (tabH-yy) : (this.height-yy),PREFERRED+(Settings.fingerTouch ? fmH : 0),PREFERRED);
            super.add(btnLeft,BEFORE-2,SAME,SAME,SAME);
            btnLeft.setEnabled(false);
            btnLeft.setFocusLess(true); // guich@570_39
            btnRight.setFocusLess(true); // guich@570_39
            btnRight.autoRepeat = btnLeft.autoRepeat = true; // guich@tc122_46
            btnRight.AUTO_DELAY = btnLeft.AUTO_DELAY = 500;
         }
         btnX = btnLeft.x-2;
      }
      else btnX = this.width;
      if (btnLeft != null)
      {
         btnRight.setVisible(scroll);
         btnLeft.setVisible(scroll);
      }
   }

   public void setEnabled(boolean b)
   {
      super.setEnabled(b);
      if (btnLeft != null)
      {
         boolean canGoLeft = activeIndex > 0;
         boolean canGoRight = activeIndex < count-1;
         btnLeft.setEnabled(enabled && canGoLeft);
         btnRight.setEnabled(enabled && canGoRight);
      }
   }

   /** compute the rects that represents each tab on the screen. */
   private void computeTabsRect()
   {
      int x0 = 1;
      int y0 = atTop?0:(height-tabH);
      int n = count;
      for (int i =0; i < n; i++)
      {
         Rect r = rects[i];
         Rect r0 = i == activeIndex ? rSel[i] : rNotSel[i];
         r.x = x0;
         r.y = r0.y + y0;
         r.width = r0.width;
         r.height = r0.height;
         x0 += r.width-1;
         rects[i] = r;
      }
   }

   /** Scroll the TabbedContainer to the given tab */
   private void scrollTab(int toIdx) // guich@564_10
   {
      if (btnLeft != null && mustScroll())
      {
         boolean canGoLeft = toIdx > 0;
         boolean canGoRight = toIdx < count-1;
         btnLeft.setEnabled(canGoLeft);
         btnRight.setEnabled(canGoRight);
         if (canGoLeft || canGoRight)
         {
            int xOfs;
            if (toIdx == 0)
               xOfs = 0;
            else
            {
               xOfs = 7*fmH/11; // keep part of the previous tab on screen
               for (int i =0; i < toIdx; i++)
                  xOfs -= rNotSel[i].width-1;
            }
            offsetRects(xOfs);
            // make sure that the last tab is near the left button
            if (rects[count-1].x2() < btnX || toIdx == count-1)
            {
               int dif = btnX - rects[count-1].x2();
               offsetRects(-xOfs);
               xOfs += dif;
               offsetRects(xOfs);
            }
            Window.needsPaint = true;
         }
      }
   }

   /** Offsets all rectangles by the given value */
   private void offsetRects(int xOfs)
   {
      // offset the rectangles
      for (int i = count-1; i >= 0; i--)
         rects[i].x += xOfs;
   }

   /** Compute the rectangles of the tabs based on the selected
    * (bolded) and unselected (plain) titles. */
   protected void onFontChanged() // guich@564_11
   {
      boolean isText = isTextCaption;
      if (wplains == null)
      {
         wplains = new int[count];
         wbolds = new int[count];
         rSel = new Rect[count];
         rNotSel = new Rect[count];
      }
      tabH = isText ? uiAndroid ? (fmH + 8 + extraTabHeight) : (fmH + 4) : (imgCaptions[0].getHeight() + 4);
      int y0 = atTop && !uiAndroid ?2:0;
      bold = uiAndroid ? font : font.asBold();
      FontMetrics fmb = bold.fm;
      int medW = (this.width-getExtraSize()) / count;
      for (int i = count; --i >= 0;)
      {
         wplains[i] = isText ? fm .stringWidth(strCaptions[i]) : imgCaptions[i].getWidth();
         wbolds[i] = isText && !uiAndroid ? fmb.stringWidth(strCaptions[i]) : wplains[i]; // in uiandroid there's no bold font
      }
      int wp = allSameWidth ? Math.max(medW,Convert.max(wplains)) : 0;
      int wb = allSameWidth ? Math.max(medW,Convert.max(wbolds))  : 0;
      for (int i = count; --i >= 0;)
      {
         if (uiAndroid)
         {
            rSel[i] = new Rect(0,0,allSameWidth ? wp : wplains[i]+12,tabH);
            rNotSel[i] = imgCaptions == null ? new Rect(0,atTop ? extraTabHeight/2 : 0,allSameWidth ? wp : wplains[i]+12,tabH-extraTabHeight/2) : new Rect(0,0,allSameWidth ? wp : wplains[i]+12,tabH);
         }
         else
         {
            rSel[i]    = new Rect(0,0,allSameWidth ? wb : wbolds[i]+5,tabH);
            rNotSel[i] = new Rect(0,y0,allSameWidth ? wp : wplains[i]+4,tabH-2);
         }
      }
      if (isText && imgCaptions != null) // have icons? resize them
         if (extraTabHeight == 0)
            Vm.warning("setIcon was called but extraTabHeight was not set.");
         else
         try
         {
            for (int size = extraTabHeight-fmH/2, i = 0; i < count; i++)
               imgCaptions[i] = imgCaptions0[i].getSmoothScaledInstance(size,size);
         }
         catch (ImageException ie) {if (Settings.onJavaSE) ie.printStackTrace();}
   }

   protected void onColorsChanged(boolean colorsChanged)
   {
      if (uiAndroid && parent != null && backColor == parent.backColor) // same background color in uiandroid does not look good.
      {
         activeTabBackColor = Color.brighter(backColor,32);
         backColor = Color.darker(backColor,32);
      }
      if (colorsChanged)
         brightBack = Color.getAlpha(foreColor) > 128;
      fColor = (enabled || !brightBack) ? getForeColor()    : Color.darker(foreColor);
      cColor = (enabled || !brightBack) ? getCaptionColor() : Color.darker(captionColor);
      if (colorsChanged && btnLeft != null)
      {
         btnRight.arrowColor = btnLeft.arrowColor = arrowsColor;
         btnRight.backColor = btnLeft.backColor = parent != null ? parent.backColor : UIColors.controlsBack; // guich@573_4
      }
   }

   /** Called by the system to draw the tab bar. */
   public void onPaint(Graphics g)
   {
      if (activeIndex == -1) return;
      Rect r;
      int n = count;
      int y = atTop?(tabH-1):0;
      int h = atTop?(height-y):(height-tabH+1);
      int yl = atTop?y:(y+h-1);
      // erase area with parent's color
      int containerColor = containers[activeIndex].backColor; // guich@580_26: use current container's backcolor instead of TabbedContainer's backcolor
      g.backColor = parent.backColor;
      if (!transparentBackground)
      {
         if (parent.backColor == containerColor) // same color? fill the whole rect
            g.fillRect(0,0,width,height);
         else
         {
            // otherwise, erase tab area...
            if (atTop)
               g.fillRect(0,0,width,y);
            else
               g.fillRect(0,yl,width,height-yl);
            // ...and erase containers area
            g.backColor = containerColor;
            g.fillRect(0,y,width,h);
         }
      }
      if (!uiAndroid)
      {
         g.foreColor = fColor;
         if (style != Window.NO_BORDER)
            g.drawRect(0,y,width,h); // guich@200b4: now the border is optional
         else
            g.drawLine(0,yl,width,yl);
      }
      
      int back = backColor;
      g.backColor = backColor;
      if (btnLeft != null && mustScroll()) // if we have scroll, don't let the title be drawn over the arrow buttons
         g.setClip(1,0,btnX,height);
      
      // draw the tabs
      
      boolean drawSelectedTabAlone = !transparentBackground && (activeTabBackColor >= 0 || uiAndroid);
      if (!transparentBackground && (uiAndroid || useOnTabTheContainerColor || tabsBackColor != null || parent.backColor != backColor || uiVista)) // guich@400_40: now we need to first fill, if needed, and at last draw the border, since the text will overlap the last pixel (bottom-right or top-right) - guich@tc100b4_10: uivista also needs this
         for (int i = 0; i < n; i++)
         {
            if (drawSelectedTabAlone && i == activeIndex)
               continue;
            r = rects[i];
            g.backColor = back = getTabColor(i); // guich@580_7: use the container's color if containersColor was not set - guich@tc110_59: use default back color if container was not yet shown.

            if (uiAndroid)
               try
               {
                  g.drawImage(NinePatch.getInstance().getNormalInstance(NinePatch.TAB, r.width,r.height, i == tempSelected && pressedColor != -1 ? pressedColor : back, !atTop, true), r.x,r.y);
               }
               catch (ImageException ie) {if (Settings.onJavaSE) ie.printStackTrace();}
            else
            if (uiFlat) // the flat style has rect borders instead of hatched ones.
               g.fillRect(r.x,r.y,r.width,r.height);
            else
            if (uiVista && enabled)
               g.fillVistaRect(r.x+1,r.y+1,r.width-2,r.height-2, back, atTop,false);
            else
               g.fillHatchedRect(r.x,r.y,r.width,r.height,atTop,!atTop); // (*)
         }
      if (drawSelectedTabAlone) // draw again for the selected tab if we want to use a different color
      {
         int b = containers[activeIndex].backColor;
         if (tabsBackColor == null && useOnTabTheContainerColor && activeTabBackColor != -1)
            g.backColor = b == backColor ? activeTabBackColor : b;
         else
         {
            boolean dontUseTabs = tabsBackColor == null || tabsBackColor[activeIndex] == -1;
            g.backColor = activeTabBackColor != -1 && dontUseTabs ? activeTabBackColor : activeTabBackColor != -1 && !dontUseTabs ? Color.interpolate(activeTabBackColor,tabsBackColor[activeIndex]) : getTabColor(activeIndex);
         }
         r = rects[activeIndex];
         if (uiAndroid)
            try
            {
               Image img = NinePatch.getInstance().getNormalInstance(NinePatch.TAB, r.width,r.height, g.backColor, !atTop, true);
               g.drawImage(img, r.x,r.y);
            }
            catch (ImageException ie) {if (Settings.onJavaSE) ie.printStackTrace();}
         else
         if (uiFlat) // the flat style has rect borders instead of hatched ones.
            g.fillRect(r.x,r.y,r.width,r.height);
         else
            g.fillHatchedRect(r.x,r.y,r.width,r.height,atTop,!atTop); // (*)
         g.backColor = backColor;
      }
      
      // draw text
      
      boolean isText = isTextCaption;
      for (int i =0; i < n; i++)
      {
         r = rects[i];
         int xx = r.x + (r.width-(i==activeIndex ? wbolds[i] : wplains[i]))/2;
         int yy = r.y + (r.height-fmH)/2;
         if (isText)
         {
            g.foreColor = disabled[i] ? Color.getCursorColor(cColor) : cColor; // guich@200b4_156
            if (uiAndroid)
               g.drawText(strCaptions[i],xx, atTop ? (extraTabHeight > 0 ? r.y + r.height-fmH-7 : yy-2) : (extraTabHeight > 0 ? r.y + 7 : yy), textShadowColor != -1, textShadowColor);
            else
            if (i != activeIndex)
               g.drawText(strCaptions[i],xx, yy, textShadowColor != -1, textShadowColor);
            else
            {
               g.setFont(bold); // guich@564_11
               g.drawText(strCaptions[i],xx, yy, textShadowColor != -1, textShadowColor);
               g.setFont(font);
            }
            if (disabled[i])
               g.foreColor = Color.getCursorColor(cColor);
            if (imgCaptions != null && imgCaptions[i] != null)
               g.drawImage(disabled[i] ? imgDis[i] : imgCaptions[i], r.x+(r.width-imgCaptions[i].getWidth())/2, atTop ? r.y+(extraTabHeight-imgCaptions[i].getHeight())/2 : r.y+(extraTabHeight+imgCaptions[i].getHeight())/2);
         }
         else
         {
            g.drawImage(disabled[i] ? imgDis[i] : imgCaptions[i], r.x+(r.width-imgCaptions[i].getWidth())/2, r.y+1);
         }
         if (uiFlat)
            g.drawRect(r.x,r.y,r.width,r.height);
         else
         if (!uiAndroid)
            g.drawHatchedRect(r.x,r.y,r.width,r.height,atTop,!atTop); // guich@400_40: moved from (*) to here
      }
      
      // guich@200b4: remove the underlaying line of the active tab.
      r = rects[activeIndex];
      if (!uiAndroid)
      {
         g.foreColor = getTabColor(activeIndex); // guich@580_7: use the container's back color
         g.drawLine(r.x,yl,r.x2(),yl);
         g.drawLine(r.x+1,yl,r.x2()-1,yl);
      }

      if (Settings.keyboardFocusTraversable && focusMode == FOCUSMODE_CHANGING_TABS) // draw the focus around the current tab - guich@580_52: draw the cursor only when changing tabs
      {
         g.drawDottedRect(r.x+1,r.y+1,r.width-2,r.height-2);
         if (Settings.screenWidth == 320)
            g.drawDottedRect(r.x+2,r.y+2,r.width-4,r.height-4);
      }
   }

   /** Returns the color of the given tab.
    * @since TotalCross 1.52
    */
   public int getTabColor(int tab)
   {
      return tabsBackColor != null && tabsBackColor[tab] != -1 ? tabsBackColor[tab] : useOnTabTheContainerColor && containers[tab].backColor != -1 ? containers[tab].backColor : backColor;
   }

   /** Sets the text color of the captions in the tabs. */
   public void setCaptionColor(int capColor)
   {
      this.captionColor = this.arrowsColor = capColor;
      onColorsChanged(true); // guich@200b4_169
   }
   /** Gets the text color of the captions. return a grayed value if this control is not enabled. */
   public int getCaptionColor()
   {
      return enabled?captionColor:Color.brighter(captionColor);
   }
   /** Returns the area excluding the tabs and borders for this TabbedContainer.
     * Note: do not change the returning rect object ! */
   public Rect getClientRect() // guich@340_27
   {
      return clientRect;
   }

   /** Returns the area excluding the tabs and borders for this TabbedContainer.
    * In this version, you provide the created Rect to be filled with the coords.*/
   protected void getClientRect(Rect r) // guich@450_36
   {
      r.set(clientRect);
   }

   /** Called by the system to pass events to the tab bar control. */
   public void onEvent(Event event)
   {
      if (event.type == PenEvent.PEN_DOWN)
         scScrolled = false;
      if (event.target != this)
      {
         if (event.type == ControlEvent.PRESSED && (event.target == btnLeft || event.target == btnRight))
            setActiveTab(nextEnabled(activeIndex,event.target == btnRight));
         if (!(flick != null && (event.type == PenEvent.PEN_DRAG || event.type == PenEvent.PEN_UP)))
            return;
      }
      
      switch (event.type)
      {
         case PenEvent.PEN_UP:
            if (tempSelected != -1)
               setActiveTab(tempSelected);
            tempSelected = -1;
            if (uiAndroid)
               Window.needsPaint = true;
            if (!flickTimerStarted)
               flickEnded(false);
            isScrolling = false;
            break;
         case PenEvent.PEN_DRAG:
            if (flick != null)
            {
               Window w = getParentWindow();
               if (w != null && w._focus == w.focusOnPenUp)
                  break;
               DragEvent de = (DragEvent)event;
               if (isScrolling)
               {
                  scrollContent(-de.xDelta, 0);
                  event.consumed = true;
               }
               else
               {
                  int direction = DragEvent.getInverseDirection(de.direction);
                  event.consumed = direction == DragEvent.LEFT || direction == DragEvent.RIGHT;
                  if (canScrollContent(direction, de.target) && scrollContent(-de.xDelta, 0))
                  {
                     flickTimerStarted = false;
                     isScrolling = scScrolled = true;
                  }
               }
            }
            break;
         case ControlEvent.FOCUS_IN: // guich@580_53: when focus is set, activate tab changing mode.
            if (Settings.keyboardFocusTraversable)
               focusMode = FOCUSMODE_CHANGING_TABS;
            break;
         case PenEvent.PEN_DOWN:
            PenEvent pe = (PenEvent)event;
            tempSelected = -1;
            if (uiAndroid)
               Window.needsPaint = true;
            if (pe.x < btnX && (flick != null || (rects[0].y <= pe.y && pe.y <= rects[0].y2()))) // guich@tc100b4_7 - guich@tc120_48: when fingerTouch, the y position may be below the tabbed container
            {
               int sel = -1;
               if (flick != null) // guich@tc120_48
               {
                  int minDist = Settings.touchTolerance;
                  for (int i = count-1; i >= 0; i--)
                  {
                     Rect r = rects[i];
                     int d = (int)(Convert.getDistancePoint2Rect(pe.x,pe.y, r.x,r.y,r.x+r.width,r.y+r.height)+0.5);
                     if (d <= minDist)
                     {
                        minDist = d;
                        sel = i;
                     }
                  }
               }
               else
               {
                  for (int i = count-1; i >= 0; i--)
                     if (rects[i].contains(pe.x,pe.y))
                     {
                        sel = i;
                        break;
                     }
               }
               if (sel != activeIndex && sel >= 0 && !disabled[sel])
               {
                  if (beepOn && !Settings.onJavaSE) Sound.beep(); // guich@300_7
                  tempSelected = sel;
                  if (!uiAndroid)
                     setActiveTab(sel);
               }
            }
            break;
         case KeyEvent.ACTION_KEY_PRESS:
            focusMode = FOCUSMODE_CHANGING_TABS;
            // guich@573_23 - super.drawHighlight(); // remove the highlight around the TabbedContainer
            Window.needsPaint = true; // guich@573_23
            break;
         case KeyEvent.SPECIAL_KEY_PRESS:
            if (Settings.keyboardFocusTraversable)
            {
               KeyEvent ke = (KeyEvent)event;
               int key = ke.key;
               if (focusMode == FOCUSMODE_CHANGING_TABS)
               {
                  if (key == SpecialKeys.LEFT || key == SpecialKeys.RIGHT)
                     setActiveTab(nextEnabled(activeIndex, key == SpecialKeys.RIGHT));
                  else
                  if (ke.isUpKey() || ke.isDownKey())
                  {
                     focusMode = FOCUSMODE_INSIDE_CONTAINERS;
                     Window.needsPaint = true; // guich@573_23 - drawHighlight();
                     containers[activeIndex].changeHighlighted(containers[activeIndex],ke.isDownKey());
                     isHighlighting = true;
                  }
               }
               if (ke.isActionKey())
               {
                  focusMode = FOCUSMODE_OUTSIDE;
                  //getParent().requestFocus(); - guich@580_54
                  isHighlighting = true;
                  Window.needsPaint = true; // guich@573_23 - drawHighlight();
               }
            }
            break;
      }
   }

   /** Tranfer the focus between the containers on this TabbedContainer */
   public void changeHighlighted(Container p, boolean forward)
   {
      Window w = getParentWindow();
      if (w != null)
         switch (focusMode)
         {
            case FOCUSMODE_OUTSIDE: // focus just got here
               if (w.getHighlighted() != this)
                  w.setHighlighted(this);
               else
                  super.changeHighlighted(p,forward);
               break;
            case FOCUSMODE_INSIDE_CONTAINERS: // was changing a control and the limits has been reached
               focusMode = FOCUSMODE_CHANGING_TABS;
               w.setHighlighted(this); // remove the focus from the last control
               Window.needsPaint = true; // guich@573_23 - drawHighlight();
               requestFocus();
               isHighlighting = false;
               break;
            default:
               super.changeHighlighted(p,forward);
         }
   }

   /** Only return to highlighting when we want */
   public void setHighlighting()
   {
      isHighlighting = false;
   }

   public void reposition()
   {
      super.reposition();
      computeTabsRect();
      addArrows(); // this is needed because the btnX was not yet repositioned when onBounds called addArrows.
      if (mustScroll())
         scrollTab(activeIndex);
      if (Settings.fingerTouch)
      {
         int tab = activeIndex;
         activeIndex = -1;
         setActiveTab(tab);
      }
   }

   public void getFocusableControls(Vector v)
   {
      if (visible && enabled) v.addElement(this);
      super.getFocusableControls(v);
   }

   public Control handleGeographicalFocusChangeKeys(KeyEvent ke)
   {
      if (MainWindow.mainWindowInstance._focus ==  this)
      {
         if ((atTop && ke.isUpKey()) || (!atTop && ke.isDownKey()))
            return null;

          if ((atTop && ke.isDownKey()) || (!atTop && ke.isUpKey()))
          {
              Control c = containers[activeIndex].children;
              while (c != null && !c.focusTraversable)
                  c = c.next;
              return c;
          }
          if ((ke.isNextKey() && activeIndex == containers.length-1) || (ke.isPrevKey() && activeIndex == 0))
             return null;
          ke.target = this;
          _onEvent(ke);
          return this;
      }

      int direction = 0;
      if (ke.isUpKey()) direction = SpecialKeys.UP;             // this order must
      else if (ke.isDownKey()) direction = SpecialKeys.DOWN;    // be preserved
      else if (ke.isNextKey()) direction = SpecialKeys.RIGHT;
      else if (ke.isPrevKey()) direction = SpecialKeys.LEFT;
      else return null;

      Control c = findNextFocusControl(MainWindow.mainWindowInstance._focus, direction);
      if (c == null)
      {
         boolean prev = direction == SpecialKeys.UP || direction == SpecialKeys.LEFT;
         c = (prev == atTop) ? this : MainWindow.mainWindowInstance.findNextFocusControl(this, direction);
      }
      return c;
   }

   /** Returns true of the type is set to TABS_TOP. */
   public boolean isAtTop()
   {
      return atTop;
   }

   /** Resizes the height of each added container and sets the height of this TabbedContainer to the maximum height of the containers. */
   public void resizeHeight() // guich@tc120_12
   {
      int h = 0;
      for (int i=0; i < containers.length; i++)
      {
         containers[i].resizeHeight();
         h = Math.max(h, containers[i].getHeight());
      }
      setRect(KEEP,KEEP,KEEP,getPreferredHeight() + h + 3);
   }

   public boolean flickStarted()
   {
      flickTimerStarted = true;
      return isScrolling;
   }
   
   public void flickEnded(boolean atPenDown)
   {
      int tab = getPositionedTab(false);
      setActiveTab(tab);
   }
   
   private int getPositionedTab(boolean exact)
   {
      int betterV = width;
      int betterI = -1;
      for (int i = 0; i < containers.length; i++)
      {
         int dif = containers[i].x - clientRect.x;
         if (dif < 0) dif = -dif;
         if (dif < betterV)
         {
            betterV = dif;
            betterI = i;
         }
      }  
      return !exact || betterV == 0 ? betterI : -1;
   }
   
   public boolean canScrollContent(int direction, Object target) // called when 
   {
      return getPositionedTab(true) == -1 ||
             (direction == DragEvent.LEFT && activeIndex > 0 && (flickIntoDisabledTabs || !disabled[activeIndex-1])) ||
             (direction == DragEvent.RIGHT && activeIndex < containers.length-1 && (flickIntoDisabledTabs || !disabled[activeIndex+1]));
   }

   public boolean scrollContent(int xDelta, int yDelta)
   {      
      if (containers.length == 1)
         return false;
      // prevent it from going beyond limits
      int maxX = -(containers[0].width * (containers.length-1) + containers.length)-1;
      int minX = 1;
      int curX = containers[0].x;
      int newX = curX - xDelta;
      if (newX > minX)
         newX = minX;
      else
      if (newX < maxX)
         newX = maxX;
      xDelta = curX - newX;
      if (xDelta == 0)
         return false;
      
      for (int i = containers.length; --i >= 0;)
         containers[i].x -= xDelta;
      Window.needsPaint = true;
      return true;
   }
   
   public int getScrollPosition(int direction)
   {
      return containers[0].getX() - clientRect.x;
   }
   
   public Flick getFlick()
   {
      return flick;
   }

   public boolean wasScrolled()
   {
      return scScrolled;
   }
}
