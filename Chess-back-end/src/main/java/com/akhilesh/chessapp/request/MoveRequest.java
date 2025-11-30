package com.akhilesh.chessapp.request;

import com.akhilesh.chessapp.model.Pair;

public class MoveRequest {
    private Pair from;
    private Pair to;

    // Default constructor is required for JSON deserialization
    public MoveRequest() {}

    public MoveRequest(Pair from, Pair to) {
        this.from = from;
        this.to = to;
    }

    public Pair getFrom() {
        return from;
    }

    public void setFrom(Pair from) {
        this.from = from;
    }

    public Pair getTo() {
        return to;
    }

    public void setTo(Pair to) {
        this.to = to;
    }
}