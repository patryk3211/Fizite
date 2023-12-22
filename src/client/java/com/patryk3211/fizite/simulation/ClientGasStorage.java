package com.patryk3211.fizite.simulation;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.simulation.gas.GasCapability;
import com.patryk3211.fizite.simulation.gas.GasCell;
import com.patryk3211.fizite.simulation.gas.GasStorage;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3d;

import java.util.*;

public class ClientGasStorage extends GasStorage {
    private final Map<Long, GasCell> gasCellSync;// = new HashMap<>();
    private final Queue<GasCell> newCells;

    private static ClientGasStorage gas;

    public ClientGasStorage() {
        gasCellSync = new HashMap<>();
        newCells = new LinkedList<>();
        GasStorage.clientStorage = this;
        gas = this;
    }

    public static ClientGasStorage get() {
        return gas;
    }

    @Override
    protected void sidedAdd(BlockPos pos, GasCapability capability) {
        newCells.addAll(capability.cells());
        ClientNetworking.addToGasSync(pos);
    }

    @Override
    protected void sidedRemove(BlockPos pos, GasCapability capability) {
        for (GasCell cell : capability.cells()) {
            gasCellSync.remove(cell.getSyncId());
            ClientNetworking.removeFromGasSync(cell.getSyncId());
        }
    }

    public void setState(long id, double Ek, double n, Vector3d momentum) {
        var processCount = newCells.size();
        while (processCount-- > 0) {
            final var cell = newCells.remove();
            if(cell.getSyncId() == 0)
                newCells.add(cell);
            else
                gasCellSync.put(cell.getSyncId(), cell);
        }
        final var cell = gasCellSync.get(id);
        if(cell != null)
            cell.set(Ek, n, momentum);
    }

    public static void onDisconnect() {
        Fizite.LOGGER.info("Clearing client gas simulation");
        gas.registeredCapabilities.clear();
        gas.collectedBoundaries.clear();
        gas.gasCellSync.clear();
        gas.newCells.clear();
    }
}
