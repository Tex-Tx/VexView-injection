package cn.pig.compat.fml.common.eventhandler;

   
                                    
   
public abstract class Event {
    public enum Result {
        DENY, DEFAULT, ALLOW
    }

    @Deprecated
    public enum Result2 {
        DENY, DEFAULT, ALLOW
    }

    private boolean cancelable;
    private boolean canceled;
    private Result result;

    public Event() {
    }

    protected Event(boolean cancelable) {
        this.cancelable = cancelable;
    }

    protected Event(boolean cancelable, boolean hasResult) {
        this.cancelable = cancelable;
        this.result = hasResult ? Result.DEFAULT : null;
    }

    public boolean isCancelable() {
                                                           
                                                                        
        if (this.cancelable) {
            return true;
        }
        for (Class<?> c = this.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            if (c.isAnnotationPresent(Cancelable.class)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCanceled() {
        return this.canceled;
    }

    public void setCanceled(boolean cancel) {
        if (!this.isCancelable()) {
            throw new UnsupportedOperationException("Attempted to cancel a non-cancelable event");
        }
        this.canceled = cancel;
    }

    public boolean hasResult() {
        return this.result != null;
    }

    public Result getResult() {
        return this.result == null ? Result.DEFAULT : this.result;
    }

    public void setResult(Result result) {
        if (this.result == null) {
            throw new UnsupportedOperationException("Attempted to set result on a non-result event");
        }
        this.result = result;
    }
}
