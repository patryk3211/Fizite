package com.patryk3211.fizite.simulation.physics.simulation;

import com.patryk3211.fizite.simulation.physics.simulation.constraints.*;
import org.apache.commons.io.output.NullOutputStream;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class PhysicsTest {
    private static OutputStreamWriter fileWriter;
    private static final boolean fileOut = true;
    private static final boolean consoleOut = true;

    private static void dumpBody(RigidBody body) {
        final var state = body.getState();
        try {
            fileWriter.write("Body" + body.index() + "\t");
            fileWriter.write(state.position.x + "," + state.position.y + "," + state.positionA + "\t");
            fileWriter.write(state.velocity.x + "," + state.velocity.y + "," + state.velocityA + "\n");
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }

        if(consoleOut) {
            System.out.println("Body " + body.index() + ":");
            System.out.println("    position " + state.position + ", angle " + state.positionA);
            System.out.println("    velocity " + state.velocity + ", angular velocity " + state.velocityA);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        try {
            if(fileOut) {
                FileOutputStream stateFile = new FileOutputStream("run/output.phys");
                fileWriter = new OutputStreamWriter(stateFile);
            } else {
                fileWriter = new OutputStreamWriter(NullOutputStream.INSTANCE);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        final var world = new PhysicsWorld();
        RigidBody body1 = new RigidBody();
        RigidBody body2 = new RigidBody();
        RigidBody body3 = new RigidBody();
        world.addRigidBody(body1);
        world.addRigidBody(body2);
//        world.addRigidBody(body3);

        body1.setMass(1);
        body3.setMass(2);

        body1.getState().position.x = 0;
        body2.getState().position.x = 4;
        body3.getState().position.x = 7;

        body1.getState().velocityA = 2;

        world.addConstraint(new PositionConstraint(body1, 0, 0));
        world.addConstraint(new RotationConstraint(body1, body2));
//        world.addConstraint(new BearingConstraint(body1, body2, 2, 0, -2, 0));
//        world.addConstraint(new WeldConstraint(body2, body3, 2, 0, -1, 0));
//        world.addConstraint(new BearingConstraint(body2, body3, 2, 0, -1, 0));
//        world.addConstraint(new LockYConstraint(body3, 0));

        for(int i = 0; i < 400; ++i) {
//            if(i > 400) {
//                body1.getState().extForceA = 10;
//            }

            long simTime = -System.nanoTime();
            world.simulate();
            simTime += System.nanoTime();

            if(consoleOut)
                System.out.println("Time = " + (i / 20.0f) + "s");
            dumpBody(body1);
            dumpBody(body2);
//            dumpBody(body3);
            if(consoleOut) {
//                System.out.println("Solve times:");
//                System.out.println("    cSolve = " + (world.constraintSolveTime / 1000.0) + "us");
//                System.out.println("    pStep = " + (world.physicsStepTime / 1000.0) + "us");
//                System.out.println("    pSolve = " + (world.physicsSolveTime / 1000.0) + "us");
//                System.out.println("    total = " + ((world.constraintSolveTime + world.physicsStepTime + world.physicsSolveTime) / 1000.0) + "us");
//                System.out.println("    simTime = " + (simTime / 1000.0) + "us");
//
//                System.out.println("System energy = " + world.totalKineticEnergy());
            }

            try {
//                int vIndex = 0;
//                for(final var c : world.constraints()) {
//                    vIndex += c.dumpImpulses(fileWriter, vIndex);
//                }
                fileWriter.write("%" + i + "\n");
                fileWriter.flush();

//                Thread.sleep(1);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }
}
