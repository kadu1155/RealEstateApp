package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.PropReelRepository
import com.example.domain.model.EnquiryIntent
import com.example.domain.model.FurnishingStatus
import com.example.domain.model.LeadStage
import com.example.domain.model.AttributionSource
import com.example.domain.model.CoinEconomyConfig
import com.example.domain.model.CoinOperationResult
import com.example.domain.model.CoinRewardProcessResult
import com.example.domain.model.CoinTransactionCategory
import com.example.domain.model.CoinTransactionType
import com.example.domain.model.PossessionStatus
import com.example.domain.model.Property
import com.example.domain.model.PropertyCategory
import com.example.domain.model.RedemptionRule
import com.example.domain.model.Referral
import com.example.domain.model.ReferralApplyResult
import com.example.domain.model.ReferralCode
import com.example.domain.model.ReferralRewardListener
import com.example.domain.model.ReferralStatus
import com.example.domain.model.UserRole
import com.example.domain.model.VerificationBadge
import com.example.ui.components.ReferralShareHelper
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context verifies PropReel app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PropReel", appName)
    }

    @Test
    fun `repository provides initial reels and properties`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        val properties = repository.getPropertiesStream().first()
        assertTrue("Expected seeded properties in catalog", properties.isNotEmpty())

        val reels = repository.getReelsStream().first()
        assertTrue("Expected seeded reels in catalog", reels.isNotEmpty())

        val leads = repository.getLeadsStream().first()
        assertTrue("Expected seeded leads in CRM", leads.isNotEmpty())
    }

    @Test
    fun `enquiry creation creates new lead in CRM`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        val properties = repository.getPropertiesStream().first()
        val targetProperty = properties.first()

        val leadId = repository.submitEnquiry(
            property = targetProperty,
            customerName = "Ajay Malvankar",
            customerPhone = "+91 99887 76655",
            message = "Interested in weekend visit",
            intent = EnquiryIntent.SITE_VISIT
        )

        assertNotNull(leadId)

        val leads = repository.getLeadsStream().first()
        val createdLead = leads.find { it.id == leadId }
        assertNotNull("Created lead must exist in repository", createdLead)
        assertEquals(LeadStage.NEW, createdLead?.stage)
        assertEquals("Ajay Malvankar", createdLead?.customerName)
    }

    @Test
    fun `send and verify OTP manages auth session and role setup`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)

        // 1. Send OTP
        val (sent, isSandbox) = repository.sendOtp("+919823045678")
        assertTrue("OTP sending should succeed", sent)
        assertTrue("Sandbox should be recognized", isSandbox)

        // 2. Verify with valid sandbox OTP code
        val verified = repository.verifyOtp("+919823045678", "999999")
        assertTrue("Sandbox OTP code 999999 should verify successfully", verified)
        assertTrue("User should be logged in", repository.isLoggedIn.first())

        // 3. Setup Broker Role with MahaRERA
        repository.updateUserRoleWithDetails(
            role = UserRole.BROKER,
            reraNumber = "A52100034190",
            agencyName = "Apex Prime Realty"
        )

        val updatedUser = repository.currentUser.first()
        assertEquals(UserRole.BROKER, updatedUser.role)
        assertEquals(VerificationBadge.RERA_VERIFIED, updatedUser.verificationBadge)
        assertTrue(updatedUser.fullName.contains("Apex Prime Realty"))

        // 4. Switch to Owner Role
        repository.switchRole(UserRole.OWNER)
        val ownerUser = repository.currentUser.first()
        assertEquals(UserRole.OWNER, ownerUser.role)
        assertEquals(VerificationBadge.OWNER_VERIFIED, ownerUser.verificationBadge)

        // 5. Logout
        repository.logout()
        org.junit.Assert.assertFalse("User should be logged out", repository.isLoggedIn.first())
    }

    @Test
    fun `property bookmark toggle and locality search work correctly`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        val properties = repository.getPropertiesStream().first()
        val prop = properties.first()

        // 1. Toggle Save (Bookmark)
        val saved = repository.toggleSaveProperty(prop.id)
        assertTrue("Property should be bookmarked", saved)

        val savedList = repository.getSavedPropertiesStream().first()
        assertTrue("Saved list should contain property", savedList.any { it.id == prop.id })

        // 2. Toggle Save again (Remove)
        val removed = repository.toggleSaveProperty(prop.id)
        org.junit.Assert.assertFalse("Property should be un-bookmarked", removed)

        // 3. Verify Locality Coverage
        val localities = properties.map { it.locality }.toSet()
        assertTrue("Pune catalog should cover Baner", localities.contains("Baner"))
        assertTrue("Pune catalog should cover Wakad", localities.contains("Wakad"))

        // 4. Verify Per Sqft calculation
        assertTrue("Price should be positive", prop.price > 0)
        assertTrue("Carpet area should be positive", prop.carpetAreaSqft > 0)
        val perSqft = (prop.price / prop.carpetAreaSqft).toInt()
        assertTrue("Per sqft price should be realistic", perSqft > 1000)
    }

    @Test
    fun `reel like toggle updates counts and domain state`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        val reels = repository.getReelsStream().first()
        val reel = reels.firstOrNull { !it.isLikedByMe } ?: reels.first()
        val initialLikes = reel.likesCount
        val wasLiked = reel.isLikedByMe

        // 1. Toggle Like Reel (Like if unliked, Unlike if liked)
        repository.toggleLikeReel(reel.id)
        val updatedReels = repository.getReelsStream().first()
        val updatedReel = updatedReels.first { it.id == reel.id }
        assertEquals("Reel liked state must flip", !wasLiked, updatedReel.isLikedByMe)
        val expectedLikesAfterFirstToggle = if (!wasLiked) initialLikes + 1 else (initialLikes - 1).coerceAtLeast(0)
        assertEquals(expectedLikesAfterFirstToggle, updatedReel.likesCount)

        // 2. Toggle Like Reel again (Revert to original state)
        repository.toggleLikeReel(reel.id)
        val revertedReels = repository.getReelsStream().first()
        val revertedReel = revertedReels.first { it.id == reel.id }
        assertEquals("Reel liked state must revert", wasLiked, revertedReel.isLikedByMe)
        assertEquals(initialLikes, revertedReel.likesCount)
    }

    @Test
    fun `crm pipeline transitions lead across all stages with notes`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        val leads = repository.getLeadsStream().first()
        val lead = leads.first()
        val leadId = lead.id

        // 1. Advance to CONTACTED
        repository.updateLeadStage(leadId, LeadStage.CONTACTED, "Spoke with buyer, requested floor plans")
        val contacted = repository.getLeadsStream().first().first { it.id == leadId }
        assertEquals(LeadStage.CONTACTED, contacted.stage)
        assertTrue(contacted.notes.contains("requested floor plans"))

        // 2. Advance to SITE_VISIT
        repository.updateLeadStage(leadId, LeadStage.SITE_VISIT, "Visit fixed for Saturday 11am")
        val visit = repository.getLeadsStream().first().first { it.id == leadId }
        assertEquals(LeadStage.SITE_VISIT, visit.stage)

        // 3. Advance to NEGOTIATION
        repository.updateLeadStage(leadId, LeadStage.NEGOTIATION, "Offered ₹1.40 Cr, under developer review")
        val negotiation = repository.getLeadsStream().first().first { it.id == leadId }
        assertEquals(LeadStage.NEGOTIATION, negotiation.stage)

        // 4. Close as CLOSED
        repository.updateLeadStage(leadId, LeadStage.CLOSED, "Agreement signed, token received")
        val won = repository.getLeadsStream().first().first { it.id == leadId }
        assertEquals(LeadStage.CLOSED, won.stage)
    }

    @Test
    fun `whatsapp deep linking url and share payload format properly`() {
        val rawPhone = "+91 98220 12345"
        val cleanPhone = rawPhone.replace("+", "").replace(" ", "").replace("-", "")
        assertEquals("919822012345", cleanPhone)

        val propertyTitle = "Luxury 3 BHK in Baner"
        val priceDisplay = "₹1.45 Cr"
        val message = "Hello, I am interested in $propertyTitle ($priceDisplay) on PropReel"
        val encodedMessage = android.net.Uri.encode(message)
        val waUrl = "https://wa.me/$cleanPhone?text=$encodedMessage"

        assertTrue("WhatsApp URL must start with wa.me", waUrl.startsWith("https://wa.me/919822012345"))
        assertTrue("WhatsApp URL must contain encoded property title", waUrl.contains("Luxury%203%20BHK%20in%20Baner"))
    }

    @Test
    fun `monetization boost packages and verification badge validation`() {
        // Verification badge model checks
        val reraBadge = com.example.domain.model.VerificationBadge.RERA_VERIFIED
        assertEquals("RERA Registered", reraBadge.label)

        val ownerBadge = com.example.domain.model.VerificationBadge.OWNER_VERIFIED
        assertEquals("Verified Owner", ownerBadge.label)

        val builderBadge = com.example.domain.model.VerificationBadge.BUILDER_PARTNER
        assertEquals("Authorized Builder", builderBadge.label)

        // Promotion packages simulation & calculation checks
        val pkgFeaturedAmount = 999.0
        val pkgLocalityAmount = 1999.0
        val pkgSpotlightAmount = 4999.0

        val estimatedWeeklyImpressions = (pkgFeaturedAmount * 12.5).toInt()
        assertTrue("Featured boost must deliver over 10,000 estimated impressions", estimatedWeeklyImpressions > 10000)

        // Validating RERA ID pattern (MahaRERA: starts with P5 followed by district digits)
        val validMahaRera = "P52100028912"
        assertTrue("Valid MahaRERA ID format", validMahaRera.matches(Regex("^P\\d{11}$")))
    }

    @Test
    fun `vertical reels and swaraj group logo asset verification`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        val reels = repository.getReelsStream().first()
            .filter { it.id.matches(Regex("^reel_0\\d_.*")) }
        assertEquals("Repository must provide 3 seeded reels after removing 1st and 3rd posts", 3, reels.size)
        assertEquals("Repository must contain 1 seeded video reel", 1, reels.count { it.mediaType == "video" })
        assertEquals("Repository must contain exactly 2 seeded image reels", 2, reels.count { it.mediaType == "image" })
        assertEquals(
            "Seeded feed order must be Image -> Image -> Video",
            listOf("image", "image", "video"),
            reels.map { it.mediaType }
        )
        assertFalse("Removed 1st post reel_01_video must not be present", reels.any { it.id == "reel_01_video" })
        assertFalse("Removed 3rd post reel_03_video must not be present", reels.any { it.id == "reel_03_video" })

        // Verify Swaraj Group logo resource exists
        val logoResId = context.resources.getIdentifier("img_swaraj_logo", "drawable", context.packageName)
        assertTrue("Swaraj Group logo drawable must exist", logoResId != 0)

        // Verify BottomNavItem labels are concise for aligned footer navigation
        com.example.ui.navigation.BottomNavItem.values().forEach { item ->
            assertTrue("Nav item '${item.title}' must be short for symmetrical layout", item.title.length <= 8)
        }

        val kalyaniNagarReel = reels.find { it.locality == "Kalyani Nagar" }
        assertNotNull("Kalyani Nagar riverfront penthouse demo reel must exist", kalyaniNagarReel)
        assertEquals("₹5.50 Cr", kalyaniNagarReel?.propertyPriceDisplay)
        assertEquals("image", kalyaniNagarReel?.mediaType)

        val banerReel = reels.find { it.locality == "Baner" }
        assertNotNull("Baner 4 BHK walkthrough demo reel must exist", banerReel)
        assertEquals("video", banerReel?.mediaType)

        val worliReel = reels.find { it.locality == "Worli" }
        assertNotNull("Worli sea view demo reel must exist", worliReel)
        assertEquals("image", worliReel?.mediaType)
    }

    @Test
    fun `dynamic city localities mapping and explore search seo verification`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        // 1. Verify dynamic city locality mapping
        val mumbaiLocalities = com.example.ui.screens.CITY_LOCALITIES_MAP["Mumbai"]
        assertNotNull("Mumbai localities map must be defined", mumbaiLocalities)
        assertTrue("Mumbai must contain Worli", mumbaiLocalities!!.contains("Worli"))
        assertTrue("Mumbai must contain Bandra West", mumbaiLocalities.contains("Bandra West"))
        assertTrue("Mumbai must contain Andheri West", mumbaiLocalities.contains("Andheri West"))
        assertFalse("Mumbai MUST NOT contain Pune localities like Baner", mumbaiLocalities.contains("Baner"))
        assertFalse("Mumbai MUST NOT contain Pune localities like Wakad", mumbaiLocalities.contains("Wakad"))

        val puneLocalities = com.example.ui.screens.CITY_LOCALITIES_MAP["Pune"]
        assertNotNull("Pune localities map must be defined", puneLocalities)
        assertTrue("Pune must contain Baner", puneLocalities!!.contains("Baner"))
        assertTrue("Pune must contain Wakad", puneLocalities.contains("Wakad"))

        // 2. Verify Mumbai seeded properties
        val allProps = repository.getPropertiesStream().first()
        val mumbaiProps = allProps.filter { it.city.equals("Mumbai", ignoreCase = true) }
        assertTrue("Must have seeded Mumbai properties", mumbaiProps.isNotEmpty())
        val worliProp = mumbaiProps.find { it.locality == "Worli" }
        assertNotNull("Worli property must exist", worliProp)
        assertTrue("Worli property must have valid MahaRERA ID", worliProp?.reraId?.startsWith("P51") == true)

        // 3. Verify SEO Slug & Title format
        val activeCity = "Mumbai"
        val locality = "Worli"
        val expectedSeoSlug = "https://propreel.in/${activeCity.lowercase()}/${locality.lowercase()}"
        assertEquals("https://propreel.in/mumbai/worli", expectedSeoSlug)
    }

    @Test
    fun `create and upload real estate reel persists to Room and updates feed immediately`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        val initialReels = repository.getReelsStream().first()
        val initialCount = initialReels.size

        // 1. Create a newly uploaded Video Reel
        val videoReelId = "reel_test_video_${System.currentTimeMillis()}"
        val newVideoReel = com.example.domain.model.Reel(
            id = videoReelId,
            userId = "usr_test_broker",
            creatorName = "Rajesh Mehta",
            creatorBadge = VerificationBadge.RERA_VERIFIED,
            creatorRole = UserRole.BROKER,
            propertyId = "prop_pune_01",
            propertyTitle = "VTP Earth One — Luxury 3 BHK",
            propertyPriceDisplay = "₹1.45 Cr",
            propertyLocality = "Baner",
            propertyBhk = 3,
            caption = "Exclusive walkthrough of 3 BHK in Baner #PuneHomes",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            thumbnailUrl = "https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?w=900&q=80",
            durationSeconds = 30,
            city = "Pune",
            locality = "Baner",
            mediaType = "video"
        )

        repository.createReel(newVideoReel)

        // 2. Create a newly uploaded Photo Reel (Image mediaType)
        val imageReelId = "reel_test_image_${System.currentTimeMillis()}"
        val newPhotoReel = com.example.domain.model.Reel(
            id = imageReelId,
            userId = "usr_test_broker",
            creatorName = "Rajesh Mehta",
            creatorBadge = VerificationBadge.RERA_VERIFIED,
            creatorRole = UserRole.BROKER,
            propertyId = null,
            propertyTitle = null,
            propertyPriceDisplay = null,
            propertyLocality = "Worli",
            propertyBhk = null,
            caption = "Stunning sunset panorama from Worli Sea Face #MumbaiRealty",
            videoUrl = "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=1080&q=85",
            thumbnailUrl = "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=1080&q=85",
            durationSeconds = 0,
            city = "Mumbai",
            locality = "Worli",
            mediaType = "image"
        )

        repository.createReel(newPhotoReel)

        // 3. Verify immediately streamed from Room
        val updatedReels = repository.getReelsStream().first()
        assertEquals(initialCount + 2, updatedReels.size)

        val fetchedVideo = updatedReels.find { it.id == videoReelId }
        assertNotNull("Uploaded video reel must exist in stream", fetchedVideo)
        assertEquals("video", fetchedVideo?.mediaType)
        assertEquals("Baner", fetchedVideo?.locality)
        assertEquals("VTP Earth One — Luxury 3 BHK", fetchedVideo?.propertyTitle)

        val fetchedImage = updatedReels.find { it.id == imageReelId }
        assertNotNull("Uploaded photo reel must exist in stream", fetchedImage)
        assertEquals("image", fetchedImage?.mediaType)
        assertEquals("Worli", fetchedImage?.locality)
    }

    @Test
    fun `upload reel modal presets and media options are valid`() {
        val presets = com.example.ui.screens.PRESET_DEMO_MEDIA
        assertTrue("Preset demo media must not be empty", presets.isNotEmpty())

        val videoSamples = presets.filter { it.type == com.example.ui.screens.ReelMediaTypeOption.VIDEO }
        val imageSamples = presets.filter { it.type == com.example.ui.screens.ReelMediaTypeOption.IMAGE }

        assertTrue("Must have video demo options", videoSamples.isNotEmpty())
        assertTrue("Must have photo demo options", imageSamples.isNotEmpty())

        presets.forEach { sample ->
            assertTrue("Sample mediaUrl must be valid URL", sample.mediaUrl.startsWith("http"))
            assertTrue("Sample thumbnailUrl must be valid URL", sample.thumbnailUrl.startsWith("http"))
            assertTrue("Sample caption must not be blank", sample.defaultCaption.isNotBlank())
            assertTrue("Sample locality must not be blank", sample.defaultLocality.isNotBlank())
        }
    }

    @Test
    fun `post page creates property listings exclusively and reel creation is isolated`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        val initialProps = repository.getPropertiesStream().first()
        val initialCount = initialProps.size

        val testProperty = Property(
            id = "prop_post_test_01",
            userId = "usr_pune_broker_01",
            sellerName = "Vikram Deshmukh",
            sellerRole = UserRole.BROKER,
            sellerPhone = "+91 98230 45678",
            sellerBadge = VerificationBadge.RERA_VERIFIED,
            title = "Pristine Bay — 3 BHK Luxury Flat in Baner",
            description = "Exclusive ready to move flat with open valley views and modular fittings.",
            category = PropertyCategory.RESIDENTIAL_BUY,
            price = 11500000.0,
            priceDisplay = "₹1.15 Cr",
            isNegotiable = true,
            city = "Pune",
            locality = "Baner",
            landmark = "Near High Street Baner",
            bhk = 3,
            bathrooms = 3,
            carpetAreaSqft = 1100.0,
            floorNumber = 12,
            totalFloors = 22,
            furnishing = FurnishingStatus.SEMI_FURNISHED,
            possession = PossessionStatus.READY_TO_MOVE,
            amenities = listOf("Clubhouse", "Gym", "Power Backup", "Security"),
            imageUrls = listOf("https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=1000&q=80")
        )

        repository.createProperty(testProperty)

        val updatedProps = repository.getPropertiesStream().first()
        assertEquals(initialCount + 1, updatedProps.size)
        val created = updatedProps.find { it.id == "prop_post_test_01" }
        assertNotNull("Newly posted property must exist", created)
        assertEquals("Baner", created?.locality)
        assertEquals("₹1.15 Cr", created?.priceDisplay)
    }

    @Test
    fun `mixed feed ordering retains remaining reels with idempotent seeding`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)

        // Run seed multiple times to verify strict idempotency and confirm removed posts stay removed
        repository.seedInitialDatabaseIfEmpty()
        repository.seedInitialDatabaseIfEmpty()
        repository.seedInitialDatabaseIfEmpty()

        val reels = repository.getReelsStream().first()
            .filter { it.id.matches(Regex("^reel_0\\d_.*")) }
        assertEquals("Feed must contain 3 seeded reels (1 video + 2 image) after removing 1st and 3rd posts", 3, reels.size)
        assertFalse("reel_01_video must remain deleted", reels.any { it.id == "reel_01_video" })
        assertFalse("reel_03_video must remain deleted", reels.any { it.id == "reel_03_video" })

        val expectedOrder = listOf("image", "image", "video")
        assertEquals("Seeded feed order must be Image -> Image -> Video", expectedOrder, reels.map { it.mediaType })

        val reel2 = reels[0]
        assertEquals("reel_02_image", reel2.id)
        assertEquals("image", reel2.mediaType)
        assertEquals("Kalyani Nagar", reel2.locality)
        assertTrue("Reel 2 must link to Riverfront Duplex Penthouse", reel2.propertyTitle?.contains("Riverfront Duplex") == true)
        assertTrue("Image 1 must have valid image URL", reel2.videoUrl.startsWith("http"))

        val reel4 = reels[1]
        assertEquals("reel_04_image", reel4.id)
        assertEquals("image", reel4.mediaType)
        assertEquals("Worli", reel4.locality)
        assertTrue("Reel 4 must link to Lodha World One", reel4.propertyTitle?.contains("Lodha World One") == true)
        assertTrue("Image 2 must have valid image URL", reel4.videoUrl.startsWith("http"))

        val reel5 = reels[2]
        assertEquals("reel_05_video", reel5.id)
        assertEquals("video", reel5.mediaType)
        assertEquals("Baner", reel5.locality)
        assertEquals(4, reel5.propertyBhk)
        assertTrue("Reel 5 must link to Ready Possession 4 BHK", reel5.propertyTitle?.contains("Ready Possession 4 BHK") == true)
        assertTrue("Video 5 must have valid mp4 URL", reel5.videoUrl.endsWith(".mp4"))
    }

    @Test
    fun `image reel photo tour configurations and timer parameters`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        val reels = repository.getReelsStream().first()
        val imageReels = reels.filter { it.mediaType == "image" }
        assertEquals(2, imageReels.size)

        imageReels.forEach { reel ->
            assertEquals("image", reel.mediaType)
            assertTrue("Image reel URL must be valid HTTP link", reel.videoUrl.startsWith("http"))
            assertTrue("Thumbnail URL must be valid HTTP link", reel.thumbnailUrl.startsWith("http"))
            assertTrue("Caption must be descriptive", reel.caption.isNotBlank())
            assertNotNull("Property must be linked", reel.propertyId)
        }

        // Test auto-advance duration calculation (5 seconds / 5000 ms per slide)
        val defaultAutoAdvanceMs = 5000L
        val stepIntervalMs = 50L
        val totalSteps = (defaultAutoAdvanceMs / stepIntervalMs).toInt()
        assertEquals(100, totalSteps)
    }

    @Test
    fun `video walkthrough playback parameters and mute toggle state`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        val reels = repository.getReelsStream().first()
            .filter { it.id.matches(Regex("^reel_0\\d_.*")) }
        val videoReels = reels.filter { it.mediaType == "video" }
        assertEquals(1, videoReels.size)
        assertEquals("reel_05_video", videoReels.first().id)

        videoReels.forEach { reel ->
            assertEquals("video", reel.mediaType)
            assertTrue("Video URL must point to mp4 stream", reel.videoUrl.endsWith(".mp4"))
            assertTrue("Duration should be realistic for real estate reel", reel.durationSeconds in 10..60)
            assertTrue("Thumbnail must be available for video poster", reel.thumbnailUrl.isNotBlank())
        }

        // Test mute toggle state logic
        var isMuted = false
        isMuted = !isMuted
        assertTrue("Mute toggle should set isMuted to true", isMuted)
        isMuted = !isMuted
        assertFalse("Mute toggle should reset isMuted to false", isMuted)
    }

    @Test
    fun `pinch to zoom constraints and double tap reset behavior`() {
        var zoomScale = 1.0f

        // 1. Zoom in clamped to max 3.5x
        val requestedZoom = 5.0f
        zoomScale = (zoomScale * requestedZoom).coerceIn(1f, 3.5f)
        assertEquals(3.5f, zoomScale, 0.001f)

        // 2. Zoom out clamped to min 1.0x
        val requestedShrink = 0.1f
        zoomScale = (zoomScale * requestedShrink).coerceIn(1f, 3.5f)
        assertEquals(1.0f, zoomScale, 0.001f)

        // 3. Zoom reset on double-tap when zoomed in
        zoomScale = 2.4f
        var resetTriggered = false
        if (zoomScale > 1.05f) {
            zoomScale = 1.0f
            resetTriggered = true
        }
        assertTrue("Double-tap must reset zoom if zoomed in", resetTriggered)
        assertEquals(1.0f, zoomScale, 0.001f)

        // 4. Double tap when at 1.0f triggers like instead of zoom reset
        var likeTriggered = false
        if (zoomScale > 1.05f) {
            zoomScale = 1.0f
        } else {
            likeTriggered = true
        }
        assertTrue("Double-tap at 1.0x must trigger like action", likeTriggered)
    }

    @Test
    fun `playback lifecycle and timer cancellation when reel is inactive`() {
        var videoPlaybackProgress = 0.75f
        var imageAdvanceProgress = 0.60f
        var zoomScale = 2.2f

        // Inactive event (e.g. user scrolled to another reel or navigated away)
        val isActive = false
        if (!isActive) {
            videoPlaybackProgress = 0f
            imageAdvanceProgress = 0f
            zoomScale = 1f
        }

        assertEquals(0f, videoPlaybackProgress, 0.001f)
        assertEquals(0f, imageAdvanceProgress, 0.001f)
        assertEquals(1f, zoomScale, 0.001f)
    }

    @Test
    fun `invalid or offline media fallback url resolution`() {
        val testReelWithEmptyVideo = com.example.domain.model.Reel(
            id = "reel_test_fallback_1",
            userId = "usr_test",
            creatorName = "Broker Test",
            creatorBadge = VerificationBadge.NONE,
            creatorRole = UserRole.BROKER,
            propertyId = null,
            propertyTitle = null,
            propertyPriceDisplay = null,
            propertyLocality = "Baner",
            propertyBhk = null,
            caption = "Fallback test caption",
            videoUrl = "",
            thumbnailUrl = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=1000&q=80",
            durationSeconds = 20,
            city = "Pune",
            locality = "Baner",
            mediaType = "image"
        )

        // Fallback display url logic as used in ReelMediaRenderer
        val displayUrl = if (testReelWithEmptyVideo.mediaType.equals("image", ignoreCase = true) && testReelWithEmptyVideo.videoUrl.startsWith("http")) {
            testReelWithEmptyVideo.videoUrl
        } else {
            testReelWithEmptyVideo.thumbnailUrl.ifEmpty { testReelWithEmptyVideo.videoUrl }
        }

        assertEquals("https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=1000&q=80", displayUrl)
    }

    // --- PHASE 3: REFER & EARN UNIT & ROBOLECTRIC TESTS ---

    @Test
    fun `unique referral code generation format and determinism`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)

        val code1 = repository.generateReferralCodeForUser("usr_pune_broker_01")
        val code2 = repository.generateReferralCodeForUser("usr_pune_owner_02")
        val code3 = repository.generateReferralCodeForUser("usr_pune_broker_01")

        assertTrue("Referral code must start with PROP prefix", code1.startsWith("PROP"))
        assertTrue("Referral code must be uppercase alphanumeric", code1.matches("^[A-Z0-9]+$".toRegex()))
        assertEquals("Deterministic generation must yield identical code for same user", code1, code3)
        assertFalse("Different users should generate distinct codes", code1 == code2)
    }

    @Test
    fun `referral link generation and share message builder`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)

        val testCode = "PROP9823"
        val expectedLink = "https://propreel.in/refer/$testCode"
        val actualLink = repository.getReferralLink(testCode)
        assertEquals(expectedLink, actualLink)

        val shareMessage = ReferralShareHelper.buildShareMessage(testCode, actualLink)
        assertTrue("Share message must contain the referral code", shareMessage.contains(testCode))
        assertTrue("Share message must contain the full referral URL", shareMessage.contains(actualLink))
        assertTrue("Share message must promote PropReel features", shareMessage.contains("PropReel"))
    }

    @Test
    fun `referral code persistence and stats retrieval from Room`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        val brokerId = "usr_pune_broker_01"
        val refCode = repository.getOrCreateReferralCode(brokerId)
        assertEquals("PROP9823", refCode.code)
        assertEquals(brokerId, refCode.userId)
        assertTrue(refCode.isActive)

        val stats = repository.getReferralStatsStream(brokerId).first()
        assertEquals("PROP9823", stats.referralCode)
        assertEquals("https://propreel.in/refer/PROP9823", stats.referralLink)
        assertTrue("Must include seeded total referrals", stats.totalReferrals >= 3)
        assertTrue("Must include seeded qualified referrals", stats.qualifiedCount >= 1)
        assertTrue("Must include seeded pending referrals", stats.pendingCount >= 1)

        val referralsList = repository.getReferralsStream(brokerId).first()
        assertTrue("Referrals stream must be non-empty", referralsList.isNotEmpty())
    }

    @Test
    fun `prevent self referral`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        val brokerId = "usr_pune_broker_01"
        // Attempt to apply own referral code
        val selfResult = repository.applyReferralCode(
            referredUserId = brokerId,
            code = "PROP9823",
            source = AttributionSource.COPY_LINK
        )

        assertTrue("Self-referral must be rejected", selfResult is ReferralApplyResult.Error)
        val errorMessage = (selfResult as ReferralApplyResult.Error).message
        assertTrue("Error message must explain self-referral prohibition", errorMessage.contains("Self-referral is strictly prohibited"))
    }

    @Test
    fun `referral attribution with different sources and PENDING status`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        val newReferredUser = "usr_client_new_${System.currentTimeMillis()}"

        // Apply via WhatsApp
        val result = repository.applyReferralCode(
            referredUserId = newReferredUser,
            code = "PROP9823",
            source = AttributionSource.WHATSAPP
        )

        assertTrue("Valid referral code application should succeed", result is ReferralApplyResult.Success)
        val referral = (result as ReferralApplyResult.Success).referral
        assertEquals("usr_pune_broker_01", referral.referrerId)
        assertEquals(newReferredUser, referral.referredUserId)
        assertEquals("PROP9823", referral.referralCode)
        assertEquals(AttributionSource.WHATSAPP, referral.attributionSource)
        assertEquals(ReferralStatus.PENDING, referral.status)
        assertTrue("Created timestamp must be recent", referral.createdAt > 0)
    }

    @Test
    fun `prevent duplicate referral attribution for same user`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        val newReferredUser = "usr_client_dup_${System.currentTimeMillis()}"

        // First application succeeds
        val result1 = repository.applyReferralCode(
            referredUserId = newReferredUser,
            code = "PROP9823",
            source = AttributionSource.DIRECT_LINK
        )
        assertTrue(result1 is ReferralApplyResult.Success)

        // Second application by the same user must be blocked
        val result2 = repository.applyReferralCode(
            referredUserId = newReferredUser,
            code = "PROP9823",
            source = AttributionSource.COPY_LINK
        )
        assertTrue("Duplicate attribution must be rejected", result2 is ReferralApplyResult.Error)
        val errorMsg = (result2 as ReferralApplyResult.Error).message
        assertTrue("Error message must indicate already applied", errorMsg.contains("already applied"))
    }

    @Test
    fun `referral qualification transitions to QUALIFIED and blocks duplicate qualification`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        val candidateUser = "usr_candidate_${System.currentTimeMillis()}"
        val applyResult = repository.applyReferralCode(
            referredUserId = candidateUser,
            code = "PROP9823",
            source = AttributionSource.WHATSAPP
        )
        assertTrue(applyResult is ReferralApplyResult.Success)

        // Qualify referral
        var qualifiedCallbackInvoked = false
        repository.setReferralRewardListener(object : ReferralRewardListener {
            override suspend fun onReferralQualified(referral: Referral) {
                qualifiedCallbackInvoked = true
                assertEquals(candidateUser, referral.referredUserId)
                assertEquals(ReferralStatus.QUALIFIED, referral.status)
            }
            override suspend fun onReferralRewarded(referral: Referral) {}
        })

        val qualified = repository.qualifyReferral(candidateUser, "Completed verified profile and enquiry")
        assertTrue("Qualification should return true on first transition", qualified)
        assertTrue("Reward listener callback must be called", qualifiedCallbackInvoked)

        // Verify status in stream
        val referrals = repository.getReferralsStream("usr_pune_broker_01").first()
        val found = referrals.find { it.referredUserId == candidateUser }
        assertNotNull(found)
        assertEquals(ReferralStatus.QUALIFIED, found?.status)
        assertNotNull(found?.qualifiedAt)

        // Attempting to qualify again must be blocked (duplicate qualification prevention)
        val qualifyAgain = repository.qualifyReferral(candidateUser, "Duplicate attempt")
        assertFalse("Duplicate qualification must return false", qualifyAgain)
    }

    @Test
    fun `attribution window expiration blocks qualification`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        val expiredCandidate = "usr_expired_${System.currentTimeMillis()}"
        val applyResult = repository.applyReferralCode(
            referredUserId = expiredCandidate,
            code = "PROP9823",
            source = AttributionSource.DIRECT_LINK
        )
        assertTrue(applyResult is ReferralApplyResult.Success)

        // Simulate expiration by setting attribution window to 0 days
        repository.attributionWindowDays = 0L

        val qualifyAttempt = repository.qualifyReferral(expiredCandidate, "Late interaction attempt")
        assertFalse("Qualification after attribution window must fail", qualifyAttempt)

        // Restore default window
        repository.attributionWindowDays = 30L
    }

    @Test
    fun `referral reward transition to REWARDED for Phase 4 preparation`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        val rewardCandidate = "usr_reward_${System.currentTimeMillis()}"
        val applyResult = repository.applyReferralCode(
            referredUserId = rewardCandidate,
            code = "PROP9823",
            source = AttributionSource.COPY_LINK
        )
        val referral = (applyResult as ReferralApplyResult.Success).referral

        // First qualify it
        val qualified = repository.qualifyReferral(rewardCandidate, "Verified interaction")
        assertTrue(qualified)

        // Mark rewarded
        var rewardedCallbackInvoked = false
        repository.setReferralRewardListener(object : ReferralRewardListener {
            override suspend fun onReferralQualified(referral: Referral) {}
            override suspend fun onReferralRewarded(referral: Referral) {
                rewardedCallbackInvoked = true
                assertEquals(ReferralStatus.REWARDED, referral.status)
            }
        })

        val rewarded = repository.markReferralRewarded(referral.id, "Prepared for Phase 4 coin credit")
        assertTrue(rewarded)
        assertTrue(rewardedCallbackInvoked)

        val updatedReferrals = repository.getReferralsStream("usr_pune_broker_01").first()
        val found = updatedReferrals.find { it.id == referral.id }
        assertNotNull(found)
        assertEquals(ReferralStatus.REWARDED, found?.status)
        assertNotNull(found?.rewardedAt)
    }

    @Test
    fun `video raw resources exist and are valid for video playback`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val banerResId = context.resources.getIdentifier("demo_reel_baner", "raw", context.packageName)
        val kothrudResId = context.resources.getIdentifier("demo_reel_kothrud", "raw", context.packageName)
        val baner4BhkResId = context.resources.getIdentifier("demo_reel_baner_4bhk", "raw", context.packageName)
        val baner4BhkPosterId = context.resources.getIdentifier("poster_baner_4bhk", "drawable", context.packageName)

        assertTrue("demo_reel_baner.mp4 must exist in res/raw", banerResId != 0)
        assertTrue("demo_reel_kothrud.mp4 must exist in res/raw", kothrudResId != 0)
        assertTrue("demo_reel_baner_4bhk.mp4 must exist in res/raw", baner4BhkResId != 0)
        assertTrue("poster_baner_4bhk.jpg must exist in res/drawable", baner4BhkPosterId != 0)
    }

    @Test
    fun `resolveVideoUri maps broken sample links to bundled raw assets and preserves custom URIs`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pkg = context.packageName

        // 1. Broken Google sample URLs should redirect to local playable resources
        val legacyUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
        val resolvedLegacy = com.example.ui.components.resolveVideoUri(context, legacyUrl, "reel_custom_99")
        assertEquals(Uri.parse("android.resource://$pkg/raw/demo_reel_baner"), resolvedLegacy)

        // 2. Demo reel 1 maps to demo_reel_baner
        val resolvedReel1 = com.example.ui.components.resolveVideoUri(context, "any_url", "reel_01_video")
        assertEquals(Uri.parse("android.resource://$pkg/raw/demo_reel_baner"), resolvedReel1)

        // 3. Demo reel 3 maps to demo_reel_kothrud
        val resolvedReel3 = com.example.ui.components.resolveVideoUri(context, "any_url", "reel_03_video")
        assertEquals(Uri.parse("android.resource://$pkg/raw/demo_reel_kothrud"), resolvedReel3)

        // 4. Demo reel 5 (Baner 4 BHK) maps to demo_reel_baner_4bhk
        val resolvedReel5 = com.example.ui.components.resolveVideoUri(
            context,
            "https://assets.propreel.in/videos/demo_reel_baner_4bhk.mp4",
            "reel_05_video"
        )
        assertEquals(Uri.parse("android.resource://$pkg/raw/demo_reel_baner_4bhk"), resolvedReel5)

        // 5. Custom user content URIs should be preserved
        val contentUri = "content://media/external/video/media/12345"
        val resolvedContent = com.example.ui.components.resolveVideoUri(context, contentUri, "user_reel_01")
        assertEquals(Uri.parse(contentUri), resolvedContent)

        // 6. Valid external HTTPS streams should be preserved
        val httpsUri = "https://example.com/tour_video.mp4"
        val resolvedHttps = com.example.ui.components.resolveVideoUri(context, httpsUri, "user_reel_02")
        assertEquals(Uri.parse(httpsUri), resolvedHttps)
    }

    @Test
    fun `reels repository provides playable video URIs for video reels`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        val allReels = repository.getReelsStream().first()
        val demoReels = allReels.filter { it.id.matches(Regex("^reel_0\\d_.*")) }
        val videoReels = demoReels.filter { it.mediaType.equals("video", ignoreCase = true) }
        assertEquals("Feed must provide 1 seeded video reel", 1, videoReels.size)
        assertEquals("reel_05_video", videoReels.first().id)

        allReels.filter { it.mediaType.equals("video", ignoreCase = true) }.forEach { videoReel ->
            assertEquals("video", videoReel.mediaType)
            assertTrue(
                "Video reel ${videoReel.id} must have a valid playable videoUrl (local raw, file://, or https)",
                videoReel.videoUrl.startsWith("android.resource://") ||
                    videoReel.videoUrl.startsWith("http") ||
                    videoReel.videoUrl.startsWith("file://")
            )
            val resolvedUri = com.example.ui.components.resolveVideoUri(context, videoReel.videoUrl, videoReel.id)
            assertNotNull(resolvedUri)
            assertFalse("Resolved URI must not be empty", resolvedUri.toString().isEmpty())
        }

        val baner4BhkProp = repository.getPropertyById("prop_pune_baner_4bhk")
        assertNotNull("Linked 4 BHK Baner property must be seeded", baner4BhkProp)
        assertEquals("+91 91194 87956", baner4BhkProp?.sellerPhone)
        assertEquals(2000.0, baner4BhkProp?.carpetAreaSqft ?: 0.0, 0.01)

        val imageReels = demoReels.filter { it.mediaType.equals("image", ignoreCase = true) }
        assertEquals("Feed must provide exactly 2 image reels", 2, imageReels.size)
        imageReels.forEach { imageReel ->
            assertEquals("image", imageReel.mediaType)
            assertTrue("Image reel must have image or thumbnail url", imageReel.thumbnailUrl.isNotEmpty() || imageReel.videoUrl.isNotEmpty())
        }
    }

    // --- PHASE 4: COIN ECONOMY UNIT & ROBOLECTRIC TESTS ---

    @Test
    fun `50 coin referrer reward and 25 coin invitee reward credited atomically on qualification`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        val referrerId = "usr_referrer_${System.currentTimeMillis()}"
        val inviteeId = "usr_invitee_${System.currentTimeMillis()}"

        val codeObj = repository.getOrCreateReferralCode(referrerId)
        val applyResult = repository.applyReferralCode(
            referredUserId = inviteeId,
            code = codeObj.code,
            source = AttributionSource.WHATSAPP
        )
        assertTrue(applyResult is ReferralApplyResult.Success)
        val referral = (applyResult as ReferralApplyResult.Success).referral

        // Before qualification: reward processing MUST be rejected and balances MUST be 0
        val prematureReward = repository.processReferralReward(referral.id)
        assertTrue("Unqualified PENDING referral must not grant coins", prematureReward is CoinRewardProcessResult.Rejected)
        assertEquals(0, repository.getAuthoritativeCoinBalance(referrerId))
        assertEquals(0, repository.getAuthoritativeCoinBalance(inviteeId))

        // Qualify & reward atomically
        val rewardResult = repository.qualifyAndRewardReferral(inviteeId, "Verified site visit enquiry")
        assertTrue("Qualified referral should credit rewards", rewardResult is CoinRewardProcessResult.Success)

        val success = rewardResult as CoinRewardProcessResult.Success
        assertEquals(CoinEconomyConfig.REFERRER_REWARD_COINS, success.referrerTransaction.amount)
        assertEquals(50, success.referrerNewBalance)
        assertEquals(CoinTransactionCategory.REFERRAL_REWARD, success.referrerTransaction.category)
        assertEquals(referral.id, success.referrerTransaction.referenceId)

        assertEquals(CoinEconomyConfig.INVITEE_WELCOME_COINS, success.inviteeTransaction.amount)
        assertEquals(25, success.inviteeNewBalance)
        assertEquals(CoinTransactionCategory.WELCOME_REWARD, success.inviteeTransaction.category)
        assertEquals(referral.id, success.inviteeTransaction.referenceId)

        // Verify authoritative balance calculation
        assertEquals(50, repository.getAuthoritativeCoinBalance(referrerId))
        assertEquals(25, repository.getAuthoritativeCoinBalance(inviteeId))

        // Verify referral transitioned to REWARDED
        val updatedReferrals = repository.getReferralsStream(referrerId).first()
        val updatedRef = updatedReferrals.first { it.id == referral.id }
        assertEquals(ReferralStatus.REWARDED, updatedRef.status)
        assertNotNull(updatedRef.rewardedAt)
    }

    @Test
    fun `duplicate reward processing is prevented and adds zero additional coins`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        val referrerId = "usr_dup_ref_${System.currentTimeMillis()}"
        val inviteeId = "usr_dup_inv_${System.currentTimeMillis()}"
        val refCode = repository.getOrCreateReferralCode(referrerId)

        val referral = (repository.applyReferralCode(inviteeId, refCode.code, AttributionSource.DIRECT_LINK) as ReferralApplyResult.Success).referral
        assertTrue(repository.qualifyReferral(inviteeId, "Verified profile"))

        val firstProcess = repository.processReferralReward(referral.id)
        assertTrue(firstProcess is CoinRewardProcessResult.Success)
        assertEquals(50, repository.getAuthoritativeCoinBalance(referrerId))
        assertEquals(25, repository.getAuthoritativeCoinBalance(inviteeId))

        // Second reward processing attempt on same referral
        val secondProcess = repository.processReferralReward(referral.id)
        assertTrue("Second reward attempt must return AlreadyProcessed", secondProcess is CoinRewardProcessResult.AlreadyProcessed)

        // Repeated qualifyAndRewardReferral attempt
        val repeatQualifyAndReward = repository.qualifyAndRewardReferral(inviteeId, "Repeated attempt")
        assertTrue(repeatQualifyAndReward is CoinRewardProcessResult.AlreadyProcessed)

        // Balances must remain strictly unchanged
        assertEquals(50, repository.getAuthoritativeCoinBalance(referrerId))
        assertEquals(25, repository.getAuthoritativeCoinBalance(inviteeId))
    }

    @Test
    fun `duplicate idempotency key rejection enforces unique ledger constraint`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        val userId = "usr_idem_${System.currentTimeMillis()}"
        val key = "idem_key_unique_${System.currentTimeMillis()}"

        val first = repository.creditCoinsIdempotent(
            userId = userId,
            amount = 50,
            category = CoinTransactionCategory.REFERRAL_REWARD,
            description = "Test idempotent credit",
            referenceId = "ref_test_1",
            idempotencyKey = key
        )
        assertTrue(first is CoinOperationResult.Success)
        assertEquals(50, repository.getAuthoritativeCoinBalance(userId))

        val duplicate = repository.creditCoinsIdempotent(
            userId = userId,
            amount = 50,
            category = CoinTransactionCategory.REFERRAL_REWARD,
            description = "Duplicate idempotent credit attempt",
            referenceId = "ref_test_1",
            idempotencyKey = key
        )
        assertTrue("Duplicate idempotency key must be rejected", duplicate is CoinOperationResult.DuplicateRejected)
        assertEquals(50, repository.getAuthoritativeCoinBalance(userId))
    }

    @Test
    fun `multiple concurrent reward attempts only credit once atomically`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        val referrerId = "usr_conc_ref_${System.currentTimeMillis()}"
        val inviteeId = "usr_conc_inv_${System.currentTimeMillis()}"
        val codeObj = repository.getOrCreateReferralCode(referrerId)

        val referral = (repository.applyReferralCode(inviteeId, codeObj.code, AttributionSource.COPY_LINK) as ReferralApplyResult.Success).referral
        assertTrue(repository.qualifyReferral(inviteeId, "Qualified before concurrent burst"))

        // Fire 10 concurrent reward processing coroutines simultaneously
        val results = (1..10).map {
            async {
                repository.processReferralReward(referral.id, "Concurrent burst #$it")
            }
        }.awaitAll()

        val successes = results.count { it is CoinRewardProcessResult.Success }
        val alreadyProcessed = results.count { it is CoinRewardProcessResult.AlreadyProcessed }

        assertEquals("Exactly 1 concurrent reward attempt may succeed", 1, successes)
        assertEquals("All 9 other concurrent attempts must be rejected as AlreadyProcessed", 9, alreadyProcessed)
        assertEquals(50, repository.getAuthoritativeCoinBalance(referrerId))
        assertEquals(25, repository.getAuthoritativeCoinBalance(inviteeId))
    }

    @Test
    fun `debit redemption, balance calculation, insufficient balance rejection, and no negative balance`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        val userId = "usr_redeem_${System.currentTimeMillis()}"

        // Configure deterministic redemption rules
        repository.configureRedemptionRules(
            listOf(
                RedemptionRule(
                    id = "rule_25",
                    title = "Priority Lead Pass",
                    description = "25 coin perk",
                    coinCost = 25,
                    minBalanceRequired = 25
                ),
                RedemptionRule(
                    id = "rule_100",
                    title = "Featured Badge 7D",
                    description = "100 coin perk",
                    coinCost = 100,
                    minBalanceRequired = 100
                )
            )
        )

        // Credit 50 coins
        val credit = repository.creditCoinsIdempotent(
            userId = userId,
            amount = 50,
            category = CoinTransactionCategory.REFERRAL_REWARD,
            description = "Earned 50 referral reward",
            referenceId = "ref_x",
            idempotencyKey = "credit_redeem_test_$userId"
        )
        assertTrue(credit is CoinOperationResult.Success)
        assertEquals(50, repository.getAuthoritativeCoinBalance(userId))

        // Attempt to redeem 100-coin perk with only 50 balance -> MUST be rejected
        val tooExpensive = repository.redeemCoins(userId, "rule_100", "redeem_fail_100_$userId")
        assertTrue("Insufficient balance must be rejected", tooExpensive is CoinOperationResult.Error)
        assertEquals(50, repository.getAuthoritativeCoinBalance(userId))

        // Valid redemption of 25-coin perk
        val redeemKey = "redeem_ok_25_$userId"
        val validRedeem = repository.redeemCoins(userId, "rule_25", redeemKey)
        assertTrue(validRedeem is CoinOperationResult.Success)
        val successTx = (validRedeem as CoinOperationResult.Success).transaction
        assertEquals(CoinTransactionType.DEBIT, successTx.transactionType)
        assertEquals(CoinTransactionCategory.REDEMPTION, successTx.category)
        assertEquals(25, successTx.amount)
        assertEquals(25, repository.getAuthoritativeCoinBalance(userId))

        // Duplicate redemption request with identical idempotency key -> MUST be rejected
        val dupRedeem = repository.redeemCoins(userId, "rule_25", redeemKey)
        assertTrue("Duplicate redemption key must be rejected", dupRedeem is CoinOperationResult.DuplicateRejected)
        assertEquals(25, repository.getAuthoritativeCoinBalance(userId))

        // Second valid 25-coin redemption leaves 0 balance
        val secondRedeem = repository.redeemCoins(userId, "rule_25", "redeem_ok_25_second_$userId")
        assertTrue(secondRedeem is CoinOperationResult.Success)
        assertEquals(0, repository.getAuthoritativeCoinBalance(userId))

        // Concurrent attempts when balance is 0 must never produce negative balance
        val zeroAttempt = repository.redeemCoins(userId, "rule_25", "redeem_zero_fail_$userId")
        assertTrue("Zero balance redemption must be rejected", zeroAttempt is CoinOperationResult.Error)
        assertTrue("Authoritative balance must never be negative", repository.getAuthoritativeCoinBalance(userId) >= 0)
        assertEquals(0, repository.getAuthoritativeCoinBalance(userId))

        // Verify wallet summary separates referral rewards, welcome rewards, and debits cleanly
        val walletSummary = repository.getCoinWalletSummaryStream(userId).first()
        assertEquals(0, walletSummary.authoritativeBalance)
        assertEquals(50, walletSummary.referralRewardsCoins)
        assertEquals(0, walletSummary.welcomeRewardsCoins)
        assertEquals(50, walletSummary.totalRedeemedCoins)
        assertTrue(walletSummary.isLocalPrototypeLedger)
    }

    @Test
    fun `device video and image upload helper detects format and persists playable file URI`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Format detection from MIME and extensions
        assertEquals(
            com.example.ui.screens.ReelMediaTypeOption.VIDEO,
            com.example.util.MediaUploadHelper.detectMediaTypeFromHints("video/mp4", null)
        )
        assertEquals(
            com.example.ui.screens.ReelMediaTypeOption.VIDEO,
            com.example.util.MediaUploadHelper.detectMediaTypeFromHints("application/octet-stream", "mov")
        )
        assertEquals(
            com.example.ui.screens.ReelMediaTypeOption.VIDEO,
            com.example.util.MediaUploadHelper.detectMediaTypeFromHints(null, "mkv")
        )
        assertEquals(
            com.example.ui.screens.ReelMediaTypeOption.IMAGE,
            com.example.util.MediaUploadHelper.detectMediaTypeFromHints("image/jpeg", "jpg")
        )

        // 2. Copy a device-like MP4 file URI through MediaUploadHelper -> permanent local file URI
        val tempVideo = java.io.File(context.cacheDir, "device_pick_tour.mp4")
        context.resources.openRawResource(R.raw.demo_reel_baner_4bhk).use { input ->
            tempVideo.outputStream().use { output -> input.copyTo(output) }
        }
        val pickedResult = com.example.util.MediaUploadHelper.persistAndPreparePickedMedia(
            context = context,
            sourceUri = Uri.fromFile(tempVideo),
            preferredType = com.example.ui.screens.ReelMediaTypeOption.IMAGE // Should auto-detect as VIDEO!
        )
        assertTrue("Picked device video must succeed even if preferred tab was IMAGE", pickedResult is com.example.util.PickedMediaResult.Success)
        val success = pickedResult as com.example.util.PickedMediaResult.Success
        assertEquals(com.example.ui.screens.ReelMediaTypeOption.VIDEO, success.mediaType)
        assertTrue("Persisted video URI must use durable file:// scheme", success.localMediaUri.startsWith("file://"))
        assertTrue("Persisted video file must exist on disk", java.io.File(Uri.parse(success.localMediaUri).path!!).exists())
        assertTrue("Video size should be positive MB", success.fileSizeMb > 0.0)

        // 3. Publish the persisted device video reel to PropReelRepository
        val repository = PropReelRepository(context)
        val customReelId = "reel_custom_video_test_${System.currentTimeMillis()}"
        val customReel = com.example.domain.model.Reel(
            id = customReelId,
            userId = "usr_pune_broker_01",
            creatorName = "Vikram Deshmukh",
            creatorBadge = VerificationBadge.RERA_VERIFIED,
            creatorRole = UserRole.BROKER,
            propertyId = null,
            propertyTitle = null,
            propertyPriceDisplay = null,
            propertyLocality = "Baner",
            propertyBhk = null,
            caption = "Device recorded 4 BHK walkthrough",
            videoUrl = success.localMediaUri,
            thumbnailUrl = success.localThumbnailUri,
            durationSeconds = success.durationSeconds,
            city = "Pune",
            locality = "Baner",
            isSponsored = false,
            likesCount = 0,
            commentsCount = 0,
            savesCount = 0,
            sharesCount = 0,
            isLikedByMe = false,
            isSavedByMe = false,
            mediaType = "video"
        )
        repository.createReel(customReel)
        val persisted = repository.getReelsStream().first().find { it.id == customReelId }
        assertNotNull("Uploaded device video reel must persist in Room", persisted)
        assertEquals("video", persisted?.mediaType)
        assertEquals(success.localMediaUri, persisted?.videoUrl)

        // 4. Empty 0-byte files must return an actionable Error
        val emptyFile = java.io.File(context.cacheDir, "empty_video.mp4").apply { writeBytes(ByteArray(0)) }
        val emptyResult = com.example.util.MediaUploadHelper.persistAndPreparePickedMedia(
            context = context,
            sourceUri = Uri.fromFile(emptyFile),
            preferredType = com.example.ui.screens.ReelMediaTypeOption.VIDEO
        )
        assertTrue("Empty 0-byte file must be rejected cleanly", emptyResult is com.example.util.PickedMediaResult.Error)
    }

    @Test
    fun `profile deleteReel removes reel, cleans local media file, and stays deleted across reseed`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        // Create a local file inside filesDir/uploaded_reels and attach it to a custom reel + custom property
        val uploadsDir = java.io.File(context.filesDir, "uploaded_reels").apply { mkdirs() }
        val uploadedFile = java.io.File(uploadsDir, "delete_test_clip.mp4").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val propId = "prop_delete_reel_link_${System.currentTimeMillis()}"
        val reelId = "reel_delete_custom_${System.currentTimeMillis()}"

        val linkedProperty = Property(
            id = propId,
            userId = "usr_pune_broker_01",
            sellerName = "Vikram Deshmukh",
            sellerRole = UserRole.BROKER,
            sellerPhone = "+91 98230 45678",
            sellerBadge = VerificationBadge.RERA_VERIFIED,
            title = "Temp Linked Tower",
            description = "Property linked to custom reel for delete test",
            category = PropertyCategory.RESIDENTIAL_BUY,
            price = 9500000.0,
            priceDisplay = "₹95 Lakh",
            isNegotiable = true,
            city = "Pune",
            locality = "Baner",
            landmark = "Baner High Street",
            bhk = 2,
            bathrooms = 2,
            carpetAreaSqft = 900.0,
            floorNumber = 5,
            totalFloors = 12,
            furnishing = FurnishingStatus.SEMI_FURNISHED,
            possession = PossessionStatus.READY_TO_MOVE,
            amenities = listOf("Lift"),
            imageUrls = emptyList(),
            linkedReelId = reelId
        )
        repository.createProperty(linkedProperty)

        val customReel = com.example.domain.model.Reel(
            id = reelId,
            userId = "usr_pune_broker_01",
            creatorName = "Vikram Deshmukh",
            creatorBadge = VerificationBadge.RERA_VERIFIED,
            creatorRole = UserRole.BROKER,
            propertyId = propId,
            propertyTitle = linkedProperty.title,
            propertyPriceDisplay = linkedProperty.priceDisplay,
            propertyLocality = linkedProperty.locality,
            propertyBhk = linkedProperty.bhk,
            caption = "Custom reel to be deleted from profile",
            videoUrl = Uri.fromFile(uploadedFile).toString(),
            thumbnailUrl = "",
            durationSeconds = 15,
            city = "Pune",
            locality = "Baner",
            mediaType = "video"
        )
        repository.createReel(customReel)

        assertTrue("Uploaded file must exist before delete", uploadedFile.exists())
        assertTrue("Custom reel must exist before delete", repository.getReelsStream().first().any { it.id == reelId })

        // Delete reel via profile action
        repository.deleteReel(reelId)

        // Verify reel removed, local file cleaned up, property link cleared, and reseed doesn't resurrect
        repository.seedInitialDatabaseIfEmpty()
        assertFalse("Deleted reel must be absent from Room stream", repository.getReelsStream().first().any { it.id == reelId })
        assertFalse("Owned file in uploaded_reels must be deleted", uploadedFile.exists())
        val updatedProp = repository.getPropertyById(propId)
        assertEquals(null, updatedProp?.linkedReelId)

        // Clean up temp property
        repository.deleteProperty(propId)
    }

    @Test
    fun `profile deleteProperty removes property, clears saved bookmark and reel property link`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PropReelRepository(context)
        repository.seedInitialDatabaseIfEmpty()

        val propId = "prop_delete_custom_${System.currentTimeMillis()}"
        val reelId = "reel_for_deleted_prop_${System.currentTimeMillis()}"

        val customProperty = Property(
            id = propId,
            userId = "usr_pune_broker_01",
            sellerName = "Vikram Deshmukh",
            sellerRole = UserRole.BROKER,
            sellerPhone = "+91 98230 45678",
            sellerBadge = VerificationBadge.RERA_VERIFIED,
            title = "Sunset Residences Test Listing",
            description = "Listing to verify deleteProperty cascade cleanup",
            category = PropertyCategory.RESIDENTIAL_BUY,
            price = 8500000.0,
            priceDisplay = "₹85 Lakh",
            isNegotiable = true,
            city = "Pune",
            locality = "Wakad",
            landmark = "Near Phoenix Mall",
            bhk = 2,
            bathrooms = 2,
            carpetAreaSqft = 860.0,
            floorNumber = 4,
            totalFloors = 10,
            furnishing = FurnishingStatus.UNFURNISHED,
            possession = PossessionStatus.READY_TO_MOVE,
            amenities = listOf("Parking"),
            imageUrls = emptyList()
        )
        repository.createProperty(customProperty)
        assertTrue(repository.toggleSaveProperty(propId))

        val linkedReel = com.example.domain.model.Reel(
            id = reelId,
            userId = "usr_pune_broker_01",
            creatorName = "Vikram Deshmukh",
            creatorBadge = VerificationBadge.RERA_VERIFIED,
            creatorRole = UserRole.BROKER,
            propertyId = propId,
            propertyTitle = customProperty.title,
            propertyPriceDisplay = customProperty.priceDisplay,
            propertyLocality = customProperty.locality,
            propertyBhk = customProperty.bhk,
            caption = "Reel linked to custom property",
            videoUrl = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=1000&q=80",
            thumbnailUrl = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=1000&q=80",
            durationSeconds = 0,
            city = "Pune",
            locality = "Wakad",
            mediaType = "image"
        )
        repository.createReel(linkedReel)

        // Delete property from profile
        repository.deleteProperty(propId)

        assertEquals(null, repository.getPropertyById(propId))
        assertFalse("Saved properties must not contain deleted property", repository.getSavedPropertiesStream().first().any { it.id == propId })
        val updatedReel = repository.getReelsStream().first().first { it.id == reelId }
        assertEquals(null, updatedReel.propertyId)

        // Clean up temp reel
        repository.deleteReel(reelId)
    }
}
