package com.byajbook.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoutes {
    // Tab Graph Roots
    @Serializable data object DashboardGraph : AppRoutes
    @Serializable data object CustomersGraph : AppRoutes
    @Serializable data object ReportsGraph : AppRoutes
    @Serializable data object SettingsGraph : AppRoutes

    // Destinations
    @Serializable data object Dashboard : AppRoutes
    @Serializable data object Customers : AppRoutes
    @Serializable data object Reports : AppRoutes
    @Serializable data object Settings : AppRoutes

    @Serializable 
    data class CustomerDetail(val customerId: String) : AppRoutes
    
    @Serializable 
    data class RecordDetail(val recordId: String) : AppRoutes

    @Serializable 
    data class CustomerReportDetail(val customerId: String) : AppRoutes
}
