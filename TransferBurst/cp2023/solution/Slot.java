package cp2023.solution;

import cp2023.base.ComponentId;

public class Slot {
    private ComponentId nextId;
    private boolean willBeRealised;
    private ComponentId currComponent;

    public void setCurrComponent(ComponentId currComponent) {
        this.currComponent = currComponent;
    }
    public void setNextId(ComponentId nextId){
        this.nextId = nextId;
    }
    public void setWillBeRealised(boolean willBeRealised){
        this.willBeRealised = willBeRealised;
    }
    public ComponentId getCurrComponent(){
        return this.currComponent;
    }
    public ComponentId getNextId(){
        return this.nextId;
    }
    public boolean getWillBeRealised(){
        return this.willBeRealised;
    }

    public Slot(){
        this.nextId = null;
        this.willBeRealised = false;
        this.currComponent = null;
    }
}
