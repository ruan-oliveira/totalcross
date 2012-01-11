/*********************************************************************************
 *  TotalCross Software Development Kit                                          *
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



package totalcross.ui.dialog;

import totalcross.sys.*;
import totalcross.ui.*;
import totalcross.ui.event.*;
import totalcross.ui.font.*;

/** This class displays a big numeric pad and is used by the Edit class when its mode is set to CURRENCY and
 * useNumericBoxInsteadOfCalculator is true, or when you set the keyboard to KBD_NUMERIC. 
 */

public class NumericBox extends Window
{
   private Edit edNumber;
   private Control cOrig;
   private PushButtonGroup pbgAction,numericPad,pbgArrows;
   private String answer;
   private KeyEvent ke = new KeyEvent(),backspace; // guich@421_59
   /** Strings used to display the action messages. You can localize these strings if you wish. */
   public static String []actions = {"Clear","Ok","Cancel"}; // guich@320_44: added reuse button

   /** The maximum length for the edit that will be created. */
   public int maxLength=-2;
   
   /** The default value of the edit. */
   public String defaultValue;
   
   public NumericBox()
   {
      super("Numeric Pad",uiAndroid ? ROUND_BORDER : RECT_BORDER); // with caption and borders
      fadeOtherWindows = Settings.fadeOtherWindows;
      transitionEffect = Settings.enableWindowTransitionEffects ? TRANSITION_OPEN : TRANSITION_NONE;
      highResPrepared = started = true;
      uiAdjustmentsBasedOnFontHeightIsSupported = false;
      backspace = new KeyEvent(); backspace.type = KeyEvent.SPECIAL_KEY_PRESS; backspace.key = SpecialKeys.BACKSPACE;
   }

   private void setupUI(boolean isReposition) // guich@tc100b5_28
   {
      setBackColor(UIColors.numericboxBack); // before control definitions!
      
      setRect(LEFT,TOP,WILL_RESIZE,WILL_RESIZE);

      if (!isReposition || edNumber == null)
      {
         if (edNumber != null) 
            remove(edNumber);
         edNumber = cOrig != null && cOrig instanceof Edit ? ((Edit)cOrig).getCopy() : new Edit();
         edNumber.setKeyboard(Edit.KBD_NONE);
         edNumber.autoSelect = true;
         if (cOrig != null && cOrig instanceof SpinList)
         {
            edNumber.setDecimalPlaces(0);
            edNumber.setMode(Edit.CURRENCY,true);
         }
         if (maxLength != -2)
            edNumber.setMaxLength(maxLength);
         backspace.target = edNumber;
         add(edNumber);
      }
      Font f = font.adjustedBy(3,false);
      edNumber.setFont(f);
      edNumber.setRect(LEFT+2,TOP+4, Math.min(f.fm.height*9,Settings.screenWidth-20),PREFERRED);

      // positioning arrows
      if (pbgArrows == null)
      {
         pbgArrows = new PushButtonGroup(new String[]{"<",">","<<"},false,-1,2,12,1,true,PushButtonGroup.BUTTON);
         pbgArrows.setFocusLess(true);
         pbgArrows.clearValueInt = -1;
         add(pbgArrows);
      }
      pbgArrows.setRect(SAME,AFTER+4,SAME,fmH*2);

      // numeric pad
      if (numericPad == null)
      {
         String []numerics = {"1","2","3","4","5","6","7","8","9","00","0","-"};
         add(numericPad=new PushButtonGroup(numerics,false,-1,-1,10,4,true,PushButtonGroup.BUTTON));
         numericPad.setFont(font.adjustedBy(2));
         numericPad.setFocusLess(true); // guich@320_32
         numericPad.clearValueInt = -1;
      }
      numericPad.setRect(SAME, AFTER+4,SAME,Settings.screenHeight > fmH*16 ? fmH*12 : fmH*8); // guich@571_9

      if (pbgAction == null)
      {
         pbgAction = new PushButtonGroup(actions,false,-1,2,12,1,true,PushButtonGroup.BUTTON);
         pbgAction.setFocusLess(true);
         pbgAction.clearValueInt = -1;
         add(pbgAction);
      }
      pbgAction.setRect(SAME,AFTER+2,SAME,fmH*2);
      
      setInsets(2,2,2,2);
      resize();
      setRect(CENTER,CENTER,KEEP,KEEP);

      numericPad.setBackColor(UIColors.numericboxFore);
      pbgAction.setBackColor(UIColors.numericboxAction);
   }

   /** Gets the answer that the user selected to be pasted.
     * It can be the first operator, the total computed or null if the user canceled.
     */
   public String getAnswer() // guich@200b4_193: get the 'pasted' answer
   {
      return answer;
   }
   
   public void clear()
   {
      super.clear();
      answer = null;
   }
   
   public void onUnpop()
   {
      setFocus(this);
   }
   
   public void onPopup()
   {
      Control c = topMost.getFocus();
      cOrig = c instanceof Edit || c instanceof SpinList ? (Control)c : null;
      setupUI(false);
      clear();
      if (cOrig != null)
      {
         String s = cOrig instanceof Edit ? ((Edit)cOrig).getTextWithoutMask() : ((SpinList)cOrig).getSelectedItem();
         if (s.length() == 0 || "+-0123456789".indexOf(s.charAt(0)) != -1) // guich@401_16: added support for + and changed the routine
            edNumber.setText(s);
      }
      if (defaultValue != null)
         edNumber.setText(defaultValue);
   }

   public void postPopup()
   {
      setFocus(edNumber);
   }
   
   protected void postUnpop()
   {
      if (answer != null) // guich@580_27
         postPressedEvent();
   }
   
   public void onEvent(Event event)
   {
      switch (event.type)
      {
         case KeyEvent.SPECIAL_KEY_PRESS:
            int key = ((KeyEvent)event).key;
            if (key == SpecialKeys.CALC)
               unpop();
            break;
         case ControlEvent.PRESSED:
            if (event.target == pbgArrows && pbgArrows.getSelectedIndex() != -1)
            {
               switch (pbgArrows.getSelectedIndex())
               {
                  case 0:
                  {
                     int p = edNumber.getCursorPos()[1] - 1;
                     if (p >= 0)
                        edNumber.setCursorPos(p,p);
                     break;
                  }
                  case 1:
                  {
                     int p = edNumber.getCursorPos()[1] + 1;
                     if (p <= edNumber.getLength())
                        edNumber.setCursorPos(p,p);
                     break;
                  }
                  case 2:
                  {
                     edNumber.onEvent(backspace);
                     break;
                  }
               }
            }
            else
            if (event.target == pbgAction && pbgAction.getSelectedIndex() != -1)
            {
               switch (pbgAction.getSelectedIndex())
               {
                  case 0:
                     clear();
                     break;
                  case 1:
                     answer = edNumber.getTextWithoutMask();
                     if (cOrig != null)
                     {
                        if (cOrig instanceof Edit)
                           ((Edit)cOrig).setText(answer,true);
                        else
                           ((SpinList)cOrig).setSelectedItem(answer);
                     }
                     unpop();
                     break;
                  case 2:
                     clear();
                     unpop();
                     break;
               }
            }
            else
            if (event.target == numericPad)
            {
               String s = numericPad.getSelectedItem();
               if (s != null)
               {
                  if (s.equals("-"))
                  {
                     String t = edNumber.getTextWithoutMask();
                     if (t.length() > 0)
                     {
                        if (t.startsWith("-"))
                           t = t.substring(1);
                        else
                           t = "-".concat(t);
                        edNumber.setText(t);
                        edNumber.setCursorPos(t.length(),t.length());
                     }
                  }
                  else
                  {
                     for (int i =0, n = s.length(); i < n; i++)
                     {
                        ke.key = s.charAt(i);
                        ke.target = edNumber;
                        edNumber._onEvent(ke);
                     }
                  }
               }
            }
            break;
      }
   }
   
   public void reposition()
   {
      setupUI(true);
   }
}
