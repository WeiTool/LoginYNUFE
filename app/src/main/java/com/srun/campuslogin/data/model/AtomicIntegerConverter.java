package com.srun.campuslogin.data.model;

import androidx.room.TypeConverter;
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIntegerConverter {
    @TypeConverter
    public static int fromAtomicInteger(AtomicInteger value) {
        return value != null ? value.get() : 0;
    }

    @TypeConverter
    public static AtomicInteger toAtomicInteger(int value) {
        return new AtomicInteger(value);
    }
}