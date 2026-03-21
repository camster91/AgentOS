package com.agentOS.agents

import com.agentOS.api.*

class NoOpAgentAPI(override val storage: StorageAPI) : AgentAPI {
    override val ui: UIAPI = object : UIAPI {
        override suspend fun showMessage(message: String) {}
        override suspend fun showOptions(message: String, options: List<String>, onSelected: suspend (String) -> Unit) {}
        override suspend fun requestInput(prompt: String, type: String): String? = null
        override suspend fun showError(message: String) {}
        override suspend fun showSuccess(message: String) {}
    }
    override val notifications: NotificationsAPI = object : NotificationsAPI {
        override suspend fun send(title: String, message: String, priority: String) {}
    }
    override val calendar: CalendarAPI? = null
    override val contacts: ContactsAPI? = null
    override val messaging: MessagingAPI? = null
    override val health: HealthAPI? = null
    override val cloudSync: CloudSyncAPI? = null
}
