/*
 * Sample input for the Java 8 round-trip showcase.
 *
 * Every comment, blank line and odd spacing in this file survives
 * parse -> unparse byte-for-byte.
 */
package demo.hello ;

/* class comment */
public  class Greeter {

  // intentionally odd formatting around the method and string
  public static void greet( String[] args ){
    System.out.println( "hello" );   // trailing comment
  }

}
