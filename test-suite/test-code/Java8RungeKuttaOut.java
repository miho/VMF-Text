 public class Java8RungeKuttaOut {
  public static final int STEPS = 100 ;
  public static double value = 0 ;
  public static double deriv ( double x , double y ) {
    return x * Math . sqrt ( 1 + y * y ) ;
  }
  public static void main ( String [] argv ) {
    double h = 1.0 / STEPS ;
    double k1 , k2 , k3 , k4 ;
    double x , y ;
    int i ;
    y = 0 ;
    for ( i = 0 ; i < STEPS ; i ++ ) {
      x = i * h ;
      y += h * deriv ( x , y ) ;
    }
    System . out . println ( "Using the Euler method " + "The value at x=1 is:" ) ;
    System . out . println ( y ) ;
    y = 0 ;
    for ( i = 0 ; i < STEPS ; i ++ ) {
      x = i * h ;
      k1 = h * deriv ( x , y ) ;
      k2 = h * deriv ( x + h / 2 , y + k1 / 2 ) ;
      k3 = h * deriv ( x + h / 2 , y + k2 / 2 ) ;
      k4 = h * deriv ( x + h , y + k3 ) ;
      y += k1 / 6 + k2 / 3 + k3 / 3 + k4 / 6 ;
    }
    System . out . println ( ) ;
    System . out . println ( "Using 4th order Runge-Kutta " + "The value at x=1 is:" ) ;
    System . out . println ( y ) ;
    value = y ;
    System . out . println ( ) ;
    System . out . println ( "The value really is:" ) ;
    y = ( Math . exp ( 0.5 ) - Math . exp ( - 0.5 ) ) / 2 ;
    System . out . println ( y ) ;
  }
}