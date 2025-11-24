package edu.curtin.saed.assignment1;

public class PlaneExecutionException extends RuntimeException{//Class only for plane's run method to throw and loop out of it for future.get()
    public PlaneExecutionException(String msg, Throwable e)
    {
        super(msg, e);
    }
}
