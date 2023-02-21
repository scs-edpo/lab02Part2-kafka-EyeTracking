package com.data;

public class Gaze {

    int eventID;
    long timestamp;
    int xPosition; // position of the gaze within the x-coordinate of the screen
    int yPosition; // position of the gaze within the y-coordinate of the screen
    int pupilSize; // size of the eye pupil as captured by the eye-tracker


    public Gaze(int eventID, long timestamp, int xPosition, int yPosition, int pupilSize) {
        this.eventID = eventID;
        this.timestamp = timestamp;
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.pupilSize = pupilSize;
    }

    public int getEventID() {
        return eventID;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getxPosition() {
        return xPosition;
    }

    public int getyPosition() {
        return yPosition;
    }

    public int getPupilSize() {
        return pupilSize;
    }


    public String toString()
    {
        return "eventID: "+eventID+", " +
                "timestamp: "+timestamp+", " +
                "xPosition: "+xPosition+", " +
                "yPosition: "+yPosition+", " +
                "pupilSize: "+pupilSize+", ";
    }
}
