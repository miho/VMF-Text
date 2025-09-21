import static java.lang.StringTemplate.STR;

/*
 * RungeKutta.java by Richard J. Davies
 * from `Introductory Java for Scientists and Engineers'
 *
 * This version is modernized to use Java 24 features.
 *
 * This problem uses Euler's method and the fourth-order
 * Runge-Kutta method to compute y at x=1
 * for the D.E. dy/dx = x * sqrt(1 + y*y)
 * with initial value y=0 at x=0.
 *
 * Note: String Templates (STR) are a preview feature in Java 24.
 * To compile and run, use:
 * javac --release 24 --enable-preview Java24RungeKutta.java
 * java --enable-preview Java24RungeKutta
 */
public class Java24RungeKutta {
    // The number of steps to use in the interval (Public API)
    public static final int STEPS = 100;

    // Result holder for external access (Public API)
    public static double value = 0;


    /**
     * The derivative dy/dx at a given value of x and y. (Public API)
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return The value of the derivative dy/dx.
     */
    public static double deriv(double x, double y) {
        return x * Math.sqrt(1 + y * y);
    }


    /**
     * The `main` method performs the computations. (Public API)
     */
    public static void main(String[] argv) {
        // Use 'var' for local variable type inference (Java 10+).
        // 'h' is the size of each step.
        final var h = 1.0 / STEPS;

        // --- Computation by Euler's method ---
        // Variable 'yEuler' is scoped to this calculation.
        var yEuler = 0.0;
        for (var i = 0; i < STEPS; i++) {
            var x = i * h;
            yEuler += h * deriv(x, yEuler);
        }

        // Use a text block (Java 15+) and a string template (Java 21+) for clean output.
        // The original code had a typo "Euclid's method" which has been corrected to "Euler's method".
        System.out.println(STR."""
        Using the Euler method, the value at x=1 is:
        \{yEuler}
        """);


        // --- Computation by 4th order Runge-Kutta ---
        var yRk4 = 0.0;
        for (var i = 0; i < STEPS; i++) {
            var x = i * h;

            // Compute the four trial values (k1, k2, k3, k4).
            var k1 = h * deriv(x, yRk4);
            var k2 = h * deriv(x + h / 2, yRk4 + k1 / 2);
            var k3 = h * deriv(x + h / 2, yRk4 + k2 / 2);
            var k4 = h * deriv(x + h, yRk4 + k3);

            // Increment y using the standard weighted average.
            yRk4 += (k1 + 2 * k2 + 2 * k3 + k4) / 6.0;
        }

        System.out.println(STR."""
        Using 4th order Runge-Kutta, the value at x=1 is:
        \{yRk4}
        """);

        // Update the public static field to maintain the original API's behavior.
        value = yRk4;


        // --- Computation by closed-form solution ---
        // The exact solution is sinh(x^2 / 2). At x=1, this is sinh(0.5).
        // Using Math.sinh() is more descriptive than the exponential form.
        var yExact = Math.sinh(0.5);
        System.out.println(STR."""
        The exact analytical value is:
        \{yExact}
        """);
    }
}