/*
 * University of Warsaw
 * Concurrent Programming Course 2023/2024
 * Java Assignment
 *
 * Author: Konrad Iwanicki (iwanicki@mimuw.edu.pl)
 */
package cp2023.solution;

import java.util.HashMap;
import java.util.Map;

import cp2023.base.ComponentId;
import cp2023.base.DeviceId;
import cp2023.base.StorageSystem;

public final class StorageSystemFactory {

    public static StorageSystem newSystem(
            Map<DeviceId, Integer> deviceTotalSlots,
            Map<ComponentId, DeviceId> componentPlacement) {
        if(zeroCap(deviceTotalSlots)){
            throw new IllegalArgumentException("One of the deviceTotalSlots has unauthorized capacity");
        }
        if(assignToUnknownDevice(deviceTotalSlots, componentPlacement)){
            throw new IllegalArgumentException("One of the components is assigned to not existing device");
        }
        if(capExceeded(deviceTotalSlots, componentPlacement)){
        throw new IllegalArgumentException("Too much components assigned to the one of the divices");
        }
        return new cp2023.solution.StorageSystem(deviceTotalSlots, componentPlacement);
    }
    // check if there is a device with capacity <= 0
    public static boolean zeroCap(Map<DeviceId, Integer> map){
        for(Integer val : map.values()){
            if(val <= 0){
                return true;
            }
        }
        return false;
    }
    // check if there is a component assigned to not known device
    public static boolean assignToUnknownDevice(Map<DeviceId, Integer> devices, Map<ComponentId, DeviceId> components){
        for(DeviceId d : components.values()){
            if(devices.get(d) == null){
                return true;
            }
        }
        return false;
    }
    // check if any device capacity is exceeded
    public static boolean capExceeded(Map<DeviceId, Integer> devices, Map<ComponentId, DeviceId> components){
        Map<DeviceId, Integer> usedCap = new HashMap<>();

        for (DeviceId d : components.values()) {
            usedCap.put(d, usedCap.getOrDefault(d, 0) + 1);
        }

        for (DeviceId d : usedCap.keySet()) {
            if (usedCap.get(d) > devices.getOrDefault(d, 0)) {
                return true;
            }
        }

        return false;
    }


}
