package jockey;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ComplexTest
{
    @Test
    void addTest()
    {
        var z = new Complex(3, 2);
        z.add(new Complex(1, 7));
        assertEquals(4, z.real);
        assertEquals(9, z.imaginary);
    }

    @Test
    void multiplyTest()
    {
        var z = new Complex(3, 2);
        z.multiply(new Complex(1, 7));
        assertEquals(-11, z.real);
        assertEquals(23, z.imaginary);
    }
}