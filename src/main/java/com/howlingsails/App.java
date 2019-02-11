package com.howlingsails;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        DependencyProcessor dependencyProcessor = new DependencyProcessor();
        dependencyProcessor.go();
    }
}
