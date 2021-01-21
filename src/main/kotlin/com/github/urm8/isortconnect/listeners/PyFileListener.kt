package com.github.urm8.isortconnect.listeners

import com.github.urm8.isortconnect.service.SorterService
import com.github.urm8.isortconnect.settings.IsortConnectService
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.jetbrains.python.PythonFileType
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

data class PyFile(val file: VirtualFile, val project: Project)

class PyFileListener : AsyncFileListener {

    override fun prepareChange(events: MutableList<out VFileEvent>): AsyncFileListener.ChangeApplier? {
        val locator = ProjectLocator.getInstance()
        val filesToUpdate = mutableSetOf<PyFile?>()
        for (
            file in events
                .mapNotNull { event -> event.file }
                .filter { file -> file.fileType == PythonFileType.INSTANCE }
        ) {
            val pyFile = locator.guessProjectForFile(file)?.run {
                if (isSourceFileAndTriggerOnSave(file)
                ) {
                    PyFile(file, this)
                } else {
                    null
                }
            }
            filesToUpdate.add(pyFile)
        }
        return PyFileApplier(filesToUpdate.filterNotNull())
    }

    private fun @Nullable Project.isSourceFileAndTriggerOnSave(file: @Nullable VirtualFile) =
        ProjectRootManager
            .getInstance(this)
            .fileIndex.run {
                inSourceContent(file) && FileDocumentManager.getInstance().isFileModified(file)
            } && this.service<IsortConnectService>().state.triggerOnSave

    private fun @NotNull ProjectFileIndex.inSourceContent(file: @Nullable VirtualFile) =
        this.isInSource(file) || this.isInContent(file) || this.isInSourceContent(file) ||
            this.isInTestSourceContent(file)

    class PyFileApplier(private val filesToUpdate: Collection<PyFile>) : AsyncFileListener.ChangeApplier {
        override fun afterVfsChange() {
            super.afterVfsChange()
            filesToUpdate.forEach { pyFile ->
                val sorter = pyFile.project.service<SorterService>()
                sorter.sort(pyFile.file)
            }
        }
    }

    companion object {
        const val PY_EXT: String = "py"
        const val TIMEOUT = 500
    }
}
