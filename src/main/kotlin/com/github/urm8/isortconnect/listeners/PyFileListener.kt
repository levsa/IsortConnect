package com.github.urm8.isortconnect.listeners

import com.github.urm8.isortconnect.service.SorterService
import com.github.urm8.isortconnect.settings.IsortConnectService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.jetbrains.python.PythonFileType

class PyFileListener : AsyncFileListener {
    private lateinit var locator: ProjectLocator
    private lateinit var rootManagers: MutableMap<Project, ProjectRootManager>

    override fun prepareChange(events: MutableList<out VFileEvent>): AsyncFileListener.ChangeApplier? {
        locator = ProjectLocator.getInstance()
        rootManagers = mutableMapOf()

        val filesToVisit = events.mapNotNull { event -> checkEvent(event = event) }
        if (filesToVisit.isNotEmpty()) {
            return PyFileApplier(filesToVisit)
        }
        return null
    }

    private fun checkEvent(event: VFileEvent): PyFileWithService? {
        if (event.isFromSave && event.file?.fileType == PythonFileType.INSTANCE) {
            val file = event.file!!
            return locator.guessProjectForFile(file)?.run {
                val project = this
                if (project.service<IsortConnectService>().state.triggerOnSave) {
                    rootManagers.getOrPut(project, { ProjectRootManager.getInstance(this) }).fileIndex.run {
                        if (isInSource(file) || isInContent(file) || isInSourceContent(file) || isInTestSourceContent
                            (file)
                        ) {
                            return PyFileWithService(file, project.service<SorterService>())
                        }
                    }
                }
                null
            }
        }
        return null
    }

    class PyFileApplier(val toSort: Collection<PyFileWithService>) : AsyncFileListener.ChangeApplier {
        override fun beforeVfsChange() {
            super.beforeVfsChange()
            toSort.forEach { pyFile -> pyFile.service.sort(pyFile.file) }
        }
    }

    companion object {
        const val PY_EXT: String = "py"
        const val TIMEOUT = 500
    }
}
