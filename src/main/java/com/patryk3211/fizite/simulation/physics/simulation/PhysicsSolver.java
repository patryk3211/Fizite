package com.patryk3211.fizite.simulation.physics.simulation;

public class PhysicsSolver {
    private PhysicalSystem system;

    private PhysicalState[] initial;
    private PhysicalState[] intermediate;
    private double deltaTime;

    // Valid stages are 0, 1, 2, 3
    private int currentStage;

    private double stageTimeDelta;

    public void resize(int newSize) {
        if(newSize == 0) {
            initial = null;
            intermediate = null;
            return;
        }

        if(initial != null && initial.length > newSize)
            // Don't decrease size TODO: Maybe do if it's a big difference
            return;

        initial = new PhysicalState[newSize];
        intermediate = new PhysicalState[newSize];
        for(int i = 0; i < newSize; ++i) {
            initial[i] = new PhysicalState();
            intermediate[i] = new PhysicalState();
        }
    }

    public void start(double deltaTime, PhysicalSystem system) {
        this.system = system;
        resize(system.size());

        currentStage = 0;
        this.deltaTime = deltaTime;
    }

    public void step() {
        final var systemStates = system.getStates();
        switch(currentStage) {
            case 0:
                stageTimeDelta = deltaTime / 6.0;
                for(int i = 0; i < system.size(); ++i) {
                    if(systemStates[i] == null)
                        continue;
                    initial[i].copy(systemStates[i]);
                    intermediate[i].copy(systemStates[i]);
                }
                break;
            case 1:
            case 2:
                stageTimeDelta = deltaTime / 3.0;
                for(int i = 0; i < system.size(); ++i) {
                    final PhysicalState sState = systemStates[i];
                    if(sState == null)
                        continue;
                    sState.velocityA = initial[i].velocityA + deltaTime * sState.accelerationA * 0.5;
                    sState.positionA = initial[i].positionA + deltaTime * sState.velocityA * 0.5;
                    sState.velocity.x = sState.acceleration.x * deltaTime * 0.5 + initial[i].velocity.x;
                    sState.velocity.y = sState.acceleration.y * deltaTime * 0.5 + initial[i].velocity.y;
                    sState.position.x = sState.velocity.x * deltaTime * 0.5 + initial[i].position.x;
                    sState.position.y = sState.velocity.y * deltaTime * 0.5 + initial[i].position.y;
                }
                break;
            case 3:
                stageTimeDelta = deltaTime / 6.0;
                for(int i = 0; i < system.size(); ++i) {
                    final PhysicalState sState = systemStates[i];
                    if(sState == null)
                        continue;
                    sState.velocityA = initial[i].velocityA + deltaTime * sState.accelerationA;
                    sState.positionA = initial[i].positionA + deltaTime * sState.velocityA;
                    sState.velocity.x = sState.acceleration.x * deltaTime * 0.5 + initial[i].velocity.x;
                    sState.velocity.y = sState.acceleration.y * deltaTime * 0.5 + initial[i].velocity.y;
                    sState.position.x = sState.velocity.x * deltaTime * 0.5 + initial[i].position.x;
                    sState.position.y = sState.velocity.y * deltaTime * 0.5 + initial[i].position.y;
                }
                break;
            default:
                stageTimeDelta = 0;
        }
    }


    public void solve() {
        final var systemStates = system.getStates();
        for(int i = 0; i < system.size(); ++i) {
            final var sState = systemStates[i];
            if(sState == null)
                continue;
            final var iState = intermediate[i];
            iState.velocityA += sState.accelerationA * stageTimeDelta;
            iState.positionA += sState.velocityA * stageTimeDelta;

            iState.velocity.x += sState.acceleration.x * stageTimeDelta;
            iState.velocity.y += sState.acceleration.y * stageTimeDelta;
            iState.position.x += sState.velocity.x * stageTimeDelta;
            iState.position.y += sState.velocity.y * stageTimeDelta;
        }

        if(currentStage++ == 3) {
            for(int i = 0; i < system.size(); ++i) {
                final PhysicalState sState = systemStates[i];
                if(sState == null)
                    continue;
                final PhysicalState iState = intermediate[i];
                sState.velocityA = iState.velocityA;
                sState.positionA = iState.positionA;
                if(sState.positionA > Math.PI * 2) {
                    sState.positionA -= Math.PI * 2;
                }
                if(sState.positionA < Math.PI * -2) {
                    sState.positionA += Math.PI * 2;
                }
                sState.velocity.set(iState.velocity);
                sState.position.set(iState.position);
            }

            currentStage = 0;
        }
    }
}
