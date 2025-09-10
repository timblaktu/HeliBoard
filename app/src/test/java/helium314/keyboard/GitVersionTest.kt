package helium314.keyboard

import helium314.keyboard.latin.BuildConfig
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GitVersionTest {

    @Test
    fun testGitVersionFieldsExist() {
        // Test that BuildConfig fields for git version info are present
        assertNotNull("GIT_VERSION field should exist", BuildConfig.GIT_VERSION)
        assertNotNull("GIT_COMMIT field should exist", BuildConfig.GIT_COMMIT)
    }

    @Test
    fun testGitVersionFieldsNotEmpty() {
        // Test that git version fields are populated with valid values
        assertFalse("GIT_VERSION should not be empty", BuildConfig.GIT_VERSION.isEmpty())
        assertFalse("GIT_COMMIT should not be empty", BuildConfig.GIT_COMMIT.isEmpty())
        
        // Should not be the fallback "unknown" value in normal circumstances
        // (though this might be "unknown" in some CI environments without git)
        assertTrue("GIT_VERSION should be meaningful", 
            BuildConfig.GIT_VERSION != "unknown" || BuildConfig.GIT_COMMIT == "unknown")
    }

    @Test
    fun testGitCommitHashFormat() {
        // Test that git commit hash follows expected format (short hash)
        if (BuildConfig.GIT_COMMIT != "unknown") {
            assertTrue("GIT_COMMIT should be 7-40 characters (short to full hash)",
                BuildConfig.GIT_COMMIT.length in 7..40)
            assertTrue("GIT_COMMIT should contain only hex characters",
                BuildConfig.GIT_COMMIT.matches(Regex("^[a-f0-9]+$")))
        }
    }

    @Test
    fun testGitDescribeFormat() {
        // Test that git describe follows expected patterns
        if (BuildConfig.GIT_VERSION != "unknown") {
            // Should either be just a commit hash, or a tag-based describe
            val isCommitHash = BuildConfig.GIT_VERSION.matches(Regex("^[a-f0-9]+(-dirty)?$"))
            val isTagDescribe = BuildConfig.GIT_VERSION.matches(Regex("^v?\\d+\\.\\d+.*"))
            val isDescribeFormat = BuildConfig.GIT_VERSION.matches(Regex("^.*-\\d+-g[a-f0-9]+(-dirty)?$"))
            
            assertTrue("GIT_VERSION should match expected git describe format",
                isCommitHash || isTagDescribe || isDescribeFormat)
        }
    }

    @Test
    fun testDebugVersionSuffix() {
        // Test that debug builds have version suffix (this test runs in debug context)
        // In a real debug build, the version name should include git info
        // This is more of a documentation test since we can't easily test build variants in unit tests
        
        // The actual version suffix testing would need to be done in build/integration tests
        // Here we just verify the git fields are available for the suffix
        assertNotNull("Git version info available for debug suffix", BuildConfig.GIT_VERSION)
    }

    @Test
    fun testGitVersionConsistency() {
        // Test that GIT_VERSION and GIT_COMMIT are consistent
        if (BuildConfig.GIT_VERSION != "unknown" && BuildConfig.GIT_COMMIT != "unknown") {
            // If GIT_VERSION contains a commit hash (ends with -g[hash]), 
            // it should be consistent with GIT_COMMIT
            val commitInDescribe = Regex("-g([a-f0-9]+)(-dirty)?$").find(BuildConfig.GIT_VERSION)
            if (commitInDescribe != null) {
                val commitFromDescribe = commitInDescribe.groupValues[1]
                assertTrue("GIT_COMMIT should start with commit hash from GIT_VERSION",
                    BuildConfig.GIT_COMMIT.startsWith(commitFromDescribe))
            }
        }
    }

    @Test
    fun testGitFallbackBehavior() {
        // Test that when git is unavailable, fallback behavior works
        // This is primarily tested by the helper functions in build.gradle.kts
        // Here we just ensure the fields are never null
        assertNotNull("GIT_VERSION should never be null", BuildConfig.GIT_VERSION)
        assertNotNull("GIT_COMMIT should never be null", BuildConfig.GIT_COMMIT)
        
        // Both should be "unknown" or both should be valid git values
        val bothUnknown = BuildConfig.GIT_VERSION == "unknown" && BuildConfig.GIT_COMMIT == "unknown"
        val bothValid = BuildConfig.GIT_VERSION != "unknown" && BuildConfig.GIT_COMMIT != "unknown"
        assertTrue("Git version fields should be consistently unknown or valid", bothUnknown || bothValid)
    }
}