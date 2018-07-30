/*
 * RungeKutta.java by Richard J. Davies
 * from `Introductory Java for Scientists and Engineers'
 * chapter: `Numerical Computation'
 * section: `Runge-Kutta Methods'
 *
 * This problem uses Euclid's method and the fourth
 * order Runge-Kutta method to compute y at x=1
 * for the D.E. dy/dx = x * sqrt(1 + y*y)
 * with initial value y=0 at x=0.
 */

public class Java8RungeKutta
{
    // The number of steps to use in the interval
    public static final int STEPS = 100;

    // result
    public static double value = 0;


    // The derivative dy/dx at a given value of x and y.
    public static double deriv(double x, double y)
    {
        return x * Math.sqrt(1 + y*y);
    }


    // The `main' method does the actual computations
    public static void main(String[] argv)
    {
        // `h' is the size of each step.
        double h = 1.0 / STEPS;
        double k1, k2, k3, k4;
        double x, y;
        int i;


        // Computation by Euclid's method
        // Initialize y
        y = 0;

        for (i=0; i<STEPS; i++)
        {
            // Step through, updating x and incrementing y
            x = i * h;

            y += h * deriv(x, y);
        }

        // Print out the result that we get.
        System.out.println("Using the Euler method "
                + "The value at x=1 is:");
        System.out.println(y);


        // Computation by 4th order Runge-Kutta
        // Initialize y
        y = 0;

        for (i=0; i<STEPS; i++)
        {
            // Step through, updating x
            x = i * h;

            // Computing all of the trial values
            k1 = h * deriv(x, y);
            k2 = h * deriv(x + h/2, y + k1/2);
            k3 = h * deriv(x + h/2, y + k2/2);
            k4 = h * deriv(x + h, y + k3);

            // Incrementing y
            y += k1/6 + k2/3+ k3/3 + k4/6;
        }

        // Print out the result that we get.
        System.out.println();
        System.out.println("Using 4th order Runge-Kutta "
                + "The value at x=1 is:");
        System.out.println(y);

        value = y;


        // Computation by closed form solution
        // Print out the result that we get.
        System.out.println();
        System.out.println("The value really is:");
        y = (Math.exp(0.5) - Math.exp(-0.5)) / 2;
        System.out.println(y);
    }
}

// duplicate to make the test code larger
class Java8RungeKutta01
{
    // The number of steps to use in the interval
    public static final int STEPS = 100;

    // result
    public static double value = 0;


    // The derivative dy/dx at a given value of x and y.
    public static double deriv(double x, double y)
    {
        return x * Math.sqrt(1 + y*y);
    }


    // The `main' method does the actual computations
    public static void main(String[] argv)
    {
        // `h' is the size of each step.
        double h = 1.0 / STEPS;
        double k1, k2, k3, k4;
        double x, y;
        int i;


        // Computation by Euclid's method
        // Initialize y
        y = 0;

        for (i=0; i<STEPS; i++)
        {
            // Step through, updating x and incrementing y
            x = i * h;

            y += h * deriv(x, y);
        }

        // Print out the result that we get.
        System.out.println("Using the Euler method "
                + "The value at x=1 is:");
        System.out.println(y);


        // Computation by 4th order Runge-Kutta
        // Initialize y
        y = 0;

        for (i=0; i<STEPS; i++)
        {
            // Step through, updating x
            x = i * h;

            // Computing all of the trial values
            k1 = h * deriv(x, y);
            k2 = h * deriv(x + h/2, y + k1/2);
            k3 = h * deriv(x + h/2, y + k2/2);
            k4 = h * deriv(x + h, y + k3);

            // Incrementing y
            y += k1/6 + k2/3+ k3/3 + k4/6;
        }

        // Print out the result that we get.
        System.out.println();
        System.out.println("Using 4th order Runge-Kutta "
                + "The value at x=1 is:");
        System.out.println(y);

        value = y;


        // Computation by closed form solution
        // Print out the result that we get.
        System.out.println();
        System.out.println("The value really is:");
        y = (Math.exp(0.5) - Math.exp(-0.5)) / 2;
        System.out.println(y);
    }
}

// duplicate to make the test code larger
class Java8RungeKutta02
{
    // The number of steps to use in the interval
    public static final int STEPS = 100;

    // result
    public static double value = 0;


    // The derivative dy/dx at a given value of x and y.
    public static double deriv(double x, double y)
    {
        return x * Math.sqrt(1 + y*y);
    }


    // The `main' method does the actual computations
    public static void main(String[] argv)
    {
        // `h' is the size of each step.
        double h = 1.0 / STEPS;
        double k1, k2, k3, k4;
        double x, y;
        int i;


        // Computation by Euclid's method
        // Initialize y
        y = 0;

        for (i=0; i<STEPS; i++)
        {
            // Step through, updating x and incrementing y
            x = i * h;

            y += h * deriv(x, y);
        }

        // Print out the result that we get.
        System.out.println("Using the Euler method "
                + "The value at x=1 is:");
        System.out.println(y);


        // Computation by 4th order Runge-Kutta
        // Initialize y
        y = 0;

        for (i=0; i<STEPS; i++)
        {
            // Step through, updating x
            x = i * h;

            // Computing all of the trial values
            k1 = h * deriv(x, y);
            k2 = h * deriv(x + h/2, y + k1/2);
            k3 = h * deriv(x + h/2, y + k2/2);
            k4 = h * deriv(x + h, y + k3);

            // Incrementing y
            y += k1/6 + k2/3+ k3/3 + k4/6;
        }

        // Print out the result that we get.
        System.out.println();
        System.out.println("Using 4th order Runge-Kutta "
                + "The value at x=1 is:");
        System.out.println(y);

        value = y;


        // Computation by closed form solution
        // Print out the result that we get.
        System.out.println();
        System.out.println("The value really is:");
        y = (Math.exp(0.5) - Math.exp(-0.5)) / 2;
        System.out.println(y);
    }
}

// duplicate to make the test code larger
class Java8RungeKutta03
{
    // The number of steps to use in the interval
    public static final int STEPS = 100;

    // result
    public static double value = 0;


    // The derivative dy/dx at a given value of x and y.
    public static double deriv(double x, double y)
    {
        return x * Math.sqrt(1 + y*y);
    }


    // The `main' method does the actual computations
    public static void main(String[] argv)
    {
        // `h' is the size of each step.
        double h = 1.0 / STEPS;
        double k1, k2, k3, k4;
        double x, y;
        int i;


        // Computation by Euclid's method
        // Initialize y
        y = 0;

        for (i=0; i<STEPS; i++)
        {
            // Step through, updating x and incrementing y
            x = i * h;

            y += h * deriv(x, y);
        }

        // Print out the result that we get.
        System.out.println("Using the Euler method "
                + "The value at x=1 is:");
        System.out.println(y);


        // Computation by 4th order Runge-Kutta
        // Initialize y
        y = 0;

        for (i=0; i<STEPS; i++)
        {
            // Step through, updating x
            x = i * h;

            // Computing all of the trial values
            k1 = h * deriv(x, y);
            k2 = h * deriv(x + h/2, y + k1/2);
            k3 = h * deriv(x + h/2, y + k2/2);
            k4 = h * deriv(x + h, y + k3);

            // Incrementing y
            y += k1/6 + k2/3+ k3/3 + k4/6;
        }

        // Print out the result that we get.
        System.out.println();
        System.out.println("Using 4th order Runge-Kutta "
                + "The value at x=1 is:");
        System.out.println(y);

        value = y;


        // Computation by closed form solution
        // Print out the result that we get.
        System.out.println();
        System.out.println("The value really is:");
        y = (Math.exp(0.5) - Math.exp(-0.5)) / 2;
        System.out.println(y);
    }
}
