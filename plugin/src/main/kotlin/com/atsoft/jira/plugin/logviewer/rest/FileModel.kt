package com.atsoft.jira.plugin.logviewer.rest

import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement

@XmlRootElement
data class FileModel(
    @XmlElement var name: String = "",
    @XmlElement var path: String = "",
    @XmlElement var size: Long = 0,
    @XmlElement var modified: Long = 0,
    @XmlElement var isDirectory: Boolean = false
)
