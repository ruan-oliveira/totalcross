// Copyright (C) 2019-2020 TotalCross Global Mobile Platform Ltda.
//
// SPDX-License-Identifier: LGPL-2.1-only
package totalcross.ui.dialog.keyboard;

import totalcross.sys.Settings;
import totalcross.ui.Button;
import totalcross.ui.Container;
import totalcross.ui.font.Font;
import totalcross.ui.gfx.Color;
import totalcross.ui.image.Image;

public class AlphabetKeyboard extends Container {

  // First line
  public Button btQ;
  public Button btW;
  public Button btE;
  public Button btR;
  public Button btT;
  public Button btY;
  public Button btU;
  public Button btI;
  public Button btO;
  public Button btP;

  // Second line
  public Button btA;
  public Button btS;
  public Button btD;
  public Button btF;
  public Button btG;
  public Button btH;
  public Button btJ;
  public Button btK;
  public Button btL;

  // Third line
  public Button btZ;
  public Button btX;
  public Button btC;
  public Button btV;
  public Button btB;
  public Button btN;
  public Button btM;

  public Button btComma;
  public Button btPeriod;
  public Button btSpace;
  public Button btCancel, btn123, btDel;

  private int FORE_COLOR = Color.BLACK;

  public Button btCase;

  public Button btSlash;

  public AlphabetKeyboard() {
    // First line
    btQ = new Button("Q");
    btQ.setDoEffect(false);
    btW = new Button("W");
    btW.setDoEffect(false);
    btE = new Button("E");
    btE.setDoEffect(false);
    btR = new Button("R");
    btR.setDoEffect(false);
    btT = new Button("T");
    btT.setDoEffect(false);
    btY = new Button("Y");
    btY.setDoEffect(false);
    btU = new Button("U");
    btU.setDoEffect(false);
    btI = new Button("I");
    btI.setDoEffect(false);
    btO = new Button("O");
    btO.setDoEffect(false);
    btP = new Button("P");
    btP.setDoEffect(false);
    // Second line
    btA = new Button("A");
    btA.setDoEffect(false);
    btS = new Button("S");
    btS.setDoEffect(false);
    btD = new Button("D");
    btD.setDoEffect(false);
    btF = new Button("F");
    btF.setDoEffect(false);
    btG = new Button("G");
    btG.setDoEffect(false);
    btH = new Button("H");
    btH.setDoEffect(false);
    btJ = new Button("J");
    btJ.setDoEffect(false);
    btK = new Button("K");
    btK.setDoEffect(false);
    btL = new Button("L");
    btL.setDoEffect(false);
    // Third line
    btZ = new Button("Z");
    btZ.setDoEffect(false);
    btX = new Button("X");
    btX.setDoEffect(false);
    btC = new Button("C");
    btC.setDoEffect(false);
    btV = new Button("V");
    btV.setDoEffect(false);
    btB = new Button("B");
    btB.setDoEffect(false);
    btN = new Button("N");
    btN.setDoEffect(false);
    btM = new Button("M");
    btM.setDoEffect(false);
    btComma = new Button(",");
    btComma.setDoEffect(false);
    btPeriod = new Button(".");
    btPeriod.setDoEffect(false);
    // Last line
    btSpace = new Button("             ");
    btSpace.setDoEffect(false);
    btSlash = new Button("/");
    btSlash.setDoEffect(false);
    btCase = new Button("[a]");
    btCase.setDoEffect(false);
    
    btn123 = new Button("?123");
    btn123.setDoEffect(false);
    btCancel = new Button("Cancel");
    btCancel.setDoEffect(false);
    try {
      int size = fmH * 3 / 2;
      btDel = new Button(new Image("totalcross/res/del.png").getSmoothScaledInstance(size, size));
    } catch (Exception e) {
      btDel = new Button("Del");
    } finally {
      btDel.setDoEffect(false);
    }
  }

  @Override
  public void initUI() {
    final float X = width * 0.01f;
    final float Y = height * 0.01f;
    final int HEIGHT_BUTTON = this.fm.height * 3;
    final int WIDTH_BUTTON = (int) (((width - (11 * X)) / 10));

    final int aLeft = LEFT + (int) X;
    final int hGap = (int) X;
    final int vGap = (int) (Y);
    final int aHeight = HEIGHT_BUTTON;

    add(btn123, aLeft, BOTTOM - vGap, WIDTH_BUTTON * 2, aHeight);
    add(btComma, AFTER + hGap, SAME, WIDTH_BUTTON, aHeight, btn123);
    add(btPeriod, AFTER + hGap, SAME, WIDTH_BUTTON, aHeight);
    add(btSpace, AFTER + hGap, SAME, (WIDTH_BUTTON * 3), aHeight);
    add(btSlash, AFTER + hGap, SAME, WIDTH_BUTTON, aHeight);
    add(btCancel, AFTER + hGap, SAME, FILL - hGap, aHeight);

    // Third line
    addButtonLine(
        WIDTH_BUTTON,
        aLeft + (int) (WIDTH_BUTTON * 0.5),
        hGap,
        vGap,
        aHeight,
        btCase,
        btZ,
        btX,
        btC,
        btV,
        btB,
        btN,
        btM,
        btDel);
    btCase.setRect(aLeft, KEEP, (int) (WIDTH_BUTTON * 1.5), KEEP);
    btDel.setRect(KEEP, KEEP, FILL - hGap, KEEP);

    // Second line
    addButtonLine(
        WIDTH_BUTTON,
        aLeft + (WIDTH_BUTTON / 2),
        hGap,
        vGap,
        aHeight,
        btA,
        btS,
        btD,
        btF,
        btG,
        btH,
        btJ,
        btK,
        btL);

    // First line
    addButtonLine(
        WIDTH_BUTTON, aLeft, hGap, vGap, aHeight, btQ, btW, btE, btR, btT, btY, btU, btI, btO, btP);

    // Last line
    configureKeyboardKey(btComma);
    configureKeyboardKey(btPeriod);
    configureKeyboardKey(btSpace);
    configureKeyboardKey(btSlash);
    configureKeyboardKey(btn123);
    configureKeyboardKey(btCancel);

    btCase.setBackForeColors(Color.getRGB(143, 152, 162), Color.WHITE);
    btn123.setBackForeColors(Color.getRGB(143, 152, 162), Color.WHITE);
    btCancel.setBackForeColors(Color.getRGB(143, 152, 162), Color.WHITE);
    btDel.setBackForeColors(Color.getRGB(143, 152, 162), Color.WHITE);
  }

  private void  addButtonLine(
      int WIDTH_BUTTON, int aLeft, int hGap, int vGap, int aHeight, Button... buttons) {
    add(buttons[0], aLeft, BEFORE - vGap, WIDTH_BUTTON, aHeight);
    configureKeyboardKey(buttons[0]);
    for (int i = 1; i < buttons.length; i++) {
      add(buttons[i], AFTER + hGap, SAME, WIDTH_BUTTON, aHeight);
      configureKeyboardKey(buttons[i]);
    }
  }

  private void configureKeyboardKey(Button button) {
    Font font = getFont().asBold();
    button.setForeColor(FORE_COLOR);
    button.setFont(font);
    button.setBackColor(Color.getRGB(233, 233, 235));
    button.effect = null;
  }

  public void changeCase(boolean toUpperCase) {
    if (toUpperCase) {
      btCase.setText("[a]");
      btQ.setText(btQ.getText().toUpperCase());
      btW.setText(btW.getText().toUpperCase());
      btE.setText(btE.getText().toUpperCase());
      btR.setText(btR.getText().toUpperCase());
      btT.setText(btT.getText().toUpperCase());
      btY.setText(btY.getText().toUpperCase());
      btU.setText(btU.getText().toUpperCase());
      btI.setText(btI.getText().toUpperCase());
      btO.setText(btO.getText().toUpperCase());
      btP.setText(btP.getText().toUpperCase());
      // Second line
      btA.setText(btA.getText().toUpperCase());
      btS.setText(btS.getText().toUpperCase());
      btD.setText(btD.getText().toUpperCase());
      btF.setText(btF.getText().toUpperCase());
      btG.setText(btG.getText().toUpperCase());
      btH.setText(btH.getText().toUpperCase());
      btJ.setText(btJ.getText().toUpperCase());
      btK.setText(btK.getText().toUpperCase());
      btL.setText(btL.getText().toUpperCase());
      // Third line
      btZ.setText(btZ.getText().toUpperCase());
      btX.setText(btX.getText().toUpperCase());
      btC.setText(btC.getText().toUpperCase());
      btV.setText(btV.getText().toUpperCase());
      btB.setText(btB.getText().toUpperCase());
      btN.setText(btN.getText().toUpperCase());
      btM.setText(btM.getText().toUpperCase());
    } else {
      btCase.setText("[A]");
      btQ.setText(btQ.getText().toLowerCase());
      btW.setText(btW.getText().toLowerCase());
      btE.setText(btE.getText().toLowerCase());
      btR.setText(btR.getText().toLowerCase());
      btT.setText(btT.getText().toLowerCase());
      btY.setText(btY.getText().toLowerCase());
      btU.setText(btU.getText().toLowerCase());
      btI.setText(btI.getText().toLowerCase());
      btO.setText(btO.getText().toLowerCase());
      btP.setText(btP.getText().toLowerCase());
      // Second line
      btA.setText(btA.getText().toLowerCase());
      btS.setText(btS.getText().toLowerCase());
      btD.setText(btD.getText().toLowerCase());
      btF.setText(btF.getText().toLowerCase());
      btG.setText(btG.getText().toLowerCase());
      btH.setText(btH.getText().toLowerCase());
      btJ.setText(btJ.getText().toLowerCase());
      btK.setText(btK.getText().toLowerCase());
      btL.setText(btL.getText().toLowerCase());
      // Third line
      btZ.setText(btZ.getText().toLowerCase());
      btX.setText(btX.getText().toLowerCase());
      btC.setText(btC.getText().toLowerCase());
      btV.setText(btV.getText().toLowerCase());
      btB.setText(btB.getText().toLowerCase());
      btN.setText(btN.getText().toLowerCase());
      btM.setText(btM.getText().toLowerCase());
    }
  }
}
