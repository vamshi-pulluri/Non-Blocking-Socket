package main.java.Client.view;

public class ThreadSafeStdOut {
	synchronized void print(String output) {
        System.out.print(output);
    }

    synchronized void println(String output) {
        System.out.println(output);
    }
}
