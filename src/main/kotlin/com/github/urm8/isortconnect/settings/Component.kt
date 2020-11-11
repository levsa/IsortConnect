package com.github.urm8.isortconnect.settings

import com.github.urm8.isortconnect.service.SorterService
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.event.ActionListener
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class Component {
    fun getPreferredFocusedComponent(): JComponent = urlTextField
    private val urlTextField = JBTextField()
    private val triggerOnSaveButton = JBCheckBox("Trigger on save ?")
    private val pyprojectTomlTextField = JBTextField()
    private val loadPyProjectTomlButton = TextFieldWithBrowseButton(pyprojectTomlTextField) {
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("toml")
        val file = FileChooser.chooseFile(descriptor, null, null) ?: return@TextFieldWithBrowseButton
        if (file.name == "pyproject.toml") {
            pyprojectTomlTextField.text = file.path
        }
    }

    var url: String
        get() = urlTextField.text
        set(value) {
            urlTextField.text = value
        }
    var triggerOnSave: Boolean
        get() = triggerOnSaveButton.isSelected
        set(value) {
            triggerOnSaveButton.isSelected = value
        }

    var pyprojectToml: String
        get() = pyprojectTomlTextField.text
        set(value) {
            pyprojectTomlTextField.text = value
        }

    private val checkBtn = JButton("Check connection").apply {
        this.addActionListener(ActionListener {
            val isReachable = SorterService.ping()
            com.github.urm8.isortconnect.dialogs.PingDialog(isReachable).showAndGet()
        })
    }

    val panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Server Url"), urlTextField, 1, true)
            .addLabeledComponent(JBLabel("Trigger On Save"), triggerOnSaveButton, 2, false)
            .addLabeledComponent(JBLabel("Check connection"), checkBtn, 3, false)
            .addLabeledComponent(JBLabel("pyproject.toml"), loadPyProjectTomlButton, 4, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
}