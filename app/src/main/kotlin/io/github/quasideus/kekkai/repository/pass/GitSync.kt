package io.github.quasideus.kekkai.repository.pass

import java.io.File
import org.eclipse.jgit.api.Git

/** JGit wrapper for in-app store sync (no Termux). Clone if empty, else pull; no conflict handling. */
object GitSync {

    fun cloneOrPull(remoteUrl: String, localDir: File) {
        if (localDir.exists() && File(localDir, ".git").exists()) {
            Git.open(localDir).use { git ->
                git.pull().call()
            }
        } else {
            localDir.mkdirs()
            Git.cloneRepository()
                .setURI(remoteUrl)
                .setDirectory(localDir)
                .call()
                .close()
        }
    }
}
