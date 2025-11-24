package edu.curtin.saed.assignment1;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

 /*
 SAED COMP3003
 Assignment 1
 Brian Tran
 20985093
  */

public class App extends Application
{
    //Ui fields
    private GridArea area;
    private TextArea textArea = new TextArea();
    private Label statusText = new Label("Planes in flight: #, Planes Servicing: #, Completed Trips: #");//set text before start is pressed

    //Hardcoded Specified quantities
    private double mapW = 12.0;
    private double mapH = 12.0;

    private int nAirports = 10;
    private int nPlanePAirport = 10;

    private List<Airport> airportList = new LinkedList<>();//keep track of all airports

    private int totalPlanes = 0;//var for captioning planes

    //atomics for threads sharing vars
    private AtomicInteger planesFlying = new AtomicInteger(0);
    private AtomicInteger planesServicing = new AtomicInteger(0);
    private AtomicInteger completedTrips = new AtomicInteger(0);

    //threads
    private ExecutorService destinationCaller, flight;
    private Thread statusLabelThread;
    private ServicingThreadPool servicingThreadPool = new ServicingThreadPool();
    public static void main(String[] args)
    {
        launch();
    }

    @Override
    public void start(Stage stage)
    {
        area = new GridArea(mapW, mapH);
        // area.setGridLines(false); // If desired
        area.setStyle("-fx-background-color: #006000;");

        //randomise airport positions
        Random rand = new Random();
        for (int i = 0; i < nAirports; i++)
        {
            //rand only gives 0.0 to 1.0, so multiply by boundaries to spread it out
            double x = rand.nextDouble() * mapW;
            double y = rand.nextDouble() * mapH;

            airportList.add(new Airport(x, y, "Airport " + i, nAirports));//add to list
        }

        //set up map
        for (Airport airport : airportList)
        {
            //create airport icons on map
            area.getIcons().add(new GridAreaIcon(airport.getX(), airport.getY(), 0.0, 1.0, App.class.getClassLoader().getResourceAsStream("airport.png"), airport.getCaption()));
            for (int i = 0; i < nPlanePAirport; i++)
            {
                totalPlanes++;//var for counting and naming planes easier
                //basically making the planes per airport in this loop
                Plane plane = new Plane(airport.getX(), airport.getY(), "Plane " + totalPlanes);
                GridAreaIcon planeIcon = new GridAreaIcon(plane.getX(), plane.getY(), 0.0, 1.0, App.class.getClassLoader().getResourceAsStream("plane.png"), plane.getCaption());
                planeIcon.setShown(false);
                plane.setIcon(planeIcon);//providing plane with access to its own icon
                area.getIcons().add(planeIcon); 

                airport.parked(plane);//add to airports blocking q of ready to fly planes
            }
        }

        //Buttons
        var startBtn = new Button("Start");
        var endBtn = new Button("End");

        startBtn.setOnAction((event) ->
        {
            System.out.println("\nStart button pressed");
            Thread thread = new Thread(() -> {//put the methods in a thread here because gui would jitter disappear and reappear in .5 secs so this is smoother
                //i believe its due to the for each looping through the list, this thread just made it smooth when i start
                //otherwise the thread itself isn't that necessary
                for (Airport airport : airportList)
                {
                    airport.startThreadFlightReq();
                }

                startFlyingThread();// start flying thread first for destination thread to enter planes without it breaking
                startDestinationCallerThreadPool();
                servicingThreadPool.startServicingPool();
                
                startStatusLabelThread();
            });
            thread.start();

        });
        endBtn.setOnAction((event) ->
        {
            System.out.println("\nEnd button pressed");
            stoppingAirportThreads();
            stopDestinationCaller();
            stopFlyingThread();
            servicingThreadPool.stopServicingPool();
            stopStatusLabelThread();
        });
        stage.setOnCloseRequest((event) ->
        {
            System.out.println("\nClose button pressed");
            stoppingAirportThreads();
            stopDestinationCaller();
            stopFlyingThread();
            servicingThreadPool.stopServicingPool();
            stopStatusLabelThread();
        });

        // Below is basically just the GUI "plumbing" (connecting things together).

        var toolbar = new ToolBar();
        toolbar.getItems().addAll(startBtn, endBtn, new Separator(), statusText);

        var splitPane = new SplitPane();
        splitPane.getItems().addAll(area, textArea);
        splitPane.setDividerPositions(0.75);

        stage.setTitle("Air Traffic Simulator");
        var contentPane = new BorderPane();
        contentPane.setTop(toolbar);
        contentPane.setCenter(splitPane);

        var scene = new Scene(contentPane, 1200, 1000);
        stage.setScene(scene);
        stage.show();
    }

    public void stoppingAirportThreads()
    {
        for (Airport airport : airportList)
        {
            airport.stopThread();
        }
    }

    //start thread for my destinations for planes to fly
    public void startDestinationCallerThreadPool()
    {
        if(destinationCaller == null){
            destinationCaller = Executors.newFixedThreadPool(nAirports);//make a fixed pool size of airports

            for (Airport airport : airportList) {//send an airport to a thread so its calling blockqueue doesn't block gui
                destinationCaller.submit(() -> {
                    while(!Thread.currentThread().isInterrupted())//this is so when it gets interrupted, it leaves without running the blocking q getDest as i had it on while(true) before
                    {
                        int dest = airport.getDest();

                        for (Airport airport2 : airportList) {//filtering airports for correct match
                            if(airport2.getId() == dest)//if found
                            {
                                //get plane from origin airports ready to fly list
                                //assign it the destination and send to the flight threadpool
                                Plane plane = airport.getfly();
                                plane.setDestination(airport2);
                                startPlaneFlight(plane);
                                break;//code is done so escape
                            }
                        }
                    }
                });
            }
        }
    }

    public void stopDestinationCaller()
    {
        if(destinationCaller != null)
        {
            destinationCaller.shutdownNow();
        }
    }

    public void startFlyingThread()
    {
        if (flight == null){
            flight = Executors.newCachedThreadPool();//planes at any given moment is varied so just let it be unbounded
        }//avg is roughly 20 odd planes ever, but this way it allows easier scaling if we add more airports and planes
    }

    //start method for submitting plane to thread pool and altering ui
    public void startPlaneFlight(Plane plane)
    {
        Platform.runLater(() -> {//textArea can only be altered properly when on a different thread using runLater
            textArea.appendText(plane.getCaption() + " has departed to " + plane.getDestinationCaption() + "\n");
        });
        planesFlying.incrementAndGet();//+1 avoids multiple threads accessing this at once and messing up the count value
        Future<?> future = flight.submit(plane);//future object for below update

        Thread thread = new Thread(() -> {
            try {
                future.get();//let thread finish first before i in/decrement and append landing text, since its blocking made a thread so it wouldn't freeze my destinationcaller thread while its on loop
                //code below only runs if the flight thread for the plane's run() completed successfully
                planesFlying.decrementAndGet();
                completedTrips.incrementAndGet();

                Platform.runLater(() -> {
                    textArea.appendText(plane.getCaption() + " has landed in " + plane.getDestinationCaption() + "\n");
                });
                servicingThreadPool.addPlaneToService(plane, planesServicing);//send plane to get serviced, atomic integer so count can get displayed properly
            } catch (InterruptedException | ExecutionException e) {//ExecutionException is from the throw new Runtime in plane.run()
                System.out.println("PlaneFly Exception: " + plane.getCaption());
            }
        });
        thread.start();
    }

    public void stopFlyingThread()
    {
        if(flight != null)
        {
            flight.shutdownNow();
        }
    }

    //starting ui thread 
    public void startStatusLabelThread()
    {
        if(statusLabelThread == null){
            statusLabelThread = new Thread(() -> {
                while(!Thread.currentThread().isInterrupted())
                {
                    Platform.runLater(() -> {
                        statusText.setText("Planes in flight: " + planesFlying.get() + ", Planes Servicing: " + planesServicing.get() + ", Completed Trips: " + completedTrips.get());
                    });
                    try {
                        area.requestLayout();//setting all planes new icons on map
                        Thread.sleep(20);//less than a plane's 100ms x y update, so their movement is much smoother when planes come out at diff times
                    } catch (InterruptedException e) {
                        System.out.println("Status interrupted exception");
                        Thread.currentThread().interrupt();
                    }
                }
            });
            statusLabelThread.start();
        }
    }

    public void stopStatusLabelThread()
    {
        if(statusLabelThread != null)
        {
            statusLabelThread.interrupt();
        }
    }
}
