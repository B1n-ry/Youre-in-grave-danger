package com.b1n_ry.yigd.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.EnumMap;

public class TimePoint {
    private final long time;
    private final long timeOfDay;
    private final LocalDateTime irlTime;

    private static final EnumMap<Month, String> MONTH_NAMES;

    public TimePoint(ServerWorld world) {
        this(world.getTime(), world.getTimeOfDay(), LocalDateTime.now());
    }
    public TimePoint(long time, long timeOfDay, LocalDateTime irlTime) {
        this.time = time;
        this.timeOfDay = timeOfDay;
        this.irlTime = irlTime;
    }

    public long getTime() {
        return this.time;
    }
    public long getDay() {
        return this.timeOfDay / 24000;
    }
    public String getIrlTime() {
        String month = MONTH_NAMES.get(this.irlTime.getMonth());
        int hour = this.irlTime.getHour();
        String timePostfix = "AM";
        if (hour >= 12) {
            hour -= 12;
            timePostfix = "PM";
        }
        return "%s %d %d, %d:%d %s".formatted(month, this.irlTime.getDayOfMonth(), this.irlTime.getYear(), hour, this.irlTime.getMinute(), timePostfix);
    }
    public String getMonthName() {
        return MONTH_NAMES.get(this.irlTime.getMonth());
    }
    public int getDate() {
        return this.irlTime.getDayOfMonth();
    }
    public int getYear() {
        return this.irlTime.getYear();
    }
    public int getHour(boolean timePostfix) {
        int hour = this.irlTime.getHour();
        return timePostfix ? (11 + hour) % 12 + 1 : hour;
    }
    public int getMinute() {
        return this.irlTime.getMinute();
    }
    public String getTimePostfix(boolean actuallyUseIt) {
        if (actuallyUseIt)
            return this.irlTime.getHour() >= 12 ? " PM" : " AM";

        return "";
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putLong("time", this.time);
        nbt.putLong("timeOfDay", this.timeOfDay);

        NbtCompound irlTimeNbt = new NbtCompound();
        irlTimeNbt.putInt("year", this.irlTime.getYear());
        irlTimeNbt.putInt("month", this.irlTime.getMonthValue());
        irlTimeNbt.putInt("date", this.irlTime.getDayOfMonth());

        irlTimeNbt.putInt("hour", this.irlTime.getHour());
        irlTimeNbt.putInt("minute", this.irlTime.getMinute());

        nbt.put("realTime", irlTimeNbt);
        return nbt;
    }

    public static TimePoint fromNbt(NbtCompound nbt) {
        long time = nbt.getLong("time");
        long timeOfDay = nbt.getLong("timeOfDay");

        NbtCompound irlTimeNbt = nbt.getCompound("realTime");
        int year = irlTimeNbt.getInt("year");
        int month = irlTimeNbt.getInt("month");
        int date = irlTimeNbt.getInt("date");
        int hour = irlTimeNbt.getInt("hour");
        int minute = irlTimeNbt.getInt("minute");

        LocalDateTime dateTime = LocalDateTime.of(year, month, date, hour, minute);
        return new TimePoint(time, timeOfDay, dateTime);
    }

    static {
        MONTH_NAMES = new EnumMap<>(Month.class) {{
            put(Month.JANUARY, "January");
            put(Month.FEBRUARY, "February");
            put(Month.MARCH, "March");
            put(Month.APRIL, "April");
            put(Month.MAY, "May");
            put(Month.JUNE, "June");
            put(Month.JULY, "July");
            put(Month.AUGUST, "August");
            put(Month.SEPTEMBER, "September");
            put(Month.OCTOBER, "October");
            put(Month.NOVEMBER, "November");
            put(Month.DECEMBER, "December");
        }};
    }
}
