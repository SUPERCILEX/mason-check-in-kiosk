package com.bymason.kiosk.checkin.feature.employeefinder

import com.bymason.kiosk.checkin.core.data.DefaultDispatcherProvider
import com.bymason.kiosk.checkin.core.data.DispatcherProvider
import com.bymason.kiosk.checkin.core.model.Employee
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.invoke
import kotlinx.coroutines.tasks.await

interface EmployeeRepository {
    suspend fun find(name: String): List<Employee>

    suspend fun registerEmployee(sessionId: String, employee: Employee): String
}

class DefaultEmployeeRepository(
        private val dispatchers: DispatcherProvider = DefaultDispatcherProvider
) : EmployeeRepository {
    override suspend fun find(name: String): List<Employee> = dispatchers.default {
        val result = Firebase.functions.getHttpsCallable("findEmployees").call(name).await()
        @Suppress("UNCHECKED_CAST") val data = result.data as List<Map<String, String>>
        data.map { Employee(it.getValue("id"), it.getValue("name"), it.getValue("photoUrl")) }
    }

    override suspend fun registerEmployee(
            sessionId: String,
            employee: Employee
    ) = dispatchers.default {
        val data = mapOf(
                "operation" to "here-to-see",
                "id" to sessionId,
                "employeeId" to employee.id
        )
        val result = Firebase.functions.getHttpsCallable("updateSession").call(data).await()
        result.data as String
    }
}
