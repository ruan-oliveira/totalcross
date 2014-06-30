package totalcross.ui.anim;

import totalcross.ui.*;
import totalcross.ui.event.*;

public class FadeAnimation extends ControlAnimation implements TimerListener
{
   int a,at,af;
   boolean fadeIn;
   
   public FadeAnimation(Control c, boolean fadeIn, AnimationFinished animFinish)
   {
      super(c,animFinish);
      this.fadeIn = fadeIn;
      at = 255;
      a = fadeIn ? 0 : 255;
      af = fadeIn ? 255 : 0;
   }

   public FadeAnimation(Control c, boolean fadeIn)
   {
      this(c, fadeIn, null);
   }
   
   public void start() throws Exception
   {
      super.start();
      c.offscreen.alphaMask = a;
   }
   
   public void animate()
   {
      int speed = (int)computeSpeed(at);
      at -= speed;
      a += fadeIn ? speed : -speed;
      if (a > 255) a = 255; else if (a < 0) a = 0;
      if (c.offscreen != null)
         c.offscreen.alphaMask = a;
      Window.needsPaint = true;
      if (a == af || speed == 0)
      {
         a = af;
         stop();
      }
   }

   public static FadeAnimation create(Control c, boolean fadeIn, AnimationFinished animFinish)
   {
      try
      {
         return new FadeAnimation(c,fadeIn, animFinish);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      return null;
   }
   
   public static FadeAnimation create(Control c, boolean fadeIn)
   {
      return create(c, fadeIn, null);
   }
}
