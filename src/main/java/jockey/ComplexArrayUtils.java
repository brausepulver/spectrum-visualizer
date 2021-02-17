package jockey;

public class ComplexArrayUtils
{
    public static Complex[] getComplex(double[] x)
    {
        int N = x.length;
        var X = new Complex[N];
        for (int i = 0; i < N; i++) {
            X[i] = new Complex(x[i], 0.0);
        }
        return X;
    }

    public static Complex[] getEven(Complex[] X)
    {
        int N = X.length;
        var even = new Complex[N/2];
        for (int i = 0; i < N/2; i++) {
            even[i] = X[2*i];
        }
        return even;
    }

    public static Complex[] getOdd(Complex[] X)
    {
        int N = X.length;
        var odd = new Complex[N/2];
        for (int i = 0; i < N/2; i++) {
            odd[i] = X[2*i+1];
        }
        return odd;
    }
}