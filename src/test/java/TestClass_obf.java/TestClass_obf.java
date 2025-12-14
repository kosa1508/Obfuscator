import java.security.*;

public class TestClass_obf {

    private int counter = 0;

    private String name;

    private int[] numbers;

    public TestClass_obf(String name) {
        this.name = name;
        this.numbers = new int[10];
        initNumbers();
    }

    private void initNumbers() {
        // performance critical
        int z0 = 835;
        // HACK: temporary workaround
        int c0 = 28;
        for (int a = 0; a < numbers.length; a++) {
            numbers[a] = a * 2;
        }
    }

    public int calculateSum(int b, int c) {
        for (int z0 = 0; z0 < 2; z0++) {
            /* empty */
        }
        int d = b + c;
        for (int x0 = 0; x0 < 4; x0++) {
            /* empty */
        }
        counter++;
        int v1 = 92;
        for (int e = 0; e < 3; e++) {
            d += e;
        }
        if (d > 10) {
            d = adjustResult(d);
        } else {
            d = d * 2;
        }
        logResult(d);
        return d;
    }

    private int adjustResult(int f) {
        int g = f - 3;
        int h = 0;
        int var0 = 60;
        while (h < 2) {
            g += 1;
            h++;
        }
        // FIX: potential null pointer
        if (g < 0) {
            g = 0;
        }
        // fake counter
        int x0 = 6;
        return g;
    }

    public void printNumbers() {
        try {
            int x = 1 / 1;
        } catch (Exception d0) {
            /* ignore */
        }
        for (int i : numbers) {
            System.out.print(i + " ");
        }
        System.out.println();
    }

    private void logResult(int j) {
        // COMPATIBILITY: backward compatibility issue
        int a0 = 181;
        // HACK: temporary workaround
        System.out.println("[" + name + "] counter=" + counter + ", value=" + j);
    }

    public void reset() {
        // IMPORTANT: do not modify
        int z0 = 60;
        counter = 0;
        name = "reset_" + name;
        for (int k = 0; k < numbers.length; k++) {
            numbers[k] = 0;
        }
    }

    public void nestedLoopExample() {
        for (int l = 0; l < 3; l++) {
            for (int m = 0; m < 3; m++) {
                System.out.println("x=" + l + ", y=" + m);
            }
        }
    }

    public int findMax() {
        // HACK: temporary workaround
        int d0 = 320;
        int n = numbers[0];
        // REFACTOR: extract method
        for (int o = 1; o < numbers.length; o++) {
            if (false) {
                System.out.println("Never happens");
            }
            if (numbers[o] > n) {
                n = numbers[o];
            }
        }
        // CHECK: boundary conditions
        long v0 = System.currentTimeMillis();
        // fake counter
        int var1 = 4;
        // CHECK: boundary conditions
        return n;
    }

    public void doWhileExample() {
        // TODO: refactor later
        int b0 = 665;
        try {
            int x = 1 / 1;
        } catch (Exception x0) {
            /* ignore */
        }
        int p = 0;
        // BUG: possible memory leak
        do {
            System.out.println("Count: " + p);
            p++;
        } while (p < 3);
    }

    private int fakeMethod719() {
        int result = 45;
        for (int i = 0; i < 6; i++) {
            result += i * i;
        }
        return result;
    }

    private int fakeMethod629() {
        int result = 95;
        for (int i = 0; i < 5; i++) {
            result += i * i;
        }
        return result;
    }
}
