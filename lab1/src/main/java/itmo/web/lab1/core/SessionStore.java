package itmo.web.lab1.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SessionStore {
    private final Map<String, Deque<Result>> sessions = new ConcurrentHashMap<>();
    private final int cap;

    public SessionStore(int cap) { this.cap = cap; }

    public void add(String sid, Result r) {
        Deque<Result> deque = sessions.computeIfAbsent(sid, k -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addFirst(r);
            while (deque.size() > cap) deque.removeLast();
        }
    }

    public List<Result> snapshot(String sid) {
        Deque<Result> deque = sessions.get(sid);
        if (deque == null) return List.of();
        synchronized (deque) { return List.copyOf(deque); }
    }
}
