package edu.curtin.saed.assignment1;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

//class to hold Airport Object Data
public class Airport {
    private static final Logger logger = Logger.getLogger(Airport.class.getName());

    private double x, y;
    private String caption;
    private int id;

    private BlockingQueue<Plane> planesReady = new LinkedBlockingQueue<>();//only planes that are ready get put in here
    private BlockingQueue<Integer> destinationIDs = new LinkedBlockingQueue<>();
    private Thread flightReqT, reader;
    private StandardFlightRequests flightRequests;


    public Airport(double x, double y, String caption, int nAirports)
    {
        this.x = x;
        this.y = y;
        this.caption = caption;
        this.id = this.provideId();
        this.flightRequests = new StandardFlightRequests(nAirports, id);
    }

    public void startThreadFlightReq() {
        //shouldn't ever hit this since i plan to call it once at the start
        if (flightReqT != null)//well i guess if the user hits the start button multiple times
        {
            System.out.println(caption + " has already started");
            return;
        }

        flightReqT = new Thread(
            () -> {
                try
                {
                    flightRequests.go();
                } catch(InterruptedException e)
                {
                    logger.info(() -> caption + " has stopped flight req thread");
                }
            }, "flight requests in " + caption);
        
        flightReqT.start();
        logger.info(() -> caption + " has started Thread");

        //threading for buffered reader to not block and continuously read ids
        reader = new Thread(() -> {
            try(BufferedReader br = flightRequests.getBufferedReader())
            {
                String line;
                while((line = br.readLine()) != null)
                {
                    System.out.println(caption + " -> " + line);//

                    //parse it into blocking q now
                    destinationIDs.put(Integer.parseInt(line));
                }
            }
            catch(IOException e)
            {
                System.err.printf("reader: Could not run command: %s: %s", e.getClass(), e.getMessage());
            }
            catch(InterruptedException e)
            {
                logger.info(() -> "Stopped destinationID put in " + caption);
            }
        });
        reader.start();
        logger.info(() -> "Started Reader in " + caption);
    }

    public void stopThread() {
        if(flightReqT != null)
        {
            flightReqT.interrupt();
            reader.interrupt();
        }
        System.out.println("\n");
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public int getId() {
        return id;
    }

    private int provideId() {
        //I'll be creating Airport objects with the caption "Airport #"
        //therefore parse and remove the Airport bit and keep the number part for the ID

        String[] parts = caption.split(" ");//splitting on space for 2 parts "Airport #"

        return Integer.parseInt(parts[1]);
    }

    public void parked(Plane plane)
    {
        try {
            planesReady.put(plane);
        } catch (InterruptedException e) {
            System.out.println("Interrupted parked in " + caption);
        }
    }

    //method to retrieve a plane object to be used from a blocking q
    public Plane getfly()
    {
        Plane plane = null;
        try {
            plane = planesReady.take();
        } catch (InterruptedException e) {
            System.out.println("Interrupted getfly in " + caption);//doesn't reach down here but safety measures anyways
            Thread.currentThread().interrupt();//usually its the getDest() that ends the threads
        }
        return plane;
    }

    //method to get from destination blocking q the destination ids
    public int getDest()
    {
        int dest = -1;
        try {
            dest = destinationIDs.take();
        } catch (InterruptedException e) {
            System.out.println("Interrupted in getDest() Airport");
            Thread.currentThread().interrupt();//allows destinationCaller to exit gracefully
        }
        return dest;
    }
}
