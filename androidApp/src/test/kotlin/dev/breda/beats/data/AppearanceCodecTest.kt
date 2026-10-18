package dev.breda.beats.data

import org.junit.Assert.*
import org.junit.Test

class AppearanceCodecTest {
    @Test fun colorsRoundTripSeparatelyFromMusicProjects() {
        val appearance=StudioAppearance("102b40","6ef5d0")
        assertEquals(StudioAppearance("102B40","6EF5D0"),AppearanceCodec.decode(AppearanceCodec.encode(appearance)))
    }
    @Test fun invalidColorsAndFutureVersionsAreRejected() {
        for(text in listOf("{\"schema\":2,\"background\":\"101115\",\"accent\":\"F6B44E\"}","{\"schema\":1,\"background\":\"broken\",\"accent\":\"F6B44E\"}")) {
            assertTrue(runCatching{AppearanceCodec.decode(text)}.isFailure)
        }
        assertFalse(StudioAppearance.validHex("12345"));assertFalse(StudioAppearance.validHex("#123456"))
    }
}
