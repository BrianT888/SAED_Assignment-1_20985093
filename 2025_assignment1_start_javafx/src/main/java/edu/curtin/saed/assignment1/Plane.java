package edu.curtin.saed.assignment1;

//class to hold Plane Object Data
public class Plane implements Runnable{
    private double x, y;
    private String caption;
    private int id;

    private Airport destinationAirport;
    private double speed = .12;//since im running it 10 times a second, i divided by 1.2 / 10

    private GridAreaIcon icon;//keep hold of its own icon

    public Plane(double x, double y, String caption)
    {
        this.x = x;
        this.y = y;
        this.caption = caption;
        this.id = parseId();
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

    private int parseId() {
        //I'll be creating Plane objects with the caption "Plane #"
        //therefore parse and remove the Plane bit and keep the number part for the ID

        String[] parts = caption.split(" ");//splitting on space for 2 parts "Plane #"

        return Integer.parseInt(parts[1]);
    }

    public void setDestination(Airport destination)
    {
        destinationAirport = destination;
    }

    public String getDestinationCaption()
    {
        return destinationAirport.getCaption();
    }

    public Airport getDestinationAirport()
    {
        return destinationAirport;
    }

    public void setIcon(GridAreaIcon icon)
    {
        this.icon = icon;
    }

    @Override
    public void run() {
        try {
            icon.setShown(true);
            //while coords don't match
            while (this.x != destinationAirport.getX() || this.y != destinationAirport.getY())
            {
                double xDiff = destinationAirport.getX() - this.x;
                double yDiff = destinationAirport.getY() - this.y;

                //calc distance between current and airport
                double distance = Math.sqrt(xDiff * xDiff + yDiff * yDiff);

                if(distance > 0)
                {
                    if(distance < 0.12)
                    {
                        //hard set it since the distance is less than the speed to end loop
                        this.x = destinationAirport.getX();
                        this.y = destinationAirport.getY();

                        icon.setShown(false);//icon disappears as we are at airport
                    } else//keep moving
                    {
                        this.x += xDiff/distance * speed;
                        this.y += yDiff/distance * speed;
                    }
                }

                icon.setPosition(x, y);
                Thread.sleep(100);//to make it run 10 times a second
                //which is why the unit of movement is .12, as a full second is 1.2
            }

        } catch (InterruptedException e) {
            System.out.println(caption + " run() interrupted");
            //throwing this so that my future.get() will throw an executionexception
            //and skip out on the try and go to the catch part, avoids updating textarea prematurely
            throw new PlaneExecutionException("Plane.run() interrupted", e);
        }
    }
}
