package com.bugsnag.android

import com.bugsnag.android.EventStore.EVENT_COMPARATOR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.io.File

@RunWith(MockitoJUnitRunner::class)
class EventFilenameTest {

    @Mock
    lateinit var event: Event

    @Mock
    lateinit var error: Error

    @Mock
    lateinit var frame: Stackframe

    @Mock
    lateinit var file: File

    @Mock
    lateinit var app: AppWithState

    private val config = BugsnagTestUtils.generateImmutableConfig()

    /**
     * Generates a client and ensures that its eventStore has 0 files persisted
     *
     * @throws Exception if initialisation failed
     */
    @Before
    fun setUp() {
        `when`(event.errors).thenReturn(listOf(error))
        `when`(error.stacktrace).thenReturn(listOf(frame))
        `when`(frame.type).thenReturn(ErrorType.ANDROID)
        `when`(event.app).thenReturn(app)
        `when`(event.apiKey).thenReturn("0000111122223333aaaabbbbcccc9999")
        `when`(app.duration).thenReturn(null)
    }

    @Test
    fun testIsLaunchCrashReport() {
        val valid =
            arrayOf("1504255147933_30b7e350-dcd1-4032-969e-98d30be62bbc_startupcrash.json")
        val invalid = arrayOf(
            "",
            ".json",
            "abcdeAO.json",
            "!@£)(%)(",
            "1504255147933.txt",
            "1504255147933.json"
        )

        for (s in valid) {
            val eventInfo = EventFilenameInfo.fromFile(File(s), config)
            assertTrue(eventInfo.isLaunchCrashReport())
        }
        for (s in invalid) {
            val eventInfo = EventFilenameInfo.fromFile(File(s), config)
            assertFalse(eventInfo.isLaunchCrashReport())
        }
    }

    @Test
    fun testComparator() {
        val first = "1504255147933_683c6b92-b325-4987-80ad-77086509ca1e.json"
        val second = "1505000000000_683c6b92-b325-4987-80ad-77086509ca1e.json"
        val startup = "1504500000000_683c6b92-b325-4987-80ad-77086509ca1e_startupcrash.json"

        // handle defaults
        assertEquals(0, EVENT_COMPARATOR.compare(null, null).toLong())
        assertEquals(-1, EVENT_COMPARATOR.compare(File(""), null).toLong())
        assertEquals(1, EVENT_COMPARATOR.compare(null, File("")).toLong())

        // same value should always be 0
        assertEquals(0, EVENT_COMPARATOR.compare(File(first), File(first)).toLong())
        assertEquals(0, EVENT_COMPARATOR.compare(File(startup), File(startup)).toLong())

        // first is before second
        assertTrue(EVENT_COMPARATOR.compare(File(first), File(second)) < 0)
        assertTrue(EVENT_COMPARATOR.compare(File(second), File(first)) > 0)

        // startup is handled correctly
        assertTrue(EVENT_COMPARATOR.compare(File(first), File(startup)) < 0)
        assertTrue(EVENT_COMPARATOR.compare(File(second), File(startup)) > 0)
    }

    @Test
    fun regularJvmEventName() {
        val filename = EventFilenameInfo.fromEvent(
            event,
            "my-uuid-123",
            null,
            1504255147933,
            config
        ).encode()
        assertEquals(
            "1504255147933_0000111122223333aaaabbbbcccc9999_android_my-uuid-123_.json",
            filename
        )
    }

    /**
     * Simulates a crash 1s after launch which is considered a startup crash
     */
    @Test
    fun startupCrashJvmEventName() {
        `when`(app.duration).thenReturn(1000)

        val filename = EventFilenameInfo.fromEvent(
            event,
            "my-uuid-123",
            null,
            1504255147933,
            config
        ).encode()
        assertEquals(
            "1504255147933_0000111122223333aaaabbbbcccc9999_" +
                    "android_my-uuid-123_startupcrash.json", filename
        )
    }

    /**
     * Simulates a crash 10s after launch which is not considered a startup crash
     */
    @Test
    fun nonStartupCrashCrashJvmEventName() {
        `when`(app.duration).thenReturn(10000)
        val filename = EventFilenameInfo.fromEvent(
            event,
            "my-uuid-123",
            null,
            1504255147933,
            config
        ).encode()

        assertEquals(
            "1504255147933_0000111122223333aaaabbbbcccc9999_android_my-uuid-123_.json",
            filename
        )
    }

    @Test
    fun ndkEventName() {
        val filename = EventFilenameInfo.fromEvent(
            "{}",
            "my-uuid-123",
            "0000111122223333aaaabbbbcccc9999",
            1504255147933,
            config
        ).encode()
        assertEquals(
            "1504255147933_0000111122223333aaaabbbbcccc9999_c_my-uuid-123_not-jvm.json",
            filename
        )
    }

    @Test
    fun ndkEventNameNoApiKey() {
        val filename = EventFilenameInfo.fromEvent(
            "{}",
            "my-uuid-123",
            "",
            1504255147933,
            config
        ).encode()
        assertEquals(
            "1504255147933_5d1ec5bd39a74caa1267142706a7fb21_c_my-uuid-123_not-jvm.json",
            filename
        )
    }

    @Test
    fun apiKeyFromEmptyFilename() {
        `when`(file.name).thenReturn("")
        val eventInfo = EventFilenameInfo.fromFile(file, config)
        assertEquals(config.apiKey, eventInfo.apiKey)
        assertEquals("", eventInfo.uuid)
        assertEquals("", eventInfo.suffix)
        assertEquals(-1, eventInfo.timestamp)
        assertEquals(emptySet<ErrorType>(), eventInfo.errorTypes)
    }

    /**
     * Should default to config value as no api key is present
     */
    @Test
    fun apiKeyFromLegacyFilename() {
        `when`(file.name).thenReturn("1504500000000_683c6b92-b325-4987-80ad-77086509ca1e_startupcrash.json")
        val eventInfo = EventFilenameInfo.fromFile(file, config)
        assertEquals(config.apiKey, eventInfo.apiKey)
        assertEquals("startupcrash", eventInfo.suffix)
    }

    @Test
    fun apiKeyFromNewFilename() {
        `when`(file.name).thenReturn(
            "1504255147933_ffff111122948633aaaabbbbcccc9999" +
                    "_683c6b92-b325-4987-80ad-77086509ca1e.json"
        )
        val eventInfo = EventFilenameInfo.fromFile(file, config)
        assertEquals("ffff111122948633aaaabbbbcccc9999", eventInfo.apiKey)
    }

    @Test
    fun apiKeyFromLegacyNdkFilename() {
        `when`(file.name).thenReturn("1603191800142_7e1041e0-7f37-4cfb-9d29-0aa6930bbb72not-jvm.json")
        val eventInfo = EventFilenameInfo.fromFile(file, config)
        assertEquals(config.apiKey, eventInfo.apiKey)
    }

    @Test
    fun apiKeyFromNdkFilename() {
        `when`(file.name).thenReturn(
            "1603191800142_5d1ec8bd39a74caa1267142706a7fb20_" +
                    "7e1041e0-7f37-4cfb-9d29-0aa6930bbb72not-jvm.json"
        )
        val eventInfo = EventFilenameInfo.fromFile(file, config)
        assertEquals("5d1ec8bd39a74caa1267142706a7fb20", eventInfo.apiKey)
    }
}
