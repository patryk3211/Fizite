package com.patryk3211.fizite.simulation.physics.simulation;

public class PhysicalSystem {
    private PhysicalState[] states;
    private int length;

    public PhysicalSystem() {
        states = null;
        length = 0;
    }

    public void resize(int newSize) {
        if(newSize == 0) {
            states = null;
        }

        if(states != null && states.length > newSize) {
            length = newSize;
            return;
        }

        PhysicalState[] newStates = new PhysicalState[newSize];
        if(states != null)
            System.arraycopy(states, 0, newStates, 0, states.length);

        states = newStates;
        length = newSize;
    }

    public void setState(int index, PhysicalState state) {
        states[index] = state;
    }

    public PhysicalState[] getStates() {
        return states;
    }

    public int size() {
        return length;
    }
}
