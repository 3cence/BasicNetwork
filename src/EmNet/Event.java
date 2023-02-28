package EmNet;

public interface Event<E> {
    /**
     * define this method for calling when event is triggered
     */
    void trigger(E e);
}
