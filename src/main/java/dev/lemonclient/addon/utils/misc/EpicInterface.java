package dev.lemonclient.addon.utils.misc;

@FunctionalInterface
public interface EpicInterface<T, E> {
    E get(T t);
}
