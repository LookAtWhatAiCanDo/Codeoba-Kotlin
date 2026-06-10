package llc.lookatwhataicando.codeoba.core.util

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalFileResolverTest {

    private fun withMockUserHome(block: (File) -> Unit) {
        val originalUserHome = System.getProperty("user.home")
        val tempDir = File(System.getProperty("java.io.tmpdir"), "codeoba_resolver_test_home_" + System.currentTimeMillis())
        tempDir.mkdirs()
        System.setProperty("user.home", tempDir.absolutePath)
        try {
            block(tempDir)
        } finally {
            System.setProperty("user.home", originalUserHome)
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testRejectWebUrl() {
        val res = LocalFileResolver.resolveLocalFileLink("https://google.com", null, null)
        assertTrue(res is LocalFileResolution.Rejected)
    }

    @Test
    fun testRejectOpaqueFileUri() {
        val res = LocalFileResolver.resolveLocalFileLink("file:C:/foo", null, null)
        assertTrue(res is LocalFileResolution.Rejected)
    }

    @Test
    fun testRejectUncShares() {
        val res = LocalFileResolver.resolveLocalFileLink("file://server/share/foo", null, null)
        assertTrue(res is LocalFileResolution.Rejected)
    }

    @Test
    fun testAllowedWorkspaceLink() = withMockUserHome { home ->
        val workspace = File(home, "my-workspace")
        workspace.mkdirs()
        val file = File(workspace, "docs/guide.md")
        file.parentFile.mkdirs()
        file.writeText("hello")

        val res = LocalFileResolver.resolveLocalFileLink(
            rawLink = file.absolutePath,
            baseDirectory = workspace.toPath(),
            trustedRoot = workspace.toPath()
        )
        assertTrue(res is LocalFileResolution.Allowed)
        assertEquals(file.toPath().toRealPath(), (res as LocalFileResolution.Allowed).path)
    }

    @Test
    fun testConfirmationRequiredOutsideWorkspace() = withMockUserHome { home ->
        val workspace = File(home, "my-workspace")
        workspace.mkdirs()
        val externalFile = File(home, "external-folder/notes.txt")
        externalFile.parentFile.mkdirs()
        externalFile.writeText("notes")

        val res = LocalFileResolver.resolveLocalFileLink(
            rawLink = externalFile.absolutePath,
            baseDirectory = workspace.toPath(),
            trustedRoot = workspace.toPath()
        )
        assertTrue(res is LocalFileResolution.ConfirmationRequired)
        assertEquals(externalFile.toPath().toRealPath(), (res as LocalFileResolution.ConfirmationRequired).path)
    }

    @Test
    fun testRelativeResolution() = withMockUserHome { home ->
        val workspace = File(home, "my-workspace")
        workspace.mkdirs()
        val docsDir = File(workspace, "docs")
        docsDir.mkdirs()

        val targetFile = File(workspace, "images/fig.png")
        targetFile.parentFile.mkdirs()
        targetFile.writeText("image-data")

        val res = LocalFileResolver.resolveLocalFileLink(
            rawLink = "../images/fig.png",
            baseDirectory = docsDir.toPath(),
            trustedRoot = workspace.toPath()
        )
        assertTrue(res is LocalFileResolution.Allowed)
        assertEquals(targetFile.toPath().toRealPath(), (res as LocalFileResolution.Allowed).path)
    }

    @Test
    fun testTildeExpansion() = withMockUserHome { home ->
        val workspace = File(home, "my-workspace")
        workspace.mkdirs()
        val fileInHome = File(home, "Downloads/notes.txt")
        fileInHome.parentFile.mkdirs()
        fileInHome.writeText("downloads")

        val res = LocalFileResolver.resolveLocalFileLink(
            rawLink = "~/Downloads/notes.txt",
            baseDirectory = workspace.toPath(),
            trustedRoot = home.toPath()
        )
        assertTrue(res is LocalFileResolution.Allowed)
        assertEquals(fileInHome.toPath().toRealPath(), (res as LocalFileResolution.Allowed).path)
    }

    @Test
    fun testPreservesAnchorAndQueryInPlainPaths() = withMockUserHome { home ->
        val workspace = File(home, "my-workspace")
        workspace.mkdirs()
        // Create file with literal # or ? in its name
        val fileWithHash = File(workspace, "docs#anchor.txt")
        fileWithHash.writeText("content hash")

        val res = LocalFileResolver.resolveLocalFileLink(
            rawLink = fileWithHash.absolutePath,
            baseDirectory = workspace.toPath(),
            trustedRoot = workspace.toPath()
        )
        assertTrue(res is LocalFileResolution.Allowed)
        assertEquals(fileWithHash.toPath().toRealPath(), (res as LocalFileResolution.Allowed).path)
    }
}
