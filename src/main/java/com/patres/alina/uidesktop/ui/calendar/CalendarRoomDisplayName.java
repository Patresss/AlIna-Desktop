package com.patres.alina.uidesktop.ui.calendar;

import java.util.regex.Pattern;

/** Produces a compact visible room label while callers retain the full Calendar resource name. */
public final class CalendarRoomDisplayName {

    private static final Pattern TRAILING_EQUIPMENT = Pattern.compile("\\s*\\[[^]]*]\\s*$");
    private static final Pattern ROOM_CODE = Pattern.compile("(?<!\\d)(\\d{1,3}-\\d{1,3}-.+)$");

    private CalendarRoomDisplayName() {
    }

    public static String shorten(final String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        final String normalized = fullName.strip();
        final String withoutEquipment = TRAILING_EQUIPMENT.matcher(normalized).replaceFirst("").strip();
        final int citySeparator = withoutEquipment.indexOf(',');
        if (citySeparator < 0) {
            return withoutEquipment;
        }

        final String city = withoutEquipment.substring(0, citySeparator).strip();
        final String details = withoutEquipment.substring(citySeparator + 1).strip();
        final int roomSeparator = details.lastIndexOf(':');
        if (roomSeparator >= 0) {
            return combine(city, details.substring(roomSeparator + 1));
        }

        final var roomCode = ROOM_CODE.matcher(details);
        if (roomCode.find()) {
            return combine(city, roomCode.group(1));
        }
        return withoutEquipment;
    }

    private static String combine(final String city, final String room) {
        final String normalizedRoom = room.strip();
        if (city.isBlank() || normalizedRoom.isBlank()) {
            return normalizedRoom.isBlank() ? city : normalizedRoom;
        }
        return city + ", " + normalizedRoom;
    }
}
