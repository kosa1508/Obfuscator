public class TestClass {
    private int counter = 0;
    private String name;
    private int[] numbers;

    public TestClass(String name) {
        this.name = name;
        this.numbers = new int[10];
        initNumbers();
    }

    private void initNumbers() {
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = i * 2;
        }
    }

    public int calculateSum(int a, int b) {
        int result = a + b;
        counter++;

        for (int i = 0; i < 3; i++) {
            result += i;
        }

        if (result > 10) {
            result = adjustResult(result);
        } else {
            result = result * 2;
        }
        logResult(result);
        return result;
    }

    private int adjustResult(int value) {
        int tmp = value - 3;

        int i = 0;
        while (i < 2) {
            tmp += 1;
            i++;
        }

        if (tmp < 0) {
            tmp = 0;
        }
        return tmp;
    }


    public void printNumbers() {
        for (int num : numbers) {
            System.out.print(num + " ");
        }
        System.out.println();
    }

    private void logResult(int value) {
        System.out.println("[" + name + "] counter=" + counter + ", value=" + value);
    }

    public void reset() {
        counter = 0;
        name = "reset_" + name;

        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = 0;
        }
    }

    public void nestedLoopExample() {
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                System.out.println("x=" + x + ", y=" + y);
            }
        }
    }

    public int findMax() {
        int max = numbers[0];
        for (int i = 1; i < numbers.length; i++) {
            if (numbers[i] > max) {
                max = numbers[i];
            }
        }
        return max;
    }

    public void doWhileExample() {
        int count = 0;
        do {
            System.out.println("Count: " + count);
            count++;
        } while (count < 3);
    }
}