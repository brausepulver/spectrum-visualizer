package jockey;

public class Complex
{
    public double real, imaginary;

    public Complex(double real, double imaginary)
    {
        this.real = real;
        this.imaginary = imaginary;
    }

    public Complex(double theta)
    {
        real = Math.cos(theta);
        imaginary = Math.sin(theta);
    }

    public Complex(Complex other)
    {
        real = other.real;
        imaginary = other.imaginary;
    }

    public void add(Complex other)
    {
        real += other.real;
        imaginary += other.imaginary;
    }

    public void subtract(Complex other)
    {
        real -= other.real;
        imaginary -= other.imaginary;
    }

    public void multiply(Complex other)
    {
        double t = real;
        real = real*other.real - imaginary*other.imaginary;
        imaginary = t*other.imaginary + imaginary*other.real;
    }

    public double getNorm()
    {
        return Math.sqrt(real*real + imaginary*imaginary);
    }

    public String toString()
    {
        return real + " " + (imaginary >= 0 ? "+" : "-") + " " + Math.abs(imaginary) + "j";
    }
}