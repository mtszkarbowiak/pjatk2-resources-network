public class InternalCommunication {
    private AppConfig config;
    private boolean isRegistered;

    public InternalCommunication(AppConfig config) {
        this.config = config;
    }

    public synchronized void markRegistered(){
        isRegistered = true;
    }

    public synchronized boolean isRegistered(){
        return isRegistered;
    }

    public synchronized boolean isIdle(){
        return isRegistered; // && allocBuilder = ready
    }
}