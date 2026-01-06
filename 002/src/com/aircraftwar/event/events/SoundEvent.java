package com.aircraftwar.event.events;

public class SoundEvent {
    private final String soundId;
    private final float volume;

    public SoundEvent(String soundId, float volume) {
        this.soundId = soundId;
        this.volume = volume;
    }

    public String getSoundId() { return soundId; }
    public float getVolume() { return volume; }
}

