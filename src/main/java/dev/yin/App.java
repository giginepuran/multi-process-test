package dev.yin;

import dev.yin.process.ParentProcess;
/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        // parse args
        int childProcessCount = Integer.parseInt(args[0]);
        int threadCount = Integer.parseInt(args[1]);

        // setup shared memory, config, etc.
        ParentProcess parent = new ParentProcess(
            1000, 
            childProcessCount, 
            threadCount, 
            1000);

        // start the parent logic
        parent.start();
    }
}
