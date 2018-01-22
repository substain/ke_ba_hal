package hlt;


public class Vector2D {
      private double x, y;
      
      public Vector2D() {
          x = y = 0;      
      }
      
      public Vector2D(double x, double y) {
          this.x = x;
          this.y = y;      
      }
      
      public double length() {
            return Math.sqrt(x*x+y*y);      
      }
    
      public Vector2D add(Vector2D v) {
          return new Vector2D(x+v.x,y+v.y);      
      }
      
      public Vector2D minus(Vector2D v) {
          return new Vector2D(x-v.x,y-v.y);      
      }
      
      public void normalize() {
          double len = length();
          if( len != 0 )
          {
              x /= len;
              y /= len;
          } 
      }      
      
      public void setLength(double value) {
          normalize();
          x *= value;
          y *= value;      
      }
      
      public void nscale(double f) {
          normalize();
          x = x*f;
          y = y*f; 
     
     }
      
      public void scale(double f) {
           x = x*f;
           y = y*f; 
      
      }
      
      public void rotate(double theta, double cx, double cy) {
           double sin = Math.sin(theta);
           double cos = Math.cos(theta);       
           double dx = x-cx;
           double dy = y-cy;
           x = cx+(dx)*cos-(dy)*sin;
           y = cy+(dy)*cos+(dx)*sin;      
      }
      
      public Vector2D normal() { 
           return new Vector2D(x,-y);      
      }

      public double getX() {
    	  return x;
      }
      
      public double getY() {
    	  return y;
      }
      
      
      // the dot product implemented as class function
      static double dot(Vector2D v1, Vector2D v2) {
             return v1.x*v2.x+v1.y*v2.y;     
      }               
};