package edu.curtin.saed.assignment1;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ServicingThreadPool {//class for servicing
    private ExecutorService servicingPool;

    public void startServicingPool()
    {
        if (servicingPool == null){
            servicingPool = Executors.newCachedThreadPool();//varied time length of servicing, and planes landing at any given time so to be safe
        }
    }

    public void addPlaneToService(Plane plane, AtomicInteger planesServicing)
    {
        servicingPool.submit(() -> {
            try {
                planesServicing.incrementAndGet();//+1 to planes servicing

                StandardPlaneServicing.service(plane.getDestinationAirport().getId(), plane.getId());//blocking with thread sleep
                //once unblocked below part is run
                plane.getDestinationAirport().parked(plane);
                planesServicing.decrementAndGet();//-1 after the blocking method is finished
            } catch (InterruptedException e) {
                System.out.println("Plane servicing Interrupted for " + plane.getCaption() + " at " + plane.getDestinationCaption());
            }
        });
    }

    public void stopServicingPool()
    {
        if(servicingPool != null)
        {
            servicingPool.shutdownNow();
        }
    }
}
