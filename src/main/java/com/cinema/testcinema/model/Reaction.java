package com.cinema.testcinema.model;

public enum Reaction {
    HEART(1), UP(2), DOWN(3), FIRE(4), LAUGH(5), CRY(6);
    public final short code;
    Reaction(int c){ this.code = (short) c; }
    public static Reaction from(short c){
        for (var r: values()) if (r.code == c) return r;
        throw new IllegalArgumentException("bad reaction: " + c);
    }
}
