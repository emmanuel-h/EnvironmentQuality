package fr.mandarine.environmentquality.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fr.mandarine.environmentquality.data.remote.CachedResponse
import fr.mandarine.environmentquality.domain.model.AlertSettings
import fr.mandarine.environmentquality.domain.model.Commune
import fr.mandarine.environmentquality.domain.model.Conformity
import fr.mandarine.environmentquality.domain.model.ConformityFlags
import fr.mandarine.environmentquality.domain.model.GeoPoint
import fr.mandarine.environmentquality.domain.model.WaterStatus
import fr.mandarine.environmentquality.domain.repository.LatestStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomRepositoriesTest {

    private lateinit var db: AppDatabase
    private val vernon = Commune("27681", "Vernon", listOf("27200"), "27", "Eure", "Normandie", 25290, GeoPoint(49.0921, 1.4827))
    private val conform = ConformityFlags(Conformity.CONFORM, Conformity.CONFORM, Conformity.CONFORM, Conformity.CONFORM)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `should round trip a commune and its last sample when following it`() = runTest {
        val repository = RoomFollowRepository(db.followedCommunes())

        repository.follow(vernon, LatestStatus("S1", Instant.parse("2026-07-29T11:30:00Z"), conform))

        val followed = repository.followed().first().single()
        assertEquals(vernon, followed.commune)
        assertEquals("S1", followed.lastSampleCode)
        assertEquals(WaterStatus.CONFORM, followed.lastStatus)
        assertTrue(repository.isFollowed("27681").first())
    }

    @Test
    fun `should flag an update until the commune is seen when a check records a new sample`() = runTest {
        val repository = RoomFollowRepository(db.followedCommunes())
        repository.follow(vernon, null)

        repository.recordCheck("27681", LatestStatus("S2", Instant.EPOCH, conform), hasUpdate = true)
        assertTrue(repository.followedNow().single().hasUpdate)

        repository.markSeen("27681")
        assertFalse(repository.followedNow().single().hasUpdate)
    }

    @Test
    fun `should forget the commune when unfollowing it`() = runTest {
        val repository = RoomFollowRepository(db.followedCommunes())
        repository.follow(vernon, null)

        repository.unfollow("27681")

        assertFalse(repository.isFollowed("27681").first())
    }

    @Test
    fun `should list recent communes newest first when remembering them`() = runTest {
        val repository = RoomRecentRepository(db.recentCommunes(), java.time.Clock.fixed(Instant.EPOCH, java.time.ZoneOffset.UTC))
        repository.remember(vernon)
        val paris = Commune("75056", "Paris")
        RoomRecentRepository(db.recentCommunes(), java.time.Clock.fixed(Instant.ofEpochSecond(10), java.time.ZoneOffset.UTC)).remember(paris)

        assertEquals(listOf("75056", "27681"), repository.recent().first().map { it.code })
    }

    @Test
    fun `should default the alert settings when none were saved`() = runTest {
        val repository = RoomSettingsRepository(db.alertSettings())

        assertEquals(AlertSettings(), repository.alertSettingsNow())
        repository.setAlertSettings(AlertSettings(nonConform = true, newSample = false, referenceExceeded = true))
        assertEquals(AlertSettings(true, false, true), repository.alertSettings().first())
    }

    @Test
    fun `should store and read back a response when caching it`() = runTest {
        val cache = RoomResponseCache(db.cachedResponses())

        assertNull(cache.read("u"))
        cache.write("u", CachedResponse("body", Instant.ofEpochMilli(42)))
        assertEquals(CachedResponse("body", Instant.ofEpochMilli(42)), cache.read("u"))
    }
}
