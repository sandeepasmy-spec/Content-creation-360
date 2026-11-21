package com.example

import com.example.data.model.LoginPortal
import com.example.data.model.TaskStatus
import com.example.data.model.UserRole
import com.example.util.GeofenceHelper
import com.example.util.SecurityUtils
import org.junit.Assert.*
import org.junit.Test

class StudioFlowCoreTest {

    @Test
    fun passwordHashing_saltedSha256VerifiesCorrectly() {
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPassword("studio360", salt)
        assertTrue(SecurityUtils.verifyPassword("studio360", salt, hash))
        assertFalse(SecurityUtils.verifyPassword("wrongpass", salt, hash))
        assertNotEquals("studio360", hash)
    }

    @Test
    fun geofenceDistance_insideAndOutsideDetection() {
        // Same point = 0 meters
        val sameDist = GeofenceHelper.calculateDistanceMeters(28.6139, 77.2090, 28.6139, 77.2090)
        assertTrue(sameDist < 1f)

        // ~1.1km away should exceed 150m radius
        val farDist = GeofenceHelper.calculateDistanceMeters(28.6139, 77.2090, 28.6239, 77.2090)
        assertTrue(farDist > 150f)
    }

    @Test
    fun dynamicQrToken_isConsistentWithinBucket() {
        val t1 = GeofenceHelper.generateDynamicQrToken(1_700_000_000_000L)
        val t2 = GeofenceHelper.generateDynamicQrToken(1_700_000_060_000L)
        assertTrue(t1.startsWith("SF360-OFFICE-"))
        assertEquals(t1, t2)
    }

    @Test
    fun rbacPermissions_enforcedPerRole() {
        assertTrue(UserRole.SUPER_ADMIN.canEditFinance)
        assertTrue(UserRole.INVESTOR.isReadOnly)
        assertFalse(UserRole.INVESTOR.canEditFinance)
        assertTrue(UserRole.INVESTOR.canViewFinance)

        assertFalse(UserRole.COORDINATOR_ANCHOR.canViewFinance)
        assertTrue(UserRole.COORDINATOR_ANCHOR.canAssignTasks)
        assertTrue(UserRole.COORDINATOR_ANCHOR.canVerifyScripts)

        assertTrue(UserRole.EXTERNAL_PANDIT.isExternal)
        assertEquals(LoginPortal.EXTERNAL, UserRole.EXTERNAL_PANDIT.portal)
        assertEquals(1.0f, TaskStatus.COMPLETED.progress)
    }
}
