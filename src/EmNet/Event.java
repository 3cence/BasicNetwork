package EmNet;

public interface Event<E> {
    void trigger(E e);
}
