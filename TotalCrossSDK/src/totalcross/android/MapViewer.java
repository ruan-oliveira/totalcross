/*********************************************************************************
 *  TotalCross Software Development Kit                                          *
 *  Copyright (C) 2000-2012 SuperWaba Ltda.                                      *
 *  All Rights Reserved                                                          *
 *                                                                               *
 *  This library and virtual machine is distributed in the hope that it will     *
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of    *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                         *
 *                                                                               *
 *********************************************************************************/

package totalcross.android;

import android.graphics.*;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.os.*;
import android.view.*;
import com.google.android.maps.*;
import java.util.*;

import totalcross.*;

public class MapViewer extends MapActivity 
{
   private int ilatMin=Integer.MAX_VALUE,ilatMax=Integer.MIN_VALUE;
   private int ilonMin=Integer.MAX_VALUE,ilonMax=Integer.MIN_VALUE;
   private int mRadius;
   
   private void computeBounds(int ilat, int ilon)
   {
      if (ilat < ilatMin) ilatMin = ilat;
      if (ilat > ilatMax) ilatMax = ilat;
      if (ilon < ilonMin) ilonMin = ilon;
      if (ilon > ilonMax) ilonMax = ilon;
   }

   private abstract class MapItem
   {
      public abstract void draw(Canvas canvas, Projection projection);
   }

   private class Circle extends MapItem
   {
      GeoPoint geo;
      double rad;
      boolean filled;
      int color;
      Circle(String s)
      {
         String[] ss = s.split(",");
         int i = 1;
         int lat = Integer.valueOf(ss[i++]);
         int lon = Integer.valueOf(ss[i++]);
         geo = new GeoPoint(lat,lon);
         computeBounds(lat,lon);
         rad  = Double.valueOf(ss[i++]);
         filled = ss[i++].equals("true");
         color = Integer.valueOf(ss[i++]); if ((color & 0xFF000000) == 0) color |= 0xFF000000;
      }
      public void draw(Canvas canvas, Projection projection)
      {
         Paint paint = new Paint();
         paint.setAntiAlias(true);
         Point point = new Point();
         projection.toPixels(geo, point);
         paint.setColor(color);
         paint.setStyle(filled ? Paint.Style.FILL_AND_STROKE : Paint.Style.STROKE);
         float frad = projection.metersToEquatorPixels((float)rad);
         RectF oval = new RectF(point.x - frad, point.y - frad, point.x + frad, point.y + frad);
         canvas.drawOval(oval, paint);
      }
   }
   private class Shape extends MapItem
   {
      GeoPoint []geos;
      boolean filled;
      int color;
      Shape(String s)
      {
         String[] ss = s.split(",");
         int i = 1;
         int n = Integer.valueOf(ss[i++]);
         geos = new GeoPoint[n];
         for (int j = 0; j < n; j++)
         {
            int lat = Integer.valueOf(ss[i++]);
            int lon = Integer.valueOf(ss[i++]);
            geos[j] = new GeoPoint(lat,lon);
            computeBounds(lat,lon);
         }
         filled = ss[i++].equals("true");
         color = Integer.valueOf(ss[i++]); if ((color & 0xFF000000) == 0) color |= 0xFF000000;
      }
      public void draw(Canvas canvas, Projection projection)
      {
         Point point = new Point();
         Paint paint = new Paint();
         paint.setAntiAlias(true);
         paint.setColor(color);
         paint.setStyle(filled ? Paint.Style.FILL_AND_STROKE : Paint.Style.STROKE);

         Path path = new Path();
         for (int i = 0; i < geos.length; i++)
         {
            projection.toPixels(geos[i], point);
            if (i == 0)
               path.moveTo(point.x, point.y); // used for first point
            else
               path.lineTo(point.x, point.y);
         }
         canvas.drawPath(path, paint);
      }
   }
   private class Place extends MapItem
   {
      GeoPoint geo;
      String pinFilename;
      String caption, detail;
      int backColor, capColor, detColor;
      Place(String s)
      {
         // *P*,"aaa","bbb","ccc",22.0,-22.1,0,1,2  - 
         String[] ss = s.split("\"");
         pinFilename = ss[1];
         caption = ss[3];
         detail = ss[5];
         ss = ss[6].split(",");
         int lat = Integer.valueOf(ss[1]);
         int lon = Integer.valueOf(ss[2]);
         geo = new GeoPoint(lat,lon);
         computeBounds(lat,lon);
         backColor = Integer.valueOf(ss[3]); if ((backColor & 0xFF000000) == 0) backColor |= 0xFF000000;
         capColor = Integer.valueOf(ss[4]);  if ((capColor  & 0xFF000000) == 0) capColor  |= 0xFF000000;
         detColor = Integer.valueOf(ss[5]);  if ((detColor  & 0xFF000000) == 0) detColor  |= 0xFF000000;
      }
      private void drawPin(Canvas canvas, Projection projection)
      {
         Paint paint = new Paint();
         paint.setAntiAlias(true);
         float frad = projection.metersToEquatorPixels((float)mRadius);
         Point point = new Point();
         projection.toPixels(geo, point);
         point.y -= frad * 3;
         paint.setColor(backColor);
         paint.setStyle(Paint.Style.FILL_AND_STROKE);
         Path path = new Path();
         RectF oval = new RectF(point.x - frad, point.y - frad, point.x + frad, point.y + frad);
         path.addArc(oval, 150,240);
         path.lineTo(point.x,point.y+frad*3);
         canvas.drawPath(path, paint);
      }
      private void drawBaloon(Canvas canvas, Projection projection)
      {
         Paint mpaint= new Paint();
         mpaint.setColor(backColor);
         mpaint.setStyle(Style.FILL);
         Paint paint2= new Paint();
         paint2.setColor(capColor);
         float frad = projection.metersToEquatorPixels((float)mRadius);
         Point point = new Point();
         projection.toPixels(geo, point);
         point.y -= frad * 4;
         paint2.setTextSize(frad*2);  //set text size

         FontMetrics fm = new FontMetrics();
         paint2.setTextAlign(Paint.Align.CENTER);
         paint2.getFontMetrics(fm);
         float ww = paint2.measureText(caption);
         
         
         canvas.drawRect(point.x - ww/2, point.y - frad*2, point.x + ww/2, point.y, mpaint);
         canvas.drawText(caption, point.x, point.y, paint2); //x=300,y=300    
      }
      public void draw(Canvas canvas, Projection projection)
      {
         drawPin(canvas,projection);
         drawBaloon(canvas,projection);
      }
   }
  
   public MapItem[] getItems(String s0)
   {
      String[] ss = s0.split("\\|");
      MapItem[] items = new MapItem[ss.length];
      for (int i = 0; i < ss.length; i++)
      {
         String s = ss[i];
         AndroidUtils.debug("item["+i+"]: "+s);
         if (s.startsWith("*S*"))
            items[i] = new Shape(s);
         else
         if (s.startsWith("*C*"))
            items[i] = new Circle(s);
         else
         if (s.startsWith("*P*"))
            items[i] = new Place(s);
            
      }
      return items;
   }
   
   /** Called when the activity is first created. */
   private class MyOverLay extends Overlay
   {
      private GeoPoint gp1;

      public MyOverLay(GeoPoint gp1) // GeoPoint is a int. (6E)
      {
         this.gp1 = gp1;
      }

      public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when)
      {
         Projection projection = mapView.getProjection();
         if (!shadow)
         {
            Paint paint = new Paint();
            paint.setAntiAlias(true);

            Point point = new Point();
            projection.toPixels(gp1, point);
            paint.setColor(Color.BLUE);
            RectF oval = new RectF(point.x - mRadius, point.y - mRadius, point.x + mRadius, point.y + mRadius);
            canvas.drawOval(oval, paint);
         }
         return super.draw(canvas, mapView, shadow, when);
      }
   }
   private class MyItemsOverLay extends Overlay
   {
      MapItem[] items;

      public MyItemsOverLay(MapItem[] items) // GeoPoint is a int. (6E)
      {
         this.items = items;
      }

      public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when)
      {
         Projection projection = mapView.getProjection();
         if (!shadow)
         {
            for (int i = 0; i < items.length; i++)
               items[i].draw(canvas, projection);
         }
         return super.draw(canvas, mapView, shadow, when);
      }
   }

   public void onCreate(Bundle savedInstanceState) 
   {
      super.onCreate(savedInstanceState);
      // setup the viewe
      MapView mapview = new MapView(this,"0FcAyehwXTAXpaMaXoVn7kGJVJdRSuSI2RsqELQ");
      mapview.setBuiltInZoomControls(true);
      mapview.setClickable(true);
      mapview.setStreetView(true);
      setContentView(mapview);
      if (Loader.isFullScreen)
         getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      // get passed parameters
      Bundle extras = getIntent().getExtras();
      mapview.setSatellite(extras.getBoolean("sat"));
      mRadius = Launcher4A.deviceFontHeight/2;
      List<Overlay> overs = mapview.getOverlays();
      String items = extras.getString("items");
      if (items != null)
      {
         AndroidUtils.debug("items: "+items);
         overs.add(new MyItemsOverLay(getItems(items)));
      }
      else
      {
         double lat = extras.getDouble("lat");
         double lon = extras.getDouble("lon");
         int ilat = toCoordI(lat);
         int ilon = toCoordI(lon);
         computeBounds(ilat,ilon);
         overs.add(new MyOverLay(new GeoPoint(ilat,ilon)));
      }      
      // move the map to the given point
      MapController mc = mapview.getController();
      int clat = (ilatMin + ilatMax) / 2;
      int clon = (ilonMin + ilonMax) / 2;
      mc.setCenter(new GeoPoint(clat,clon));
      mc.setZoom(21);
   }
   
   private static int toCoordI(double v)
   {
      return (int)(v * 1e6);
   }
   
   protected boolean isRouteDisplayed()
   {
      return false;
   }

   void centerMap(MapView mapView, GeoPoint center, int offX, int offY)
   {
      GeoPoint tl = mapView.getProjection().fromPixels(0, 0);
      GeoPoint br = mapView.getProjection().fromPixels(mapView.getWidth(), mapView.getHeight());

      int newLon = offX * (br.getLongitudeE6() - tl.getLongitudeE6()) / mapView.getWidth() + center.getLongitudeE6(); 
      int newLat = offY * (br.getLatitudeE6() - tl.getLatitudeE6()) / mapView.getHeight() + center.getLatitudeE6();

      mapView.getController().setCenter(new GeoPoint(newLat, newLon));
   }
}
