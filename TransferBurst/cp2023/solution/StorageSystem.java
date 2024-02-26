package cp2023.solution;

import cp2023.base.ComponentTransfer;
import cp2023.base.DeviceId;
import cp2023.base.ComponentId;
import cp2023.exceptions.*;
import cp2023.solution.DirectedMultigraph.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.List;

public class StorageSystem implements cp2023.base.StorageSystem {
    Map<DeviceId, Integer> deviceTotalSlots;
    boolean cycle = false;
    int cycle_counter = 0;
    Map<ComponentId, DeviceId> componentPlacement;
    Semaphore protection;
    Set<ComponentId> submittedComponents;
    ConcurrentMap<DeviceId, ConcurrentLinkedDeque<Slot>> deviceSlots;
    DirectedMultigraph graph;
    ConcurrentMap<ComponentTransfer, Semaphore> mapToWaitingTransfers;
    ConcurrentMap<DeviceId, ConcurrentLinkedDeque<ComponentTransfer>> waitingToPrepare;
    ConcurrentMap<DeviceId, ConcurrentLinkedDeque<ComponentTransfer>> waitingToPerform;

    public StorageSystem(
            Map<DeviceId, Integer> deviceTotalSlots,
            Map<ComponentId, DeviceId> componentPlacement
    ){
        this.deviceTotalSlots = deviceTotalSlots;
        this.componentPlacement = componentPlacement;
        this.protection = new Semaphore(1);
        this.submittedComponents = new HashSet<>();
        this.graph = createGraph();
        this.waitingToPrepare = createWaitingLists();
        this.waitingToPerform = createWaitingLists();
        this.mapToWaitingTransfers = new ConcurrentHashMap<>();
        this.deviceSlots = createSlots();
    }
    // creates distinguishable slots for every deviceId
    private ConcurrentMap<DeviceId, ConcurrentLinkedDeque<Slot>> createSlots(){
        ConcurrentMap<DeviceId, ConcurrentLinkedDeque<Slot>> deviceSlots = new ConcurrentHashMap<>();
        // for every device, add new list
        for(Map.Entry<DeviceId, Integer> entry : deviceTotalSlots.entrySet()){
            DeviceId deviceId = entry.getKey();
            ConcurrentLinkedDeque<Slot> slots = new ConcurrentLinkedDeque<>();
            deviceSlots.put(deviceId, slots);
        }

        // for every component, we create proper Slot
        for(ComponentId c : componentPlacement.keySet()){
            Slot newSlot = new Slot();
            newSlot.setCurrComponent(c);
            deviceSlots.get(componentPlacement.get(c)).add(newSlot);
        }
        for(DeviceId d : deviceTotalSlots.keySet()){
            while(deviceTotalSlots.get(d) > deviceSlots.get(d).size()){
                Slot newSlot = new Slot();
                deviceSlots.get(d).add(newSlot);
            }
        }
        return deviceSlots;
    }
    private DirectedMultigraph createGraph(){
        DirectedMultigraph graph = new DirectedMultigraph();
        for(Map.Entry<DeviceId, Integer> entry : deviceTotalSlots.entrySet()){
            graph.addVertex(entry.getKey());
        }
        return graph;
    }
    private ConcurrentMap<DeviceId, ConcurrentLinkedDeque<ComponentTransfer>> createWaitingLists(){
        ConcurrentMap<DeviceId, ConcurrentLinkedDeque<ComponentTransfer>> map = new ConcurrentHashMap<>();
        for(Map.Entry<DeviceId, Integer> entry : deviceTotalSlots.entrySet()){
            map.put(entry.getKey(), new ConcurrentLinkedDeque<>());
        }
        return map;
    }
    private boolean isIllegalTransferType(ComponentTransfer transfer){
        if(isRelocation(transfer) || isRemoval(transfer)
                || isAddition(transfer)){
            return false;
        }
        return true;
    }
    // check if transfer type is removal
    private boolean isRemoval(ComponentTransfer transfer){
        if(transfer.getDestinationDeviceId() == null
                && transfer.getSourceDeviceId() != null){
            return true;
        }
        return false;
    }
    // check if transfer type is Addition
    private boolean isAddition(ComponentTransfer transfer){
        if(transfer.getSourceDeviceId() == null
                && transfer.getDestinationDeviceId() != null){
            return true;
        }
        return false;
    }
    // check if transfer type is Relocation
    private boolean isRelocation(ComponentTransfer transfer){
        if(transfer.getSourceDeviceId() != null
                && transfer.getDestinationDeviceId()!=null){
            return true;
        }
        return false;
    }
    // try  to find a cycle in transfers representations
    private List<Edge> isCycle(ComponentTransfer transfer){
        return graph.findCycle(transfer.getSourceDeviceId());
    }
    // check if device doesn't exist in a system
    private DeviceId doesDeviceDoesNotExist(ComponentTransfer transfer){
        if(transfer.getSourceDeviceId() != null
                && !deviceTotalSlots.containsKey(transfer.getSourceDeviceId())){
            return transfer.getSourceDeviceId();
        }
        if(transfer.getDestinationDeviceId() != null &&!deviceTotalSlots.containsKey(transfer.getDestinationDeviceId())){
            return transfer.getDestinationDeviceId();
        }
        return null;
    }
    // check if currently added component already exists in a system

    private boolean doesComponentAlreadyExists(ComponentTransfer transfer){
        if(componentPlacement.containsKey(transfer.getComponentId())
                && isAddition(transfer)){
            return true;
        }
        return false;
    }
    // check if removing or relocating component exists in a system
    private boolean doesComponentDoesNotExist(ComponentTransfer transfer){
        if(isRemoval(transfer) || isRelocation(transfer)){
            if(!componentPlacement.containsKey(transfer.getComponentId())){
                return true;
            }
            if(componentPlacement.containsKey(transfer.getComponentId())
                    && !(componentPlacement.get(transfer.getComponentId()).equals(transfer.getSourceDeviceId()))){
                return true;
            }
        }
        return false;
    }
    // check if component is already at transfer's destination place
    private boolean doesComponentDoesNotNeedTransfer(ComponentTransfer transfer){
        if(transfer.getDestinationDeviceId() != null) {
            if (componentPlacement.containsKey(transfer.getComponentId())) {
                if (componentPlacement.get(transfer.getComponentId()).equals( transfer.getDestinationDeviceId())) {
                    return true;
                }
            }
        }
        return false;
    }
    // check if component belongs to group of submitted components
    private boolean isComponentBeingOperated(ComponentTransfer transfer){
        if(submittedComponents.contains(transfer.getComponentId())){
            return true;
        }
        return false;
    }
    // check if transfer is legal
    private void checkForExceptions(ComponentTransfer transfer) throws TransferException{

        if(isIllegalTransferType(transfer)){
            throw new IllegalTransferType(transfer.getComponentId());
        }

        if (doesDeviceDoesNotExist(transfer) != null) {
            throw new DeviceDoesNotExist(doesDeviceDoesNotExist(transfer));
        }
        if(doesComponentAlreadyExists(transfer)){
            throw new ComponentAlreadyExists(transfer.getComponentId(), componentPlacement.get(transfer.getComponentId()));
        }
        if(doesComponentDoesNotExist(transfer)){
            throw new ComponentDoesNotExist(transfer.getComponentId(), transfer.getSourceDeviceId());
        }
        if(doesComponentDoesNotNeedTransfer(transfer)){
            throw new ComponentDoesNotNeedTransfer(transfer.getComponentId(), transfer.getDestinationDeviceId());
        }
        if (isComponentBeingOperated(transfer)) {
            throw new ComponentIsBeingOperatedOn(transfer.getComponentId());
        }
        submittedComponents.add(transfer.getComponentId());
    }
    // check if transfer perform is possible
    // try to perform it
    private void checkPerformPossibleTry(ComponentTransfer transfer){
        switch(tellTransferType(transfer)){
            case 1:
                // it's Addition
                checkAdditionPerformPossibleTry(transfer);
                break;
            case 2:
                // it's Removal
                checkRemovalPerformPossibleTry(transfer);
                break;
            case 3:
                // it's Relocation
                checkRelocationPerformPossibleTry(transfer);
        }
        try {
            protection.acquire();
            findNextToPrepare(transfer);
            findNextToPerform(transfer);
            protection.release();
        }catch(Exception e){
            throw new RuntimeException("panic: unexpected thread interruption");
        }
    }
    // check if Relocation is possible, and try to perform it
    private void checkRelocationPerformPossibleTry(ComponentTransfer transfer){
        for(Slot slot : deviceSlots.get(transfer.getSourceDeviceId())){
            if(slot.getWillBeRealised() == true &&
                    slot.getCurrComponent()!= null && slot.getCurrComponent().equals( transfer.getComponentId())){
                slot.setCurrComponent(null);
                slot.setWillBeRealised(false);
                componentPlacement.remove(transfer.getComponentId(), transfer.getSourceDeviceId());
                break;
            }
        }
        for(Slot slot : deviceSlots.get(transfer.getDestinationDeviceId())) {
            // free place, which is not reserved
            if (slot.getCurrComponent() == null && slot.getNextId()!= null && slot.getNextId().equals( transfer.getComponentId())) {
                slot.setNextId(null);
                slot.setCurrComponent(transfer.getComponentId());
                slot.setWillBeRealised(false);
                componentPlacement.put(transfer.getComponentId(), transfer.getDestinationDeviceId());
                protection.release();
                return;
            }
        }
        addToWaiting(transfer, waitingToPerform);
    }
    private void checkAdditionPerformPossibleTry(ComponentTransfer transfer){
        for(Slot slot : deviceSlots.get(transfer.getDestinationDeviceId())) {
            // free place, which is not reserved
            if (slot.getCurrComponent() == null && slot.getNextId() == transfer.getComponentId()) {
                slot.setNextId(null);
                slot.setCurrComponent(transfer.getComponentId());
                slot.setWillBeRealised(false);
                componentPlacement.put(transfer.getComponentId(), transfer.getDestinationDeviceId());
                protection.release();
                return;
            }
        }
        addToWaiting(transfer, waitingToPerform);
    }
    // check if component removal is possible and allow it
    // if possible
    private void checkRemovalPerformPossibleTry(ComponentTransfer transfer){
        for(Slot slot : deviceSlots.get(transfer.getSourceDeviceId())) {
            if (slot.getWillBeRealised() == true && slot.getCurrComponent() != null &&
                    slot.getCurrComponent().equals(transfer.getComponentId())) {
                slot.setWillBeRealised(false);
                slot.setCurrComponent(null);
                componentPlacement.remove(transfer.getComponentId());
                protection.release();
                return;
            }
        }
    }
    // check if transfer can prepare
    // try reserve slot for it
    private void checkPreparePosibleTryReserve(ComponentTransfer transfer){
        switch(tellTransferType(transfer)){
            case 1:
                // it's Addition
                checkAdditionPreparePossibleTryReserve(transfer);
                return;
            case 2:
                // it's Removal
                checkRemovalPreparePossibleTryReserve(transfer);
                return;
            case 3:
                // it's Relocation
                // add edge to transfers representation
                graph.addEdge(transfer.getSourceDeviceId(), transfer.getDestinationDeviceId(),
                        transfer.getComponentId(), transfer);
                checkRelocationPreparePossibleTryReserve(transfer);
        }
    }
    private void checkRelocationPreparePossibleTryReserve(ComponentTransfer transfer){
        List<Edge> edges = isCycle(transfer);
        // there is a cycle
        if(!edges.isEmpty()){
            cycle = true;
            cycle_counter = edges.size()-1;
            // reserve places for the next component
            ComponentId next = edges.get(edges.size() - 1).transfer.getComponentId();
            for(Edge e : edges) {
                graph.removeEdge(e);
                for (Slot s : deviceSlots.get(e.source)) {
                    if (s.getCurrComponent() != null && s.getCurrComponent().equals(e.transfer.getComponentId())) {
                        s.setWillBeRealised(true);
                        s.setNextId(next);
                        next = s.getCurrComponent();
                        break;
                    }
                }
            }
            for(int i = 1; i < edges.size(); i++){
                removeFromWaitingToPrepare(edges.get(i).transfer);
            }
            protection.release();
            return;
            // there is no cycle
        }else{
            // check if adding element is possible
            for(Slot slot : deviceSlots.get(transfer.getDestinationDeviceId())) {
                // free place, which is not reserved
                if (slot.getCurrComponent() == null && slot.getNextId() == null || (slot.getWillBeRealised() == true && slot.getNextId() == null)) {
                    slot.setNextId(transfer.getComponentId());
                    // find source slot, set is as realising
                    for(Slot s : deviceSlots.get(transfer.getSourceDeviceId())){
                        if(s.getCurrComponent() != null && s.getCurrComponent().equals(transfer.getComponentId())){
                            s.setWillBeRealised(true);
                        }
                    }
                    // remove edge from transfers representations
                    graph.removeEdge(transfer.getSourceDeviceId(), transfer.getDestinationDeviceId(),
                            transfer.getComponentId(), transfer);
                    protection.release();
                    return;
                }
            }
        }
        // sleep transfer if it has to wait with it preparation
        addToWaiting(transfer, waitingToPrepare);
        try{
            protection.acquire();
            graph.removeEdge(transfer.getSourceDeviceId(), transfer.getDestinationDeviceId(),
                    transfer.getComponentId(), transfer);
            // if it's cycle, don't try to wake up transfers
            if(!cycle){
                findNextToPrepare(transfer);
            }else{
                cycle_counter--;
                if(cycle_counter == 0){
                    cycle = false;
                }
            }
        }catch (Exception e){
            throw new RuntimeException("panic: unexpected thread interruption");
        }
        protection.release();
    }
    private void checkRemovalPreparePossibleTryReserve(ComponentTransfer transfer){
        for(Slot slot : deviceSlots.get(transfer.getSourceDeviceId())) {
            if (slot.getCurrComponent() != null && slot.getCurrComponent().equals(transfer.getComponentId())) {
                slot.setWillBeRealised(true);
                findNextToPrepare(transfer);
                protection.release();
                return;
            }
        }
    }
    private void checkAdditionPreparePossibleTryReserve(ComponentTransfer transfer){
        for(Slot slot : deviceSlots.get(transfer.getDestinationDeviceId())) {
            // free place, which is not reserved
            if (slot.getCurrComponent() == null && slot.getNextId() == null) {
                slot.setNextId(transfer.getComponentId());
                protection.release();
                return;
                // place is not free, but will be released and is not reserved
            }
        }
        for(Slot slot : deviceSlots.get(transfer.getDestinationDeviceId())) {
            if (slot.getWillBeRealised() == true && slot.getNextId() == null) {
                slot.setNextId(transfer.getComponentId());
                protection.release();
                return;
            }
        }
        addToWaiting(transfer, waitingToPrepare);
    }
    // return transfer type as:
    // 1 : Addition
    // 2 : Removal
    // 3 : Relocation
    private int tellTransferType(ComponentTransfer transfer){
        if(transfer.getSourceDeviceId() == null){
            // it's Addition
            return 1;
        }else if(transfer.getDestinationDeviceId() == null){
            // it's Removal
            return 2;
        }else{
            // it's Relocation
            return 3;
        }
    }
    // try to find next transfer, which is ready to prepare
    // function called by another transfer
    private void findNextToPrepare(ComponentTransfer transfer){
        DeviceId deviceToIterate;
        if(isRemoval(transfer) || isRelocation(transfer)){
            deviceToIterate = transfer.getSourceDeviceId();
        }else{
            return;
        }
        for(ComponentTransfer t : waitingToPrepare.get(deviceToIterate)){

            if(isAddition(t)){

                for(Slot s : deviceSlots.get(deviceToIterate)){
                    // it's slot which will be realised, and it's not reserved
                    if(s.getWillBeRealised() == true && s.getNextId() == null){
                        s.setNextId(t.getComponentId());
                        removeFromWaitingToPrepare(t);
                        return;
                    }
                }
                for(Slot s : deviceSlots.get(deviceToIterate)){
                    // it's free slot, reserve it
                    if(s.getNextId() == null && s.getCurrComponent() == null){
                        s.setNextId(t.getComponentId());
                        removeFromWaitingToPrepare(t);
                        return;
                    }
                }
            }else if(isRelocation(t)){

                for(Slot s : deviceSlots.get(deviceToIterate)){
                    // it's slot reserved for this relocation
                    if( s.getWillBeRealised() == true && s.getNextId() == null){
                        for(Slot s1 : deviceSlots.get(t.getSourceDeviceId())){
                            if(s1.getCurrComponent()!= null && s1.getCurrComponent().equals(t.getComponentId())){
                                s1.setWillBeRealised(true);
                                s.setNextId(t.getComponentId());
                                removeFromWaitingToPrepare(t);
                                return;
                            }
                        }

                    }
                }
                for(Slot s : deviceSlots.get(deviceToIterate)){
                    // it's free slot, reserve it
                    if(s.getNextId() == null && s.getCurrComponent() == null){
                        for(Slot s1 : deviceSlots.get(t.getSourceDeviceId())){
                            if(s1.getCurrComponent()!= null && s1.getCurrComponent().equals(t.getComponentId())){
                                s1.setWillBeRealised(true);
                                s.setNextId(t.getComponentId());
                                removeFromWaitingToPrepare(t);
                                return;
                            }
                        }
                    }
                }

            }
        }
    }
    // try to find next transfer, which is ready to perform
    // function called by another transfer
    private void findNextToPerform(ComponentTransfer transfer){
        DeviceId deviceToIterate;
        if(isRemoval(transfer) || isRelocation(transfer)){
            deviceToIterate = transfer.getSourceDeviceId();
        }else{
            return;
        }
        for(ComponentTransfer t : waitingToPerform.get(deviceToIterate)){

            if(isAddition(t)){

                for(Slot s : deviceSlots.get(deviceToIterate)){
                    // it's slot which will be realised, and it's not reserved
                    if(s.getCurrComponent() == null && s.getNextId()!= null && s.getNextId().equals( t.getComponentId())){
                        s.setNextId(null);
                        s.setWillBeRealised(false);
                        s.setCurrComponent(t.getComponentId());
                        componentPlacement.put(t.getComponentId(), t.getDestinationDeviceId());
                        removeFromWaitingToPerform(t);
                        return;
                    }
                }
            }else if(isRelocation(t)){

                for(Slot s : deviceSlots.get(deviceToIterate)){
                    // it's slot reserved for this relocation
                    if( s.getCurrComponent() == null && s.getNextId()!= null && s.getNextId().equals( t.getComponentId())){
                                s.setNextId(null);
                                s.setWillBeRealised(false);
                                s.setCurrComponent(t.getComponentId());
                                for(Slot s1 : deviceSlots.get(t.getSourceDeviceId())){
                                    if(s1.getCurrComponent() != null && s1.getCurrComponent().equals(t.getComponentId())){
                                        s1.setCurrComponent(null);
                                        s1.setWillBeRealised(false);
                                    }
                                }
                                componentPlacement.put(t.getComponentId(), t.getDestinationDeviceId());
                                removeFromWaitingToPerform(t);
                                return;
                    }
                }
            }
        }
    }
    // remove from list of transfers waiting
    // which are waiting to be prepared on device
    private void removeFromWaitingToPrepare(ComponentTransfer t){
        Semaphore toAwake = mapToWaitingTransfers.get(t);
        mapToWaitingTransfers.remove(t);
        waitingToPrepare.get(t.getDestinationDeviceId()).remove(t);
        toAwake.release();
    }
    // remove from list of transfers waiting
    // which are waiting to be performed on device
    private void removeFromWaitingToPerform(ComponentTransfer t){
        Semaphore toAwake = mapToWaitingTransfers.get(t);
        mapToWaitingTransfers.remove(t);
        waitingToPerform.get(t.getDestinationDeviceId()).remove(t);
        toAwake.release();
    }
    // add transfer to waiting for prepare/perform list on device
    private void addToWaiting(ComponentTransfer transfer, ConcurrentMap<DeviceId, ConcurrentLinkedDeque<ComponentTransfer>> waitingList){
        Semaphore newSemaphore = new Semaphore(0);
        waitingList.get(transfer.getDestinationDeviceId()).add(transfer);
        mapToWaitingTransfers.put(transfer, newSemaphore);
        protection.release();
        try {
            newSemaphore.acquire();
        }catch(InterruptedException e){
            throw new RuntimeException("panic: unexpected thread interruption");
        }
    }
    public void execute(ComponentTransfer transfer) throws TransferException {

        try{
            protection.acquire();
            checkForExceptions(transfer);
            checkPreparePosibleTryReserve(transfer);
            transfer.prepare();
            protection.acquire();
            checkPerformPossibleTry(transfer);
            transfer.perform();
            submittedComponents.remove(transfer.getComponentId());
            protection.acquire();
            protection.release();
        }catch(TransferException e){
            throw e;
        }catch(InterruptedException e){
            throw new RuntimeException("panic: unexpected thread interruption");
        }

    }
}