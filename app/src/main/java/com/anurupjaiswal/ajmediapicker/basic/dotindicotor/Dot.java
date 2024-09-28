package com.anurupjaiswal.ajmediapicker.basic.dotindicotor;


/**
 * Created by Anurup Jaiswal on 27th August 2024
 */

public class Dot {
    enum State {
        SMALL,
        MEDIUM,
        INACTIVE,
        ACTIVE
    }

    private State state;

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

}